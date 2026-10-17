package com.campusconnect.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Reusable JDBC Utility for MySQL Database Connection Management.
 * Supports credential resolution via environment variables, system properties,
 * external db.properties configuration files, and safe defaults.
 */
public class DBUtil {

    private static final Logger LOGGER = Logger.getLogger(DBUtil.class.getName());

    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/campus_connect?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DEFAULT_USER = "root";
    private static final String DEFAULT_PASSWORD = "";

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static boolean driverLoaded = false;

    static {
        loadDriver();
        loadConfiguration();
    }

    private static void loadDriver() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            driverLoaded = true;
            LOGGER.info("MySQL JDBC Driver registered successfully.");
        } catch (ClassNotFoundException e) {
            LOGGER.log(Level.SEVERE, "Failed to load MySQL JDBC Driver", e);
        }
    }

    /**
     * Loads database connection settings in priority order:
     * 1. JVM System Properties (db.url, db.user, db.password)
     * 2. Environment Variables (DB_URL, DB_USER, DB_PASSWORD or MYSQL_URL, MYSQL_USER, MYSQL_PASSWORD)
     * 3. Classpath / file db.properties
     * 4. Safe defaults
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try classpath resource db.properties
        try (InputStream in = DBUtil.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                props.load(in);
                LOGGER.info("Loaded database configuration from classpath db.properties");
            }
        } catch (IOException e) {
            LOGGER.log(Level.FINE, "No classpath db.properties found: " + e.getMessage());
        }

        // 2. Try filesystem db.properties if not already populated
        if (props.isEmpty()) {
            File[] candidateFiles = new File[]{
                    new File("db.properties"),
                    new File("config/db.properties"),
                    new File("src/main/resources/db.properties")
            };
            for (File file : candidateFiles) {
                if (file.exists() && file.isFile()) {
                    try (FileInputStream fis = new FileInputStream(file)) {
                        props.load(fis);
                        LOGGER.info("Loaded database configuration from file: " + file.getAbsolutePath());
                        break;
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Failed reading config from " + file.getName(), e);
                    }
                }
            }
        }

        // 3. Resolve URL
        String url = System.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) url = System.getenv("DB_URL");
        if (url == null || url.trim().isEmpty()) url = System.getenv("MYSQL_URL");
        if (url == null || url.trim().isEmpty()) url = props.getProperty("db.url");
        if (url == null || url.trim().isEmpty()) url = DEFAULT_URL;
        dbUrl = url.trim();

        // 4. Resolve User
        String user = System.getProperty("db.user");
        if (user == null || user.trim().isEmpty()) user = System.getenv("DB_USER");
        if (user == null || user.trim().isEmpty()) user = System.getenv("MYSQL_USER");
        if (user == null || user.trim().isEmpty()) user = props.getProperty("db.user");
        if (user == null || user.trim().isEmpty()) user = DEFAULT_USER;
        dbUser = user.trim();

        // 5. Resolve Password
        String password = System.getProperty("db.password");
        if (password == null) password = System.getenv("DB_PASSWORD");
        if (password == null) password = System.getenv("MYSQL_PASSWORD");
        if (password == null) password = props.getProperty("db.password");
        if (password == null) password = DEFAULT_PASSWORD;
        dbPassword = password.trim();

        LOGGER.info(String.format("Configured MySQL Connection -> URL: %s | User: %s", dbUrl, dbUser));
    }

    /**
     * Obtains a new JDBC Connection to the MySQL database.
     */
    public static Connection getConnection() throws SQLException {
        if (!driverLoaded) {
            loadDriver();
        }
        return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
    }

    /**
     * Checks if a live connection to the database can be established.
     */
    public static boolean isConnected() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(2);
        } catch (SQLException e) {
            LOGGER.log(Level.FINE, "Database connectivity test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * Safely rolls back a transaction.
     */
    public static void rollback(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Failed to rollback transaction", e);
            }
        }
    }

    /**
     * Safely closes one or more JDBC resources (Connection, Statement, ResultSet).
     */
    public static void close(AutoCloseable... resources) {
        if (resources == null) return;
        for (AutoCloseable res : resources) {
            if (res != null) {
                try {
                    res.close();
                } catch (Exception ignored) {
                }
            }
        }
    }

    /**
     * Executes the SQL schema & seed script from file or classpath resource.
     */
    public static synchronized boolean executeSqlScript(String scriptPathOrResource) {
        InputStream stream = null;
        try {
            File f = new File(scriptPathOrResource);
            if (f.exists() && f.isFile()) {
                stream = new FileInputStream(f);
            } else {
                stream = DBUtil.class.getClassLoader().getResourceAsStream(scriptPathOrResource);
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
            close(stream);
        }
    }

    // Getters for configuration metadata
    public static String getDbUrl() { return dbUrl; }
    public static String getDbUser() { return dbUser; }
}
