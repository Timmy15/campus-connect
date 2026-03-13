import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

let cachedClubs = [];
let cachedEvents = [];
let editClubId = null;
let editEventId = null;
let selectedClubId = null;

export function renderManageClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Manage Clubs</h2>
                    <p class="text-muted mb-0">Create, update, or deactivate clubs and manage events.</p>
                </div>
            </div>

            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card p-4 h-100">
                        <h5 class="fw-semibold mb-3" id="clubFormTitle">Create Club</h5>
                        <form id="clubForm">
                            <div class="mb-3">
                                <label class="form-label" for="clubName">Club Name</label>
                                <input type="text" class="form-control" id="clubName" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="clubCategory">Category</label>
                                <input type="text" class="form-control" id="clubCategory" placeholder="e.g. Sports, Tech">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="clubDescription">Description</label>
                                <textarea class="form-control" id="clubDescription" rows="4"></textarea>
                            </div>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary flex-grow-1" id="clubFormSubmit">Create Club</button>
                                <button type="button" class="btn btn-outline-secondary d-none" id="clubFormCancel">Cancel</button>
                            </div>
                            <div class="small mt-2" id="clubFormStatus"></div>
                        </form>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card p-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                            <h5 class="fw-semibold mb-0">Existing Clubs</h5>
                            <span class="text-muted small" id="clubCount">Loading...</span>
                        </div>
                        <div id="clubTableWrap" class="table-responsive"></div>
                    </div>
                </div>
            </div>

            <div class="row g-4 mt-1" id="eventManagementSection">
                <div class="col-lg-4">
                    <div class="card p-4 h-100">
                        <h5 class="fw-semibold mb-3" id="eventFormTitle">Create Event</h5>
                        <form id="eventForm">
                            <div class="mb-3">
                                <label class="form-label" for="eventClubSelect">Club</label>
                                <select class="form-select" id="eventClubSelect"></select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventTitle">Event Title</label>
                                <input type="text" class="form-control" id="eventTitle">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventLocation">Location</label>
                                <input type="text" class="form-control" id="eventLocation">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventCapacity">Capacity</label>
                                <input type="number" class="form-control" id="eventCapacity">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventStartTime">Start Time</label>
                                <input type="datetime-local" class="form-control" id="eventStartTime">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventEndTime">End Time (optional)</label>
                                <input type="datetime-local" class="form-control" id="eventEndTime">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventDescription">Description</label>
                                <textarea class="form-control" id="eventDescription" rows="3"></textarea>
                            </div>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary flex-grow-1" id="eventFormSubmit">Create Event</button>
                                <button type="button" class="btn btn-outline-secondary d-none" id="eventFormCancel">Cancel</button>
                            </div>
                            <div class="small mt-2" id="eventFormStatus"></div>
                        </form>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card p-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                            <h5 class="fw-semibold mb-0">Club Events</h5>
                            <span class="text-muted small" id="eventCount">Select a club</span>
                        </div>
                        <div id="eventTableWrap" class="table-responsive"></div>
                    </div>
                </div>
            </div>
        </div>
    `;

    bindClubForm();
    bindEventForm();
    loadClubs();
}

function bindClubForm() {
    const form = document.getElementById('clubForm');
    const cancelBtn = document.getElementById('clubFormCancel');

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        handleSubmit();
    });

    cancelBtn.addEventListener('click', () => resetForm());
}

function bindEventForm() {
    const form = document.getElementById('eventForm');
    const cancelBtn = document.getElementById('eventFormCancel');
    const clubSelect = document.getElementById('eventClubSelect');

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        handleEventSubmit();
    });

    cancelBtn.addEventListener('click', () => resetEventForm());
    clubSelect.addEventListener('change', () => {
        selectedClubId = clubSelect.value || null;
        editEventId = null;
        resetEventForm(true);
        loadEventsForClub();
    });
}

async function loadClubs() {
    try {
        const response = await apiRequest('/api/admin/clubs');
        if (!response.ok) {
            setClubStatus('Unable to load clubs.', false);
            cachedClubs = [];
            renderClubTable();
            populateClubSelect();
            return;
        }
        cachedClubs = await response.json();
        renderClubTable();
        populateClubSelect();
    } catch (error) {
        console.warn('Unable to load clubs.', error);
        setClubStatus('Unable to load clubs.', false);
        cachedClubs = [];
        renderClubTable();
        populateClubSelect();
    }
}

function renderClubTable() {
    const tableWrap = document.getElementById('clubTableWrap');
    const clubCount = document.getElementById('clubCount');

    if (!cachedClubs || cachedClubs.length === 0) {
        clubCount.textContent = '0 clubs';
        tableWrap.innerHTML = `<div class="text-muted">No clubs yet. Create the first one.</div>`;
        return;
    }

    clubCount.textContent = `${cachedClubs.length} club${cachedClubs.length === 1 ? '' : 's'}`;

    tableWrap.innerHTML = `
        <table class="table table-sm align-middle">
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                ${cachedClubs.map(club => `
                    <tr>
                        <td>
                            <div class="fw-semibold">${escapeHtml(club.name)}</div>
                            <div class="text-muted small">${escapeHtml(club.description || 'No description')}</div>
                        </td>
                        <td>${escapeHtml(club.category || 'Uncategorized')}</td>
                        <td>
                            <span class="badge ${club.active ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">
                                ${club.active ? 'Active' : 'Inactive'}
                            </span>
                        </td>
                        <td class="text-end">
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-primary" data-action="edit" data-id="${club.id}">Edit</button>
                                ${club.active
                                    ? `<button class="btn btn-outline-danger" data-action="deactivate" data-id="${club.id}">Deactivate</button>`
                                    : `<button class="btn btn-outline-success" data-action="activate" data-id="${club.id}">Activate</button>`
                                }
                                <button class="btn btn-outline-danger" data-action="delete" data-id="${club.id}">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    tableWrap.querySelectorAll('button[data-action="edit"]').forEach(btn => {
        btn.addEventListener('click', () => startEdit(btn.dataset.id));
    });

    tableWrap.querySelectorAll('button[data-action="deactivate"]').forEach(btn => {
        btn.addEventListener('click', () => handleDeactivate(btn.dataset.id));
    });

    tableWrap.querySelectorAll('button[data-action="activate"]').forEach(btn => {
        btn.addEventListener('click', () => handleActivate(btn.dataset.id));
    });

    tableWrap.querySelectorAll('button[data-action="delete"]').forEach(btn => {
        btn.addEventListener('click', () => handleDeleteClub(btn.dataset.id));
    });
}

function populateClubSelect() {
    const select = document.getElementById('eventClubSelect');
    if (!select) return;

    const previousSelection = selectedClubId;
    const options = cachedClubs.map(club => `
        <option value="${club.id}">${escapeHtml(club.name)}</option>
    `).join('');

    select.innerHTML = `
        <option value="">Select a club</option>
        ${options}
    `;

    if (previousSelection && cachedClubs.some(club => String(club.id) === String(previousSelection))) {
        select.value = previousSelection;
        selectedClubId = previousSelection;
    } else if (cachedClubs.length > 0) {
        selectedClubId = String(cachedClubs[0].id);
        select.value = selectedClubId;
    } else {
        selectedClubId = null;
    }

    select.disabled = Boolean(editEventId);
    setEventFormEnabled(cachedClubs.length > 0);
    loadEventsForClub();
}

function setEventFormEnabled(enabled) {
    const form = document.getElementById('eventForm');
    if (form === null) {
        return;
    }

    const disableInputs = enabled === false;
    form.querySelectorAll('input, textarea, select, button').forEach(el => {
        el.disabled = disableInputs;
    });

    const clubSelect = document.getElementById('eventClubSelect');
    const isEditingEvent = editEventId !== null;
    if (clubSelect && isEditingEvent) {
        clubSelect.disabled = true;
    }

    if (enabled) {
        setEventStatus('', false);
    } else {
        setEventStatus('Create a club first to add events.', false);
    }
}

async function loadEventsForClub() {
    const countEl = document.getElementById('eventCount');
    const tableWrap = document.getElementById('eventTableWrap');

    if (!selectedClubId) {
        cachedEvents = [];
        countEl.textContent = 'Select a club';
        tableWrap.innerHTML = `<div class="text-muted">Select a club to view events.</div>`;
        return;
    }

    try {
        const response = await apiRequest(`/api/admin/clubs/${selectedClubId}/events`);
        if (!response.ok) {
            cachedEvents = [];
            countEl.textContent = 'Unable to load events.';
            tableWrap.innerHTML = `<div class="text-muted">Unable to load events.</div>`;
            return;
        }
        cachedEvents = await response.json();
        renderEventTable();
    } catch (error) {
        console.warn('Unable to load events.', error);
        cachedEvents = [];
        countEl.textContent = 'Unable to load events.';
        tableWrap.innerHTML = `<div class="text-muted">Unable to load events.</div>`;
    }
}

function renderEventTable() {
    const tableWrap = document.getElementById('eventTableWrap');
    const countEl = document.getElementById('eventCount');

    if (!selectedClubId) {
        countEl.textContent = 'Select a club';
        tableWrap.innerHTML = `<div class="text-muted">Select a club to view events.</div>`;
        return;
    }

    countEl.textContent = `${cachedEvents.length} event${cachedEvents.length === 1 ? '' : 's'}`;

    if (!cachedEvents.length) {
        tableWrap.innerHTML = `<div class="text-muted">No events yet for this club.</div>`;
        return;
    }

    tableWrap.innerHTML = `
        <table class="table table-sm align-middle">
            <thead>
                <tr>
                    <th>Event</th>
                    <th>Location</th>
                    <th>Date & Time</th>
                    <th>Capacity</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                ${cachedEvents.map(event => `
                    <tr>
                        <td>
                            <div class="fw-semibold">${escapeHtml(event.title)}</div>
                            <div class="text-muted small">${escapeHtml(event.description || 'No description')}</div>
                        </td>
                        <td>${escapeHtml(event.location || 'TBD')}</td>
                        <td>
                            <div class="text-muted small">${formatEventDate(event.startTime)}</div>
                            <div class="text-muted small">${event.endTime ? formatEventDate(event.endTime) : 'No end time'}</div>
                        </td>
                        <td>${event.capacity ?? '-'}</td>
                        <td class="text-end">
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-primary" data-action="edit-event" data-id="${event.id}">Edit</button>
                                <button class="btn btn-outline-danger" data-action="delete-event" data-id="${event.id}">Delete</button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    tableWrap.querySelectorAll('button[data-action="edit-event"]').forEach(btn => {
        btn.addEventListener('click', () => startEventEdit(btn.dataset.id));
    });

    tableWrap.querySelectorAll('button[data-action="delete-event"]').forEach(btn => {
        btn.addEventListener('click', () => handleDeleteEvent(btn.dataset.id));
    });
}

async function handleSubmit() {
    setClubStatus('');
    const nameInput = document.getElementById('clubName');
    const categoryInput = document.getElementById('clubCategory');
    const descriptionInput = document.getElementById('clubDescription');

    const payload = {
        name: nameInput.value.trim(),
        category: categoryInput.value.trim(),
        description: descriptionInput.value.trim()
    };

    if (!payload.name) {
        setClubStatus('Club name is required.', false);
        return;
    }

    const isEdit = editClubId !== null;
    const url = isEdit ? `/api/admin/clubs/${editClubId}` : '/api/admin/clubs';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await apiRequest(url, {
            method,
            body: JSON.stringify(payload)
        });
        const data = await safeJson(response);

        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to save club.', false);
            return;
        }

        setClubStatus(data?.message || (isEdit ? 'Club updated.' : 'Club created.'), true);
        resetForm();
        await loadClubs();
    } catch (error) {
        console.warn('Unable to save club.', error);
        setClubStatus('Unable to save club.', false);
    }
}

function startEdit(clubId) {
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club) return;

    editClubId = club.id;
    document.getElementById('clubName').value = club.name || '';
    document.getElementById('clubCategory').value = club.category || '';
    document.getElementById('clubDescription').value = club.description || '';
    document.getElementById('clubFormTitle').textContent = 'Update Club';
    document.getElementById('clubFormSubmit').textContent = 'Update Club';
    document.getElementById('clubFormCancel').classList.remove('d-none');
    setClubStatus('Editing club details.', true);
}

function resetForm() {
    editClubId = null;
    document.getElementById('clubForm').reset();
    document.getElementById('clubFormTitle').textContent = 'Create Club';
    document.getElementById('clubFormSubmit').textContent = 'Create Club';
    document.getElementById('clubFormCancel').classList.add('d-none');
}

async function handleDeactivate(clubId) {
    setClubStatus('');
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club?.active) return;

    const confirmed = globalThis.confirm(`Are you sure you want to deactivate "${club.name}"?`);
    if (!confirmed) return;

    try {
        const response = await apiRequest(`/api/admin/clubs/${clubId}`, {
            method: 'DELETE'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to deactivate club.', false);
            return;
        }
        setClubStatus(data?.message || 'Club deactivated.', true);
        await loadClubs();
    } catch (error) {
        console.warn('Unable to deactivate club.', error);
        setClubStatus('Unable to deactivate club.', false);
    }
}

async function handleActivate(clubId) {
    setClubStatus('');
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club || club.active) return;

    try {
        const response = await apiRequest(`/api/admin/clubs/${clubId}/activate`, {
            method: 'PUT'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to activate club.', false);
            return;
        }
        setClubStatus(data?.message || 'Club activated.', true);
        await loadClubs();
    } catch (error) {
        console.warn('Unable to activate club.', error);
        setClubStatus('Unable to activate club.', false);
    }
}

async function handleDeleteClub(clubId) {
    setClubStatus('');
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club) {
        return;
    }

    const confirmed = globalThis.confirm(`Are you sure you want to delete "${club.name}"?`);
    if (!confirmed) {
        return;
    }

    try {
        const response = await apiRequest(`/api/admin/clubs/${clubId}/delete`, {
            method: 'DELETE'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to delete club.', false);
            return;
        }

        if (editClubId && String(editClubId) === String(clubId)) {
            resetForm();
        }
        if (selectedClubId && String(selectedClubId) === String(clubId)) {
            selectedClubId = null;
            resetEventForm();
        }

        setClubStatus(data?.message || 'Club deleted.', true);
        await loadClubs();
    } catch (error) {
        console.warn('Unable to delete club.', error);
        setClubStatus('Unable to delete club.', false);
    }
}

async function handleEventSubmit() {
    setEventStatus('');

    if (!selectedClubId) {
        setEventStatus('Select a club before creating an event.', false);
        return;
    }

    const title = document.getElementById('eventTitle').value.trim();
    const location = document.getElementById('eventLocation').value.trim();
    const capacityValue = document.getElementById('eventCapacity').value;
    const startInput = document.getElementById('eventStartTime').value;
    const endInput = document.getElementById('eventEndTime').value;
    const description = document.getElementById('eventDescription').value.trim();

    const payload = {
        title,
        location,
        capacity: capacityValue ? Number.parseInt(capacityValue, 10) : null,
        startTime: normalizeDateTimeInput(startInput),
        endTime: normalizeDateTimeInput(endInput),
        description
    };

    const validationError = validateEventPayload(payload);
    if (validationError) {
        setEventStatus(validationError, false);
        return;
    }

    const isEdit = editEventId !== null;
    const url = isEdit ? `/api/admin/events/${editEventId}` : `/api/admin/clubs/${selectedClubId}/events`;
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await apiRequest(url, {
            method,
            body: JSON.stringify(payload)
        });
        const data = await safeJson(response);

        if (!response.ok) {
            setEventStatus(data?.message || 'Unable to save event.', false);
            return;
        }

        setEventStatus(data?.message || (isEdit ? 'Event updated.' : 'Event created.'), true);
        resetEventForm(true);
        await loadEventsForClub();
    } catch (error) {
        console.warn('Unable to save event.', error);
        setEventStatus('Unable to save event.', false);
    }
}

async function handleDeleteEvent(eventId) {
    setEventStatus('');
    const event = cachedEvents.find(item => String(item.id) === String(eventId));
    if (!event) {
        return;
    }

    const confirmed = globalThis.confirm(`Are you sure you want to delete "${event.title || 'this event'}"?`);
    if (!confirmed) {
        return;
    }

    try {
        const response = await apiRequest(`/api/admin/events/${eventId}`, {
            method: 'DELETE'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setEventStatus(data?.message || 'Unable to delete event.', false);
            return;
        }

        if (editEventId && String(editEventId) === String(eventId)) {
            resetEventForm(true);
        }

        setEventStatus(data?.message || 'Event deleted.', true);
        await loadEventsForClub();
    } catch (error) {
        console.warn('Unable to delete event.', error);
        setEventStatus('Unable to delete event.', false);
    }
}

function startEventEdit(eventId) {
    const event = cachedEvents.find(item => String(item.id) === String(eventId));
    if (!event) return;

    editEventId = event.id;
    selectedClubId = String(event.clubId ?? selectedClubId ?? '');

    const clubSelect = document.getElementById('eventClubSelect');
    clubSelect.value = selectedClubId;
    clubSelect.disabled = true;

    document.getElementById('eventTitle').value = event.title || '';
    document.getElementById('eventLocation').value = event.location || '';
    document.getElementById('eventCapacity').value = event.capacity ?? '';
    document.getElementById('eventStartTime').value = toInputDateTime(event.startTime);
    document.getElementById('eventEndTime').value = toInputDateTime(event.endTime);
    document.getElementById('eventDescription').value = event.description || '';

    document.getElementById('eventFormTitle').textContent = 'Update Event';
    document.getElementById('eventFormSubmit').textContent = 'Update Event';
    document.getElementById('eventFormCancel').classList.remove('d-none');
    setEventStatus('Editing event details.', true);
}

function resetEventForm(keepSelection = false) {
    editEventId = null;
    const form = document.getElementById('eventForm');
    const clubSelect = document.getElementById('eventClubSelect');

    form.reset();
    if (keepSelection && selectedClubId) {
        clubSelect.value = selectedClubId;
    }
    clubSelect.disabled = false;
    document.getElementById('eventFormTitle').textContent = 'Create Event';
    document.getElementById('eventFormSubmit').textContent = 'Create Event';
    document.getElementById('eventFormCancel').classList.add('d-none');
    setEventStatus('', false);
}

function setClubStatus(message, isSuccess = false) {
    const statusEl = document.getElementById('clubFormStatus');
    if (!statusEl) return;
    statusEl.textContent = message || '';
    statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
    statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
}

function setEventStatus(message, isSuccess = false) {
    const statusEl = document.getElementById('eventFormStatus');
    if (!statusEl) return;
    statusEl.textContent = message || '';
    statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
    statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
}

function normalizeDateTimeInput(value) {
    if (!value) {
        return null;
    }
    return value.length === 16 ? `${value}:00` : value;
}

function toInputDateTime(value) {
    if (!value) {
        return '';
    }
    return value.length >= 16 ? value.substring(0, 16) : value;
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}

function validateEventPayload(payload) {
    if (!payload.title) {
        return 'Event title is required.';
    }
    if (!payload.location) {
        return 'Event location is required.';
    }
    const capacity = payload.capacity;
    if (capacity === null || capacity === undefined || Number.isNaN(capacity) || capacity <= 0) {
        return 'Capacity must be greater than 0.';
    }
    if (!payload.startTime) {
        return 'Start time is required.';
    }

    const startDate = parseLocalDateTime(payload.startTime);
    if (!startDate) {
        return 'Start time is invalid.';
    }

    const now = new Date();
    now.setSeconds(0, 0);
    if (startDate < now) {
        return 'Start time must be in the future.';
    }

    if (payload.endTime) {
        const endDate = parseLocalDateTime(payload.endTime);
        if (!endDate) {
            return 'End time is invalid.';
        }
        if (endDate < startDate) {
            return 'End time must be after the start time.';
        }
    }

    return '';
}

function parseLocalDateTime(value) {
    if (!value) {
        return null;
    }
    const match = value.match(/^(\d{4})-(\d{2})-(\d{2})[T ](\d{2}):(\d{2})(?::(\d{2}))?$/);
    if (!match) {
        return null;
    }
    const year = Number(match[1]);
    const month = Number(match[2]) - 1;
    const day = Number(match[3]);
    const hour = Number(match[4]);
    const minute = Number(match[5]);
    const second = Number(match[6] || 0);
    const date = new Date(year, month, day, hour, minute, second, 0);
    if (Number.isNaN(date.getTime())) {
        return null;
    }
    if (date.getFullYear() !== year ||
        date.getMonth() !== month ||
        date.getDate() !== day ||
        date.getHours() !== hour ||
        date.getMinutes() !== minute) {
        return null;
    }
    return date;
}
