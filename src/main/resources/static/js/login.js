import { safeJson } from './utils/api.js';

const AUTH_TEMPLATE = `
    <div class="auth-shell" data-auth-root>
        <div class="container d-flex justify-content-center align-items-center" style="min-height: 100vh;">
            <div class="card p-4 shadow-sm auth-card" style="max-width: 460px; width: 100%;">
                <h3 class="fw-bold mb-2">Campus Connect</h3>
                <p class="text-muted">Sign in or create an account to access your dashboard.</p>

                <fieldset class="btn-group w-100 auth-toggle mt-2" aria-label="Authentication toggle">
                    <legend class="visually-hidden">Authentication toggle</legend>
                    <button type="button" class="btn btn-outline-primary active" data-auth-toggle="login">Login</button>
                    <button type="button" class="btn btn-outline-primary" data-auth-toggle="register">Register</button>
                </fieldset>

                <form id="loginForm" class="mt-3 auth-view" data-auth-view="login">
                    <div class="mb-3">
                        <label for="email" class="form-label">Email or Username</label>
                        <input type="text" class="form-control" id="email" name="email" required>
                    </div>
                    <div class="mb-3">
                        <label for="password" class="form-label">Password</label>
                        <input type="password" class="form-control" id="password" name="password" required>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Login</button>
                    <div id="loginStatus" class="small text-danger mt-2"></div>
                </form>

                <form id="registerForm" class="mt-3 auth-view d-none" data-auth-view="register">
                    <div class="mb-3">
                        <label for="registerUsername" class="form-label">Username</label>
                        <input type="text" class="form-control" id="registerUsername" name="username" required>
                        <div id="usernameStatus" class="small mt-1 text-muted"></div>
                    </div>
                    <div class="mb-3">
                        <label for="registerEmail" class="form-label">Email</label>
                        <input type="email" class="form-control" id="registerEmail" name="email" required>
                        <div class="small text-muted">Use @student.tus.com or @admin.tus.com.</div>
                        <div id="roleHint" class="small mt-1 text-muted"></div>
                    </div>
                    <div class="mb-3">
                        <label for="registerPassword" class="form-label">Password</label>
                        <input type="password" class="form-control" id="registerPassword" name="password" required>
                        <div class="small text-muted">Minimum 5 characters.</div>
                    </div>
                    <button type="submit" class="btn btn-primary w-100">Create Account</button>
                    <div id="registerStatus" class="small text-danger mt-2"></div>
                </form>
            </div>
        </div>
    </div>
`;

export function renderAuthView(options = {}) {
    const appRoot = document.getElementById('app-root');
    if (!appRoot) {
        return;
    }
    appRoot.innerHTML = AUTH_TEMPLATE;
    initAuthView(options);
}

export function initAuthView(options = {}) {
    const root = document.querySelector('[data-auth-root]') ?? document;
    const loginForm = root.querySelector('#loginForm');
    const registerForm = root.querySelector('#registerForm');
    if (!loginForm || !registerForm) {
        return;
    }
    if (loginForm.dataset.bound === 'true') {
        return;
    }
    loginForm.dataset.bound = 'true';
    registerForm.dataset.bound = 'true';

    const loginStatus = root.querySelector('#loginStatus');
    const registerStatus = root.querySelector('#registerStatus');
    const toggleButtons = root.querySelectorAll('[data-auth-toggle]');
    const views = root.querySelectorAll('[data-auth-view]');
    const usernameInput = root.querySelector('#registerUsername');
    const emailInput = root.querySelector('#registerEmail');
    const passwordInput = root.querySelector('#registerPassword');
    const usernameStatus = root.querySelector('#usernameStatus');
    const roleHint = root.querySelector('#roleHint');

    const { onAuthSuccess } = options;

    const setStatus = (el, message, isSuccess = false) => {
        if (!el) {
            return;
        }
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

    const readFormValue = (value) => (typeof value === 'string' ? value : '');

    const checkUsernameAvailability = async (username) => {
        if (!usernameStatus) {
            return;
        }
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
            console.warn('Username availability check failed.', error);
            usernameStatus.textContent = 'Unable to check username.';
            usernameStatus.className = 'small mt-1 text-danger';
        }
    };

    const completeAuth = () => {
        if (typeof onAuthSuccess === 'function') {
            onAuthSuccess();
            return;
        }
        globalThis.location.href = '/';
    };

    const handleLogin = async (event) => {
        event.preventDefault();
        setStatus(loginStatus, '');

        const formData = new FormData(loginForm);
        const emailValue = readFormValue(formData.get('email')).trim();
        const passwordValue = readFormValue(formData.get('password'));
        const payload = {
            email: emailValue,
            password: passwordValue
        };

        try {
            const response = await fetch('/api/auth/login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });

            const data = await safeJson(response);
            if (!response.ok) {
                let message = 'Wrong email/password combo.';
                if (response.status !== 401 && response.status !== 403) {
                    const apiMessage = data?.message;
                    message = apiMessage || 'Login failed. Please try again.';
                }
                setStatus(loginStatus, message);
                return;
            }

            if (!data.token) {
                setStatus(loginStatus, 'Login succeeded but no token was returned.');
                return;
            }

            localStorage.setItem('cc.token', data.token);
            localStorage.setItem('cc.role', data.role || 'STUDENT');
            setStatus(loginStatus, data?.message || 'Login successful.', true);
            setTimeout(completeAuth, 1200);
        } catch (error) {
            console.warn('Login request failed.', error);
            setStatus(loginStatus, 'Login service unavailable.');
        }
    };

    const handleRegister = async (event) => {
        event.preventDefault();
        setStatus(registerStatus, '');

        const payload = {
            username: readFormValue(usernameInput?.value).trim(),
            email: readFormValue(emailInput?.value).trim(),
            password: readFormValue(passwordInput?.value)
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

            const data = await safeJson(response);
            if (!response.ok) {
                setStatus(registerStatus, data?.message || 'Registration failed.');
                return;
            }

            setStatus(registerStatus, data?.message || 'Registration successful.', true);
            const loginEmail = loginForm.querySelector('#email');
            if (loginEmail) {
                loginEmail.value = payload.email;
            }
            setActiveView('login');
        } catch (error) {
            console.warn('Registration request failed.', error);
            setStatus(registerStatus, 'Registration service unavailable.');
        }
    };

    toggleButtons.forEach((btn) => {
        btn.addEventListener('click', () => setActiveView(btn.dataset.authToggle));
    });

    loginForm.addEventListener('submit', handleLogin);
    registerForm.addEventListener('submit', handleRegister);

    let usernameTimer = null;
    if (usernameInput) {
        usernameInput.addEventListener('input', () => {
            clearTimeout(usernameTimer);
            usernameTimer = setTimeout(() => checkUsernameAvailability(usernameInput.value), 300);
        });
    }

    if (emailInput && roleHint) {
        emailInput.addEventListener('input', () => {
            roleHint.textContent = deriveRoleHint(emailInput.value);
            const isRole = roleHint.textContent.startsWith('Role');
            const isError = roleHint.textContent.startsWith('Email must');
            let roleClass = 'small mt-1 text-muted';
            if (isRole) {
                roleClass = 'small mt-1 text-success';
            } else if (isError) {
                roleClass = 'small mt-1 text-danger';
            }
            roleHint.className = roleClass;
        });
    }

    setActiveView('login');
}
