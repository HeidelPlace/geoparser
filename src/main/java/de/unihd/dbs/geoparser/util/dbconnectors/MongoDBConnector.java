package de.unihd.dbs.geoparser.util.dbconnectors;

import java.util.Arrays;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

/**
 * Connector for maintaining a connection to a MongoDB server, optionally via SSH tunneling.
 * 
 * @author lrichter
 *
 */
public class MongoDBConnector extends AbstractDBConnector {

	public MongoDBConnector(final DBConnectionInfo dbConnectionInfo) {
		super(dbConnectionInfo);
	}

	/**
	 * Establish connection to the MongoDB database server.
	 * 
	 * If Password-authentication is required, the connection-information `dbName`, `userName` and `password` in
	 * {@link MongoDBConnector#dbConnectionInfo.dbConnectionData} will be added as credentials-list entry.
	 */
	@Override
	protected void establishDBConnection() {
		final DBConnectionData dbData = dbConnectionInfo.dbConnectionData;

		if (dbData.authenticationRequired) {
			connection = new MongoClient(new ServerAddress(dbData.host, dbData.port),
					Arrays.asList(MongoCredential.createCredential(dbData.userName, dbData.dbName, dbData.password)));
		}
		else {
			connection = new MongoClient(new ServerAddress(dbData.host, dbData.port));
		}
	}

	@Override
	public MongoClient getConnection() {
		if (connection != null) {
			return (MongoClient) connection;
		}
		else {
			return null;
		}
	}

}
