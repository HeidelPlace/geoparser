package de.unihd.dbs.geoparser.gazetteer.viewer;

/**
 * This class centralizes the resource paths for all FXML and HTML files used in the {@link GazetteerViewerApp}
 * application.
 *
 * @author lrichter
 *
 */
public class GazetteerViewerResources {

	public static final String FXML_DIRECTORY = "ui/gazetteer_viewer/";
	public static final String MAIN_VIEW_FXML_FILE = FXML_DIRECTORY + "MainView.fxml";
	public static final String LOGIN_VIEW_FXML_FILE = FXML_DIRECTORY + "LoginView.fxml";
	public static final String DETAILS_FXML_FILE = FXML_DIRECTORY + "DetailsView.fxml";
	public static final String OVERVIEW_VIEW_FXML_FILE = FXML_DIRECTORY + "OverviewView.fxml";
	public static final String SEARCH_VIEW_FXML_FILE = FXML_DIRECTORY + "SearchView.fxml";
	public static final String OSM_MAP_HTML_FILE = "ui/osm_map/osm_map.html";

	private GazetteerViewerResources() {
		// should not be instantiated
	}

}
