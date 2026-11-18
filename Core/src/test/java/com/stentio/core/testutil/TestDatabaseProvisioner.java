package com.stentio.core.testutil;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class TestDatabaseProvisioner {
    private static final int ADVISORY_LOCK_NAMESPACE = 20261118;
    private static final int ADVISORY_LOCK_KEY = 2201;

    private TestDatabaseProvisioner() {
    }

    public static void main(String[] args) throws SQLException {
        String host = requiredEnvironmentVariable("POSTGRES_HOST");
        String port = requiredEnvironmentVariable("POSTGRES_PORT");
        String database = requiredEnvironmentVariable("POSTGRES_DB");
        String username = requiredEnvironmentVariable("POSTGRES_USER");
        String password = requiredEnvironmentVariable("POSTGRES_PASSWORD");
        String adminUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";

        try (Connection connection = DriverManager.getConnection(adminUrl, username, password)) {
            lockProvisioning(connection);
            try {
                if (databaseExists(connection, database)) {
                    System.out.println("PostgreSQL test database is ready.");
                } else {
                    createDatabase(connection, database);
                    System.out.println("Created PostgreSQL test database.");
                }
            } finally {
                unlockProvisioning(connection);
            }
        }
    }

    private static String requiredEnvironmentVariable(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Required environment variable is missing: " + name);
        }
        return value;
    }

    private static void lockProvisioning(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_lock(" + ADVISORY_LOCK_NAMESPACE + ", " + ADVISORY_LOCK_KEY + ")");
        }
    }

    private static void unlockProvisioning(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SELECT pg_advisory_unlock(" + ADVISORY_LOCK_NAMESPACE + ", " + ADVISORY_LOCK_KEY + ")");
        }
    }

    private static boolean databaseExists(Connection connection, String database) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT 1 FROM pg_database WHERE datname = ?")) {
            statement.setString(1, database);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private static void createDatabase(Connection connection, String database) throws SQLException {
        String quotedDatabase = "\"" + database.replace("\"", "\"\"") + "\"";
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + quotedDatabase);
        }
    }
}
