package de.unihd.dbs.geoparser.core;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;

import org.junit.Before;
import org.junit.Test;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionData;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionInfo;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule.SSHPortForwardingDirection;

/**
 * Test Cases for GeoParserConfig class.
 * 
 * @author lrichter
 *
 */
public class GeoparserConfigTest {

	private static final String TEST_FULL_DATABASE_INFO_LABEL = "test.geoparser_config.full_dbinfo";
	private static final String TEST_MININFO_DATABASE_INFO_LABEL = "test.geoparser_config.min_dbinfo";

	private GeoparserConfig config;

	@Before
	public void loadConfig() throws IOException {
		config = new GeoparserConfig();
		assertThat(config.getConfigStringLabels(), not(empty()));
		assertThat(config.getDBConnectionInfoLabels(), not(empty()));
	}

	@Test
	public void testCreateNewConfig() throws IOException {
		config = new GeoparserConfig(null);
		assertThat(config.getConfigStringLabels(), empty());
		assertThat(config.getDBConnectionInfoLabels(), empty());
	}

	@Test(expected = IOException.class)
	public void testConfigFileDoesNotExist() throws IOException {
		new GeoparserConfig("invalid_filename");
	}

	@Test
	public void testGetConfigStringByValidName() throws UnknownConfigLabelException {
		final String validConfigStringLabel = "geoparser.config.version";
		final String expectedConfigString = "0.1";

		final String actualConfigString = config.getConfigStringByLabel(validConfigStringLabel);

		assertThat(actualConfigString, equalTo(expectedConfigString));
	}

	@Test(expected = UnknownConfigLabelException.class)
	public void testGetConfigStringByInvalidName() throws UnknownConfigLabelException {
		final String invalidConfigStringLabel = "invalid_name";

		config.getConfigStringByLabel(invalidConfigStringLabel);
	}

	@Test
	public void testGetDBConnectionInfoWithFullInfo() throws UnknownConfigLabelException {
		final DBConnectionData expectedFullDbConnectionData = new DBConnectionData("testdb", "localhost", 5432,
				"tester", "tester".toCharArray(), true);
		final Hashtable<String, String> sshConfig = new Hashtable<>();
		sshConfig.put("string", "string_value");
		final SSHConnectionData expectedFullSSHConnectionData = new SSHConnectionData("localhost", 22, "tester",
				"tester".toCharArray(),
				new HashSet<>(Arrays.asList(
						new SSHPortForwardingRule("*", 12345, "localhost", 56789, SSHPortForwardingDirection.LOCAL),
						new SSHPortForwardingRule("*", 56789, "localhost", 12345, SSHPortForwardingDirection.REMOTE))),
				true, sshConfig);
		final DBConnectionInfo expectedFullDbConnectionInfo = new DBConnectionInfo(expectedFullDbConnectionData,
				expectedFullSSHConnectionData);

		final String validDbInfoLabel = TEST_FULL_DATABASE_INFO_LABEL;
		final DBConnectionInfo actualFullDbConnectionInfo = config.getDBConnectionInfoByLabel(validDbInfoLabel);

		assertThat(actualFullDbConnectionInfo, equalTo(expectedFullDbConnectionInfo));
	}

	@Test
	public void testGetDBConnectionWithMinInfo() throws UnknownConfigLabelException {
		final String validDbInfoLabel = TEST_MININFO_DATABASE_INFO_LABEL;
		final DBConnectionData expectedMinDbConnectionData = new DBConnectionData("testdb", "localhost", 5432, null,
				null, false);
		final SSHConnectionData expectedMinSSHConnectionData = new SSHConnectionData(null, 0, null, null, null, false,
				null);
		final DBConnectionInfo expectedMinDbConnectionInfo = new DBConnectionInfo(expectedMinDbConnectionData,
				expectedMinSSHConnectionData);

		final DBConnectionInfo actualMinDbConnectionInfo = config.getDBConnectionInfoByLabel(validDbInfoLabel);

		assertThat(actualMinDbConnectionInfo, equalTo(expectedMinDbConnectionInfo));
	}

	@Test(expected = UnknownConfigLabelException.class)
	public void testGetDBConnectionInfoByInvalidName() throws UnknownConfigLabelException {
		final String invalidDbInfoLabel = "invalid_name";

		config.getDBConnectionInfoByLabel(invalidDbInfoLabel);
	}

	@Test
	public void testSetConfigString() throws UnknownConfigLabelException {
		final String newLabel = "test";
		final String expecedConfigString = "test_config_string";
		config.setConfigString(newLabel, expecedConfigString);

		final String actualConfigString = config.getConfigStringByLabel(newLabel);

		assertThat(actualConfigString, equalTo(expecedConfigString));
	}

	@Test(expected = NullPointerException.class)
	public void testSetConfigStringWithNullLabel() {
		config.setConfigString(null, null);
	}

	@Test
	public void testSetDBConnectionInfo() throws UnknownConfigLabelException {
		final String newLabel = "test";
		final DBConnectionData expectedDbConnectionData = new DBConnectionData("testdb", "localhost", 5432, null, null,
				false);
		final SSHConnectionData expectedSSHConnectionData = new SSHConnectionData(null, 0, null, null, null, false,
				null);
		final DBConnectionInfo expectedDbConnectionInfo = new DBConnectionInfo(expectedDbConnectionData,
				expectedSSHConnectionData);
		config.setDBConnectionInfo(newLabel, expectedDbConnectionInfo);

		final DBConnectionInfo actualDbConnectionInfo = config.getDBConnectionInfoByLabel(newLabel);

		assertThat(actualDbConnectionInfo, equalTo(expectedDbConnectionInfo));
	}

	@Test(expected = NullPointerException.class)
	public void testSetDBConnectionInfoWithNullLabel() {
		config.setDBConnectionInfo(null, null);
	}

	@Test(expected = UnknownConfigLabelException.class)
	public void testRemoveConfigString() throws UnknownConfigLabelException {
		final String newLabel = "test";
		final String configString = "test_config_string";
		config.setConfigString(newLabel, configString);
		config.removeConfigStringByLabel(newLabel);
		config.getConfigStringByLabel(newLabel);
	}

	@Test(expected = UnknownConfigLabelException.class)
	public void testRemoveDBConnectionInfo() throws UnknownConfigLabelException {
		final String newLabel = "test";
		final DBConnectionData dbConnectionData = new DBConnectionData("testdb", "localhost", 5432, null, null, false);
		final SSHConnectionData sshConnectionData = new SSHConnectionData(null, 0, null, null, null, false, null);
		final DBConnectionInfo dbConnectionInfo = new DBConnectionInfo(dbConnectionData, sshConnectionData);
		config.setDBConnectionInfo(newLabel, dbConnectionInfo);
		config.removeDBConnectionInfoByLabel(newLabel);
		config.getDBConnectionInfoByLabel(newLabel);
	}

	@Test
	public void testWriteConfigToJsonFile() throws IOException {
		// simple smoke test...
		final String fileName = "testConfigFile.json";
		try {
			config.writeConfigToJsonFile(fileName);
		}
		finally {
			assertTrue(new File(fileName).delete());
		}
	}

}
