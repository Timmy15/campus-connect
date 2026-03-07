const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');
const loginStatus = document.getElementById('loginStatus');
const registerStatus = document.getElementById('registerStatus');
const toggleButtons = document.querySelectorAll('[data-auth-toggle]');
const views = document.querySelectorAll('[data-auth-view]');
const usernameInput = document.getElementById('registerUsername');
const emailInput = document.getElementById('registerEmail');
const passwordInput = document.getElementById('registerPassword');
const usernameStatus = document.getElementById('usernameStatus');
const roleHint = document.getElementById('roleHint');

const setStatus = (el, message, isSuccess = false) => {
    el.textContent = message || '';
    if (!message) {
        el.classList.remove('text-success', 'text-danger');
        return;
    }
    el.classList.toggle('text-success', isSuccess);
    el.classList.toggle('text-danger', !isSuccess);
};

const setActiveView = (view) => {
    views.forEach((section) => {
        section.classList.toggle('d-none', section.dataset.authView !== view);
    });
    toggleButtons.forEach((btn) => {
        btn.classList.toggle('active', btn.dataset.authToggle === view);
    });
    setStatus(loginStatus, '');
    setStatus(registerStatus, '');
};

const deriveRoleHint = (email) => {
    const value = (email || '').toLowerCase().trim();
    if (value.endsWith('@admin.tus.com')) return 'Role: ADMIN';
    if (value.endsWith('@student.tus.com')) return 'Role: STUDENT';
    if (!value) return '';
    return 'Email must end with @student.tus.com or @admin.tus.com.';
};

const checkUsernameAvailability = async (username) => {
    const value = String(username || '').trim();
    if (!value) {
        usernameStatus.textContent = '';
        return;
    }

    try {
        const response = await fetch(`/api/auth/username-available?username=${encodeURIComponent(value)}`);
        const data = await response.json();
        if (!response.ok) {
            usernameStatus.textContent = data?.message || 'Unable to check username.';
            usernameStatus.className = 'small mt-1 text-danger';
            return;
        }
        usernameStatus.textContent = data.message;
        usernameStatus.className = `small mt-1 ${data.available ? 'text-success' : 'text-danger'}`;
    } catch (error) {
        usernameStatus.textContent = 'Unable to check username.';
        usernameStatus.className = 'small mt-1 text-danger';
    }
};

const handleLogin = async (event) => {
    event.preventDefault();
    setStatus(loginStatus, '');

    const formData = new FormData(loginForm);
    const payload = {
        email: String(formData.get('email') || '').trim(),
        password: String(formData.get('password') || '')
    };

    try {
        const response = await fetch('/api/auth/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            let message = 'Wrong email/password combo.';
            if (response.status !== 401 && response.status !== 403) {
                try {
                    const data = await response.json();
                    if (data && data.message) message = data.message;
                } catch (error) {
                    message = 'Login failed. Please try again.';
                }
            }
            setStatus(loginStatus, message);
            return;
        }

        const data = await response.json();
        if (!data.token) {
            setStatus(loginStatus, 'Login succeeded but no token was returned.');
            return;
        }

        localStorage.setItem('cc.token', data.token);
        localStorage.setItem('cc.role', data.role || 'STUDENT');
        setStatus(loginStatus, data.message || 'Login successful.', true);
        setTimeout(() => {
            globalThis.location.href = '/';
        }, 1200);
    } catch (error) {
        setStatus(loginStatus, 'Login service unavailable.');
    }
};

const handleRegister = async (event) => {
    event.preventDefault();
    setStatus(registerStatus, '');

    const payload = {
        username: String(usernameInput.value || '').trim(),
        email: String(emailInput.value || '').trim(),
        password: String(passwordInput.value || '')
    };

    if (!payload.username || !payload.email || !payload.password) {
        setStatus(registerStatus, 'All fields are required.');
        return;
    }

    if (payload.password.length < 5) {
        setStatus(registerStatus, 'Password must be at least 5 characters.');
        return;
    }

    const roleMessage = deriveRoleHint(payload.email);
    if (roleMessage.startsWith('Email must')) {
        setStatus(registerStatus, roleMessage);
        return;
    }

    try {
        const response = await fetch('/api/auth/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });

        const data = await response.json();
        if (!response.ok) {
            setStatus(registerStatus, data?.message || 'Registration failed.');
            return;
        }

        setStatus(registerStatus, data.message || 'Registration successful.', true);
        loginForm.querySelector('#email').value = payload.email;
        setActiveView('login');
    } catch (error) {
        setStatus(registerStatus, 'Registration service unavailable.');
    }
};

toggleButtons.forEach((btn) => {
    btn.addEventListener('click', () => setActiveView(btn.dataset.authToggle));
});

loginForm.addEventListener('submit', handleLogin);
registerForm.addEventListener('submit', handleRegister);

let usernameTimer = null;
usernameInput.addEventListener('input', () => {
    clearTimeout(usernameTimer);
    usernameTimer = setTimeout(() => checkUsernameAvailability(usernameInput.value), 300);
});

emailInput.addEventListener('input', () => {
    roleHint.textContent = deriveRoleHint(emailInput.value);
    const isRole = roleHint.textContent.startsWith('Role');
    const isError = roleHint.textContent.startsWith('Email must');
    roleHint.className = `small mt-1 ${isRole ? 'text-success' : isError ? 'text-danger' : 'text-muted'}`;
});

setActiveView('login');
