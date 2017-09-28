package de.unihd.dbs.geoparser.gazetteer.viewer.controller;

import java.net.ConnectException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ResourceBundle;

import org.postgresql.util.PSQLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;

import de.unihd.dbs.geoparser.core.GeoparserConfig.UnknownConfigLabelException;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.util.GazetteerPersistenceManager;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionData;
import de.unihd.dbs.geoparser.util.dbconnectors.DBConnectionInfo;
import de.unihd.dbs.geoparser.util.dbconnectors.SSHConnectionData;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;

/**
 * This controller manages the login view. It connects to the gazetteer source based on the user-provided connection
 * details. Other controllers can wait for a successful login by retrieving a login observable via
 * {@link #getLoggedInObservable} and adding a change-listener to it.
 *
 * @author lrichter
 *
 */
public class LoginViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(LoginViewController.class);

	@FXML
	private AnchorPane loginPane;

	@FXML
	private TextField persistenceUnitField;

	@FXML
	private TextField hostField;

	@FXML
	private TextField portField;

	@FXML
	private TextField databaseField;

	@FXML
	private TextField userNameField;

	@FXML
	private PasswordField passwordField;

	@FXML
	private Button loginButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label infoLabel;

	ViewerContext appContext;
	private Task<Void> loginTask;
	private final BooleanProperty loggedInProperty = new SimpleBooleanProperty(false);

	/**
	 * Return an {@link ObservableValue} that represents the current login success.
	 * <p>
	 * Can be used to react on successful gazettteer login.
	 *
	 * @return an {@link ObservableValue} that represents the current login success.
	 */
	public ObservableValue<Boolean> getLoggedInObservable() {
		return loggedInProperty;
	}

	@Override
	public void initialize(final URL arg0, final ResourceBundle arg1) {
		appContext = ViewerContext.getInstance();

		initControls();
		setViewToReadyToConnect();
		setLoginCredentialsFromConfig();
	}

	private final ChangeListener<String> portFieldChangeListener = (observable, oldValue, newValue) -> {
		// Refactoring lrichter 17.03.2017: create extra UI class for numeric input?
		// force the port field to be numeric only
		if (!newValue.matches("\\d*")) {
			portField.setText(newValue.replaceAll("[^\\d]", ""));
		}
	};

	private void initControls() {
		portField.textProperty().addListener(portFieldChangeListener);
	}

	private void setViewToConnecting() {
		infoLabel.setText("");
		progressIndicator.setVisible(true);
		loginButton.setText("Cancel");
		loginButton.setOnAction(event -> cancelLogin());
	}

	private void setViewToReadyToConnect() {
		progressIndicator.setVisible(false);
		loginButton.setText("Connect");
		loginButton.setOnAction(event -> login());
	}

	private void setLoginCredentialsFromConfig() {
		try {
			logger.debug("Trying to load login credentials from GeoparserConfig...");
			persistenceUnitField.setText(
					appContext.config.getConfigStringByLabel(GazetteerPersistenceManager.PERSISTENCE_UNIT_NAME_LABEL));
			final DBConnectionInfo dbInfo = appContext.config.getDBConnectionInfoByLabel(appContext.config
					.getConfigStringByLabel(GazetteerPersistenceManager.PERSISTENCE_UNIT_DB_CONNECTION_INFO_LABEL));
			final DBConnectionData dbData = dbInfo.dbConnectionData;
			hostField.setText(dbData.host);
			portField.setText(String.valueOf(dbData.port));
			databaseField.setText(dbData.dbName);
			userNameField.setText(dbData.userName);
			passwordField.setText(String.valueOf(dbData.password));
			logger.debug("Successfully loaded credentials from GeoparserConfig.");
		}
		catch (final UnknownConfigLabelException e) {
			infoLabel.setText("Could not load default credentials from GeoparserConfig! " + e.getMessage());
			logger.error("An error occurred when trying to load the login credentials!", e);
		}
	}

	private void login() {
		int port;

		try {
			port = Integer.parseInt(portField.getText());
		}
		catch (final NumberFormatException e) {
			infoLabel.setText("Port must be a number!");
			return;
		}

		final DBConnectionData dbData = new DBConnectionData(databaseField.getText(), hostField.getText(), port,
				userNameField.getText(), passwordField.getText().toCharArray(), true);
		// Refactoring lrichter 23.03.2017: change the way DBConnectionInfo works: allow null for SSHConnectionData
		final DBConnectionInfo dbInfo = new DBConnectionInfo(dbData,
				new SSHConnectionData("", 0, "", null, null, false, null));

		loginTask = createLoginTask(dbInfo);
		new Thread(loginTask).start();

		setViewToConnecting();
	}

	private Task<Void> createLoginTask(final DBConnectionInfo dbInfo) {
		final Task<Void> loginTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				logger.info("Starting the JPA persistence manager...");
				appContext.gazetteerPersistenceManager = new GazetteerPersistenceManager(persistenceUnitField.getText(),
						dbInfo);
				logger.info("Starting the gazetteer...");
				appContext.gazetteer = new Gazetteer(appContext.gazetteerPersistenceManager.getEntityManager());
				// FEATURE_REQUEST lrichter: we should ensure there also that we have sufficient access privileges
				// (this is not trivial though since getting the tables names for each JPA entity is not so easy)
				return null;
			}
		};
		loginTask.setOnSucceeded(eventHandler -> loginSucceeded());
		loginTask.setOnFailed(eventHandler -> loginFailed());
		loginTask.setOnCancelled(eventHandler -> loginCancelled());
		return loginTask;
	}

	private void cancelLogin() {
		loginTask.cancel();
	}

	private void loginSucceeded() {
		logger.info("Login to gazetteer was successful!");
		cleanupListeners();
		loggedInProperty.set(true);
	}

	/*
	 * We need to remove all listeners so memory will be released when the LoginView is closed (lambdas contain
	 * references to "this", preventing GC to kick in). see http://stackoverflow.com/a/28447015
	 */
	private void cleanupListeners() {
		loginTask = null; // removes all login handlers at once...
		portField.textProperty().removeListener(portFieldChangeListener);
		loginButton.setOnAction(null);
	}

	private void loginCancelled() {
		logger.info("Login to gazetteer was cancelled!");
		infoLabel.setText("Login was cancelled!");
		setViewToReadyToConnect();
	}

	private void loginFailed() {
		final Throwable e = loginTask.getException();
		final Throwable rootCause = Throwables.getRootCause(e);
		String errorMessage = e.getMessage();

		if (rootCause instanceof UnknownHostException) {
			errorMessage = "Unknown host `" + rootCause.getMessage() + "`.";
		}
		else if (rootCause instanceof ConnectException) {
			errorMessage = "Failed to connect to database server. Check your host and port.";
		}
		else if (rootCause instanceof PSQLException) {
			errorMessage = "Could not connect to database. Check the database name and credentials.";
		}

		logger.error("An error occurred when trying login!", e);
		infoLabel.setText("Login failed! " + errorMessage);
		setViewToReadyToConnect();
	}

}
