package de.unihd.dbs.geoparser.util.viewer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import de.unihd.dbs.geoparser.gazetteer.models.Place;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName;
import de.unihd.dbs.geoparser.gazetteer.models.PlaceName.NameFlag;
import de.unihd.dbs.geoparser.gazetteer.viewer.GazetteerViewerApp;
import de.unihd.dbs.geoparser.viewer.GeoparserViewerApp;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * A bunch of methods that are shared by {@link GazetteerViewerApp} and {@link GeoparserViewerApp}.
 *
 * @author lrichter
 *
 */
public class ViewerUtils {

	public static void showErrorDialog(final Throwable throwable, final String messageTitle) {
		showErrorDialog(throwable, messageTitle, true);
	}

	/**
	 * Show an JavaFX error dialog, optionally with stack-trace information.
	 *
	 * @param throwable the error message
	 * @param messageTitle a title for the error message
	 * @param printStackTrace if <code>true</code>, the stack-trace is printed in a text-area.
	 */
	public static void showErrorDialog(final Throwable throwable, final String messageTitle,
			final boolean printStackTrace) {
		final Alert alert = new Alert(AlertType.ERROR);

		alert.setHeaderText(messageTitle);

		if (!printStackTrace) {
			alert.setContentText(throwable.getMessage());
			alert.showAndWait();
			return;
		}

		final StringWriter sw = new StringWriter();
		final PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		final String exceptionText = sw.toString();

		final Label label = new Label("The exception stacktrace was:");
		final TextArea textArea = new TextArea(exceptionText);
		textArea.setEditable(false);
		textArea.setWrapText(true);
		textArea.setMaxWidth(Double.MAX_VALUE);
		textArea.setMaxHeight(Double.MAX_VALUE);
		GridPane.setVgrow(textArea, Priority.ALWAYS);
		GridPane.setHgrow(textArea, Priority.ALWAYS);
		final GridPane expContent = new GridPane();
		expContent.setMaxWidth(Double.MAX_VALUE);
		expContent.add(label, 0, 0);
		expContent.add(textArea, 0, 1);
		alert.getDialogPane().setExpandableContent(expContent);
		alert.getDialogPane().setExpanded(true);

		alert.showAndWait();
	}

	/**
	 * Get the preferred name for the given place.
	 * <p>
	 * Preference is given to English names. If none exist, German names are considered next. If neither exists, all
	 * names are eligible. The first name of the remaining set of names that has a preference flag it is taken.
	 * Otherwise the first occurrence of candidate place names is taken.
	 *
	 * @param place the place for which to get the preferred name.
	 * @return the preferred name, or <code>null</code> if no place or place names are given.
	 */
	public static String getPreferredName(final Place place) {
		if (Objects.isNull(place) || Objects.isNull(place.getPlaceNames())) {
			return null;
		}

		Set<PlaceName> names = place.getPlaceNames().stream()
				.filter(placeName -> placeName.getLanguage() != null && placeName.getLanguage().equals("en"))
				.collect(Collectors.toSet());
		if (names.size() == 0) {
			names = place.getPlaceNames().stream()
					.filter(placeName -> placeName.getLanguage() != null && placeName.getLanguage().equals("de"))
					.collect(Collectors.toSet());
		}
		if (names.size() == 0) {
			names = place.getPlaceNames().stream().filter(placeName -> Objects.isNull(placeName.getLanguage()))
					.collect(Collectors.toSet());
		}
		if (names.size() == 0) {
			names = place.getPlaceNames();
		}

		final Set<PlaceName> preferredNames = names.stream()
				.filter(placeName -> placeName.getNameFlags().contains(NameFlag.IS_PREFERRED))
				.collect(Collectors.toSet());

		if (preferredNames.size() > 0) {
			names = preferredNames;
		}

		if (names.size() > 0) {
			return names.iterator().next().getName();
		}
		else {
			return null;
		}
	}

}
