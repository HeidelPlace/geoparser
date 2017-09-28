package de.unihd.dbs.geoparser.util.dbconnectors;

import java.util.Arrays;

/**
 * Simple data structure to store database connection information.
 * 
 * @author lrichter
 *
 */
public final class DBConnectionData {

	/**
	 * Name of the database to connect to
	 */
	public String dbName;

	/**
	 * Host address under which the database server is running
	 */
	public String host;

	/**
	 * Port on which the database server is listening
	 */
	public int port;

	/**
	 * User name to be used on the database server
	 */
	public String userName;

	/**
	 * Password for the database user
	 */
	public char[] password;

	/**
	 * Flag indicating if user authentication is required for the database server
	 */
	public boolean authenticationRequired;

	public DBConnectionData(final String dbName, final String host, final int port, final String userName,
			final char[] password, final boolean authenticationRequired) {
		super();
		this.dbName = dbName;
		this.host = host;
		if (password != null) {
			this.password = Arrays.copyOf(password, password.length);
		}
		this.port = port;
		this.authenticationRequired = authenticationRequired;
		this.userName = userName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (authenticationRequired ? 1231 : 1237);
		result = prime * result + ((dbName == null) ? 0 : dbName.hashCode());
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + Arrays.hashCode(password);
		result = prime * result + port;
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
		final DBConnectionData other = (DBConnectionData) obj;
		if (authenticationRequired != other.authenticationRequired)
			return false;
		if (dbName == null) {
			if (other.dbName != null)
				return false;
		}
		else if (!dbName.equals(other.dbName))
			return false;
		if (host == null) {
			if (other.host != null)
				return false;
		}
		else if (!host.equals(other.host))
			return false;
		if (!Arrays.equals(password, other.password))
			return false;
		if (port != other.port)
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
		return "DBConnectionData [dbName=" + dbName + ", host=" + host + ", port=" + port + ", userName=" + userName
				+ ", password=***" + ", authenticationRequired=" + authenticationRequired + "]";
	}

}