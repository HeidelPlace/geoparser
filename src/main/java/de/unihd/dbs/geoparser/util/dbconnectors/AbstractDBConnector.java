package de.unihd.dbs.geoparser.util.dbconnectors;

import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * Abstract class for maintaining a database connection based on a {@link DBConnectionInfo} database configuration.
 * 
 * AbstractDBConnector fully implements the SSH connection aspect, but leaves it to the inherited classes to implement
 * functionality to establish the database connection.
 * 
 * <b>Note:</b> Always call {@link AbstractDBConnector#connect} and {@link AbstractDBConnector#disconnect} instead of
 * connection.connect() and connection.close() in order to ensure that the SSH tunnel is maintained correctly!
 */
public abstract class AbstractDBConnector implements AutoCloseable {

	protected AutoCloseable connection;
	protected final DBConnectionInfo dbConnectionInfo;
	protected Session sshClientSession;

	protected AbstractDBConnector(final DBConnectionInfo dbConnectionInfo) {
		super();
		this.dbConnectionInfo = dbConnectionInfo;
	}

	/**
	 * Establish connection to the database server, using SSH tunneling if needed.
	 * 
	 * @throws IllegalStateException if the database connection is already active.
	 * @throws JSchException if establishing the SSH connection failed
	 * @throws Exception if establishing the database connection failed
	 */
	public void connect() throws IllegalStateException, JSchException, Exception {
		if ((sshClientSession != null && sshClientSession.isConnected()) || connection != null) {
			throw new IllegalStateException("Already connected to the database!");
		}
		try {
			establishSSHConnection();
		}
		catch (final JSchException e) {
			throw e;
		}
		try {
			establishDBConnection();
		}
		catch (final Exception e) {
			try {
				disconnectSSH();
			}
			catch (final Exception eNew) {
				e.addSuppressed(eNew);
			}
			throw e;
		}
	}

	/**
	 * Establish a connection the a remote server via SSH, if needed. Called by {@link #connect()} only.
	 * 
	 * @throws JSchException if establishing the SSH connection failed
	 */
	private void establishSSHConnection() throws JSchException {
		final SSHConnectionData sshData = dbConnectionInfo.sshConnectionData;

		if (sshData.sshRequired) {
			sshClientSession = SSHClientSessionFactory.getConnectedSessionWithPortForwarding(sshData);
		}
	}

	/**
	 * Establish a connection to the database. Called by {@link #connect()} only.
	 * 
	 * Abstract method that needs to be implemented by inheriting classes. Visibility should be kept as `protected`
	 * since users should not need to call this method individually.
	 * 
	 * @throws Exception if establishing the database connection failed
	 */
	protected abstract void establishDBConnection() throws Exception;

	/**
	 * Disconnect from database the database server and SSH tunnel.
	 * 
	 * <b>Note:</b> Always call this method instead of connection.close() in order to ensure that the SSH connection is
	 * also closed!
	 * 
	 * @throws Exception if closing the database connection failed.
	 */
	public void disconnect() throws Exception {
		disconnectSSH();
		disconnectDB();
	}

	private void disconnectDB() throws Exception {
		if (connection != null) {
			connection.close();
			connection = null;
		}
	}

	private void disconnectSSH() {
		if (sshClientSession != null) {
			sshClientSession.disconnect();
			sshClientSession = null;
		}
	}

	/**
	 * Get the database connection.
	 * 
	 * Abstract method that needs to be implemented by inheriting classes. The inheriting class should adjust the return
	 * type to the respective database-connection class implementing the AutoCloseable interface.
	 * 
	 * @return the database connection
	 */
	public abstract AutoCloseable getConnection();

	@Override
	public void close() throws Exception {
		disconnect();
	}
}
