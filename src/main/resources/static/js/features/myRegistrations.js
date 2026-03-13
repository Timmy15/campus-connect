import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

export function renderMyRegistrations() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">My Registrations</h2>
                    <p class="text-muted mb-0">Track your event registrations.</p>
                </div>
            </div>
            <div class="card p-4">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h5 class="fw-semibold mb-0">Registered Events</h5>
                    <span class="text-muted small" id="registrationCount">Loading...</span>
                </div>
                <div class="small mb-2" id="registrationStatus"></div>
                <div id="registrationTableWrap" class="table-responsive"></div>
                <div id="registrationEmpty" class="text-muted d-none">No registrations yet.</div>
            </div>
        </div>
    `;

    loadRegistrations();
}

async function loadRegistrations() {
    const countEl = document.getElementById('registrationCount');
    const statusEl = document.getElementById('registrationStatus');
    const tableWrap = document.getElementById('registrationTableWrap');
    const emptyEl = document.getElementById('registrationEmpty');

    try {
        const response = await apiRequest('/api/student/registrations');
        const data = await safeJson(response);

        if (!response.ok) {
            countEl.textContent = 'Unable to load registrations.';
            statusEl.textContent = data?.message || 'Unable to load registrations.';
            statusEl.classList.add('text-danger');
            emptyEl.classList.remove('d-none');
            tableWrap.innerHTML = '';
            return;
        }

        const registrations = Array.isArray(data) ? data : [];
        countEl.textContent = `${registrations.length} registration${registrations.length === 1 ? '' : 's'}`;
        if (!registrations.length) {
            tableWrap.innerHTML = '';
            emptyEl.classList.remove('d-none');
            return;
        }
        emptyEl.classList.add('d-none');

        tableWrap.innerHTML = `
            <table class="table table-sm align-middle">
                <thead>
                    <tr>
                        <th>Event</th>
                        <th>Club</th>
                        <th>Date & Time</th>
                        <th>Location</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    ${registrations.map(item => {
                        const badge = getRegistrationBadge(item);
                        return `
                        <tr>
                            <td class="fw-semibold">${escapeHtml(item.eventTitle || 'Event')}</td>
                            <td>${escapeHtml(item.clubName || 'Club')}</td>
                            <td class="text-muted small">${formatEventDate(item.startTime)}${item.endTime ? ` - ${formatEventDate(item.endTime)}` : ''}</td>
                            <td>${escapeHtml(item.location || 'TBD')}</td>
                            <td>
                                <span class="badge ${badge.className}">
                                    ${escapeHtml(badge.label)}
                                </span>
                            </td>
                        </tr>
                    `;
                    }).join('')}
                </tbody>
            </table>
        `;
    } catch (error) {
        console.warn('Unable to load registrations.', error);
        countEl.textContent = 'Unable to load registrations.';
        statusEl.textContent = 'Unable to load registrations.';
        statusEl.classList.add('text-danger');
        emptyEl.classList.remove('d-none');
        tableWrap.innerHTML = '';
    }
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}

function getRegistrationBadge(item) {
    if (isEventCompleted(item)) {
        return { label: 'COMPLETED', className: 'bg-secondary-subtle text-secondary' };
    }
    return { label: item.status || 'REGISTERED', className: 'bg-success-subtle text-success' };
}

function isEventCompleted(item) {
    const now = new Date();
    const end = parseEventDate(item?.endTime);
    if (end) {
        return end < now;
    }
    const start = parseEventDate(item?.startTime);
    return Boolean(start && start < now);
}

function parseEventDate(value) {
    if (!value) {
        return null;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return null;
    }
    return date;
}
