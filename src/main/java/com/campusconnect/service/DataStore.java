package com.campusconnect.service;

import com.campusconnect.model.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread-safe In-Memory Data Store.
 * Preloaded with realistic college tickets and sample accounts.
 * Structured cleanly to easily transition to JDBC/MySQL DAO in the future.
 */
public class DataStore {

    private static final DataStore INSTANCE = new DataStore();

    public static DataStore getInstance() {
        return INSTANCE;
    }

    private final Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private final Map<String, User> usersById = new ConcurrentHashMap<>();
    private final List<Ticket> tickets = new CopyOnWriteArrayList<>();
    private final AtomicInteger ticketSeq = new AtomicInteger(105);

    private static final DateTimeFormatter ISO_FMT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private DataStore() {
        resetToInitialData();
    }

    public synchronized void resetToInitialData() {
        usersByEmail.clear();
        usersById.clear();
        tickets.clear();
        ticketSeq.set(105);

        // Preload Users
        User student1 = new User(
                "usr_student_1",
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
                "usr_admin_1",
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
                "usr_student_2",
                "Priya Sharma",
                "priya.s@college.edu",
                "password123",
                "student",
                "CS-2023-088",
                "Computer Science & Engineering",
                "+1 (555) 345-6789",
                null
        );

        addUser(student1);
        addUser(admin1);
        addUser(student2);

        // Preload Tickets
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
        tickets.add(t1);

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
        tickets.add(t2);

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
        tickets.add(t3);

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
        tickets.add(t4);

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
        tickets.add(t5);
    }

    public void addUser(User u) {
        if (u != null) {
            usersByEmail.put(u.getEmail().toLowerCase(), u);
            usersById.put(u.getId(), u);
        }
    }

    public User getUserByEmail(String email) {
        if (email == null) return null;
        return usersByEmail.get(email.trim().toLowerCase());
    }

    public User getUserById(String id) {
        if (id == null) return null;
        return usersById.get(id);
    }

    public User getDemoUser(String role) {
        for (User u : usersByEmail.values()) {
            if (role.equalsIgnoreCase(u.getRole())) {
                return u;
            }
        }
        return null;
    }

    public List<Ticket> getTickets(String studentEmail, String status, String category, String priority, String search) {
        List<Ticket> result = new ArrayList<>();
        String q = search != null ? search.trim().toLowerCase() : null;

        for (Ticket t : tickets) {
            if (studentEmail != null && !studentEmail.isEmpty()) {
                if (t.getSubmittedBy() == null || !studentEmail.equalsIgnoreCase(t.getSubmittedBy().getEmail())) {
                    continue;
                }
            }
            if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
                if (!status.equalsIgnoreCase(t.getStatus())) {
                    continue;
                }
            }
            if (category != null && !category.isEmpty() && !"all".equalsIgnoreCase(category)) {
                if (!category.equalsIgnoreCase(t.getCategory())) {
                    continue;
                }
            }
            if (priority != null && !priority.isEmpty() && !"all".equalsIgnoreCase(priority)) {
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

        // Sort descending by createdAt
        result.sort((a, b) -> {
            String ca = a.getCreatedAt() != null ? a.getCreatedAt() : "";
            String cb = b.getCreatedAt() != null ? b.getCreatedAt() : "";
            return cb.compareTo(ca);
        });

        return result;
    }

    public Ticket getTicketById(String id) {
        if (id == null) return null;
        String searchId = id.trim().toUpperCase();
        for (Ticket t : tickets) {
            if (searchId.equalsIgnoreCase(t.getId())) {
                return t;
            }
        }
        return null;
    }

    public synchronized Ticket createTicket(String title, String category, String priority, String location, String description, User user) {
        int seq = ticketSeq.incrementAndGet();
        String ticketId = "TKT-2026-" + seq;
        String nowIso = LocalDateTime.now().format(ISO_FMT) + "Z";
        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);

        UserSummary summary = new UserSummary(
                user != null ? user.getName() : "Anonymous",
                user != null ? user.getEmail() : "anonymous@college.edu",
                user != null && user.getStudentId() != null ? user.getStudentId() : "STU-GEN"
        );

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
        tickets.add(0, ticket);
        return ticket;
    }

    public synchronized Ticket updateTicketStatus(String id, String newStatus, String adminNote, User admin) {
        Ticket t = getTicketById(id);
        if (t == null) return null;

        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);
        t.setStatus(newStatus);
        t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT) + "Z");

        String label = "Status Updated";
        if ("open".equalsIgnoreCase(newStatus)) label = "Status reset to Open";
        else if ("in_progress".equalsIgnoreCase(newStatus)) label = "Marked In-Progress";
        else if ("resolved".equalsIgnoreCase(newStatus)) label = "Ticket Resolved";
        else if ("closed".equalsIgnoreCase(newStatus)) label = "Ticket Closed";

        t.addTimelineItem(new TimelineItem(
                newStatus,
                label,
                displayNow,
                adminNote != null && !adminNote.trim().isEmpty() ? adminNote.trim() : "Status updated to " + newStatus
        ));

        if (adminNote != null && !adminNote.trim().isEmpty()) {
            String author = admin != null ? admin.getName() : "Administrator";
            t.addComment(new Comment(
                    "c_" + System.currentTimeMillis(),
                    author,
                    "admin",
                    "[Status Update: " + newStatus.toUpperCase() + "] " + adminNote.trim(),
                    displayNow
            ));
        }

        return t;
    }

    public synchronized Ticket assignTicket(String id, String assignee) {
        Ticket t = getTicketById(id);
        if (t == null) return null;

        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);
        t.setAssignedTo(assignee);
        if ("open".equalsIgnoreCase(t.getStatus())) {
            t.setStatus("in_progress");
        }
        t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT) + "Z");

        t.addTimelineItem(new TimelineItem(
                t.getStatus(),
                "Ticket Assigned",
                displayNow,
                "Assigned to: " + assignee
        ));

        return t;
    }

    public synchronized Comment addComment(String ticketId, String text, User user) {
        Ticket t = getTicketById(ticketId);
        if (t == null || text == null || text.trim().isEmpty()) return null;

        String displayNow = LocalDateTime.now().format(DISPLAY_FMT);
        String author = user != null ? user.getName() : "Anonymous";
        String role = user != null ? user.getRole() : "student";

        Comment comment = new Comment(
                "c_" + System.currentTimeMillis(),
                author,
                role,
                text.trim(),
                displayNow
        );

        t.addComment(comment);
        t.setUpdatedAt(LocalDateTime.now().format(ISO_FMT) + "Z");
        return comment;
    }

    public Stats getStats() {
        int total = tickets.size();
        int open = 0;
        int inProg = 0;
        int resolved = 0;
        int closed = 0;
        int urgent = 0;

        for (Ticket t : tickets) {
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
}
