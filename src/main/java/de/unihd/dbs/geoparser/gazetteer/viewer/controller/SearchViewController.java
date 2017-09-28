package de.unihd.dbs.geoparser.gazetteer.viewer.controller;

import java.net.URL;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.gazetteer.GazetteerQuery;
import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.models.PlacePropertyType;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceType;
import de.unihd.dbs.geoparser.gazetteer.models.Type;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceIdPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceNamePlaceFilter.MatchMode;
import de.unihd.dbs.geoparser.gazetteer.query.PlacePropertyPlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.PlaceTypePlaceFilter;
import de.unihd.dbs.geoparser.gazetteer.query.QueryFilter;
import de.unihd.dbs.geoparser.gazetteer.types.PlaceTypes;
import de.unihd.dbs.geoparser.gazetteer.types.PropertyTypes;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerApp;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;

import com.google.common.base.Stopwatch;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.util.StringConverter;

/**
 * This controller manages the search view for the {@link GazetteerViewerApp}.
 *
 * @author lrichter
 *
 */
// FEATURE_REQUEST lrichter: make Max-Distance for {@link MatchMode#FUZZY_LEVENSTHEIN} configurable
// FEATURE_REQUEST lrichter: enabled filter by language for place name filter
// FEATURE_REQUEST lrichter: add filter for relationships and footprints
// FEATURE_REQUEST lrichter: support a list of property filters
// XXX: there is a bug if canceling an unfinished query and starting a new one afterwards (JPA related problem)
public class SearchViewController implements Initializable {

	private static final class InvalidQueryParameterException extends Exception {

		private static final long serialVersionUID = -4771593464295841812L;

		public InvalidQueryParameterException(final String message) {
			super(message);
		}
	}

	private static final Logger logger = LoggerFactory.getLogger(SearchViewController.class);
	private static final Double LEVENSTHEIN_DISTANCE_THRESHOLD = 3.0;
	// avoid too large results sets that freeze the view and cause a lot of data traffic
	private static final int MAX_RESULT_LIMIT = 200;

	private static class TypeToStringConverter<T extends Type> extends StringConverter<T> {
		@Override
		public T fromString(final String string) {
			throw new UnsupportedOperationException();
		}

		@Override
		public String toString(final T type) {
			return type.getName();
		}
	}

	@FXML
	private CheckBox filterNameCheckBox;

	@FXML
	private HBox filterNameHBox;

	@FXML
	private TextField filterNameField;

	@FXML
	private CheckBox ignoreCaseCheckBox;

	@FXML
	private ComboBox<MatchMode> matchModeComboBox;

	@FXML
	private CheckBox filterTypeCheckBox;

	@FXML
	private HBox filterTypeHBox;

	@FXML
	private CheckComboBox<PlaceType> placeTypeCheckComboBox;

	@FXML
	private CheckBox filterPropertyCheckBox;

	@FXML
	private HBox filterPropertyHBox;

	@FXML
	private ComboBox<PlacePropertyType> propertyTypeComboBox;

	@FXML
	private TextField propertyValueTextField;

	@FXML
	private CheckBox filterIdCheckBox;

	@FXML
	private TextField idFilterTextField;

	@FXML
	private Button searchButton;

	@FXML
	private TextField maxResultsField;

	@FXML
	private CheckBox countOnlyCheckBox;

	@FXML
	private Label searchProgressLabel;

	@FXML
	private ProgressIndicator progressIndicator;

	private ViewerContext appContext;
	private Task<Void> searchTask;
	private final Stopwatch stopwatch = Stopwatch.createUnstarted();
	private final ObservableList<Place> searchResults = FXCollections.observableArrayList();

	@Override
	public void initialize(final URL arg0, final ResourceBundle arg1) {
		appContext = ViewerContext.getInstance();
		initControls();
		initSelectionControls();
		initDefaultView();
		setViewToReadyToSearch();
	}

	public ObservableList<Place> getSearchResultsObservable() {
		return searchResults;
	}

