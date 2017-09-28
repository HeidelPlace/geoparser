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
public class AdvancedContextToponymDisambiguator extends ToponymDisambiguator {


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

            final Set<Place> linkedPlaces =  new HashSet<>(namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class));
            final HashMap<Place, Place> alternateIds = new HashMap<>(getAlternatePlaces(linkedPlaces_2));
            //linkedPlaces_2 umbenennen und zu Set machen. alternateIds -> alle places
            if ( linkedPlaces == null || linkedPlaces.isEmpty()) {
                output.add(null);
                continue;
            }
            Place resolvedLocation = getPlaceWithHighestRating(linkedPlaces, linkedPlaces_2, alternateIds);

            if (resolvedLocation == null){
                List<Place> places = new ArrayList<>();
                places.addAll(linkedPlaces);
                resolvedLocation = HighestPopulationDisambiguator.getPlaceWithHighestPopulation(places);
            }
            output.add(new ResolvedLocation(resolvedLocation));
        }

        return output;
    }

    private Place getPlaceWithHighestRating(final Set<Place> linkedPlaces, final Set<Place> linkedPlaces_2, final HashMap<Place, Place> alternateIds) {
        final Set<Long> allIds = new HashSet<>();
        linkedPlaces_2.forEach(place ->allIds.add(place.getId()));

        final Set<Long> idsSamePlace = new HashSet<>();
        linkedPlaces.forEach(place -> idsSamePlace.add(place.getId()));

        final List<PlaceRelationship> bestRelations = new ArrayList<>(getBestRelationships(linkedPlaces, allIds, alternateIds));
        final PlaceRelationship output = getHighestRelationship(bestRelations, alternateIds);

        //System.out.println(output);
        if (output != null){
            Place outputLeftPlace = output.getLeftPlace();
            Place outputRightPlace = output.getRightPlace();

            if (alternateIds.containsKey(outputLeftPlace)){
                outputLeftPlace = alternateIds.get(outputLeftPlace);
            }

            if (alternateIds.containsKey(outputRightPlace)){
                outputRightPlace = alternateIds.get(outputRightPlace);
            }

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

    private Set<PlaceRelationship> getPlaceRelationships(final Place place){
        Set<PlaceRelationship> allRelations = new HashSet<>(place.getLeftPlaceRelationships());
        Set<PlaceRelationship> weightRelations = new HashSet<>();

        allRelations.addAll(place.getRightPlaceRelationships());

        allRelations.forEach(placeRelationship ->{
            if (placeRelationship.getType() != null && placeRelationship.getType().getId() == 33){
                weightRelations.add(placeRelationship);
            }
        });
        return weightRelations;
    }


    private Set<PlaceRelationship> getRelevantPlaceRelationships(final Place place, Set<Long> allIds, final HashMap<Place, Place> alternateIds, Set<Long> samePlaceIds){
        final Set<PlaceRelationship> allRelations = new HashSet<>(getPlaceRelationships(place));

        alternateIds.forEach((place1, place2) -> {
            if (allIds.contains(place2.getId())){
                allIds.remove(place2.getId());
                allIds.add(place1.getId());
            }
            if (samePlaceIds.contains(place2.getId())){
                samePlaceIds.add(place1.getId());
            }
        });
        Set<PlaceRelationship> relevantRelations = new HashSet<>();

        //Filter same place ids and alternate ids in order to get only relations between this place and another and not another instance of itself.
        allRelations.forEach(placeRelationship -> {
            if(allIds.contains(placeRelationship.getLeftPlace().getId()) && (allIds.contains(placeRelationship.getRightPlace().getId()))){
                if((samePlaceIds.contains(placeRelationship.getLeftPlace().getId()) && !samePlaceIds.contains(placeRelationship.getRightPlace().getId())) ||
                        (!samePlaceIds.contains(placeRelationship.getLeftPlace().getId()) && samePlaceIds.contains(placeRelationship.getRightPlace().getId()))) {
                    relevantRelations.add(placeRelationship);
                }
            }
        });
        return relevantRelations;
    }

    private PlaceRelationship getHighestRelationship(final List<PlaceRelationship> relations, final HashMap<Place, Place> alternateIds){
        PlaceRelationship output = null;
        Double alternatePlaceFactor;

        for (final PlaceRelationship relation : relations) {
            alternatePlaceFactor = 1.0;
            if (alternateIds.containsKey(relation.getLeftPlace()) || alternateIds.containsKey(relation.getRightPlace())){
                alternatePlaceFactor = 0.01;
            }
            if (relation.getLeftPlace() != null && (output == null || (Float.parseFloat(relation.getValue())*alternatePlaceFactor) > Float.parseFloat(output.getValue()))) {
                output = relation;
            }
        }

        if (output != null){
            System.out.println(output.getValue());
            System.out.println(output.getLeftPlace().getId());
            System.out.println(output.getRightPlace().getId());
        }

        return output;
    }

    private List<PlaceRelationship> getBestRelationships(final Set<Place> linkedPlaces, final Set<Long> allIds, final HashMap<Place, Place> alternateIds){
        Set<PlaceRelationship> finalRelation;
        List<PlaceRelationship> bestRelations = new ArrayList<>();
        PlaceRelationship result = new PlaceRelationship();
        final Set<Long> samePlaceIds = new HashSet<>();
        linkedPlaces.forEach(place -> samePlaceIds.add(place.getId()));

        for (final Place place : linkedPlaces) {
            Place bestPlace = getAlternatePlace(place, alternateIds);
            finalRelation = getRelevantPlaceRelationships(bestPlace, allIds, alternateIds, samePlaceIds);

            for (final PlaceRelationship relationResult : finalRelation) {
                if (result.getLeftPlace() == null || Float.parseFloat(relationResult.getValue()) > Float.parseFloat(result.getValue())) {
                    result = relationResult;
                }
            }
            bestRelations.add(result);
        }
        return bestRelations;
    }

    private Place getLeftPlaceRelationshipPlace(final Place place){
        Place leftPlace;

        for (PlaceRelationship relation : place.getLeftPlaceRelationships()){
            Boolean admin = false;

            if (relation.getType().getName().equals(RelationshipTypes.SUBDIVISION.typeName) ||
                    relation.getType().getName().equals(RelationshipTypes.WITHIN_DIVISION.typeName)) {
                for (int i=2; i<6; i++){
                    if(!(relation.getRightPlace().getPlaceTypeAssignmentsByType((("geonames_adm" + i))).isEmpty())){
                        admin = true;
                    }
                }
                if (admin) {
                    leftPlace = relation.getRightPlace();

                    return leftPlace;
                }
            }
        }
        return null;
    }

    private Place getMostRelationshipsPlace(final Place place, final Set<Place> linkedPlaces){
        Place topPlace = place;
        Place thisPlace = place;
        Place lastPlace = null;
        Integer size;
        Integer mostRelations = 0;

        while (thisPlace != null && lastPlace != thisPlace){
            size = getPlaceRelationships(thisPlace).size();

            if (size>mostRelations && !linkedPlaces.contains(thisPlace)){
                mostRelations = size;
                topPlace = thisPlace;
            }
            lastPlace = thisPlace;
            thisPlace = getLeftPlaceRelationshipPlace(thisPlace);
        }

        return topPlace;
    }

    private HashMap<Place, Place> getAlternatePlaces(final Set<Place> linkedPlaces){
        HashMap<Place, Place> alternatePlaces = new HashMap<>();

        for(Place place : linkedPlaces){
            final Place originPlace = place;
            place = getMostRelationshipsPlace(place, linkedPlaces);

            if (originPlace != place){
                alternatePlaces.put(place, originPlace);
            }
        }
        return alternatePlaces;
    }

    private Place getAlternatePlace(final Place place, final HashMap<Place, Place> alternateIds){
        if(alternateIds.containsValue(place)){
            for(Map.Entry<Place, Place> entry : alternateIds.entrySet()){
                if(entry.getValue() == place){
                    return entry.getKey();
                }
            }
        }
        return place;
    }

    private Place resolveCountry(final Place place){
        return place;
    }
}
