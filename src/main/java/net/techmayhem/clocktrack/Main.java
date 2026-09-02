package net.techmayhem.clocktrack;

import org.tinylog.Logger;

import java.sql.SQLException;

public class Main {
    public static void main(String[] args) {
        Database database = Database.getInstance();
        try {
            database.initSchema();
            database.close();
        } catch (SQLException e) {
            System.err.println("Error: " + e.getMessage());
        }

        Logger.info("App ran successfully!");
    }
}