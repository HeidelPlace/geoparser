package de.unihd.dbs.geoparser.gazetteer.viewer.controller;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerResources;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.util.viewer.NoEditTextFieldTableCell;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.util.converter.LongStringConverter;

/**
 * This controller manages the view that displays the search result overview. This includes a table with the id and
 * place names per found place as well as an overview map with a default footprint per place.
 * <p>
 * In order to display search results an observable list containing the found places must be passed via
 * {@link #setSearchResultsObservable}.
 * <p>
 * Other controllers can be notified, which place (i.e., a row) is selected either in the table or the map. This is made
 * possible by observing the currently selected place using {@link #getCurrentPlaceObservableValue}.
 *
 * @author lrichter
 *
 */
/*
 * Technical details on how the Map visualization and interplay works:
 * http://captaincasa.blogspot.de/2014/01/javafx-and-osm-openstreetmap.html
 * https://docs.oracle.com/javase/8/javafx/embedded-browser-tutorial/js-javafx.htm
 */
// XXX: sometimes the WebView control refuses to work after a while; it's unclear when this happens exactly
public class OverviewViewController implements Initializable {

	private static final int DEFAULT_MAP_ZOOM = 1;

	@FXML
	private TableView<Place> overviewTable;

	@FXML
	private TableColumn<Place, Long> placeIdColumn;

	@FXML
	private TableColumn<Place, String> placeNameColumn;

	@FXML
	private WebView mapWebView;

	private ViewerContext appContext;
	private WebEngine webEngine;

	@Override
	public void initialize(final URL arg0, final ResourceBundle arg1) {
		appContext = ViewerContext.getInstance();
		initTable();
		initMap();
	}

	/**
	 * Return an {@link ObservableValue} that represents the currently selected place.
	 * <p>
	 * Can be used to react on place selection for displaying place details.
	 *
	 * @return an {@link ObservableValue} that represents the currently selected place.
	 */
	public ObservableValue<Place> getCurrentPlaceObservableValue() {
		return overviewTable.getSelectionModel().selectedItemProperty();
	}

	/**
	 * Set the {@link ObservableList} of places found during gazetteer search.
	 *
	 * @param searchResults the search results to listen to
	 */
	@SuppressWarnings("unchecked")
	public void setSearchResultsObservable(final ObservableList<Place> searchResults) {
		overviewTable.setItems(searchResults);
		overviewTable.getItems().addListener((final Change<? extends Place> listChange) -> {
			while (listChange.next()) {
				for (final Place place : listChange.getRemoved()) {
					removePlaceLocation(place);
				}
				for (final Place place : listChange.getAddedSubList()) {
					addPlaceLocation(place);
				}
				centerMap((List<Place>) listChange.getList());
			}
		});
	}

	public void addWebEngineStateListener(final ChangeListener<? super State> listener) {
		webEngine.getLoadWorker().stateProperty().addListener(listener);
	}

	private void initTable() {
		placeIdColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getId()));
		placeNameColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(getDisplayName(cellData.getValue())));
		placeIdColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn(new LongStringConverter()));
		placeNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		overviewTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> highlightPlaceLocation(newValue));
	}

	private void initMap() {
		webEngine = mapWebView.getEngine();
		// load HTML page showing a world map using Leaflet
		// http://stackoverflow.com/questions/23683350/javafx-webview-load-local-css-files
		webEngine.load(
				getClass().getClassLoader().getResource(GazetteerViewerResources.OSM_MAP_HTML_FILE).toExternalForm());

		// hack to implement a JavaScript callback to Java
		// since webEngine.executeScript("window").setMember() uses WeakReferences, callbacks via a bridging class do
		// not work reliably (see http://stackoverflow.com/q/41903154/7480395). I therefore decided to use
		// webEngine.setOnAlert() instead, which works fine all the time
		webEngine.setOnAlert(handler -> {
			final Long id = Long.parseLong(handler.getData());
			selectPlace(id);
		});
	}

	private String getDisplayName(final Place place) {
		// we use a separate SQL query to fetch the names efficiently as plain strings! otherwise, lots of JPA querying
		// is going on to load PlaceName instances, which we don't need yet. This approach greatly speeds up displaying
		// performance after the search has finished!
		@SuppressWarnings("unchecked")
		final List<String> placeNames = appContext.gazetteer.getEntityManger()
				.createNativeQuery("SELECT name FROM place_name WHERE place_id = " + place.getId()).getResultList();
		return placeNames.stream().collect(Collectors.joining(", "));
	}

	private void highlightPlaceLocation(final Place place) {
		if (place == null) {
			return;
		}

		final Geometry geometry = getCentroidOfFirstPlaceFootprint(place);
		if (geometry != null) {
			final double longitude = geometry.getCoordinate().x;
			final double latitude = geometry.getCoordinate().y;

			webEngine.executeScript(
					"jumpTo(" + longitude + "," + latitude + ", Math.max(getZoom(), " + DEFAULT_MAP_ZOOM + "))");
			webEngine.executeScript("popupFootprint(" + place.getId() + ")");
		}
	}

	private void selectPlace(final Long placeId) {
		final Place focusedPlace = overviewTable.getItems().stream().filter(place -> place.getId().equals(placeId))
				.findFirst().get();
		overviewTable.getSelectionModel().select(focusedPlace);
		overviewTable.scrollTo(focusedPlace);
	}

	private void removePlaceLocation(final Place place) {
		webEngine.executeScript("removeFootprint(" + place.getId() + ")");
	}

	private void addPlaceLocation(final Place place) {
		final Geometry geometry = getCentroidOfFirstPlaceFootprint(place);
		if (geometry != null) {
			webEngine.executeScript("addFootprint(" + place.getId() + ", '" + geometry.toText() + "'," + "'"
					+ ViewerUtils.getPreferredName(place) + "')");
		}
	}

	private void centerMap(final List<Place> places) {
		final List<Geometry> centroids = places.stream().map(place -> getCentroidOfFirstPlaceFootprint(place))
				.filter(centroid -> centroid != null).collect(Collectors.toList());

		if (!centroids.isEmpty()) {
			final Envelope minimumBoundingBox = new Envelope();
			centroids.forEach(footprint -> minimumBoundingBox.expandToInclude(footprint.getEnvelopeInternal()));
			webEngine.executeScript("jumpTo(" + minimumBoundingBox.centre().x + "," + minimumBoundingBox.centre().y
					+ "," + DEFAULT_MAP_ZOOM + ")");
		}
	}

	private static Point getCentroidOfFirstPlaceFootprint(final Place place) {
		if (place.getFootprints().isEmpty()) {
			return null;
		}
		return place.getFootprints().iterator().next().getGeometry().getCentroid();
	}

}
