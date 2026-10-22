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
    private static final java.util.Properties PROPS = new java.util.Properties();
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/campus_connect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASS = "";

    static {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            LOGGER.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load MySQL JDBC Driver", e);
        }
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                PROPS.load(in);
            }
        } catch (Exception ignored) {}

        File propFile = new File("db.properties");
        if (propFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propFile)) {
                PROPS.load(fis);
            } catch (Exception ignored) {}
        }
    }

    public static String getJdbcUrl() {
        String envUrl = System.getenv("DB_URL");
        if (envUrl == null || envUrl.trim().isEmpty()) {
            envUrl = System.getenv("MYSQL_URL");
        }
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            return envUrl.trim();
        }

        String propUrl = System.getProperty("db.url");
        if (propUrl != null && !propUrl.trim().isEmpty()) {
            return propUrl.trim();
        }

        String fileUrl = PROPS.getProperty("db.url");
        if (fileUrl != null && !fileUrl.trim().isEmpty()) {
            return fileUrl.trim();
        }

        String host = System.getenv("DB_HOST");
        if (host != null && !host.trim().isEmpty()) {
            String port = System.getenv("DB_PORT");
            if (port == null || port.trim().isEmpty()) port = "3306";
            String db = System.getenv("DB_NAME");
            if (db == null || db.trim().isEmpty()) db = "campus_connect";
            return "jdbc:mysql://" + host.trim() + ":" + port.trim() + "/" + db.trim() + "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
        }

        return DEFAULT_URL;
    }

    /**
     * Obtains a live JDBC Connection to the MySQL database.
     * User and password are read from environment variables DB_USER and DB_PASS,
     * falling back to System properties (db.user, db.pass) and then defaults (root / empty string).
     */
    public static Connection getConnection() throws SQLException {
        String user = System.getenv("DB_USER");
        if (user == null || user.trim().isEmpty()) {
            user = System.getenv("DB_USERNAME");
        }
        if (user == null || user.trim().isEmpty()) {
            user = System.getProperty("db.user", PROPS.getProperty("db.user", DEFAULT_USER));
        }

        String pass = System.getenv("DB_PASS");
        if (pass == null) {
            pass = System.getenv("DB_PASSWORD");
        }
        if (pass == null) {
            pass = System.getProperty("db.pass", System.getProperty("db.password", PROPS.getProperty("db.password", PROPS.getProperty("db.pass", DEFAULT_PASS))));
        }

        return DriverManager.getConnection(getJdbcUrl(), user.trim(), pass != null ? pass : "");
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
