import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

let registrationsTable = null;
let detailTable = null;

const state = {
    events: [],
    registrations: [],
    selectedEventId: null,
    selectedEventTitle: '',
    eventsRequestId: 0,
    detailsRequestId: 0
};

export function renderAdminDashboard(user) {
    resetDashboardState();
    const appRoot = document.getElementById('app-root');
    const displayName = user?.username || user?.fullName || user?.email || 'User';

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Admin Dashboard</h2>
                    <p class="text-muted mb-0">Review event registrations.</p>
                    <p class="small text-muted mb-0">Signed in as ${escapeHtml(displayName)}</p>
                </div>
            </div>

            <div class="card p-4">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Event registrations</h5>
                    <button class="btn btn-sm btn-primary" id="adminLoadRegistrations">Load registrations</button>
                </div>
                <div class="small text-muted" id="adminRegistrationsStatus">Click to load event registration data.</div>
                <div id="adminRegistrationsEmpty" class="text-muted d-none mt-3">No data available.</div>

                <div id="adminRegistrationsTableWrapper" class="mt-3 d-none">
                    <div class="table-responsive">
                        <table class="table table-sm align-middle w-100" id="adminRegistrationsTable">
                            <thead>
                                <tr>
                                    <th>Event</th>
                                    <th>Club</th>
                                    <th>Start time</th>
                                    <th>Capacity</th>
                                    <th>Registered</th>
                                    <th>Remaining</th>
                                    <th>Fill %</th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>
            </div>

            <div class="card p-4 mt-4 d-none" id="adminEventDetails">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Registrations for <span id="adminEventDetailsTitle">Event</span></h5>
                    <button class="btn btn-sm btn-outline-secondary" id="adminEventDetailsRefresh">Refresh</button>
                </div>
                <div class="small text-muted" id="adminEventDetailsStatus">Select an event to view registrations.</div>
                <div id="adminEventDetailsEmpty" class="text-muted d-none mt-3">No registrations yet.</div>

                <div id="adminEventDetailsTableWrapper" class="mt-3 d-none">
                    <div class="table-responsive">
                        <table class="table table-sm align-middle w-100" id="adminEventDetailsTable">
                            <thead>
                                <tr>
                                    <th>Student</th>
                                    <th>Email</th>
                                    <th>Username</th>
                                    <th>Registered at</th>
                                    <th></th>
                                </tr>
                            </thead>
                            <tbody></tbody>
                        </table>
                    </div>
                </div>
            </div>
        </div>
    `;

    bindDashboardActions();
}

function resetDashboardState() {
    if (registrationsTable && typeof registrationsTable.destroy === 'function') {
        registrationsTable.destroy();
    }
    if (detailTable && typeof detailTable.destroy === 'function') {
        detailTable.destroy();
    }
    registrationsTable = null;
    detailTable = null;
    state.events = [];
    state.registrations = [];
    state.selectedEventId = null;
    state.selectedEventTitle = '';
    state.eventsRequestId = 0;
    state.detailsRequestId = 0;
}

function bindDashboardActions() {
    const loadButton = document.getElementById('adminLoadRegistrations');
    if (loadButton) {
        loadButton.addEventListener('click', () => refreshEventsTable({ silent: false }));
    }

    const refreshButton = document.getElementById('adminEventDetailsRefresh');
    if (refreshButton) {
        refreshButton.addEventListener('click', () => {
            if (state.selectedEventId) {
                loadEventRegistrationsForEvent(state.selectedEventId, state.selectedEventTitle);
            }
        });
    }
}

async function refreshEventsTable({ silent = false } = {}) {
    const requestId = ++state.eventsRequestId;
    const statusEl = document.getElementById('adminRegistrationsStatus');
    const emptyEl = document.getElementById('adminRegistrationsEmpty');
    const tableWrapper = document.getElementById('adminRegistrationsTableWrapper');
    const loadButton = document.getElementById('adminLoadRegistrations');

    if (!silent) {
        if (loadButton) {
            loadButton.disabled = true;
        }
        statusEl.textContent = 'Loading event registrations...';
        emptyEl.classList.add('d-none');
        tableWrapper.classList.add('d-none');
    }

    try {
        const response = await apiRequest('/api/events');
        const data = await safeJson(response);
        if (requestId !== state.eventsRequestId) {
            return;
        }
        if (!response.ok) {
            if (!silent) {
                statusEl.textContent = data?.message || 'Unable to load events.';
            }
            return;
        }

        state.events = Array.isArray(data) ? data : [];
        if (!state.events.length) {
            if (!silent) {
                statusEl.textContent = 'No event data available.';
                emptyEl.classList.remove('d-none');
            }
            return;
        }

        tableWrapper.classList.remove('d-none');
        renderEventsTable();
        if (registrationsTable) {
            registrationsTable.columns.adjust();
        }
        if (!silent) {
            statusEl.textContent = `Loaded ${state.events.length} event${state.events.length === 1 ? '' : 's'}.`;
        }
    } catch (error) {
        console.warn('Unable to load event registrations.', error);
        if (!silent) {
            statusEl.textContent = 'Unable to load event registrations.';
        }
    } finally {
        if (!silent && loadButton) {
            loadButton.disabled = false;
        }
    }
}

function renderEventsTable() {
    const table = document.getElementById('adminRegistrationsTable');
    if (!table) {
        return;
    }

    if (window.$ && window.$.fn?.DataTable) {
        const columns = [
            {
                data: 'title',
                render: (data) => escapeHtml(data || 'Event')
            },
            {
                data: 'clubName',
                render: (data) => escapeHtml(data || 'Club')
            },
            {
                data: 'startTime',
                render: (data) => formatEventDate(data)
            },
            {
                data: 'capacity',
                render: (data, type, row) => formatCapacity(row)
            },
            {
                data: 'registeredCount',
                render: (data, type, row) => resolveRegisteredCount(row)
            },
            {
                data: 'capacityRemaining',
                render: (data, type, row) => formatRemaining(row)
            },
            {
                data: null,
                render: (data, type, row) => formatFillPercent(row)
            }
        ];

        if (!registrationsTable) {
            registrationsTable = window.$(table).DataTable({
                data: state.events,
                columns,
                rowId: (row) => `event-${row.id}`,
                pageLength: 10,
                lengthChange: false,
                autoWidth: false,
                order: [[4, 'desc']]
            });
        } else {
            registrationsTable.clear();
            registrationsTable.rows.add(state.events).draw(false);
        }

        bindEventRowClicks();
        highlightSelectedEventRow();
        return;
    }

    const tbody = table.querySelector('tbody');
    tbody.innerHTML = state.events.map(event => {
        return `
            <tr data-event-id="${event.id}">
                <td>${escapeHtml(event.title || 'Event')}</td>
                <td>${escapeHtml(event.clubName || 'Club')}</td>
                <td>${formatEventDate(event.startTime)}</td>
                <td>${formatCapacity(event)}</td>
                <td>${resolveRegisteredCount(event)}</td>
                <td>${formatRemaining(event)}</td>
                <td>${formatFillPercent(event)}</td>
            </tr>
        `;
    }).join('');

    bindEventRowClicksFallback(table);
    highlightSelectedEventRowFallback(table);
}

function bindEventRowClicks() {
    const table = document.getElementById('adminRegistrationsTable');
    if (!table || !registrationsTable || !window.$) {
        return;
    }
    const $table = window.$(table);
    $table.off('click', 'tbody tr');
    $table.on('click', 'tbody tr', function () {
        const data = registrationsTable.row(this).data();
        if (!data) {
            return;
        }
        loadEventRegistrationsForEvent(data.id, data.title || 'Event');
    });
}

function bindEventRowClicksFallback(table) {
    if (table.dataset.clickBound) {
        return;
    }
    table.dataset.clickBound = 'true';
    table.querySelector('tbody')?.addEventListener('click', event => {
        const row = event.target.closest('tr[data-event-id]');
        if (!row) {
            return;
        }
        const eventId = row.dataset.eventId;
        const eventInfo = state.events.find(item => String(item.id) === String(eventId));
        if (!eventInfo) {
            return;
        }
        loadEventRegistrationsForEvent(eventInfo.id, eventInfo.title || 'Event');
    });
}

function highlightSelectedEventRow() {
    if (!registrationsTable || state.selectedEventId == null) {
        return;
    }
    registrationsTable.rows().every(function () {
        const data = this.data();
        const isSelected = data && String(data.id) === String(state.selectedEventId);
        const node = this.node();
        if (node) {
            node.classList.toggle('table-primary', Boolean(isSelected));
        }
    });
}

function highlightSelectedEventRowFallback(table) {
    if (!table || state.selectedEventId == null) {
        return;
    }
    table.querySelectorAll('tbody tr').forEach(row => {
        row.classList.toggle('table-primary', row.dataset.eventId === String(state.selectedEventId));
    });
}

async function loadEventRegistrationsForEvent(eventId, eventTitle) {
    const requestId = ++state.detailsRequestId;
    const detailsCard = document.getElementById('adminEventDetails');
    const titleEl = document.getElementById('adminEventDetailsTitle');
    const statusEl = document.getElementById('adminEventDetailsStatus');
    const emptyEl = document.getElementById('adminEventDetailsEmpty');
    const tableWrapper = document.getElementById('adminEventDetailsTableWrapper');
    const refreshButton = document.getElementById('adminEventDetailsRefresh');

    state.selectedEventId = eventId;
    state.selectedEventTitle = eventTitle || 'Event';

    if (titleEl) {
        titleEl.textContent = state.selectedEventTitle;
    }

    if (detailsCard) {
        detailsCard.classList.remove('d-none');
    }
    if (refreshButton) {
        refreshButton.disabled = true;
    }

    statusEl.textContent = `Loading registrations for ${state.selectedEventTitle}...`;
    emptyEl.classList.add('d-none');
    tableWrapper.classList.add('d-none');

    highlightSelectedEventRow();

    try {
        const response = await apiRequest(`/api/admin/events/${eventId}/registrations`);
        const data = await safeJson(response);
        if (requestId !== state.detailsRequestId) {
            return;
        }
        if (!response.ok) {
            statusEl.textContent = data?.message || 'Unable to load registrations.';
            return;
        }

        state.registrations = Array.isArray(data) ? data : [];
        if (!state.registrations.length) {
            statusEl.textContent = 'No registrations yet.';
            emptyEl.classList.remove('d-none');
            await refreshEventsTable({ silent: true });
            highlightSelectedEventRow();
            return;
        }

        tableWrapper.classList.remove('d-none');
        renderEventDetailsTable();
        if (detailTable) {
            detailTable.columns.adjust();
        }
        statusEl.textContent = `Loaded ${state.registrations.length} registration${state.registrations.length === 1 ? '' : 's'}.`;
        await refreshEventsTable({ silent: true });
        highlightSelectedEventRow();
    } catch (error) {
        console.warn('Unable to load event registrations.', error);
        statusEl.textContent = 'Unable to load registrations.';
    } finally {
        if (refreshButton) {
            refreshButton.disabled = false;
        }
    }
}

function renderEventDetailsTable() {
    const table = document.getElementById('adminEventDetailsTable');
    if (!table) {
        return;
    }

    if (window.$ && window.$.fn?.DataTable) {
        const columns = [
            {
                data: 'fullName',
                render: (data) => escapeHtml(data || 'Student')
            },
            {
                data: 'email',
                render: (data) => escapeHtml(data || '')
            },
            {
                data: 'username',
                render: (data) => escapeHtml(data || '-')
            },
            {
                data: 'registeredAt',
                render: (data) => formatEventDate(data)
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                render: (data, type, row) => {
                    const name = row?.fullName || 'Student';
                    const encodedName = encodeURIComponent(name);
                    return `<button class="btn btn-sm btn-outline-danger" data-registration-id="${row.registrationId}" data-student-name="${encodedName}">Unregister</button>`;
                }
            }
        ];

        if (!detailTable) {
            detailTable = window.$(table).DataTable({
                data: state.registrations,
                columns,
                rowId: (row) => `registration-${row.registrationId}`,
                pageLength: 8,
                lengthChange: false,
                autoWidth: false,
                order: [[3, 'desc']]
            });
        } else {
            detailTable.clear();
            detailTable.rows.add(state.registrations).draw(false);
        }

        bindDetailActions();
        return;
    }

    const tbody = table.querySelector('tbody');
    tbody.innerHTML = state.registrations.map(registration => {
        const studentName = registration.fullName || 'Student';
        const email = registration.email || '';
        const username = registration.username || '';
        const encodedName = encodeURIComponent(studentName);
        return `
            <tr>
                <td>${escapeHtml(studentName)}</td>
                <td>${escapeHtml(email)}</td>
                <td>${escapeHtml(username || '-')}</td>
                <td>${formatEventDate(registration.registeredAt)}</td>
                <td class="text-end">
                    <button class="btn btn-sm btn-outline-danger" data-registration-id="${registration.registrationId}" data-student-name="${encodedName}">Unregister</button>
                </td>
            </tr>
        `;
    }).join('');

    bindDetailActionsFallback(table);
}

function bindDetailActions() {
    const table = document.getElementById('adminEventDetailsTable');
    if (!table || !window.$) {
        return;
    }
    const $table = window.$(table);
    $table.off('click', 'button[data-registration-id]');
    $table.on('click', 'button[data-registration-id]', async function () {
        const registrationId = this.dataset.registrationId;
        if (!registrationId || !state.selectedEventId) {
            return;
        }
        const encodedName = this.dataset.studentName || '';
        const studentName = encodedName ? decodeURIComponent(encodedName) : 'this student';
        const confirmed = window.confirm(`Unregister ${studentName} from ${state.selectedEventTitle}?`);
        if (!confirmed) {
            return;
        }
        this.disabled = true;
        await unregisterStudent(state.selectedEventId, registrationId);
    });
}

function bindDetailActionsFallback(table) {
    if (table.dataset.actionBound) {
        return;
    }
    table.dataset.actionBound = 'true';
    table.addEventListener('click', async event => {
        const button = event.target.closest('button[data-registration-id]');
        if (!button) {
            return;
        }
        const registrationId = button.dataset.registrationId;
        if (!registrationId || !state.selectedEventId) {
            return;
        }
        const encodedName = button.dataset.studentName || '';
        const studentName = encodedName ? decodeURIComponent(encodedName) : 'this student';
        const confirmed = window.confirm(`Unregister ${studentName} from ${state.selectedEventTitle}?`);
        if (!confirmed) {
            return;
        }
        button.disabled = true;
        await unregisterStudent(state.selectedEventId, registrationId);
    });
}

async function unregisterStudent(eventId, registrationId) {
    const statusEl = document.getElementById('adminEventDetailsStatus');
    if (statusEl) {
        statusEl.textContent = 'Unregistering student...';
    }

    try {
        const response = await apiRequest(`/api/admin/events/${eventId}/registrations/${registrationId}`, {
            method: 'DELETE'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            if (statusEl) {
                statusEl.textContent = data?.message || 'Unable to unregister student.';
            }
            return;
        }

        if (statusEl) {
            statusEl.textContent = data?.message || 'Registration cancelled.';
        }

        await loadEventRegistrationsForEvent(eventId, state.selectedEventTitle);
    } catch (error) {
        console.warn('Unable to unregister student.', error);
        if (statusEl) {
            statusEl.textContent = 'Unable to unregister student.';
        }
    }
}

function resolveRegisteredCount(event) {
    return Number.isFinite(event?.registeredCount) ? event.registeredCount : 0;
}

function formatCapacity(event) {
    if (event && typeof event.capacity === 'number') {
        return event.capacity;
    }
    return 'N/A';
}

function formatRemaining(event) {
    if (event && typeof event.capacityRemaining === 'number') {
        return event.capacityRemaining;
    }
    if (event && typeof event.capacity === 'number') {
        return Math.max(event.capacity - resolveRegisteredCount(event), 0);
    }
    return 'N/A';
}

function formatFillPercent(event) {
    if (!event || typeof event.capacity !== 'number' || event.capacity <= 0) {
        return 'N/A';
    }
    const count = resolveRegisteredCount(event);
    const percent = Math.min(Math.round((count / event.capacity) * 100), 100);
    return `${percent}%`;
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}
