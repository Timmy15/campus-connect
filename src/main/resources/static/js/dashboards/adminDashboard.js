import { escapeHtml } from '../utils/dom.js';
import AdminDashboardState from './admin/adminDashboardState.js';
import DashboardUi from './admin/dashboardUi.js';
import ParticipationDashboard from './admin/participationDashboard.js';
import RegistrationsTable from './admin/registrationsTable.js';
import UserDrilldownTable from './admin/userDrilldownTable.js';

export function renderAdminDashboard(user) {
    AdminDashboardState.reset();
    const appRoot = document.getElementById('app-root');
    const displayName = user?.username || user?.fullName || user?.email || 'User';

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Admin Dashboard</h2>
                    <p class="text-muted mb-0">Review event registrations and participation trends.</p>
                    <p class="small text-muted mb-0">Signed in as ${escapeHtml(displayName)}</p>
                </div>
            </div>

            <div class="card p-4 mb-4 admin-participation-card" id="adminParticipationCard">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Participation dashboard</h5>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-sm btn-primary" id="adminLoadParticipation">View dashboard</button>
                        <button class="btn btn-sm btn-outline-secondary" data-action="toggle-card" data-target="adminParticipationBody" aria-controls="adminParticipationBody" aria-expanded="true" title="Collapse">
                            <i class="bi bi-chevron-up"></i>
                        </button>
                    </div>
                </div>
                <div id="adminParticipationBody">
                    <div class="small text-muted" id="adminParticipationStatus">Click to load participation charts.</div>
                    <div id="adminParticipationEmpty" class="text-muted d-none mt-3">No data available.</div>

                    <div id="adminParticipationCharts" class="row g-3 mt-3 d-none">
                        <div class="col-lg-6">
                            <div class="admin-chart-card h-100">
                                <div class="d-flex flex-column gap-1">
                                    <h6 class="fw-semibold mb-0">Registrations per event</h6>
                                    <span class="text-muted small">Ranked by registrations. Hover for event + club names.</span>
                                </div>
                                <div class="admin-chart-canvas">
                                    <canvas id="adminEventRegistrationsChart" aria-label="Registrations per event chart" role="img"></canvas>
                                </div>
                            </div>
                        </div>
                        <div class="col-lg-6">
                            <div class="admin-chart-card h-100">
                                <div class="d-flex flex-column gap-1">
                                    <h6 class="fw-semibold mb-0">Top clubs by total registrations</h6>
                                    <span class="text-muted small">Ranked by registrations. Hover for club names.</span>
                                </div>
                                <div class="admin-chart-canvas">
                                    <canvas id="adminTopClubsChart" aria-label="Top clubs by total registrations chart" role="img"></canvas>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="card p-4" id="adminRegistrationsCard">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Event registrations</h5>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-sm btn-primary" id="adminLoadRegistrations">Load registrations</button>
                        <button class="btn btn-sm btn-outline-secondary" data-action="toggle-card" data-target="adminRegistrationsBody" aria-controls="adminRegistrationsBody" aria-expanded="true" title="Collapse">
                            <i class="bi bi-chevron-up"></i>
                        </button>
                    </div>
                </div>
                <div id="adminRegistrationsBody">
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
            </div>

            <div class="card p-4 mt-4 d-none" id="adminEventDetails">
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-2 gap-2">
                    <h5 class="fw-semibold mb-0">Registrations for <span id="adminEventDetailsTitle">Event</span></h5>
                    <div class="d-flex flex-wrap gap-2">
                        <button class="btn btn-sm btn-outline-secondary" id="adminEventDetailsRefresh">Refresh</button>
                        <button class="btn btn-sm btn-outline-secondary" data-action="toggle-card" data-target="adminEventDetailsBody" aria-controls="adminEventDetailsBody" aria-expanded="true" title="Collapse">
                            <i class="bi bi-chevron-up"></i>
                        </button>
                    </div>
                </div>
                <div id="adminEventDetailsBody">
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
        </div>
    `;

    RegistrationsTable.setEventSelectHandler((eventId, eventTitle) => {
        UserDrilldownTable.loadEventRegistrationsForEvent(eventId, eventTitle);
    });
    bindDashboardActions();
    ParticipationDashboard.bindActions();
    DashboardUi.bindCardToggleActions();
}

function bindDashboardActions() {
    const loadButton = document.getElementById('adminLoadRegistrations');
    if (loadButton) {
        loadButton.addEventListener('click', () => RegistrationsTable.refreshEventsTable({ silent: false }));
    }

    const refreshButton = document.getElementById('adminEventDetailsRefresh');
    if (refreshButton) {
        refreshButton.addEventListener('click', () => {
            const { selectedEventId, selectedEventTitle } = AdminDashboardState.state;
            if (selectedEventId) {
                UserDrilldownTable.loadEventRegistrationsForEvent(selectedEventId, selectedEventTitle);
            }
        });
    }
}
