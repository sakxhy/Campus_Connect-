package com.campusconnect.service;

import com.campusconnect.model.*;
import com.campusconnect.util.DBConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Service Layer & Database Access Object for CampusConnect.
 * Supports dual-mode persistence:
 * 1. Persistent 3NF Relational MySQL database via JDBC PreparedStatement queries when connected.
 * 2. High-performance, thread-safe in-memory store fallback when MySQL is offline or initializing.
 */
public class DataStore {

    private static final Logger LOGGER = Logger.getLogger(DataStore.class.getName());
    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // In-Memory Fallback Collections
    private final Map<String, User> memoryUsersByEmail = new ConcurrentHashMap<>();
    private final Map<String, User> memoryUsersById = new ConcurrentHashMap<>();
    private final List<Ticket> memoryTickets = new CopyOnWriteArrayList<>();
    private final AtomicInteger memoryTicketSeq = new AtomicInteger(105);

    private DataStore() {
        initMemoryData();
        initDatabase();
    }

    private void initDatabase() {
        try {
            if (DBConnection.isConnected()) {
                LOGGER.info("Connected to MySQL database. Verifying database tables...");
                ensureSchemaInitialized();
            } else {
                LOGGER.warning("MySQL database not reachable on startup. Operating with resilient in-memory store.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error initializing database connection: " + e.getMessage(), e);
        }
    }

    private void ensureSchemaInitialized() {
        try (Connection conn = DBConnection.getConnection();
             ResultSet rs = conn.getMetaData().getTables(null, null, "tickets", null)) {
            if (!rs.next()) {
                LOGGER.info("Tables not found. Initializing schema from campus_connect_schema.sql...");
                DBConnection.executeSqlScript("campus_connect_schema.sql");
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Could not check existing tables", e);
        }
    }

    private synchronized void initMemoryData() {
        memoryUsersByEmail.clear();
        memoryUsersById.clear();
        memoryTickets.clear();
        memoryTicketSeq.set(105);

        // Preload Demo Users
        User student1 = new User(
                "1",
                "Alex Rivera",
                "alex.student@college.edu",
                "password123",
                "student",
                "CS-2023-042",
                "Computer Science & Engineering",
                "+1 (555) 234-5678",
                null
        );
        User admin1 = new User(
                "2",
                "Dr. Sarah Jenkins",
                "admin@college.edu",
                "admin123",
                "admin",
                null,
                "Campus Grievance & Operations Cell",
                "+1 (555) 987-6543",
                "Chief Grievance Officer"
        );
        User student2 = new User(
                "3",
                "Priya Sharma",
                "priya.s@college.edu",
                "password123",
                "student",
                "CS-2023-088",
                "Computer Science & Engineering",
                "+1 (555) 345-6789",
                null
        );

        addMemoryUser(student1);
        addMemoryUser(admin1);
        addMemoryUser(student2);

        // Preload Demo Tickets
        // TKT-2026-101
        Ticket t1 = new Ticket(
                "TKT-2026-101",
                "Hostel Block B Wi-Fi AP frequent dropouts during evening hours",
                "IT & Network Support",
                "High",
                "in_progress",
                "Hostel Block B, 3rd Floor Corridor",
                "The Wi-Fi access point in 3rd-floor hallway disconnects repeatedly after 7 PM, preventing students from accessing online lecture archives and coursework submissions.",
                new UserSummary("Alex Rivera", "alex.student@college.edu", "CS-2023-042"),
                "Network Operations Team",
                "2026-09-16T14:30:00Z",
                "2026-09-17T09:15:00Z"
        );
        t1.addTimelineItem(new TimelineItem("open", "Ticket Submitted", "2026-09-16 14:30", "Complaint logged via student portal."));
        t1.addTimelineItem(new TimelineItem("in_progress", "Assigned to Network Operations", "2026-09-17 09:15", "Hardware diagnostic scheduled for AP-B304 switch port."));
        t1.addComment(new Comment("c1", "Alex Rivera", "student", "This has been ongoing for 3 days. Several roommates are also affected.", "2026-09-16 14:35"));
        t1.addComment(new Comment("c2", "Network Operations Team", "admin", "A technician will inspect the signal interference and reboot the switch tonight between 11 PM and 11:30 PM.", "2026-09-17 09:15"));
        memoryTickets.add(t1);

        // TKT-2026-102
        Ticket t2 = new Ticket(
                "TKT-2026-102",
                "Lab 3 Air Conditioning unit leaking water over student workstations",
                "Hostel & Maintenance",
                "Urgent",
                "open",
                "Turing Hall, Lab 3, Row 4",
                "Condensation line is clogged and dripping onto Desktop stations 14 and 15. Risk of electrical short circuit if not serviced immediately.",
                new UserSummary("Alex Rivera", "alex.student@college.edu", "CS-2023-042"),
                "Electrical & HVAC Dept",
                "2026-09-18T08:10:00Z",
                "2026-09-18T08:10:00Z"
        );
        t2.addTimelineItem(new TimelineItem("open", "Ticket Submitted", "2026-09-18 08:10", "High priority maintenance request registered."));
        t2.addComment(new Comment("c3", "Alex Rivera", "student", "We covered the desktop screens with protective tarp for now.", "2026-09-18 08:12"));
        memoryTickets.add(t2);

        // TKT-2026-103
        Ticket t3 = new Ticket(
                "TKT-2026-103",
                "Mid-term hall ticket missing course code CS-402 Database Systems",
                "Academics & Examination",
                "Medium",
                "resolved",
                "Academic Registrar Portal",
                "My hall ticket generated on the student ERP portal displays only 4 registered courses instead of 5. CS-402 is omitted despite confirmed elective registration.",
                new UserSummary("Alex Rivera", "alex.student@college.edu", "CS-2023-042"),
                "Exam Cell Controller",
                "2026-09-14T10:00:00Z",
                "2026-09-15T16:20:00Z"
        );
        t3.addTimelineItem(new TimelineItem("open", "Submitted", "2026-09-14 10:00", "Hall ticket discrepancy filed."));
        t3.addTimelineItem(new TimelineItem("in_progress", "Under Review", "2026-09-14 15:45", "Cross-verifying ERP elective records."));
        t3.addTimelineItem(new TimelineItem("resolved", "Resolved & Updated", "2026-09-15 16:20", "Elective sync completed. Student hall ticket regenerated."));
        t3.addComment(new Comment("c4", "Exam Cell Controller", "admin", "The elective database sync lag was corrected. Please log in to your ERP portal and re-download the amended hall ticket.", "2026-09-15 16:20"));
        memoryTickets.add(t3);

        // TKT-2026-104
        Ticket t4 = new Ticket(
                "TKT-2026-104",
                "Digital Library IEEE Xplore off-campus VPN access credentials expiring",
                "Library & Digital Resources",
                "Low",
                "open",
                "Central Digital Library",
                "Attempting to access IEEE Xplore through institutional proxy returns 403 authorization error for batch of final year CSE students.",
                new UserSummary("Priya Sharma", "priya.s@college.edu", "CS-2023-088"),
                "Library IT Helpdesk",
                "2026-09-17T11:20:00Z",
                "2026-09-17T11:20:00Z"
        );
        t4.addTimelineItem(new TimelineItem("open", "Ticket Submitted", "2026-09-17 11:20", "Proxy authentication ticket logged."));
        memoryTickets.add(t4);

        // TKT-2026-105
        Ticket t5 = new Ticket(
                "TKT-2026-105",
                "Cafeteria Water Cooler filter replacement and hygiene audit",
                "Cafeteria & Mess",
                "High",
                "resolved",
                "North Block Canteen Area",
                "The TDS indicator on the central water purification station in North Canteen showed service required since Monday.",
                new UserSummary("Rohan Gupta", "rohan.g@college.edu", "ME-2023-019"),
                "Campus Sanitation Unit",
                "2026-09-12T09:00:00Z",
                "2026-09-13T14:10:00Z"
        );
        t5.addTimelineItem(new TimelineItem("open", "Ticket Submitted", "2026-09-12 09:00", "Canteen filter service requested."));
        t5.addTimelineItem(new TimelineItem("in_progress", "Vendor Dispatched", "2026-09-12 13:00", "RO service engineer called."));
        t5.addTimelineItem(new TimelineItem("resolved", "Filters Replaced", "2026-09-13 14:10", "Sediment and carbon candles renewed, TDS calibrated to 110 ppm."));
        t5.addComment(new Comment("c5", "Campus Sanitation Unit", "admin", "All filters replaced and water quality certified by the campus health committee.", "2026-09-13 14:10"));
        memoryTickets.add(t5);
    }

    private void addMemoryUser(User u) {
        if (u != null && u.getEmail() != null) {
            memoryUsersByEmail.put(u.getEmail().trim().toLowerCase(), u);
            if (u.getId() != null) {
                memoryUsersById.put(u.getId(), u);
            }
        }
    }

    /**
     * Resets database records and in-memory store.
     */
    public synchronized void resetToInitialData() {
        initMemoryData();
        if (DBConnection.isConnected()) {
            LOGGER.info("Resetting database to initial seed data from schema...");
            boolean success = DBConnection.executeSqlScript("campus_connect_schema.sql");
            if (!success) {
                LOGGER.warning("Could not reset database using root schema file. Attempting fallback...");
                DBConnection.executeSqlScript("src/main/resources/campus_connect_schema.sql");
            }
        }
    }

    /**
     * Inserts a new user into MySQL (if connected) and in-memory store.
     */
    public void addUser(User u) {
        if (u == null || u.getEmail() == null) return;
        addMemoryUser(u);

        if (!DBConnection.isConnected()) {
            return;
        }

        String insertUserSql = "INSERT INTO users (name, email, password_hash, role, phone) VALUES (?, ?, ?, ?, ?)";
        String insertStudentSql = "INSERT INTO students (user_id, student_id, department) VALUES (?, ?, ?)";
        String insertAdminSql = "INSERT INTO admins (user_id, department, title) VALUES (?, ?, ?)";

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);
            try {
                int userId = 0;
                try (PreparedStatement pUser = conn.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                    pUser.setString(1, u.getName() != null ? u.getName().trim() : "Unknown");
                    pUser.setString(2, u.getEmail().trim().toLowerCase());
                    pUser.setString(3, u.getPassword() != null ? u.getPassword() : "");
                    pUser.setString(4, u.getRole() != null ? u.getRole().toLowerCase() : "student");
                    pUser.setString(5, u.getPhone() != null ? u.getPhone().trim() : null);
                    pUser.executeUpdate();

                    try (ResultSet rsKeys = pUser.getGeneratedKeys()) {
                        if (rsKeys.next()) {
                            userId = rsKeys.getInt(1);
                            u.setId(String.valueOf(userId));
                            memoryUsersById.put(u.getId(), u);
                        }
                    }
                }

                if ("student".equalsIgnoreCase(u.getRole())) {
                    try (PreparedStatement pStudent = conn.prepareStatement(insertStudentSql)) {
                        pStudent.setInt(1, userId);
                        pStudent.setString(2, u.getStudentId() != null ? u.getStudentId().trim() : "ID-" + userId);
                        pStudent.setString(3, u.getDepartment() != null ? u.getDepartment().trim() : "General");
                        pStudent.executeUpdate();
                    }
                } else if ("admin".equalsIgnoreCase(u.getRole())) {
                    try (PreparedStatement pAdmin = conn.prepareStatement(insertAdminSql)) {
                        pAdmin.setInt(1, userId);
                        pAdmin.setString(2, u.getDepartment() != null ? u.getDepartment().trim() : "Campus Grievance & Operations Cell");
                        pAdmin.setString(3, u.getTitle() != null ? u.getTitle().trim() : "Staff");
                        pAdmin.executeUpdate();
                    }
                }

                conn.commit();
                LOGGER.info("User registered successfully in database with ID: " + userId);
            } catch (SQLException ex) {
                conn.rollback();
                LOGGER.log(Level.WARNING, "Failed to insert user into DB, transaction rolled back", ex);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Database connection error in addUser: " + u.getEmail(), e);
        }
    }

    /**
     * Finds a user by email address.
     */
    public User getUserByEmail(String email) {
        if (email == null || email.trim().isEmpty()) return null;

        if (DBConnection.isConnected()) {
            String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                    "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                    "FROM users u " +
                    "LEFT JOIN students s ON u.user_id = s.user_id " +
                    "LEFT JOIN admins a ON u.user_id = a.user_id " +
                    "WHERE LOWER(u.email) = LOWER(?)";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, email.trim().toLowerCase());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUserFromResultSet(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error fetching user from DB by email: " + email, e);
            }
        }

        return memoryUsersByEmail.get(email.trim().toLowerCase());
    }

