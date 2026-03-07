export function renderStudentDashboard(user) {
    const appRoot = document.getElementById('app-root');
    const displayName = user?.username || user?.fullName || user?.email || 'User';

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Student Dashboard</h2>
                    <p class="text-muted mb-0">Browse clubs and track your registrations.</p>
                    <p class="small text-muted mb-0">Signed in as ${displayName}</p>
                </div>
            </div>
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">Upcoming events</div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">My registrations</div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">Clubs followed</div>
                </div>
            </div>
        </div>
    `;
}
