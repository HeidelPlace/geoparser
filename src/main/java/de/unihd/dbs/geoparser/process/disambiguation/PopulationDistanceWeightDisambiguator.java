package de.unihd.dbs.geoparser.process.disambiguation;

import com.vividsolutions.jts.geom.Coordinate;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import de.unihd.dbs.geoparser.process.util.Haversine;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by their summarized distance to all
 * candidates multiplied with a value between 0 and 1 decreasing with higher population.
 * Currently performing best in comparison to baseline modules and WLN module.
 *
 * @author Fabio Becker
 */
public class PopulationDistanceWeightDisambiguator extends ToponymDisambiguator {
    private Gazetteer gazetteer;
    private Map<Long, Double> placeDistances;

    /**
     * Constructor.
     *
     * @param gazetteer currently used gazetteer instance.
     */
    public PopulationDistanceWeightDisambiguator(final Gazetteer gazetteer) {
        super();
        this.gazetteer = gazetteer;
    }

    /**
     * Overridden requirements that are to be fulfilled.
     *
     * @return requirements for the implemented module.
     */
    @Override
    public Set<Annotator.Requirement> requires() {
        return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
    }

    /**
     * Requirements fulfilled by the Disambiguator.
     *
     * @return set of requirements.
     */
    @Override
    public Set<Annotator.Requirement> requirementsSatisfied() {
        return Collections.emptySet();
    }