    /**
     * Finds a user by ID.
     */
    public User getUserById(String id) {
        if (id == null || id.trim().isEmpty()) return null;

        if ("usr_student_1".equalsIgnoreCase(id.trim())) return getUserByEmail("alex.student@college.edu");
        if ("usr_admin_1".equalsIgnoreCase(id.trim())) return getUserByEmail("admin@college.edu");
        if ("usr_student_2".equalsIgnoreCase(id.trim())) return getUserByEmail("priya.s@college.edu");

        if (DBConnection.isConnected()) {
            int numericId;
            try {
                numericId = Integer.parseInt(id.trim());
                String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                        "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                        "FROM users u " +
                        "LEFT JOIN students s ON u.user_id = s.user_id " +
                        "LEFT JOIN admins a ON u.user_id = a.user_id " +
                        "WHERE u.user_id = ?";

                try (Connection conn = DBConnection.getConnection();
                     PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, numericId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            return mapUserFromResultSet(rs);
                        }
                    }
                } catch (SQLException e) {
                    LOGGER.log(Level.WARNING, "Error fetching user by id from DB: " + id, e);
                }
            } catch (NumberFormatException ignored) {}
        }

        return memoryUsersById.get(id.trim());
    }

    /**
     * Retrieves the default demo user for a given role (student or admin).
     */
    public User getDemoUser(String role) {
        String roleStr = role != null && !role.trim().isEmpty() ? role.trim().toLowerCase() : "student";

        if (DBConnection.isConnected()) {
            String sql = "SELECT u.user_id, u.name, u.email, u.password_hash, u.role, u.phone, " +
                    "s.student_id, COALESCE(s.department, a.department) AS department, a.title " +
                    "FROM users u " +
                    "LEFT JOIN students s ON u.user_id = s.user_id " +
                    "LEFT JOIN admins a ON u.user_id = a.user_id " +
                    "WHERE LOWER(u.role) = LOWER(?) ORDER BY u.user_id ASC LIMIT 1";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, roleStr);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return mapUserFromResultSet(rs);
                    }
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error fetching demo user from DB for role: " + role, e);
            }
        }

        for (User u : memoryUsersByEmail.values()) {
            if (roleStr.equalsIgnoreCase(u.getRole())) {
                return u;
            }
        }
        return null;
    }

    /**
     * Queries grievance tickets with dynamic filtering and search parameters.
     */
    public List<Ticket> getTickets(String studentEmail, String status, String category, String priority, String search) {
        if (DBConnection.isConnected()) {
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

            try (Connection conn = DBConnection.getConnection();
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
                    return list;
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error querying tickets from DB, falling back to memory", e);
            }
        }

        // In-memory fallback
        List<Ticket> result = new ArrayList<>();
        String q = search != null ? search.trim().toLowerCase() : null;

        for (Ticket t : memoryTickets) {
            if (studentEmail != null && !studentEmail.trim().isEmpty()) {
                if (t.getSubmittedBy() == null || !studentEmail.equalsIgnoreCase(t.getSubmittedBy().getEmail())) {
                    continue;
                }
            }
            if (status != null && !status.trim().isEmpty() && !"all".equalsIgnoreCase(status.trim())) {
                if (!status.equalsIgnoreCase(t.getStatus())) {
                    continue;
                }
            }
            if (category != null && !category.trim().isEmpty() && !"all".equalsIgnoreCase(category.trim())) {
                if (!category.equalsIgnoreCase(t.getCategory())) {
                    continue;
                }
            }
            if (priority != null && !priority.trim().isEmpty() && !"all".equalsIgnoreCase(priority.trim())) {
                if (!priority.equalsIgnoreCase(t.getPriority())) {
                    continue;
                }
            }
            if (q != null && !q.isEmpty()) {
                boolean match = (t.getId() != null && t.getId().toLowerCase().contains(q))
                        || (t.getTitle() != null && t.getTitle().toLowerCase().contains(q))
                        || (t.getCategory() != null && t.getCategory().toLowerCase().contains(q))
                        || (t.getDescription() != null && t.getDescription().toLowerCase().contains(q))
                        || (t.getLocation() != null && t.getLocation().toLowerCase().contains(q));
                if (!match) continue;
            }

            result.add(t);
        }

        result.sort((a, b) -> {
            String ca = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String cb = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return cb.compareTo(ca);
        });

        return result;
    }

    /**
     * Retrieves a single ticket with full timeline and comment conversation history.
     */
    public Ticket getTicketById(String id) {
        if (id == null || id.trim().isEmpty()) return null;

        if (DBConnection.isConnected()) {
            String sql = "SELECT t.ticket_id, t.title, t.description, c.name AS category_name, t.priority, t.status, " +
                    "t.location, t.assigned_to, t.created_at, t.updated_at, " +
                    "u.name AS submitter_name, u.email AS submitter_email, s.student_id AS submitter_student_id " +
                    "FROM tickets t " +
                    "JOIN categories c ON t.category_id = c.category_id " +
                    "JOIN users u ON t.submitted_by = u.user_id " +
                    "LEFT JOIN students s ON u.user_id = s.user_id " +
                    "WHERE LOWER(t.ticket_id) = LOWER(?)";

            try (Connection conn = DBConnection.getConnection();
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
                LOGGER.log(Level.WARNING, "Error getting ticket by id from DB: " + id, e);
            }
        }

        String searchId = id.trim().toUpperCase();
        for (Ticket t : memoryTickets) {
            if (searchId.equalsIgnoreCase(t.getId())) {
                return t;
            }
        }
        return null;
    }

    /**
     * Creates a new ticket, writing to both DB and memory store.
     */
    public synchronized Ticket createTicket(String title, String category, String priority, String location, String description, User submittedBy) {
        String nowIso = LocalDateTime.now().format(ISO_FMT);
        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);

        UserSummary summary = new UserSummary(
                submittedBy != null ? submittedBy.getName() : "Alex Rivera",
                submittedBy != null ? submittedBy.getEmail() : "alex.student@college.edu",
                submittedBy != null && submittedBy.getStudentId() != null ? submittedBy.getStudentId() : "CS-2023-042"
        );

        if (DBConnection.isConnected()) {
            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    String ticketId = generateNextTicketId(conn);
                    int catId = resolveCategoryId(conn, category);
                    int userId = resolveUserId(conn, submittedBy);

                    String insertTicketSql = "INSERT INTO tickets (ticket_id, title, category_id, priority, status, location, description, submitted_by, assigned_to, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, 'open', ?, ?, ?, 'Pending Assignment', NOW(), NOW())";

                    try (PreparedStatement ps = conn.prepareStatement(insertTicketSql)) {
                        ps.setString(1, ticketId);
                        ps.setString(2, title != null ? title.trim() : "Untitled Ticket");
                        ps.setInt(3, catId);
                        ps.setString(4, priority != null ? priority.trim() : "Medium");
                        ps.setString(5, location != null ? location.trim() : "Campus Premises");
                        ps.setString(6, description != null ? description.trim() : "");
                        ps.setInt(7, userId);
                        ps.executeUpdate();
                    }

                    String insertTimelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, 'open', 'Ticket Submitted', NOW(), 'New grievance logged via student portal.')";
                    try (PreparedStatement pt = conn.prepareStatement(insertTimelineSql)) {
                        pt.setString(1, ticketId);
                        pt.executeUpdate();
                    }

                    conn.commit();

                    Ticket created = getTicketById(ticketId);
                    if (created != null) {
                        memoryTickets.add(0, created);
                        return created;
                    }
                } catch (SQLException ex) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Error creating ticket in DB, falling back to memory store", ex);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Connection error creating ticket", e);
            }
        }

        // In-memory creation
        int seq = memoryTicketSeq.incrementAndGet();
        String ticketId = "TKT-2026-" + seq;
        Ticket ticket = new Ticket(
                ticketId,
                title != null ? title.trim() : "Untitled",
                category != null ? category : "General",
                priority != null ? priority : "Medium",
                "open",
                location != null && !location.trim().isEmpty() ? location.trim() : "Campus Premises",
                description != null ? description.trim() : "",
                summary,
                "Pending Assignment",
                nowIso,
                nowIso
        );
        ticket.addTimelineItem(new TimelineItem("open", "Ticket Submitted", displayNow, "New grievance logged by student."));
        memoryTickets.add(0, ticket);
        return ticket;
    }

    /**
     * Updates ticket status with timeline audit record and comment.
     */
    public synchronized Ticket updateTicketStatus(String ticketId, String newStatus, String resolutionRemark, User adminUser) {
        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);

        if (DBConnection.isConnected()) {
            String updateTicketSql = "UPDATE tickets SET status = ?, updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";
            String insertTimelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, ?, ?, NOW(), ?)";
            String insertCommentSql = "INSERT INTO ticket_comments (ticket_id, author_id, role, comment_text, comment_date) VALUES (?, ?, 'admin', ?, NOW())";

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(updateTicketSql)) {
                        ps.setString(1, newStatus);
                        ps.setString(2, ticketId.trim());
                        ps.executeUpdate();
                    }

                    String label = "Status Updated";
                    if ("open".equalsIgnoreCase(newStatus)) label = "Status reset to Open";
                    else if ("in_progress".equalsIgnoreCase(newStatus)) label = "Marked In-Progress";
                    else if ("resolved".equalsIgnoreCase(newStatus)) label = "Ticket Resolved";
                    else if ("closed".equalsIgnoreCase(newStatus)) label = "Ticket Closed";

                    String note = (resolutionRemark != null && !resolutionRemark.trim().isEmpty())
                            ? resolutionRemark.trim() : "Status updated to " + newStatus;

                    try (PreparedStatement pt = conn.prepareStatement(insertTimelineSql)) {
                        pt.setString(1, ticketId.trim());
                        pt.setString(2, newStatus);
                        pt.setString(3, label);
                        pt.setString(4, note);
                        pt.executeUpdate();
                    }

                    if (resolutionRemark != null && !resolutionRemark.trim().isEmpty()) {
                        int adminId = resolveAdminUserId(conn, adminUser);
                        try (PreparedStatement pc = conn.prepareStatement(insertCommentSql)) {
                            pc.setString(1, ticketId.trim());
                            pc.setInt(2, adminId);
                            pc.setString(3, "[Status Update: " + newStatus.toUpperCase() + "] " + resolutionRemark.trim());
                            pc.executeUpdate();
                        }
                    }

                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Error updating ticket status in DB", ex);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Connection error in updateTicketStatus", e);
            }
        }

        // In-memory update
        for (Ticket t : memoryTickets) {
            if (t.getId().equalsIgnoreCase(ticketId)) {
                t.setStatus(newStatus);
                t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT));
                String label = "Status Updated";
                if ("open".equalsIgnoreCase(newStatus)) label = "Status reset to Open";
                else if ("in_progress".equalsIgnoreCase(newStatus)) label = "Marked In-Progress";
                else if ("resolved".equalsIgnoreCase(newStatus)) label = "Ticket Resolved";
                else if ("closed".equalsIgnoreCase(newStatus)) label = "Ticket Closed";

                t.addTimelineItem(new TimelineItem(newStatus, label, displayNow, resolutionRemark != null ? resolutionRemark.trim() : "Status updated to " + newStatus));
                if (resolutionRemark != null && !resolutionRemark.trim().isEmpty()) {
                    String author = adminUser != null ? adminUser.getName() : "Administrator";
                    t.addComment(new Comment("c_" + System.currentTimeMillis(), author, "admin", "[Status Update: " + newStatus.toUpperCase() + "] " + resolutionRemark.trim(), displayNow));
                }
                return t;
            }
        }

        return getTicketById(ticketId);
    }

    /**
     * Assigns a ticket to a department or staff member.
     */
    public synchronized Ticket assignTicket(String ticketId, String assignedTo) {
        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);

        if (DBConnection.isConnected()) {
            String updateSql = "UPDATE tickets SET assigned_to = ?, status = CASE WHEN status = 'open' THEN 'in_progress' ELSE status END, updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";
            String timelineSql = "INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES (?, 'in_progress', 'Ticket Assigned', NOW(), ?)";

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                        ps.setString(1, assignedTo != null ? assignedTo.trim() : "Pending Assignment");
                        ps.setString(2, ticketId.trim());
                        ps.executeUpdate();
                    }

                    try (PreparedStatement pt = conn.prepareStatement(timelineSql)) {
                        pt.setString(1, ticketId.trim());
                        pt.setString(2, "Assigned to: " + (assignedTo != null ? assignedTo.trim() : "Staff"));
                        pt.executeUpdate();
                    }

                    conn.commit();
                } catch (SQLException ex) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Error assigning ticket in DB", ex);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Connection error in assignTicket", e);
            }
        }

        // In-memory update
        for (Ticket t : memoryTickets) {
            if (t.getId().equalsIgnoreCase(ticketId)) {
                t.setAssignedTo(assignedTo);
                if ("open".equalsIgnoreCase(t.getStatus())) {
                    t.setStatus("in_progress");
                }
                t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT));
                t.addTimelineItem(new TimelineItem(t.getStatus(), "Ticket Assigned", displayNow, "Assigned to: " + assignedTo));
                return t;
            }
        }

        return getTicketById(ticketId);
    }

    /**
     * Appends a comment to a ticket thread.
     */
    public synchronized Comment addComment(String ticketId, String text, User user) {
        if (text == null || text.trim().isEmpty()) return null;
        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);
        String author = user != null ? user.getName() : "Anonymous";
        String role = user != null ? user.getRole() : "student";

        if (DBConnection.isConnected()) {
            String insertSql = "INSERT INTO ticket_comments (ticket_id, author_id, role, comment_text, comment_date) VALUES (?, ?, ?, ?, NOW())";
            String updateTicketSql = "UPDATE tickets SET updated_at = NOW() WHERE LOWER(ticket_id) = LOWER(?)";

            try (Connection conn = DBConnection.getConnection()) {
                conn.setAutoCommit(false);
                try {
                    int userId = resolveUserId(conn, user);
                    int commentId = 0;
                    try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                        ps.setString(1, ticketId.trim());
                        ps.setInt(2, userId);
                        ps.setString(3, role != null ? role.toLowerCase() : "student");
                        ps.setString(4, text.trim());
                        ps.executeUpdate();
                        try (ResultSet rs = ps.getGeneratedKeys()) {
                            if (rs.next()) {
                                commentId = rs.getInt(1);
                            }
                        }
                    }

                    try (PreparedStatement pu = conn.prepareStatement(updateTicketSql)) {
                        pu.setString(1, ticketId.trim());
                        pu.executeUpdate();
                    }

                    conn.commit();

                    Comment c = new Comment("c" + commentId, author, role, text.trim(), displayNow);
                    Ticket t = getTicketById(ticketId);
                    if (t != null) {
                        t.addComment(c);
                    }
                    return c;
                } catch (SQLException ex) {
                    conn.rollback();
                    LOGGER.log(Level.WARNING, "Error adding comment in DB", ex);
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Connection error in addComment", e);
            }
        }

        // In-memory comment
        Comment comment = new Comment("c_" + System.currentTimeMillis(), author, role, text.trim(), displayNow);
        for (Ticket t : memoryTickets) {
            if (t.getId().equalsIgnoreCase(ticketId)) {
                t.addComment(comment);
                t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT));
                break;
            }
        }
        return comment;
    }

    /**
     * Calculates live KPI statistics.
     */
    public Stats getStats() {
        if (DBConnection.isConnected()) {
            String countSql = "SELECT " +
                    "COUNT(*) AS total, " +
                    "SUM(CASE WHEN LOWER(status) = 'open' THEN 1 ELSE 0 END) AS open_cnt, " +
                    "SUM(CASE WHEN LOWER(status) = 'in_progress' THEN 1 ELSE 0 END) AS in_prog_cnt, " +
                    "SUM(CASE WHEN LOWER(status) = 'resolved' THEN 1 ELSE 0 END) AS res_cnt, " +
                    "SUM(CASE WHEN LOWER(status) = 'closed' THEN 1 ELSE 0 END) AS closed_cnt, " +
                    "SUM(CASE WHEN LOWER(priority) = 'urgent' AND LOWER(status) NOT IN ('resolved', 'closed') THEN 1 ELSE 0 END) AS urgent_cnt " +
                    "FROM tickets";

            try (Connection conn = DBConnection.getConnection();
                 Statement s = conn.createStatement();
                 ResultSet rs = s.executeQuery(countSql)) {
                if (rs.next()) {
                    return new Stats(
                            rs.getInt("total"),
                            rs.getInt("open_cnt"),
                            rs.getInt("in_prog_cnt"),
                            rs.getInt("res_cnt"),
                            rs.getInt("closed_cnt"),
                            rs.getInt("urgent_cnt")
                    );
                }
            } catch (SQLException e) {
                LOGGER.log(Level.WARNING, "Error calculating stats from DB, falling back to memory", e);
            }
        }

        // In-memory stats
        int total = memoryTickets.size();
        int open = 0;
        int inProg = 0;
        int resolved = 0;
        int closed = 0;
        int urgent = 0;

        for (Ticket t : memoryTickets) {
            String s = t.getStatus() != null ? t.getStatus().toLowerCase() : "";
            if ("open".equals(s)) open++;
            else if ("in_progress".equals(s)) inProg++;
            else if ("resolved".equals(s)) resolved++;
            else if ("closed".equals(s)) closed++;

            if ("urgent".equalsIgnoreCase(t.getPriority()) && !"resolved".equals(s) && !"closed".equals(s)) {
                urgent++;
            }
        }

        return new Stats(total, open, inProg, resolved, closed, urgent);
    }

    // -------------------------------------------------------------------------
    // Internal Helper & Mapping Methods
    // -------------------------------------------------------------------------

    private User mapUserFromResultSet(ResultSet rs) throws SQLException {
        String password = rs.getString("password_hash");
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
