package de.unihd.dbs.geoparser.process.disambiguation;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.CoreMap;

import java.math.BigInteger;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by the edges of the Wikipedia-Location-Network.
 * The edge between two toponyms in a sentence with the highest value predicts the most likely match for the linked toponyms.
 *
 * @author fbecker
 */
public class WikipediaLocationNetworkDisambiguator extends ToponymDisambiguator {
    private final Gazetteer gazetteer;
    private List<Place> unambiguousPlaces = new ArrayList<>();
    private Map<Long, List<Double>> seeds = new HashMap<>();
    private static final Double WEIGHT_THRESHOLD = 0.0;

    /**
     * Constructor.
     *
     * @param gazetteer currently used gazetteer instance.
     */
    public WikipediaLocationNetworkDisambiguator(final Gazetteer gazetteer) {
        super();
        this.gazetteer = gazetteer;
    }

    /**
     * Overridden requirements that are to be fulfilled.
     *
     * @return requirements for the implemented module.
     */
    @Override
    public Set<Requirement> requires() {
        return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
    }

    /**
     * Requirements fulfilled by the Disambiguator.
     *
     * @return set of requirements.
     */
    @Override
    public Set<Requirement> requirementsSatisfied() {
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

        namedEntities.forEach(entity -> {
            try {
                allLinkedPlacesList.add(new ArrayList<>(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class)));
            } catch (NullPointerException ignored) {
            }
        });

        if (allLinkedPlacesList.isEmpty()) {
            output.add(null);
            return output;
        }

        allLinkedPlacesList.forEach(list -> allLinkedPlacesIdList.add(new ArrayList<>(list.stream().map(AbstractEntity::getId).collect(Collectors.toList()))));

        final List<Long> allLinkedIds = allLinkedPlacesIdList.stream().flatMap(Collection::stream).collect(Collectors.toList());

        allLinkedPlacesList.forEach(places -> {
            try {
                getSeed(places, allLinkedIds);
            } catch (NullPointerException ignored) {
            }
        });

        for (final List<Place> linkedPlaces : allLinkedPlacesList) {
            Place resolvedLocation = null;

            if (!Collections.disjoint(unambiguousPlaces, linkedPlaces)) {
                List<Place> getIntersection = new ArrayList<>(unambiguousPlaces);

                getIntersection.retainAll(linkedPlaces);
                resolvedLocation = getIntersection.get(0);
            } else if (!unambiguousPlaces.isEmpty() && unambiguousPlaces.size() != namedEntities.size()) {
                resolvedLocation = getPlaceByEdgeWeightSum(new HashSet<>(linkedPlaces));
            }

            if (resolvedLocation == null) {
                resolvedLocation = HighestPopulationDisambiguator.getPlaceWithHighestPopulation(new ArrayList<>(linkedPlaces));
            }

            try {
                output.add(new ResolvedLocation(resolvedLocation));
            } catch (Exception e) {
                output.add(null);
            }
        }

        seeds.clear();
        unambiguousPlaces.clear();

