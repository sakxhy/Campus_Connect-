package com.campusconnect.service;

import com.campusconnect.model.*;
import com.campusconnect.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service Layer & Database Access Object for CampusConnect.
 * Replaces thread-safe in-memory collections with persistent MySQL JDBC operations.
 * Implements prepared statements, resource management, transactions, and robust exception handling.
 */
public class DataStore {

    private static final Logger LOGGER = Logger.getLogger(DataStore.class.getName());
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DataStore() {
        initDatabase();
    }

    private void initDatabase() {
        try {
            if (DBUtil.isConnected()) {
                LOGGER.info("Connected to MySQL database. Verifying database tables...");
                ensureSchemaInitialized();
            } else {
                LOGGER.warning("MySQL database not reachable on startup. Please verify db.properties or environment variables.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing database connection: " + e.getMessage(), e);
        }
    }

    private void ensureSchemaInitialized() {
        try (Connection conn = DBUtil.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tickets", null)) {
            if (!rs.next()) {
                LOGGER.info("Tables not found. Initializing schema from campus_connect_schema.sql...");
                DBUtil.executeSqlScript("campus_connect_schema.sql");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not check existing tables", e);
        }
    }

    /**
     * Resets database records to the default seed data defined in campus_connect_schema.sql.
     */
    public synchronized void resetToInitialData() {
        LOGGER.info("Resetting database to initial seed data...");
        boolean success = DBUtil.executeSqlScript("campus_connect_schema.sql");
        if (!success) {
            LOGGER.warning("Could not reset database using campus_connect_schema.sql directly from root path. Attempting fallback...");
            DBUtil.executeSqlScript("src/main/resources/campus_connect_schema.sql");
        }
    }

    /**
     * Adds a new User (and corresponding student or admin record) within a transaction.
     */
    public void addUser(User u) {
        if (u == null || u.getEmail() == null) return;

        String insertUserSql = "INSERT INTO users (name, email, password_hash, role, phone) VALUES (?, ?, ?, ?, ?)";
        String insertStudentSql = "INSERT INTO students (user_id, student_id, department) VALUES (?, ?, ?)";
        String insertAdminSql = "INSERT INTO admins (user_id, department, title) VALUES (?, ?, ?)";

        Connection conn = null;
        PreparedStatement pUser = null;
        PreparedStatement pRole = null;
        ResultSet rsKeys = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            pUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS);
            pUser.setString(1, u.getName() != null ? u.getName().trim() : "Unknown");
            pUser.setString(2, u.getEmail().trim().toLowerCase());
            pUser.setString(3, u.getPassword() != null ? u.getPassword() : "");
            pUser.setString(4, u.getRole() != null ? u.getRole().toLowerCase() : "student");
            pUser.setString(5, u.getPhone() != null ? u.getPhone().trim() : null);

            pUser.executeUpdate();
            rsKeys = pUser.getGeneratedKeys();

            int userId = 0;
            if (rsKeys.next()) {
                userId = rsKeys.getInt(1);
                u.setId(String.valueOf(userId));
            }

            if ("student".equalsIgnoreCase(u.getRole())) {
                pRole = conn.prepareStatement(insertStudentSql);
                pRole.setInt(1, userId);
                pRole.setString(2, u.getStudentId() != null ? u.getStudentId().trim() : "ID-" + userId);
                pRole.setString(3, u.getDepartment() != null ? u.getDepartment().trim() : "General");
                pRole.executeUpdate();
            } else if ("admin".equalsIgnoreCase(u.getRole())) {
                pRole = conn.prepareStatement(insertAdminSql);
                pRole.setInt(1, userId);
                pRole.setString(2, u.getDepartment() != null ? u.getDepartment().trim() : "Campus Grievance & Operations Cell");
                pRole.setString(3, u.getTitle() != null ? u.getTitle().trim() : "Staff");
                pRole.executeUpdate();
            }

            conn.commit();
            LOGGER.info("User registered successfully in database with ID: " + userId);
        } catch (SQLException e) {
            DBUtil.rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to insert user: " + u.getEmail(), e);
            throw new RuntimeException("Database error saving user", e);
        } finally {
            DBUtil.close(rsKeys, pUser, pRole, conn);
        }
    }