    /**
     * Core function used by the geoparsing pipeline.
     *
     * @param namedEntities the {@link CoreAnnotations.MentionsAnnotation}, from which to disambiguate contained toponyms.
     * @param document      the source document.
     * @param sentence      the source sentence.
     * @return set of resolved locations.
     */
    @Override
    public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document,
                                               final CoreMap sentence) {
        final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());
        final List<ArrayList<Long>> allLinkedPlacesIdList = new ArrayList<>();
        final List<ArrayList<Place>> allLinkedPlacesList = new ArrayList<>();
        final Map<Long, Coordinate> placeCoordinates;

        namedEntities.forEach(entity -> {
            try {
                allLinkedPlacesList.add(new ArrayList<>(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class)));
            } catch (NullPointerException ignored) {
            }
        });
        placeCoordinates = getAllCoordinates(allLinkedPlacesList);

        if (allLinkedPlacesList.isEmpty()) {
            output.add(null);
            return output;
        }

        allLinkedPlacesList.forEach(list -> allLinkedPlacesIdList.add(new ArrayList<>(list.stream().map(AbstractEntity::getId).collect(Collectors.toList()))));

        placeDistances = getAllDistances(placeCoordinates, allLinkedPlacesIdList);

        for (final CoreMap namedEntity : namedEntities) {
            Place resolvedPlace;

            if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
                    NamedEntityType.LOCATION.name)) {
                output.add(null);
                continue;
            }

            final List<Place> linkedPlaces = namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class);

            if (linkedPlaces == null || linkedPlaces.isEmpty()) {
                output.add(null);
                continue;
            }

            resolvedPlace = resolveByScore(linkedPlaces, allLinkedPlacesIdList);

            if (resolvedPlace == null) {
                resolvedPlace = linkedPlaces.get(0);
            }

            output.add(new ResolvedLocation(resolvedPlace));
        }
        placeDistances.clear();
        return output;
    }

    /**
     * Creates a map containing all ids and the according coordinates received from getFootprints().
     *
     * @param allLinkedPlaces all candidate locations retrieved for a toponym.
     * @return map containing coordinates associated with a place identifier.
     */
    private Map<Long, Coordinate> getAllCoordinates(final List<ArrayList<Place>> allLinkedPlaces) {
        Map<Long, Coordinate> resultMap = new HashMap<>();

        for (final List<Place> places : allLinkedPlaces) {
            for (final Place place : places) {
                resultMap.put(place.getId(), place.getFootprints().iterator().next().getGeometry().getCoordinate());
            }
        }

        return resultMap;
    }


    /**
     * Creates a map for each linked toponym with the sum of its distances to all other toponyms.
     *
     * @param allCoordinates place identifier and coordinate mapping
     * @param allPlaceIds    list of all candidate places retrieved from the document.
     * @return map containing place identifiers and the according sum of distances to all candidate locations
     */
    private Map<Long, Double> getAllDistances(final Map<Long, Coordinate> allCoordinates, final List<ArrayList<Long>> allPlaceIds) {
        Map<Long, Double> resultMap = new HashMap<>();

        for (final ArrayList<Long> places : allPlaceIds) {
            for (final Long place : places) {
                List<Double> sum = new ArrayList<>();

                allCoordinates.forEach((aLong, coordinate) -> {
                    if (!aLong.equals(place)) {
                        sum.add(Haversine.distance(coordinate.y, coordinate.x, allCoordinates.get(place).y, allCoordinates.get(place).x));
                    }
                });

                resultMap.put(place, sum.stream().mapToDouble(Double::doubleValue).sum());
            }
        }

        return resultMap;
    }

    /**
     * Resolve the best candidate by calculating a score of its summarized distance to all other candidates, its population
     * and a score computed by the edge weight according to the WLN.
     * <p>
     * Note that this calculation can be weighted in various ways and may be extended by additional components, such as
     * administrative levels.
     *
     * @param places candidate locations.
     * @return candidate with best score.
     */

    private Place resolveByScore(final List<Place> places, final List<ArrayList<Long>> allLinkedIds) {
        Place resolvedPlace = null;
        double tmpScore = 0.0;
        final Map<Long, Double> weightMap = getWeightSum(places, allLinkedIds);

        for (final Place place : places) {
            final Long placeId = place.getId();
            double placeScore;
            double weightBonus = 1.0;
            double popBonus = 1.0;

            if (weightMap.containsKey(placeId)) {
                weightBonus = 0.5 - weightMap.get(placeId);
            }

            try {
                popBonus = (1 / Double.parseDouble(place.getPropertiesByType("population")
                        .iterator().next().getValue()));
            } catch (Exception ignored) {
            }

            try {
                placeScore = placeDistances.get(placeId) * popBonus * weightBonus;
            } catch (Exception exc) {
                placeScore = 100000000;
            }

            if (placeScore < tmpScore || tmpScore == 0.0) {
                tmpScore = placeScore;
                resolvedPlace = place;
            }
        }
        return resolvedPlace;
    }

    /**
     * Compute a score by retrieving the best edge weight of a place to a bucket of candidates (candidate set for a
     * specific location in the document). All score are summed up and stored in a map.
     *
     * @param linkedPlaces candidate locations for the current toponym
     * @param allLinkedIds all candidate location identifiers retrieved from the document
     * @return map containing the identifier of a location and its according score.
     */
    private Map<Long, Double> getWeightSum(final List<Place> linkedPlaces, final List<ArrayList<Long>> allLinkedIds) {
        Map<Long, Double> resultMap = new HashMap<>();

        for (final Place linkedPlace : linkedPlaces) {
            Long placeId = linkedPlace.getId();
            double tmpScore = 0.0;

            for (ArrayList<Long> bucket : allLinkedIds) {
                if (bucket.contains(placeId)) continue;

                // retrieve all edge weights between this place and all candidates from other named entities
                final List<Object[]> relationships = gazetteer.getEntityManger().createNativeQuery("SELECT place_1, place_2, weight " +
                        "FROM wln WHERE place_1 = ?1 AND place_2 IN ?2 OR " +
                        "place_2 = ?1 AND place_1 IN ?2 ORDER BY cast(weight AS DOUBLE PRECISION) DESC LIMIT 1")
                        .setParameter(1, placeId)
                        .setParameter(2, bucket)
                        .getResultList();

                if (relationships.isEmpty()) {
                    continue;
                }
                tmpScore += Double.parseDouble(relationships.get(0)[2].toString());
            }
            if (tmpScore != 0.0) {
                resultMap.put(placeId, tmpScore);
            }
        }

        return resultMap;
    }


}
