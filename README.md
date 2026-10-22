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
- **Database Layer**: MySQL 8.0+ / MariaDB via JDBC (`mysql-connector-j`, InnoDB, 3NF Normalized Relational Model).
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
│       │   ├── service/        # Persistent MySQL JDBC DataStore DAO
│       │   └── util/           # DBConnection & high-performance JsonUtil
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

### 1. Database Setup & Configuration
1. **Import Relational Database Schema & Seed Data**:
   Ensure MySQL 8.0+ is running locally and import the database schema:
   ```bash
   mysql -u root -p < campus_connect_schema.sql
   ```

2. **Configure Environment Variables**:
   Provide your local MySQL credentials safely via environment variables (defaults are `root` with no password):
   ```bash
   # Linux / macOS
   export DB_USER="your_mysql_username"
   export DB_PASS="your_mysql_password"

   # Windows (PowerShell)
   $env:DB_USER="your_mysql_username"
   $env:DB_PASS="your_mysql_password"
   ```
   > Real passwords and credentials must never be committed to source control.

### 2. Build the Project
Compile classes, package the WAR archive, and verify the MySQL JDBC driver:
```bash
./build.sh
```

### 3. Start Apache Tomcat Server
Launch the server runtime:

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

---

## 🌐 Public Deployment Guide

CampusConnect can be deployed publicly using multiple turnkey options:

### Option A: Instant Live Public URL (Cloudflare Tunnel)
You can instantly expose your local Tomcat server to the global internet over secure HTTPS with zero server setup:
```bash
cloudflared tunnel --url http://localhost:8090
```
This outputs a public URL (e.g., `https://your-tunnel-name.trycloudflare.com`) accessible from any browser or smartphone worldwide.

### Option B: 24/7 Cloud Hosting (Render / Railway with Docker)
A production multi-stage [`Dockerfile`](Dockerfile) is pre-configured with Tomcat 10.1 and Java 17:
1. Push your code to GitHub: `git push origin main`.
2. On **[Render](https://render.com)** or **[Railway](https://railway.app)**:
   - Create a **New Web Service** from your GitHub repository `Campus_Connect-`.
   - Select runtime: **Docker** (uses repository `Dockerfile` and `render.yaml`).
   - (Optional) Connect a managed MySQL database and add environment variables:
     - `DB_URL`: `jdbc:mysql://<host>:<port>/<dbname>?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC`
     - `DB_USER`: `<username>`
     - `DB_PASS`: `<password>`
   - If no MySQL is connected, CampusConnect automatically runs with its resilient in-memory store so the app is always live and functional.

### Option C: GitHub Pages (Frontend & Client-Side Interactive Demo)
The frontend (`index.html`, `auth.html`, `user-dashboard.html`, `admin-dashboard.html`) includes built-in offline synchronization with demo data:
1. Go to your GitHub repository **Settings** > **Pages**.
2. Under **Build and deployment**, select source: **Deploy from a branch**.
3. Choose branch `main` and folder `/ (root)`, then click **Save**.
4. Your site will be published at: `https://<username>.github.io/Campus_Connect-/`.

---

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
