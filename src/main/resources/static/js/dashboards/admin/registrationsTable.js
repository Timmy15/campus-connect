import { apiRequest, safeJson } from '../../utils/api.js';
import { escapeHtml } from '../../utils/dom.js';
import AdminDashboardState from './adminDashboardState.js';
import DashboardUi from './dashboardUi.js';
import DashboardFormatters from './dashboardFormatters.js';

const RegistrationsTable = (() => {
    let onEventSelected = null;

    function setEventSelectHandler(handler) {
        onEventSelected = typeof handler === 'function' ? handler : null;
    }

    function getRegistrationsUi() {
        return {
            statusEl: document.getElementById('adminRegistrationsStatus'),
            emptyEl: document.getElementById('adminRegistrationsEmpty'),
            tableWrapper: document.getElementById('adminRegistrationsTableWrapper'),
            loadButton: document.getElementById('adminLoadRegistrations')
        };
    }

    function updateRegistrationsUi(ui, { status, showEmpty, showTable, disableButton } = {}) {
        if (!ui) {
            return;
        }
        if (typeof disableButton === 'boolean') {
            if (ui.loadButton) {
                ui.loadButton.disabled = disableButton;
            }
        }
        if (status !== undefined && ui.statusEl) {
            ui.statusEl.textContent = status;
        }
        if (typeof showEmpty === 'boolean') {
            ui.emptyEl?.classList.toggle('d-none', !showEmpty);
        }
        if (typeof showTable === 'boolean') {
            ui.tableWrapper?.classList.toggle('d-none', !showTable);
        }
    }

    async function refreshEventsTable({ silent = false } = {}) {
        const requestId = AdminDashboardState.nextEventsRequestId();
        const ui = silent ? null : getRegistrationsUi();
        updateRegistrationsUi(ui, {
            status: 'Loading event registrations...',
            showEmpty: false,
            showTable: false,
            disableButton: true
        });
        if (!silent) {
            DashboardUi.ensureCardBodyVisible('adminRegistrationsBody');
        }

        try {
            const response = await apiRequest('/api/events');
            const data = await safeJson(response);
            if (AdminDashboardState.isStaleEventsRequest(requestId)) {
                return;
            }
            if (!response.ok) {
                updateRegistrationsUi(ui, {
                    status: data?.message || 'Unable to load events.'
                });
                return;
            }

            const events = Array.isArray(data) ? data : [];
            AdminDashboardState.state.events = events;
            if (events.length === 0) {
                updateRegistrationsUi(ui, {
                    status: 'No event data available.',
                    showEmpty: true,
                    showTable: false
                });
                return;
            }

            updateRegistrationsUi(ui, { showEmpty: false, showTable: true });
            renderEventsTable();
            if (AdminDashboardState.getRegistrationsTable()) {
                AdminDashboardState.getRegistrationsTable().columns.adjust();
            }
            updateRegistrationsUi(ui, {
                status: `Loaded ${events.length} event${events.length === 1 ? '' : 's'}.`
            });
        } catch (error) {
            console.warn('Unable to load event registrations.', error);
            updateRegistrationsUi(ui, { status: 'Unable to load event registrations.' });
        } finally {
            updateRegistrationsUi(ui, { disableButton: false });
        }
    }

    function renderEventsTable() {
        const table = document.getElementById('adminRegistrationsTable');
        if (!table) {
            return;
        }
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
                render: (data) => DashboardFormatters.formatEventDate(data)
            },
            {
                data: 'capacity',
                render: (data, type, row) => DashboardFormatters.formatCapacity(row)
            },
            {
                data: 'registeredCount',
                render: (data, type, row) => DashboardFormatters.resolveRegisteredCount(row)
            },
            {
                data: 'capacityRemaining',
                render: (data, type, row) => DashboardFormatters.formatRemaining(row)
            },
            {
                data: null,
                render: (data, type, row) => DashboardFormatters.formatFillPercent(row)
            }
        ];

        const existingTable = AdminDashboardState.getRegistrationsTable();
        if (existingTable) {
            existingTable.clear();
            existingTable.rows.add(AdminDashboardState.state.events).draw(false);
        } else {
            const dataTable = globalThis.$(table).DataTable({
                data: AdminDashboardState.state.events,
                columns,
                rowId: (row) => `event-${row.id}`,
                pageLength: 10,
                lengthChange: false,
                autoWidth: false,
                order: [[4, 'desc']]
            });
            AdminDashboardState.setRegistrationsTable(dataTable);
        }

        bindEventRowClicks();
        highlightSelectedEventRow();
    }

    function bindEventRowClicks() {
        const table = document.getElementById('adminRegistrationsTable');
        const dataTable = AdminDashboardState.getRegistrationsTable();
        if (table && dataTable) {
            const $table = globalThis.$(table);
            $table.off('click', 'tbody tr');
            $table.on('click', 'tbody tr', function () {
                const data = dataTable.row(this).data();
                if (!data) {
                    return;
                }
                if (onEventSelected) {
                    onEventSelected(data.id, data.title || 'Event');
                }
            });
        }
    }

    function highlightSelectedEventRow() {
        const dataTable = AdminDashboardState.getRegistrationsTable();
        if (!dataTable || AdminDashboardState.state.selectedEventId == null) {
            return;
        }
        dataTable.rows().every(function () {
            const data = this.data();
            const isSelected = String(data?.id) === String(AdminDashboardState.state.selectedEventId);
            const node = this.node();
            if (node) {
                node.classList.toggle('table-primary', Boolean(isSelected));
            }
        });
    }

    function applyClubFilterToRegistrationsTable(clubName) {
        if (!clubName) {
            return;
        }
        const dataTable = AdminDashboardState.getRegistrationsTable();
        if (dataTable?.columns) {
            dataTable.columns().search('');
            const escaped = DashboardFormatters.escapeRegex(clubName);
            dataTable.column(1).search(`^${escaped}$`, true, false).draw();
            return;
        }
        const table = document.getElementById('adminRegistrationsTable');
        if (!table) {
            return;
        }
        const rows = table.querySelectorAll('tbody tr');
        rows.forEach(row => {
            const cells = row.querySelectorAll('td');
            const cellText = cells[1]?.textContent?.trim() || '';
            row.classList.toggle('d-none', cellText !== clubName);
        });
    }

    return {
        setEventSelectHandler,
        getRegistrationsUi,
        updateRegistrationsUi,
        refreshEventsTable,
        applyClubFilterToRegistrationsTable,
        highlightSelectedEventRow
    };
})();

export default RegistrationsTable;
