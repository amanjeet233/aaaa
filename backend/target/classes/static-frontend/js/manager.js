const token = localStorage.getItem('token');
const role = localStorage.getItem('role');
if (!token || !role || (String(role).toLowerCase() !== 'manager')) {
  // redirect to login if not manager
  // allow admin or manager keywords if needed in future
  window.location.href = 'index.html';
}

const headers = {'Content-Type':'application/json', 'Authorization': `Bearer ${token}`};

async function fetchJSON(path, opts = {}) {
  opts.headers = Object.assign({}, headers, opts.headers || {});
  const res = await fetch(path, opts);
  if (res.status === 401) { alert('Unauthorized'); window.location.href='index.html'; return null; }
  return res.json();
}

/* load employees list */
async function loadEmployees() {
  const tbody = document.querySelector('#empTable tbody');
  tbody.innerHTML = '<tr><td colspan="7">Loading...</td></tr>';
  const arr = await fetchJSON('/api/employees');
  if (!arr) return;
  if (!arr.length) { tbody.innerHTML = '<tr><td colspan="7">No employees</td></tr>'; return; }
  tbody.innerHTML = arr.map((e,i)=>`
    <tr>
      <td>${i+1}</td>
      <td>${e.empId}</td>
      <td>${e.name}</td>
      <td>${e.email}</td>
      <td>${e.department || ''}</td>
      <td>${e.role}</td>
      <td>
        <button onclick="editEmp('${e.empId}')">Edit</button>
        <button onclick="deleteEmp('${e.empId}')">Delete</button>
        <button onclick="openEmployeeDashboard('${e.empId}')">Open Dashboard</button>
      </td>
    </tr>
  `).join('');
  document.getElementById('totalEmployees').textContent = arr.length;
  // compute departments from employee list
  const depts = new Set(arr.map(e => e.department).filter(Boolean));
  const deptEl = document.getElementById('totalDepartments');
  if (deptEl) deptEl.textContent = depts.size || 0;
  
  // Also populate employee filter for attendance view
  const select = document.getElementById('employeeFilter');
  if (select) {
    // Keep the first "All Employees" option
    select.innerHTML = '<option value="">All Employees</option>';
    arr.forEach(e => {
      const option = document.createElement('option');
      option.value = e.empId;
      option.textContent = `${e.empId} - ${e.name}`;
      select.appendChild(option);
    });
  }
}

/* show add form */
document.getElementById('showAdd').addEventListener('click', ()=> {
  document.getElementById('addForm').classList.remove('hidden');
  document.getElementById('addMsg').textContent = '';
  // clear fields
  ['empId','name','email','password','dept'].forEach(id => document.getElementById(id).value = '');
  document.getElementById('role').value = 'employee';
});

/* cancel add */
document.getElementById('cancelAdd').addEventListener('click', ()=> {
  document.getElementById('addForm').classList.add('hidden');
});

/* save (add or update) */
let editingId = null;
document.getElementById('saveEmp').addEventListener('click', async ()=> {
  const empId = document.getElementById('empId').value.trim();
  const name = document.getElementById('name').value.trim();
  const email = document.getElementById('email').value.trim();
  const password = document.getElementById('password').value.trim();
  const department = document.getElementById('dept').value.trim();
  const roleSel = document.getElementById('role').value;
  const msg = document.getElementById('addMsg'); msg.textContent = '';

  if (!empId || !name || !email) { msg.textContent = 'Please fill EmpID, Name, Email'; return; }

  const payload = { empId, name, email, password, department, role: roleSel };

  if (!editingId) {
    // create
    const res = await fetch('/api/employees', { method:'POST', headers, body: JSON.stringify(payload) });
    if (res.status === 409) { msg.textContent = 'Employee exists'; return; }
    if (!res.ok) { msg.textContent = 'Failed to create'; return; }
    msg.textContent = 'Created';
  } else {
    // update
    const res = await fetch(`/api/employees/${editingId}`, { method:'PUT', headers, body: JSON.stringify(payload) });
    if (!res.ok) { msg.textContent = 'Update failed'; return; }
    msg.textContent = 'Updated';
    editingId = null;
  }
  document.getElementById('addForm').classList.add('hidden');
  loadEmployees();
});

/* edit function */
window.editEmp = async function(empId) {
  editingId = empId;
  const res = await fetch(`/api/employees/${empId}`, { headers });
  if (!res.ok) { alert('Not found'); return; }
  const e = await res.json();
  document.getElementById('empId').value = e.empId;
  document.getElementById('name').value = e.name;
  document.getElementById('email').value = e.email;
  document.getElementById('password').value = ''; // no show
  document.getElementById('dept').value = e.department || '';
  document.getElementById('role').value = e.role || 'employee';
  document.getElementById('addForm').classList.remove('hidden');
};

// Open employee portal for a given empId in a new tab (static frontend)
window.openEmployeeDashboard = function(empId) {
  // construct URL relative to the static frontend folder
  const url = `/static-frontend/employee-portal/index.html?empId=${encodeURIComponent(empId)}`;
  window.open(url, '_blank');
}

