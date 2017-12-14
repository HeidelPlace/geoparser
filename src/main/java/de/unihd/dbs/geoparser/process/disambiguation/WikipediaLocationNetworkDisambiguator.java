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
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CollectionUtils;
import edu.stanford.nlp.util.CoreMap;

import java.math.BigInteger;
import java.util.*;

/**
 * Implementation of {@link ToponymDisambiguator} that disambiguates toponyms by the edges of the Wikipedia-Location-Network.
 * The edge between two toponyms in a sentence with the highest value predicts the most likely match for the linked toponyms.
 *
 * @author fbecker
 *
 */
public class WikipediaLocationNetworkDisambiguator extends ToponymDisambiguator {
    private final Gazetteer gazetteer;
    private List<Place> unambiguousPlaces = new ArrayList<>();
    private Map<Long, List<Double>> seeds = new HashMap<>();
    private static final Double WEIGHT_THRESHOLD = 1.0;
    private int count_weight = 0;
    private int count_first = 0;

    public WikipediaLocationNetworkDisambiguator(final Gazetteer gazetteer){
        super();
        this.gazetteer = gazetteer;


    }

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
        double startTime = System.nanoTime();
        final List<ResolvedLocation> output = new ArrayList<>(namedEntities.size());
        final List<Place> allLinkedPlaces = new ArrayList<>();
        final List<Long> allLinkedPlacesIds = new ArrayList<>();

