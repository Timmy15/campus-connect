export function renderBrowseClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Browse Clubs</h2>
                    <p class="text-muted mb-0">Find clubs that match your interests.</p>
                </div>
            </div>

            <div class="card p-4">
                <div class="d-flex justify-content-between align-items-center mb-3">
                    <h5 class="fw-semibold mb-0">Available Clubs</h5>
                    <span class="text-muted small" id="clubBrowseCount">Loading...</span>
                </div>
                <div id="clubBrowseGrid" class="row g-3"></div>
                <div id="clubBrowseEmpty" class="text-muted d-none">No active clubs yet.</div>
            </div>
        </div>
    `;

    loadClubs();
}

async function loadClubs() {
    const countEl = document.getElementById('clubBrowseCount');
    const grid = document.getElementById('clubBrowseGrid');
    const emptyEl = document.getElementById('clubBrowseEmpty');

    try {
        const response = await apiRequest('/api/clubs', { method: 'GET' });
        if (!response.ok) {
            countEl.textContent = 'Unable to load clubs.';
            emptyEl.classList.remove('d-none');
            return;
        }
        const clubs = await response.json();
        countEl.textContent = `${clubs.length} club${clubs.length === 1 ? '' : 's'}`;
        if (!clubs.length) {
            emptyEl.classList.remove('d-none');
            return;
        }

        grid.innerHTML = clubs.map(club => `
            <div class="col-md-6 col-xl-4">
                <div class="card h-100 p-3">
                    <div class="d-flex justify-content-between align-items-start mb-2">
                        <h6 class="fw-semibold mb-0">${escapeHtml(club.name)}</h6>
                        <span class="badge bg-primary-subtle text-primary">${escapeHtml(club.category || 'Club')}</span>
                    </div>
                    <p class="text-muted small mb-0">${escapeHtml(club.description || 'No description yet.')}</p>
                </div>
            </div>
        `).join('');
    } catch (error) {
        countEl.textContent = 'Unable to load clubs.';
        emptyEl.classList.remove('d-none');
    }
}

async function apiRequest(url, options = {}) {
    const token = localStorage.getItem('cc.token');
    const headers = {
        'Content-Type': 'application/json',
        ...(options.headers || {})
    };
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    return fetch(url, {
        ...options,
        headers
    });
}

function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value ?? '';
    return div.innerHTML;
}
