export function renderAdminDashboard(user) {
    const appRoot = document.getElementById('app-root');
    const displayName = user?.username || user?.fullName || user?.email || 'User';

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Admin Dashboard</h2>
                    <p class="text-muted mb-0">Manage clubs and event approvals.</p>
                    <p class="small text-muted mb-0">Signed in as ${displayName}</p>
                </div>
            </div>
            <div class="row g-3">
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">Club approvals</div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">Event queue</div>
                </div>
                <div class="col-md-4">
                    <div class="card p-3 text-center text-muted">Participation stats</div>
                </div>
            </div>
        </div>
    `;
}
