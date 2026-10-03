package net.techmayhem.clocktrack.ui.component;

import java.util.function.Function;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import org.jspecify.annotations.Nullable;

public class TableHelper {
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

    public static class DisplayCell<T, V> extends TableCell<T, @Nullable V> {
        private final Function<V, String> formatter;

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
