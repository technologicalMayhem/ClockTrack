package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jspecify.annotations.Nullable;

/// Builds table columns from [ColumnDef]s.
public class TableHelper {
    /// Adds a column to the table for each of the given definitions.
    ///
    /// @param table The table to add the columns to
    /// @param columns The definitions of the columns
    /// @param <T> The type of the table rows
    @SafeVarargs
    public static <T> void buildTableColumns(TableView<T> table, ColumnDef<T, ?>... columns) {
        for (ColumnDef<T, ?> column : columns) {
            table.getColumns().add(buildColumn(column));
        }
    }

    private static <T, V> TableColumn<T, @Nullable V> buildColumn(ColumnDef<T, V> def) {
        TableColumn<T, @Nullable V> col = new TableColumn<>(def.name());
        col.setCellValueFactory(cd -> new ReadOnlyObjectWrapper<>(def.value().apply(cd.getValue())));
        col.setCellFactory(_ -> new DisplayCell<>(def.display()));
        return col;
    }

    /// A table cell that shows its value as formatted text. Empty cells and null values show no text.
    ///
    /// @param <T> The type of the table rows
    /// @param <V> The type of the cell value
    public static class DisplayCell<T, V> extends TableCell<T, @Nullable V> {
        private final Function<V, String> formatter;

        /// @param formatter The function that formats the value
        public DisplayCell(Function<V, String> formatter) {
            this.formatter = formatter;
        }

        @Override
        protected void updateItem(@Nullable V item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : formatter.apply(item));
        }
    }
}
