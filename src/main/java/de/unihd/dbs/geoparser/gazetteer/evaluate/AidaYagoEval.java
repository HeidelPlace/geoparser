package de.unihd.dbs.geoparser.gazetteer.evaluate;

import de.unihd.dbs.geoparser.core.Document;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.demo.GeoparsingPipelineFactory;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.util.GeoparserUtil;
import edu.stanford.nlp.pipeline.AnnotationPipeline;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AidaYagoEval {

    private static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.persistence_unit.name";
    private static final String WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.persistence_unit.db_source";

    private static GazetteerPersistenceManager gpm;
    private static Gazetteer gazetteer;
    private static AnnotationPipeline pipeline;

    public static void main(final String args[]) throws Exception {
        AidaYagoEval test = new AidaYagoEval();
        setup();
        //test.testLinkSimpleEntities();
        test.readFiles();
        tearDownAfterClass();
    }


    private static void setup() throws Exception {
        GeoparserConfig config = new GeoparserConfig();
        gpm = new GazetteerPersistenceManager(
                config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_NAME_LABEL),
                config.getDBConnectionInfoByLabel(
                        config.getConfigStringByLabel(WORKING_GAZETTEER_PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)));
        gazetteer = new Gazetteer(gpm.getEntityManager());
        pipeline = GeoparsingPipelineFactory.buildGazetteerDisambiguationPipeline(config, gazetteer);
    }

    private static void tearDownAfterClass() throws Exception {
        try {
            if (gazetteer != null) {
                gazetteer.close();
            }
        } finally {
            gpm.close();
        }
    }

    private List<Integer> testLinkSimpleEntities(final String textSample, final Set<Integer> ids) {
        final Document document = new Document(textSample);
        final Set<Integer> idList = new HashSet<>();
        final Set<Integer> possibleIds = new HashSet<>();
        final int originSize = ids.size();
        int resolvableSize;
        final List<Integer> out = new ArrayList<>(4);

        pipeline.annotate(document);

        final List<LinkedToponym> actualLinkedToponyms = GeoparserUtil.getLinkedToponyms(document);
        final List<ResolvedToponym> resolvedToponyms = GeoparserUtil.getResolvedToponyms(document);

        actualLinkedToponyms.forEach(toponym ->
                possibleIds.add(Integer.parseInt(toponym.gazetteerEntries.iterator().next()
                        .getPropertiesByType("geonamesId").iterator().next().getValue())));

        ids.retainAll(possibleIds);
        resolvableSize = ids.size();

        resolvedToponyms.forEach(place -> {
            if (place.resolvedLocation.gazetteerEntry != null) {
                idList.add(Integer.parseInt(place.resolvedLocation.gazetteerEntry
                        .getPropertiesByType("geonamesId").iterator().next().getValue()));
            }
        });

        //ids.forEach(id -> System.out.println("ID-TEXT:" + id));

        idList.forEach(id -> {
            //System.out.println("ID-RESOLVED:" + id);
            try {
                ids.remove(id);
            }
            catch (Exception ignored){
            }
        });

        out.add(resolvableSize);
        out.add(resolvableSize - ids.size());
        out.add(originSize - resolvableSize);
        out.add(idList.size());

        return out;
    }

    private void readFiles() throws IOException {
        final BufferedReader file = new BufferedReader(new FileReader("C:\\Users\\Fabio Becker\\Intellij\\HeidelPlace\\src\\main\\java\\de\\unihd\\dbs\\geoparser\\gazetteer\\evaluate\\AIDA_documents.txt"));
        int count = 0;
        int totalResolved = 0;
        int resolved = 0;
        int toResolve = 0;
        int notLinked = 0;
        long startTime;
        String read;
        String[] parts;
        String ids;
        StringBuilder document = new StringBuilder();
        double timeSum = 0.0;

        while ((read = file.readLine()) != null) {
            if (read.contains("DOCEND")) {
                List<Integer> idList = new ArrayList<>();
                List<Integer> results;

                if (document.toString().length() < 30) {
                    document = new StringBuilder();
                    System.out.println("SKIP");
                    continue;
                }
                parts = read.split(":");
                ids = parts[2].substring(parts[2].indexOf("[") + 1, parts[2].indexOf("]")).replaceAll(" ", "");
                parts = ids.split(",");

                if (ids.length() > 1) {
                    for (String s : parts) idList.add(Integer.valueOf(s));
                }

                startTime = System.nanoTime();
                results = testLinkSimpleEntities(document.toString(), new HashSet<>(idList));
                timeSum += (System.nanoTime() - startTime);
                toResolve += results.get(0);
                resolved += results.get(1);
                notLinked += results.get(2);
                totalResolved += results.get(3);

                count++;
                System.out.println(results.get(1) + " of " + results.get(0) + " toponyms resolved successfully. " +
                        (results.get(2)) + " not linked for disambiguation.");
                System.out.println("Avg precision (only linked):" + 100.0 / toResolve * resolved + "%" +
                        " [" + resolved + "/" + toResolve + "]");
                System.out.println("Avg precision (linked/unlinked):" + 100.0 / (toResolve + notLinked) * resolved + "%");
                System.out.println("Progress: " + 100.0 / 230.0 * count + "% (" + count + "/230 Documents)");
                System.out.println("Average time per toponym: " + ((timeSum) / 1000000000) / totalResolved + " s");
                document = new StringBuilder();
            } else {
                document.append(read);
            }
        }
    }
}
