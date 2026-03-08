import { apiRequest, safeJson } from '../utils/api.js';
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
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h5 class="fw-semibold mb-0">Upcoming Events</h5>
                    <span class="text-muted small" id="eventBrowseCount">Loading...</span>
                </div>
                <div class="small mb-2" id="eventBrowseStatus"></div>
                <div class="row g-2 align-items-center mb-3">
                    <div class="col-md-5">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text">Search</span>
                            <input type="text" class="form-control" id="eventSearchInput" placeholder="Search by event name">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <select class="form-select form-select-sm" id="eventCategoryFilter">
                            <option value="">All categories</option>
                        </select>
                    </div>
                    <div class="col-md-3">
                        <select class="form-select form-select-sm" id="eventSortSelect">
                            <option value="date-asc">Date: soonest first</option>
                            <option value="date-desc">Date: latest first</option>
                        </select>
                    </div>
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
    const emptyEl = document.getElementById('eventBrowseEmpty');
    const searchInput = document.getElementById('eventSearchInput');
    const categorySelect = document.getElementById('eventCategoryFilter');
    const sortSelect = document.getElementById('eventSortSelect');

    try {
        const response = await apiRequest('/api/events');
        if (!response.ok) {
            countEl.textContent = 'Unable to load events.';
            emptyEl.classList.remove('d-none');
            return;
        }
        const events = await response.json();
        const eventState = {
            allEvents: events,
            filtered: events
        };

        searchInput.addEventListener('input', () => applyEventFilters(eventState));
        categorySelect.addEventListener('change', () => applyEventFilters(eventState));
        sortSelect.addEventListener('change', () => applyEventFilters(eventState));
        populateEventCategories(eventState, categorySelect);
        applyEventFilters(eventState);
    } catch (error) {
        console.warn('Failed to load events.', error);
        countEl.textContent = 'Unable to load events.';
        emptyEl.classList.remove('d-none');
    }
}

function applyEventFilters(state) {
    const countEl = document.getElementById('eventBrowseCount');
    const grid = document.getElementById('eventBrowseGrid');
    const emptyEl = document.getElementById('eventBrowseEmpty');
    const searchInput = document.getElementById('eventSearchInput');
    const categorySelect = document.getElementById('eventCategoryFilter');
    const sortSelect = document.getElementById('eventSortSelect');

    const term = (searchInput.value || '').trim().toLowerCase();
    const category = (categorySelect.value || '').trim().toLowerCase();
    const sortMode = sortSelect.value || 'date-asc';

    const filtered = state.allEvents.filter(event => {
        const title = (event.title || '').toLowerCase();
        const eventCategory = normalizeCategory(event.clubCategory).toLowerCase();
        const matchesTerm = !term || title.includes(term);
        const matchesCategory = !category || eventCategory === category;
        return matchesTerm && matchesCategory;
    });

    filtered.sort((a, b) => {
        const aTime = toSortableDate(a.startTime);
        const bTime = toSortableDate(b.startTime);
        return sortMode === 'date-desc' ? bTime - aTime : aTime - bTime;
    });

    state.filtered = filtered;
    countEl.textContent = `${filtered.length} event${filtered.length === 1 ? '' : 's'}`;
    if (!filtered.length) {
        grid.innerHTML = '';
        emptyEl.textContent = state.allEvents.length ? 'No events match your search.' : 'No active events yet.';
        emptyEl.classList.remove('d-none');
        return;
    }
    emptyEl.classList.add('d-none');

    grid.innerHTML = filtered.map(event => `
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
                <div class="mt-2 d-flex flex-wrap gap-2">
                    <span class="badge bg-secondary-subtle text-secondary">Capacity ${event.capacity ?? '-'}</span>
                    <span class="badge bg-primary-subtle text-primary">${escapeHtml(normalizeCategory(event.clubCategory))}</span>
                </div>
                <div class="mt-3 d-flex justify-content-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="register" data-id="${event.id}">
                        Register
                    </button>
                </div>
            </div>
        </div>
    `).join('');

    grid.querySelectorAll('button[data-action="register"]').forEach(button => {
        button.addEventListener('click', () => handleEventRegistration(button.dataset.id));
    });
}

async function handleEventRegistration(eventId) {
    setEventBrowseStatus('');
    if (!eventId) {
        setEventBrowseStatus('Unable to register for this event.', false);
        return;
    }

    try {
        const response = await apiRequest(`/api/student/events/${eventId}/register`, {
            method: 'POST'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setEventBrowseStatus(data?.message || 'Unable to register for this event.', false);
            return;
        }
        setEventBrowseStatus(data?.message || 'Registration successful.', true);
    } catch (error) {
        console.warn('Unable to register for event.', error);
        setEventBrowseStatus('Unable to register for this event.', false);
    }
}

function populateEventCategories(state, selectEl) {
    const categories = new Set();
    state.allEvents.forEach(event => {
        categories.add(normalizeCategory(event.clubCategory));
    });

    const options = Array.from(categories)
        .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
        .map(category => `
        <option value="${escapeHtml(category.toLowerCase())}">${escapeHtml(category)}</option>
    `);

    selectEl.innerHTML = `
        <option value="">All categories</option>
        ${options.join('')}
    `;
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}

function toSortableDate(value) {
    if (!value) {
        return Number.MAX_SAFE_INTEGER;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return Number.MAX_SAFE_INTEGER;
    }
    return date.getTime();
}

function normalizeCategory(value) {
    const trimmed = (value || '').trim();
    return trimmed || 'Uncategorized';
}

function setEventBrowseStatus(message, isSuccess = false) {
    const statusEl = document.getElementById('eventBrowseStatus');
    if (!statusEl) return;
    statusEl.textContent = message || '';
    statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
    statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
}
