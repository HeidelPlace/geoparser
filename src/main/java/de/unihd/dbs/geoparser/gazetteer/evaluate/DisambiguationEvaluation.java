package de.unihd.dbs.geoparser.gazetteer.evaluate;

import com.vividsolutions.jts.geom.Coordinate;
import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.process.util.Haversine;
import de.unihd.dbs.geoparser.util.GeoparserUtil;
import edu.stanford.nlp.pipeline.AnnotationPipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implementation of a naive evaluation method using the 2003 shared task test-b document (AIDA_documents_coords.txt)
 * annotated with Wikidata coordinates and identifiers.
 * <p>
 * Note that this implementation is not implemented well. Tis is only a trivial evaluation example to get an overview
 * of a module's performance.
 *
 * @author Fabio Becker
 */
public class DisambiguationEvaluation {

    private static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.persistence_unit.name";
    private static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.persistence_unit.db_source";

    private static GazetteerPersistenceManager gpm;
    private static Gazetteer gazetteer;
    private static AnnotationPipeline pipeline;
    // Select the Disambiguation module you want to evaluate.
    private static String DISAMBIGUATOR = "PDWD";
    private Set<Integer> wrongFootprints = new HashSet<>();

    /**
     * Main function starting the evaluation process.
     *
     * @param args arguments.
     * @throws Exception any exception.
     */
    public static void main(final String args[]) throws Exception {
        DisambiguationEvaluation test = new DisambiguationEvaluation();
        setup();
        //test.getCoordinateMap();
        test.readFiles();
        tearDownAfterClass();
    }

