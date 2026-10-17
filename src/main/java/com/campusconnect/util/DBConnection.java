package com.campusconnect.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility for establishing MySQL JDBC connections.
 * Resolves credentials via environment variables DB_USER and DB_PASS.
 */
public class DBConnection {

    private static final Logger LOGGER = Logger.getLogger(DBConnection.class.getName());
    private static final String URL = "jdbc:mysql://localhost:3306/campus_connect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load MySQL JDBC Driver", e);
        }
    }

    /**
     * Obtains a live JDBC Connection to the MySQL database.
     * User and password are read from environment variables DB_USER and DB_PASS,
     * falling back to System properties (db.user, db.pass) and then defaults (root / empty string).
     */
    public static Connection getConnection() throws SQLException {
        String user = System.getenv("DB_USER");
        if (user == null || user.trim().isEmpty()) {
            user = System.getProperty("db.user", DEFAULT_USER);
        }

        String pass = System.getenv("DB_PASS");
        if (pass == null) {
            pass = System.getenv("DB_PASSWORD");
        }
        if (pass == null) {
            pass = System.getProperty("db.pass", System.getProperty("db.password", DEFAULT_PASS));
        }

        return DriverManager.getConnection(URL, user.trim(), pass);
    }

    /**
     * Checks if the database is reachable.
     */
    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Executes SQL statements from a schema or script file.
     */
    public static synchronized boolean executeSqlScript(String scriptPathOrResource) {
        InputStream stream = null;
        try {
            File f = new File(scriptPathOrResource);
            if (f.exists() && f.isFile()) {
                stream = new FileInputStream(f);
            } else {
                stream = DBConnection.class.getClassLoader().getResourceAsStream(scriptPathOrResource);
            }

            if (stream == null) {
                LOGGER.warning("SQL script not found: " + scriptPathOrResource);
                return false;
            }

            StringBuilder sb = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.startsWith("--") || line.startsWith("//") || line.isEmpty()) {
                        continue;
                    }
                    sb.append(line).append(" ");
                }
            }

            String[] statements = sb.toString().split(";");
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (!trimmed.isEmpty()) {
                        try {
                            stmt.execute(trimmed);
                        } catch (SQLException ex) {
                            LOGGER.log(Level.WARNING, "Error executing SQL statement: " + trimmed, ex);
                        }
                    }
                }
            }
            return true;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed executing SQL script: " + scriptPathOrResource, e);
            return false;
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (Exception ignored) {}
            }
        }
    }
}
