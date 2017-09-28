package de.unihd.dbs.geoparser.util.dbconnectors;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule.SSHPortForwardingDirection;

import nl.jqno.equalsverifier.EqualsVerifier;
import nl.jqno.equalsverifier.Warning;

public class DBConnectionInfoTest {

	private static final String dbName = "db_name";
	private static final String dbHost = "db_host";
	private static final String dbUserName = "db_username";
	private static final char[] dbPassword = "db_password".toCharArray();
	private static final int dbPort = 1234;
	private static final boolean dbAuthRequired = true;

	private static final String sshHostAddress = "ssh_host_address";
	private static final String sshUserName = "ssh_username";
	private static final char[] sshPassword = "ssh_password".toCharArray();
	private static final int sshPort = 1234;
	private static final boolean sshRequired = true;

	private static final String sshPortFordwardingBindAddress = "*";
	private static final int sshPortForwardingLocalPort = 1234;
	private static final String sshPortForwardingHostAddress = "localhost";
	private static final int sshPortForwardingRemotePort = 5678;
	private static final SSHPortForwardingDirection sshPortForwardingDirection = SSHPortForwardingDirection.LOCAL;
	private static final SSHPortForwardingRule sshPortForwardingRule = new SSHPortForwardingRule(
			sshPortFordwardingBindAddress, sshPortForwardingLocalPort, sshPortForwardingHostAddress,
			sshPortForwardingRemotePort, sshPortForwardingDirection);
	private static final Set<SSHPortForwardingRule> sshPortForwardingRules = new HashSet<>(
			Arrays.asList(sshPortForwardingRule));
	private static final Hashtable<String, String> sshConfig = new Hashtable<>();

	static {
		sshConfig.put("string", "string_value");
	}

	private DBConnectionData dbConnectionData;
	private SSHConnectionData sshConnectionData;

	private DBConnectionInfo dbConnectionInfo;

	@Before
	public void createDbConnectionInfo() {
		dbConnectionData = new DBConnectionData(dbName, dbHost, dbPort, dbUserName, dbPassword, dbAuthRequired);
		sshConnectionData = new SSHConnectionData(sshHostAddress, sshPort, sshUserName, sshPassword,
				sshPortForwardingRules, sshRequired, sshConfig);
		dbConnectionInfo = new DBConnectionInfo(dbConnectionData, sshConnectionData);
	}

	@Test
	public void testEqualsContractShouldHold() {
		EqualsVerifier.forClass(DBConnectionInfo.class).suppress(Warning.NONFINAL_FIELDS).verify();
	}

	@Test
	public void testToStringShouldWork() {
		// perform a simple smoke test
		dbConnectionInfo.toString();
	}

	@Test
	public void testDBConnectionInfoConstructorShouldSetFieldsCorrectly() {
		assertThat(dbConnectionInfo.dbConnectionData, equalTo(dbConnectionData));
		assertThat(dbConnectionInfo.sshConnectionData, equalTo(sshConnectionData));
	}

	@Test(expected = NullPointerException.class)
	public void testDBConnectionInfoConstructorWithNullDbConnectionDataShouldFail() {
		new DBConnectionInfo(null, sshConnectionData);
	}

	@Test(expected = NullPointerException.class)
	public void testDBConnectionInfoConstructorWithNullSshConnectionDataShouldFail() {
		new DBConnectionInfo(dbConnectionData, null);
	}

	@Test
	public void testRetrievePasswordFromConsoleShouldSetNullPasswords() throws Exception {
		dbConnectionInfo.dbConnectionData.authenticationRequired = true;
		dbConnectionInfo.dbConnectionData.password = null;
		dbConnectionInfo.sshConnectionData.sshRequired = true;
		dbConnectionInfo.sshConnectionData.password = null;

		readPasswordFromConsoleMock(sshPassword, dbPassword);

		assertThat(dbConnectionInfo.dbConnectionData.password, equalTo(dbPassword));
		assertThat(dbConnectionInfo.sshConnectionData.password, equalTo(sshPassword));
	}

	@Test
	public void testRetrieveNullPasswordFromConsoleShouldSetNullPasswords() throws Exception {
		dbConnectionInfo.dbConnectionData.authenticationRequired = true;
		dbConnectionInfo.dbConnectionData.password = null;
		dbConnectionInfo.sshConnectionData.sshRequired = true;
		dbConnectionInfo.sshConnectionData.password = null;

		readPasswordFromConsoleMock(null, null);

		assertThat(dbConnectionInfo.dbConnectionData.password, nullValue());
		assertThat(dbConnectionInfo.sshConnectionData.password, nullValue());
	}

	@Test
	public void testRetrievePasswordFromConsoleShouldSetEmptyPasswords() throws Exception {
		dbConnectionInfo.dbConnectionData.authenticationRequired = true;
		dbConnectionInfo.dbConnectionData.password = "".toCharArray();
		dbConnectionInfo.sshConnectionData.sshRequired = true;
		dbConnectionInfo.sshConnectionData.password = "".toCharArray();

		readPasswordFromConsoleMock(sshPassword, dbPassword);

		assertThat(dbConnectionInfo.dbConnectionData.password, equalTo(dbPassword));
		assertThat(dbConnectionInfo.sshConnectionData.password, equalTo(sshPassword));
	}

	private void readPasswordFromConsoleMock(final char[] sshPassword, final char[] dbPassword) throws IOException {
		// http://stackoverflow.com/questions/15849978/unit-testing-multiple-consecutive-keyboard-inputs
		// http://stackoverflow.com/questions/5513152/easy-way-to-concatenate-two-byte-arrays
		final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		if (sshPassword != null) {
			outputStream.write(Arrays.copyOf((new String(sshPassword) + '\n').getBytes(StandardCharsets.UTF_8), 8192));
		}
		if (dbPassword != null) {
			outputStream.write((new String(dbPassword) + '\n').getBytes(StandardCharsets.UTF_8));
		}
		final InputStream stdIn = System.in;
		try (final ByteArrayInputStream in = new ByteArrayInputStream(outputStream.toByteArray())) {
			System.setIn(in);
			dbConnectionInfo.setPasswordsFromConsoleInput();
		}
		finally {
			System.setIn(stdIn);
		}
	}

	@Test
	public void testRetrievePasswordFromConsoleShouldNotChangeNonEmptyPasswords() {
		dbConnectionInfo.setPasswordsFromConsoleInput();

		assertThat(dbConnectionInfo.dbConnectionData.password, equalTo(dbPassword));
		assertThat(dbConnectionInfo.sshConnectionData.password, equalTo(sshPassword));
	}

	@Test
	public void testRetrievePasswordFromConsoleShouldNotSetUnneededPasswords() {
		dbConnectionInfo.dbConnectionData.authenticationRequired = false;
		dbConnectionInfo.sshConnectionData.sshRequired = false;

		dbConnectionInfo.setPasswordsFromConsoleInput();

		assertThat(dbConnectionInfo.dbConnectionData.password, equalTo(dbPassword));
		assertThat(dbConnectionInfo.sshConnectionData.password, equalTo(sshPassword));
	}

}
