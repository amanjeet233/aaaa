document.addEventListener('DOMContentLoaded', () => {
  const form = document.getElementById('loginForm');
  const msg = document.getElementById('loginMsg');

  form.addEventListener('submit', async (e) => {
    e.preventDefault();
    msg.textContent = '';
    const username = document.getElementById('username').value.trim();
    const password = document.getElementById('password').value.trim();

    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: {'Content-Type':'application/json'},
        body: JSON.stringify({ username, password })
      });
      const data = await res.json();
      if (!res.ok) {
        msg.textContent = data.message || 'Login failed';
        return;
      }

  localStorage.setItem('token', data.token);
  // normalize role to lowercase for consistent checks across UI
  const roleNormalized = data.role ? String(data.role).toLowerCase() : '';
  localStorage.setItem('role', roleNormalized);
      localStorage.setItem('empId', data.empId);
      localStorage.setItem('name', data.name);
      // Fetch authenticated profile (/api/auth/me) to get empId, userId, email and name
      try {
        const profileRes = await fetch('/api/auth/me', { headers: { 'Authorization': `Bearer ${data.token}` } });
        if (profileRes.ok) {
          const profile = await profileRes.json();
          if (profile.empId) localStorage.setItem('empId', profile.empId);
          if (profile.userId) localStorage.setItem('userId', String(profile.userId));
          if (profile.email) localStorage.setItem('email', profile.email);
          if (profile.name) localStorage.setItem('name', profile.name);
        }
      } catch (e) { /* ignore */ }

      if (roleNormalized === 'manager') location.href = 'dashboard.html';
      else location.href = 'employee.html';
    } catch (err) {
      msg.textContent = 'Network error';
    }
  });
});
