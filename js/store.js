/**
 * Complaint & Ticket Management System - Data Store & Mock API Layer
 * 
 * Provides client-side persistence using localStorage, preloaded with 
 * realistic college tickets and sample accounts. Structured cleanly so it 
 * can be replaced with Java Spring Boot REST API calls seamlessly.
 */

const STORAGE_KEYS = {
  TICKETS: 'campus_tickets_data',
  CURRENT_USER: 'campus_current_user',
  USERS: 'campus_users_data',
  INITIALIZED: 'campus_store_initialized'
};

const INITIAL_USERS = [
  {
    id: 'usr_student_1',
    name: 'Alex Rivera',
    email: 'alex.student@college.edu',
    password: 'password123',
    role: 'student',
    studentId: 'CS-2023-042',
    department: 'Computer Science & Engineering',
    phone: '+1 (555) 234-5678'
  },
  {
    id: 'usr_admin_1',
    name: 'Dr. Sarah Jenkins',
    email: 'admin@college.edu',
    password: 'admin123',
    role: 'admin',
    department: 'Campus Grievance & Operations Cell',
    title: 'Chief Grievance Officer'
  }
];

const INITIAL_TICKETS = [
  {
    id: 'TKT-2026-101',
    title: 'Hostel Block B Wi-Fi AP frequent dropouts during evening hours',
    category: 'IT & Network Support',
    priority: 'High',
    status: 'in_progress',
    location: 'Hostel Block B, 3rd Floor Corridor',
    description: 'The Wi-Fi access point in 3rd-floor hallway disconnects repeatedly after 7 PM, preventing students from accessing online lecture archives and coursework submissions.',
    submittedBy: {
      name: 'Alex Rivera',
      email: 'alex.student@college.edu',
      studentId: 'CS-2023-042'
    },
    assignedTo: 'Network Operations Team',
    createdAt: '2026-09-16T14:30:00Z',
    updatedAt: '2026-09-17T09:15:00Z',
    timeline: [
      { status: 'open', label: 'Ticket Submitted', date: '2026-09-16 14:30', note: 'Complaint logged via student portal.' },
      { status: 'in_progress', label: 'Assigned to Network Operations', date: '2026-09-17 09:15', note: 'Hardware diagnostic scheduled for AP-B304 switch port.' }
    ],
    comments: [
      {
        id: 'c1',
        author: 'Alex Rivera',
        role: 'student',
        text: 'This has been ongoing for 3 days. Several roommates are also affected.',
        date: '2026-09-16 14:35'
      },
      {
        id: 'c2',
        author: 'Network Operations Team',
        role: 'admin',
        text: 'A technician will inspect the signal interference and reboot the switch tonight between 11 PM and 11:30 PM.',
        date: '2026-09-17 09:15'
      }
    ]
  },
  {
    id: 'TKT-2026-102',
    title: 'Lab 3 Air Conditioning unit leaking water over student workstations',
    category: 'Hostel & Maintenance',
    priority: 'Urgent',
    status: 'open',
    location: 'Turing Hall, Lab 3, Row 4',
    description: 'Condensation line is clogged and dripping onto Desktop stations 14 and 15. Risk of electrical short circuit if not serviced immediately.',
    submittedBy: {
      name: 'Alex Rivera',
      email: 'alex.student@college.edu',
      studentId: 'CS-2023-042'
    },
    assignedTo: 'Electrical & HVAC Dept',
    createdAt: '2026-09-18T08:10:00Z',
    updatedAt: '2026-09-18T08:10:00Z',
    timeline: [
      { status: 'open', label: 'Ticket Submitted', date: '2026-09-18 08:10', note: 'High priority maintenance request registered.' }
    ],
    comments: [
      {
        id: 'c3',
        author: 'Alex Rivera',
        role: 'student',
        text: 'We covered the desktop screens with protective tarp for now.',
        date: '2026-09-18 08:12'
      }
    ]
  },
  {
    id: 'TKT-2026-103',
    title: 'Mid-term hall ticket missing course code CS-402 Database Systems',
    category: 'Academics & Examination',
    priority: 'Medium',
    status: 'resolved',
    location: 'Academic Registrar Portal',
    description: 'My hall ticket generated on the student ERP portal displays only 4 registered courses instead of 5. CS-402 is omitted despite confirmed elective registration.',
    submittedBy: {
      name: 'Alex Rivera',
      email: 'alex.student@college.edu',
      studentId: 'CS-2023-042'
    },
    assignedTo: 'Exam Cell Controller',
    createdAt: '2026-09-14T10:00:00Z',
    updatedAt: '2026-09-15T16:20:00Z',
    timeline: [
      { status: 'open', label: 'Submitted', date: '2026-09-14 10:00', note: 'Hall ticket discrepancy filed.' },
      { status: 'in_progress', label: 'Under Review', date: '2026-09-14 15:45', note: 'Cross-verifying ERP elective records.' },
      { status: 'resolved', label: 'Resolved & Updated', date: '2026-09-15 16:20', note: 'Elective sync completed. Student hall ticket regenerated.' }
    ],
    comments: [
      {
        id: 'c4',
        author: 'Exam Cell Controller',
        role: 'admin',
        text: 'The elective database sync lag was corrected. Please log in to your ERP portal and re-download the amended hall ticket.',
        date: '2026-09-15 16:20'
      }
    ]
  },
  {
    id: 'TKT-2026-104',
    title: 'Digital Library IEEE Xplore off-campus VPN access credentials expiring',
    category: 'Library & Digital Resources',
    priority: 'Low',
    status: 'open',
    location: 'Central Digital Library',
    description: 'Attempting to access IEEE Xplore through institutional proxy returns 403 authorization error for batch of final year CSE students.',
    submittedBy: {
      name: 'Priya Sharma',
      email: 'priya.s@college.edu',
      studentId: 'CS-2023-088'
    },
    assignedTo: 'Library IT Helpdesk',
    createdAt: '2026-09-17T11:20:00Z',
    updatedAt: '2026-09-17T11:20:00Z',
    timeline: [
      { status: 'open', label: 'Ticket Submitted', date: '2026-09-17 11:20', note: 'Proxy authentication ticket logged.' }
    ],
    comments: []
  },
  {
    id: 'TKT-2026-105',
    title: 'Cafeteria Water Cooler filter replacement and hygiene audit',
    category: 'Cafeteria & Mess',
    priority: 'High',
    status: 'resolved',
    location: 'North Block Canteen Area',
    description: 'The TDS indicator on the central water purification station in North Canteen showed service required since Monday.',
    submittedBy: {
      name: 'Rohan Gupta',
      email: 'rohan.g@college.edu',
      studentId: 'ME-2023-019'
    },
    assignedTo: 'Campus Sanitation Unit',
    createdAt: '2026-09-12T09:00:00Z',
    updatedAt: '2026-09-13T14:10:00Z',
    timeline: [
      { status: 'open', label: 'Ticket Submitted', date: '2026-09-12 09:00', note: 'Canteen filter service requested.' },
      { status: 'in_progress', label: 'Vendor Dispatched', date: '2026-09-12 13:00', note: 'RO service engineer called.' },
      { status: 'resolved', label: 'Filters Replaced', date: '2026-09-13 14:10', note: 'Sediment and carbon candles renewed, TDS calibrated to 110 ppm.' }
    ],
    comments: [
      {
        id: 'c5',
        author: 'Campus Sanitation Unit',
        role: 'admin',
        text: 'All filters replaced and water quality certified by the campus health committee.',
        date: '2026-09-13 14:10'
      }
    ]
  }
];

