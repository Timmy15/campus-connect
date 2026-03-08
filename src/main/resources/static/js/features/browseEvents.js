import { apiRequest } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

export function renderBrowseEvents() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Browse Events</h2>
                    <p class="text-muted mb-0">Explore upcoming club events.</p>
                </div>
            </div>

            <div class="card p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-semibold mb-0">Upcoming Events</h5>
                    <span class="text-muted small" id="eventBrowseCount">Loading...</span>
                </div>
                <div id="eventBrowseGrid" class="row g-3"></div>
                <div id="eventBrowseEmpty" class="text-muted d-none">No active events yet.</div>
            </div>
        </div>
    `;

    loadEvents();
}

async function loadEvents() {
    const countEl = document.getElementById('eventBrowseCount');
    const grid = document.getElementById('eventBrowseGrid');
    const emptyEl = document.getElementById('eventBrowseEmpty');

    try {
        const response = await apiRequest('/api/events');
        if (!response.ok) {
            countEl.textContent = 'Unable to load events.';
            emptyEl.classList.remove('d-none');
            return;
        }
        const events = await response.json();
        countEl.textContent = `${events.length} event${events.length === 1 ? '' : 's'}`;
        if (!events.length) {
            emptyEl.classList.remove('d-none');
            return;
        }

        grid.innerHTML = events.map(event => `
            <div class="col-md-6 col-xl-4">
                <div class="card h-100 p-3">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h6 class="fw-semibold mb-0">${escapeHtml(event.title)}</h6>
                        <span class="badge bg-info-subtle text-info">${escapeHtml(event.clubName || 'Club')}</span>
                    </div>
                    <div class="text-muted small mb-2">
                        ${formatEventDate(event.startTime)}
                        ${event.endTime ? ` - ${formatEventDate(event.endTime)}` : ''}
                    </div>
                    <div class="text-muted small mb-2">${escapeHtml(event.location || 'Location TBD')}</div>
                    <p class="text-muted small mb-0">${escapeHtml(event.description || 'No description yet.')}</p>
                    <div class="mt-2">
                        <span class="badge bg-secondary-subtle text-secondary">Capacity ${event.capacity ?? '-'}</span>
                    </div>
                </div>
            </div>
        `).join('');
    } catch (error) {
        console.warn('Failed to load events.', error);
        countEl.textContent = 'Unable to load events.';
        emptyEl.classList.remove('d-none');
    }
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}
