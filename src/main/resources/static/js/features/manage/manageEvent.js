import { apiRequest, safeJson } from '../../utils/api.js';
import { escapeHtml } from '../../utils/dom.js';
import { confirmModal } from '../../utils/modal.js';
import ManageState from './manageState.js';

const ManageEvent = (() => {
    function bindEventForm() {
        const form = document.getElementById('eventForm');
        const cancelBtn = document.getElementById('eventFormCancel');
        const clubSelect = document.getElementById('eventClubSelect');

        if (!form || !cancelBtn || !clubSelect) {
            return;
        }

        form.addEventListener('submit', (event) => {
            event.preventDefault();
            handleEventSubmit();
        });

        cancelBtn.addEventListener('click', () => resetEventForm());
        clubSelect.addEventListener('change', () => {
            ManageState.setSelectedClubId(clubSelect.value || null);
            ManageState.setEditEventId(null);
            resetEventForm(true);
            loadEventsForClub();
        });
    }

    function handleClubsUpdated() {
        populateClubSelect();
    }

    function populateClubSelect() {
        const select = document.getElementById('eventClubSelect');
        if (!select) return;

        const previousSelection = ManageState.state.selectedClubId;
        const clubs = ManageState.state.cachedClubs;
        const options = clubs.map(club => `
            <option value="${club.id}">${escapeHtml(club.name)}</option>
        `).join('');

        select.innerHTML = `
            <option value="">Select a club</option>
            ${options}
        `;

        const hasPrevious = previousSelection && clubs.some(club => String(club.id) === String(previousSelection));

        if (hasPrevious) {
            select.value = previousSelection;
            ManageState.setSelectedClubId(previousSelection);
        } else if (clubs.length > 0) {
            const newSelection = String(clubs[0].id);
            ManageState.setSelectedClubId(newSelection);
            select.value = newSelection;
        } else {
            ManageState.setSelectedClubId(null);
        }

        if (!hasPrevious) {
            resetEventForm(true);
        }

        select.disabled = Boolean(ManageState.state.editEventId);
        setEventFormEnabled(clubs.length > 0);
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
        const isEditingEvent = ManageState.state.editEventId !== null;
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

        if (!countEl || !tableWrap) {
            return;
        }

        if (!ManageState.state.selectedClubId) {
            ManageState.setEvents([]);
            countEl.textContent = 'Select a club';
            tableWrap.innerHTML = `<div class="text-muted">Select a club to view events.</div>`;
            return;
        }

        try {
            const response = await apiRequest(`/api/admin/clubs/${ManageState.state.selectedClubId}/events`);
            if (!response.ok) {
                ManageState.setEvents([]);
                countEl.textContent = 'Unable to load events.';
                tableWrap.innerHTML = `<div class="text-muted">Unable to load events.</div>`;
                return;
            }
            const events = await response.json();
            ManageState.setEvents(events);
            renderEventTable();
        } catch (error) {
            console.warn('Unable to load events.', error);
            ManageState.setEvents([]);
            countEl.textContent = 'Unable to load events.';
            tableWrap.innerHTML = `<div class="text-muted">Unable to load events.</div>`;
        }
    }

    function renderEventTable() {
        const tableWrap = document.getElementById('eventTableWrap');
        const countEl = document.getElementById('eventCount');

        if (!tableWrap || !countEl) {
            return;
        }

        if (!ManageState.state.selectedClubId) {
            countEl.textContent = 'Select a club';
            tableWrap.innerHTML = `<div class="text-muted">Select a club to view events.</div>`;
            return;
        }

        const events = ManageState.state.cachedEvents;
        countEl.textContent = `${events.length} event${events.length === 1 ? '' : 's'}`;

        if (!events.length) {
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
                    ${events.map(event => `
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
                                    <button class="btn btn-outline-primary" data-action="edit-event" data-id="${event.id}"><i class="bi bi-pencil-square me-1"></i>Edit</button>
                                    <button class="btn btn-outline-danger" data-action="delete-event" data-id="${event.id}"><i class="bi bi-trash me-1"></i>Delete</button>
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

    async function handleEventSubmit() {
        setEventStatus('');

        if (!ManageState.state.selectedClubId) {
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

        const isEdit = ManageState.state.editEventId !== null;
        const url = isEdit ? `/api/admin/events/${ManageState.state.editEventId}` : `/api/admin/clubs/${ManageState.state.selectedClubId}/events`;
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
        const event = ManageState.state.cachedEvents.find(item => String(item.id) === String(eventId));
        if (!event) {
            return;
        }

        const confirmed = await confirmModal({
            title: 'Delete event',
            message: `Are you sure you want to delete "${event.title || 'this event'}"?`,
            confirmText: 'Delete',
            confirmVariant: 'danger',
            iconClass: 'bi bi-trash'
        });
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

            if (ManageState.state.editEventId && String(ManageState.state.editEventId) === String(eventId)) {
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
        const event = ManageState.state.cachedEvents.find(item => String(item.id) === String(eventId));
        if (!event) return;

        ManageState.setEditEventId(event.id);
        ManageState.setSelectedClubId(String(event.clubId ?? ManageState.state.selectedClubId ?? ''));

        const clubSelect = document.getElementById('eventClubSelect');
        clubSelect.value = ManageState.state.selectedClubId;
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
        ManageState.setEditEventId(null);
        const form = document.getElementById('eventForm');
        const clubSelect = document.getElementById('eventClubSelect');

        form.reset();
        if (keepSelection && ManageState.state.selectedClubId) {
            clubSelect.value = ManageState.state.selectedClubId;
        }
        clubSelect.disabled = false;
        document.getElementById('eventFormTitle').textContent = 'Create Event';
        document.getElementById('eventFormSubmit').textContent = 'Create Event';
        document.getElementById('eventFormCancel').classList.add('d-none');
        setEventStatus('', false);
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

    return {
        bindEventForm,
        handleClubsUpdated
    };
})();

export default ManageEvent;
