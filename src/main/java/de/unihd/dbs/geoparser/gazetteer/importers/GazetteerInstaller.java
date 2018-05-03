package de.unihd.dbs.geoparser.gazetteer.importers;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jcraft.jsch.JSchException;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager.HibernateDDLMode;

/**
 * Installer for setting up the gazetteer database.
 * <p>
 * Configure <code>/GeoParser/src/main/	resources/geoparser.config.json</code> before use.
 * <p>
 * Documentation for speeding up bulk insertions: https://www.postgresql.org/docs/current/static/populate.html
 *
 * @author lrichter
 *
 */
public class GazetteerInstaller implements AutoCloseable {

	private static final Logger logger = LoggerFactory.getLogger(GazetteerInstaller.class);

	private static GeoparserConfig config; // access to configuration file

	private final GazetteerPersistenceManager gazetteerPersistenceManager; // connection to our gazetteer database
	private final EntityManager em; // current JPA session

	public static void main(final String[] args) throws IllegalArgumentException, JSchException, Exception {
		logger.debug("Loading configuration");
		config = new GeoparserConfig();

		// remove any existing data and (re-)create the database schema
		createTableSchema(config);

		try (final GazetteerInstaller installer = new GazetteerInstaller()) {
			installer.dropAllEntityForeignKeyConstraints();
			installer.importTypes();
			installer.importGeoNamesData();
		}

		// restore foreign key-constraints etc.
		updateTableSchema(config);
	}

	public static void createTableSchema(final GeoparserConfig config)
			throws IllegalArgumentException, JSchException, UnknownConfigLabelException {
		logger.info("Creating table schema");
		try (final GazetteerPersistenceManager gazetteerPersistenceManager = new GazetteerPersistenceManager(config,
				HibernateDDLMode.CREATE)) {
			// no-op
		}
	}

	public static void updateTableSchema(final GeoparserConfig config)
			throws IllegalArgumentException, JSchException, UnknownConfigLabelException {
		logger.info("Updating table schema to re-create foreign key constraints. This may take some time...");
		try (final GazetteerPersistenceManager gazetteerPersistenceManager = new GazetteerPersistenceManager(config,
				HibernateDDLMode.UPDATE)) {
			ensureExtraIndicesExist(gazetteerPersistenceManager.getEntityManager());
		}
	}

	/**
	 * Create a {@link GazetteerInstaller} and initialize all required resources.
	 *
	 * @throws Exception if something went wrong while initializing the required resources
	 */
	public GazetteerInstaller() throws Exception {
		logger.debug("Firing up PersistenceManager");

		gazetteerPersistenceManager = new GazetteerPersistenceManager(config);
		em = gazetteerPersistenceManager.getEntityManager();
	}

