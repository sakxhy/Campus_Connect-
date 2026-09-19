/**
 * Complaint & Ticket Management System - UI Controller & Interactions
 */

// Toast Notifications
function showToast(message, type = 'info') {
  let container = document.querySelector('.toast-container');
  if (!container) {
    container = document.createElement('div');
    container.className = 'toast-container';
    document.body.appendChild(container);
  }

  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  
  const icon = type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ';
  toast.innerHTML = `
    <span style="font-weight:bold; font-size:1.1rem;">${icon}</span>
    <div class="toast-message">${message}</div>
  `;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = '0';
    toast.style.transform = 'translateX(100%)';
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}

// Badge Helpers
function getStatusBadge(status) {
  const map = {
    open: { class: 'badge-open', label: 'Open' },
    in_progress: { class: 'badge-progress', label: 'In Progress' },
    resolved: { class: 'badge-resolved', label: 'Resolved' },
    closed: { class: 'badge-closed', label: 'Closed' }
  };
  const item = map[status] || { class: 'badge-open', label: status };
  return `<span class="badge ${item.class}">${item.label}</span>`;
}

function getPriorityBadge(priority) {
  const p = (priority || 'medium').toLowerCase();
  const map = {
    urgent: 'badge-urgent',
    high: 'badge-high',
    medium: 'badge-medium',
    low: 'badge-low'
  };
  const cls = map[p] || 'badge-medium';
  return `<span class="badge ${cls}">${priority}</span>`;
}

// Stepper Progress Generator
function renderStepper(status) {
  const steps = [
    { key: 'open', label: 'Submitted' },
    { key: 'in_progress', label: 'In Progress' },
    { key: 'resolved', label: 'Resolved' }
  ];

  let activeIndex = 0;
  if (status === 'in_progress') activeIndex = 1;
  if (status === 'resolved' || status === 'closed') activeIndex = 2;

  return `
    <div class="stepper">
      ${steps.map((step, idx) => {
        let stateClass = '';
        if (idx < activeIndex) stateClass = 'completed';
        else if (idx === activeIndex) stateClass = 'active';
        return `
          <div class="step-item ${stateClass}">
            <div class="step-circle">${idx < activeIndex ? '✓' : idx + 1}</div>
            <div class="step-label">${step.label}</div>
          </div>
        `;
      }).join('')}
    </div>
  `;
}

// Modal Helpers
function openModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.add('active');
}

function closeModal(modalId) {
  const el = document.getElementById(modalId);
  if (el) el.classList.remove('active');
}

// Close modals when clicking backdrop
document.addEventListener('click', (e) => {
  if (e.target.classList.contains('modal-backdrop')) {
    e.target.classList.remove('active');
  }
});

// Setup Common Navbar Controls (User profile, Logout)
function initNavbar() {
  const currentUser = window.campusStore.getCurrentUser();
  const navUserEl = document.getElementById('nav-user-info');
  const logoutBtn = document.getElementById('btn-logout');

  if (navUserEl && currentUser) {
    navUserEl.innerHTML = `
      <div style="display:flex; align-items:center; gap:0.6rem;">
        <div style="width:34px; height:34px; border-radius:50%; background:linear-gradient(135deg, var(--primary), var(--secondary)); display:flex; align-items:center; justify-content:center; font-weight:700; font-size:0.85rem; color:#fff;">
          ${currentUser.name.charAt(0)}
        </div>
        <div style="text-align:left; line-height:1.2;">
          <div style="font-weight:600; font-size:0.85rem; color:var(--text-main);">${currentUser.name}</div>
          <div style="font-size:0.75rem; color:var(--text-muted); text-transform:capitalize;">${currentUser.role}</div>
        </div>
      </div>
    `;
  }

  if (logoutBtn) {
    logoutBtn.addEventListener('click', (e) => {
      e.preventDefault();
      window.campusStore.logout();
      showToast('You have been logged out.', 'info');
      setTimeout(() => {
        window.location.href = 'auth.html';
      }, 700);
    });
  }
}