/* delete function */
window.deleteEmp = async function(empId) {
  if (!confirm('Delete ' + empId + '?')) return;
  const res = await fetch(`/api/employees/${empId}`, { method:'DELETE', headers });
  if (!res.ok) { alert('Delete failed'); return; }
  loadEmployees();
};

/* LEAVE MANAGEMENT */
async function loadLeaves() {
  const tbody = document.querySelector('#leaveTable tbody');
  if (!tbody) return; // Skip if element doesn't exist
  
  tbody.innerHTML = '<tr><td colspan="9">Loading...</td></tr>';
  const leaves = await fetchJSON('/api/leaves');
  if (!leaves) return;
  if (!leaves.length) { 
    tbody.innerHTML = '<tr><td colspan="9">No leave requests</td></tr>'; 
    document.getElementById('pendingLeaves').textContent = '0';
    return; 
  }
  
  const pendingCount = leaves.filter(leave => leave.status === 'PENDING').length;
  document.getElementById('pendingLeaves').textContent = pendingCount;
  // set leave summary tiles (if present)
  const applied = leaves.length;
  const approved = leaves.filter(l => l.status === 'APPROVED').length;
  const rejected = leaves.filter(l => l.status === 'REJECTED' || l.status === 'REJECTED').length;
  const appliedEl = document.getElementById('leaveApplied'); if (appliedEl) appliedEl.textContent = applied;
  const pendingEl = document.getElementById('leavePending'); if (pendingEl) pendingEl.textContent = pendingCount;
  const approvedEl = document.getElementById('leaveApproved'); if (approvedEl) approvedEl.textContent = approved;
  const rejectedEl = document.getElementById('leaveRejected'); if (rejectedEl) rejectedEl.textContent = rejected;
  
  tbody.innerHTML = leaves.map((leave, i) => `
    <tr>
      <td>${i+1}</td>
      <td>${leave.employee ? leave.employee.name : leave.employeeId}</td>
      <td>${leave.leaveType}</td>
      <td>${formatDate(leave.fromDate)}</td>
      <td>${formatDate(leave.toDate)}</td>
      <td>${leave.days}</td>
      <td>${leave.reason}</td>
      <td><span class="status-${leave.status.toLowerCase()}">${leave.status}</span></td>
      <td>
        ${leave.status === 'PENDING' ? `
          <button onclick="updateLeaveStatus('${leave.id}', 'APPROVED')">Approve</button>
          <button onclick="updateLeaveStatus('${leave.id}', 'REJECTED')">Reject</button>
        ` : ''}
      </td>
    </tr>
  `).join('');
}

window.updateLeaveStatus = async function(leaveId, status) {
  const res = await fetch(`/api/leaves/${leaveId}/status`, {
    method: 'PUT',
    headers,
    body: JSON.stringify({ status })
  });
  
  if (!res.ok) {
    alert('Failed to update leave status');
    return;
  }
  
  loadLeaves();
};

/* ATTENDANCE MANAGEMENT */
// Set today's date as default
document.addEventListener('DOMContentLoaded', () => {
  const dateInput = document.getElementById('attendanceDate');
  if (dateInput) {
    const today = new Date().toISOString().split('T')[0];
    dateInput.value = today;
    
    // Set current month for attendance filter
    const monthInput = document.getElementById('monthFilter');
    if (monthInput) {
      monthInput.value = today.substring(0, 7);
    }
  }
});

// Tab switching
document.addEventListener('click', e => {
  if (e.target.classList.contains('tab-btn')) {
    const tabId = e.target.dataset.tab;
    
    // Update active tab button
    document.querySelectorAll('.tab-btn').forEach(btn => {
      btn.classList.remove('active');
    });
    e.target.classList.add('active');
    
    // Show selected tab content
    document.querySelectorAll('.tab-content').forEach(content => {
      content.classList.remove('active');
    });
    document.getElementById(tabId).classList.add('active');
    
    // Load appropriate data
    if (tabId === 'markAttendance') {
      loadAttendanceForDate();
    } else if (tabId === 'viewAttendance') {
      loadAttendanceReport();
    }
  }
});

// Load attendance for specific date
document.getElementById('loadAttendance')?.addEventListener('click', loadAttendanceForDate);

