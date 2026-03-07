import { renderAdminDashboard } from './dashboards/adminDashboard.js';
import { renderStudentDashboard } from './dashboards/studentDashboard.js';
import { renderManageClubs } from './features/manageClubs.js';
import { renderManageEvents } from './features/manageEvents.js';
import { renderBrowseClubs } from './features/browseClubs.js';
import { renderBrowseEvents } from './features/browseEvents.js';
import { renderMyRegistrations } from './features/myRegistrations.js';

let currentUser = null;
const clearAuthStorage = () => {
    localStorage.removeItem('cc.token');
    localStorage.removeItem('cc.role');
};

document.addEventListener('DOMContentLoaded', () => {
    initApp();
    initTheme();
});

function initApp() {
    const token = localStorage.getItem('cc.token');
    if (!token) {
        globalThis.location.href = '/login.html';
        return;
    }

    $.ajax({
        url: '/api/user/me',
        method: 'GET',
        headers: {
            Authorization: `Bearer ${token}`
        }
    })
        .done(user => {
            currentUser = user;
            updateHeader(user);
            configureSidebar(user.role);

            const role = user.role?.replaceAll("ROLE_", "") ?? "";

            renderDashboardByRole(role, currentUser);
            setupNavigation();
        })
        .fail(() => {
            clearAuthStorage();
            globalThis.location.href = '/login.html';
        });
}

function renderDashboardByRole(role, user) {
    if (role === 'ADMIN') renderAdminDashboard(user);
    else renderStudentDashboard(user);
}

function updateHeader(user) {
    if (!user) return;
    const friendlyRole = user.role?.replaceAll('ROLE_', '').replaceAll('_', ' ') ?? "";
    const displayName = user.fullName || user.username || user.email || "User";
    $('#userInfoDisplay').html(`
        <span class="opacity-75">Logged in as:</span> 
        <span class="fw-bold">${displayName}</span> 
        <span class="badge bg-secondary ms-1">${friendlyRole}</span>
    `);
}

function configureSidebar(userRole) {
    const role = userRole?.replaceAll('ROLE_', '') ?? "";

    document.querySelectorAll('.role-restricted').forEach(item => {
        const allowedRoles = item.dataset.allowed;
        if (allowedRoles?.includes(role)) {
            $(item).show();
        } else {
            $(item).hide();
        }
    });
}

function setupNavigation() {
    const navLinks = document.querySelectorAll('.sidebar .nav-link');
    const brandLink = document.getElementById('brandLink');
    const logoutBtn = document.getElementById('logoutBtn');

    navLinks.forEach(link => {
        link.addEventListener('click', (e) => {
            e.preventDefault();
            navigateTo(link.id);
        });
    });

    if (brandLink) {
        brandLink.addEventListener('click', (e) => {
            e.preventDefault();
            const role = currentUser.role?.replaceAll("ROLE_", "") ?? "";
            renderDashboardByRole(role, currentUser);
        });
    }

    if (logoutBtn) {
        logoutBtn.addEventListener('click', () => {
            clearAuthStorage();
            globalThis.location.href = '/login.html';
        });
    }
}

function navigateTo(navId) {
    document.querySelectorAll('.sidebar .nav-link').forEach(l => l.classList.remove('active'));
    const activeLink = document.getElementById(navId);
    if (activeLink) activeLink.classList.add('active');

    const role = currentUser.role?.replaceAll("ROLE_", "") ?? "";

    switch (navId) {
        case 'nav-dashboard':
            renderDashboardByRole(role, currentUser);
            break;
        case 'nav-manage-clubs': renderManageClubs(); break;
        case 'nav-manage-events': renderManageEvents(); break;
        case 'nav-browse-clubs': renderBrowseClubs(); break;
        case 'nav-browse-events': renderBrowseEvents(); break;
        case 'nav-my-registrations': renderMyRegistrations(); break;

    }
}

function initTheme() {
    const toggleBtn = document.getElementById('themeToggle');
    const htmlElement = document.documentElement;

    const currentTheme = localStorage.getItem('theme') || 'light';
    htmlElement.dataset.bsTheme = currentTheme;
    updateIcon(toggleBtn, currentTheme);

    if (toggleBtn) {
        toggleBtn.addEventListener('click', () => {
            const current = htmlElement.dataset.bsTheme;
            const newTheme = current === 'dark' ? 'light' : 'dark';

            htmlElement.dataset.bsTheme = newTheme;
            localStorage.setItem('theme', newTheme);
            updateIcon(toggleBtn, newTheme);
        });
    }
}

function updateIcon(btn, theme) {
    if (!btn) return;
    btn.innerHTML = theme === 'dark' ? '<i class="bi bi-sun-fill"></i>' : '<i class="bi bi-moon-stars-fill"></i>';
}
