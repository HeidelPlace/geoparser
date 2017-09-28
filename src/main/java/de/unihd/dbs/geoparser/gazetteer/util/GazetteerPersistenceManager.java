package de.unihd.dbs.geoparser.gazetteer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationship;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionData;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionInfo;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHClientSessionFactory;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Manager for the gazetteer persistence stuff.
 * <p>
 * The manager creates a {@link EntityManagerFactory} from information provided by a {@link GeoparserConfig} instance or
 * by manually specified parameters. Optionally, a SSH tunnel is also maintained for connecting to remote databases.
 * <p>
 * Additionally, the manager provides a number of helper routines.
 * <p>
 * Ensure to call {@link #close()} when done with gazetteer persistence related work to ensure allocated resources are
 * released properly.
 * 
 * @author lrichter
 *
 */
public class GazetteerPersistenceManager implements AutoCloseable {

	/**
	 * Possible values for the Hibernate specific property <code>hibernate.hbm2ddl.auto</code>.
	 * 
	 * @author lrichter
	 *
	 */
	public enum HibernateDDLMode {
		CREATE("create"), CREATE_DROP("create-drop"), VALIDATE("validate"), UPDATE("update");

		private final String hibernateCode;

		private HibernateDDLMode(final String name) {
			this.hibernateCode = name;
		}

		public String getHibernateCode() {
			return hibernateCode;
		}
	}

	/**
	 * Default label for the configuration string specifying the persistence unit name.
	 */
	public static final String PERSISTENCE_UNIT_NAME_LABEL = "gazetteer.persistence_unit.name";

	/**
	 * Default label for the configuration string specifying the database source to be used for the persistence unit.
	 */
	public static final String PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL = "gazetteer.persistence_unit.db_source";

	/**
	 * JDBC batch size used for inserting / updating data. If 0, no batching is used. Should be a value between 10-50.
	 * <p>
	 * See https://docs.jboss.org/hibernate/orm/5.1/userguide/html_single/Hibernate_User_Guide.html#batch-jdbcbatch for
	 * more details.
	 */
	public static final Integer JBDC_BATCH_SIZE = 75;

	/**
	 * Number of sequence Ids to allocate in a batch. The larger the number, the less database queries are required
	 * during bulk insertions of entities.
	 * <p>
	 * <b>Note:</b> If few than the allocated Ids are actually used, gaps may occur...
	 */
	public static final int SEQUENCE_ALLOCATION_SIZE = 100;

	private EntityManagerFactory emFactory;
	private Session sshClientSession;

	/**
	 * Create a {@link GazetteerPersistenceManager} instance from the given {@link GeoparserConfig}.
	 * 
	 * @param config the geoparser configuration to be used as source for parameterized connection information
	 * @throws IllegalArgumentException if a configuration string could not be found
	 * @throws JSchException if something went wrong with setting up the SSH tunnel
	 * @throws UnknownConfigLabelException if the configuration string labels are invalid
	 */
	public GazetteerPersistenceManager(final GeoparserConfig config)
			throws IllegalArgumentException, JSchException, UnknownConfigLabelException {
		this(config, null);
	}

	/**
	 * Create a {@link GazetteerPersistenceManager} instance from the given {@link GeoparserConfig}.
	 * 
	 * @param config the geoparser configuration to be used as source for parameterized connection information
	 * @param ddlMode the Hibernate DDL mode to be used. This way, its value can be programmatically determined. If
	 *            null, the original value from the persistence.xml is kept.
	 * @throws IllegalArgumentException if a configuration string could not be found
	 * @throws JSchException if something went wrong with setting up the SSH tunnel
	 * @throws UnknownConfigLabelException if the configuration string labels are invalid
	 */
	public GazetteerPersistenceManager(final GeoparserConfig config, final HibernateDDLMode ddlMode)
			throws IllegalArgumentException, JSchException, UnknownConfigLabelException {
		Objects.requireNonNull(config);

		init(config.getConfigStringByLabel(PERSISTENCE_UNIT_NAME_LABEL), config.getDBConnectionInfoByLabel(
				config.getConfigStringByLabel(PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL)), ddlMode);
	}

	/**
	 * Create a {@link GazetteerPersistenceManager} instance for the given connection information.
	 * 
	 * @param persistenceUnitName name of the persistence unit to be used (from META-INF/persistence.xml)
	 * @param dbConnectionInfo database connection details to be used for setting up the persistence configuration and
	 *            for optionally setting up a SSH tunnel
	 * @throws JSchException if something went wrong with setting up the SSH tunnel
	 */
	public GazetteerPersistenceManager(final String persistenceUnitName, final DBConnectionInfo dbConnectionInfo)
			throws JSchException {
		this(persistenceUnitName, dbConnectionInfo, null);
	}

	/**
	 * Create a {@link GazetteerPersistenceManager} instance for the given connection information.
	 * 
	 * @param persistenceUnitName name of the persistence unit to be used (from META-INF/persistence.xml)
	 * @param dbConnectionInfo database connection details to be used for setting up the persistence configuration and
	 *            for optionally setting up a SSH tunnel
	 * @param ddlMode the Hibernate DDL mode to be used. This way, its value can be programmatically determined. If
	 *            null, the original value from the persistence.xml is kept.
	 * @throws JSchException if something went wrong with setting up the SSH tunnel
	 */
	public GazetteerPersistenceManager(final String persistenceUnitName, final DBConnectionInfo dbConnectionInfo,
			final HibernateDDLMode ddlMode) throws JSchException {
		init(persistenceUnitName, dbConnectionInfo, ddlMode);
	}

	private void init(final String persistenceUnitName, final DBConnectionInfo dbConnectionInfo,
			final HibernateDDLMode ddlMode) throws JSchException {
		Objects.requireNonNull(persistenceUnitName);
		Objects.requireNonNull(dbConnectionInfo);

		final DBConnectionData dbConnectionData = dbConnectionInfo.dbConnectionData;
		final SSHConnectionData sshConnectionData = dbConnectionInfo.sshConnectionData;

		if (sshConnectionData.sshRequired) {
			sshClientSession = SSHClientSessionFactory.getSession(sshConnectionData);
		}

		try {
			final Map<String, String> properties = new HashMap<>();
			properties.put("javax.persistence.jdbc.url", "jdbc:postgresql://" + dbConnectionData.host + ':'
					+ dbConnectionData.port + '/' + dbConnectionData.dbName);
			properties.put("javax.persistence.jdbc.password", new String(dbConnectionData.password));
			properties.put("javax.persistence.jdbc.user", dbConnectionData.userName);

			if (ddlMode != null) {
				properties.put("hibernate.hbm2ddl.auto", ddlMode.getHibernateCode());
			}

			// https://vladmihalcea.com/2015/03/18/how-to-batch-insert-and-update-statements-with-hibernate/
			// Currently the following flags are deactivated, since Hibernate does not handle Batch-insertions correctly
			// when using InheritanceType.JOINED on AbstractEntity)
			// properties.put("hibernate.jdbc.batch_size", GazetteerPersistenceManager.JBDC_BATCH_SIZE.toString());
			// the did improve performance in the long run
			// properties.put("hibernate.order_inserts", "true");
			// properties.put("hibernate.jdbc.batch_versioned_data", "true");

			emFactory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
		}
		catch (final Throwable e) {
			try {
				close();
			}
			catch (final Throwable e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}
	}

	/**
	 * Workaround for Hibernate related problem with \@OneToOne relations annotated with orphanRemoval=true and
	 * optional=false. See https://hibernate.atlassian.net/browse/HHH-6484 for bug-description
	 * 
	 * Deprecated since bug was solved by using OneToMany instead. Kept for reference, though.
	 * 
	 * @param entityManager the entity manager session used for persistence management
	 * @param entity the entity from which to remove the provenance
	 */
	@Deprecated
	public static void removeProvenanceFromEntity(final EntityManager entityManager, final AbstractEntity entity) {
		if (entity.getProvenance() != null) {
			entityManager.remove(entity.getProvenance());
		}
		entity.setProvenance(null);
		entityManager.flush();
	}

	/**
	 * Remove the given type from the gazetteer, unlinking it from associated types beforehand.
	 * 
	 * @param entityManager the entity manager used for persistence management
	 * @param type the type to be removed
	 */
	public static void removeType(final EntityManager entityManager, final Type type) {
		type.setParentType(null);
		type.setChildTypes(null);
		type.setSimilarTypes(null);
		entityManager.remove(type);
	}

	/**
	 * Remove the given place relationship from the gazetteer, unlinking it from associated places beforehand.
	 * 
	 * @param entityManager the entity manager used for persistence management
	 * @param relationship the place relationship to be removed
	 */
	public static void removePlaceRelationship(final EntityManager entityManager,
			final PlaceRelationship relationship) {
		relationship.setLeftPlace(null);
		relationship.setRightPlace(null);
		entityManager.remove(relationship);
	}

	/**
	 * Return a new {@link EntityManager}.
	 * <p>
	 * This is the standard entrance point for persistence related work...
	 * 
	 * @return a {@link EntityManager} associated with the {@link EntityManagerFactory} maintained by the manager
	 */
	public EntityManager getEntityManager() {
		return emFactory.createEntityManager();
	}

	/**
	 * Return the {@link EntityManagerFactory}.
	 * <p>
	 * Use this for setting up e.g., a NamedQuery or for dealing with L2-Cache...
	 * 
	 * @return the {@link EntityManagerFactory} maintained by the manager
	 */
	public EntityManagerFactory getEntityManagerFactory() {
		return emFactory;
	}

	/**
	 * Close the manager by releasing the maintained {@link EntityManagerFactory} and an optional SSH tunnel.
	 */
	@Override
	public void close() {
		try {
			if (emFactory != null) {
				emFactory.close();
			}
		}
		finally {
			if (sshClientSession != null) {
				sshClientSession.disconnect();
			}
		}
	}

}