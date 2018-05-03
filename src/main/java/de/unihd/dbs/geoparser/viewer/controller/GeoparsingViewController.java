package de.unihd.dbs.geoparser.viewer.controller;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.controlsfx.control.CheckComboBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.unihd.dbs.geoparser.core.LinkedToponym;
import de.unihd.dbs.geoparser.core.NamedEntity;
import de.unihd.dbs.geoparser.core.ResolvedToponym;
import de.unihd.dbs.geoparser.gazetteer.viewer.ViewerContext;
import de.unihd.dbs.geoparser.util.GeoparserUtil;
import de.unihd.dbs.geoparser.util.viewer.ViewerUtils;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingApproach;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingApproachBuilder;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingResult;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingRun;
import de.unihd.dbs.geoparser.viewer.model.GeoparsingStep;

import com.google.common.base.Stopwatch;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.paint.Color;

/**
 * This controller manages the geoparsing view. Here, different geoparsing modules can be selected and run (iteratively)
 * over a given input text.
 *
 * @author lrichter
 *
 */
// Refactoring lrichter 21.03.2017: create separate class to configure geoparser for an geoparsing approach
// Refactoring lrichter 21.03.2017: merge code for creating task into single method to avoid excessive code duplication
// Refactoring lrichter 21.03.2017: move GeoparsingApproach, GeoparsingResult, etc to new class files in model package
public class GeoparsingViewController implements Initializable {

	private static final Logger logger = LoggerFactory.getLogger(GeoparsingViewController.class);

	@FXML
	private TextArea inputTextArea;

	@FXML
	private Button geoparseButton;

	@FXML
	private Button findToponymsButton;

	@FXML
	private Button linkToponymsButton;

	@FXML
	private Button disambiguateToponymsButton;

	@FXML
	private Button cancelButton;

	@FXML
	private Button resetButton;

	@FXML
	private ProgressIndicator progressIndicator;

	@FXML
	private Label geoparsingProgressLabel;

	@FXML
	private CheckComboBox<GeoparsingApproach.RecognitionModule> recognitionModuleCheckComboBox;

	@FXML
	private CheckComboBox<GeoparsingApproach.LinkingModule> linkingModuleCheckComboBox;

	@FXML
	private CheckComboBox<GeoparsingApproach.DisambiguationModule> disambiguationModuleCheckComboBox;

	private ViewerContext appContext;
	private final BooleanProperty finishedGeoparsing = new SimpleBooleanProperty(true);
	private final ObservableList<GeoparsingRun> geoparsingRuns = FXCollections.observableArrayList();
	private Task<Void> geoparsingTask;
	private final Property<GeoparsingStep> lastStep = new SimpleObjectProperty<>(GeoparsingStep.NONE);
	private GeoparsingStep stepToExecute;
	public Stopwatch stopwatch = Stopwatch.createUnstarted();
	private GeoparsingApproachBuilder approachBuilder;

	@Override
	public void initialize(final URL location, final ResourceBundle resources) {
		appContext = ViewerContext.getInstance();
		approachBuilder = new GeoparsingApproachBuilder(appContext.config, appContext.gazetteer);
		initControls();
		initSelectionControls();
		initDefaultView();
		resetView();
	}

	public ObservableValue<Boolean> getFinishedGeoparsingObservable() {
		return finishedGeoparsing;
	}

	public ObservableList<GeoparsingRun> getGeoparsingRunsObservable() {
		return geoparsingRuns;
	}

	private void initControls() {
		geoparseButton.setOnAction(event -> runGeoparsingStep(GeoparsingStep.ALL));
		findToponymsButton.setOnAction(event -> runGeoparsingStep(GeoparsingStep.RECOGNITION));
		linkToponymsButton.setOnAction(event -> runGeoparsingStep(GeoparsingStep.LINKING));
		disambiguateToponymsButton.setOnAction(event -> runGeoparsingStep(GeoparsingStep.DISAMBIGUATION));

		progressIndicator.visibleProperty().bind(finishedGeoparsing.not());
		cancelButton.disableProperty().bind(finishedGeoparsing);
		geoparseButton.disableProperty().bind(finishedGeoparsing.not());
		findToponymsButton.disableProperty().bind(finishedGeoparsing.not());
		final BooleanBinding isLastStepNone = Bindings
				.createBooleanBinding(() -> lastStep.getValue().equals(GeoparsingStep.NONE), lastStep);
		final BooleanBinding isLastStepRecognition = Bindings
				.createBooleanBinding(() -> lastStep.getValue().equals(GeoparsingStep.RECOGNITION), lastStep);
		linkToponymsButton.disableProperty().bind(finishedGeoparsing.not().or(isLastStepNone));
		disambiguateToponymsButton.disableProperty()
				.bind(finishedGeoparsing.not().or(isLastStepNone).or(isLastStepRecognition));
		resetButton.disableProperty().bind(finishedGeoparsing.not().or(isLastStepNone));
	}

