package de.unihd.dbs.geoparser.util.dbconnectors;

import static org.junit.Assert.*;
import static org.hamcrest.CoreMatchers.*;

import org.junit.Before;
import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class DBConnectionDataTest {

	private static final String dbName = "db_name";
	private static final String dbHost = "db_host";
	private static final String dbUserName = "db_username";
	private static final char[] dbPassword = "db_password".toCharArray();
	private static final int dbPort = 1234;
	private static final boolean dbAuthRequired = true;

	private DBConnectionData dbConnectionData;

	@Before
	public void createDBConnectionData() {
		dbConnectionData = new DBConnectionData(dbName, dbHost, dbPort, dbUserName, dbPassword, dbAuthRequired);
	}

	@Test
	public void testEqualsContractShouldHold() {
		EqualsVerifier.forClass(DBConnectionData.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testDBConnectionDataConstructorShouldSetFieldsCorrectly() {
		assertThat(dbConnectionData.dbName, equalTo(dbName));
		assertThat(dbConnectionData.host, equalTo(dbHost));
		assertThat(dbConnectionData.userName, equalTo(dbUserName));
		assertThat(dbConnectionData.password, equalTo(dbPassword));
		assertThat(dbConnectionData.port, equalTo(dbPort));
		assertThat(dbConnectionData.authenticationRequired, equalTo(dbAuthRequired));
	}

	@Test
	public void testDBConnectionDataConstructorShouldSetNullPasswordCorrectly() {
		dbConnectionData = new DBConnectionData(dbName, dbHost, dbPort, dbUserName, null, dbAuthRequired);
		assertThat(dbConnectionData.password, nullValue());
	}

	@Test
	public void testToStringShouldWork() {
		// perform a simple smoke test
		dbConnectionData.toString();
	}

}
