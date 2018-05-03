package de.unihd.dbs.geoparser.process.disambiguation;

import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.CoreMap;

import java.util.*;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by the edges of the WikipediaLocationNetwork.
 * The edge between two toponyms in a document with the highest value predicts the most likely match for named entities.
 *
 * @author Fabio Becker
 */
public class NaiveDisambiguator extends ToponymDisambiguator {
    private final Gazetteer gazetteer;
    private final Set<Long> allLinkedIds = new HashSet<>();

    /**
     * Constructor.
     *
     * @param gazetteer currently used gazetteer instance.
     */
    public NaiveDisambiguator(final Gazetteer gazetteer) {
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
     * Overridden implementation of the abstract disambiguate method. Gathering all ids and iterating over each
     * linked toponym, then calling the resolvePlace function to determine the best fitting location.
     *
     * @param namedEntities the {@link CoreAnnotations.MentionsAnnotation}, from which to disambiguate contained toponyms.
     * @param document      the source document.
     * @param sentence      the source sentence.
     * @return
     */
    @Override
    public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document,
                                               final CoreMap sentence) {
        final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());
        List<Place> allLinkedPlaces = new ArrayList<>();


        namedEntities.forEach(entity -> {
            if (entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class) != null) {
                allLinkedPlaces.addAll(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class));
            }
        });

        allLinkedPlaces.forEach(list -> allLinkedIds.add(list.getId()));

        for (final CoreMap namedEntity : namedEntities) {
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
            Place resolvedLocation = resolvePlace(linkedPlaces);

            if (resolvedLocation == null) {
                resolvedLocation = HighestPopulationDisambiguator.getPlaceWithHighestPopulation(linkedPlaces);
            }
            output.add(new ResolvedLocation(resolvedLocation));
        }
        allLinkedIds.clear();

        return output;
    }

    /**
     * Resolve the best fitting candidate location for each mentioned entity by retrieving
     * all edge weights from the WikipediaLocationNetwork.
     *
     * @param linkedPlaces All candidate locations for one mentioned entity.
     * @return Candidate with the highest scores/sum of edge weights.
     */
    private Place resolvePlace(final List<Place> linkedPlaces) {
        final Set<Long> idsSamePlace = new HashSet<>();
        final Set<Long> filteredPlaceIds = new HashSet<>(allLinkedIds);
        double topScore = 0.0;
        Place resolvedPlace = null;

        linkedPlaces.forEach(place -> idsSamePlace.add(place.getId()));

        filteredPlaceIds.removeAll(idsSamePlace);

        if (idsSamePlace.isEmpty() || filteredPlaceIds.isEmpty()) {
            return null;
        }

        for (final Place linkedPlace : linkedPlaces) {
            double tmpScore = 0.0;

            // retrieve all edge weights between this place and all candidates from other named entities
            final List relationships = gazetteer.getEntityManger().createNativeQuery("SELECT value " +
                    "FROM place_relationship WHERE left_place_id = ?1 AND right_place_id IN ?2 AND type_id=33 OR " +
                    "right_place_id = ?1 AND left_place_id IN ?2 AND type_id=33")
                    .setParameter(1, linkedPlace.getId())
                    .setParameter(2, allLinkedIds)
                    .getResultList();

            // sum up edge weights to temporal score
            for (Object relationship : relationships) {
                tmpScore = tmpScore + Double.parseDouble(relationship.toString());
            }

            //check if score is new high score
            if (tmpScore > topScore) {
                topScore = tmpScore;
                resolvedPlace = linkedPlace;
            }
        }

        return resolvedPlace;
    }
}
