package de.unihd.dbs.geoparser.viewer.controller;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.NamedEntityType;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerResources;
import de.unihd.dbs.geoparser.gazetteer.viewer.controller.DetailsViewController;
import de.unihd.dbs.geoparser.util.StringUtil;
import de.unihd.dbs.geoparser.util.viewer.NoEditTextFieldTableCell;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import de.unihd.dbs.geoparser.viewer.GeoparserViewerResources;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingRun;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingStep;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Worker;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.LongStringConverter;

/**
 * This controller manages a result view.
 *
 * @author lrichter
 *
 */
public class ResultViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(ResultViewController.class);

	private static final int DEFAULT_MAP_ZOOM = 1;

	private static class NamedEntityTypeToStringConverter extends StringConverter<NamedEntityType> {
		@Override
		public NamedEntityType fromString(final String string) {
			return NamedEntityType.valueOf(string);
		}

		@Override
		public String toString(final NamedEntityType namedEntityType) {
			return namedEntityType.name;
		}
	}

	@FXML
	private AnchorPane resultAnchorPane;

	@FXML
	private Label geoparserLabel;

	@FXML
	private Label resultInfoLabel;

	@FXML
	private WebView resultsVisualizationWebView;

	@FXML
	private TableView<NamedEntity> resultsTableView;

	@FXML
	private TableColumn<NamedEntity, String> namedEntityTableColumn;

	@FXML
	private TableColumn<NamedEntity, NamedEntityType> namedEntityTypeTableColumn;

	@FXML
	private TableView<Place> linkedPlacesTableView;

	@FXML
	private TableColumn<Place, Long> linkedPlaceIdTableColumn;

	@FXML
	private WebView mapWebView;

	private GeoparsingRun geoparsingRun;
	private WebEngine webEngine;
	private final ObservableList<NamedEntity> mixedEntities = FXCollections.observableArrayList();

	public void setGeoparsingInfo(final GeoparsingRun geoparsingRun) {
		this.geoparsingRun = geoparsingRun;
		this.geoparserLabel.setText(geoparsingRun.geoparsingApproach.recognitionModule + ", "
				+ geoparsingRun.geoparsingApproach.linkingModule + ", "
				+ geoparsingRun.geoparsingApproach.disambiguationModule);
		updateGeoparsingResultInfo();
	}

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		initControls();
		resultsTableView.setItems(mixedEntities);
	}

	private void initControls() {
		initNamedEntityTableColumns();
		initResultsTable();
		initLinkedPlacesTable();
		initResultsVisualization();
		initMap();
	}

	private void initNamedEntityTableColumns() {
		namedEntityTableColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().text));
		namedEntityTableColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		namedEntityTypeTableColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().type));
		namedEntityTypeTableColumn
				.setCellFactory(NoEditTextFieldTableCell.forTableColumn(new NamedEntityTypeToStringConverter()));
	}

	private void initResultsTable() {
		resultsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
			List<Place> relatedPlaces = new ArrayList<>();
			if (newValue instanceof LinkedToponym) {
				relatedPlaces = ((LinkedToponym) newValue).gazetteerEntries;
			}
			else if (newValue instanceof ResolvedToponym) {
				relatedPlaces = Arrays.asList(((ResolvedToponym) newValue).resolvedLocation.gazetteerEntry);
			}
			linkedPlacesTableView.setItems(FXCollections.observableArrayList(relatedPlaces));
			updatePlaceLocations();
			if (newValue != null) {
				resultsVisualizationWebView.getEngine()
						.executeScript("selectNE(" + mixedEntities.indexOf(newValue) + ")");
			}
		});
	}

	private void initLinkedPlacesTable() {
		linkedPlaceIdTableColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getId()));
		linkedPlaceIdTableColumn.setCellFactory(new Callback<TableColumn<Place, Long>, TableCell<Place, Long>>() {
			@Override
			public TableCell<Place, Long> call(final TableColumn<Place, Long> p) {
				final TableCell<Place, Long> cell = new NoEditTextFieldTableCell<>(new LongStringConverter());

				cell.addEventFilter(MouseEvent.MOUSE_CLICKED, new EventHandler<MouseEvent>() {
					@Override
					public void handle(final MouseEvent event) {
						final Place selectedPlace = linkedPlacesTableView.getSelectionModel().getSelectedItem();
						if (event.getClickCount() > 1 && selectedPlace != null) {
							showDetailsView(selectedPlace);
						}
					}
				});
				return cell;
			}
		});
		linkedPlacesTableView.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> {
					jumpToPlaceLocation(newValue);
				});
	}

	private void initResultsVisualization() {
		final WebEngine webEngine = resultsVisualizationWebView.getEngine();
		webEngine.load(getClass().getClassLoader().getResource(GeoparserViewerResources.RESULT_VISUALIZATION_HTML_FILE)
				.toExternalForm());
		webEngine.setOnAlert(handler -> {
			final NamedEntity entity = mixedEntities.get(Integer.parseInt(handler.getData()));
			resultsTableView.getSelectionModel().select(entity);
			resultsTableView.scrollTo(entity);
		});
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
			final NamedEntity selectedEntity = resultsTableView.getSelectionModel().getSelectedItem();

			if (selectedEntity instanceof LinkedToponym) {
				final List<Place> places = new ArrayList<>();
				((LinkedToponym) selectedEntity).gazetteerEntries.forEach(place1 -> {
					if (place1.getId() == Integer.parseInt(handler.getData())){
						 places.add(place1);
					}
				});

				linkedPlacesTableView.getSelectionModel().select(places.get(0));
				linkedPlacesTableView.scrollTo(places.get(0));
			}
			else if (selectedEntity instanceof ResolvedToponym) {
				final Place place = ((ResolvedToponym) selectedEntity).resolvedLocation.gazetteerEntry;
				linkedPlacesTableView.getSelectionModel().select(place);
				linkedPlacesTableView.scrollTo(place);
			}

		});
	}

	public void updateGeoparsingResults() {
		updateMixedEntities();
		updateGeoparsingResultInfo();
		updateNamedEntityVisualization();
	}

	private void updateMixedEntities() {
		mixedEntities.clear();
		mixedEntities.addAll(geoparsingRun.results.foundEntities);
		// XXX: very ugly code; right now I don't know a better solution, though. Would need to change everything!
		// direct equalsTo() does not work!
		for (final LinkedToponym linkedToponym : geoparsingRun.results.linkedToponyms) {
			for (int i = 0; i < mixedEntities.size(); i++) {
				final NamedEntity mixedEntity = mixedEntities.get(i);
				if (linkedToponym.beginPosition == mixedEntity.beginPosition
						&& linkedToponym.endPosition == mixedEntity.endPosition
						&& linkedToponym.text.equals(mixedEntity.text) && linkedToponym.type.equals(mixedEntity.type)) {
					mixedEntities.set(i, linkedToponym);
					break;
				}
			}
		}

		for (final ResolvedToponym resolvedToponym : geoparsingRun.results.resolvedToponyms) {
			for (int i = 0; i < mixedEntities.size(); i++) {
				final NamedEntity mixedEntity = mixedEntities.get(i);
				if (resolvedToponym.beginPosition == mixedEntity.beginPosition
						&& resolvedToponym.endPosition == mixedEntity.endPosition
						&& resolvedToponym.text.equals(mixedEntity.text)
						&& resolvedToponym.type.equals(mixedEntity.type)) {
					mixedEntities.set(i, resolvedToponym);
					break;
				}
			}
		}
	}

	private void updateGeoparsingResultInfo() {
		this.resultInfoLabel.setText(geoparsingRun.lastStep.equals(GeoparsingStep.NONE) ? "No geoparsing performed yet."
				: geoparsingRun.lastStep + " took " + geoparsingRun.stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms");
	}

	private void updateNamedEntityVisualization() {
		resultsVisualizationWebView.getEngine().executeScript("clearNEs()");
		int lastWordStart = 0;
		int toponymIndex = 0;

		for (final NamedEntity namedEntity : mixedEntities) {
			if (namedEntity.beginPosition - lastWordStart > 0) {
				final String textBeforeEntity = geoparsingRun.results.inputText
						.substring(lastWordStart, namedEntity.beginPosition).replace("\n", "<br>");
				if (textBeforeEntity.length() > 0) {
					resultsVisualizationWebView.getEngine()
							.executeScript("addNE('" + textBeforeEntity.replaceAll("'", "\\\\\'") + "', 'none', null)");
				}
			}

			resultsVisualizationWebView.getEngine().executeScript("addNE('" + namedEntity.text.replaceAll("'", "\\\\\'") + "', '"
					+ StringUtil.toLowerCase(namedEntity.type.name).replaceAll("'", "\\\\\'") + "', " + toponymIndex + ")");

			lastWordStart = namedEntity.endPosition;
			toponymIndex++;
		}

		if (lastWordStart < geoparsingRun.results.inputText.length() - 1) {
			final String textAfterLastEntity = geoparsingRun.results.inputText.substring(lastWordStart).replace("\n",
					"<br>");
			if (textAfterLastEntity.length() > 0) {
				resultsVisualizationWebView.getEngine()
						.executeScript("addNE('" + textAfterLastEntity.replaceAll("'", "\\\\\'") + "', 'none', null)");
			}
		}
	}

	private void updatePlaceLocations() {
		mapWebView.getEngine().executeScript("removeAllFootprints()");
		linkedPlacesTableView.getItems().forEach(place -> addPlaceLocation(place));

		centerMap(linkedPlacesTableView.getItems());
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

	private void jumpToPlaceLocation(final Place place) {
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

	private void showDetailsView(final Place place) {
		try {
			final View<DetailsViewController> detailsView = new View<>(GazetteerViewerResources.DETAILS_FXML_FILE);
			final Stage stage = new Stage();
			stage.initOwner(resultAnchorPane.getScene().getWindow());
			detailsView.showViewAsScene(stage);
			// we need to delay showing the place details until the website is fully loaded!
			detailsView.getController().addWebEngineStateListener((observable, oldValue, newValue) -> {
				if (newValue != Worker.State.SUCCEEDED) {
					return;
				}
				detailsView.getController().showPlaceDetails(place);
			});
		}
		catch (final IOException e) {
			logger.error("An error occurred when trying to load the details view!", e);
			ViewerUtils.showErrorDialog(e, "An error occurred when trying to load the details view!");
		}
	}
}