        namedEntities.forEach(entity -> {
            try {
                allLinkedPlaces.addAll(Objects.requireNonNull(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class)));
            }
            catch (NullPointerException ignored){
            }
        });

        if(allLinkedPlaces.isEmpty()){
            output.add(null);
            return output;
        }

        allLinkedPlaces.forEach(place -> allLinkedPlacesIds.add(place.getId()));
        final Set<Long> idsInWLN = getIdsInWLN(allLinkedPlacesIds);

        namedEntities.forEach(entity -> {
            try {
                getSeed(entity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class), allLinkedPlacesIds, idsInWLN);
            }
            catch (NullPointerException ignored){
            }
        });


        System.out.println(idsInWLN.size());

        for (final CoreMap namedEntity : namedEntities) {
            List<Place> linkedPlaces;
            Place resolvedLocation = null;

            try {
                linkedPlaces = new ArrayList<>(namedEntity.get(GeoparsingAnnotations.GazetteerEntriesAnnotation.class));
            } catch (NullPointerException error) {
                continue;
            }

            if (!Objects.equals(namedEntity.get(CoreAnnotations.NamedEntityTagAnnotation.class),
                    NamedEntityType.LOCATION.name) || linkedPlaces.isEmpty()) {
                output.add(null);
                continue;
            }

            if (!Collections.disjoint(unambiguousPlaces, linkedPlaces)) {
                List<Place> getIntersection = new ArrayList<>(unambiguousPlaces);
                getIntersection.retainAll(linkedPlaces);
                assert(getIntersection.size() == 1);
                resolvedLocation = getIntersection.get(0);
            }

            else if(seeds.size() > 1){
                resolvedLocation = getResolvedPlace(new HashSet<>(linkedPlaces));
            }

            if (resolvedLocation == null) {
                resolvedLocation = new ArrayList<>(linkedPlaces).get(0);
                count_first ++;

                if (resolvedLocation == null){
                    resolvedLocation = HighestAdminLevelDisambiguator.getPlaceWithHighestAdminLevel(new ArrayList<>(linkedPlaces));
                    //System.out.println("NO SEED RELATION - FIRST ITEM SELECTED");
                }
            }

            try{
                output.add(new ResolvedLocation(resolvedLocation));
            }
            catch (Exception e ){
                System.out.println(e.getMessage());
                output.add(null);
            }
        }
        System.out.println("DISAMBIGUATION TIME: " + (System.nanoTime() - startTime)/1000000 + "ms");
        seeds.clear();
        unambiguousPlaces.clear();
        System.out.println("WEIGHT RESOLVED: " + count_weight);
        System.out.println("FIRST RESOLVED: " + count_first);
        return output;
    }



    private Place getResolvedPlace(final Set<Place> linkedPlaces) {
        if (seeds.isEmpty()){
            System.out.println("NO SEEEDS");
            return null;
        }

        return getPlaceByEdgeWeightSum(linkedPlaces);
    }



    private Set<Long> getIdsInWLN(final List<Long> allLinkedIds) {
        final Set<Long> occurredIds = new HashSet<>();

        final List<Object> checkSeedList = gazetteer.getEntityManger().createNativeQuery("SELECT DISTINCT * FROM " +
                "(SELECT DISTINCT left_place_id FROM place_relationship WHERE left_place_id IN :ids AND type_id = 33 UNION ALL " +
                "SELECT DISTINCT right_place_id FROM place_relationship WHERE right_place_id IN :ids AND type_id = 33 " +
                "GROUP BY left_place_id, right_place_id HAVING count(left_place_id)>1 OR count(right_place_id)>1) AS ids").setParameter("ids", allLinkedIds)
                .getResultList();

        checkSeedList.forEach(id -> occurredIds.add(((BigInteger) id).longValue()));
        return occurredIds;
    }



    private void getSeed(final List<Place> linkedPlaces, final List<Long> allLinkedPlaces, final Set<Long> idsInWLN) {
        Place seedPlace;
        Set<Long> linkedIds = new HashSet<>(linkedPlaces.size());

        linkedPlaces.forEach(place -> linkedIds.add(place.getId()));

        final List<Long> intersection = new ArrayList<>(CollectionUtils.intersection(linkedIds, idsInWLN));
        if (intersection.size() == 1){
           seedPlace = gazetteer.getPlace(intersection.get(0));
        }
        else{
            return;
        }

        final Long id = seedPlace.getId();
        List<Object[]> queryList = gazetteer.getEntityManger().createNativeQuery("SELECT value, left_place_id, right_place_id " +
                "FROM place_relationship WHERE (left_place_id = CAST((:id_1) AS BIGINT) OR right_place_id = CAST((:id_2) AS BIGINT))" +
                " AND type_id = cast(33 AS BIGINT)").setParameter("id_1", id).setParameter("id_2", id).getResultList();

        queryList.forEach(objects -> {
            List<Double> tmpList;
            if (objects[1] == id && allLinkedPlaces.contains(toLong(objects[2]))) {
                tmpList = seeds.computeIfAbsent(toLong(objects[2]), aLong ->  new ArrayList<Double>(){{
                    add(strToDouble(objects[0]));
                }});
                if (tmpList.size() > 1 || seeds.get(toLong(objects[2])).get(0).equals(strToDouble(objects[0]))){
                    tmpList.add(strToDouble(objects[0]));
                }
            } else {
                tmpList = seeds.computeIfAbsent(toLong(objects[1]), aLong ->  new ArrayList<Double>(){{
                    add(strToDouble(objects[0]));
                }});
                if (tmpList.size() > 1 || seeds.get(toLong(objects[1])).get(0).equals(strToDouble(objects[0]))){
                    tmpList.add(strToDouble(objects[0]));
                }
            }
        });
        unambiguousPlaces.add(seedPlace);
        count_weight ++;
    }


    private Long toLong(final Object object){
        return ((BigInteger) object).longValue();
    }

    private Double strToDouble(final Object object){
        return Double.parseDouble((String) object);
    }


    private double getSeedRelations(final Place place) {
        double sum = 0.0;
        List<Double> seedWeights = seeds.get(place.getId());

        if(seedWeights != null){
            for (double aDouble : seedWeights) {
                sum += aDouble;
            }
        }
        return sum;
    }



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

        if (output != null) count_weight++;
        return output;
    }
}