// ==========================================================================
// PAGE CONTROLLER: Landing Page (index.html)
// ==========================================================================
function initLandingPage() {
  const trackForm = document.getElementById('quick-track-form');
  const trackInput = document.getElementById('quick-track-input');
  const trackResultModal = document.getElementById('track-result-modal');

  if (trackForm && trackInput) {
    trackForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const ticketId = trackInput.value.trim().toUpperCase();
      if (!ticketId) {
        showToast('Please enter a valid Ticket ID (e.g. TKT-2026-101)', 'error');
        return;
      }

      const ticket = window.campusStore.getTicketById(ticketId);
      if (!ticket) {
        showToast(`No ticket found matching ID "${ticketId}".`, 'error');
        return;
      }

      // Render details in Track Result Modal
      document.getElementById('track-modal-id').textContent = ticket.id;
      document.getElementById('track-modal-title').textContent = ticket.title;
      document.getElementById('track-modal-badges').innerHTML = `
        ${getStatusBadge(ticket.status)}
        ${getPriorityBadge(ticket.priority)}
        <span class="badge" style="background:rgba(255,255,255,0.06); color:var(--text-muted);">${ticket.category}</span>
      `;
      document.getElementById('track-modal-stepper').innerHTML = renderStepper(ticket.status);
      document.getElementById('track-modal-location').textContent = ticket.location;
      document.getElementById('track-modal-assigned').textContent = ticket.assignedTo || 'Under Review';
      document.getElementById('track-modal-desc').textContent = ticket.description;
      
      const timelineHtml = ticket.timeline.map(item => `
        <div style="display:flex; gap:0.75rem; margin-bottom:0.75rem; font-size:0.85rem;">
          <span style="color:#818cf8; font-family:var(--font-mono); font-weight:600;">${item.date}</span>
          <div>
            <strong style="color:var(--text-main);">${item.label}</strong>
            <div style="color:var(--text-muted);">${item.note}</div>
          </div>
        </div>
      `).join('');
      document.getElementById('track-modal-timeline').innerHTML = timelineHtml;

      openModal('track-result-modal');
    });
  }
}

// ==========================================================================
// PAGE CONTROLLER: Authentication (auth.html)
// ==========================================================================
function initAuthPage() {
  const tabs = document.querySelectorAll('.auth-tab');
  const loginForm = document.getElementById('login-form');
  const registerForm = document.getElementById('register-form');
  const demoStudentBtn = document.getElementById('demo-student-btn');
  const demoAdminBtn = document.getElementById('demo-admin-btn');

  tabs.forEach(tab => {
    tab.addEventListener('click', () => {
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');
      const target = tab.getAttribute('data-target');

      if (target === 'login') {
        loginForm.style.display = 'block';
        registerForm.style.display = 'none';
      } else {
        loginForm.style.display = 'none';
        registerForm.style.display = 'block';
      }
    });
  });

  // Login handler
  if (loginForm) {
    loginForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const email = document.getElementById('login-email').value;
      const pass = document.getElementById('login-password').value;
      const res = window.campusStore.login(email, pass);

      if (res.success) {
        showToast(`Welcome back, ${res.user.name}!`, 'success');
        setTimeout(() => {
          if (res.user.role === 'admin') {
            window.location.href = 'admin-dashboard.html';
          } else {
            window.location.href = 'user-dashboard.html';
          }
        }, 600);
      } else {
        showToast(res.message, 'error');
      }
    });
  }

  // Registration handler
  if (registerForm) {
    registerForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const name = document.getElementById('reg-name').value;
      const email = document.getElementById('reg-email').value;
      const studentId = document.getElementById('reg-id').value;
      const dept = document.getElementById('reg-dept').value;
      const password = document.getElementById('reg-password').value;

      const res = window.campusStore.register({
        name, email, studentId, department: dept, password, role: 'student'
      });

      if (res.success) {
        showToast('Registration successful! Redirecting to student dashboard...', 'success');
        setTimeout(() => {
          window.location.href = 'user-dashboard.html';
        }, 700);
      } else {
        showToast(res.message, 'error');
      }
    });
  }

  // Quick Demo Logins
  if (demoStudentBtn) {
    demoStudentBtn.addEventListener('click', () => {
      const res = window.campusStore.demoLogin('student');
      if (res.success) {
        showToast('Logged in as Student: Alex Rivera', 'success');
        setTimeout(() => {
          window.location.href = 'user-dashboard.html';
        }, 500);
      }
    });
  }

  if (demoAdminBtn) {
    demoAdminBtn.addEventListener('click', () => {
      const res = window.campusStore.demoLogin('admin');
      if (res.success) {
        showToast('Logged in as Grievance Admin: Dr. Sarah Jenkins', 'success');
        setTimeout(() => {
          window.location.href = 'admin-dashboard.html';
        }, 500);
      }
    });
  }
}

