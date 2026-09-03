package net.techmayhem.clocktrack;

import net.techmayhem.clocktrack.Database.Result;
import net.techmayhem.clocktrack.models.FromDb;
import net.techmayhem.clocktrack.models.Person;
import org.tinylog.Logger;

import java.util.List;

public class Main {
    public static void main(String[] args) {
        Database database = Database.getInstance();

        database.initSchema();

        Logger.info("Inserting people");
        for (String name : new String[]{"Adam", "Sam", "Kim"}) {
            database.insertPerson(new Person(name));
        }
        Logger.info("Successfully inserted Persons");

        int id = 2;
        switch (database.getPerson(id)) {
            case Result.Err<FromDb<Person>> err ->
                    Logger.error("Failed to get Person: {}", err.message());
            case Result.Ok<FromDb<Person>> result -> {
                Logger.info("Id {} is {}", id, result.value().model().name);
                FromDb<Person> dbPerson = result.value();
                dbPerson.model().name = "Alex";
                Result<Void> updateResult =  database.updatePerson(dbPerson);
                if (updateResult instanceof Result.Err<Void>(String message)) {
                    Logger.error("Failed to update Person: {}", message);
                }
            }
        }

        Result<Void> deleteResult = database.deletePerson(1);
        if (deleteResult instanceof Result.Err<Void>(String message)) {
            Logger.error("Failed to delete Person: {}", message);
        }

        switch (database.getAllPersons()) {
            case Result.Err<List<FromDb<Person>>> err ->
                    Logger.error("Failed to get Persons: {}", err.message());
            case Result.Ok<List<FromDb<Person>>> result -> {
                StringBuilder builder = new StringBuilder();
                builder.append("People in Database:");
                for (FromDb<Person> personFromDb : result.value()) {
                    builder.append("\n");
                    builder.append(personFromDb.id());
                    builder.append(": ");
                    builder.append(personFromDb.model().name);
                }

                Logger.info(builder.toString());
            }
        }

        Logger.info("App ran successfully!");
    }
}