	@Override
	public void close() throws Exception {
		logger.debug("Tearing down PersistenceManager");
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

	private void importTypes() {
		try (final TypeImporter importer = new TypeImporter(em)) {
			importer.importTypes();
		}
	}

	private void importGeoNamesData() throws Exception {
		try (final GeoNamesImporter importer = new GeoNamesImporter(config, em)) {
			importer.loadTypes();
			importer.importPlaces();
			importer.linkPlaces();

			// Do this only when completely done with importing and merging since valuable data might be lost afterwards
			// importer.deletePropertyTypesSpecficForGeonamesImport();
		}
	}

	/**
	 * Drop all foreign key constraints on all entity tables to speed up bulk insertions.
	 * <p>
	 * Rational behind this: https://www.postgresql.org/docs/current/static/populate.html#POPULATE-RM-FKEYS
	 *
	 * entityManager the entity manager session used for persistence management
	 */
	private void dropAllEntityForeignKeyConstraints() {
		logger.info("Dropping all foreign key constraints.");
		// XXX: dropping constraints this way strongly depends on the naming of our model. no so maintainable...
		em.getTransaction().begin();
		em.createNativeQuery(
		//@formatter:off
				"ALTER TABLE footprint DROP CONSTRAINT IF EXISTS footprint_place_fk;" +
				"ALTER TABLE footprint DROP CONSTRAINT IF EXISTS footprint_entity_id_fk;" +
				"ALTER TABLE place_name DROP CONSTRAINT IF EXISTS place_name_place_fk;" +
				"ALTER TABLE place_name DROP CONSTRAINT IF EXISTS place_name_entity_id_fk;" +
				"ALTER TABLE place_property DROP CONSTRAINT IF EXISTS place_property_place_fk;" +
				"ALTER TABLE place_property DROP CONSTRAINT IF EXISTS place_property_place_property_type_fk;" +
				"ALTER TABLE place_property DROP CONSTRAINT IF EXISTS property_entity_fk;" +
				"ALTER TABLE place_relationship DROP CONSTRAINT IF EXISTS place_relationship_place_1_fk;" +
				"ALTER TABLE place_relationship DROP CONSTRAINT IF EXISTS place_relationship_place_2_fk;" +
				"ALTER TABLE place_relationship DROP CONSTRAINT IF EXISTS place_relationship_place_relationship_type_fk;" +
				"ALTER TABLE place_relationship DROP CONSTRAINT IF EXISTS place_relationship_entity_id_fk;" +
				"ALTER TABLE place_type_assignment DROP CONSTRAINT IF EXISTS type_assignment_place_fk;" +
				"ALTER TABLE place_type_assignment DROP CONSTRAINT IF EXISTS type_assignment_type_fk;" +
				"ALTER TABLE place_type_assignment DROP CONSTRAINT IF EXISTS place_type_assignment_entity_id_fk;" +
				"ALTER TABLE provenance DROP CONSTRAINT IF EXISTS provenance_entity_fk;" +
				"ALTER TABLE place DROP CONSTRAINT IF EXISTS place_entity_id_fk;"
			  //@formatter:on
		).executeUpdate();
		em.getTransaction().commit();
	}

	/**
	 * Ensure that a number of indices are set up to speed up the gazetteer query performance.
	 *
	 * @param em the entity manager session used for persistence management
	 */
	public static void ensureExtraIndicesExist(final EntityManager em) {
		logger.info("Creating a number of indexes. This may take a while...");
		// XXX: creating indexes this way strongly depends on the naming of our model. no so maintainable...
		// Sample code to set up indices via JPA:
		// @Index(unique="true", name="idx")
		// @Indices({
		// @Index(members={"lastName","firstName"})
		// @Index(members={"firstName"}, unique="true")
		// }

		// indexes for fast entity match by place_id
		ensureIndexExists(em, "place_name_place_fk_idx", "ON place_name(place_id)");
		ensureIndexExists(em, "footprint_place_fk_idx", "ON footprint(place_id)");
		ensureIndexExists(em, "place_property_place_fk_idx", "ON place_property(place_id)");
		ensureIndexExists(em, "place_type_assignment_place_fk_idx", "ON place_type_assignment(place_id)");
		ensureIndexExists(em, "place_relationship_left_place_fk_idx", "ON place_relationship(left_place_id)");
		ensureIndexExists(em, "place_relationship_lright_place_fk_idx", "ON place_relationship(right_place_id)");

		// index for fast entity match by id
		ensureIndexExists(em, "place_entity_id_fk_idx", "ON place(id)");
		ensureIndexExists(em, "place_name_entity_id_fk_idx", "ON place_name(id)");
		ensureIndexExists(em, "footprint_entity_id_fk_idx", "ON footprint(id)");
		ensureIndexExists(em, "place_property_entity_id_fk_idx", "ON place_property(id)");
		ensureIndexExists(em, "place_type_assignment_entity_id_fk_idx", "ON place_type_assignment(id)");
		ensureIndexExists(em, "place_relationship_entity_id_fk_idx", "ON place_relationship(id)");
		ensureIndexExists(em, "provenance_entity_fk_idx", "ON provenance(entity_id)");

		// indexes for fast search filters
		ensureIndexExists(em, "place_name_name_pattern_idx", "ON place_name(name text_pattern_ops)");
		ensureIndexExists(em, "place_name_lower_name_idx", "ON place_name(lower(name))");
		ensureIndexExists(em, "place_property_place_id_type_id_idx",
				"ON place_property USING btree(place_id, type_id)");
		ensureIndexExists(em, "place_property_type_id_idx", "ON place_property(type_id)");
		ensureIndexExists(em, "place_type_assignment_place_id_type_id_idx",
				"ON place_type_assignment(place_id, type_id)");
		ensureIndexExists(em, "footprint_geom_idx", "ON footprint USING gist(geom)");
	}

	public static void ensureIndexExists(final EntityManager em, final String indexName, final String indexSQL) {
		// http://dba.stackexchange.com/questions/35616/create-index-if-it-does-not-exist
		em.getTransaction().begin();
		em.createNativeQuery(
		// @formatter:off
				"DO $$ " +
				"BEGIN " +
				"IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace " +
				"	WHERE c.relname = '" + indexName + "' AND n.nspname = 'public') THEN " +
				"    CREATE INDEX " + indexName + " " + indexSQL + "; " + "END IF; " + "END$$; ")
				// @formatter:on
				.executeUpdate();
		em.getTransaction().commit();
	}

}
