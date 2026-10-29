package net.techmayhem.clocktrack.ui.component;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.ui.Layout;

public class StatBlocks extends HBox {

    public StatBlocks(StatBlock... blocks) {
        super(Layout.SPACING);
        ObservableList<Node> children = getChildren();
        for (StatBlock block : blocks) {
            Text value = new Text(block.value);
            value.setStyle("-fx-font-weight: bold; -fx-font-size: 26px;");
            VBox blockBox = new VBox(Layout.SPACING, value, new Text(block.label));

            blockBox.setBackground(Background.fill(Color.WHITE));
            blockBox.setPadding(new Insets(Layout.PADDING));
            blockBox.setBorder(Border.stroke(Color.gray(0.75)));

            HBox.setHgrow(blockBox, Priority.ALWAYS);
            blockBox.setMaxWidth(Double.MAX_VALUE);
            blockBox.setPrefWidth(0);
            blockBox.setMinWidth(0);

            children.add(blockBox);
        }
    }

    public record StatBlock(String value, String label) {}
}
