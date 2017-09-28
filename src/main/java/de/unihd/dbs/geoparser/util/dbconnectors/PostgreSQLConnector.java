package de.unihd.dbs.geoparser.util.dbconnectors;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Connection;

/**
 * Connector for maintaining a connection to a PostgreSQL database server, optionally via SSH tunneling.
 * 
 * @author lrichter
 * 
 */
public class PostgreSQLConnector extends AbstractDBConnector {

	public PostgreSQLConnector(final DBConnectionInfo dbConnectionInfo) {
		super(dbConnectionInfo);
	}

	/**
	 * Establish connection to the PostgreSQL database server.
	 * 
	 * @throws SQLException if setting up a PostgreSQL connection failed.
	 */
	@Override
	protected void establishDBConnection() throws SQLException {
		final DBConnectionData dbData = dbConnectionInfo.dbConnectionData;
		connection = DriverManager.getConnection(
				"jdbc:postgresql://" + dbData.host + ":" + dbData.port + "/" + dbData.dbName, dbData.userName,
				new String(dbData.password));
	}

	@Override
	public Connection getConnection() {
		if (connection != null) {
			return (Connection) connection;
		}
		else {
			return null;
		}
	}

}