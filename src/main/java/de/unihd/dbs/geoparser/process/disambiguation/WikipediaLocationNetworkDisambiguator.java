package de.unihd.dbs.geoparser.process.disambiguation;

import java.lang.reflect.Array;
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
import net.bytebuddy.implementation.bytecode.Throw;
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
public class WikipediaLocationNetworkDisambiguator extends ToponymDisambiguator {


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
        final Set<Place> seeds = new HashSet<>();
        final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());

        namedEntities.forEach(entity ->
                seeds.add(getSeed(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class))));

        if (seeds.contains(null)){
            seeds.remove(null);
        }

        for (final CoreMap namedEntity : namedEntities) {
            Set<Place> linkedPlaces = null;
            ArrayList<Place> unambiguousPlace = new ArrayList<>(seeds);
            Place resolvedLocation;

            try{
                linkedPlaces = new HashSet<>(namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class));
            }
            catch (NullPointerException error){
            }

            if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
                    NamedEntityType.LOCATION.name)) {
                output.add(null);
                continue;
            }

            if (linkedPlaces == null || linkedPlaces.isEmpty()) {
                output.add(null);
                continue;
            }

            if (!Collections.disjoint(unambiguousPlace, linkedPlaces)) {
                unambiguousPlace.retainAll(linkedPlaces);
                resolvedLocation = unambiguousPlace.get(0);
            }
            else {
                resolvedLocation = getResolvedPlace(linkedPlaces, seeds);
            }


            if (resolvedLocation == null) {
                List<Place> places = new ArrayList<>();
                places.addAll(linkedPlaces);
                resolvedLocation = HighestPopulationDisambiguator.getPlaceWithHighestPopulation(places);
                System.out.println("NO SEEDS - RESULTS BY HIGHEST POPULATION");
            }
            output.add(new ResolvedLocation(resolvedLocation));
        }

        return output;
    }

    private Place getResolvedPlace(final Set<Place> linkedPlaces, final Set<Place> seeds) {
        if (seeds.isEmpty()){
            return null;
        }

        return getPlaceByEdgeWeightSum(linkedPlaces, seeds);
    }

    private Place getSeed(final List<Place> linkedPlaces) {
        Place seed = new Place();
        Integer count = 0;

        if (linkedPlaces == null){
            return null;
        }
        for (final Place place : linkedPlaces) {
            if (getPlaceRelationships(place).size() != 0) {
                count += 1;
                seed = place;
            }
            if (count > 1) {
                return null;
            }
        }
        if (count == 1) {
            return seed;
        } else {
            return null;
        }

    }

    private Set<PlaceRelationship> getPlaceRelationships(final Place place) {
        Set<PlaceRelationship> allRelations = new HashSet<>(place.getLeftPlaceRelationships());
        Set<PlaceRelationship> weightRelations = new HashSet<>();

        allRelations.addAll(place.getRightPlaceRelationships());

        allRelations.forEach(placeRelationship -> {
            if (placeRelationship.getType() != null && placeRelationship.getType().getId() == 33) {
                weightRelations.add(placeRelationship);
            }
        });
        return weightRelations;
    }

    private Set<PlaceRelationship> getSeedRelations(final Place place, final Set<PlaceRelationship> relationships, final Set<Place> seeds) {
        Set<PlaceRelationship> relationResults = new HashSet<>();

        for (final PlaceRelationship relation : relationships) {
            if (relation.getRightPlace() == place && seeds.contains(relation.getLeftPlace())) {
                relationResults.add(relation);
            }
            if (relation.getLeftPlace() == place && seeds.contains(relation.getRightPlace())) {
                relationResults.add(relation);
            }
        }
        return relationResults;
    }

    private Place getPlaceByEdgeWeightSum(final Set<Place> linkedPlaces, final Set<Place> seeds) {
        Set<PlaceRelationship> relationships;
        Place output = new Place();
        Float edgeWeightSum = 0.0f;
        System.out.println(seeds.size());

        for (final Place place : linkedPlaces) {
            Float tempSum = 0.0f;
            relationships = getSeedRelations(place, getPlaceRelationships(place), seeds);
            for (final PlaceRelationship relation : relationships) {
                tempSum = tempSum + Float.parseFloat(relation.getValue());
            }
            if (tempSum > edgeWeightSum) {
                edgeWeightSum = tempSum;
                output = place;
            }
        }
        return output;
    }
}
