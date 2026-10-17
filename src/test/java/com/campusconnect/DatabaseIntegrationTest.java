package com.campusconnect;

import com.campusconnect.model.*;
import com.campusconnect.service.DataStore;
import com.campusconnect.util.DBUtil;
import org.junit.jupiter.api.*;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DatabaseIntegrationTest {

    private static DataStore dataStore;

    @BeforeAll
    public static void setUp() {
        dataStore = DataStore.getInstance();
        assertTrue(DBUtil.isConnected(), "Database should be connected");
    }

    @Test
    @Order(1)
    public void testDatabaseConnection() throws Exception {
        try (Connection conn = DBUtil.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(2));
        }
    }

    @Test
    @Order(2)
    public void testDemoUsersLookup() {
        User student = dataStore.getUserByEmail("alex.student@college.edu");
        assertNotNull(student, "Demo student Alex Rivera should exist");
        assertEquals("student", student.getRole());
        assertEquals("CS-2023-042", student.getStudentId());
        assertEquals("password123", student.getPassword());

        User admin = dataStore.getUserByEmail("admin@college.edu");
        assertNotNull(admin, "Demo admin Dr. Sarah Jenkins should exist");
        assertEquals("admin", admin.getRole());
        assertEquals("admin123", admin.getPassword());

        User demoStudent = dataStore.getDemoUser("student");
        assertNotNull(demoStudent);
        assertEquals("student", demoStudent.getRole());
    }

    @Test
    @Order(3)
    public void testUserRegistration() {
        String testEmail = "test.student." + System.currentTimeMillis() + "@college.edu";
        User newUser = new User(
                null,
                "Test Student",
                testEmail,
                "testpass123",
                "student",
                "STU-" + (System.currentTimeMillis() % 10000),
                "Electrical Engineering",
                "+1 555-0199",
                null
        );

        dataStore.addUser(newUser);
        assertNotNull(newUser.getId(), "User ID should be populated after insert");

        User retrieved = dataStore.getUserByEmail(testEmail);
        assertNotNull(retrieved, "User should be retrievable from database by email");
        assertEquals("Test Student", retrieved.getName());
        assertEquals("testpass123", retrieved.getPassword());
        assertEquals("student", retrieved.getRole());
        assertEquals("Electrical Engineering", retrieved.getDepartment());
    }

    @Test
    @Order(4)
    public void testCreateAndReadTicket() {
        User student = dataStore.getUserByEmail("alex.student@college.edu");
        assertNotNull(student);

        String title = "Automated Test Ticket - " + System.currentTimeMillis();
        Ticket created = dataStore.createTicket(
                title,
                "IT & Network Support",
                "High",
                "Building 4, Room 102",
                "Network cable damaged during lab setup.",
                student
        );

        assertNotNull(created, "Created ticket should not be null");
        assertTrue(created.getId().startsWith("TKT-2026-"), "Ticket ID should follow TKT-2026- format");
        assertEquals(title, created.getTitle());
        assertEquals("open", created.getStatus());
        assertEquals("High", created.getPriority());
        assertEquals("IT & Network Support", created.getCategory());
        assertNotNull(created.getSubmittedBy());
        assertEquals("Alex Rivera", created.getSubmittedBy().getName());

        // Verify initial timeline audit
        assertNotNull(created.getTimeline());
        assertFalse(created.getTimeline().isEmpty());
        assertEquals("open", created.getTimeline().get(0).getStatus());

        // Read by ID
        Ticket fetched = dataStore.getTicketById(created.getId());
        assertNotNull(fetched);
        assertEquals(created.getId(), fetched.getId());
    }

    @Test
    @Order(5)
    public void testTicketFiltersAndSearch() {
        List<Ticket> allTickets = dataStore.getTickets(null, null, null, null, null);
        assertNotNull(allTickets);
        assertTrue(allTickets.size() >= 5, "Initial seed data has at least 5 tickets");

        // Filter by status
        List<Ticket> openTickets = dataStore.getTickets(null, "open", null, null, null);
        for (Ticket t : openTickets) {
            assertEquals("open", t.getStatus());
        }

        // Filter by category
        List<Ticket> itTickets = dataStore.getTickets(null, null, "IT & Network Support", null, null);
        for (Ticket t : itTickets) {
            assertEquals("IT & Network Support", t.getCategory());
        }

        // Search keyword
        List<Ticket> searchWi = dataStore.getTickets(null, null, null, null, "Wi-Fi");
        assertFalse(searchWi.isEmpty());
        assertTrue(searchWi.get(0).getTitle().toLowerCase().contains("wi-fi") ||
                   searchWi.get(0).getDescription().toLowerCase().contains("wi-fi"));
    }

    @Test
    @Order(6)
    public void testTicketStatusUpdate() {
        User admin = dataStore.getUserByEmail("admin@college.edu");
        assertNotNull(admin);

        // Update TKT-2026-102
        Ticket updated = dataStore.updateTicketStatus("TKT-2026-102", "in_progress", "Technician dispatched to inspection site.", admin);
        assertNotNull(updated);
        assertEquals("in_progress", updated.getStatus());

        // Verify timeline added
        List<TimelineItem> timeline = updated.getTimeline();
        boolean foundTimeline = timeline.stream().anyMatch(i -> "in_progress".equals(i.getStatus()));
        assertTrue(foundTimeline, "Timeline should include in_progress event");

        // Verify comment added
        List<Comment> comments = updated.getComments();
        boolean foundComment = comments.stream().anyMatch(c -> c.getText().contains("Technician dispatched"));
        assertTrue(foundComment, "Comments should include admin status remark");
    }

    @Test
    @Order(7)
    public void testAssignTicket() {
        Ticket assigned = dataStore.assignTicket("TKT-2026-104", "Digital Systems Support Unit");
        assertNotNull(assigned);
        assertEquals("Digital Systems Support Unit", assigned.getAssignedTo());
        assertEquals("in_progress", assigned.getStatus(), "Assigning an open ticket should advance it to in_progress");
    }

    @Test
    @Order(8)
    public void testAddComment() {
        User student = dataStore.getUserByEmail("alex.student@college.edu");
        Comment comment = dataStore.addComment("TKT-2026-101", "Checking in on ETA for the resolution.", student);
        assertNotNull(comment);
        assertEquals("Alex Rivera", comment.getAuthor());
        assertEquals("student", comment.getRole());
        assertEquals("Checking in on ETA for the resolution.", comment.getText());

        Ticket ticket = dataStore.getTicketById("TKT-2026-101");
        assertNotNull(ticket);
        boolean commentFound = ticket.getComments().stream().anyMatch(c -> c.getText().equals("Checking in on ETA for the resolution."));
        assertTrue(commentFound, "Comment should be present in refreshed ticket");
    }

    @Test
    @Order(9)
    public void testStatsCalculation() {
        Stats stats = dataStore.getStats();
        assertNotNull(stats);
        assertTrue(stats.getTotal() >= 5);
        assertTrue(stats.getOpen() >= 0);
        assertTrue(stats.getInProgress() >= 0);
        assertTrue(stats.getResolved() >= 0);
        assertEquals(stats.getTotal(), stats.getOpen() + stats.getInProgress() + stats.getResolved() + stats.getClosed());
    }
}
