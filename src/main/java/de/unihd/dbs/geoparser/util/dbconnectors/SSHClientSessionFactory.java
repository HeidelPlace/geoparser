package de.unihd.dbs.geoparser.util.dbconnectors;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Objects;
import java.util.Set;

import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData.SSHPortForwardingRule;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Factory class for creating SSH client sessions using the JSch-framework.
 * 
 * It provides a set of convenient methods to create JSch client connections, with a particular focus on the
 * {@link SSHConnectionData} data structure.
 * 
 * @author lrichter
 *
 */
public class SSHClientSessionFactory {

	/**
	 * Return a connected SSH client session, for SSH connection data and port forwarding set up if needed.
	 * 
	 * @param sshConnectionData SSH connection configuration
	 * @return A connected Jsch SSH client session
	 * @throws JSchException if interacting with Jsch fails for some reason
	 */
	public static Session getConnectedSessionWithPortForwarding(final SSHConnectionData sshConnectionData)
			throws JSchException {
		final Session sshSession = getSession(sshConnectionData);
		sshSession.connect();
		try {
			setPortForwardings(sshSession, sshConnectionData.portForwardingRules);
		}
		catch (final JSchException e) {
			sshSession.disconnect();
			throw e;
		}
		return sshSession;
	}

	/**
	 * Establish port forwarding for the given SSH client session.
	 * 
	 * @param sshSession SSH connection for which port forwarding should be configured
	 * @param portForwardingRules list of port-forwarding rules to establish
	 * @throws JSchException if setting up a port forwarding failed
	 */
	public static void setPortForwardings(final Session sshSession,
			final Set<SSHPortForwardingRule> portForwardingRules) throws JSchException {
		if (portForwardingRules != null) {
			for (final SSHPortForwardingRule rule : portForwardingRules) {
				Objects.requireNonNull(rule.direction, "Port-forwarding direction may not be 'null'!");
				switch (rule.direction) {
				case LOCAL:
					sshSession.setPortForwardingL(rule.bindAddress, rule.localPort, rule.hostAddress, rule.remotePort);
					break;
				case REMOTE:
					sshSession.setPortForwardingR(rule.bindAddress, rule.localPort, rule.hostAddress, rule.remotePort);
					break;
				default:
					throw new IllegalArgumentException("Unsupported port-forwarding direction " + rule.direction);
				}
			}
		}
	}

	/**
	 * Return a SSH client session, which is initialized with the given SSH connection data.
	 * 
	 * @param sshConnectionData SSH connection configuration
	 * @return a Jsch SSH client session
	 * @throws JSchException if setting up the session failed
	 */
	public static Session getSession(final SSHConnectionData sshConnectionData) throws JSchException {
		return getSession(sshConnectionData.hostAddress, sshConnectionData.port, sshConnectionData.userName,
				sshConnectionData.password, sshConnectionData.sshConfig);
	}

	/**
	 * Return a SSH client session, which is initialized with the given SSH connection data.
	 * 
	 * @param sshHostAddress address of the SSH host
	 * @param sshPort port on which the SSH host is listening
	 * @param sshUserName user name to be used for SSH authentication
	 * @param sshPassword password to be used for SSH authentication
	 * @param sshConfig optional set of properties for configuring the session
	 * @return a Jsch SSH client session
	 * @throws JSchException if setting up the session failed
	 */
	public static Session getSession(final String sshHostAddress, final int sshPort, final String sshUserName,
			final char[] sshPassword, final Hashtable<String, String> sshConfig) throws JSchException {
		final JSch jsch = new JSch();
		final Session sshSession = jsch.getSession(sshUserName, sshHostAddress, sshPort);
		sshSession.setPassword(toBytes(sshPassword));
		if (sshConfig != null) {
			sshSession.setConfig(sshConfig);
		}

		return sshSession;
	}

	private static byte[] toBytes(final char[] chars) {
		if (chars == null) {
			return null;
		}

		final CharBuffer charBuffer = CharBuffer.wrap(chars);
		final ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
		final byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
		Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
		return bytes;
	}

}