    /**
     * Sets up all components of the framework needed to use the geoparser.
     *
     * @throws Exception any exception.
     */
    private static void setup() throws Exception {
        GeoparserConfig config = new GeoparserConfig();
        gpm = new GazetteerPersistenceManager(
                config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL),
                config.getDBConnectionInfoByLabel(
                        config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
        gazetteer = new Gazetteer(gpm.getEntityManager());
        pipeline = GeoparsingPipelineFactory.buildGazetteerDisambiguationPipeline(config, gazetteer, DISAMBIGUATOR);
    }

    /**
     * Tears down the gazetteer and closes the persistence manager.
     *
     * @throws Exception any exception.
     */
    private static void tearDownAfterClass() throws Exception {
        try {
            if (gazetteer != null) {
                gazetteer.close();
            }
        } finally {
            gpm.close();
        }
    }

    /**
     * Retrieves the coordinates from the gazetteer for all resolved toponyms.
     *
     * @param textSample input text document.
     * @param wikiIds    Wikidata identifiers from the annotated text document.
     * @return map containing identifiers of resolved places and the according coordinates.
     */
    private Map<Integer, Coordinate> getCoordinateMap(final String textSample, final Map<Integer, Integer> wikiIds) {
        final Document document = new Document(textSample);
        final Map<Integer, Coordinate> coordinateMap = new HashMap<>();
        pipeline.annotate(document);

        final List<ResolvedToponym> resolvedToponyms = GeoparserUtil.getResolvedToponyms(document);

        resolvedToponyms.forEach(place -> {
            if (place.resolvedLocation.gazetteerEntry != null) {
                try {
                    coordinateMap.put(place.beginPosition - 1,
                            place.resolvedLocation.gazetteerEntry.getFootprints().iterator().next().getGeometry().getCoordinate());

                    // check if wiki ids match
                    if (wikiIds.get(place.beginPosition - 1) ==
                            Long.parseLong(place.resolvedLocation.gazetteerEntry.getPropertiesByType("wikidataId")
                                    .iterator().next().getValue().substring(1))) {
                        wrongFootprints.add(place.beginPosition - 1);
                    }
                } catch (Exception ignored) {

                }
            }
        });

        return coordinateMap;
    }

    /**
     * Reads the evaluation file and slices it to extract the annotated information.
     * Note that this is not implemented very well and might be quite confusing.
     * This is supposed to provide a simple method to evaluate implemented modules.
     * Further evaluation scripts are highly welcome.
     *
     * @throws IOException exception thrown if problem with input file occurs.
     */
    private void readFiles() throws IOException {
        final BufferedReader file = new BufferedReader(new FileReader(
                "src\\main\\java\\de\\unihd\\dbs\\geoparser\\gazetteer\\evaluate\\AIDA_documents_coords.txt"));
        PrintWriter writer = new PrintWriter("src\\main\\java\\de\\unihd\\dbs\\geoparser\\" +
                "\\gazetteer\\evaluate\\dumps\\" + DISAMBIGUATOR + "\\" +
                LocalDateTime.now().getDayOfMonth() + "_" + LocalDateTime.now().getMonthValue() + "_"
                + LocalDateTime.now().getHour() + LocalDateTime.now().getMinute() + "_eval_dump_" + DISAMBIGUATOR + ".txt");

        List<Double> long_lat = new ArrayList<>(2);
        String read;
        List<Double> meanDistance = new ArrayList<>();
        StringBuilder document = new StringBuilder();
        Map<Integer, Coordinate> coords = new HashMap<>();
        Map<Integer, Integer> wikiIds = new HashMap<>();
        List<Integer> idList = new ArrayList<>();
        int last_size = 0;
        int doc_number = 0;
        int documentToponyms = 0;

        while ((read = file.readLine()) != null) {
            if (read.contains("DOCSTART")) {
                if (doc_number < 230) {
                    doc_number = Integer.parseInt(read.substring(read.indexOf("testb") - 4, read.indexOf("testb"))) - 1163;
                }

                Map<Integer, Coordinate> resolvedMap;


                if (document.toString().length() < 30) {
                    document = new StringBuilder();
                    System.out.println("SKIP");
                    continue;
                }

                resolvedMap = getCoordinateMap(document.toString(), wikiIds);

                resolvedMap.forEach((integer, coordinate) -> {
                    try {
                        if (wrongFootprints.contains(integer)) {
                            meanDistance.add(0.0);
                            writer.println(Haversine.distance(coords.get(integer).y, coords.get(integer).x, coordinate.y, coordinate.x) + "\t" + 1);
                        } else {
                            meanDistance.add(Haversine.distance(coords.get(integer).y, coords.get(integer).x, coordinate.y, coordinate.x));
                            writer.println(Haversine.distance(coords.get(integer).y, coords.get(integer).x, coordinate.y, coordinate.x));
                        }
                        //System.out.println(meanDistance.get(meanDistance.size()-1) + " " + coords.get(integer).y + " " + coordinate.x);
                    } catch (Exception ignored) {
                    }
                });

                documentToponyms = documentToponyms + coords.size();
                System.out.println("RESOLVED: " + (meanDistance.size() - last_size) + "/" + idList.size());
                System.out.println("MEAN DISTANCE: " + (meanDistance.stream().mapToDouble(Double::doubleValue).sum())
                        / meanDistance.size());
                System.out.println(doc_number + "/230 -- " + ((100.0 / 230) * doc_number) + "%");
                System.out.println("TOTAL RECOGNIZED TOPONYMS: " + meanDistance.size() + "/" + documentToponyms);

                document = new StringBuilder();
                coords.clear();
                idList.clear();
                wikiIds.clear();
                wrongFootprints.clear();
                last_size = meanDistance.size();

            } else {
                if (read.contains("-[-")) {
                    // retrieve coordinates from text
                    long_lat.add(0, Double.parseDouble(read.substring(read.indexOf("-[-") + 3, read.indexOf("-]-")).split("/")[0]));
                    long_lat.add(1, Double.parseDouble(read.substring(read.indexOf("-[-") + 3, read.indexOf("-]-")).split("/")[1]));
                    // save coordinates to map
                    coords.put(document.length() + read.indexOf("-]-") + 4, new Coordinate(long_lat.get(0), long_lat.get(1)));
                    // store wikidata ids
                    wikiIds.put(document.length() + read.indexOf("-]-") + 4, Integer.parseInt(read.substring(read.indexOf("-[-") + 3,
                            read.indexOf("-]-")).split("/")[2]));
                    // store position of annotated location from text
                    idList.add(document.length() + read.indexOf("-]-") + 4);
                    long_lat.clear();
                }
                if (read.contains("-[-+")) {
                    // retrieve coordinates from text
                    long_lat.add(0, Double.parseDouble(read.substring(read.indexOf("-[-+") + 4, read.indexOf("+-]-")).split("/")[0]));
                    long_lat.add(1, Double.parseDouble(read.substring(read.indexOf("-[-+") + 4, read.indexOf("+-]-")).split("/")[1]));
                    // save coordinates to map
                    coords.put(document.length() + read.indexOf("+-]-") + 5, new Coordinate(long_lat.get(0), long_lat.get(1)));
                    // store wikidata ids
                    wikiIds.put(document.length() + read.indexOf("+-]-") + 5, Integer.parseInt(read.substring(read.indexOf("-[-+") + 4,
                            read.indexOf("+-]-")).split("/")[2]));
                    // store position of annotated location from text
                    idList.add(document.length() + read.indexOf("+-]-") + 5);
                    long_lat.clear();
                }
                document.append(read);
            }
        }
        writer.close();
    }
}
