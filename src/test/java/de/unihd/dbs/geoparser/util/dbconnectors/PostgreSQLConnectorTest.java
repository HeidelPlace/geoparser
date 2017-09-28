package de.unihd.dbs.geoparser.util.dbconnectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.sql.Connection;
import java.sql.SQLException;

import org.junit.Before;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparserConfig;

public class PostgreSQLConnectorTest {

	private static final String TEST_DATABASE_LABEL = "test.postgres";

	private DBConnectionInfo connInfo;
	private PostgreSQLConnector pgConnector;

	@Before
	public void createPostgreSQLConnector() throws Exception {
		connInfo = new GeoparserConfig().getDBConnectionInfoByLabel(TEST_DATABASE_LABEL);
		pgConnector = new PostgreSQLConnector(connInfo);
	}

	@Test
	public void testGetConnectionAfterConnectShouldBeNotNull() throws IllegalStateException, Exception {
		pgConnector.connect();
		try (final Connection client = pgConnector.getConnection()) {
			assertThat(client, notNullValue());
		}
		finally {
			pgConnector.disconnect();
		}
	}

	@Test
	public void testGetConnectionBeforeConnectShouldBeNull() throws SQLException {
		try (final Connection client = pgConnector.getConnection()) {
			assertThat(client, nullValue());
		}
	}

}