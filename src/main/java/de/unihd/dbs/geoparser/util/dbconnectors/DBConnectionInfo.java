package de.unihd.dbs.geoparser.util.dbconnectors;

import java.io.BufferedReader;
import java.io.Console;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * Maintainer class for database and SSH connection information.
 * 
 * @author lrichter
 *
 */
public final class DBConnectionInfo {

	public DBConnectionData dbConnectionData;
	public SSHConnectionData sshConnectionData;

	public DBConnectionInfo(final DBConnectionData dbConnectionData, final SSHConnectionData sshConnectionData)
			throws IllegalArgumentException {
		super();
		Objects.requireNonNull(dbConnectionData);
		Objects.requireNonNull(sshConnectionData);

		this.dbConnectionData = dbConnectionData;
		this.sshConnectionData = sshConnectionData;
	}

	/**
	 * Helper method for retrieving a password from the console.
	 * 
	 * If there is no Console-instance since the program was started in an IDE, the password is read from the
	 * stdin-stream.
	 * 
	 * @param promptMessage prompt message to be printed
	 * @return the password. If reading from stdin-stream fails, null is returned
	 */
	private static char[] getPasswordFromConsole(final String promptMessage) {
		final Console c = System.console();
		if (c != null) {
			return c.readPassword(promptMessage);
		}
		// Dirty work-around in case Eclipse is running, since in this case no Console-instance exists!
		// Note that in this case the password is temporarily stored as String-object and thus living quite long in the
		// garbage collector. Could be a security risk, thus use only for testing purposes!
		// http://stackoverflow.com/questions/4203646/system-console-returns-null
		else {
			try (final BufferedReader bufferRead = new BufferedReader(
					new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
				System.out.print(promptMessage);
				final String input = bufferRead.readLine();
				System.out.println();
				return (input != null) ? input.toCharArray() : null;
			}
			catch (final IOException e) {
				return null;
			}
		}
	}

	/**
	 * Set the passwords for the database and SSH connection via Console-inputs, if needed.
	 */
	public void setPasswordsFromConsoleInput() {
		if (sshConnectionData.sshRequired
				&& (sshConnectionData.password == null || sshConnectionData.password.length == 0)) {
			sshConnectionData.password = getPasswordFromConsole("Enter SSH-password for " + sshConnectionData.userName
					+ "@" + sshConnectionData.hostAddress + ":" + sshConnectionData.port + ": ");
		}

		if (dbConnectionData.authenticationRequired
				&& (dbConnectionData.password == null || dbConnectionData.password.length == 0)) {
			dbConnectionData.password = getPasswordFromConsole(
					"Enter password for " + dbConnectionData.userName + "@" + dbConnectionData.host + ":"
							+ dbConnectionData.port + " on database " + dbConnectionData.dbName + ": ");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dbConnectionData == null) ? 0 : dbConnectionData.hashCode());
		result = prime * result + ((sshConnectionData == null) ? 0 : sshConnectionData.hashCode());
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
		final DBConnectionInfo other = (DBConnectionInfo) obj;
		if (dbConnectionData == null) {
			if (other.dbConnectionData != null)
				return false;
		}
		else if (!dbConnectionData.equals(other.dbConnectionData))
			return false;
		if (sshConnectionData == null) {
			if (other.sshConnectionData != null)
				return false;
		}
		else if (!sshConnectionData.equals(other.sshConnectionData))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "DBConnectionInfo [dbConnectionData=" + dbConnectionData + ", sshConnectionData=" + sshConnectionData
				+ "]";
	}

}
