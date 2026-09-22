# CampusConnect - Complaint & Ticket Management System

A centralized campus grievance redressal and ticket resolution portal designed for students, faculty, and administration.

## 🌟 Key Features
- **Public Landing Page**: Quick ticket tracking, feature discovery, departmental directory, and FAQs.
- **Role-Based Authentication**: Dedicated sign-in and registration portals for students and administrators.
- **Student Dashboard**: 
  - File detailed grievances with category, priority, description, and attachments.
  - Track grievance statuses with real-time timeline visualization.
  - Direct updates and resolution logs.
- **Admin Operations Desk**:
  - Live KPI metrics (Total, Pending, In Progress, Resolved).
  - Triage grievances, reassign priority/department, and add resolution remarks.
  - Advanced search and filter controls.

## 🛠️ Tech Stack
- **Frontend**: HTML5, Vanilla CSS3 (Custom responsive design system with modern dark glassmorphism aesthetic), JavaScript (ES6+).
- **Database (Ready for Backend Integration)**: MySQL / MariaDB (InnoDB, 3NF Normalized Relational Model).
  - Schema Script: [`campus_connect_schema.sql`](campus_connect_schema.sql)
  - Schema Design Specification: [`campus_connect_schema.pdf`](campus_connect_schema.pdf)

## 📄 Evaluation Outputs
- **Phase 1 Output Screenshots & Documentation**: [`CampusConnect_Key_Outputs.pdf`](CampusConnect_Key_Outputs.pdf)
- High-resolution screen captures available in [`screenshots/`](screenshots/).

## 🚀 Running Locally
You can run this project locally using Python's built-in HTTP server:

```bash
python3 -m http.server 5500
```

Then visit: [http://localhost:5500/index.html](http://localhost:5500/index.html)

