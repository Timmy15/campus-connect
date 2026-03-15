import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

let eventState = null;
let filtersBound = false;
let registerHandlerBound = false;

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
    bindRegisterHandler();
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
        eventState = {
            allEvents: events,
            filtered: events
        };

        if (!filtersBound) {
            searchInput.addEventListener('input', () => applyEventFilters(eventState));
            categorySelect.addEventListener('change', () => applyEventFilters(eventState));
            sortSelect.addEventListener('change', () => applyEventFilters(eventState));
            filtersBound = true;
        }
        populateEventCategories(eventState, categorySelect);
        applyEventFilters(eventState);
    } catch (error) {
        console.warn('Failed to load events.', error);
        countEl.textContent = 'Unable to load events.';
        emptyEl.classList.remove('d-none');
    }
}

function applyEventFilters(state) {
    const ui = getBrowseEventsUi();
    const filters = getEventFilters(ui);
    const filtered = filterEvents(state.allEvents, filters);
    sortEvents(filtered, filters.sortMode);

    state.filtered = filtered;
    updateEventCount(ui, filtered.length);
    if (renderEmptyState(ui, filtered.length, state.allEvents.length)) {
        return;
    }

    const adminView = isAdminUser();
    ui.grid.innerHTML = filtered.map(event => buildEventCard(event, adminView)).join('');
}

function getBrowseEventsUi() {
    return {
        countEl: document.getElementById('eventBrowseCount'),
        grid: document.getElementById('eventBrowseGrid'),
        emptyEl: document.getElementById('eventBrowseEmpty'),
        searchInput: document.getElementById('eventSearchInput'),
        categorySelect: document.getElementById('eventCategoryFilter'),
        sortSelect: document.getElementById('eventSortSelect')
    };
}

function getEventFilters(ui) {
    return {
        term: (ui.searchInput?.value || '').trim().toLowerCase(),
        category: (ui.categorySelect?.value || '').trim().toLowerCase(),
        sortMode: ui.sortSelect?.value || 'date-asc'
    };
}

function filterEvents(events, { term, category }) {
    return events.filter(event => {
        const title = (event.title || '').toLowerCase();
        const eventCategory = normalizeCategory(event.clubCategory).toLowerCase();
        const matchesTerm = !term || title.includes(term);
        const matchesCategory = !category || eventCategory === category;
        return matchesTerm && matchesCategory;
    });
}

function sortEvents(events, sortMode) {
    events.sort((a, b) => {
        const aTime = toSortableDate(a.startTime);
        const bTime = toSortableDate(b.startTime);
        if (sortMode === 'date-desc') {
            return bTime - aTime;
        }
        return aTime - bTime;
    });
}

function updateEventCount(ui, count) {
    if (!ui.countEl) {
        return;
    }
    const label = count === 1 ? 'event' : 'events';
    ui.countEl.textContent = `${count} ${label}`;
}

function renderEmptyState(ui, filteredCount, totalCount) {
    if (filteredCount !== 0) {
        ui.emptyEl?.classList.add('d-none');
        return false;
    }

    if (ui.grid) {
        ui.grid.innerHTML = '';
    }
    if (ui.emptyEl) {
        ui.emptyEl.textContent = totalCount ? 'No events match your search.' : 'No active events yet.';
        ui.emptyEl.classList.remove('d-none');
    }
    return true;
}

