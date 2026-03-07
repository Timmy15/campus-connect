export function renderBrowseEvents() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Browse Events</h2>
                    <p class="text-muted mb-0">Explore upcoming campus events.</p>
                </div>
            </div>
            <div class="card p-4 text-muted">Event listings coming soon.</div>
        </div>
    `;
}
