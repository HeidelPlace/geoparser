package de.unihd.dbs.geoparser.gazetteer.viewer;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.unihd.dbs.geoparser.core.GeoparserConfig;
import de.unihd.dbs.geoparser.gazetteer.Gazetteer;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.LoginViewController;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.MainViewController;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.stage.Stage;

/**
 * Entry point for the "Gazetteer Viewer", a graphical user interface (using JavaFX) that allows to query a
 * {@link Gazetteer} compatible data-source and display the search results.
 * <p>
 * When the application is started, the user is first prompted for login credentials to the gazetteer data-source. If
 * a connection to the gazetteer could be established successfully, the main view of the application is shown. If the
 * application is closed by the user or due to an unrecoverable exception, any resources allocated for the gazetteer
 * connection are released before the application is terminated.
 *
 * @author lrichter
 *
 */
public class GazetteerViewerApp extends Application {

	private static final String MAIN_VIEW_TITLE = "Gazetteer Viewer";
	private static final Logger logger = LoggerFactory.getLogger(GazetteerViewerApp.class);

	private ViewerContext appContext;
	private View<LoginViewController> loginView;
	private View<MainViewController> mainView;
	private Stage primaryStage;

	/**
	 * Entry point for launching the "Gazetteer Viewer" GUI.
	 *
	 * @param args the command line arguments passed to the application will be ignored.
	 */
	public static void main(final String[] args) {
		launch(args);
	}

	@Override
	public void start(final Stage primaryStage) {
		this.primaryStage = primaryStage;
		primaryStage.setTitle(MAIN_VIEW_TITLE);
		appContext = ViewerContext.getInstance();
		installUncaughtExceptionHandler();
		loadGeoparserConfig();
		runGUI();
	}

	@Override
	public void stop() {
		releaseResources();
	}

	private static void installUncaughtExceptionHandler() {
		Thread.currentThread().setUncaughtExceptionHandler((thread, throwable) -> {
			showAndLogError("An unhandled exception occurred!", throwable);
		});
	}

	private void loadGeoparserConfig() {
		try {
			appContext.config = new GeoparserConfig();
		}
		catch (final IOException e) {
			showAndLogError("Failed to load the geoparsing configuration!", e);
			Platform.exit();
		}
	}

	private final ChangeListener<Boolean> loginChangeListener = (observable, oldValue, newValue) -> {
		if (newValue == true) {
			mainView = initView(GazetteerViewerResources.MAIN_VIEW_FXML_FILE, "main");
			if (mainView != null) {
				showView(mainView, true, true);
			}
		}
	};

	private void runGUI() {
		loginView = initView(GazetteerViewerResources.LOGIN_VIEW_FXML_FILE, "login");
		loginView.getController().getLoggedInObservable().addListener(loginChangeListener);
		if (loginView != null) {
			showView(loginView, false, false);
		}
	}

	private static <T> View<T> initView(final String viewResourcePath, final String viewName) {
		try {
			return new View<>(viewResourcePath);
		}
		catch (final IOException e) {
			showAndLogError("An error occurred when trying to load the " + viewName + " view!", e);
			Platform.exit();
		}
		return null;
	}

	private <T> void showView(final View<T> view, final boolean resizable, final boolean maximized) {
		view.showViewAsScene(primaryStage);
		primaryStage.setResizable(resizable);
		primaryStage.setMaximized(maximized);
	}

	private void releaseResources() {
		if (appContext.gazetteer != null) {
			try {
				appContext.gazetteer.close();
			}
			catch (final Exception e) {
				showAndLogError("An error occurred while shutting down the gazetteer!", e);
			}
		}
		if (appContext.gazetteerPersistenceManager != null) {
			appContext.gazetteerPersistenceManager.close();
		}
	}

	private static void showAndLogError(final String message, final Throwable t) {
		logger.error(message, t);
		ViewerUtils.showErrorDialog(t, message);
	}

}
