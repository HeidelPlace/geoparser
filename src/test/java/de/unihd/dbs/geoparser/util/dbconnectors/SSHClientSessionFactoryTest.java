package de.unihd.dbs.geoparser.util.dbconnectors;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.forward.RejectAllForwardingFilter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule.SSHPortForwardingDirection;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

public class SSHClientSessionFactoryTest {

	private static final String sshHostAddress = "localhost";
	private static final String sshUserName = "ssh_username";
	private static final char[] sshPassword = "ssh_password".toCharArray();
	private static final int sshPort = 22;
	private static final boolean sshRequired = true;

	private static final String sshPortFordwardingBindAddress = null;
	private static final int sshPortForwardingLocalPort = 12345;
	private static final String sshPortForwardingHostAddress = "localhost";
	private static final int sshPortForwardingRemotePort = 45678;
	private static final SSHPortForwardingRule sshPortForwardingRuleLocal = new SSHPortForwardingRule(
			sshPortFordwardingBindAddress, sshPortForwardingLocalPort, sshPortForwardingHostAddress,
			sshPortForwardingRemotePort, SSHPortForwardingDirection.LOCAL);
	private static final SSHPortForwardingRule sshPortForwardingRuleRemote = new SSHPortForwardingRule(
			sshPortFordwardingBindAddress, sshPortForwardingRemotePort, sshPortForwardingHostAddress,
			sshPortForwardingLocalPort, SSHPortForwardingDirection.REMOTE);
	private static final Set<SSHPortForwardingRule> sshPortForwardingRules = new HashSet<>(
			Arrays.asList(sshPortForwardingRuleLocal, sshPortForwardingRuleRemote));
	private static final Hashtable<String, String> sshConfig = new Hashtable<>();

	static {
		sshConfig.put("StrictHostKeyChecking", "no");
	}

	private static SshServer sshServer;

	private SSHConnectionData sshConnectionData;

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
		sshConnectionData = new SSHConnectionData(sshHostAddress, sshPort, sshUserName, sshPassword,
				sshPortForwardingRules, sshRequired, sshConfig);
	}

	@Test
	public void testGetConnectedSessionShouldWork() throws JSchException {
		SSHClientSessionFactory.getSession(sshConnectionData);
	}

	@Test
	public void testConnectToSessionShouldWork() throws JSchException {
		final Session sshSession = SSHClientSessionFactory.getSession(sshConnectionData);
		try {
			sshSession.connect();
		}
		finally {
			sshSession.disconnect();
		}
	}

	@Test
	public void testGetConnectedSessionWithPortForwardingShouldWork() throws JSchException {
		final Session sshSession = SSHClientSessionFactory.getConnectedSessionWithPortForwarding(sshConnectionData);
		sshSession.disconnect();
	}

	@Test(expected = JSchException.class)
	public void testGetConnectedSessionWithNoServerPortForwardingSupportShouldFail() throws JSchException {
		try {
			sshServer.setTcpipForwardingFilter(new RejectAllForwardingFilter());
			final Session sshSession = SSHClientSessionFactory.getConnectedSessionWithPortForwarding(sshConnectionData);
			sshSession.disconnect();
		}
		finally {
			sshServer.setTcpipForwardingFilter(new AcceptAllForwardingFilter());
		}
	}

	@Test
	public void testGetConnectedSessionWithNullPortForwardingShouldWork() throws JSchException {
		sshConnectionData.portForwardingRules = null;
		final Session sshSession = SSHClientSessionFactory.getConnectedSessionWithPortForwarding(sshConnectionData);
		sshSession.disconnect();
	}

	@Test(expected = NullPointerException.class)
	public void testGetConnectedSessionWithPortForwardingUsingNullDirectionShouldFail() throws JSchException {
		sshConnectionData.portForwardingRules = new HashSet<>(
				Arrays.asList(new SSHPortForwardingRule("", 0, "", 0, null)));
		final Session sshSession = SSHClientSessionFactory.getConnectedSessionWithPortForwarding(sshConnectionData);
		sshSession.disconnect();
	}

}
