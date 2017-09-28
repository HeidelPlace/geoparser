package de.unihd.dbs.geoparser.viewer.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;

/**
 * This controller manages the main view of the "Geoparser Viewer". It functions as a simple container and mediator
 * that links the embedded views.
 *
 * @author lrichter
 *
 */
public class MainViewController implements Initializable {

	@FXML
	private GeoparsingViewController geoparsingViewController;

	@FXML
	private ResultsViewController resultsViewController;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		geoparsingViewController.getGeoparsingRunsObservable()
				.addListener(resultsViewController.getGeoparsingRunChangeListener());
		geoparsingViewController.getFinishedGeoparsingObservable()
				.addListener(resultsViewController.getFinishedGeoparsingListener());
	}

}