// ==========================================================================
// PAGE CONTROLLER: Student Dashboard (user-dashboard.html)
// ==========================================================================
function initUserDashboard() {
  let currentUser = window.campusStore.getCurrentUser();
  if (!currentUser) {
    window.location.href = 'auth.html';
    return;
  }

  // If navigating from Admin, seamlessly activate Student demo profile
  if (currentUser.role !== 'student') {
    window.campusStore.demoLogin('student');
    currentUser = window.campusStore.getCurrentUser();
    initNavbar();
  }

  // Update greeting
  const greetingEl = document.getElementById('user-greeting');
  if (greetingEl) {
    greetingEl.textContent = `Welcome back, ${currentUser.name}`;
  }

  // Filter state
  let currentFilter = {
    studentEmail: currentUser.role === 'admin' ? null : currentUser.email,
    status: 'all',
    category: 'all',
    search: ''
  };

  function updateStudentKPIs() {
    const myTickets = window.campusStore.getTickets({
      studentEmail: currentUser.role === 'admin' ? null : currentUser.email
    });

    document.getElementById('kpi-total').textContent = myTickets.length;
    document.getElementById('kpi-open').textContent = myTickets.filter(t => t.status === 'open').length;
    document.getElementById('kpi-progress').textContent = myTickets.filter(t => t.status === 'in_progress').length;
    document.getElementById('kpi-resolved').textContent = myTickets.filter(t => t.status === 'resolved' || t.status === 'closed').length;
  }

  function renderTicketsList() {
    const listContainer = document.getElementById('tickets-table-body');
    if (!listContainer) return;

    const tickets = window.campusStore.getTickets(currentFilter);

    if (tickets.length === 0) {
      listContainer.innerHTML = `
        <tr>
          <td colspan="6" style="text-align:center; padding:3rem; color:var(--text-muted);">
            <div style="font-size:2rem; margin-bottom:0.5rem;">📋</div>
            <div style="font-weight:600;">No complaints or tickets found</div>
            <div style="font-size:0.85rem; color:var(--text-dim); margin-top:0.25rem;">Try adjusting your search or submit a new grievance above.</div>
          </td>
        </tr>
      `;
      return;
    }

    listContainer.innerHTML = tickets.map(t => `
      <tr>
        <td class="ticket-id-cell">${t.id}</td>
        <td>
          <div style="font-weight:600; color:var(--text-main); margin-bottom:0.2rem;">${t.title}</div>
          <div style="font-size:0.8rem; color:var(--text-dim);"><span style="color:var(--text-muted);">📍 ${t.location}</span></div>
        </td>
        <td>
          <span style="font-size:0.85rem; color:var(--text-muted);">${t.category}</span>
        </td>
        <td>${getPriorityBadge(t.priority)}</td>
        <td>${getStatusBadge(t.status)}</td>
        <td style="text-align:right;">
          <button class="btn btn-secondary btn-sm" onclick="viewTicketDetails('${t.id}')">
            View Details
          </button>
        </td>
      </tr>
    `).join('');
  }

  // Filter Listeners
  const searchInput = document.getElementById('ticket-search');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      currentFilter.search = e.target.value;
      renderTicketsList();
    });
  }

  const statusSelect = document.getElementById('ticket-status-filter');
  if (statusSelect) {
    statusSelect.addEventListener('change', (e) => {
      currentFilter.status = e.target.value;
      renderTicketsList();
    });
  }

  const catSelect = document.getElementById('ticket-category-filter');
  if (catSelect) {
    catSelect.addEventListener('change', (e) => {
      currentFilter.category = e.target.value;
      renderTicketsList();
    });
  }

  // New Ticket Submission Form
  const newTicketForm = document.getElementById('new-ticket-form');
  if (newTicketForm) {
    newTicketForm.addEventListener('submit', (e) => {
      e.preventDefault();
      const title = document.getElementById('ticket-title').value;
      const category = document.getElementById('ticket-category').value;
      const priority = document.getElementById('ticket-priority').value;
      const location = document.getElementById('ticket-location').value;
      const description = document.getElementById('ticket-description').value;

      const res = window.campusStore.createTicket({
        title, category, priority, location, description
      });

      if (res.success) {
        showToast(`Complaint registered successfully! ID: ${res.ticket.id}`, 'success');
        newTicketForm.reset();
        closeModal('modal-new-ticket');
        updateStudentKPIs();
        renderTicketsList();
      } else {
        showToast(res.message, 'error');
      }
    });
  }

  // Initial render
  updateStudentKPIs();
  renderTicketsList();
}

