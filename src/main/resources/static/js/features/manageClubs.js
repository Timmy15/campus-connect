export function renderManageClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Manage Clubs</h2>
                    <p class="text-muted mb-0">Create, update, or deactivate clubs.</p>
                </div>
            </div>
            <div class="card p-4 text-muted">Club management tools coming soon.</div>
        </div>
    `;
}