class CampusStore {
  constructor() {
    this.init();
  }

  init() {
    if (!localStorage.getItem(STORAGE_KEYS.INITIALIZED)) {
      localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(INITIAL_USERS));
      localStorage.setItem(STORAGE_KEYS.TICKETS, JSON.stringify(INITIAL_TICKETS));
      localStorage.setItem(STORAGE_KEYS.CURRENT_USER, JSON.stringify(INITIAL_USERS[0])); // default student
      localStorage.setItem(STORAGE_KEYS.INITIALIZED, 'true');
    }
  }

  // --- Auth Methods ---
  getCurrentUser() {
    const userStr = localStorage.getItem(STORAGE_KEYS.CURRENT_USER);
    return userStr ? JSON.parse(userStr) : null;
  }

  setCurrentUser(user) {
    if (!user) {
      localStorage.removeItem(STORAGE_KEYS.CURRENT_USER);
    } else {
      localStorage.setItem(STORAGE_KEYS.CURRENT_USER, JSON.stringify(user));
    }
  }

  login(email, password, roleHint = null) {
    const users = JSON.parse(localStorage.getItem(STORAGE_KEYS.USERS) || '[]');
    const user = users.find(u => u.email.toLowerCase() === email.trim().toLowerCase());
    
    if (!user) {
      return { success: false, message: 'No account found with this email address.' };
    }
    if (user.password !== password) {
      return { success: false, message: 'Invalid password. Please try again.' };
    }
    if (roleHint && user.role !== roleHint) {
      return { success: false, message: `Access denied. Account does not have ${roleHint} permissions.` };
    }

    this.setCurrentUser(user);
    return { success: true, user };
  }

  demoLogin(role) {
    const users = JSON.parse(localStorage.getItem(STORAGE_KEYS.USERS) || '[]');
    const target = users.find(u => u.role === role);
    if (target) {
      this.setCurrentUser(target);
      return { success: true, user: target };
    }
    return { success: false, message: 'Demo account not found' };
  }

  register(userData) {
    const users = JSON.parse(localStorage.getItem(STORAGE_KEYS.USERS) || '[]');
    const exists = users.some(u => u.email.toLowerCase() === userData.email.trim().toLowerCase());
    
    if (exists) {
      return { success: false, message: 'An account with this email already exists.' };
    }

    const newUser = {
      id: 'usr_' + Date.now(),
      name: userData.name,
      email: userData.email,
      password: userData.password,
      role: userData.role || 'student',
      studentId: userData.studentId || 'ID-' + Math.floor(1000 + Math.random() * 9000),
      department: userData.department || 'General'
    };

    users.push(newUser);
    localStorage.setItem(STORAGE_KEYS.USERS, JSON.stringify(users));
    this.setCurrentUser(newUser);
    return { success: true, user: newUser };
  }

  logout() {
    this.setCurrentUser(null);
  }

  // --- Ticket CRUD Methods ---
  getTickets(filter = {}) {
    let tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');

    // Filter by role if user is student
    if (filter.studentEmail) {
      tickets = tickets.filter(t => t.submittedBy.email.toLowerCase() === filter.studentEmail.toLowerCase());
    }

    // Filter by status
    if (filter.status && filter.status !== 'all') {
      tickets = tickets.filter(t => t.status === filter.status);
    }

    // Filter by category
    if (filter.category && filter.category !== 'all') {
      tickets = tickets.filter(t => t.category === filter.category);
    }

    // Filter by priority
    if (filter.priority && filter.priority !== 'all') {
      tickets = tickets.filter(t => t.priority.toLowerCase() === filter.priority.toLowerCase());
    }

    // Search query
    if (filter.search) {
      const q = filter.search.toLowerCase().trim();
      tickets = tickets.filter(t => 
        t.id.toLowerCase().includes(q) ||
        t.title.toLowerCase().includes(q) ||
        t.category.toLowerCase().includes(q) ||
        t.description.toLowerCase().includes(q) ||
        t.location.toLowerCase().includes(q)
      );
    }

    // Sort by latest
    tickets.sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
    return tickets;
  }

  getTicketById(id) {
    if (!id) return null;
    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    return tickets.find(t => t.id.toUpperCase() === id.trim().toUpperCase()) || null;
  }

  createTicket({ title, category, priority, location, description }) {
    const user = this.getCurrentUser();
    if (!user) {
      return { success: false, message: 'You must be signed in to submit a ticket.' };
    }

    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    const nextSeq = 100 + tickets.length + 1;
    const newId = `TKT-2026-${nextSeq}`;
    const nowIso = new Date().toISOString();
    const formattedNow = new Date().toLocaleString('en-US', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    }).replace(',', '');

    const newTicket = {
      id: newId,
      title: title.trim(),
      category: category,
      priority: priority || 'Medium',
      status: 'open',
      location: location ? location.trim() : 'Campus Premises',
      description: description.trim(),
      submittedBy: {
        name: user.name,
        email: user.email,
        studentId: user.studentId || 'STU-GEN'
      },
      assignedTo: 'Pending Assignment',
      createdAt: nowIso,
      updatedAt: nowIso,
      timeline: [
        {
          status: 'open',
          label: 'Ticket Submitted',
          date: formattedNow,
          note: 'New grievance logged by student.'
        }
      ],
      comments: []
    };

    tickets.unshift(newTicket);
    localStorage.setItem(STORAGE_KEYS.TICKETS, JSON.stringify(tickets));
    return { success: true, ticket: newTicket };
  }

  updateTicketStatus(id, newStatus, adminNote = '') {
    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    const idx = tickets.findIndex(t => t.id.toUpperCase() === id.trim().toUpperCase());
    if (idx === -1) {
      return { success: false, message: 'Ticket not found' };
    }

    const formattedNow = new Date().toLocaleString('en-US', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    }).replace(',', '');

    tickets[idx].status = newStatus;
    tickets[idx].updatedAt = new Date().toISOString();

    const statusLabels = {
      open: 'Status reset to Open',
      in_progress: 'Marked In-Progress',
      resolved: 'Ticket Resolved',
      closed: 'Ticket Closed'
    };

    tickets[idx].timeline.push({
      status: newStatus,
      label: statusLabels[newStatus] || 'Status Updated',
      date: formattedNow,
      note: adminNote || `Status updated to ${newStatus}`
    });

    if (adminNote) {
      const user = this.getCurrentUser();
      tickets[idx].comments.push({
        id: 'c_' + Date.now(),
        author: user ? user.name : 'Administrator',
        role: 'admin',
        text: `[Status Update: ${newStatus.toUpperCase()}] ${adminNote}`,
        date: formattedNow
      });
    }

    localStorage.setItem(STORAGE_KEYS.TICKETS, JSON.stringify(tickets));
    return { success: true, ticket: tickets[idx] };
  }

  assignTicket(id, departmentOrStaff) {
    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    const idx = tickets.findIndex(t => t.id.toUpperCase() === id.trim().toUpperCase());
    if (idx === -1) return { success: false, message: 'Ticket not found' };

    const formattedNow = new Date().toLocaleString('en-US', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    }).replace(',', '');

    tickets[idx].assignedTo = departmentOrStaff;
    if (tickets[idx].status === 'open') {
      tickets[idx].status = 'in_progress';
    }
    tickets[idx].updatedAt = new Date().toISOString();

    tickets[idx].timeline.push({
      status: tickets[idx].status,
      label: 'Ticket Assigned',
      date: formattedNow,
      note: `Assigned to: ${departmentOrStaff}`
    });

    localStorage.setItem(STORAGE_KEYS.TICKETS, JSON.stringify(tickets));
    return { success: true, ticket: tickets[idx] };
  }

  addComment(ticketId, text) {
    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    const idx = tickets.findIndex(t => t.id.toUpperCase() === ticketId.trim().toUpperCase());
    if (idx === -1) return { success: false, message: 'Ticket not found' };

    const user = this.getCurrentUser() || { name: 'Anonymous', role: 'student' };
    const formattedNow = new Date().toLocaleString('en-US', {
      year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false
    }).replace(',', '');

    const newComment = {
      id: 'c_' + Date.now(),
      author: user.name,
      role: user.role,
      text: text.trim(),
      date: formattedNow
    };

    if (!tickets[idx].comments) tickets[idx].comments = [];
    tickets[idx].comments.push(newComment);
    tickets[idx].updatedAt = new Date().toISOString();

    localStorage.setItem(STORAGE_KEYS.TICKETS, JSON.stringify(tickets));
    return { success: true, comment: newComment };
  }

  getStats() {
    const tickets = JSON.parse(localStorage.getItem(STORAGE_KEYS.TICKETS) || '[]');
    return {
      total: tickets.length,
      open: tickets.filter(t => t.status === 'open').length,
      inProgress: tickets.filter(t => t.status === 'in_progress').length,
      resolved: tickets.filter(t => t.status === 'resolved').length,
      closed: tickets.filter(t => t.status === 'closed').length,
      urgent: tickets.filter(t => t.priority.toLowerCase() === 'urgent' && t.status !== 'resolved' && t.status !== 'closed').length
    };
  }

  // Reset store helper for demo presentations
  resetToSampleData() {
    localStorage.removeItem(STORAGE_KEYS.INITIALIZED);
    this.init();
  }
}

// Global Singleton Instance
window.campusStore = new CampusStore();
