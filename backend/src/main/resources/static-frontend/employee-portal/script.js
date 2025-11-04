// Helper: get auth headers if token present
function getAuthHeaders() {
  const token = localStorage.getItem('token');
  return token ? { 'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json' } : { 'Content-Type': 'application/json' };
}

function getEmployeeId() {
  // Try common storage keys created by backend auth flow
  const empId = localStorage.getItem('empId') || (localStorage.getItem('user') ? JSON.parse(localStorage.getItem('user')).empId : null) || localStorage.getItem('empId_cached');
  if (!empId) {
    console.warn('No employee id found in localStorage (empId or user). Frontend will use a demo id. Login to enable real data.');
  }
  return empId || '23bcs10270';
}

function getNumericUserId() {
  // Prefer the numeric user id returned by /api/auth/me
  const uid = localStorage.getItem('userId');
  return uid ? uid : null;
}

// Load authenticated profile from backend (uses JWT in Authorization header)
async function loadAuthProfile() {
  const token = localStorage.getItem('token');
  if (!token) return;
  try {
    const res = await fetch('/api/auth/me', { headers: { 'Authorization': 'Bearer ' + token } });
    if (!res.ok) return;
    const profile = await res.json();
    if (profile.empId) localStorage.setItem('empId_cached', profile.empId);
    if (profile.userId) localStorage.setItem('userId', String(profile.userId));
    if (profile.name) localStorage.setItem('name', profile.name);
    if (profile.email) localStorage.setItem('email', profile.email);
  } catch (e) {
    console.error('Failed to load auth profile', e);
  }
}

async function loadDashboardData() {
  // Prefer empId from query param if provided (manager opens portal with ?empId=...)
  const params = new URLSearchParams(window.location.search);
  const paramEmp = params.get('empId');
  const empId = paramEmp || getEmployeeId();
  // Load employee info
  try {
    const headers = getAuthHeaders();
    // Employee endpoint expects empId as string id
  const res = await fetch(`/api/employees/${empId}`, { headers });
    if (res.ok) {
      const emp = await res.json();
      // Populate fields if present
      document.querySelectorAll('.emp-id').forEach(el => el.textContent = emp.empId || empId);
      document.querySelectorAll('.emp-email').forEach(el => el.textContent = emp.email || '');
      document.querySelectorAll('.emp-dept').forEach(el => el.textContent = emp.department || '');
    }
  } catch (e) {
    console.error('Failed to load employee info', e);
  }

  // Load leave summary (try balance endpoint if numeric id available, else fallback to scanning all leaves)
  try {
    const headers = getAuthHeaders();
    let applied = 0, pending = 0, approved = 0, rejected = 0;

    // Try numeric id for balance endpoint
    if (!isNaN(Number(empId))) {
      const resBal = await fetch(`/api/leaves/balance/${empId}`, { headers });
      if (resBal.ok) {
        const bal = await resBal.json();
        document.querySelectorAll('.leave-total').forEach(el => el.textContent = bal.total || 0);
        document.querySelectorAll('.leave-casual').forEach(el => el.textContent = bal.casual || 0);
        document.querySelectorAll('.leave-sick').forEach(el => el.textContent = bal.sick || 0);
      }
    }

    // Get all leaves and compute summary for this employee (safe fallback)
    const resAll = await fetch('/api/leaves', { headers });
    if (resAll.ok) {
      const all = await resAll.json();
      const myLeaves = all.filter(l => (l.employee && (String(l.employee.empId) === String(empId) || String(l.employee.email) === String(empId) || String(l.employee.id) === String(empId))));
      applied = myLeaves.length;
      pending = myLeaves.filter(l => (l.status||'').toUpperCase() === 'PENDING').length;
      approved = myLeaves.filter(l => (l.status||'').toUpperCase() === 'APPROVED').length;
      rejected = myLeaves.filter(l => (l.status||'').toUpperCase() === 'REJECTED').length;

      document.querySelectorAll('.leave-total').forEach(el => el.textContent = applied || 0);
      document.querySelectorAll('.leave-casual').forEach(el => el.textContent = pending || 0); // reuse slots if specific counts not available
      document.querySelectorAll('.leave-sick').forEach(el => el.textContent = approved || 0);

      // If there are dedicated placeholders for applied/pending/approved/rejected, set them
      document.querySelectorAll('#dashLeaveApplied').forEach(el => el.textContent = applied || 0);
      document.querySelectorAll('#dashLeavePending').forEach(el => el.textContent = pending || 0);
      document.querySelectorAll('#dashLeaveApproved').forEach(el => el.textContent = approved || 0);
      document.querySelectorAll('#dashLeaveRejected').forEach(el => el.textContent = rejected || 0);
    }
  } catch (e) {
    console.error('Failed to load leave summary', e);
  }

  // Load today's attendance (date-based)
  try {
    const headers = getAuthHeaders();
    const today = new Date().toISOString().split('T')[0];
    const res = await fetch(`/api/attendance/date/${today}`, { headers });
    if (res.ok) {
      const list = await res.json();
      // Find employee record
      const empIdVal = getEmployeeId();
      const rec = list.find(a => String(a.employeeId) === String(empIdVal) || (a.employee && a.employee.empId === empIdVal));
      if (rec) {
        document.querySelectorAll('.today-status').forEach(el => el.textContent = rec.status || '-');
        document.querySelectorAll('.today-in').forEach(el => el.textContent = rec.inTime || '-');
      }
    }
  } catch (e) {
    console.error('Failed to load today attendance', e);
  }
}

