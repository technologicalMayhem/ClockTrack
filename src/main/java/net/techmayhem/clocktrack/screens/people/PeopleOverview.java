package net.techmayhem.clocktrack.screens.people;

import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import net.techmayhem.clocktrack.models.Person;
import net.techmayhem.clocktrack.screens.Overview;
import net.techmayhem.clocktrack.screens.Screen;

import java.util.function.Consumer;

public class PeopleOverview extends Overview {
    private final StackPane root;

    public PeopleOverview(Consumer<Screen> createTabCallback) {
        super(createTabCallback);
        root = new StackPane();
        Button button = new Button("Open person");
        button.setOnAction(_ -> editPerson());
        root.getChildren().add(button);
    }

    @Override
    protected String getSectionName() {
        return "People";
    }

    @Override
    protected Parent getView() {
        return root;
    }

    private void editPerson() {
        Person person = new Person("Mayo");
        createTabCallback.accept(new PersonView(person));
    }
}
