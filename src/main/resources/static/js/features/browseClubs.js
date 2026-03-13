import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

let clubState = null;
let clubClickBound = false;
let eventsRequestId = 0;
let selectedClubId = null;
let clubEventActionBound = false;

export function renderBrowseClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Browse Clubs</h2>
                    <p class="text-muted mb-0">Find clubs that match your interests.</p>
                </div>
            </div>

            <div class="card p-4">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h5 class="fw-semibold mb-0">Available Clubs</h5>
                    <span class="text-muted small" id="clubBrowseCount">Loading...</span>
                </div>
                <div class="row g-2 align-items-center mb-3">
                    <div class="col-md-6">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text">Search</span>
                            <input type="text" class="form-control" id="clubSearchInput" placeholder="Search by club name">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <select class="form-select form-select-sm" id="clubCategoryFilter">
                            <option value="">All categories</option>
                        </select>
                    </div>
                </div>
                <div id="clubBrowseGrid" class="row g-3"></div>
                <div id="clubBrowseEmpty" class="text-muted d-none">No active clubs yet.</div>
            </div>

            <div class="card p-4 mt-4 d-none" id="clubEventsSection">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Events for <span id="clubEventsTitle">Club</span></h5>
                </div>
                <div class="small text-muted" id="clubEventsStatus">Select a club to view events.</div>
                <div id="clubEventsEmpty" class="text-muted d-none mt-3">No events for this club yet.</div>
                <div id="clubEventsGrid" class="row g-3 mt-2"></div>
            </div>
        </div>
    `;

    loadClubs();
    bindClubClickHandler();
    bindClubEventActionHandler();
}

async function loadClubs() {
    const countEl = document.getElementById('clubBrowseCount');
    const emptyEl = document.getElementById('clubBrowseEmpty');
    const searchInput = document.getElementById('clubSearchInput');
    const categorySelect = document.getElementById('clubCategoryFilter');

    try {
        const response = await apiRequest('/api/clubs');
        if (!response.ok) {
            countEl.textContent = 'Unable to load clubs.';
            emptyEl.classList.remove('d-none');
            return;
        }
        const clubs = await response.json();
        clubState = {
            allClubs: clubs,
            filtered: clubs
        };

        searchInput.addEventListener('input', () => applyClubFilters(clubState));
        categorySelect.addEventListener('change', () => applyClubFilters(clubState));
        populateClubCategories(clubState, categorySelect);
        applyClubFilters(clubState);
    } catch (error) {
        console.warn('Failed to load clubs.', error);
        countEl.textContent = 'Unable to load clubs.';
        emptyEl.classList.remove('d-none');
    }
}

function applyClubFilters(state) {
    const countEl = document.getElementById('clubBrowseCount');
    const grid = document.getElementById('clubBrowseGrid');
    const emptyEl = document.getElementById('clubBrowseEmpty');
    const searchInput = document.getElementById('clubSearchInput');
    const categorySelect = document.getElementById('clubCategoryFilter');

    const term = (searchInput.value || '').trim().toLowerCase();
    const category = (categorySelect.value || '').trim().toLowerCase();

    const filtered = state.allClubs.filter(club => {
        const name = (club.name || '').toLowerCase();
        const clubCategory = normalizeCategory(club.category).toLowerCase();
        const matchesTerm = !term || name.includes(term);
        const matchesCategory = !category || clubCategory === category;
        return matchesTerm && matchesCategory;
    });

    state.filtered = filtered;
    countEl.textContent = `${filtered.length} club${filtered.length === 1 ? '' : 's'}`;
    if (!filtered.length) {
        grid.innerHTML = '';
        emptyEl.textContent = state.allClubs.length ? 'No clubs match your search.' : 'No active clubs yet.';
        emptyEl.classList.remove('d-none');
        return;
    }
    emptyEl.classList.add('d-none');

    grid.innerHTML = filtered.map(club => `
        <div class="col-md-6 col-xl-4">
            <div class="card h-100 p-3" data-role="club-card" data-club-id="${club.id}" role="button" tabindex="0" style="cursor: pointer;">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="fw-semibold mb-0">${escapeHtml(club.name)}</h6>
                    <span class="badge bg-primary-subtle text-primary">${escapeHtml(normalizeCategory(club.category))}</span>
                </div>
                <p class="text-muted small mb-0">${escapeHtml(club.description || 'No description yet.')}</p>
            </div>
        </div>
    `).join('');
}

function populateClubCategories(state, selectEl) {
    const categories = new Set();
    state.allClubs.forEach(club => {
        categories.add(normalizeCategory(club.category));
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

function normalizeCategory(value) {
    const trimmed = (value || '').trim();
    return trimmed || 'Uncategorized';
}

function bindClubClickHandler() {
    if (clubClickBound) {
        return;
    }
    document.addEventListener('click', event => {
        const card = event.target.closest('[data-role="club-card"]');
        if (!card) {
            return;
        }
        const grid = document.getElementById('clubBrowseGrid');
        if (!grid?.contains(card)) {
            return;
        }
        handleClubSelection(card.dataset.clubId);
    });

    document.addEventListener('keydown', event => {
        if (event.key !== 'Enter' && event.key !== ' ') {
            return;
        }
        const card = event.target.closest('[data-role="club-card"]');
        if (!card) {
            return;
        }
        const grid = document.getElementById('clubBrowseGrid');
        if (!grid?.contains(card)) {
            return;
        }
        event.preventDefault();
        handleClubSelection(card.dataset.clubId);
    });
    clubClickBound = true;
}

function bindClubEventActionHandler() {
    if (clubEventActionBound) {
        return;
    }
    document.addEventListener('click', event => {
        const button = event.target.closest('button[data-action="register-event"]');
        if (!button) {
            return;
        }
        const grid = document.getElementById('clubEventsGrid');
        if (!grid?.contains(button)) {
            return;
        }
        handleEventRegistration(button.dataset.eventId);
    });
    clubEventActionBound = true;
}

async function handleEventRegistration(eventId) {
    if (!eventId) {
        setClubEventsStatus('Unable to register for this event.');
        return;
    }

    try {
        const response = await apiRequest(`/api/student/events/${eventId}/register`, {
            method: 'POST'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setClubEventsStatus(data?.message || 'Unable to register for this event.');
            return;
        }
        setClubEventsStatus(data?.message || 'Registration successful.');
        if (selectedClubId) {
            await handleClubSelection(selectedClubId);
        }
    } catch (error) {
        console.warn('Unable to register for event.', error);
        setClubEventsStatus('Unable to register for this event.');
    }
}

function setClubEventsStatus(message) {
    const statusEl = document.getElementById('clubEventsStatus');
    if (statusEl) {
        statusEl.textContent = message || '';
    }
}

async function handleClubSelection(clubId) {
    if (!clubId) {
        return;
    }

    const ui = getClubEventsUi();
    const clubName = resolveClubName(clubId);
    selectedClubId = clubId;
    setClubEventsLoading(ui, clubName);
    const requestId = ++eventsRequestId;

    try {
        const response = await apiRequest('/api/events');
        const data = await safeJson(response);
        if (requestId !== eventsRequestId) {
            return;
        }
        if (!response.ok) {
            const message = data?.message || 'Unable to load events for this club.';
            setClubEventsError(ui, clubName, message);
            return;
        }
        const events = Array.isArray(data) ? data : [];
        const clubEvents = events.filter(event => String(event.clubId) === String(clubId));
        renderClubEvents(ui, clubName, clubEvents, isAdminUser());
    } catch (error) {
        console.warn('Failed to load club events.', error);
        setClubEventsError(ui, clubName, 'Unable to load events for this club.');
    }
}

function resolveClubName(clubId) {
    if (!clubState?.allClubs) {
        return 'Club';
    }
    const match = clubState.allClubs.find(club => String(club.id) === String(clubId));
    return match?.name || 'Club';
}

function getClubEventsUi() {
    return {
        section: document.getElementById('clubEventsSection'),
        titleEl: document.getElementById('clubEventsTitle'),
        statusEl: document.getElementById('clubEventsStatus'),
        emptyEl: document.getElementById('clubEventsEmpty'),
        grid: document.getElementById('clubEventsGrid')
    };
}

function setClubEventsLoading(ui, clubName) {
    ui.section?.classList.remove('d-none');
    if (ui.titleEl) {
        ui.titleEl.textContent = clubName || 'Club';
    }
    if (ui.statusEl) {
        ui.statusEl.textContent = `Loading events for ${clubName || 'this club'}...`;
    }
    ui.emptyEl?.classList.add('d-none');
    if (ui.grid) {
        ui.grid.innerHTML = '';
    }
}

function setClubEventsError(ui, clubName, message) {
    ui.section?.classList.remove('d-none');
    if (ui.titleEl) {
        ui.titleEl.textContent = clubName || 'Club';
    }
    if (ui.statusEl) {
        ui.statusEl.textContent = message;
    }
    ui.emptyEl?.classList.add('d-none');
    if (ui.grid) {
        ui.grid.innerHTML = '';
    }
}

function renderClubEvents(ui, clubName, events, adminView) {
    ui.section?.classList.remove('d-none');
    if (ui.titleEl) {
        ui.titleEl.textContent = clubName || 'Club';
    }

    if (events.length === 0) {
        if (ui.statusEl) {
            ui.statusEl.textContent = 'No events for this club yet.';
        }
        ui.emptyEl?.classList.remove('d-none');
        if (ui.grid) {
            ui.grid.innerHTML = '';
        }
        return;
    }

    ui.emptyEl?.classList.add('d-none');
    if (ui.statusEl) {
        ui.statusEl.textContent = `Showing ${events.length} event${events.length === 1 ? '' : 's'} for ${clubName || 'this club'}.`;
    }
    if (ui.grid) {
        ui.grid.innerHTML = events.map(event => buildEventCard(event, adminView)).join('');
    }
}

function buildEventCard(event, adminView) {
    const cardState = getClubEventCardState(event, adminView);
    const endTime = event.endTime ? ` - ${formatEventDate(event.endTime)}` : '';
    const location = escapeHtml(event.location || 'Location TBD');
    const registerButton = cardState.allowRegister
        ? renderRegisterButton(event.id, cardState.buttonLabel, cardState.buttonDisabled)
        : '';
    const noteHtml = cardState.note
        ? `<div class="small mt-2 ${cardState.noteClass}" data-role="registration-note">${escapeHtml(cardState.note)}</div>`
        : '';
    return `
        <div class="col-md-6 col-xl-4">
            <div class="card h-100 p-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="fw-semibold mb-0">${escapeHtml(event.title || 'Event')}</h6>
                    <span class="badge bg-info-subtle text-info">${escapeHtml(event.clubName || 'Club')}</span>
                </div>
                <div class="text-muted small mb-2">${formatEventDate(event.startTime)}${endTime}</div>
                <div class="text-muted small mb-2">${location}</div>
                <p class="text-muted small mb-0">${escapeHtml(event.description || 'No description yet.')}</p>
                <div class="mt-2 d-flex flex-wrap gap-2">
                    <span class="badge bg-secondary-subtle text-secondary">${escapeHtml(cardState.capacityLabel)}</span>
                    <span class="badge bg-primary-subtle text-primary">${escapeHtml(cardState.registeredLabel)}</span>
                </div>
                ${registerButton}
                ${noteHtml}
            </div>
        </div>
    `;
}

function getClubEventCardState(event, adminView) {
    const remaining = resolveCapacityRemaining(event);
    const registeredCount = resolveRegisteredCount(event);
    const isRegistered = Boolean(event.registered);
    const isFull = remaining !== null && remaining <= 0;
    const allowRegister = !adminView;

    return {
        allowRegister,
        buttonLabel: getRegisterButtonLabel(isRegistered, isFull),
        buttonDisabled: isRegistered || isFull,
        capacityLabel: getCapacityLabel(remaining),
        registeredLabel: `${registeredCount} registered`,
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

function renderRegisterButton(eventId, label, disabled) {
    const disabledAttr = disabled ? 'disabled' : '';
    return `
                <div class="mt-3 d-flex justify-content-end">
                    <button class="btn btn-sm btn-outline-primary" data-action="register-event" data-event-id="${eventId}" ${disabledAttr}>
                        ${escapeHtml(label)}
                    </button>
                </div>
    `;
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
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

function resolveRegisteredCount(event) {
    return Number.isFinite(event?.registeredCount) ? event.registeredCount : 0;
}