function buildEventCard(event, adminView) {
    const cardState = getEventCardState(event, adminView);
    const endTimeHtml = event.endTime ? ` - ${formatEventDate(event.endTime)}` : '';
    const registerButtonHtml = cardState.allowRegister
        ? renderRegisterButton(event.id, cardState.buttonLabel, cardState.buttonDisabled)
        : '';
    const noteHtml = cardState.note
        ? `<div class="small mt-2 ${cardState.noteClass}" data-role="registration-note">${escapeHtml(cardState.note)}</div>`
        : '';

    return `
        <div class="col-md-6 col-xl-4">
            <div class="card h-100 p-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="fw-semibold mb-0">${escapeHtml(event.title)}</h6>
                    <span class="badge bg-info-subtle text-info">${escapeHtml(event.clubName || 'Club')}</span>
                </div>
                <div class="text-muted small mb-2">
                    ${formatEventDate(event.startTime)}${endTimeHtml}
                </div>
                <div class="text-muted small mb-2">${escapeHtml(event.location || 'Location TBD')}</div>
                <p class="text-muted small mb-0">${escapeHtml(event.description || 'No description yet.')}</p>
                <div class="mt-2 d-flex flex-wrap gap-2">
                    <span class="badge bg-secondary-subtle text-secondary">${escapeHtml(cardState.capacityLabel)}</span>
                    <span class="badge bg-primary-subtle text-primary">${escapeHtml(normalizeCategory(event.clubCategory))}</span>
                </div>
                ${registerButtonHtml}
                ${noteHtml}
            </div>
        </div>
    `;
}

function renderRegisterButton(eventId, label, disabled) {
    const disabledAttr = disabled ? 'disabled' : '';
    return `
                <div class="mt-3 d-flex justify-content-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="register" data-id="${eventId}" ${disabledAttr}>
                        <i class="bi bi-check2-circle me-1"></i>${escapeHtml(label)}
                    </button>
                </div>
    `;
}

function getEventCardState(event, adminView) {
    const remaining = resolveCapacityRemaining(event);
    const isRegistered = Boolean(event.registered);
    const isFull = remaining !== null && remaining <= 0;
    const allowRegister = !adminView;

    return {
        remaining,
        isRegistered,
        isFull,
        allowRegister,
        buttonLabel: getRegisterButtonLabel(isRegistered, isFull),
        buttonDisabled: isRegistered || isFull,
        capacityLabel: getCapacityLabel(remaining),
        note: getRegistrationNote(allowRegister, isRegistered, isFull),
        noteClass: allowRegister && isFull ? 'text-danger' : 'text-muted'
    };
}

function getRegisterButtonLabel(isRegistered, isFull) {
    if (isRegistered) {
        return 'Already registered';
    }
    if (isFull) {
        return 'Full';
    }
    return 'Register';
}

function getCapacityLabel(remaining) {
    if (remaining === null) {
        return 'Capacity unavailable';
    }
    const plural = remaining === 1 ? '' : 's';
    return `${remaining} place${plural} left`;
}

function getRegistrationNote(allowRegister, isRegistered, isFull) {
    if (!allowRegister) {
        return 'Admin accounts cannot register for events.';
    }
    if (isRegistered) {
        return "You're already registered for this event page";
    }
    if (isFull) {
        return 'Capacity for this event is reached';
    }
    return '';
}

function bindRegisterHandler() {
    if (registerHandlerBound) return;
    document.addEventListener('click', event => {
        const button = event.target.closest('button[data-action="register"]');
        if (!button) return;
        const grid = document.getElementById('eventBrowseGrid');
        if (!grid?.contains(button)) return;
        handleEventRegistration(button.dataset.id);
    });
    registerHandlerBound = true;
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
        await loadEvents();
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

    const selected = selectEl.value;
    const options = Array.from(categories)
        .sort((a, b) => a.localeCompare(b, undefined, { sensitivity: 'base' }))
        .map(category => `
        <option value="${escapeHtml(category.toLowerCase())}">${escapeHtml(category)}</option>
    `);

    selectEl.innerHTML = `
        <option value="">All categories</option>
        ${options.join('')}
    `;

    if (selected) {
        selectEl.value = selected;
    }
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

function isAdminUser() {
    const role = localStorage.getItem('cc.role') || '';
    return role.replace('ROLE_', '') === 'ADMIN';
}

function resolveCapacityRemaining(event) {
    if (typeof event?.capacityRemaining === 'number') {
        return event.capacityRemaining;
    }
    if (typeof event?.capacity === 'number') {
        return event.capacity;
    }
    return null;
}

function setEventBrowseStatus(message, isSuccess = false) {
    const statusEl = document.getElementById('eventBrowseStatus');
    if (!statusEl) return;
    statusEl.textContent = message || '';
    statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
    statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
}
