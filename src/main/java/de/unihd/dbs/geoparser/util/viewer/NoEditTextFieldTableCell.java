package de.unihd.dbs.geoparser.util.viewer;

import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TextInputControl;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

/**
 * Equivalent to {@link EditTextFieldTableCell} using all its features, but prohibiting actual editing by setting the
 * {@link TextInputControl} to non-editable.
 *
 * @param <S> The type of the TableView generic type (i.e., S == TableView&lt;S&gt;). This should also match with the
 *            first generic type in TableColumn.
 * @param <T> The type of the elements contained within the TableColumn.
 *
 * @author Ludwig Richter
 */
public class NoEditTextFieldTableCell<S, T> extends EditTextFieldTableCell<S, T> {

	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn() {
		return forTableColumn(new DefaultStringConverter(), false);
	}

	public static <S> Callback<TableColumn<S, String>, TableCell<S, String>> forTableColumn(final boolean wrapText) {
		return forTableColumn(new DefaultStringConverter(), wrapText);
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(
			final StringConverter<T> converter) {
		return list -> new NoEditTextFieldTableCell<>(converter, false);
	}

	public static <S, T> Callback<TableColumn<S, T>, TableCell<S, T>> forTableColumn(final StringConverter<T> converter,
			final boolean wrapText) {
		return list -> new NoEditTextFieldTableCell<>(converter, wrapText);
	}

	public NoEditTextFieldTableCell() {
		this(null);
	}

	public NoEditTextFieldTableCell(final StringConverter<T> converter) {
		this(converter, false);
	}

	public NoEditTextFieldTableCell(final StringConverter<T> converter, final boolean wrapText) {
		this.getStyleClass().add("text-field-table-cell");
		setConverter(converter);
		setWrapText(wrapText);
	}

	@Override
	public void startEdit() {
		super.startEdit();

		if (isEditing()) {
			getTextInputControl().setEditable(false);
		}
	}

}