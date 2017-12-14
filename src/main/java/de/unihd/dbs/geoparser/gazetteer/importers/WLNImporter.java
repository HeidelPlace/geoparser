package de.unihd.dbs.geoparser.gazetteer.importers;

import com.jcraft.jsch.JSchException;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.models.*;
import de.unihd.dbs.geoparser.gazetteer.types.RelationshipTypes;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

/**
 * Importer class to get data from text files, transform it and write it into the database.
 * @author Fabio Becker
 */
public class WLNImporter implements AutoCloseable {
    private final EntityManager em;
    private final GazetteerPersistenceManager gazetteerPersistenceManager;
    private static GeoparserConfig config;
    public Gazetteer gazetteer;

    public static void main(String[] args) throws Exception {
        config = new GeoparserConfig();
        try(final WLNImporter importer = new WLNImporter()) {
            importer.writeToDataBase();
        }
        //String fileName = "test.tsv";

        /**
         Files.lines(Paths.get(fileName))
         .map(line -> line.split("\t"))
         .map(line -> new Relationship(Long.parseLong(line[0]), Long.parseLong(line[1]), line[2]))
         .forEach(relationship -> {
         em.getTransaction().begin();
         em.persist(relationship);
         em.getTransaction().commit();
         });
         */

    }

    /**
     * Constructor of the WLImporter.
     * Sets up the persistence and entity manager with the configuration given in the config file.
     *
     * @throws Exception thrown if exception is thrown either from the persistence manager or the entity manager
     */
    private WLNImporter() throws Exception {
        gazetteerPersistenceManager = new GazetteerPersistenceManager(config);
        em = gazetteerPersistenceManager.getEntityManager();
    }

    /**
     * Writes the data from text files into the database. First, a new Relationship type is created (if not already existing).
     * Second, all relationships are written into db, considering unique constraint on Place_relationships.
     * TODO: Create UNIQUE CONSTRAINT on place_relationship(left_place_id, right_place_id, type_id)
     *
     * @throws GeoparserConfig.UnknownConfigLabelException .
     * @throws JSchException .
     * @throws IOException .
     */
    private void writeToDataBase() throws GeoparserConfig.UnknownConfigLabelException, JSchException, IOException {
        gazetteer = new Gazetteer(em);
        insertData();

    }

    /**
     *
     * @return
     */
    private void insertData(){
        PlaceRelationshipType type = new PlaceRelationshipType("edge_weight_2",
                "Left-Side place is place_1 and right-side place_2 in WikiLocationNetwork",
                null, null, null);
        System.out.println(type);


        try {
            em.getTransaction().begin();
            em.persist(type);
            em.getTransaction().commit();
        }
        catch (final Exception e){
            System.out.println(e.toString());
            type = (PlaceRelationshipType) gazetteer.getType("edge_weight_2");
        }

        try{
            em.getTransaction().begin();
            em.persist(new PlaceRelationship(gazetteer.getPlace((long)1), gazetteer.getPlace((long)56), type,
                    "0.0012354156345", null, null ));
            em.getTransaction().commit();
        }
        catch (final Exception e){
            System.out.println(e.toString());
        }

    }

    /**
     * Override close function in order to keep this class non-abstract.
     * @throws Exception
     */

    @Override
    public void close() throws Exception {
        try {
            if (em != null && em.isOpen()) {
                em.close();
            }
        }
        finally {
            if (gazetteerPersistenceManager != null) {
                gazetteerPersistenceManager.close();
            }
        }
    }


}