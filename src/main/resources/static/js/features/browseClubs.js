export function renderBrowseClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-3">
                <div class="col">
                    <h2 class="fw-bold">Browse Clubs</h2>
                    <p class="text-muted mb-0">Find clubs that match your interests.</p>
                </div>
            </div>
            <div class="card p-4 text-muted">Club listings coming soon.</div>
        </div>
    `;
}
