// Check authentication
checkAuth();

document.addEventListener('DOMContentLoaded', () => {
    loadDashboardStats();
    loadRecentLeaves();
    updateWelcomeMessage();
});

const baseUrl = window.location.origin + '/api';

// Update welcome message
function updateWelcomeMessage() {
    const user = JSON.parse(localStorage.getItem('user'));
    if (user) {
        document.getElementById('welcomeMessage').textContent = `Welcome ${user.name || 'Admin'}`;
    }
}

// Function to load dashboard statistics
async function loadDashboardStats() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${baseUrl}/dashboard/stats`, {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        if (!response.ok) throw new Error('Failed to load dashboard stats');

        const stats = await response.json();

        // Update stats in the UI
        document.getElementById('dashTotalEmployees').textContent = stats.totalEmployees || '0';
        document.getElementById('dashTotalDepts').textContent = stats.totalDepartments || '0';
        document.getElementById('dashPresentToday').textContent = stats.presentToday || '0';
        document.getElementById('dashOnLeave').textContent = stats.onLeave || '0';

        // Update leave summary
        document.getElementById('dashLeaveApplied').textContent = stats.leaveStats?.applied || '0';
        document.getElementById('dashLeavePending').textContent = stats.leaveStats?.pending || '0';
        document.getElementById('dashLeaveApproved').textContent = stats.leaveStats?.approved || '0';
        document.getElementById('dashLeaveRejected').textContent = stats.leaveStats?.rejected || '0';
    } catch (error) {
        console.error('Error loading dashboard stats:', error);
    }
}

// Function to load recent leave applications
async function loadRecentLeaves() {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${baseUrl}/leaves/recent`, {
            headers: {
                'Authorization': 'Bearer ' + token
            }
        });

        if (!response.ok) throw new Error('Failed to load recent leaves');

        const leaves = await response.json();
        const tbody = document.querySelector('#dashRecentLeaves tbody');
        tbody.innerHTML = '';

        leaves.forEach(leave => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${leave.employeeName}</td>
                <td>${leave.department}</td>
                <td>${new Date(leave.fromDate).toLocaleDateString()}</td>
                <td>${new Date(leave.toDate).toLocaleDateString()}</td>
                <td>${leave.leaveType}</td>
                <td><span class="status-badge ${leave.status.toLowerCase()}">${leave.status}</span></td>
                <td>
                    ${leave.status === 'PENDING' ? `
                        <button class="btn-action approve" onclick="updateLeaveStatus('${leave.id}', 'APPROVED')">
                            <i class="fas fa-check"></i>
                        </button>
                        <button class="btn-action reject" onclick="updateLeaveStatus('${leave.id}', 'REJECTED')">
                            <i class="fas fa-times"></i>
                        </button>
                    ` : ''}
                </td>
            `;
            tbody.appendChild(row);
        });
    } catch (error) {
        console.error('Error loading recent leaves:', error);
    }
}

// Function to update leave status
async function updateLeaveStatus(leaveId, status) {
    try {
        const token = localStorage.getItem('token');
        const response = await fetch(`${baseUrl}/leaves/${leaveId}/status`, {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': 'Bearer ' + token
            },
            body: JSON.stringify({ status: status })
        });

        if (!response.ok) throw new Error('Failed to update leave status');

        // Reload dashboard data after updating
        loadDashboardStats();
        loadRecentLeaves();
    } catch (error) {
        console.error('Error updating leave status:', error);
        alert('Failed to update leave status');
    }
}

// Auto refresh dashboard data every 5 minutes
setInterval(() => {
    loadDashboardStats();
    loadRecentLeaves();
}, 300000);
    tbody.innerHTML = "";
    employees.forEach((e, i) => {
        tbody.innerHTML += `
        <tr>
            <td>${i + 1}</td>
            <td>${e.empId}</td>
            <td>${e.name}</td>
            <td>${e.email}</td>
            <td>${e.department}</td>
            <td>${e.role}</td>
            <td>
                <button onclick="editEmployee('${e.empId}')">Edit</button>
                <button onclick="deleteEmployee('${e.empId}')">Delete</button>
            </td>
        </tr>`;
    });
}

// 🔹 Add Employee
async function addEmployee() {
    const emp = {
        empId: document.getElementById("empId").value,
        name: document.getElementById("empName").value,
        email: document.getElementById("empEmail").value,
        password: document.getElementById("empPass").value,
        department: document.getElementById("empDept").value,
        role: document.getElementById("empRole").value
    };
    await fetch(`${baseUrl}/employees`, {
        method: "POST",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify(emp)
    });
    loadEmployees();
}

// 🔹 Delete Employee
async function deleteEmployee(empId) {
    await fetch(`${baseUrl}/employees/${empId}`, { method: "DELETE" });
    loadEmployees();
}

// 🔹 Load Leave Requests
async function loadLeaves() {
    const res = await fetch(`${baseUrl}/leaves`);
    const leaves = await res.json();
    const tbody = document.querySelector("#leaveTableBody");
    tbody.innerHTML = "";
    leaves.forEach((l, i) => {
        tbody.innerHTML += `
        <tr>
            <td>${i + 1}</td>
            <td>${l.empId}</td>
            <td>${l.fromDate}</td>
            <td>${l.toDate}</td>
            <td>${l.status}</td>
            <td>
                <button onclick="approveLeave(${l.id})">Approve</button>
                <button onclick="rejectLeave(${l.id})">Reject</button>
            </td>
        </tr>`;
    });
}

async function approveLeave(id) {
    await fetch(`${baseUrl}/leaves/${id}/approve`, { method: "PUT" });
    loadLeaves();
}

async function rejectLeave(id) {
    const remark = prompt("Enter rejection remark:");
    await fetch(`${baseUrl}/leaves/${id}/reject`, {
        method: "PUT",
        headers: {"Content-Type": "application/json"},
        body: JSON.stringify({ remark })
    });
    loadLeaves();
}

// 🔹 Auto load
loadEmployees();
loadLeaves();
