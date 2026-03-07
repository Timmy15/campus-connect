export function renderMyRegistrations() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">My Registrations</h2>
                    <p class="text-muted mb-0">Track your event registrations.</p>
                </div>
            </div>
            <div class="card p-4 text-muted">Registration history coming soon.</div>
        </div>
    `;
}
