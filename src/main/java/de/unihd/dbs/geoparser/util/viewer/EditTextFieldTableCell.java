package de.unihd.dbs.geoparser.util.viewer;

import java.util.ArrayList;
import java.util.List;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableColumn.CellEditEvent;
import javafx.scene.control.TablePosition;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * A class containing a {@link TableCell} implementation that draws a {@link TextField} or {@link TextArea} node inside
 * the cell if in editing mode.
 * <p>
 * In contrast to {@link TextFieldTableCell} the editing mode can be committed not only by pressing ENTER, but also by
 * loosing the Cell focus (e.g., by clicking somewhere else) or by pressing TAB. If TAB is pressed the next cell is
 * selected for editing. If SHIFT-TAB is pressed, the previous one is selected. The editing mode can be cancelled by
 * pressing ESCAPE.
 * <p>
 * If committing fails due to an exception the {@link #getConverter() converter's} {@link StringConverter#fromString}
 * method, the cell is reset to its original value and the exception is re-thrown. This behavior avoids an endless-loop
 * of exceptions...
 * <p>
 * By default, the EditableTableCell is rendered as a {@link Label} when not being edited, and as a TextField when in
 * editing mode. The TextField will, by default, stretch to fill the entire table cell. If
 * {@link TableCell#isWrapText()} is <code>true</code> the EditableTableCell is rendered as {@link Text} when not being
 * edited in order to support text wrapping. When in editing mode, is is rendered as TextArea, by default stretched to
 * fill the entire table cell.
 * <p>
 * By pressing Shift+ENTER, a line-separator "{@code \n}" is added after the caret's current position if in editing
 * mode. {@link System#lineSeparator()} is not used since TextInputControl seems to ignore OS dependent line-breaks like
 * "\r\n" for Windows (cf. comment in http://stackoverflow.com/a/28397763).
 * <p>
 * <b>Note</b>: In order to provide a work-around for a known bug "TableView does not commit values on focus lost"
 * (https://bugs.openjdk.java.net/browse/JDK-8089514), the approach proposed in http://stackoverflow.com/a/33410688 is
 * implemented. That is, if ESCAPE is pressed, the text input is reset to the original cell value before
 * {@link #cancelEdit()} is called. In {@link #cancelEdit()}, we check if the text input is different from the original
 * value. If not, we know that a the cancel event was requested by the user. If it is different, we assume that the
 * cancel event was requested due to a focus change and thus commit the change instead of canceling it.
 *
 * @param <S> The type of the TableView generic type (i.e., S == TableView&lt;S&gt;). This should also match with the
 *            first generic type in TableColumn.
 * @param <T> The type of the elements contained within the TableColumn.
 *
 * @author Ludwig Richter
 */
public class EditTextFieldTableCell<S, T> extends TableCell<S, T> {

	private static final double WRAPPED_TEXT_HORIZONTAL_PADDING = 10.;

	/**
	 * Provides a {{@link TextInputControl} that allows editing of the cell content when the cell is double-clicked, or
	 * when {@link TableView#edit} is called. By default, the control is not wrapping text and therefore is represented
	 * by a {@link TextField}. This method will only work on {@link TableColumn} instances which are of type String.
	 *
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
		return forTableColumn(new DefaultStringConverter(), false);
	}

	/**
	 * Provides a {@link TextInputControl} that allows editing of the cell content when the cell is double-clicked, or
	 * when {@link TableView#edit} is called. This method will only work on {@link TableColumn} instances which are of
	 * type String.
	 *
	 * @param wrapText if <code>true</code> the TextInputControl is a {@link TextArea}, otherwise a {@link TextField}
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn(final boolean wrapText) {
		return forTableColumn(new DefaultStringConverter(), wrapText);
	}

	/**
	 * Provides a {@link TextInputControl} that allows editing of the cell content when the cell is double-clicked, or
	 * when {@link TableView#edit} is called. By default, the control is not wrapping text and therefore is represented
	 * by a {@link TextField}. This method will work on any {@link TableColumn} instance, regardless of its generic
	 * type. However, to enable this, a {@link StringConverter} must be provided that will convert the given String
	 * (from what the user typed in) into an instance of type T. This item will then be passed along to the
	 * {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter} that can convert the given String (from what the user typed in) into
	 *            an instance of type T.
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
			final StringConverter<T> converter) {
		return list -> new EditTextFieldTableCell<>(converter, false);
	}

	/**
	 * Provides a {@link TextInputControl} that allows editing of the cell content when the cell is double-clicked, or
	 * when {@link TableView#edit} is called. This method will work on any {@link TableColumn} instance, regardless of
	 * its generic type. However, to enable this, a {@link StringConverter} must be provided that will convert the given
	 * String (from what the user typed in) into an instance of type T. This item will then be passed along to the
	 * {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter} that can convert the given String (from what the user typed in) into
	 *            an instance of type T.
	 * @param wrapText if <code>true</code> the TextInputControl is a {@link TextArea}, otherwise a {@link TextField}
	 * @return A {@link Callback} that can be inserted into the {@link TableColumn#cellFactoryProperty() cell factory
	 *         property} of a TableColumn, that enables textual editing of the content.
	 */
	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final StringConverter<T> converter,
			final boolean wrapText) {
		return list -> new EditTextFieldTableCell<>(converter, wrapText);
	}

	private TextInputControl textInputControl;

	/**
	 * Creates a default EditableTableCell with a null converter. Without a {@link StringConverter} specified, this cell
	 * will not be able to accept input from the TextInputControl (as it will not know how to convert this back to the
	 * domain object). It is therefore strongly encouraged to not use this constructor unless you intend to set the
	 * converter separately.
	 */
	public EditTextFieldTableCell() {
		this(null);
	}

	/**
	 * Creates a EditableTableCell that provides a {@link TextInputControl} when put into editing mode that allows
	 * editing of the cell content. This method will work on any TableColumn instance, regardless of its generic type.
	 * However, to enable this, a {@link StringConverter} must be provided that will convert the given String (from what
	 * the user typed in) into an instance of type T. This item will then be passed along to the
	 * {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param converter A {@link StringConverter converter} that can convert the given String (from what the user typed
	 *            in) into an instance of type T.
	 */
	public EditTextFieldTableCell(final StringConverter<T> converter) {
		this(converter, false);
	}

	/**
	 * Creates a EditableTableCell that provides a {@link TextInputControl} when put into editing mode that allows
	 * editing of the cell content. This method will work on any TableColumn instance, regardless of its generic type.
	 * However, to enable this, a {@link StringConverter} must be provided that will convert the given String (from what
	 * the user typed in) into an instance of type T. This item will then be passed along to the
	 * {@link TableColumn#onEditCommitProperty()} callback.
	 *
	 * @param wrapText if <code>true</code> the TextInputControl is a {@link TextArea}, otherwise a {@link TextField}
	 * @param converter A {@link StringConverter converter} that can convert the given String (from what the user typed
	 *            in) into an instance of type T.
	 */
	public EditTextFieldTableCell(final StringConverter<T> converter, final boolean wrapText) {
		this.getStyleClass().add("text-field-table-cell");
		setConverter(converter);
		setWrapText(wrapText);
	}

	private final ObjectProperty<StringConverter<T>> converter = new SimpleObjectProperty<>(this, "converter");

	/**
	 * The {@link StringConverter} property.
	 *
	 * @return the converter property
	 */
	public final ObjectProperty<StringConverter<T>> converterProperty() {
		return converter;
	}

	/**
	 * Sets the {@link StringConverter} to be used in this cell.
	 *
	 * @param value the new converter.
	 */
	public final void setConverter(final StringConverter<T> value) {
		converterProperty().set(value);
	}

	/**
	 * Returns the {@link StringConverter} used in this cell.
	 *
	 * @return the converter.
	 */
	public final StringConverter<T> getConverter() {
		return converterProperty().get();
	}

	protected TextInputControl getTextInputControl() {
		return textInputControl;
	}

	@Override
	public void startEdit() {
		super.startEdit();

		if (isEditing()) {
			ensureTextInputControlOfCorrectType();
			setText(null);
			setGraphic(textInputControl);
			resetTextInput();
			textInputControl.selectAll();
			textInputControl.requestFocus();
		}
	}

	@Override
	public void cancelEdit() {
		final String inputText = textInputControl.getText();
		/*
		 * =============================================================================================================
		 * PART OF EDIT HACK: if input text was changed we know that it wasn't the user who requested the canceling (or
		 * didn't change anything), but it was a focus change that caused cancelEdit() to be called. If so, we send a
		 * editCommitEvent for this TableCell before calling cancelEdit() cf. Bug
		 * "TableView does not commit values on focus lost": https://bugs.openjdk.java.net/browse/JDK-8089514
		 * =============================================================================================================
		 */
		if (!inputText.equals(getItemText())) {
			commitEdit(true);
		}
		super.cancelEdit();
		setText(inputText);
		setGraphic(null);
	}

	private void commitEdit(final boolean sendEditCommitEventOnly) {
		// try to convert the input text to an instance of type T. If it fails reset the text and re-throw exception.
		T newValue;
		try {
			if (getConverter() == null) {
				throw new IllegalStateException("Attempting to convert text input into Object, but provided "
						+ "StringConverter is null. Be sure to set a StringConverter in your cell factory.");
			}
			newValue = getConverter().fromString(textInputControl.getText());
		}
		catch (final Exception e) {
			resetTextInput();
			throw e;
		}

		/*
		 * =============================================================================================================
		 * PART OF EDIT HACK: if input text was changed we know that it wasn't the user who requested the canceling (or
		 * didn't change anything), but it was a focus change that caused cancelEdit() to be called. If so, we send a
		 * editCommitEvent for this TableCell before calling cancelEdit() cf. Bug
		 * "TableView does not commit values on focus lost": https://bugs.openjdk.java.net/browse/JDK-8089514
		 * =============================================================================================================
		 */
		if (sendEditCommitEventOnly) {
			fireCommitEvent(newValue);
		}
		else {
			super.commitEdit(newValue);
		}
	}

	private void fireCommitEvent(final T newValue) {
		final TableView<S> table = getTableView();
		final TableColumn<S, T> column = getTableColumn();
		final int row = getTableRow().getIndex();

		final TablePosition<S, ?> cellPosition = new TablePosition<>(table, row, column);
		@SuppressWarnings({ "rawtypes", "unchecked" })
		final CellEditEvent<S, ?> editEvent = new CellEditEvent(table, cellPosition, TableColumn.editCommitEvent(),
				newValue);
		Event.fireEvent(column, editEvent);
	}

	@Override
	public void updateItem(final T item, final boolean empty) {
		super.updateItem(item, empty);
		if (isEmpty()) {
			setText(null);
			setGraphic(null);
		}
		else {
			if (isEditing()) {
				updateToEditing();
			}
			else {
				updateToNonEditing();
			}
		}
	}

	private void updateToEditing() {
		resetTextInput();
		setText(null);
		setGraphic(textInputControl);
	}

	private void updateToNonEditing() {
		if (isWrapText()) {
			setText(null);
			final Text text = new Text(getItemText());
			setGraphic(text);
			setPrefHeight(Region.USE_COMPUTED_SIZE);
			text.wrappingWidthProperty().bind(widthProperty().subtract(WRAPPED_TEXT_HORIZONTAL_PADDING));
			setWrapText(true);
		}
		else {
			setText(getItemText());
			setGraphic(null);
		}
	}

	private String getItemText() {
		return getConverter() == null ? getItem() == null ? "" : getItem().toString()
				: getConverter().toString(getItem());
	}

	private void ensureTextInputControlOfCorrectType() {
		if (isWrapText()) {
			if (textInputControl == null || textInputControl instanceof TextField) {
				final TextArea textArea = new TextArea(getItemText());
				textArea.setWrapText(true);
				textInputControl = textArea;
				configTextInputField();
			}
		}
		else {
			if (textInputControl == null || textInputControl instanceof TextArea) {
				textInputControl = new TextField(getItemText());
				configTextInputField();
			}
		}

	}

	private void configTextInputField() {
		// To me it is unclear why this is necessary, but was taken from JavaFX code-base so I assume it has a reason
		textInputControl.setOnInputMethodTextChanged(event -> {
			commitEdit(false);
			event.consume();
		});

		textInputControl.focusedProperty().addListener((observable, oldValue, newValue) -> {
			if (!newValue && isEditing()) {
				commitEdit(false);
			}
		});

		textInputControl.setOnKeyPressed(keyEvent -> {
			/*
			 * EDIT TEXT COMMAND
			 */
			// insert a line-break on SHIFT-ENTER
			// Note: we enforce "\n" instead of System.lineSeparator() since TextInputControl seems to ignore OS
			// dependent line-breaks like "\r\n" for Windows (cf. comment in http://stackoverflow.com/a/28397763)
			if (keyEvent.isShiftDown() && (keyEvent.getCode() == KeyCode.ENTER)) {
				textInputControl.insertText(textInputControl.getCaretPosition(), "\n");
				keyEvent.consume();
			}

			/*
			 * COMMIT EDIT COMMANDS
			 */

			// commit the edit on ENTER
			else if (keyEvent.getCode() == KeyCode.ENTER) {
				commitEdit(false);
				keyEvent.consume();
			}

			// commit the edit on TAB and jump to next cell in row (or the previous one if SHIFT-TAB was pressed)
			else if (keyEvent.getCode() == KeyCode.TAB) {
				commitEdit(false);
				final TableColumn<S, ?> nextColumn = getNextColumn(getTableView(), getTableColumn(),
						!keyEvent.isShiftDown());
				if (nextColumn != null) {
					getTableView().edit(getTableRow().getIndex(), nextColumn);
				}
				keyEvent.consume();
			}

			/*
			 * CANCEL EDIT COMMANDS
			 */

			// cancel the edit on ESCAPE
			else if (keyEvent.getCode() == KeyCode.ESCAPE) {
				/*
				 * ====================================================================================================
				 * PART OF EDIT HACK: resetting text so we know in cancelEdit() that it was the user who requested the
				 * canceling and not a focus change. Consequently, the canceling semantic is only applied if pressing
				 * ESCAPE!! cf. Bug "TableView does not commit values on focus lost":
				 * https://bugs.openjdk.java.net/browse/JDK-8089514
				 * ====================================================================================================
				 */
				resetTextInput();
				cancelEdit();
				keyEvent.consume();
			}

		});
	}

	private void resetTextInput() {
		textInputControl.setText(getItemText());
	}

	/*
	 * Code taken from: https://gist.github.com/abhinayagarwal/9383881
	 */
	private static <S> TableColumn<S, ?> getNextColumn(final TableView<S> tableView,
			final TableColumn<S, ?> currentTableColumn, final boolean forward) {
		final List<TableColumn<S, ?>> columns = new ArrayList<>();
		for (final TableColumn<S, ?> column : tableView.getColumns()) {
			columns.addAll(getLeaves(column));
		}
		// there is no other column that supports editing
		if (columns.size() < 2) {
			return null;
		}
		final int currentIndex = columns.indexOf(currentTableColumn);
		int nextIndex = currentIndex;
		if (forward) {
			nextIndex++;
			if (nextIndex > columns.size() - 1) {
				nextIndex = 0;
			}
		}
		else {
			nextIndex--;
			if (nextIndex < 0) {
				nextIndex = columns.size() - 1;
			}
		}
		return columns.get(nextIndex);
	}

	private static <S> List<TableColumn<S, ?>> getLeaves(final TableColumn<S, ?> root) {
		final List<TableColumn<S, ?>> columns = new ArrayList<>();
		if (root.getColumns().isEmpty()) {
			// we only want the leaves that are editable
			if (root.isEditable()) {
				columns.add(root);
			}
			return columns;
		}
		for (final TableColumn<S, ?> column : root.getColumns()) {
			columns.addAll(getLeaves(column));
		}
		return columns;
	}

}