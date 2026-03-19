import { apiRequest, safeJson } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

export function renderStudentDashboard(user) {
    const appRoot = document.getElementById('app-root');
    const displayName = user?.username || user?.fullName || user?.email || 'User';

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Student Dashboard</h2>
                    <p class="text-muted mb-0">Browse clubs and track your registrations.</p>
                    <p class="small text-muted mb-0">Signed in as ${escapeHtml(displayName)}</p>
                </div>
            </div>
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="card p-3 h-100">
                        <div class="d-flex align-items-start justify-content-between">
                            <div>
                                <div class="text-muted small">Upcoming events</div>
                                <div class="h4 mb-1" id="studentUpcomingCount">...</div>
                                <div class="small text-muted" id="studentUpcomingNext">Loading...</div>
                            </div>
                            <i class="bi bi-calendar-event fs-4 text-primary"></i>
                        </div>
                        <div class="mt-3">
                            <button class="btn btn-sm btn-outline-primary" id="studentUpcomingAction">Browse events</button>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 h-100">
                        <div class="d-flex align-items-start justify-content-between">
                            <div>
                                <div class="text-muted small">My registrations</div>
                                <div class="h4 mb-1" id="studentRegistrationCount">...</div>
                                <div class="small text-muted" id="studentRegistrationNext">Loading...</div>
                            </div>
                            <i class="bi bi-check2-circle fs-4 text-success"></i>
                        </div>
                        <div class="mt-3">
                            <button class="btn btn-sm btn-outline-success" id="studentRegistrationAction">View registrations</button>
                        </div>
                    </div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 h-100">
                        <div class="d-flex align-items-start justify-content-between">
                            <div>
                                <div class="text-muted small">Clubs I'm attending</div>
                                <div class="h4 mb-1" id="studentFollowCount">...</div>
                                <div class="small text-muted" id="studentFollowNext">Loading...</div>
                            </div>
                            <i class="bi bi-heart fs-4 text-danger"></i>
                        </div>
                        <div class="mt-3">
                            <button class="btn btn-sm btn-outline-secondary" id="studentFollowAction">Browse clubs</button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    `;

    bindDashboardActions();
    loadStudentDashboard();
}

function bindDashboardActions() {
    const browseEvents = document.getElementById('studentUpcomingAction');
    const registrations = document.getElementById('studentRegistrationAction');
    const browseClubs = document.getElementById('studentFollowAction');

    if (browseEvents) {
        browseEvents.addEventListener('click', () => {
            document.getElementById('nav-browse-events')?.click();
        });
    }
    if (registrations) {
        registrations.addEventListener('click', () => {
            document.getElementById('nav-my-registrations')?.click();
        });
    }
    if (browseClubs) {
        browseClubs.addEventListener('click', () => {
            document.getElementById('nav-browse-clubs')?.click();
        });
    }
}

async function loadStudentDashboard() {
    await Promise.all([loadUpcomingEvents(), loadRegistrationsSummary()]);
}

async function loadUpcomingEvents() {
    const countEl = document.getElementById('studentUpcomingCount');
    const nextEl = document.getElementById('studentUpcomingNext');

    try {
        const response = await apiRequest('/api/events');
        const data = await safeJson(response);
        if (!response.ok) {
            countEl.textContent = '--';
            nextEl.textContent = 'Unable to load upcoming events.';
            return;
        }

        const events = Array.isArray(data) ? data : [];
        countEl.textContent = String(events.length);

        if (!events.length) {
            nextEl.textContent = 'No upcoming events.';
            return;
        }

        const next = events[0];
        const nextLabel = `${next.title || 'Event'} - ${formatEventDate(next.startTime)}`;
        nextEl.textContent = nextLabel;
    } catch (error) {
        console.warn('Unable to load upcoming events.', error);
        countEl.textContent = '--';
        nextEl.textContent = 'Unable to load upcoming events.';
    }
}

async function loadRegistrationsSummary() {
    const ui = getRegistrationsSummaryUi();

    try {
        const response = await apiRequest('/api/student/registrations');
        const data = await safeJson(response);
        if (!response.ok) {
            setRegistrationsError(ui);
            return;
        }

        const registrations = Array.isArray(data) ? data : [];
        updateRegistrationsSummary(ui, registrations);
    } catch (error) {
        console.warn('Unable to load registrations.', error);
        setRegistrationsError(ui);
    }
}

function getRegistrationsSummaryUi() {
    return {
        countEl: document.getElementById('studentRegistrationCount'),
        nextEl: document.getElementById('studentRegistrationNext'),
        followCountEl: document.getElementById('studentFollowCount'),
        followNextEl: document.getElementById('studentFollowNext')
    };
}

function setRegistrationsError(ui) {
    if (ui.countEl) {
        ui.countEl.textContent = '--';
    }
    if (ui.nextEl) {
        ui.nextEl.textContent = 'Unable to load registrations.';
    }
    if (ui.followCountEl) {
        ui.followCountEl.textContent = '--';
    }
    if (ui.followNextEl) {
        ui.followNextEl.textContent = 'Unable to load clubs.';
    }
}

function updateRegistrationsSummary(ui, registrations) {
    const activeRegistrations = registrations.filter(isRegistrationActive);
    const count = activeRegistrations.length;
    if (ui.countEl) {
        ui.countEl.textContent = String(count);
    }

    if (count === 0) {
        setRegistrationsEmpty(ui);
        return;
    }

    setLatestRegistration(ui, activeRegistrations[0]);
    updateClubSummary(ui, activeRegistrations);
}

function setRegistrationsEmpty(ui) {
    if (ui.nextEl) {
        ui.nextEl.textContent = 'No active registrations.';
    }
    if (ui.followCountEl) {
        ui.followCountEl.textContent = '0';
    }
    if (ui.followNextEl) {
        ui.followNextEl.textContent = 'No clubs yet.';
    }
}

function setLatestRegistration(ui, registration) {
    if (!ui.nextEl) {
        return;
    }
    const label = `${registration.eventTitle || 'Event'} - ${formatEventDate(registration.startTime)}`;
    ui.nextEl.textContent = label;
}

function updateClubSummary(ui, registrations) {
    const clubNames = registrations
        .map(item => item.clubName?.trim?.())
        .filter(Boolean);
    const uniqueClubs = new Set(clubNames);

    if (ui.followCountEl) {
        ui.followCountEl.textContent = String(uniqueClubs.size);
    }
    if (ui.followNextEl) {
        const latestClub = clubNames[0];
        ui.followNextEl.textContent = latestClub ? `Latest: ${latestClub}` : 'Based on your registrations.';
    }
}

function formatEventDate(value) {
    if (!value) {
        return 'TBD';
    }
    return value.replace('T', ' ').substring(0, 16);
}

function isRegistrationActive(item) {
    return !isEventCompleted(item);
}

function isEventCompleted(item) {
    const now = new Date();
    const end = parseEventDate(item?.endTime);
    if (end) {
        return end < now;
    }
    const start = parseEventDate(item?.startTime);
    return Boolean(start && start < now);
}

function parseEventDate(value) {
    if (!value) {
        return null;
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return null;
    }
    return date;
}
