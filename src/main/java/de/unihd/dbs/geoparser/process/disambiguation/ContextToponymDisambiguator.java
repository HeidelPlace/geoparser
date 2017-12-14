package de.unihd.dbs.geoparser.process.disambiguation;

import java.util.*;
import java.util.stream.Collectors;

import com.jcraft.jsch.HASH;
import de.unihd.dbs.geoparser.core.GeoparsingAnnotations;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedLocation;
import de.unihd.dbs.geoparser.gazetteer.models.*;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.types.RelationshipTypes;
import de.unihd.dbs.geoparser.process.linking.ToponymLinkingAnnotator;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import de.unihd.dbs.geoparser.process.disambiguation.HighestPopulationDisambiguator;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator.Requirement;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.io.filefilter.FalseFileFilter;
import org.apache.xpath.operations.Bool;

import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by the edges of the Wikipedia-Location-Network.
 * The edge between two toponyms in a sentence with the highest value predicts the most likely match for the linked toponyms.
 *
 * @author fbecker
 *
 */
public class ContextToponymDisambiguator extends ToponymDisambiguator {


    @Override
    public Set<Requirement> requires() {
        return Collections.singleton(ToponymLinkingAnnotator.TOPONYM_LINKING_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.emptySet();
    }

    @Override
    public List<ResolvedLocation> disambiguate(final List<CoreMap> namedEntities, final Annotation document,
                                               final CoreMap sentence) {
        final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());
        Set<Place> linkedPlaces_2 = new HashSet<>();

        namedEntities.forEach(entity -> {
            if(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class) != null){
                linkedPlaces_2.addAll(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class));
            }
        });
        //getToponymTextDistance(namedEntities, sentence);
        for (final CoreMap namedEntity : namedEntities) {
            if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
                    NamedEntityType.LOCATION.name)) {
                output.add(null);
                continue;
            }

            final List<Place> linkedPlaces = namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class);
            if ( linkedPlaces == null || linkedPlaces.isEmpty()) {
                output.add(null);
                continue;
            }
            Place resolvedLocation = getPlaceWithHighestRating(linkedPlaces, linkedPlaces_2);

            if (resolvedLocation == null){
                resolvedLocation = HighestPopulationDisambiguator.getPlaceWithHighestPopulation(linkedPlaces);
                if (resolvedLocation == null){
                    resolvedLocation = HighestAdminLevelDisambiguator.getPlaceWithHighestAdminLevel(linkedPlaces);
                }
            }
            output.add(new ResolvedLocation(resolvedLocation));
        }

        return output;
    }

    public static Place getPlaceWithHighestRating(final List<Place> linkedPlaces, final Set<Place> linkedPlaces_2) {
        final Set<Long> allIds = new HashSet<>();
        linkedPlaces_2.forEach(place ->allIds.add(place.getId()));
        final Set<Long> idsSamePlace = new HashSet<>();
        linkedPlaces.forEach(place -> idsSamePlace.add(place.getId()));

        final List<PlaceRelationship> bestRelations = new ArrayList<>(getBestRelationships(linkedPlaces, allIds));
        final PlaceRelationship output = getHighestRelationship(bestRelations);

        if (output != null){
            final Place outputLeftPlace = output.getLeftPlace();
            final Place outputRightPlace = output.getRightPlace();

            if (idsSamePlace.contains(outputLeftPlace.getId())) {
                for (Place place : linkedPlaces) {
                    if (Objects.equals(place.getId(), outputLeftPlace.getId())) {
                        return resolveCountry(place);
                    }
                }
            } else {
                for (Place place : linkedPlaces) {
                    if (Objects.equals(place.getId(), outputRightPlace.getId())) {
                        return resolveCountry(place);
                    }
                }
            }
        }

        System.out.println("NO MATCH --- HIGHEST POP. SHOWN");
        return null;
    }

    private static Set<PlaceRelationship> getPlaceRelationships(final Place place){
        Set<PlaceRelationship> allRelations = new HashSet<>(place.getLeftPlaceRelationships());
        Set<PlaceRelationship> weightRelations = new HashSet<>();

        allRelations.addAll(place.getRightPlaceRelationships());

        allRelations.forEach(placeRelationship ->{
            if (placeRelationship.getType().getId() == 33){
                weightRelations.add(placeRelationship);
            }
        });
        return weightRelations;
    }


    private static Set<PlaceRelationship> getRelevantPlaceRelationships(final Place place, final Set<Long> allIds){
        final Set<PlaceRelationship> allRelations = new HashSet<>(getPlaceRelationships(place));
        Set<PlaceRelationship> relevantRelations = new HashSet<>();

        allRelations.forEach(placeRelationship -> {
            if(allIds.contains(placeRelationship.getLeftPlace().getId()) && allIds.contains(placeRelationship.getRightPlace().getId())){
                relevantRelations.add(placeRelationship);
            }
        });
        return relevantRelations;
    }

    private static PlaceRelationship getHighestRelationship(final List<PlaceRelationship> relations){
        PlaceRelationship output = null;

        for (final PlaceRelationship relation : relations) {
            if (relation.getLeftPlace() != null && (output == null || Float.parseFloat(relation.getValue()) > Float.parseFloat(output.getValue()))) {
                output = relation;
            }
        }
        return output;
    }

    private static List<PlaceRelationship> getBestRelationships(final List<Place> linkedPlaces, final Set<Long> allIds){
        Set<PlaceRelationship> finalRelation;
        List<PlaceRelationship> bestRelations = new ArrayList<>();

        for (Place place : linkedPlaces) {
            PlaceRelationship result = new PlaceRelationship();
            finalRelation = getRelevantPlaceRelationships(place, allIds);

            for (final PlaceRelationship relationResult : finalRelation) {
                if (result.getLeftPlace() == null || Float.parseFloat(relationResult.getValue()) > Float.parseFloat(result.getValue())) {
                    result = relationResult;
                }
            }
            bestRelations.add(result);
        }
        return bestRelations;
    }

    private Integer getToponymTextDistance(final List<CoreMap> namedEntities, final CoreMap sentence){
        for (final CoreMap entity : namedEntities){
            String place = entity.get(CoreAnnotations.TextAnnotation.class);
            System.out.println(place);
            String sentence_2 = sentence.toString();
            System.out.println(sentence_2.substring(sentence_2.indexOf(place), sentence_2.indexOf("Hamburg.")));
        }

        return null;
    }

    private static Place resolveCountry(final Place place){
        return place;
    }
}
