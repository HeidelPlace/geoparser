package de.unihd.dbs.geoparser.util.dbconnectors;

import java.io.IOException;
import java.util.Hashtable;

import org.apache.sshd.server.SshServer;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.jcraft.jsch.JSchException;

public class AbstractDBConnectorTest {

	private static class DummyDBConnection implements AutoCloseable {

		@Override
		public void close() throws Exception {
		}

	}

	private static class FailingDummyDBConnection implements AutoCloseable {

		@Override
		public void close() throws Exception {
			throw new Exception("FailingDummyDBConnection.close() should fail...");
		}

	}

	private static class DummyDBConnector extends AbstractDBConnector {

		private final AutoCloseable dbDummyType;
		private final boolean establishDBConnectionShouldFail;

		protected DummyDBConnector(final DBConnectionInfo dbConnectionInfo, final AutoCloseable dbDummyType,
				final boolean establishDBConnectionShouldFail) {
			super(dbConnectionInfo);
			this.dbDummyType = dbDummyType;
			this.establishDBConnectionShouldFail = establishDBConnectionShouldFail;
		}

		@Override
		protected void establishDBConnection() throws Exception {
			if (establishDBConnectionShouldFail) {
				throw new Exception("DummyDBConnector.establishDBConnection() should fail...");
			}
			this.connection = dbDummyType;
		}

		@Override
		public AutoCloseable getConnection() {
			return null;
		}
	}

	private static final String sshHostAddress = "localhost";
	private static final String sshUserName = "ssh_username";
	private static final char[] sshPassword = "ssh_password".toCharArray();
	private static final int sshPort = 22;
	private static final boolean sshRequired = true;
	private static final Hashtable<String, String> sshConfig = new Hashtable<>();

	static {
		sshConfig.put("StrictHostKeyChecking", "no");
	}

	private static SshServer sshServer;

	private DBConnectionInfo dbConnectionInfo;
	private DummyDBConnector dummyDBConnector;

	@BeforeClass
	public static void startSSHServer() throws IOException {
		sshServer = SSHServerMockFactory.createSSHServerAcceptAllForwarding(sshPort, sshUserName, sshPassword);
		sshServer.start();
	}

	@AfterClass
	public static void tearDownSSHServer() throws IOException {
		if (sshServer != null) {
			sshServer.stop();
		}
	}

	@Before
	public void createSSHConnectionData() {
		final SSHConnectionData sshConnectionData = new SSHConnectionData(sshHostAddress, sshPort, sshUserName,
				sshPassword, null, sshRequired, sshConfig);
		final DBConnectionData dbConnectionData = new DBConnectionData("", "", 0, "", null, false);
		dbConnectionInfo = new DBConnectionInfo(dbConnectionData, sshConnectionData);
		@SuppressWarnings("resource")
		final DummyDBConnection dummyConnection = new DummyDBConnection();
		dummyDBConnector = new DummyDBConnector(dbConnectionInfo, dummyConnection, false);
	}

	@Test
	public void testConnectWithSSHShouldWork() throws IllegalStateException, JSchException, Exception {
		try {
			dummyDBConnector.connect();
		}
		finally {
			dummyDBConnector.disconnect();
		}
	}

	@Test(expected = JSchException.class)
	public void testConnectWithSSHUsingInvalidCredentialsShouldFail()
			throws IllegalStateException, JSchException, Exception {
		dummyDBConnector.dbConnectionInfo.sshConnectionData.userName = "wrong name";
		try {
			dummyDBConnector.connect();
		}
		finally {
			dummyDBConnector.disconnect();
		}
	}

	@Test
	public void testConnectWithoutSSHShouldWork() throws IllegalStateException, JSchException, Exception {
		dummyDBConnector.dbConnectionInfo.sshConnectionData.sshRequired = false;
		try {
			dummyDBConnector.connect();
		}
		finally {
			dummyDBConnector.disconnect();
		}
	}

	@Test(expected = IllegalStateException.class)
	public void testMultipleConnectShouldFail() throws IOException, Exception {
		dummyDBConnector.connect();
		try {
			dummyDBConnector.connect();
		}
		finally {
			dummyDBConnector.disconnect();
		}
	}

	@Test(expected = Exception.class)
	public void testConnectWithFailingDBConnectionShouldFail() throws IllegalStateException, JSchException, Exception {
		@SuppressWarnings("resource")
		final DummyDBConnection dummyConnection = new DummyDBConnection();
		try (DummyDBConnector dummyDBConnector = new DummyDBConnector(dbConnectionInfo, dummyConnection, true)) {
			dummyDBConnector.connect();
		}
	}

	@Test
	public void testMultipleDisconnectShouldSucceed() throws IllegalStateException, IOException, Exception {
		dummyDBConnector.connect();
		dummyDBConnector.disconnect();
		dummyDBConnector.disconnect();
	}

	@Test
	public void testDisconnectWithoutConnectShouldSucceed() throws Exception {
		dummyDBConnector.disconnect();
	}

	@Test(expected = Exception.class)
	public void testDisconnectWithFailingDBConnectionShouldFail()
			throws IllegalStateException, JSchException, Exception {
		@SuppressWarnings("resource")
		final FailingDummyDBConnection failingDummyConnection = new FailingDummyDBConnection();
		try (DummyDBConnector dummyDBConnector = new DummyDBConnector(dbConnectionInfo, failingDummyConnection,
				false)) {
			dummyDBConnector.connect();
		}
	}
}