        return output;
    }

    /**
     * Retrieves the intersecting ids of all candidate locations and those present in the WLN.
     *
     * @param allLinkedIds identifiers for all candidate locations.
     * @return identifiers that occur in the WLN.
     */
    private Set<Long> getIdsInWLN(final List<Long> allLinkedIds) {
        final Set<Long> occurredIds = new HashSet<>();
        final List<Object> checkSeedList = new ArrayList<Object>(gazetteer.getEntityManger().createNativeQuery("SELECT * FROM " +
                "(SELECT DISTINCT place_1 FROM wln " +
                "WHERE wln.place_1 IN :ids UNION DISTINCT " +
                "SELECT DISTINCT place_2 FROM wln WHERE wln.place_2 IN :ids) AS ids")
                .setParameter("ids", allLinkedIds)
                .getResultList());

        checkSeedList.forEach(id -> occurredIds.add(((BigInteger) id).longValue()));
        return occurredIds;
    }

    /**
     * Retrieves seed locations, i.e., locations where exactly one candidate appears in the WLN.
     * These locations are used as the base neighbourhood of locations in a document.
     *
     * @param linkedPlaces    candidate locations for the current place.
     * @param allLinkedPlaces identifiers of all candidate locations.
     */
    private void getSeed(final List<Place> linkedPlaces, final List<Long> allLinkedPlaces) {
        Place seedPlace;
        Set<Long> linkedIds = new HashSet<>(linkedPlaces.size());

        linkedPlaces.forEach(place -> linkedIds.add(place.getId()));

        final List<Long> intersection = new ArrayList<>(CollectionUtils.intersection(linkedIds, getIdsInWLN(allLinkedPlaces)));
        if (intersection.size() == 1) {
            seedPlace = gazetteer.getPlace(intersection.get(0));
            fillSeedMap(seedPlace.getId(), allLinkedPlaces);
            unambiguousPlaces.add(seedPlace);
        }
    }

    /**
     * Computes the sum of edge weights between a candidate and all seed locations.
     *
     * @param place current candidate location.
     * @return sum of edge weights to all seed locations.
     */
    private double getSeedRelations(final Place place) {
        double sum = 0.0;
        List<Double> seedWeights = seeds.get(place.getId());

        if (seedWeights != null) {
            for (double aDouble : seedWeights) {
                sum += aDouble;
            }
        }
        return sum;
    }

    /**
     * Retrieves the best scoring candidate per bucket (set of candidates).
     *
     * @param linkedPlaces candidate locations of the current toponym.
     * @return best scoring candidate location.
     */
    private Place getPlaceByEdgeWeightSum(final Set<Place> linkedPlaces) {
        double edgeWeightSum = 0.0;
        Place output = null;

        for (final Place place : linkedPlaces) {
            double tempSum = getSeedRelations(place);
            if (tempSum > edgeWeightSum && tempSum > WEIGHT_THRESHOLD) {
                edgeWeightSum = tempSum;
                output = place;
            }
        }
        return output;
    }

    /**
     * Fill the map with seed locations and a list of edges to candidates from the document.
     *
     * @param id              current seed id.
     * @param allLinkedPlaces identifiers of all candidate locations.
     */
    private void fillSeedMap(final Long id, final List<Long> allLinkedPlaces) {
        final List<Object[]> queryList = new ArrayList<Object[]>(gazetteer.getEntityManger().createNativeQuery("SELECT " +
                "weight, place_1, place_2 FROM wln " +
                "WHERE (place_1 = CAST((:id) AS BIGINT) OR place_2 = CAST((:id) AS BIGINT))")
                .setParameter("id", id).getResultList());

        queryList.forEach(objects -> {
            final List<Double> tmpList;

            if (objects[1] == id && allLinkedPlaces.contains(toLong(objects[2]))) {
                tmpList = seeds.computeIfAbsent(toLong(objects[2]), aLong -> new ArrayList<Double>() {{
                    add(strToDouble(objects[0].toString()));
                }});
                if (tmpList.size() > 1 || seeds.get(toLong(objects[2])).get(0).equals(strToDouble(objects[0].toString()))) {
                    tmpList.add(strToDouble(objects[0].toString()));
                }
            } else {
                tmpList = seeds.computeIfAbsent(toLong(objects[1]), aLong -> new ArrayList<Double>() {{
                    add(strToDouble(objects[0].toString()));
                }});
                if (tmpList.size() > 1 || seeds.get(toLong(objects[1])).get(0).equals(strToDouble(objects[0].toString()))) {
                    tmpList.add(strToDouble(objects[0].toString()));
                }
            }
        });
    }

    /**
     * Util function for casting objects to type long.
     *
     * @param object to be cast.
     * @return cast object.
     */
    private Long toLong(final Object object) {
        return ((BigInteger) object).longValue();
    }

    /**
     * Util function for casting objects to type string.
     *
     * @param object to be cast.
     * @return cast object.
     */
    private Double strToDouble(final Object object) {
        return Double.parseDouble((String) object);
    }
}
