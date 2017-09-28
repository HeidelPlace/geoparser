package de.unihd.dbs.geoparser.util.dbconnectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparserConfig;

import com.mongodb.MongoClient;

public class MongoDBConnectorTest {

	private static final String TEST_DATABASE_LABEL = "test.mongodb";

	private DBConnectionInfo connInfo;
	private MongoDBConnector mongoDbConnector;

	@Before
	public void createMongoDBConnector() throws Exception {
		connInfo = new GeoparserConfig().getDBConnectionInfoByLabel(TEST_DATABASE_LABEL);

		mongoDbConnector = new MongoDBConnector(connInfo);
	}

	@Test
	public void testGetConnectionWithAuthenticationShouldSucceed() throws IllegalStateException, Exception {
		mongoDbConnector.connect();
		try (final MongoClient client = mongoDbConnector.getConnection()) {
			// we need this step make sure we connect to MongoDB due to Lazy Loading
			final String actualHost = client.getAddress().getHost();
			assertThat(client, notNullValue());
			assertThat(actualHost, equalTo(connInfo.dbConnectionData.host));
		}
		finally {
			mongoDbConnector.disconnect();
		}
	}

	@Test
	public void testGetConnectionWithoutAuthenticationShouldSucceed() throws IllegalStateException, Exception {
		mongoDbConnector.dbConnectionInfo.dbConnectionData.authenticationRequired = false;
		mongoDbConnector.connect();
		try (final MongoClient client = mongoDbConnector.getConnection()) {
			// we need this step make sure we connect to MongoDB due to Lazy Loading
			final String actualHost = client.getAddress().getHost();
			assertThat(client, notNullValue());
			assertThat(actualHost, equalTo(connInfo.dbConnectionData.host));
		}
		finally {
			mongoDbConnector.disconnect();
		}
	}

	@Test
	public void testGetConnectionBeforeConnectShouldBeNull() {
		try (final MongoClient client = mongoDbConnector.getConnection()) {
			assertNull(client);
		}
	}

}
