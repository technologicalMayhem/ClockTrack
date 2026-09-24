package net.techmayhem.clocktrack.screens.people;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import net.techmayhem.clocktrack.models.Person;
import net.techmayhem.clocktrack.screens.Screen;

public class PersonView extends Screen {
    private final StackPane root;
    private final Person person;
    private final String name;

    PersonView(Person person) {
        this.person = person;
        root = new StackPane();
        root.getChildren().add(new Text("Hello, my name is " + person.name()));
        name = "View: " + person.name();
    }

    @Override
    protected Parent getView() {
        return root;
    }

    @Override
    protected String getName() {
        return name;
    }
}