	private void initSelectionControls() {
		recognitionModuleCheckComboBox.getItems().addAll(GeoparsingApproach.RecognitionModule.values());
		linkingModuleCheckComboBox.getItems().addAll(GeoparsingApproach.LinkingModule.values());
		disambiguationModuleCheckComboBox.getItems().addAll(GeoparsingApproach.DisambiguationModule.values());
	}

	private void initDefaultView() {
		inputTextArea.setText("Berlin and Hamburg.");
		// recognitionModuleCheckComboBox.getCheckModel().check(RecognitionModule.STANFORD_NER);
		recognitionModuleCheckComboBox.getCheckModel().check(GeoparsingApproach.RecognitionModule.GAZETTEER_LOOKUP);
		linkingModuleCheckComboBox.getCheckModel().check(GeoparsingApproach.LinkingModule.GAZETTEER_LOOKUP_EXACT);
		//disambiguationModuleCheckComboBox.getCheckModel()
		//		.check(GeoparsingApproach.DisambiguationModule.FIRST_MATCH_DISAMBIGUATION);
		//disambiguationModuleCheckComboBox.getCheckModel()
		//		.check(GeoparsingApproach.DisambiguationModule.HIGHEST_POPULATION_DISAMBIGUATION);
		//disambiguationModuleCheckComboBox.getCheckModel()
		//		.check(GeoparsingApproach.DisambiguationModule.HIGHEST_ADMIN_LEVEL_DISAMBIGUATION);
		disambiguationModuleCheckComboBox.getCheckModel()
				.check(GeoparsingApproach.DisambiguationModule.POPULATION_DISTANCE_WEIGHT_DISAMBIGUATOR);
	}

	@FXML
	private void resetView() {
		resetGeoparsingState();
		setViewToReadyToGeoparse();
	}

	private void setViewToReadyToGeoparse() {
		finishedGeoparsing.set(true);
	}

	private void setViewToGeoparsing() {
		displayProgressMessage("Running geoparsing step " + stepToExecute.name() + "...", false);
		finishedGeoparsing.set(false);
	}

	private void runGeoparsingStep(final GeoparsingStep stepToExecute) {
		if (stepToExecute.equals(GeoparsingStep.RECOGNITION) || stepToExecute.equals(GeoparsingStep.ALL)) {
			resetGeoparsingState();
			initGeoparsingRuns(inputTextArea.getText());
		}

		geoparsingTask = createGeoparsingTask(stepToExecute);
		this.stepToExecute = stepToExecute;
		geoparsingTask.setOnSucceeded(eventHandler -> geoparsingStepSucceeded());
		geoparsingTask.setOnFailed(eventHandler -> geoparsingStepFailed());
		geoparsingTask.setOnCancelled(eventHandler -> geoparsingStepCancelled());

		logger.debug("Running geoparsing step " + stepToExecute.name() + "...");
		stopwatch.reset();
		stopwatch.start();
		new Thread(geoparsingTask).start();
		setViewToGeoparsing();
	}

	@FXML
	private void cancelGeoparsing() {
		geoparsingTask.cancel();
	}

	private void geoparsingStepSucceeded() {
		stopwatch.stop();
		final String infoText = "Running geoparsing step " + stepToExecute + " took "
				+ stopwatch.elapsed(TimeUnit.MILLISECONDS) + "ms";
		logger.info("Geoparsing was successful! " + infoText);
		displayProgressMessage(infoText, false);
		lastStep.setValue(stepToExecute);
		setViewToReadyToGeoparse();
	}

	private void geoparsingStepCancelled() {
		logger.info("Geoparsing was cancelled!");
		displayProgressMessage("Geoparsing was cancelled!", false);
		setViewToReadyToGeoparse();
	}

	private void geoparsingStepFailed() {
		final Throwable e = geoparsingTask.exceptionProperty().getValue();
		logger.error("An error occurred during geoparsing!", e);
		displayProgressMessage("Geoparsing failed!", true);
		ViewerUtils.showErrorDialog(e, "Geoparsing failed!", true);
		setViewToReadyToGeoparse();
	}

	private void displayProgressMessage(final String message, final boolean error) {
		if (error) {
			geoparsingProgressLabel.setTextFill(Color.RED);
		}
		else {
			geoparsingProgressLabel.setTextFill(Color.BLACK);
		}
		geoparsingProgressLabel.setText(message);
	}