    /**
     * Finds a user by email address, fetching polymorphic student or admin fields.
     */
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;

        String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                "FROM users u " +
                "LEFT JOIN students s ON u.user_id = s.user_id " +
                "LEFT JOIN admins a ON u.user_id = a.user_id " +
                "WHERE LOWER(u.email) = LOWER(?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim().toLowerCase());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user by email: " + email, e);
        }
        return null;
    }

    /**
     * Finds a user by ID.
     */
    public User getUserById(String id) {
        if (id == null || id.trim().isEmpty()) return null;

        // Support string demo IDs from previous implementation
        if ("usr_student_1".equalsIgnoreCase(id.trim())) return getUserByEmail("alex.student@college.edu");
        if ("usr_admin_1".equalsIgnoreCase(id.trim())) return getUserByEmail("admin@college.edu");
        if ("usr_student_2".equalsIgnoreCase(id.trim())) return getUserByEmail("priya.s@college.edu");

        int numericId;
        try {
            numericId = Integer.parseInt(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }

        String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                "FROM users u " +
                "LEFT JOIN students s ON u.user_id = s.user_id " +
                "LEFT JOIN admins a ON u.user_id = a.user_id " +
                "WHERE u.user_id = ?";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, numericId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching user by id: " + id, e);
        }
        return null;
    }

    /**
     * Retrieves the default demo user for a given role (student or admin).
     */
    public User getDemoUser(String role) {
        String roleStr = role != null && !role.trim().isEmpty() ? role.trim().toLowerCase() : "student";
        String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                "FROM users u " +
                "LEFT JOIN students s ON u.user_id = s.user_id " +
                "LEFT JOIN admins a ON u.user_id = a.user_id " +
                "WHERE LOWER(u.role) = LOWER(?) ORDER BY u.user_id ASC LIMIT 1";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, roleStr);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapUserFromResultSet(rs);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching demo user for role: " + role, e);
        }
        return null;
    }

    /**
     * Queries grievance tickets with dynamic filtering and search parameters.
     */
    public List<Ticket> getTickets(String studentEmail, String status, String category, String priority, String search) {
        List<Ticket> list = new ArrayList<>();
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT t.ticket_id, t.title, t.description, c.name AS category_name, t.priority, t.status, ")
           .append("t.location, t.assigned_to, t.created_at, t.updated_at, ")
           .append("u.name AS submitter_name, u.email AS submitter_email, s.student_id AS submitter_student_id ")
           .append("FROM tickets t ")
           .append("JOIN categories c ON t.category_id = c.category_id ")
           .append("JOIN users u ON t.submitted_by = u.user_id ")
           .append("LEFT JOIN students s ON u.user_id = s.user_id ")
           .append("WHERE 1=1 ");

        List<Object> params = new ArrayList<>();

        if (studentEmail != null && !studentEmail.trim().isEmpty()) {
            sql.append("AND LOWER(u.email) = LOWER(?) ");
            params.add(studentEmail.trim());
        }
        if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
            sql.append("AND LOWER(t.status) = LOWER(?) ");
            params.add(status.trim());
        }
        if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category.trim())) {
            sql.append("AND LOWER(c.name) = LOWER(?) ");
            params.add(category.trim());
        }
        if (priority != null && !priority.trim().isEmpty() && !"all".equalsIgnoreCase(priority.trim())) {
            sql.append("AND LOWER(t.priority) = LOWER(?) ");
            params.add(priority.trim());
        }
        if (search != null && !search.trim().isEmpty()) {
            String wildcard = "%" + search.trim().toLowerCase() + "%";
            sql.append("AND (LOWER(t.ticket_id) LIKE ? OR LOWER(t.title) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(t.description) LIKE ? OR LOWER(t.location) LIKE ?) ");
            for (int i = 0; i < 5; i++) {
                params.add(wildcard);
            }
        }

        sql.append("ORDER BY t.created_at DESC");

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Ticket t = mapTicketSummary(rs);
                    loadTicketTimelineAndComments(conn, t);
                    list.add(t);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error querying tickets", e);
        }

        return list;
    }

    /**
     * Retrieves a single ticket with full timeline and comment conversation history.
     */
    public Ticket getTicketById(String id) {
        if (id == null || id.trim().isEmpty()) return null;

        String sql = "SELECT t.ticket_id, t.title, t.description, c.name AS category_name, t.priority, t.status, " +
                "t.location, t.assigned_to, t.created_at, t.updated_at, " +
                "u.name AS submitter_name, u.email AS submitter_email, s.student_id AS submitter_student_id " +
                "FROM tickets t " +
                "JOIN categories c ON t.category_id = c.category_id " +
                "JOIN users u ON t.submitted_by = u.user_id " +
                "LEFT JOIN students s ON u.user_id = s.user_id " +
                "WHERE LOWER(t.ticket_id) = LOWER(?)";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Ticket t = mapTicketSummary(rs);
                    loadTicketTimelineAndComments(conn, t);
                    return t;
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching ticket by ID: " + id, e);
        }
        return null;
    }

    /**
     * Creates a new ticket grievance with automatic sequential ID generation and initial audit timeline.
     */
    public synchronized Ticket createTicket(String title, String category, String priority, String location, String description, User user) {
        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            // 1. Generate sequential Ticket ID
            String ticketId = generateNextTicketId(conn);

            // 2. Resolve Category ID
            int categoryId = resolveCategoryId(conn, category);

            // 3. Resolve Submitting User ID
            int submitterId = resolveUserId(conn, user);

            String finalPriority = priority != null && !priority.trim().isEmpty() ? priority.trim() : "Medium";
            String finalLocation = location != null && !location.trim().isEmpty() ? location.trim() : "Campus Premises";
            String finalTitle = title != null && !title.trim().isEmpty() ? title.trim() : "Untitled Grievance";
            String finalDesc = description != null ? description.trim() : "";

            // 4. Insert Ticket
            String insertTicketSql = "INSERT INTO tickets " +
                    "(ticket_id, title, description, category_id, priority, status, location, submitted_by, assigned_to, created_at, updated_at) " +
                    "VALUES (?, ?, ?, ?, ?, 'open', ?, ?, 'Pending Assignment', NOW(), NOW())";

            try (PreparedStatement pt = conn.prepareStatement(insertTicketSql)) {
                pt.setString(1, ticketId);
                pt.setString(2, finalTitle);
                pt.setString(3, finalDesc);
                pt.setInt(4, categoryId);
                pt.setString(5, finalPriority);
                pt.setString(6, finalLocation);
                pt.setInt(7, submitterId);
                pt.executeUpdate();
            }

            // 5. Insert initial timeline item
            String insertTimelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, 'open', ?, NOW(), ?)";
            try (PreparedStatement ptime = conn.prepareStatement(insertTimelineSql)) {
                ptime.setString(1, ticketId);
                ptime.setString(2, "Ticket Submitted");
                ptime.setString(3, "New grievance logged by student.");
                ptime.executeUpdate();
            }

            conn.commit();
            LOGGER.info("Created new grievance ticket: " + ticketId);
            return getTicketById(ticketId);

        } catch (SQLException e) {
            DBUtil.rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to create ticket: " + title, e);
            throw new RuntimeException("Database error creating ticket", e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * Updates ticket status, writes to audit timeline, and appends admin comment if remark provided.
     */
    public synchronized Ticket updateTicketStatus(String id, String newStatus, String adminNote, User admin) {
        if (id == null || newStatus == null) return null;

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String statusNorm = newStatus.trim().toLowerCase();
            String updateSql = "UPDATE tickets SET status = ?, updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";
            try (PreparedStatement pu = conn.prepareStatement(updateSql)) {
                pu.setString(1, statusNorm);
                pu.setString(2, id.trim());
                int affected = pu.executeUpdate();
                if (affected == 0) {
                    conn.rollback();
                    return null;
                }
            }

            String label = "Status Updated";
            if ("open".equalsIgnoreCase(statusNorm)) label = "Status reset to Open";
            else if ("in_progress".equalsIgnoreCase(statusNorm)) label = "Marked In-Progress";
            else if ("resolved".equalsIgnoreCase(statusNorm)) label = "Ticket Resolved";
            else if ("closed".equalsIgnoreCase(statusNorm)) label = "Ticket Closed";

            String note = adminNote != null && !adminNote.trim().isEmpty() ? adminNote.trim() : "Status updated to " + statusNorm;
            String timelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, ?, ?, NOW(), ?)";
            try (PreparedStatement pt = conn.prepareStatement(timelineSql)) {
                pt.setString(1, id.trim());
                pt.setString(2, statusNorm);
                pt.setString(3, label);
                pt.setString(4, note);
                pt.executeUpdate();
            }

            if (adminNote != null && !adminNote.trim().isEmpty()) {
                int authorId = resolveAdminUserId(conn, admin);
                String commentSql = "INSERT INTO ticket_comments (ticket_id, author_id, role, comment_text, comment_date) VALUES (?, ?, 'admin', ?, NOW())";
                try (PreparedStatement pc = conn.prepareStatement(commentSql)) {
                    pc.setString(1, id.trim());
                    pc.setInt(2, authorId);
                    pc.setString(3, "[Status Update: " + statusNorm.toUpperCase() + "] " + adminNote.trim());
                    pc.executeUpdate();
                }
            }

            conn.commit();
            return getTicketById(id);

        } catch (SQLException e) {
            DBUtil.rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to update ticket status for: " + id, e);
            throw new RuntimeException("Database error updating ticket status", e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * Assigns ticket to department/staff and advances status to in_progress if currently open.
     */
    public synchronized Ticket assignTicket(String id, String assignee) {
        if (id == null || assignee == null || assignee.trim().isEmpty()) return null;

        Connection conn = null;
        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            String selectStatusSql = "SELECT status FROM tickets WHERE LOWER(ticket_id) = LOWER(?)";
            String curStatus = "open";
            try (PreparedStatement ps = conn.prepareStatement(selectStatusSql)) {
                ps.setString(1, id.trim());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        curStatus = rs.getString("status");
                    } else {
                        conn.rollback();
                        return null;
                    }
                }
            }

            String targetStatus = "open".equalsIgnoreCase(curStatus) ? "in_progress" : curStatus;
            String updateSql = "UPDATE tickets SET assigned_to = ?, status = ?, updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";
            try (PreparedStatement pu = conn.prepareStatement(updateSql)) {
                pu.setString(1, assignee.trim());
                pu.setString(2, targetStatus);
                pu.setString(3, id.trim());
                pu.executeUpdate();
            }

            String timelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, ?, 'Ticket Assigned', NOW(), ?)";
            try (PreparedStatement pt = conn.prepareStatement(timelineSql)) {
                pt.setString(1, id.trim());
                pt.setString(2, targetStatus);
                pt.setString(3, "Assigned to: " + assignee.trim());
                pt.executeUpdate();
            }

            conn.commit();
            return getTicketById(id);

        } catch (SQLException e) {
            DBUtil.rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to assign ticket: " + id, e);
            throw new RuntimeException("Database error assigning ticket", e);
        } finally {
            DBUtil.close(conn);
        }
    }

    /**
     * Appends a comment to a ticket thread.
     */
    public synchronized Comment addComment(String ticketId, String text, User user) {
        if (ticketId == null || text == null || text.trim().isEmpty()) return null;

        Connection conn = null;
        PreparedStatement pc = null;
        ResultSet rsKey = null;

        try {
            conn = DBUtil.getConnection();
            conn.setAutoCommit(false);

            int authorId = resolveUserId(conn, user);
            String role = (user != null && user.getRole() != null) ? user.getRole().toLowerCase() : "student";
            String authorName = (user != null && user.getName() != null) ? user.getName() : "Anonymous";
            String displayNow = LocalDateTime.now().format(DISPLAY_FMT);

            String commentSql = "INSERT INTO ticket_comments (ticket_id, author_id, role, comment_text, comment_date) VALUES (?, ?, ?, ?, NOW())";
            pc = conn.prepareStatement(commentSql, Statement.RETURN_GENERATED_KEYS);
            pc.setString(1, ticketId.trim());
            pc.setInt(2, authorId);
            pc.setString(3, role);
            pc.setString(4, text.trim());
            pc.executeUpdate();

            rsKey = pc.getGeneratedKeys();
            int commentId = 0;
            if (rsKey.next()) {
                commentId = rsKey.getInt(1);
            }

            String updateTicketSql = "UPDATE tickets SET updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";
            try (PreparedStatement pu = conn.prepareStatement(updateTicketSql)) {
                pu.setString(1, ticketId.trim());
                pu.executeUpdate();
            }

            conn.commit();
            return new Comment("c" + commentId, authorName, role, text.trim(), displayNow);

        } catch (SQLException e) {
            DBUtil.rollback(conn);
            LOGGER.log(Level.SEVERE, "Failed to add comment to ticket: " + ticketId, e);
            throw new RuntimeException("Database error adding comment", e);
        } finally {
            DBUtil.close(rsKey, pc, conn);
        }
    }

    /**
     * Calculates system-wide grievance metric statistics directly through an optimized SQL aggregation query.
     */
    public Stats getStats() {
        String sql = "SELECT " +
                "COUNT(*) AS total_count, " +
                "COALESCE(SUM(CASE WHEN LOWER(status) = 'open' THEN 1 ELSE 0 END), 0) AS open_count, " +
                "COALESCE(SUM(CASE WHEN LOWER(status) = 'in_progress' THEN 1 ELSE 0 END), 0) AS in_prog_count, " +
                "COALESCE(SUM(CASE WHEN LOWER(status) = 'resolved' THEN 1 ELSE 0 END), 0) AS resolved_count, " +
                "COALESCE(SUM(CASE WHEN LOWER(status) = 'closed' THEN 1 ELSE 0 END), 0) AS closed_count, " +
                "COALESCE(SUM(CASE WHEN LOWER(priority) = 'urgent' AND LOWER(status) NOT IN ('resolved', 'closed') THEN 1 ELSE 0 END), 0) AS urgent_count " +
                "FROM tickets";

        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return new Stats(
                        rs.getInt("total_count"),
                        rs.getInt("open_count"),
                        rs.getInt("in_prog_count"),
                        rs.getInt("resolved_count"),
                        rs.getInt("closed_count"),
                        rs.getInt("urgent_count")
                );
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error calculating statistics from database", e);
        }

        return new Stats(0, 0, 0, 0, 0, 0);
    }

    // -------------------------------------------------------------------------
    // Internal Helper & Mapping Methods
    // -------------------------------------------------------------------------

    private User mapUserFromResultSet(ResultSet rs) throws SQLException {
        String password = rs.getString("password_hash");
        // Normalize demo hash passwords to their plain-text equivalents for login compatibility
        if ("$2a$12$e8x/demo_hash_student_password123".equals(password)) {
            password = "password123";
        } else if ("$2a$12$e8x/demo_hash_admin_admin123".equals(password)) {
            password = "admin123";
        } else if ("$2a$12$e8x/demo_hash_student_priya123".equals(password)) {
            password = "password123";
        }

        return new User(
                String.valueOf(rs.getInt("user_id")),
                rs.getString("name"),
                rs.getString("email"),
                password,
                rs.getString("role"),
                rs.getString("student_id"),
                rs.getString("department"),
                rs.getString("phone"),
                rs.getString("title")
        );
    }

    private Ticket mapTicketSummary(ResultSet rs) throws SQLException {
        String id = rs.getString("ticket_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String category = rs.getString("category_name");
        String priority = rs.getString("priority");
        String status = rs.getString("status");
        String location = rs.getString("location");
        String assignedTo = rs.getString("assigned_to");

        Timestamp createdTs = rs.getTimestamp("created_at");
        Timestamp updatedTs = rs.getTimestamp("updated_at");

        String createdAt = createdTs != null ? createdTs.toLocalDateTime().format(ISO_FMT) : LocalDateTime.now().format(ISO_FMT);
        String updatedAt = updatedTs != null ? updatedTs.toLocalDateTime().format(ISO_FMT) : createdAt;

        UserSummary summary = new UserSummary(
                rs.getString("submitter_name"),
                rs.getString("submitter_email"),
                rs.getString("submitter_student_id")
        );

        return new Ticket(id, title, category, priority, status, location, description, summary, assignedTo, createdAt, updatedAt);
    }

    private void loadTicketTimelineAndComments(Connection conn, Ticket t) {
        // Load Timeline
        String timeSql = "SELECT status, label, event_date, note FROM ticket_timeline WHERE ticket_id = ? ORDER BY event_date ASC, timeline_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(timeSql)) {
            ps.setString(1, t.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("event_date");
                    String dateStr = ts != null ? ts.toLocalDateTime().format(DISPLAY_FMT) : "";
                    t.addTimelineItem(new TimelineItem(
                            rs.getString("status"),
                            rs.getString("label"),
                            dateStr,
                            rs.getString("note")
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load timeline for ticket: " + t.getId(), e);
        }

        // Load Comments
        String commentSql = "SELECT c.comment_id, u.name AS author_name, c.role, c.comment_text, c.comment_date " +
                "FROM ticket_comments c " +
                "JOIN users u ON c.author_id = u.user_id " +
                "WHERE c.ticket_id = ? " +
                "ORDER BY c.comment_date ASC, c.comment_id ASC";
        try (PreparedStatement ps = conn.prepareStatement(commentSql)) {
            ps.setString(1, t.getId());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Timestamp ts = rs.getTimestamp("comment_date");
                    String dateStr = ts != null ? ts.toLocalDateTime().format(DISPLAY_FMT) : "";
                    t.addComment(new Comment(
                            "c" + rs.getInt("comment_id"),
                            rs.getString("author_name"),
                            rs.getString("role"),
                            rs.getString("comment_text"),
                            dateStr
                    ));
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Failed to load comments for ticket: " + t.getId(), e);
        }
    }

    private String generateNextTicketId(Connection conn) throws SQLException {
        String sql = "SELECT ticket_id FROM tickets WHERE ticket_id LIKE 'TKT-%' ORDER BY ticket_id DESC";
        int maxSeq = 100;
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                String tid = rs.getString(1);
                if (tid != null) {
                    int lastDash = tid.lastIndexOf('-');
                    if (lastDash != -1 && lastDash < tid.length() - 1) {
                        try {
                            int seq = Integer.parseInt(tid.substring(lastDash + 1));
                            if (seq > maxSeq) {
                                maxSeq = seq;
                            }
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        }
        return "TKT-2026-" + (maxSeq + 1);
    }

    private int resolveCategoryId(Connection conn, String categoryName) throws SQLException {
        String name = categoryName != null && !categoryName.trim().isEmpty() ? categoryName.trim() : "IT & Network Support";
        String selectSql = "SELECT category_id FROM categories WHERE LOWER(name) = LOWER(?)";
        try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        // If category does not exist, insert it
        String insertSql = "INSERT INTO categories (name) VALUES (?)";
        try (PreparedStatement pi = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            pi.setString(1, name);
            pi.executeUpdate();
            try (ResultSet rsKey = pi.getGeneratedKeys()) {
                if (rsKey.next()) {
                    return rsKey.getInt(1);
                }
            }
        }
        return 1;
    }

    private int resolveUserId(Connection conn, User user) throws SQLException {
        if (user != null && user.getEmail() != null) {
            String selectSql = "SELECT user_id FROM users WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, user.getEmail().trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        // Fallback: pick first available student or user
        String fallbackSql = "SELECT user_id FROM users ORDER BY user_id ASC LIMIT 1";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(fallbackSql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 1;
    }

    private int resolveAdminUserId(Connection conn, User admin) throws SQLException {
        if (admin != null && admin.getEmail() != null) {
            String selectSql = "SELECT user_id FROM users WHERE LOWER(email) = LOWER(?)";
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setString(1, admin.getEmail().trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        // Fallback admin: pick admin user
        String fallbackSql = "SELECT user_id FROM users WHERE role = 'admin' LIMIT 1";
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery(fallbackSql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 2;
    }
}
