export function renderManageEvents() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Manage Events</h2>
                    <p class="text-muted mb-0">Review and approve event submissions.</p>
                </div>
            </div>
            <div class="card p-4 text-muted">Event management tools coming soon.</div>
        </div>
    `;
}
