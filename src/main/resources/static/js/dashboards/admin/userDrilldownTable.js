import { apiRequest, safeJson } from '../../utils/api.js';
import { escapeHtml } from '../../utils/dom.js';
import { confirmModal } from '../../utils/modal.js';
import AdminDashboardState from './adminDashboardState.js';
import DashboardFormatters from './dashboardFormatters.js';
import DashboardUi from './dashboardUi.js';
import RegistrationsTable from './registrationsTable.js';

const UserDrilldownTable = (() => {
    async function loadEventRegistrationsForEvent(eventId, eventTitle) {
        const requestId = AdminDashboardState.nextDetailsRequestId();
        const ui = getEventDetailsUi();
        applySelectedEvent(eventId, eventTitle, ui);
        setDetailsLoading(ui);

        RegistrationsTable.highlightSelectedEventRow();

        try {
            const response = await apiRequest(`/api/admin/events/${eventId}/registrations`);
            const data = await safeJson(response);
            if (AdminDashboardState.isStaleDetailsRequest(requestId)) {
                return;
            }
            if (!response.ok) {
                setDetailsError(ui, data?.message);
                return;
            }

            const registrations = Array.isArray(data) ? data : [];
            updateEventDetails(ui, registrations);
            await RegistrationsTable.refreshEventsTable({ silent: true });
            RegistrationsTable.highlightSelectedEventRow();
        } catch (error) {
            console.warn('Unable to load event registrations.', error);
            setDetailsError(ui);
        } finally {
            setDetailsRefreshState(ui, false);
        }
    }

    function renderEventDetailsTable() {
        const table = document.getElementById('adminEventDetailsTable');
        if (!table) {
            return;
        }
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
                render: (data) => DashboardFormatters.formatEventDate(data)
            },
            {
                data: null,
                orderable: false,
                searchable: false,
                render: (data, type, row) => {
                    const name = row?.fullName || 'Student';
                    const encodedName = encodeURIComponent(name);
                    return `<button class="btn btn-sm btn-outline-danger" data-registration-id="${row.registrationId}" data-student-name="${encodedName}"><i class="bi bi-person-x me-1"></i>Unregister</button>`;
                }
            }
        ];

        const existingTable = AdminDashboardState.getDetailTable();
        if (existingTable) {
            existingTable.clear();
            existingTable.rows.add(AdminDashboardState.state.registrations).draw(false);
        } else {
            const dataTable = globalThis.$(table).DataTable({
                data: AdminDashboardState.state.registrations,
                columns,
                rowId: (row) => `registration-${row.registrationId}`,
                pageLength: 8,
                lengthChange: false,
                autoWidth: false,
                order: [[3, 'desc']]
            });
            AdminDashboardState.setDetailTable(dataTable);
        }

        bindDetailActions();
    }

    function bindDetailActions() {
        const table = document.getElementById('adminEventDetailsTable');
        if (table) {
            const $table = globalThis.$(table);
            $table.off('click', 'button[data-registration-id]');
            $table.on('click', 'button[data-registration-id]', async function () {
                const registrationId = this.dataset.registrationId;
                if (!registrationId || !AdminDashboardState.state.selectedEventId) {
                    return;
                }
                const encodedName = this.dataset.studentName || '';
                const studentName = encodedName ? decodeURIComponent(encodedName) : 'this student';
                const confirmed = await confirmModal({
                    title: 'Unregister student',
                    message: `Unregister ${studentName} from ${AdminDashboardState.state.selectedEventTitle}?`,
                    confirmText: 'Unregister',
                    confirmVariant: 'danger',
                    iconClass: 'bi bi-person-x'
                });
                if (!confirmed) {
                    return;
                }
                this.disabled = true;
                await unregisterStudent(AdminDashboardState.state.selectedEventId, registrationId);
            });
        }
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

            await loadEventRegistrationsForEvent(eventId, AdminDashboardState.state.selectedEventTitle);
        } catch (error) {
            console.warn('Unable to unregister student.', error);
            if (statusEl) {
                statusEl.textContent = 'Unable to unregister student.';
            }
        }
    }

    function getEventDetailsUi() {
        return {
            detailsCard: document.getElementById('adminEventDetails'),
            titleEl: document.getElementById('adminEventDetailsTitle'),
            statusEl: document.getElementById('adminEventDetailsStatus'),
            emptyEl: document.getElementById('adminEventDetailsEmpty'),
            tableWrapper: document.getElementById('adminEventDetailsTableWrapper'),
            refreshButton: document.getElementById('adminEventDetailsRefresh')
        };
    }

    function applySelectedEvent(eventId, eventTitle, ui) {
        AdminDashboardState.setSelectedEvent(eventId, eventTitle);

        if (ui.titleEl) {
            ui.titleEl.textContent = AdminDashboardState.state.selectedEventTitle;
        }
        if (ui.detailsCard) {
            ui.detailsCard.classList.remove('d-none');
        }
        DashboardUi.ensureCardBodyVisible('adminEventDetailsBody');
        setDetailsRefreshState(ui, true);
    }

    function setDetailsRefreshState(ui, disabled) {
        if (ui.refreshButton) {
            ui.refreshButton.disabled = disabled;
        }
    }

    function setDetailsLoading(ui) {
        setDetailsStatus(ui, `Loading registrations for ${AdminDashboardState.state.selectedEventTitle}...`);
        ui.emptyEl?.classList.add('d-none');
        ui.tableWrapper?.classList.add('d-none');
    }

    function setDetailsStatus(ui, message) {
        if (ui.statusEl) {
            ui.statusEl.textContent = message;
        }
    }

    function setDetailsError(ui, message) {
        setDetailsStatus(ui, message || 'Unable to load registrations.');
    }

    function updateEventDetails(ui, registrations) {
        AdminDashboardState.state.registrations = registrations;
        if (!registrations.length) {
            setDetailsStatus(ui, 'No registrations yet.');
            ui.emptyEl?.classList.remove('d-none');
            ui.tableWrapper?.classList.add('d-none');
            return;
        }

        ui.emptyEl?.classList.add('d-none');
        ui.tableWrapper?.classList.remove('d-none');
        renderEventDetailsTable();
        if (AdminDashboardState.getDetailTable()) {
            AdminDashboardState.getDetailTable().columns.adjust();
        }
        setDetailsStatus(ui, `Loaded ${registrations.length} registration${registrations.length === 1 ? '' : 's'}.`);
    }

    return {
        loadEventRegistrationsForEvent
    };
})();

export default UserDrilldownTable;