	private void initControls() {
		filterNameHBox.disableProperty().bind(filterNameCheckBox.selectedProperty().not());
		filterTypeHBox.disableProperty().bind(filterTypeCheckBox.selectedProperty().not());
		filterPropertyHBox.disableProperty().bind(filterPropertyCheckBox.selectedProperty().not());
		// Refactoring lrichter 17.03.2017: create extra UI class for numeric input?
		// force the field to be numeric only or empty
		maxResultsField.textProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue.matches("\\d*")) {
				maxResultsField.setText(newValue.replaceAll("[^\\d]", ""));
			}
		});
		propertyTypeComboBox.setConverter(new TypeToStringConverter<>());
		placeTypeCheckComboBox.setConverter(new TypeToStringConverter<>());
	}

	private void initSelectionControls() {
		matchModeComboBox.getItems().addAll(MatchMode.values());
		Set<Type> types = appContext.gazetteer.getAllTypes(PlaceType.class);
		types.forEach(type -> placeTypeCheckComboBox.getItems().add((PlaceType) type));
		types = appContext.gazetteer.getAllTypes(PlacePropertyType.class);
		types.forEach(type -> propertyTypeComboBox.getItems().add((PlacePropertyType) type));
	}

	private void initDefaultView() {
		filterNameCheckBox.setSelected(true);
		filterNameField.setText("Heidelberg");
		ignoreCaseCheckBox.setSelected(false);
		matchModeComboBox.setValue(MatchMode.EXACT);
		filterTypeCheckBox.setSelected(false);
		// XXX: remove dependencies to Demo place types! its nice to have but confusing in the long run
		Type type = appContext.gazetteer.getType(PlaceTypes.ADMINISTRATIVE_DIVISION.typeName);
		if (type != null) {
			placeTypeCheckComboBox.getCheckModel().check((PlaceType) type);
		}
		filterPropertyCheckBox.setSelected(true);
		type = appContext.gazetteer.getType(PropertyTypes.GEONAMES_FCLASS.typeName);
		if (type != null) {
			propertyTypeComboBox.getSelectionModel().select((PlacePropertyType) type);
			propertyValueTextField.setText("A,P");
		}
	}

	private void runSearch() {
		logger.debug("Compiling Query...");
		GazetteerQuery<Place> query;
		try {
			query = buildQueryFromInput();
		}
		catch (final InvalidQueryParameterException e) {
			displayProgressMessage(e.getMessage(), true);
			return;
		}
		searchTask = createSearchTask(query);
		searchResults.clear();

		logger.debug("Running Query...");
		stopwatch.reset();
		stopwatch.start();
		final Thread searchThread = new Thread(searchTask);
		searchThread.start();

		setViewToSearching();
	}

	private GazetteerQuery<Place> buildQueryFromInput() throws InvalidQueryParameterException {
		final int maxResults = Integer.parseInt(maxResultsField.getText());
		if (maxResults < 1 || maxResults > MAX_RESULT_LIMIT) {
			throw new InvalidQueryParameterException(
					"To avoid large result sets, the max. number of results must be in range [1, 200]!");
		}

		final GazetteerQuery<Place> query = new GazetteerQuery<>(maxResults);

		if (filterNameCheckBox.isSelected()) {
			query.filters.add(buildPlaceNameQueryFilter());
		}

		if (filterTypeCheckBox.isSelected()) {
			query.filters.add(buildPlaceTypeQueryFilter());
		}

		if (filterPropertyCheckBox.isSelected()) {
			query.filters.add(buildPlacePropertyQueryFilter());
		}

		if (filterIdCheckBox.isSelected()) {
			query.filters.add(buildPlaceIdQueryFilter());
		}

		return query;
	}

	private QueryFilter<Place> buildPlaceNameQueryFilter() {
		final EnumSet<NameFlag> flags = EnumSet.noneOf(NameFlag.class);
		final String placeName = filterNameField.getText();
		final String language = null;
		final boolean ignoreCase = ignoreCaseCheckBox.isSelected();
		final MatchMode matchMode = matchModeComboBox.getValue();
		final Double maxDistance = LEVENSTHEIN_DISTANCE_THRESHOLD;
		logger.debug("adding place name filter: name=" + placeName + ", language=" + language + ", ignoreCase="
				+ ignoreCase + ", matchMode=" + matchMode + ", maxDistance=" + maxDistance);

		return new PlaceNamePlaceFilter(placeName, language, flags, ignoreCase, matchMode, maxDistance, false);
	}

	private QueryFilter<Place> buildPlaceTypeQueryFilter() {
		final Set<PlaceType> placeTypes = new HashSet<>(placeTypeCheckComboBox.getCheckModel().getCheckedItems());
		logger.debug("adding place type filter: place_types=" + placeTypes);
		return new PlaceTypePlaceFilter(placeTypes, false);
	}

	private QueryFilter<Place> buildPlacePropertyQueryFilter() throws InvalidQueryParameterException {
		final PlacePropertyType propertyType = propertyTypeComboBox.getSelectionModel().getSelectedItem();
		final String inputString = propertyValueTextField.getText();
		final boolean rangeCheck = inputString.startsWith("[") && inputString.endsWith("]");

		if (rangeCheck) {
			return buildPlacedPropertyQueryFilterForRange(propertyType,
					inputString.substring(1, inputString.length() - 1));
		}
		else if (inputString.isEmpty()) {
			logger.debug("adding property existence filter: property_type=" + propertyType);
			return new PlacePropertyPlaceFilter<>(propertyType, false);
		}
		else {
			final List<String> propertyValues = Arrays.asList(inputString.split(","));
			logger.debug("adding property values filter: property_type=" + propertyType + ", values=" + propertyValues);
			return new PlacePropertyPlaceFilter<>(propertyType, new HashSet<>(propertyValues), String.class, false);
		}
	}

	private static QueryFilter<Place> buildPlacedPropertyQueryFilterForRange(final PlacePropertyType propertyType,
			final String inputString) throws InvalidQueryParameterException {
		final int commaPos = inputString.indexOf(',');
		if (commaPos == -1 || commaPos != inputString.lastIndexOf(',')) {
			throw new InvalidQueryParameterException(
					"For range filters, two comma separated values or an open interval must be given!");
		}
		try {
			final Double minValue = commaPos == 0 ? null : Double.parseDouble(inputString.substring(0, commaPos));
			final Double maxValue = commaPos == inputString.length() - 1 ? null
					: Double.parseDouble(inputString.substring(commaPos + 1));
			logger.debug("adding property value range filter: property_type=" + propertyType + ", min=" + minValue
					+ ", max=" + maxValue);
			return new PlacePropertyPlaceFilter<>(propertyType, minValue, maxValue, Double.class, false);
		}
		catch (final NumberFormatException e) {
			throw new InvalidQueryParameterException("The range values must be numbers or empty!");
		}
	}

	private QueryFilter<Place> buildPlaceIdQueryFilter() throws InvalidQueryParameterException {
		try {
			final Set<Long> ids = new HashSet<>(
					Arrays.asList(idFilterTextField.getText().replaceAll(" ", "").split(",")).stream()
							.map(id -> Long.parseLong(id)).collect(Collectors.toSet()));
			logger.debug("adding place id filter: ids=" + ids);
			return new PlaceIdPlaceFilter(ids, false);
		}
		catch (final NumberFormatException e) {
			throw new InvalidQueryParameterException("The place ids must be numbers!");
		}
	}

	private Task<Void> createSearchTask(final GazetteerQuery<Place> query) {
		final Task<Void> searchTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				final List<Place> places = appContext.gazetteer.getPlaces(query);
				final int numberOfPlaces = places.size();
				int currentPlace = 0;

				for (final Place place : places) {
					if (this.isCancelled()) {
						return null;
					}

					this.updateProgress(currentPlace, numberOfPlaces);
					// we need to use Platform.runLater since we're not on the JavaFx thread when adding places to
					// searchResults fails, if listeners are added to it!
					Platform.runLater(() -> searchResults.add(place));
					currentPlace++;
				}

				return null;
			}
		};

		searchTask.setOnSucceeded(event -> searchSucceeded());
		searchTask.setOnFailed(event -> searchFailed());
		searchTask.setOnCancelled(event -> searchCancelled());
		searchTask.progressProperty().addListener(event -> searchProgressUpdated());

		return searchTask;
	}

	private void cancelSearch() {
		appContext.gazetteer.cancelQuery();
		searchTask.cancel();
	}

	private void searchProgressUpdated() {
		displayProgressMessage(
				"Loading results: " + (int) searchTask.getWorkDone() + "/" + (int) searchTask.getTotalWork(), false);
	}

	private void searchSucceeded() {
		stopwatch.stop();
		final String infoText = "Found " + searchResults.size() + " matches in "
				+ stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms";
		displayProgressMessage(infoText, false);
		logger.info("Search was successful! " + infoText);
		setViewToReadyToSearch();
	}

	private void searchCancelled() {
		logger.info("Search was cancelled!");
		displayProgressMessage("Search was cancelled!", false);
		setViewToReadyToSearch();
	}

	private void searchFailed() {
		final Throwable e = searchTask.exceptionProperty().getValue();
		logger.error("An error occurred when querying the gazetteer!", e);
		displayProgressMessage("Search failed!", true);
		ViewerUtils.showErrorDialog(e, "Search failed!", true);
		setViewToReadyToSearch();
	}

	private void setViewToReadyToSearch() {
		searchButton.setOnAction(event -> runSearch());
		progressIndicator.setVisible(false);
		searchButton.setText("Search");
	}

	private void setViewToSearching() {
		searchButton.setOnAction(event -> cancelSearch());
		progressIndicator.setVisible(true);
		searchButton.setText("Cancel");
		displayProgressMessage("Searching...", false);
	}

	private void displayProgressMessage(final String message, final boolean error) {
		if (error) {
			searchProgressLabel.setTextFill(Color.RED);
		}
		else {
			searchProgressLabel.setTextFill(Color.BLACK);
		}
		searchProgressLabel.setText(message);
	}

}