async function markAttendance() {
  const empId = getEmployeeId();
  const today = new Date().toISOString().split('T')[0];
  const payload = { employeeId: empId, date: today, status: 'PRESENT', inTime: new Date().toTimeString().split(' ')[0] };
  try {
    const res = await fetch('/api/attendance', {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload)
    });
    if (!res.ok) throw new Error('Failed to mark attendance');
    alert('Attendance marked');
    loadDashboardData();
  } catch (e) {
    console.error(e);
    alert('Error marking attendance — see console');
  }
}

async function applyLeave() {
  const empId = getEmployeeId();
  const leaveTypeEl = document.getElementById('leaveType');
  const fromEl = document.getElementById('fromDate');
  const toEl = document.getElementById('toDate');
  const reasonEl = document.getElementById('reason');
  const payload = {
    // prefer numeric user id when available (backend expects Long for leaves)
    employeeId: getNumericUserId() || empId,
    leaveType: leaveTypeEl ? leaveTypeEl.value : 'Casual',
    fromDate: fromEl ? fromEl.value : undefined,
    toDate: toEl ? toEl.value : undefined,
    reason: reasonEl ? reasonEl.value : ''
  };

  try {
    if (!getNumericUserId()) {
      alert('Unable to apply leave: numeric user id not available. Please login through the main app to link your account.');
      return;
    }
    const res = await fetch('/api/leaves/apply', {
      method: 'POST',
      headers: getAuthHeaders(),
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      const err = await res.json().catch(() => ({}));
      throw new Error(err.message || 'Failed to apply leave');
    }
    alert('Leave applied');
    // Optionally redirect to leave history
    window.location.href = 'leave-history.html';
  } catch (e) {
    console.error(e);
    alert('Error applying leave — check console');
  }
}

async function loadAttendancePage() {
  const empId = getEmployeeId();
  try {
    const headers = getAuthHeaders();
    // Fetch monthly or all attendance for this employee
  const res = await fetch(`/api/attendance/employee/${empId}`, { headers });
    if (!res.ok) throw new Error('Failed to fetch attendance');
    const list = await res.json();
    const tbody = document.querySelector('.table-wrap table tbody');
    tbody.innerHTML = '';
    let presentCount = 0;
    list.forEach((rec, idx) => {
      const tr = document.createElement('tr');
      tr.innerHTML = `<td>${idx+1}</td><td>${rec.date || rec.attDate || ''}</td><td>${rec.status || ''}</td><td>${rec.inTime || ''}</td><td>${rec.outTime || ''}</td><td>${rec.workedHours || ''}</td>`;
      tbody.appendChild(tr);
      if ((rec.status||'').toUpperCase() === 'PRESENT') presentCount++;
    });
    const percentage = list.length ? Math.round((presentCount / list.length) * 100) : 0;
    document.querySelectorAll('.att-percentage').forEach(el => el.textContent = percentage + '%');
  } catch (e) {
    console.error('Error loading attendance page', e);
  }
}

async function loadLeaveHistoryPage() {
  const empId = getEmployeeId();
  try {
    const headers = getAuthHeaders();
    // GET all leaves and filter client-side by nested employee info (works even if employee mapping uses User entity)
    const res = await fetch(`/api/leaves`, { headers });
    if (!res.ok) throw new Error('Failed to fetch leaves');
    const all = await res.json();
    const list = all.filter(l => l.employee && (String(l.employee.empId) === String(empId) || String(l.employee.email) === String(empId) || String(l.employee.id) === String(empId)));
    const tbody = document.querySelector('.table-wrap table tbody');
    tbody.innerHTML = '';
    list.forEach((l, idx) => {
      const tr = document.createElement('tr');
      const days = l.fromDate && l.toDate ? ( (new Date(l.toDate) - new Date(l.fromDate))/(1000*60*60*24) + 1) : '';
      tr.innerHTML = `<td>${idx+1}</td><td>${l.leaveType}</td><td>${l.fromDate}</td><td>${l.toDate}</td><td>${days}</td><td>${l.status}</td><td>${l.managerComment||l.reason||''}</td>`;
      tbody.appendChild(tr);
    });
  } catch (e) {
    console.error('Error loading leave history', e);
  }
}

// Auto-run loader depending on page
document.addEventListener('DOMContentLoaded', async () => {
  // load authenticated profile first (sets numeric userId if available)
  await loadAuthProfile();
  const path = window.location.pathname.split('/').pop();
  if (path === '' || path === 'index.html') loadDashboardData();
  if (path === 'attendance.html') loadAttendancePage();
  if (path === 'leave-history.html') loadLeaveHistoryPage();
});
