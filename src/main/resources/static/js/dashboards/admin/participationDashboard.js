import { apiRequest, safeJson } from '../../utils/api.js';
import AdminDashboardState from './adminDashboardState.js';
import ChartHelper from './chartHelper.js';
import DashboardUi from './dashboardUi.js';
import RegistrationsTable from './registrationsTable.js';
import UserDrilldownTable from './userDrilldownTable.js';

const ParticipationDashboard = (() => {
    function bindActions() {
        const loadButton = document.getElementById('adminLoadParticipation');
        if (loadButton) {
            loadButton.addEventListener('click', () => loadParticipationStats());
        }
    }

    function getParticipationUi() {
        return {
            statusEl: document.getElementById('adminParticipationStatus'),
            emptyEl: document.getElementById('adminParticipationEmpty'),
            chartsWrapper: document.getElementById('adminParticipationCharts'),
            loadButton: document.getElementById('adminLoadParticipation')
        };
    }

    function updateParticipationUi(ui, { status, showEmpty, showCharts, disableButton } = {}) {
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
        if (typeof showCharts === 'boolean') {
            ui.chartsWrapper?.classList.toggle('d-none', !showCharts);
        }
    }

    async function loadParticipationStats() {
        const requestId = AdminDashboardState.nextParticipationRequestId();
        const ui = getParticipationUi();
        updateParticipationUi(ui, {
            status: 'Loading participation statistics...',
            showEmpty: false,
            showCharts: false,
            disableButton: true
        });
        DashboardUi.ensureCardBodyVisible('adminParticipationBody');

        try {
            const response = await apiRequest('/api/admin/participation');
            const data = await safeJson(response);
            if (AdminDashboardState.isStaleParticipationRequest(requestId)) {
                return;
            }
            if (!response.ok) {
                updateParticipationUi(ui, {
                    status: data?.message || 'Unable to load participation statistics.'
                });
                return;
            }

            const eventStats = Array.isArray(data?.registrationsPerEvent) ? data.registrationsPerEvent : [];
            const clubStats = Array.isArray(data?.topClubs) ? data.topClubs : [];

            if (!eventStats.length && !clubStats.length) {
                updateParticipationUi(ui, {
                    status: 'No data available.',
                    showEmpty: true,
                    showCharts: false
                });
                return;
            }

            ChartHelper.renderParticipationCharts(eventStats, clubStats, {
                onEventSelected: handleEventStatSelection,
                onClubSelected: handleClubStatSelection
            });
            updateParticipationUi(ui, {
                status: 'Participation dashboard loaded.',
                showEmpty: false,
                showCharts: true
            });
        } catch (error) {
            console.warn('Unable to load participation statistics.', error);
            updateParticipationUi(ui, {
                status: 'Unable to load participation statistics.'
            });
        } finally {
            updateParticipationUi(ui, { disableButton: false });
        }
    }

    function handleEventStatSelection(stat) {
        if (!stat?.eventId) {
            return;
        }
        const title = stat.eventTitle || 'Event';
        UserDrilldownTable.loadEventRegistrationsForEvent(stat.eventId, title);
        DashboardUi.scrollCardIntoView('adminEventDetails');
    }

    async function handleClubStatSelection(stat) {
        if (!stat?.clubId) {
            return;
        }
        const clubName = stat.clubName || 'Club';
        const registrationsCard = document.getElementById('adminRegistrationsCard');
        if (registrationsCard) {
            registrationsCard.classList.remove('d-none');
        }
        if (!AdminDashboardState.state.events.length) {
            await RegistrationsTable.refreshEventsTable({ silent: false });
        }
        RegistrationsTable.applyClubFilterToRegistrationsTable(clubName);
        RegistrationsTable.updateRegistrationsUi(RegistrationsTable.getRegistrationsUi(), {
            status: `Filtered to ${clubName}. Click an event to view registered students.`,
            showEmpty: false,
            showTable: true
        });
        DashboardUi.scrollCardIntoView('adminRegistrationsCard');
    }

    return {
        bindActions,
        loadParticipationStats
    };
})();

export default ParticipationDashboard;
