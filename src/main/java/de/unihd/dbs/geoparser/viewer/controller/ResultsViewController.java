package de.unihd.dbs.geoparser.viewer.controller;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import de.unihd.dbs.geoparser.viewer.GeoparserViewerResources;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingRun;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.layout.VBox;

/**
 * This controller manages the results view. For each geoparsing module combination a separate result view is
 * maintained.
 *
 * @author lrichter
 *
 */
public class ResultsViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ResultsViewController.class);

	@FXML
	private VBox resultsVBox;

	private Map<GeoparsingRun, View<ResultViewController>> resultViews;

	private final ChangeListener<? super Boolean> finishedGeoparsingListener = (observable, oldValue, newValue) -> {
		if (newValue == true) {
			updateGeoparsingResults();
			resultsVBox.setDisable(false);
		}
		else {
			resultsVBox.setDisable(true);
		}
	};

	private final ListChangeListener<? super GeoparsingRun> geoparsingRunChangeListener = (listChange) -> {
		while (listChange.next()) {
			for (final GeoparsingRun geoparsingRun : listChange.getRemoved()) {
				removeResultView(geoparsingRun);
			}
			for (final GeoparsingRun geoparsingRun : listChange.getAddedSubList()) {
				addResultView(geoparsingRun);
			}
		}
	};

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		resultViews = new HashMap<>();
	}

	public ListChangeListener<? super GeoparsingRun> getGeoparsingRunChangeListener() {
		return geoparsingRunChangeListener;
	}

	public ChangeListener<? super Boolean> getFinishedGeoparsingListener() {
		return finishedGeoparsingListener;
	}

	private void addResultView(final GeoparsingRun geoparsingRun) {
		View<ResultViewController> resultView;
		try {
			resultView = new View<>(GeoparserViewerResources.RESULT_VIEW_FXML_FILE);
			resultsVBox.getChildren().add(resultView.getRootNode());
			resultView.getController().setGeoparsingInfo(geoparsingRun);
			resultViews.put(geoparsingRun, resultView);
		}
		catch (final IOException e) {
			logger.error("An error occurred when trying to load the result view!", e);
			ViewerUtils.showErrorDialog(e, "An error occurred when trying to load the result view!");
		}
	}

	private void removeResultView(final GeoparsingRun geoparsingRun) {
		resultsVBox.getChildren().remove(resultViews.remove(geoparsingRun).getRootNode());
	}

	private void updateGeoparsingResults() {
		for (final View<ResultViewController> resultView : resultViews.values()) {
			resultView.getController().updateGeoparsingResults();
		}

	}

}
