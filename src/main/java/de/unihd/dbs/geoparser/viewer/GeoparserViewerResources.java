package de.unihd.dbs.geoparser.viewer;

/**
 * This class centralizes the resource paths for all FXML and HTML files used in the {@link GeoparserViewerApp}
 * application.
 *
 * @author lrichter
 *
 */
public class GeoparserViewerResources {

	public static final String FXML_DIRECTORY = "ui/geoparser_viewer/";
	public static final String DETAILS_FXML_FILE = "ui/gazetteer_viewer/DetailsView.fxml";
	public static final String LOGIN_VIEW_FXML_FILE = "ui/gazetteer_viewer/LoginView.fxml";
	public static final String GEOPARSING_VIEW_FXML_FILE = FXML_DIRECTORY + "GeoparsingView.fxml";
	public static final String MAIN_VIEW_FXML_FILE = FXML_DIRECTORY + "MainView.fxml";
	public static final String RESULT_VIEW_FXML_FILE = FXML_DIRECTORY + "ResultView.fxml";
	public static final String RESULTS_VIEW_FXML_FILE = FXML_DIRECTORY + "ResultsView.fxml";
	public static final String RESULT_VISUALIZATION_HTML_FILE = FXML_DIRECTORY + "Results_Visualization.html";
	public static final String OSM_MAP_HTML_FILE = "ui/osm_map/osm_map.html";

	private GeoparserViewerResources() {
		// should not be instantiated
	}

}
