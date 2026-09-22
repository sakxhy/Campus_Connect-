-- ============================================================================
-- CampusConnect: Complaint & Ticket Management System
-- Relational Database Schema & Seed Data (MySQL / MariaDB Syntax)
-- Designed for Part 1 Project Evaluation (Relational Model & Frontend Sync)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS campus_connect;
USE campus_connect;

-- Disable foreign key checks for clean teardown/rebuild during testing
SET FOREIGN_KEY_CHECKS = 0;
DROP TABLE IF EXISTS ticket_comments;
DROP TABLE IF EXISTS ticket_timeline;
DROP TABLE IF EXISTS tickets;
DROP TABLE IF EXISTS categories;
DROP TABLE IF EXISTS admins;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;
SET FOREIGN_KEY_CHECKS = 1;

-- ----------------------------------------------------------------------------
-- 1. USERS — Base authentication and identity table for all platform roles
-- ----------------------------------------------------------------------------
CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('student', 'admin') NOT NULL,
    phone VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. STUDENTS — 1:1 extension table of users where role = 'student'
-- ----------------------------------------------------------------------------
CREATE TABLE students (
    user_id INT PRIMARY KEY,
    student_id VARCHAR(30) NOT NULL UNIQUE, -- e.g. CS-2023-042
    department VARCHAR(100) NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. ADMINS — 1:1 extension table of users where role = 'admin'
-- ----------------------------------------------------------------------------
CREATE TABLE admins (
    user_id INT PRIMARY KEY,
    department VARCHAR(100) NOT NULL, -- e.g. Campus Grievance & Operations Cell
    title VARCHAR(100),                -- e.g. Chief Grievance Officer
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. CATEGORIES — Departmental grievance classification lookup table
-- ----------------------------------------------------------------------------
CREATE TABLE categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. TICKETS — Core grievance record submitted by students
-- ----------------------------------------------------------------------------
CREATE TABLE tickets (
    ticket_id VARCHAR(20) PRIMARY KEY, -- e.g. TKT-2026-101
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category_id INT NOT NULL,
    priority ENUM('Low', 'Medium', 'High', 'Urgent') NOT NULL DEFAULT 'Medium',
    status ENUM('open', 'in_progress', 'resolved', 'closed') NOT NULL DEFAULT 'open',
    location VARCHAR(200),
    submitted_by INT NOT NULL, -- FK to users(user_id)
    assigned_to VARCHAR(150) DEFAULT 'Pending Assignment',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (category_id) REFERENCES categories(category_id) ON UPDATE CASCADE,
    FOREIGN KEY (submitted_by) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. TICKET_TIMELINE — Audit trail and state transition progression history
-- ----------------------------------------------------------------------------
CREATE TABLE ticket_timeline (
    timeline_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(20) NOT NULL,
    status ENUM('open', 'in_progress', 'resolved', 'closed') NOT NULL,
    label VARCHAR(150) NOT NULL, -- e.g. "Assigned to Network Operations"
    event_date DATETIME NOT NULL,
    note TEXT,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 7. TICKET_COMMENTS — Conversation thread on tickets between students & staff
-- ----------------------------------------------------------------------------
CREATE TABLE ticket_comments (
    comment_id INT AUTO_INCREMENT PRIMARY KEY,
    ticket_id VARCHAR(20) NOT NULL,
    author_id INT NOT NULL, -- FK to users(user_id)
    role ENUM('student', 'admin') NOT NULL,
    comment_text TEXT NOT NULL,
    comment_date DATETIME NOT NULL,
    FOREIGN KEY (ticket_id) REFERENCES tickets(ticket_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 8. INDEXES — Query performance optimizations for frontend search and filters
-- ----------------------------------------------------------------------------
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_priority ON tickets(priority);
CREATE INDEX idx_tickets_category ON tickets(category_id);
CREATE INDEX idx_tickets_submitted_by ON tickets(submitted_by);
CREATE INDEX idx_ticket_timeline_ticket_id ON ticket_timeline(ticket_id);
CREATE INDEX idx_ticket_comments_ticket_id ON ticket_comments(ticket_id);


-- ============================================================================
-- SEED DATA (Synchronized with Frontend Initial State in js/store.js)
-- ============================================================================

-- Categories
INSERT INTO categories (category_id, name) VALUES
(1, 'IT & Network Support'),
(2, 'Hostel & Maintenance'),
(3, 'Academics & Examination'),
(4, 'Library & Digital Resources'),
(5, 'Cafeteria & Mess'),
(6, 'Accounts & Finance');

-- Base Users
INSERT INTO users (user_id, name, email, password_hash, role, phone, created_at) VALUES
(1, 'Alex Rivera', 'alex.student@college.edu', '$2a$12$e8x/demo_hash_student_password123', 'student', '+1 (555) 234-5678', '2026-09-01 09:00:00'),
(2, 'Dr. Sarah Jenkins', 'admin@college.edu', '$2a$12$e8x/demo_hash_admin_admin123', 'admin', '+1 (555) 987-6543', '2026-08-15 10:00:00'),
(3, 'Priya Sharma', 'priya.s@college.edu', '$2a$12$e8x/demo_hash_student_priya123', 'student', '+1 (555) 345-6789', '2026-09-05 11:30:00');

-- Student Profiles
INSERT INTO students (user_id, student_id, department) VALUES
(1, 'CS-2023-042', 'Computer Science & Engineering'),
(3, 'CS-2023-088', 'Computer Science & Engineering');

-- Admin Profiles
INSERT INTO admins (user_id, department, title) VALUES
(2, 'Campus Grievance & Operations Cell', 'Chief Grievance Officer');

-- Tickets
INSERT INTO tickets (ticket_id, title, description, category_id, priority, status, location, submitted_by, assigned_to, created_at, updated_at) VALUES
('TKT-2026-101', 
 'Hostel Block B Wi-Fi AP frequent dropouts during evening hours',
 'The Wi-Fi access point in 3rd-floor hallway disconnects repeatedly after 7 PM, preventing students from accessing online lecture archives and coursework submissions.',
 1, 'High', 'in_progress', 'Hostel Block B, 3rd Floor Corridor', 1, 'Network Operations Team',
 '2026-09-16 14:30:00', '2026-09-17 09:15:00'),

('TKT-2026-102',
 'Lab 3 Air Conditioning unit leaking water over student workstations',
 'Condensation line is clogged and dripping onto Desktop stations 14 and 15. Risk of electrical short circuit if not serviced immediately.',
 2, 'Urgent', 'open', 'Turing Hall, Lab 3, Row 4', 1, 'Electrical & HVAC Dept',
 '2026-09-18 08:10:00', '2026-09-18 08:10:00'),

('TKT-2026-103',
 'Mid-term hall ticket missing course code CS-402 Database Systems',
 'My hall ticket generated on the student ERP portal displays only 4 registered courses instead of 5. CS-402 is omitted despite confirmed elective registration.',
 3, 'Medium', 'resolved', 'Academic Registrar Portal', 1, 'Exam Cell Controller',
 '2026-09-14 10:00:00', '2026-09-15 16:20:00'),

('TKT-2026-104',
 'Digital Library IEEE Xplore off-campus VPN access credentials expiring',
 'Attempting to access IEEE Xplore through institutional proxy returns 403 authorization error for batch of final year CSE students.',
 4, 'Low', 'open', 'Central Digital Library', 3, 'Library IT Helpdesk',
 '2026-09-17 11:20:00', '2026-09-17 11:20:00'),

('TKT-2026-105',
 'Cafeteria Water Cooler filter replacement and hygiene audit',
 'Water dispenser on Ground floor cafeteria tastes chlorinated and filter indicator light has been blinking red for 48 hours.',
 5, 'Medium', 'open', 'Central Cafeteria Block, Ground Floor', 1, 'Campus Facilities & Hygiene',
 '2026-09-18 12:45:00', '2026-09-18 12:45:00');

-- Ticket Timeline History
INSERT INTO ticket_timeline (ticket_id, status, label, event_date, note) VALUES
('TKT-2026-101', 'open', 'Ticket Submitted', '2026-09-16 14:30:00', 'Complaint logged via student portal.'),
('TKT-2026-101', 'in_progress', 'Assigned to Network Operations', '2026-09-17 09:15:00', 'Hardware diagnostic scheduled for AP-B304 switch port.'),

('TKT-2026-102', 'open', 'Ticket Submitted', '2026-09-18 08:10:00', 'High priority maintenance request registered.'),

('TKT-2026-103', 'open', 'Submitted', '2026-09-14 10:00:00', 'Hall ticket discrepancy filed.'),
('TKT-2026-103', 'in_progress', 'Under Review', '2026-09-14 15:45:00', 'Cross-verifying ERP elective records.'),
('TKT-2026-103', 'resolved', 'Resolved & Updated', '2026-09-15 16:20:00', 'Elective sync completed. Student hall ticket regenerated.'),

('TKT-2026-104', 'open', 'Ticket Submitted', '2026-09-17 11:20:00', 'Proxy authentication ticket logged.'),

('TKT-2026-105', 'open', 'Ticket Submitted', '2026-09-18 12:45:00', 'Water dispenser maintenance request submitted.');

-- Ticket Comments
INSERT INTO ticket_comments (ticket_id, author_id, role, comment_text, comment_date) VALUES
('TKT-2026-101', 1, 'student', 'This has been ongoing for 3 days. Several roommates are also affected.', '2026-09-16 14:35:00'),
('TKT-2026-101', 2, 'admin', 'A technician will inspect the signal interference and reboot the switch tonight between 11 PM and 11:30 PM.', '2026-09-17 09:15:00'),

('TKT-2026-102', 1, 'student', 'We covered the desktop screens with protective tarp for now.', '2026-09-18 08:12:00'),

('TKT-2026-103', 2, 'admin', 'The elective database sync lag was corrected. Please log in to your ERP portal and re-download the amended hall ticket.', '2026-09-15 16:20:00'),

('TKT-2026-105', 1, 'student', 'Students are purchasing bottled water currently. Please expedite.', '2026-09-18 13:00:00');