// Global View Ticket Details Modal handler
window.viewTicketDetails = function(ticketId) {
  const ticket = window.campusStore.getTicketById(ticketId);
  if (!ticket) {
    showToast('Unable to locate ticket record.', 'error');
    return;
  }

  const modal = document.getElementById('modal-ticket-details');
  if (!modal) return;

  document.getElementById('detail-ticket-id').textContent = ticket.id;
  document.getElementById('detail-title').textContent = ticket.title;
  document.getElementById('detail-badges').innerHTML = `
    ${getStatusBadge(ticket.status)}
    ${getPriorityBadge(ticket.priority)}
    <span class="badge" style="background:rgba(255,255,255,0.06); color:var(--text-muted);">${ticket.category}</span>
  `;
  document.getElementById('detail-stepper').innerHTML = renderStepper(ticket.status);
  document.getElementById('detail-location').textContent = ticket.location;
  document.getElementById('detail-assigned').textContent = ticket.assignedTo || 'Pending Review';
  document.getElementById('detail-submitted-by').textContent = `${ticket.submittedBy.name} (${ticket.submittedBy.studentId})`;
  document.getElementById('detail-description').textContent = ticket.description;

  // Timeline
  const timelineEl = document.getElementById('detail-timeline');
  if (timelineEl) {
    timelineEl.innerHTML = ticket.timeline.map(item => `
      <div style="display:flex; gap:0.75rem; margin-bottom:0.75rem; font-size:0.85rem;">
        <span style="color:#818cf8; font-family:var(--font-mono); font-weight:600; min-width:130px;">${item.date}</span>
        <div>
          <strong style="color:var(--text-main);">${item.label}</strong>
          <div style="color:var(--text-muted); font-size:0.8rem;">${item.note}</div>
        </div>
      </div>
    `).join('');
  }

  // Conversation Comments
  renderComments(ticket);

  // Bind comment reply form
  const replyForm = document.getElementById('ticket-comment-form');
  if (replyForm) {
    replyForm.onsubmit = (e) => {
      e.preventDefault();
      const input = document.getElementById('comment-input');
      const text = input.value.trim();
      if (!text) return;

      const res = window.campusStore.addComment(ticket.id, text);
      if (res.success) {
        input.value = '';
        showToast('Comment sent', 'success');
        const updated = window.campusStore.getTicketById(ticket.id);
        renderComments(updated);
      }
    };
  }

  openModal('modal-ticket-details');
};

function renderComments(ticket) {
  const container = document.getElementById('detail-comments-list');
  if (!container) return;

  if (!ticket.comments || ticket.comments.length === 0) {
    container.innerHTML = `
      <div style="text-align:center; padding:1.5rem; color:var(--text-dim); font-size:0.85rem;">
        No comments yet. Post an inquiry or update below.
      </div>
    `;
    return;
  }

  container.innerHTML = ticket.comments.map(c => `
    <div class="message-bubble ${c.role === 'admin' ? 'admin' : 'student'}">
      <div class="message-header">
        <span class="message-author">${c.author} <small style="font-weight:normal; color:var(--text-dim);">(${c.role})</small></span>
        <span class="message-time">${c.date}</span>
      </div>
      <div class="message-text">${c.text}</div>
    </div>
  `).join('');
}

