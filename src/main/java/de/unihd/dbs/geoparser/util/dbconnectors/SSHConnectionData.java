package de.unihd.dbs.geoparser.util.dbconnectors;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.Set;

/**
 * Simple data structure to store SSH connection information.
 * 
 * @author lrichter
 *
 */
public final class SSHConnectionData {

	/**
	 * Data structure defining a SSH port forwarding rule.
	 * 
	 * @author lrichter
	 *
	 */
	public final static class SSHPortForwardingRule {

		public enum SSHPortForwardingDirection {
			LOCAL("LOCAL_PORT_FORWARD"), REMOTE("REMOTE_PORT_FORWARD");

			private final String name;

			private SSHPortForwardingDirection(final String s) {
				name = s;
			}

			@Override
			public String toString() {
				return this.name;
			}
		}

		/**
		 * Bind address for local port forwarding
		 */
		public String bindAddress;

		/**
		 * Local port for port forwarding
		 */
		public int localPort;

		/**
		 * Host address for port forwarding
		 */
		public String hostAddress;

		/**
		 * Remote port number for port forwarding
		 */
		public int remotePort;

		/**
		 * Direction of the port forwarding
		 */
		public SSHPortForwardingDirection direction;

		public SSHPortForwardingRule(final String bindAddress, final int localPort, final String hostAddress,
				final int remotePort, final SSHPortForwardingDirection direction) {
			super();
			this.bindAddress = bindAddress;
			this.localPort = localPort;
			this.hostAddress = hostAddress;
			this.remotePort = remotePort;
			this.direction = direction;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((bindAddress == null) ? 0 : bindAddress.hashCode());
			result = prime * result + ((direction == null) ? 0 : direction.hashCode());
			result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
			result = prime * result + localPort;
			result = prime * result + remotePort;
			return result;
		}

		@Override
		public boolean equals(final Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			final SSHPortForwardingRule other = (SSHPortForwardingRule) obj;
			if (bindAddress == null) {
				if (other.bindAddress != null)
					return false;
			}
			else if (!bindAddress.equals(other.bindAddress))
				return false;
			if (direction != other.direction)
				return false;
			if (hostAddress == null) {
				if (other.hostAddress != null)
					return false;
			}
			else if (!hostAddress.equals(other.hostAddress))
				return false;
			if (localPort != other.localPort)
				return false;
			if (remotePort != other.remotePort)
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "SSHPortForwardingRule [bindAddress=" + bindAddress + ", localPort=" + localPort + ", hostAddress="
					+ hostAddress + ", remotePort=" + remotePort + ", direction=" + direction + "]";
		}

	}

	/**
	 * Address of SSH server
	 */
	public String hostAddress;

	/**
	 * Port on which the SSH-server is listening
	 */
	public int port;

	/**
	 * User name used for SSH-authentication
	 */
	public String userName;

	/**
	 * Password for the SSH user
	 */
	public char[] password;

	/**
	 * Port-forwarding details. If null, no port forwarding is required.
	 */
	public Set<SSHPortForwardingRule> portForwardingRules;

	/**
	 * Flag indicating if a SSH connection is required
	 */
	public boolean sshRequired;

	/**
	 * List of configuration parameters to be passed to the SSH client
	 */
	public Hashtable<String, String> sshConfig;

	public SSHConnectionData(final String host, final int port, final String userName, final char[] password,
			final Set<SSHPortForwardingRule> portForwardingRules, final boolean sshRequired,
			final Hashtable<String, String> sshConfig) {
		super();

		if (password != null) {
			this.password = Arrays.copyOf(password, password.length);
		}
		this.hostAddress = host;
		this.port = port;
		this.portForwardingRules = portForwardingRules;
		this.sshRequired = sshRequired;
		this.userName = userName;
		if (sshConfig != null) {
			this.sshConfig = new Hashtable<>(sshConfig);
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((hostAddress == null) ? 0 : hostAddress.hashCode());
		result = prime * result + Arrays.hashCode(password);
		result = prime * result + port;
		result = prime * result + ((portForwardingRules == null) ? 0 : portForwardingRules.hashCode());
		result = prime * result + ((sshConfig == null) ? 0 : sshConfig.hashCode());
		result = prime * result + (sshRequired ? 1231 : 1237);
		result = prime * result + ((userName == null) ? 0 : userName.hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final SSHConnectionData other = (SSHConnectionData) obj;
		if (hostAddress == null) {
			if (other.hostAddress != null)
				return false;
		}
		else if (!hostAddress.equals(other.hostAddress))
			return false;
		if (!Arrays.equals(password, other.password))
			return false;
		if (port != other.port)
			return false;
		if (portForwardingRules == null) {
			if (other.portForwardingRules != null)
				return false;
		}
		else if (!portForwardingRules.equals(other.portForwardingRules))
			return false;
		if (sshConfig == null) {
			if (other.sshConfig != null)
				return false;
		}
		else if (!sshConfig.equals(other.sshConfig))
			return false;
		if (sshRequired != other.sshRequired)
			return false;
		if (userName == null) {
			if (other.userName != null)
				return false;
		}
		else if (!userName.equals(other.userName))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SSHConnectionData [hostAddress=" + hostAddress + ", port=" + port + ", userName=" + userName
				+ ", password=***" + ", portForwardingRules=" + portForwardingRules + ", sshRequired=" + sshRequired
				+ ", sshConfig=" + sshConfig + "]";
	}

}