	private Task<Void> createGeoparsingTask(final GeoparsingStep step) {
		return new Task<Void>() {

			@Override
			protected Void call() {
				for (final GeoparsingRun run : geoparsingRuns) {
					final GeoparsingResult result = run.results;
					final GeoparsingApproach approach = run.geoparsingApproach;

					if (this.isCancelled()) {
						return null;
					}

					if (step.equals(GeoparsingStep.RECOGNITION) || step.equals(GeoparsingStep.ALL)) {
						approachBuilder.initGeoparser(approach);
						run.lastStep = GeoparsingStep.NONE;
						result.foundEntities.clear();
						result.linkedToponyms.clear();
						result.resolvedToponyms.clear();
					}

					if (step.equals(GeoparsingStep.LINKING)) {
						result.linkedToponyms.clear();
						result.resolvedToponyms.clear();
					}

					if (step.equals(GeoparsingStep.DISAMBIGUATION)) {
						result.resolvedToponyms.clear();
					}

					if (this.isCancelled()) {
						return null;
					}

					run.stopwatch.reset();
					run.stopwatch.start();

					if (step.equals(GeoparsingStep.RECOGNITION) || step.equals(GeoparsingStep.ALL)) {
						final List<NamedEntity> namedEntities = approach.geoparser
								.recognizeNamedEntities(result.document);
						result.foundEntities.addAll(namedEntities);
						run.lastStep = GeoparsingStep.RECOGNITION;
					}

					if (this.isCancelled()) {
						return null;
					}

					if (step.equals(GeoparsingStep.LINKING) || step.equals(GeoparsingStep.ALL)) {
						final List<LinkedToponym> linkedToponyms = approach.geoparser.linkToponyms(result.document);
						result.linkedToponyms.addAll(linkedToponyms);
						run.lastStep = GeoparsingStep.LINKING;

						// some recognition modules might already produce resolved toponyms
						final List<ResolvedToponym> resolvedToponyms = GeoparserUtil
								.getResolvedToponyms(result.document);
						result.resolvedToponyms.addAll(resolvedToponyms);
					}

					if (this.isCancelled()) {
						return null;
					}

					if (step.equals(GeoparsingStep.DISAMBIGUATION) || step.equals(GeoparsingStep.ALL)) {
						final List<ResolvedToponym> resolvedToponyms = approach.geoparser
								.disambiguateToponyms(result.document);
						result.resolvedToponyms.addAll(resolvedToponyms);
						run.lastStep = GeoparsingStep.DISAMBIGUATION;
					}

					if (step.equals(GeoparsingStep.RECOGNITION)) {
						// some recognition modules might already produce linked toponyms...
						final List<LinkedToponym> linkedToponyms = GeoparserUtil.getLinkedToponyms(result.document);
						result.linkedToponyms.addAll(linkedToponyms);
					}

					if (step.equals(GeoparsingStep.RECOGNITION) || step.equals(GeoparsingStep.LINKING)) {
						// some recognition or linking modules might already produce resolved toponyms...
						final List<ResolvedToponym> resolvedToponyms = GeoparserUtil
								.getResolvedToponyms(result.document);
						result.resolvedToponyms.addAll(resolvedToponyms);
					}

					if (this.isCancelled()) {
						return null;
					}

					run.stopwatch.stop();
				}

				return null;
			}

		};

	}

	private void resetGeoparsingState() {
		geoparsingRuns.clear();
		// TODO: this has too many side-effects, but clearing should be done to avoid over-performing by cached data
		// app.gazetteer.getEntityManger().clear();
		lastStep.setValue(GeoparsingStep.NONE);
	}

	private void initGeoparsingRuns(final String inputText) {
		final List<GeoparsingApproach.RecognitionModule> recognitionModules = recognitionModuleCheckComboBox
				.getCheckModel().getCheckedItems();
		final List<GeoparsingApproach.LinkingModule> linkingModules = linkingModuleCheckComboBox.getCheckModel()
				.getCheckedItems();
		final List<GeoparsingApproach.DisambiguationModule> disambiguationModules = disambiguationModuleCheckComboBox
				.getCheckModel().getCheckedItems();

		// TODO: quite restrictive rule!
		if (recognitionModules.isEmpty() || linkingModules.isEmpty() || disambiguationModules.isEmpty()) {
			throw new IllegalArgumentException("At least one module per geoparsing step must be selected!");
		}

		final List<GeoparsingApproach> geoparsingApproaches = createGeoparsingApproachesFromModuleCrossProduct(
				recognitionModules, linkingModules, disambiguationModules);
		geoparsingRuns.addAll(createGeoparsingRuns(geoparsingApproaches, inputText));
	}

	private static List<GeoparsingRun> createGeoparsingRuns(final List<GeoparsingApproach> geoparsingApproaches,
			final String inputText) {
		return geoparsingApproaches.stream()
				.map(approach -> new GeoparsingRun(approach, new GeoparsingResult(inputText)))
				.collect(Collectors.toList());
	}

	private static List<GeoparsingApproach> createGeoparsingApproachesFromModuleCrossProduct(
			final List<GeoparsingApproach.RecognitionModule> recognitionModules,
			final List<GeoparsingApproach.LinkingModule> linkingModules,
			final List<GeoparsingApproach.DisambiguationModule> disambiguationModules) {
		final List<GeoparsingApproach> geoparsingApproaches = new ArrayList<>();

		for (final GeoparsingApproach.RecognitionModule recognitionModule : recognitionModules) {
			for (final GeoparsingApproach.LinkingModule linkingModule : linkingModules) {
				for (final GeoparsingApproach.DisambiguationModule disambiguationModule : disambiguationModules) {
					geoparsingApproaches
							.add(new GeoparsingApproach(recognitionModule, linkingModule, disambiguationModule));
				}
			}
		}

		return geoparsingApproaches;
	}

}
