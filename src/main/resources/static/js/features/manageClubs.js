let cachedClubs = [];
let editClubId = null;

export function renderManageClubs() {
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Manage Clubs</h2>
                    <p class="text-muted mb-0">Create, update, or deactivate clubs.</p>
                </div>
            </div>

            <div class="row g-4">
                <div class="col-lg-4">
                    <div class="card p-4 h-100">
                        <h5 class="fw-semibold mb-3" id="clubFormTitle">Create Club</h5>
                        <form id="clubForm">
                            <div class="mb-3">
                                <label class="form-label" for="clubName">Club Name</label>
                                <input type="text" class="form-control" id="clubName" required>
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="clubCategory">Category</label>
                                <input type="text" class="form-control" id="clubCategory" placeholder="e.g. Sports, Tech">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="clubDescription">Description</label>
                                <textarea class="form-control" id="clubDescription" rows="4"></textarea>
                            </div>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary flex-grow-1" id="clubFormSubmit">Create Club</button>
                                <button type="button" class="btn btn-outline-secondary d-none" id="clubFormCancel">Cancel</button>
                            </div>
                            <div class="small mt-2" id="clubFormStatus"></div>
                        </form>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card p-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                            <h5 class="fw-semibold mb-0">Existing Clubs</h5>
                            <span class="text-muted small" id="clubCount">Loading...</span>
                        </div>
                        <div id="clubTableWrap" class="table-responsive"></div>
                    </div>
                </div>
            </div>
        </div>
    `;

    bindClubForm();
    loadClubs();
}

function bindClubForm() {
    const form = document.getElementById('clubForm');
    const cancelBtn = document.getElementById('clubFormCancel');

    form.addEventListener('submit', (event) => {
        event.preventDefault();
        handleSubmit();
    });

    cancelBtn.addEventListener('click', () => resetForm());
}

async function loadClubs() {
    setClubStatus('');
    try {
        const response = await apiRequest('/api/admin/clubs', { method: 'GET' });
        if (!response.ok) {
            setClubStatus('Unable to load clubs.', false);
            return;
        }
        cachedClubs = await response.json();
        renderClubTable();
    } catch (error) {
        setClubStatus('Unable to load clubs.', false);
    }
}

function renderClubTable() {
    const tableWrap = document.getElementById('clubTableWrap');
    const clubCount = document.getElementById('clubCount');

    if (!cachedClubs || cachedClubs.length === 0) {
        clubCount.textContent = '0 clubs';
        tableWrap.innerHTML = `<div class="text-muted">No clubs yet. Create the first one.</div>`;
        return;
    }

    clubCount.textContent = `${cachedClubs.length} club${cachedClubs.length === 1 ? '' : 's'}`;

    tableWrap.innerHTML = `
        <table class="table table-sm align-middle">
            <thead>
                <tr>
                    <th>Name</th>
                    <th>Category</th>
                    <th>Status</th>
                    <th class="text-end">Actions</th>
                </tr>
            </thead>
            <tbody>
                ${cachedClubs.map(club => `
                    <tr>
                        <td>
                            <div class="fw-semibold">${escapeHtml(club.name)}</div>
                            <div class="text-muted small">${escapeHtml(club.description || 'No description')}</div>
                        </td>
                        <td>${escapeHtml(club.category || 'Uncategorized')}</td>
                        <td>
                            <span class="badge ${club.active ? 'bg-success-subtle text-success' : 'bg-secondary-subtle text-secondary'}">
                                ${club.active ? 'Active' : 'Inactive'}
                            </span>
                        </td>
                        <td class="text-end">
                            <div class="btn-group btn-group-sm">
                                <button class="btn btn-outline-primary" data-action="edit" data-id="${club.id}">Edit</button>
                                <button class="btn btn-outline-danger" data-action="deactivate" data-id="${club.id}" ${club.active ? '' : 'disabled'}>Deactivate</button>
                            </div>
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;

    tableWrap.querySelectorAll('button[data-action="edit"]').forEach(btn => {
        btn.addEventListener('click', () => startEdit(btn.dataset.id));
    });

    tableWrap.querySelectorAll('button[data-action="deactivate"]').forEach(btn => {
        btn.addEventListener('click', () => handleDeactivate(btn.dataset.id));
    });
}

async function handleSubmit() {
    const nameInput = document.getElementById('clubName');
    const categoryInput = document.getElementById('clubCategory');
    const descriptionInput = document.getElementById('clubDescription');

    const payload = {
        name: nameInput.value.trim(),
        category: categoryInput.value.trim(),
        description: descriptionInput.value.trim()
    };

    if (!payload.name) {
        setClubStatus('Club name is required.', false);
        return;
    }

    const isEdit = Boolean(editClubId);
    const url = isEdit ? `/api/admin/clubs/${editClubId}` : '/api/admin/clubs';
    const method = isEdit ? 'PUT' : 'POST';

    try {
        const response = await apiRequest(url, {
            method,
            body: JSON.stringify(payload)
        });
        const data = await safeJson(response);

        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to save club.', false);
            return;
        }

        setClubStatus(data?.message || (isEdit ? 'Club updated.' : 'Club created.'), true);
        resetForm();
        await loadClubs();
    } catch (error) {
        setClubStatus('Unable to save club.', false);
    }
}

function startEdit(clubId) {
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club) return;

    editClubId = club.id;
    document.getElementById('clubName').value = club.name || '';
    document.getElementById('clubCategory').value = club.category || '';
    document.getElementById('clubDescription').value = club.description || '';
    document.getElementById('clubFormTitle').textContent = 'Update Club';
    document.getElementById('clubFormSubmit').textContent = 'Update Club';
    document.getElementById('clubFormCancel').classList.remove('d-none');
    setClubStatus('Editing club details.', true);
}

function resetForm() {
    editClubId = null;
    document.getElementById('clubForm').reset();
    document.getElementById('clubFormTitle').textContent = 'Create Club';
    document.getElementById('clubFormSubmit').textContent = 'Create Club';
    document.getElementById('clubFormCancel').classList.add('d-none');
    setClubStatus('');
}

async function handleDeactivate(clubId) {
    const club = cachedClubs.find(item => String(item.id) === String(clubId));
    if (!club || !club.active) return;

    const confirmed = globalThis.confirm(`Are you sure you want to deactivate "${club.name}"?`);
    if (!confirmed) return;

    try {
        const response = await apiRequest(`/api/admin/clubs/${clubId}`, {
            method: 'DELETE'
        });
        const data = await safeJson(response);
        if (!response.ok) {
            setClubStatus(data?.message || 'Unable to deactivate club.', false);
            return;
        }
        setClubStatus(data?.message || 'Club deactivated.', true);
        await loadClubs();
    } catch (error) {
        setClubStatus('Unable to deactivate club.', false);
    }
}

function setClubStatus(message, isSuccess = false) {
    const statusEl = document.getElementById('clubFormStatus');
    if (!statusEl) return;
    statusEl.textContent = message || '';
    statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
    statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
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

async function safeJson(response) {
    try {
        return await response.json();
    } catch (error) {
        return null;
    }
}

function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value ?? '';
    return div.innerHTML;
}