async function loadAttendanceForDate() {
  const date = document.getElementById('attendanceDate')?.value || new Date().toISOString().split('T')[0];
  if (!date) {
    alert('Please select a date');
    return;
  }
  
  const tbody = document.querySelector('#markAttendanceTable tbody');
  tbody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
  
  try {
    // First get all employees
    const employees = await fetchJSON('/api/employees');
    if (!employees || !employees.length) {
      tbody.innerHTML = '<tr><td colspan="5">No employees found</td></tr>';
      return;
    }
    
    // Then get attendance for the selected date
    const attendance = await fetchJSON(`/api/attendance/date/${date}`);
    
    // Map employees with their attendance status
    tbody.innerHTML = employees.map((emp, i) => {
      const empAttendance = attendance?.find(a => a.employeeId === emp.empId || a.employee?.empId === emp.empId);
      const status = empAttendance ? empAttendance.status : 'ABSENT';

      return `
        <tr>
          <td>${i+1}</td>
          <td>${emp.name}</td>
          <td>${emp.empId}</td>
          <td>${emp.department || ''}</td>
          <td>
            <button onclick="markPresent('${emp.empId}','${date}')">Present</button>
            <button onclick="markAbsent('${emp.empId}','${date}')">Absent</button>
          </td>
        </tr>
      `;
    }).join('');
  } catch (err) {
    console.error('Error loading attendance:', err);
    tbody.innerHTML = '<tr><td colspan="5">Error loading attendance data</td></tr>';
  }
}

window.markPresent = async function(empId, date) {
  try {
    const res = await fetch('/api/attendance', {
      method: 'POST',
      headers,
      body: JSON.stringify({ employeeId: empId, date: date, status: 'PRESENT', inTime: new Date().toTimeString().split(' ')[0] })
    });
    if (!res.ok) throw new Error('Failed');
    loadAttendanceForDate();
  } catch (e) { alert('Failed to mark present'); }
}

window.markAbsent = async function(empId, date) {
  try {
    const res = await fetch('/api/attendance', {
      method: 'POST',
      headers,
      body: JSON.stringify({ employeeId: empId, date: date, status: 'ABSENT' })
    });
    if (!res.ok) throw new Error('Failed');
    loadAttendanceForDate();
  } catch (e) { alert('Failed to mark absent'); }
}

// Save attendance
document.getElementById('saveAttendance')?.addEventListener('click', async () => {
  const date = document.getElementById('attendanceDate').value;
  if (!date) {
    alert('Please select a date');
    return;
  }
  
  const attendanceRecords = [];
  const rows = document.querySelectorAll('#markAttendanceTable tbody tr');
  
  rows.forEach(row => {
    const empId = row.querySelector('td:nth-child(2)').textContent;
    const statusSelect = document.getElementById(`status-${empId}`);
    const inTimeInput = document.getElementById(`inTime-${empId}`);
    const outTimeInput = document.getElementById(`outTime-${empId}`);
    
    if (statusSelect && inTimeInput && outTimeInput) {
      attendanceRecords.push({
        employeeId: empId,
        date: date,
        status: statusSelect.value,
        inTime: inTimeInput.value,
        outTime: outTimeInput.value
      });
    }
  });
  
  if (attendanceRecords.length === 0) {
    alert('No attendance records to save');
    return;
  }
  
  try {
    const res = await fetch('/api/attendance/bulk', {
      method: 'POST',
      headers,
      body: JSON.stringify(attendanceRecords)
    });
    
    if (!res.ok) {
      throw new Error('Failed to save attendance');
    }
    
    alert('Attendance saved successfully');
  } catch (err) {
    console.error('Error saving attendance:', err);
    alert('Failed to save attendance: ' + err.message);
  }
});

// Filter attendance report
document.getElementById('filterAttendance')?.addEventListener('click', loadAttendanceReport);

async function loadAttendanceReport() {
  const month = document.getElementById('monthFilter').value;
  const employeeId = document.getElementById('employeeFilter').value;
  
  if (!month) {
    alert('Please select a month');
    return;
  }
  
  const tbody = document.querySelector('#attendanceReportTable tbody');
  tbody.innerHTML = '<tr><td colspan="5">Loading...</td></tr>';
  
  try {
    let url = `/api/attendance/month/${month}`;
    if (employeeId) {
      url += `?employeeId=${employeeId}`;
    }
    
    const attendance = await fetchJSON(url);
    
    if (!attendance || !attendance.length) {
      tbody.innerHTML = '<tr><td colspan="5">No attendance records found</td></tr>';
      return;
    }
    
    tbody.innerHTML = attendance.map(record => `
      <tr>
        <td>${formatDate(record.date)}</td>
        <td>${record.employeeName || record.employeeId}</td>
        <td><span class="status-${record.status.toLowerCase()}">${record.status}</span></td>
        <td>${record.inTime || 'N/A'}</td>
        <td>${record.outTime || 'N/A'}</td>
      </tr>
    `).join('');
  } catch (err) {
    console.error('Error loading attendance report:', err);
    tbody.innerHTML = '<tr><td colspan="5">Error loading attendance report</td></tr>';
  }
}

// Helper function to format dates
function formatDate(dateString) {
  if (!dateString) return 'N/A';
  const date = new Date(dateString);
  return date.toLocaleDateString();
}

function logout(){ localStorage.clear(); window.location.href='index.html'; }

/* init */
document.addEventListener('DOMContentLoaded', () => {
  loadEmployees();
  loadLeaves();
  
  // Set date display for attendance
  const today = new Date().toISOString().split('T')[0];
  const attEl = document.getElementById('attDateDisplay'); if (attEl) attEl.textContent = today;
  loadAttendanceForDate();
});
