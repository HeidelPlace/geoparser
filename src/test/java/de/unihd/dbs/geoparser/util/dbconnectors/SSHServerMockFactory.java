package de.unihd.dbs.geoparser.util.dbconnectors;

import java.util.ArrayList;
import java.util.List;

import org.apache.sshd.common.NamedFactory;
import org.apache.sshd.server.SshServer;
import org.apache.sshd.server.auth.UserAuth;
import org.apache.sshd.server.auth.password.PasswordAuthenticator;
import org.apache.sshd.server.auth.password.UserAuthPasswordFactory;
import org.apache.sshd.server.forward.AcceptAllForwardingFilter;
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider;
import org.apache.sshd.server.session.ServerSession;

/**
 * Factory class for creating SSH server sessions using the SSHD-framework, which are used as mocks in Unit Testing.
 *
 * @author lrichter
 *
 */
public class SSHServerMockFactory {

	/**
	 * Return a SSH server session for the given configuration, which accepts all port forwarding requests.
	 *
	 * @param sshPort port on which the server should listen
	 * @param sshUserName user name, which should be allowed to authenticate
	 * @param sshPassword password for the user
	 * @return a SSH server which is initialized with the given configuration
	 */
	public static SshServer createSSHServerAcceptAllForwarding(final int sshPort, final String sshUserName,
			final char[] sshPassword) {
		final SshServer sshServer;
		sshServer = SshServer.setUpDefaultServer();
		sshServer.setPort(sshPort);
		sshServer.setKeyPairProvider(new SimpleGeneratorHostKeyProvider());
		final List<NamedFactory<UserAuth>> userAuthFactories = new ArrayList<>();
		userAuthFactories.add(new UserAuthPasswordFactory());
		sshServer.setUserAuthFactories(userAuthFactories);
		sshServer.setPasswordAuthenticator(new PasswordAuthenticator() {
			@Override
			public boolean authenticate(final String username, final String password, final ServerSession session) {
				return sshUserName.equals(username) && (new String(sshPassword)).equals(password);
			}
		});

		// Allow port forwarding
		sshServer.setTcpipForwardingFilter(new AcceptAllForwardingFilter());

		return sshServer;
	}
}
