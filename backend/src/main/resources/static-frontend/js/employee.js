const tokenE = localStorage.getItem('token');
if (!tokenE) location.href = 'index.html';
document.getElementById('empName').textContent = localStorage.getItem('name') || '';
document.getElementById('empIdView').textContent = localStorage.getItem('empId') || '';
document.getElementById('empEmailView').textContent = localStorage.getItem('email') || localStorage.getItem('empId');
document.getElementById('empDeptView').textContent = localStorage.getItem('department') || '';

const API_HEADERS = { 'Content-Type': 'application/json', 'Authorization': `Bearer ${tokenE}` };

async function fetchJSON(path, opts = {}) {
	opts.headers = Object.assign({}, API_HEADERS, opts.headers || {});
	const res = await fetch(path, opts);
	if (res.status === 401) { localStorage.clear(); location.href = 'index.html'; return null; }
	return res.json();
}

async function loadEmployeeData() {
	const empId = localStorage.getItem('empId');
	if (!empId) return;

	// fetch leave balance
	try {
		const balance = await fetchJSON(`/api/leaves/balance/${empId}`);
		if (balance) {
			document.getElementById('leaveTotal').textContent = balance.totalLeaves ?? '-';
			document.getElementById('casualBal').textContent = balance.casual ?? '-';
			document.getElementById('sickBal').textContent = balance.sick ?? '-';
		}
	} catch (e) { /* ignore */ }

	// load leave history
	try {
		const leaves = await fetchJSON(`/api/leaves/employee/${empId}`);
		const tbody = document.querySelector('#myLeaveTable tbody');
		if (!leaves || leaves.length === 0) {
			tbody.innerHTML = '<tr><td colspan="7">No leave history</td></tr>';
		} else {
			tbody.innerHTML = leaves.map((l,i)=>`<tr>
				<td>${i+1}</td>
				<td>${l.leaveType}</td>
				<td>${formatDate(l.fromDate)}</td>
				<td>${formatDate(l.toDate)}</td>
				<td>${l.days || ''}</td>
				<td>${l.status}</td>
				<td>${l.managerComment || ''}</td>
			</tr>`).join('');
		}
	} catch (e) { console.error(e); }

	// Check today's attendance
	try {
		const today = new Date().toISOString().split('T')[0];
		const attendance = await fetchJSON(`/api/attendance/date/${today}`);
		if (attendance) {
			const my = attendance.find(a => a.employeeId === empId || a.employee?.empId === empId);
			if (my) {
				document.getElementById('todayStatus').textContent = my.status;
				document.getElementById('todayIn').textContent = my.inTime || '-';
			} else {
				document.getElementById('todayStatus').textContent = 'ABSENT';
				document.getElementById('todayIn').textContent = '-';
			}
		}
	} catch (e) { console.error(e); }
}

// Apply leave
document.getElementById('applyLeaveBtn')?.addEventListener('click', async () => {
	const empId = localStorage.getItem('empId');
	const leaveType = document.getElementById('leaveType').value;
	const fromDate = document.getElementById('fromDate').value;
	const toDate = document.getElementById('toDate').value;
	const reason = document.getElementById('leaveReason').value.trim();
	const msg = document.getElementById('applyMsg'); msg.textContent = '';
	if (!fromDate || !toDate) { msg.textContent = 'Select from/to dates'; return; }

	try {
		const res = await fetch('/api/leaves/apply', {
			method: 'POST',
			headers: API_HEADERS,
			body: JSON.stringify({ employeeId: empId, leaveType, fromDate, toDate, reason })
		});
		if (!res.ok) {
			const data = await res.json();
			msg.textContent = data?.error || 'Failed to apply leave';
			return;
		}
		msg.textContent = 'Leave applied';
		loadEmployeeData();
	} catch (e) { msg.textContent = 'Network error'; }
});

function logout(){ localStorage.clear(); window.location.href='index.html'; }

// init
document.addEventListener('DOMContentLoaded', () => {
	loadEmployeeData();
});
