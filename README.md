# CampusConnect - Complaint & Ticket Management System

A centralized campus grievance redressal and ticket resolution portal designed for students, faculty, and administration.

---

## 🌟 Key Features
- **Public Landing Page**: Quick ticket tracking, feature discovery, departmental directory, and FAQs.
- **Role-Based Authentication**: Dedicated sign-in and registration portals with session management for students and administrators.
- **Student Dashboard**: 
  - File detailed grievances with category, priority, description, and location.
  - Track grievance statuses with real-time timeline visualization.
  - Direct updates, resolution logs, and conversation threads.
- **Admin Operations Desk**:
  - Live KPI metrics (Total, Pending, In Progress, Resolved, Urgent).
  - Triage grievances, reassign priority/department, and add official resolution remarks.
  - Advanced search and category/status filter controls.

---

## 🛠️ Tech Stack
- **Frontend**: HTML5, Vanilla CSS3 (Custom responsive design system with modern dark glassmorphism aesthetic), JavaScript (ES6+).
- **Backend**: Java Servlets (Jakarta EE 10 / Servlet 6.0 API), Session Management, CORS Filtering.
- **Servlet Container / Web Server**: Apache Tomcat 10.1+.
- **Build System**: Apache Maven (`pom.xml` -> `CampusConnect.war`).
- **Database Model (Ready for JDBC/MySQL Integration)**: MySQL / MariaDB (InnoDB, 3NF Normalized Relational Model).
  - Schema Script: [`campus_connect_schema.sql`](campus_connect_schema.sql)
  - Schema Design Specification: [`campus_connect_schema.pdf`](campus_connect_schema.pdf)

---

## 📁 Project Structure

```text
├── pom.xml                     # Maven project descriptor & build configuration
├── build.sh                    # Compiles code & packages CampusConnect.war
├── start-server.sh             # Starts Apache Tomcat on port 8090
├── stop-server.sh              # Stops Apache Tomcat
├── campus_connect_schema.sql   # Relational database schema & seed SQL
├── src/
│   └── main/
│       ├── java/com/campusconnect/
│       │   ├── controller/     # Java Servlets (Auth, Tickets, Track, Stats, Reset)
│       │   ├── filter/         # CORS & Preflight Filter
│       │   ├── model/          # User, Ticket, TimelineItem, Comment, Stats
│       │   ├── service/        # Thread-safe in-memory DataStore (Seed synced)
│       │   └── util/           # High-performance JsonUtil
│       └── webapp/
│           ├── WEB-INF/web.xml # Deployment descriptor
│           ├── css/            # Style sheets
│           ├── js/             # Frontend controllers & store API layer
│           ├── index.html      # Landing & Quick Track page
│           ├── auth.html       # Sign-in & Registration page
│           ├── user-dashboard.html  # Student Grievance Portal
│           └── admin-dashboard.html # Administration Operations Desk
```

---

## 🚀 Running the Full Stack Application

### 1. Start with Apache Tomcat (Recommended)
You can start the Java Servlet backend and web portal with a single command:

```bash
./start-server.sh
```

- Portal URL: [http://localhost:8090/](http://localhost:8090/)
- Stats API: [http://localhost:8090/api/stats](http://localhost:8090/api/stats)
- Tickets API: [http://localhost:8090/api/tickets](http://localhost:8090/api/tickets)

To stop the server:
```bash
./stop-server.sh
```

### 2. Building from Source
To recompile Java classes and rebuild the WAR bundle:

```bash
./build.sh
```

Target WAR file generated at: `target/CampusConnect.war`

---

## 🔌 Backend REST API Reference

| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/login` | Authenticates user and sets session |
| `POST` | `/api/auth/register` | Registers a new student account |
| `POST` | `/api/auth/demo` | 1-Click quick demo session (`student` or `admin`) |
| `GET` | `/api/auth/me` | Fetches active authenticated user session |
| `POST` | `/api/auth/logout` | Invalidates active session |
| `GET` | `/api/tickets` | Returns filterable ticket list (`status`, `category`, `search`) |
| `POST` | `/api/tickets` | Creates a new grievance ticket (`TKT-2026-xxx`) |
| `GET` | `/api/ticket/{id}` | Returns full ticket details with timeline and comments |
| `POST` | `/api/ticket/{id}/status` | Updates ticket status with resolution remarks |
| `POST` | `/api/ticket/{id}/assign` | Reassigns ticket to department or staff |
| `POST` | `/api/ticket/{id}/comments` | Appends comment to discussion thread |
| `GET` | `/api/track?id=...` | Public ticket status tracker |
| `GET` | `/api/stats` | Returns real-time KPI metrics |
| `POST` | `/api/reset` | Resets in-memory store to sample demo tickets |

---

## 📄 Evaluation Outputs
- **Phase 1 Output Screenshots & Documentation**: [`CampusConnect_Key_Outputs.pdf`](CampusConnect_Key_Outputs.pdf)
- High-resolution screen captures available in [`screenshots/`](screenshots/).
