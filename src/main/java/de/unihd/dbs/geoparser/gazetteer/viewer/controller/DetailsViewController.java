package de.unihd.dbs.geoparser.gazetteer.viewer.controller;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;

import de.unihd.dbs.geoparser.gazetteer.models.AbstractEntity;
import de.unihd.dbs.geoparser.gazetteer.models.Footprint;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceProperty;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceRelationship;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceTypeAssignment;
import de.unihd.dbs.geoparser.gazetteer.models.Provenance;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerResources;
import de.unihd.dbs.geoparser.util.viewer.NoEditTextFieldTableCell;
import de.unihd.dbs.geoparser.util.viewer.View;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.concurrent.Worker;
import javafx.concurrent.Worker.State;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.AnchorPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

/**
 * This controller manages the view that displays the details of a place.
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
// XXX: should use Weakreferences for Event-Handlers or remove them when the window is closed -> otherwise memory leaks
public class DetailsViewController implements Initializable {

	private static final int DEFAULT_MAP_ZOOM = 8;

	@FXML
	private AnchorPane detailsPane;

	@FXML
	private TableView<PlaceName> placeNameTable;

	@FXML
	private TableColumn<PlaceName, String> placeNameNameColumn;

	@FXML
	private TableColumn<PlaceName, String> placeNameLanguageColumn;

	@FXML
	private TableColumn<PlaceName, Boolean> placeNamePrererredColumn;

	@FXML
	private TableColumn<PlaceName, Boolean> placeNameOfficialColumn;

	@FXML
	private TableColumn<PlaceName, Boolean> placeNameAbbreviationColumn;

	@FXML
	private TableColumn<PlaceName, Boolean> placeNameColloquialColumn;

	@FXML
	private TableColumn<PlaceName, Boolean> placeNameHistoricalColumn;

	@FXML
	private WebView mapWebView;

	@FXML
	private TableView<PlaceTypeAssignment> placeTypeTable;

	@FXML
	private TableColumn<PlaceTypeAssignment, String> placeTypeNameColumn;

	@FXML
	private TableView<PlaceProperty> placePropertyTable;

	@FXML
	private TableColumn<PlaceProperty, String> placePropertyNameColumn;

	@FXML
	private TableColumn<PlaceProperty, String> placePropertyValueColumn;

	@FXML
	private TableView<PlaceRelationship> placeLeftRelationshipTable;

	@FXML
	private TableColumn<PlaceRelationship, Long> placeLeftRelationshipRightPlaceColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeLeftRelationshipRightPlaceNameColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeLeftRelationshipTypeColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeLeftRelationshipValueColumn;

	@FXML
	private TableView<PlaceRelationship> placeRightRelationshipTable;

	@FXML
	private TableColumn<PlaceRelationship, Long> placeRightRelationshipLeftPlaceColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeRightRelationshipLeftPlaceNameColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeRightRelationshipTypeColumn;

	@FXML
	private TableColumn<PlaceRelationship, String> placeRightRelationshipValueColumn;

	@FXML
	private TextField entityIdTextField;

	@FXML
	private DatePicker validTimeStartDatePicker;

	@FXML
	private DatePicker validTimeEndDatePicker;

	@FXML
	private TextField provenanceIdTextField;

	@FXML
	private TextField provenanceURITextField;

	@FXML
	private TextField provenanceAggregationToolTextField;

	private Place place;
	private WebEngine webEngine;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		initPlaceNameTable();
		initPlaceFootprintWebView();
		initPlaceTypeTable();
		initPlacePropertyTable();
		initPlaceRelationshipTables();
		initEntityDetailsSection();
	}

	public void showPlaceDetails(final Place place) {
		this.place = place;
		// Refactoring lrichter 17.03.2017: necessary to check for null?
		if (place == null) {
			placeNameTable.setItems(null);
			placeTypeTable.setItems(null);
			placePropertyTable.setItems(null);
			placeLeftRelationshipTable.setItems(null);
			placeRightRelationshipTable.setItems(null);
			showFootprintDetails(null);
		}
		else {
			placeNameTable.setItems(FXCollections.observableArrayList(place.getPlaceNames()));
			placeTypeTable.setItems(FXCollections.observableArrayList(place.getPlaceTypeAssignments()));
			placePropertyTable.setItems(FXCollections.observableArrayList(place.getProperties()));
			placeLeftRelationshipTable.setItems(FXCollections.observableArrayList(place.getLeftPlaceRelationships()));
			placeRightRelationshipTable.setItems(FXCollections.observableArrayList(place.getRightPlaceRelationships()));
			showFootprints();
			showEntityDetails(place);
		}
	}

	public void addWebEngineStateListener(final ChangeListener<? super State> listener) {
		webEngine.getLoadWorker().stateProperty().addListener(listener);
	}

	private void initPlaceNameTable() {
		placeNameNameColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getName()));
		placeNameLanguageColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getLanguage()));
		placeNamePrererredColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				cellData.getValue().getNameFlags().contains(NameFlag.IS_PREFERRED)));
		placeNameOfficialColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				cellData.getValue().getNameFlags().contains(NameFlag.IS_OFFICIAL)));
		placeNameAbbreviationColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				cellData.getValue().getNameFlags().contains(NameFlag.IS_ABBREVIATION)));
		placeNameColloquialColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				cellData.getValue().getNameFlags().contains(NameFlag.IS_COLLOQUIAL)));
		placeNameHistoricalColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				cellData.getValue().getNameFlags().contains(NameFlag.IS_HISTORICAL)));

		placeNameNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeNameLanguageColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeNamePrererredColumn.setCellFactory(CheckBoxTableCell.forTableColumn(placeNamePrererredColumn));
		placeNameOfficialColumn.setCellFactory(CheckBoxTableCell.forTableColumn(placeNameOfficialColumn));
		placeNameAbbreviationColumn.setCellFactory(CheckBoxTableCell.forTableColumn(placeNameAbbreviationColumn));
		placeNameColloquialColumn.setCellFactory(CheckBoxTableCell.forTableColumn(placeNameColloquialColumn));
		placeNameHistoricalColumn.setCellFactory(CheckBoxTableCell.forTableColumn(placeNameHistoricalColumn));
	}

	private void initPlaceFootprintWebView() {
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
			showFootprintDetails(id);
		});

	}

	private void initPlaceTypeTable() {
		placeTypeNameColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getType().getName()));
		placeTypeNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
	}

	private void initPlacePropertyTable() {
		placePropertyNameColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getType().getName()));
		placePropertyValueColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getValue()));
		placePropertyNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placePropertyValueColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
	}

	private void initPlaceRelationshipTables() {
		initRightPlaceRelationshipTable();
		initLeftPlaceRelationshipTable();
	}

	private void initRightPlaceRelationshipTable() {
		placeRightRelationshipLeftPlaceColumn.setCellValueFactory(
				cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getLeftPlace().getId()));
		placeRightRelationshipLeftPlaceNameColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				ViewerUtils.getPreferredName(cellData.getValue().getLeftPlace())));
		placeRightRelationshipTypeColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getType().getName()));
		placeRightRelationshipValueColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getValue()));

		// FEATURE_REQUEST lrichter: not so intuitive access to place id of related places (same for left side)
		// we show place details for a place in a relationships in a new details view, if the user double clicks on the
		// row or the "Right/Left Place" cell. To do so, we need to disable the NoEditTextFieldTableCell approach, so
		// the user cannot simply access the cell data. Instead, she now has to open the details view and select the id
		// in the "Entity Details" area

		// placeRightRelationshipLeftPlaceColumn
		// .setCellFactory(NoEditTextFieldTableCell.forTableColumn(new LongStringConverter()));
		placeRightRelationshipLeftPlaceNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeRightRelationshipTypeColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeRightRelationshipValueColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());

		placeRightRelationshipTable.setRowFactory(tableView -> {
			final TableRow<PlaceRelationship> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					showDetailsView(row.getItem().getLeftPlace());
				}
			});
			return row;
		});
	}

	private void initLeftPlaceRelationshipTable() {
		placeLeftRelationshipRightPlaceColumn.setCellValueFactory(
				cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getRightPlace().getId()));
		placeLeftRelationshipRightPlaceNameColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(
				ViewerUtils.getPreferredName(cellData.getValue().getRightPlace())));
		placeLeftRelationshipTypeColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getType().getName()));
		placeLeftRelationshipValueColumn
				.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue().getValue()));

		// placeLeftRelationshipRightPlaceColumn
		// .setCellFactory(NoEditTextFieldTableCell.forTableColumn(new LongStringConverter()));
		placeLeftRelationshipRightPlaceNameColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeLeftRelationshipTypeColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());
		placeLeftRelationshipValueColumn.setCellFactory(NoEditTextFieldTableCell.forTableColumn());

		placeLeftRelationshipTable.setRowFactory(tableView -> {
			final TableRow<PlaceRelationship> row = new TableRow<>();
			row.setOnMouseClicked(event -> {
				if (event.getClickCount() == 2 && (!row.isEmpty())) {
					showDetailsView(row.getItem().getRightPlace());
				}
			});
			return row;
		});
	}

	private void showDetailsView(final Place place) {
		try {
			final View<DetailsViewController> detailsView = new View<>(GazetteerViewerResources.DETAILS_FXML_FILE);
			// ensure the details view is linked to the main window, otherwise the window is still opened if main
			// window is closed
			final Stage stage = new Stage();
			stage.initOwner(detailsPane.getScene().getWindow());
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
			ViewerUtils.showErrorDialog(e, "An error occurred when trying to load the details view!");
		}
	}

	private void initEntityDetailsSection() {
		placeNameTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showEntityDetails(newValue));
		placeTypeTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showEntityDetails(newValue));
		placePropertyTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showEntityDetails(newValue));
		placeLeftRelationshipTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showEntityDetails(newValue));
		placeRightRelationshipTable.getSelectionModel().selectedItemProperty()
				.addListener((observable, oldValue, newValue) -> showEntityDetails(newValue));
	}

	private void showFootprints() {
		mapWebView.getEngine().executeScript("removeAllFootprints()");

		final Envelope minimumBoundingBox = new Envelope();

		for (final Footprint footprint : place.getFootprints()) {
			final Geometry geometry = footprint.getGeometry();
			minimumBoundingBox.expandToInclude(geometry.getEnvelopeInternal());

			mapWebView.getEngine().executeScript("addFootprint(" + footprint.getId() + ", '" + geometry.toText() + "',"
					+ "'" + ViewerUtils.getPreferredName(footprint.getPlace()) + "')");
		}

		final double bboxCenterLatitude = minimumBoundingBox.centre().y;
		final double bboxCenterLongitude = minimumBoundingBox.centre().x;

		mapWebView.getEngine().executeScript(
				"jumpTo(" + bboxCenterLongitude + "," + bboxCenterLatitude + "," + DEFAULT_MAP_ZOOM + ")");
	}

	private void showFootprintDetails(final Long footprintId) {
		final Footprint focusedFootprint = footprintId == null ? null
				: place.getFootprints().stream().filter(footprint -> footprint.getId().equals(footprintId)).findFirst()
						.get();
		showEntityDetails(focusedFootprint);
	}

	private void showEntityDetails(final AbstractEntity entity) {
		if (entity == null) {
			entityIdTextField.setText(null);
			validTimeEndDatePicker.setValue(null);
			validTimeStartDatePicker.setValue(null);
			provenanceIdTextField.setText(null);
			provenanceURITextField.setText(null);
			provenanceAggregationToolTextField.setText(null);
		}
		else {
			entityIdTextField.setText("" + entity.getId());
			if (entity.getValidTimeEndDate() != null) {
				validTimeEndDatePicker.setValue(LocalDate.from(entity.getValidTimeEndDate().toInstant()));
			}
			else {
				validTimeEndDatePicker.setValue(null);
			}
			if (entity.getValidTimeEndDate() != null) {
				validTimeStartDatePicker.setValue(LocalDate.from(entity.getValidTimeStartDate().toInstant()));
			}
			else {
				validTimeStartDatePicker.setValue(null);
			}

			final Provenance provenance = entity.getProvenance();
			if (provenance != null) {
				provenanceIdTextField.setText(provenance.getId().toString());
				provenanceURITextField.setText(provenance.getUri());
				provenanceAggregationToolTextField.setText(provenance.getAggregationTool());
			}
		}
	}

}
