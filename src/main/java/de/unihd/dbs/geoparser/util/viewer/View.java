package de.unihd.dbs.geoparser.util.viewer;

import java.io.IOException;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

/**
 * This class represents a view in a JavaFX user interface that was loaded via {@link FXMLLoader} from a FXML file.
 * <p>
 * A view consists of a scene's root node and its controller instance.
 *
 * @param <T> type of the view controller.
 *
 * @author lrichter
 */
public class View<T> {
	private final Parent rootNode;
	private final T controller;

	public View(final Parent rootNode, final T controller) {
		super();
		this.rootNode = rootNode;
		this.controller = controller;
	}

	public Parent getRootNode() {
		return rootNode;
	}

	public T getController() {
		return controller;
	}

	/**
	 * Load a view from the given FXML file.
	 *
	 * @param viewResourcePath the resource path pointing to a FXML file from which to load the view
	 * @throws IOException if something goes wrong while loading the FXML file
	 */
	public View(final String viewResourcePath) throws IOException {
		final FXMLLoader loader = new FXMLLoader();
		loader.setLocation(getClass().getClassLoader().getResource(viewResourcePath));
		if (loader.getLocation() == null) {
			throw new IOException(String.format("Could not find the FXML resource `%s`", viewResourcePath));
		}
		rootNode = (Parent) loader.load();
		controller = loader.getController();
	}

	/**
	 * Show the view as {@link Scene} in the given {@link Stage}.
	 * <p>
	 * The stage is always centered on the screen.
	 *
	 * @param stage the stage in which this view should be shown as new scene.
	 */
	public void showViewAsScene(final Stage stage) {
		showViewAsScene(stage, false);
	}

	/**
	 * Show the view as {@link Scene} in the given {@link Stage}.
	 * <p>
	 * The stage is always centered on the screen.
	 *
	 * @param stage the stage in which this view should be shown as new scene.
	 * @param modal if <code>true</code>, the scene is shown modal (i.e., {@link Modality#APPLICATION_MODAL}).
	 */
	public void showViewAsScene(final Stage stage, final boolean modal) {
		stage.setScene(new Scene(rootNode));
		stage.centerOnScreen();
		if (modal) {
			stage.initModality(Modality.APPLICATION_MODAL);
		}

		stage.show();
	}

}