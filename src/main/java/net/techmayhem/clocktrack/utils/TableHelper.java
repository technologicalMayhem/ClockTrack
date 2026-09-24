package net.techmayhem.clocktrack.utils;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.TableView;

public class TableHelper {
    @SafeVarargs
    public static <T> void buildTableColumns(TableView<T> tableView, TableColumn<T>... columns) {
        ObservableList<javafx.scene.control.TableColumn<T, ?>> tableViewColumns = tableView.getColumns();
        for (TableColumn<T> column : columns) {
            javafx.scene.control.TableColumn<T, String> tableColumn = new javafx.scene.control.TableColumn<>(column.name());
            tableColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(column.valueMap().apply(cellData.getValue())));
            tableViewColumns.add(tableColumn);
        }
    }
}