// ==========================================================================
// PAGE CONTROLLER: Admin Dashboard (admin-dashboard.html)
// ==========================================================================
function initAdminDashboard() {
  let currentUser = window.campusStore.getCurrentUser();
  if (!currentUser) {
    window.location.href = 'auth.html';
    return;
  }

  // If navigating from Student, seamlessly activate Admin demo profile
  if (currentUser.role !== 'admin') {
    window.campusStore.demoLogin('admin');
    currentUser = window.campusStore.getCurrentUser();
    initNavbar();
  }

  let adminFilter = {
    status: 'all',
    category: 'all',
    priority: 'all',
    search: ''
  };

  function updateAdminKPIs() {
    const stats = window.campusStore.getStats();
    document.getElementById('admin-kpi-total').textContent = stats.total;
    document.getElementById('admin-kpi-open').textContent = stats.open;
    document.getElementById('admin-kpi-progress').textContent = stats.inProgress;
    document.getElementById('admin-kpi-resolved').textContent = stats.resolved;
    document.getElementById('admin-kpi-urgent').textContent = stats.urgent;
  }

  function renderAdminTable() {
    const tbody = document.getElementById('admin-tickets-body');
    if (!tbody) return;

    const tickets = window.campusStore.getTickets(adminFilter);

    if (tickets.length === 0) {
      tbody.innerHTML = `
        <tr>
          <td colspan="7" style="text-align:center; padding:3rem; color:var(--text-muted);">
            <div style="font-size:2rem; margin-bottom:0.5rem;">🔍</div>
            <div style="font-weight:600;">No matching tickets found in queue</div>
          </td>
        </tr>
      `;
      return;
    }

    tbody.innerHTML = tickets.map(t => `
      <tr>
        <td class="ticket-id-cell">${t.id}</td>
        <td>
          <div style="font-weight:600; color:var(--text-main);">${t.title}</div>
          <div style="font-size:0.8rem; color:var(--text-dim);">By: ${t.submittedBy.name} (${t.submittedBy.studentId}) • 📍 ${t.location}</div>
        </td>
        <td>
          <span style="font-size:0.85rem; color:var(--text-muted);">${t.category}</span>
        </td>
        <td>${getPriorityBadge(t.priority)}</td>
        <td>${getStatusBadge(t.status)}</td>
        <td>
          <span style="font-size:0.85rem; color:#38bdf8; font-weight:500;">
            ${t.assignedTo || 'Unassigned'}
          </span>
        </td>
        <td style="text-align:right;">
          <div style="display:flex; gap:0.4rem; justify-content:flex-end;">
            <button class="btn btn-secondary btn-sm" onclick="viewTicketDetails('${t.id}')">View</button>
            <button class="btn btn-primary btn-sm" onclick="openActionModal('${t.id}')">Manage</button>
          </div>
        </td>
      </tr>
    `).join('');
  }

  // Admin filter inputs
  const searchInput = document.getElementById('admin-search');
  if (searchInput) {
    searchInput.addEventListener('input', (e) => {
      adminFilter.search = e.target.value;
      renderAdminTable();
    });
  }

  const statusSelect = document.getElementById('admin-status-filter');
  if (statusSelect) {
    statusSelect.addEventListener('change', (e) => {
      adminFilter.status = e.target.value;
      renderAdminTable();
    });
  }

  const catSelect = document.getElementById('admin-category-filter');
  if (catSelect) {
    catSelect.addEventListener('change', (e) => {
      adminFilter.category = e.target.value;
      renderAdminTable();
    });
  }

  // Sample data reset helper
  const resetBtn = document.getElementById('btn-reset-data');
  if (resetBtn) {
    resetBtn.addEventListener('click', () => {
      if (confirm('Reset system data to initial college demo complaints?')) {
        window.campusStore.resetToSampleData();
        showToast('Demo data restored successfully', 'success');
        updateAdminKPIs();
        renderAdminTable();
      }
    });
  }

  updateAdminKPIs();
  renderAdminTable();
}

// Admin Action Modal
window.openActionModal = function(ticketId) {
  const ticket = window.campusStore.getTicketById(ticketId);
  if (!ticket) return;

  document.getElementById('action-ticket-id').value = ticket.id;
  document.getElementById('action-modal-title').textContent = `Manage Ticket: ${ticket.id}`;
  document.getElementById('action-status-select').value = ticket.status;
  document.getElementById('action-assign-select').value = ticket.assignedTo || '';
  document.getElementById('action-remark').value = '';

  openModal('modal-manage-ticket');
};

// Setup Action Submit
function initAdminActionModal() {
  const form = document.getElementById('manage-ticket-form');
  if (form) {
    form.addEventListener('submit', (e) => {
      e.preventDefault();
      const ticketId = document.getElementById('action-ticket-id').value;
      const newStatus = document.getElementById('action-status-select').value;
      const newAssignee = document.getElementById('action-assign-select').value;
      const remark = document.getElementById('action-remark').value;

      if (newAssignee) {
        window.campusStore.assignTicket(ticketId, newAssignee);
      }
      if (newStatus) {
        window.campusStore.updateTicketStatus(ticketId, newStatus, remark);
      }

      showToast(`Ticket ${ticketId} updated successfully`, 'success');
      closeModal('modal-manage-ticket');

      // Refresh admin view
      if (typeof initAdminDashboard === 'function') {
        initAdminDashboard();
      }
    });
  }
}

// Auto-run page initializers on DOMContentLoaded
document.addEventListener('DOMContentLoaded', () => {
  initNavbar();
  
  if (document.getElementById('quick-track-form')) {
    initLandingPage();
  }
  if (document.getElementById('login-form')) {
    initAuthPage();
  }
  if (document.getElementById('new-ticket-form')) {
    initUserDashboard();
  }
  if (document.getElementById('admin-tickets-body')) {
    initAdminDashboard();
    initAdminActionModal();
  }
});
