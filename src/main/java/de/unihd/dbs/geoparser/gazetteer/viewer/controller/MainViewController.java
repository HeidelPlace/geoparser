package de.unihd.dbs.geoparser.gazetteer.viewer.controller;

import java.net.URL;
import java.util.ResourceBundle;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;

/**
 * This controller manages the main view of the "Gazetteer Viewer". It functions as a simple container and mediator
 * that links the embedded views.
 *
 * @author lrichter
 *
 */
public class MainViewController implements Initializable {

	@FXML
	private SearchViewController searchViewController;

	@FXML
	private OverviewViewController overviewViewController;

	@FXML
	private DetailsViewController detailsViewController;

	private int loadedWebMaps = 0;
	private boolean linked = false;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		// we need to delay linking the views until all web maps are fully loaded
		detailsViewController.addWebEngineStateListener((observable, oldValue, newValue) -> {
			if (newValue != Worker.State.SUCCEEDED) {
				return;
			}
			increaseLoadedWebEngines();
			if (loadedWebMaps == 2) {
				linkOverViewAndDetailsView();
			}
		});
		overviewViewController.addWebEngineStateListener((observable, oldValue, newValue) -> {
			if (newValue != Worker.State.SUCCEEDED) {
				return;
			}
			increaseLoadedWebEngines();
			if (loadedWebMaps == 2) {
				linkOverViewAndDetailsView();
			}
		});
	}

	private void increaseLoadedWebEngines() {
		loadedWebMaps += 1;
	}

	private synchronized void linkOverViewAndDetailsView() {
		// logic to ensure that linking is done only once
		if (linked) {
			return;
		}
		linked = true;

		overviewViewController.getCurrentPlaceObservableValue()
				.addListener((observable, oldValue, newValue) -> detailsViewController.showPlaceDetails(newValue));
		overviewViewController.setSearchResultsObservable(searchViewController.getSearchResultsObservable());
	}

}
