package de.unihd.dbs.geoparser.gazetteer.importers;

import java.sql.*;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO: Extract all relevant relationships from WLN .txt file and COPY them into place_relationship.
 * TODO: Before that the same number of rows has to be inserted in table 'entity' in order to assign the relations.
 * TODO: Add new relationtype 33 to types. i.e. edge_weight_relation.
 */
public class WikipediaLocationNetworkImporter {
    private static final Logger logger = LoggerFactory.getLogger(WikipediaLocationNetworkImporter.class);
    private static Connection c = null;

    public static void main(String args[]) throws SQLException {
        WikipediaLocationNetworkImporter importer = new WikipediaLocationNetworkImporter();
        importer.connectToDatabase();
        importer.updateEntities();
        c.close();
    }

    /**
     * Creates a temporary table 'edge_weight'.
     * @throws SQLException if table 'edge_weight' already exists
     */
    private void createTable() throws SQLException {
        Statement stmt;
        try {
            stmt = c.createStatement();
            String sql = "CREATE TABLE edge_weight " +
                    "(place_1 BIGINT NOT NULL," +
                    " place_2 BIGINT NOT NULL," +
                    " value DOUBLE PRECISION NOT NULL)";
            stmt.executeUpdate(sql);
            stmt.close();
        }
        catch (Exception e){
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            c.rollback();
            System.exit(0);
        }
        logger.info("-- TEMP TABLE CREATED --");
    }

    /**
     * Fills the temporary table ('edge_weight') with relevant relationships between two locations of the WLN.
     * The data written into the temporary table is extracted from the WLN .txt file.
     * Therefore it might be the best way to create a temporary .txt file containing only relevant relationships.
     * This file can be copied into the new temp table or might as well be copied directly into place_relationship.
     * @throws SQLException
     */
    private void fillTable() throws SQLException {

        try {
            PreparedStatement stmt = c.prepareStatement(
                    "INSERT INTO edge_weight(place_1, place_2, value) " +
                        "SELECT place_1, place_2, value FROM edge_weight_2 " +
                        "INNER JOIN place ON place_2 = place.id");

            logger.info("-- RUNNING QUERY --");

            stmt.executeUpdate();
            stmt.close();

        } catch (Exception e) {
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            c.rollback();
            System.exit(0);
        }
        logger.info("-- TEMP TABLE FILLED SUCCESSFULLY --");
    }

    /**
     * Inserts new entities in entity table. Number of insertions is given by the size of temp table 'edge_weight'.
     * Due to a sequence bug a workaround is used. First, the last value of the entity table is queried.
     * Second, entities are inserted and the last_value of the sequence is increased by 1.
     *
     * 40 values in one sql statement in order to get faster queries.
     *
     * @throws SQLException if entity_id_sequence does not exist
     */
    private void updateEntities() throws SQLException {

        try {
            logger.info("-- RUNNING QUERIES --");
            int j =5; //getTableSize();
            PreparedStatement stmt;
            Statement stmt_2;

            c.setAutoCommit(false);

            // Set sequence to maximum value of entity ids (just to be sure).

            stmt_2 = c.createStatement();
            String sql_2 = "SELECT setval('entity_id_sequence', ( SELECT  max(entity.id) from entity));";
            stmt_2.execute(sql_2);
            stmt_2.close();
            c.commit();


        } catch (Exception e) {
            System.err.println( e.getClass().getName()+": "+ e.getMessage() );
            c.rollback();
            System.exit(0);
        }
        logger.info("-- ENTITY INSERTION DONE --");
    }

    /**
     * Sets the last value of 'entity_id_sequence' to the maximum value of entity ids.
     * @throws SQLException
     */
    private void setSequenceLastVal() throws SQLException {
        Statement stmt_2;

        stmt_2 = c.createStatement();
        String sql = "SELECT setval('entity_id_sequence', ( SELECT  max(entity.id) from entity)); ";
        stmt_2.execute(sql);
        stmt_2.close();
        c.commit();
    }


    /**
     * Queries the size of the temporary table and returns it.
     * @return table size as int
     * @throws SQLException if table 'edge_weight' does not exist
     */
    private int getTableSize() throws SQLException {
        logger.info("-- FETCHING TABLE SIZE --");

        int out = 0;
        PreparedStatement stmt;

        stmt = c.prepareStatement("SELECT count(*) AS a FROM edge_weight;");

        ResultSet result = stmt.executeQuery();

        while (result.next()){
            out = out + result.getInt("a");
        }

        stmt.close();

        logger.info(out + " ROWS FOUND");

        return out;
    }

    /**
     * Sets up the connection to the given database.
     * Database is hardcoded atm. Can be drawn out of config file.
     * @throws SQLException if connection to database fails
     */
    private void connectToDatabase() throws SQLException {
        try {
            Class.forName("org.postgresql.Driver");
            c = DriverManager
                    .getConnection("jdbc:postgresql://localhost:5432/gazetteer",
                            "postgres", "admin");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println(e.getClass().getName()+": "+e.getMessage());
            System.exit(0);
        }
        logger.info("-- CONNECTION TO DB SUCCESSFUL --");
    }
}
