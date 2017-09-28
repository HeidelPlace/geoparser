package de.unihd.dbs.geoparser.viewer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.LoginViewController;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import de.unihd.dbs.geoparser.viewer.controller.MainViewController;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;

/**
 * Entry point for a Geoparser GUI (using JavaFX).
 * <p>
 * When the application is started, the user is first prompted for login credentials to the gazetteer data-source. If
 * a connection to the gazetteer could be established successfully, the main view of the application is shown. If the
 * application is closed by the user or due to an unrecoverable exception, any resources allocated for the gazetteer
 * connection are released before the application is terminated.
 *
 * @author lrichter
 *
 */
// Refactoring lrichter 20.03.2017: duplication of GazetteerViewerApp. Centralize to single configurable class
public class GeoparserViewerApp extends Application {

	private static final Logger logger = LoggerFactory.getLogger(GeoparserViewerApp.class);
	private static final String MAIN_VIEW_TITLE = "Geoparser Viewer";

	private ViewerContext appContext;
	private View<LoginViewController> loginView;
	private View<MainViewController> mainView;
	private Stage primaryStage;

	/**
	 * Entry point for launching the "Geoparser" GUI .
	 *
	 * @param args the command line arguments passed to the application will be ignored.
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) throws IOException {
		this.primaryStage = primaryStage;
		primaryStage.setTitle(MAIN_VIEW_TITLE);
		appContext = ViewerContext.getInstance();
		installUncaugtExceptionHandler();
		loadGeoparserConfig();
		initLoginView();
		runGUI();
	}

	@Override
	public void stop() {
		logger.info("Releasing resources before stopping application...");
		releaseResources();
		logger.info("Bye!");
	}

	private static void installUncaugtExceptionHandler() {
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
			showAndLogError("An unhandled exception occurred!", throwable);
		});
	}

	private void loadGeoparserConfig() {
		try {
			logger.info("Loading geoparser configuration...");
			appContext.config = new GeoparserConfig();
		}
		catch (final IOException e) {
			showAndLogError("Failed to load the geoparsing configuration!", e);
			Platform.exit();
		}
	}

	private final ChangeListener<Boolean> loginChangeListener = (observable, oldValue, newValue) -> {
		if (newValue == true) {
			loginView = null;
			initMainView();
			showMainView();
		}
	};

	private void runGUI() {
		initLoginView();
		loginView.getController().getLoggedInObservable().addListener(loginChangeListener);
		showLoginView();
	}

	private void initLoginView() {
		try {
			loginView = new View<>(GeoparserViewerResources.LOGIN_VIEW_FXML_FILE);
		}
		catch (final IOException e) {
			showAndLogError("An error occurred when trying to load the login view!", e);
			Platform.exit();
		}
	}

	private void initMainView() {
		try {
			mainView = new View<>(GeoparserViewerResources.MAIN_VIEW_FXML_FILE);
		}
		catch (final IOException e) {
			showAndLogError("An error occurred when trying to load the main view!", e);
			Platform.exit();
		}
	}

	private void showLoginView() {
		loginView.showViewAsScene(primaryStage);
		primaryStage.setResizable(false);
	}

	private void showMainView() {
		mainView.showViewAsScene(primaryStage);
		primaryStage.setResizable(true);
		primaryStage.setMaximized(true);
	}

	private void releaseResources() {
		if (appContext.gazetteer != null) {
			logger.info("Shutting down the gazetteer...");
			try {
				appContext.gazetteer.close();
			}
			catch (final Exception e) {
				showAndLogError("An error occurred while shutting down the gazetteer!", e);
			}
		}
		if (appContext.gazetteerPersistenceManager != null) {
			logger.info("Closing the JPA persistence manager...");
			appContext.gazetteerPersistenceManager.close();
		}
	}

	private static void showAndLogError(final String message, final Throwable t) {
		logger.error(message, t);
		ViewerUtils.showErrorDialog(t, message);
	}

}
