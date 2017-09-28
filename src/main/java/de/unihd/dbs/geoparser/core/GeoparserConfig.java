package de.unihd.dbs.geoparser.core;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionInfo;

import com.google.gson.Gson;

/**
 * Manager for the configuration data required for the various externally configurable Geoparser related applications.
 * <p>
 * The configuration file is loaded from a JSON document at object instantiation. The configuration manager supports a
 * list of labeled configuration strings, which can be queried using {@link #getConfigStringByLabel}. Furthermore, a
 * list of labeled database connection details can be queried using {@link #getDBConnectionInfoByLabel}.
 * <p>
 * The JSON document is organized as follows:
 *
 * <pre>
 * {
 *   "dbConnectionConfigurations": {
 *     "db_connection_label": {
 *     	GSON-representation of class {@link DBConnectionInfo}
 *     },
 *     ...
 *   },
 *   "configurationStrings": {
 *   	"configuration_label": "configuration_string",
 *   	...
 *   }
 * }
 * </pre>
 * <p>
 * Each application requiring configuration data should transparently define its required property fields by declaring
 * static final variables like e.g. <code>public static final String GAZETTEER_NAME_LABEL = "gazetteer.name";</code>.
 * Also they should provide their own logic to load the respective configuration data from the GeoParserConfig.
 *
 * @author lrichter
 *
 */
public class GeoparserConfig {

	public static class UnknownConfigLabelException extends Exception {

		private static final long serialVersionUID = -567213351621341954L;

		public UnknownConfigLabelException(final String message) {
			super(message);
		}
	}

	private static final class Configuration {
		public Map<String, String> configurationStrings;
		public Map<String, DBConnectionInfo> dbConnectionConfigurations;

		public Configuration(final Map<String, String> configurationStrings,
				final Map<String, DBConnectionInfo> dbConnectionConfigurations) {
			super();
			this.configurationStrings = configurationStrings;
			this.dbConnectionConfigurations = dbConnectionConfigurations;
		}
	}

	/**
	 * Default file name from which to load the configuration file.
	 */
	public static final String DEFAULT_CONFIG_FILE_NAME = "geoparser.config.json";

	private Configuration config;

	/**
	 * Create a {@link GeoparserConfig} instance and load the configuration from {@link #DEFAULT_CONFIG_FILE_NAME}.
	 *
	 * @throws IOException if loading the configuration file failed
	 */
	// Refactoring lrichter 16.03.2017: change behavior -> create empty config
	public GeoparserConfig() throws IOException {
		this(DEFAULT_CONFIG_FILE_NAME);
	}

	/**
	 * Create a {@link GeoparserConfig} instance and load the configuration from the given JSON document file name.
	 *
	 * @param configFileName name of the file, where the geoparser configuration is stored as JSON document. If null, no
	 *            configuration file is loaded. This is useful if a configuration should be created by
	 *            {@link #writeConfigToJsonFile(String)}.
	 * @throws IOException if loading the configuration file failed
	 */
	// Refactoring lrichter 16.03.2017: create a new class Geoparserconfig to make initialization transparent
	// here we load from a Resource path which is not clear to the user
	// also, empty config is created if no path provided; this is far from obvious
	public GeoparserConfig(final String configFileName) throws IOException {
		if (configFileName != null) {
			// see http://stackoverflow.com/a/14739608 for why we use getClassLoader()...
			try (final InputStream configFileStream = getClass().getClassLoader().getResourceAsStream(configFileName)) {
				if (configFileStream == null) {
					throw new IOException("Couldn't find configuration file `" + configFileName + "`!");
				}
				try (final BufferedReader reader = new BufferedReader(
						new InputStreamReader(configFileStream, StandardCharsets.UTF_8))) {
					final Gson gson = new Gson();
					config = gson.fromJson(reader, Configuration.class);
				}
			}
		}
		else {
			config = new Configuration(new HashMap<>(), new HashMap<>());
		}
	}

	/**
	 * Return the labels of all registered configuration strings.
	 *
	 * @return a set containing all registered configuration string labels
	 */
	public Set<String> getConfigStringLabels() {
		return config.configurationStrings.keySet();
	}

	/**
	 * Return the labels of all registered database connections.
	 *
	 * @return a set containing all registered database connection labels
	 */
	public Set<String> getDBConnectionInfoLabels() {
		return config.dbConnectionConfigurations.keySet();
	}

	/**
	 * Return the configuration string stored for the given label.
	 *
	 * @param label name of the configuration string
	 * @return the configuration string
	 * @throws UnknownConfigLabelException if the configuration string is not listed
	 */
	public String getConfigStringByLabel(final String label) throws UnknownConfigLabelException {
		if (!config.configurationStrings.containsKey(label)) {
			throw new UnknownConfigLabelException("No configuration value stored for `" + label + "`!");
		}

		return config.configurationStrings.get(label);
	}

	/**
	 * Return the database connection information stored for the given database connection label.
	 *
	 * @param label name of the database connection
	 * @return the database connection information associated with the database connection label
	 * @throws UnknownConfigLabelException if the database connection label is not listed
	 */
	public DBConnectionInfo getDBConnectionInfoByLabel(final String label) throws UnknownConfigLabelException {
		if (!config.dbConnectionConfigurations.containsKey(label)) {
			throw new UnknownConfigLabelException("No database connection information stored for `" + label + "`!");
		}

		return config.dbConnectionConfigurations.get(label);
	}

	/**
	 * Remove the configuration string with the given label from the configuration.
	 * <p>
	 * If the label is not listed, nothing happens.
	 *
	 * @param label the name of the configuration string to be removed
	 */
	public void removeConfigStringByLabel(final String label) {
		config.configurationStrings.remove(label);
	}

	/**
	 * Set the configuration string with the given label in the configuration.
	 * <p>
	 * If the label already exists, its associated configuration string is overwritten.
	 *
	 * @param label non-null name of the configuration string to be set
	 * @param configString the configuration string associated with the label
	 */
	public void setConfigString(final String label, final String configString) {
		Objects.requireNonNull(label);
		config.configurationStrings.put(label, configString);
	}

	/**
	 * Remove the database connection information with the given label from the configuration.
	 * <p>
	 * If the label is not listed, nothing happens.
	 *
	 * @param label the name of the database connection to be removed
	 */
	public void removeDBConnectionInfoByLabel(final String label) {
		config.dbConnectionConfigurations.remove(label);
	}

	/**
	 * Set the database connection information with the given label in the configuration.
	 * <p>
	 * If the label already exists, its associated database connection information is overwritten.
	 *
	 * @param label non-null name of the database connection to be set
	 * @param dbInfo the database connection information associated with the label
	 */
	public void setDBConnectionInfo(final String label, final DBConnectionInfo dbInfo) {
		Objects.requireNonNull(label);
		config.dbConnectionConfigurations.put(label, dbInfo);
	}

	/**
	 * Write the configuration to a JSON file.
	 *
	 * @param configFileName the file name to which the configuration should be written to
	 * @throws IOException if writing the configuration file failed
	 */
	public void writeConfigToJsonFile(final String configFileName) throws IOException {
		try (final FileOutputStream outputStream = new FileOutputStream(configFileName);
				final BufferedWriter writer = new BufferedWriter(
						new OutputStreamWriter(outputStream, StandardCharsets.UTF_8))) {
			final Gson gson = new Gson();
			gson.toJson(config, Configuration.class, writer);
		}
	}
}