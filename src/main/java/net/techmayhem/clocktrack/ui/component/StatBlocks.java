package net.techmayhem.clocktrack.ui.component;

import javafx.beans.binding.Bindings;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.ui.Layout;
import net.techmayhem.clocktrack.ui.Styles;

public class StatBlocks extends HBox {

    private static final double GRAY_INTENSITY = 0.75;

    public StatBlocks(StatBlock... blocks) {
        super(Layout.SPACING);
        ObservableList<Node> children = getChildren();
        for (StatBlock block : blocks) {
            Text value = new Text(block.value);
            value.getStyleClass().add(Styles.TITLE_TEXT);
            Text text = new Text(block.label);
            VBox blockBox = new VBox(Layout.SPACING, value, text);

            blockBox.setBackground(Background.fill(Color.WHITE));
            blockBox.setPadding(new Insets(Layout.PADDING));
            blockBox.setBorder(Border.stroke(Color.gray(GRAY_INTENSITY)));

            HBox.setHgrow(blockBox, Priority.ALWAYS);
            blockBox.setMaxWidth(Double.MAX_VALUE);
            blockBox.setPrefWidth(0);
            blockBox.setMinWidth(0);

            text.wrappingWidthProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> blockBox.getWidth() - blockBox.snappedLeftInset() - blockBox.snappedRightInset(),
                            blockBox.widthProperty()));

            children.add(blockBox);
        }
    }

    public record StatBlock(String value, String label) {}
}
