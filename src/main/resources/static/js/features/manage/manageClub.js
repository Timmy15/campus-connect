import { apiRequest, safeJson } from '../../utils/api.js';
import { escapeHtml } from '../../utils/dom.js';
import { confirmModal } from '../../utils/modal.js';
import ManageState from './manageState.js';

const ManageClub = (() => {
    let onClubsUpdated = null;

    function setCallbacks(callbacks = {}) {
        onClubsUpdated = typeof callbacks.onClubsUpdated === 'function'
            ? callbacks.onClubsUpdated
            : null;
    }

    function bindClubForm() {
        const form = document.getElementById('clubForm');
        const cancelBtn = document.getElementById('clubFormCancel');
        if (!form || !cancelBtn) {
            return;
        }

        form.addEventListener('submit', (event) => {
            event.preventDefault();
            handleSubmit();
        });

        cancelBtn.addEventListener('click', () => resetForm());
    }

    async function loadClubs() {
        try {
            const response = await apiRequest('/api/admin/clubs');
            if (!response.ok) {
                setClubStatus('Unable to load clubs.', false);
                ManageState.setClubs([]);
                renderClubTable();
                notifyClubsUpdated();
                return;
            }
            const clubs = await response.json();
            ManageState.setClubs(clubs);
            renderClubTable();
            notifyClubsUpdated();
        } catch (error) {
            console.warn('Unable to load clubs.', error);
            setClubStatus('Unable to load clubs.', false);
            ManageState.setClubs([]);
            renderClubTable();
            notifyClubsUpdated();
        }
    }

    function notifyClubsUpdated() {
        if (onClubsUpdated) {
            onClubsUpdated(ManageState.state.cachedClubs);
        }
    }

    function renderClubTable() {
        const tableWrap = document.getElementById('clubTableWrap');
        const clubCount = document.getElementById('clubCount');

        if (!tableWrap || !clubCount) {
            return;
        }

        const clubs = ManageState.state.cachedClubs;
        if (!clubs || clubs.length === 0) {
            clubCount.textContent = '0 clubs';
            tableWrap.innerHTML = `<div class="text-muted">No clubs yet. Create the first one.</div>`;
            return;
        }

        clubCount.textContent = `${clubs.length} club${clubs.length === 1 ? '' : 's'}`;

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
                    ${clubs.map(club => `
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
                                    <button class="btn btn-outline-primary" data-action="edit" data-id="${club.id}"><i class="bi bi-pencil-square me-1"></i>Edit</button>
                                    ${club.active
                                        ? `<button class="btn btn-outline-danger" data-action="deactivate" data-id="${club.id}"><i class="bi bi-slash-circle me-1"></i>Deactivate</button>`
                                        : `<button class="btn btn-outline-success" data-action="activate" data-id="${club.id}"><i class="bi bi-check-circle me-1"></i>Activate</button>`
                                    }
                                    <button class="btn btn-outline-danger" data-action="delete" data-id="${club.id}"><i class="bi bi-trash me-1"></i>Delete</button>
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

        tableWrap.querySelectorAll('button[data-action="activate"]').forEach(btn => {
            btn.addEventListener('click', () => handleActivate(btn.dataset.id));
        });

        tableWrap.querySelectorAll('button[data-action="delete"]').forEach(btn => {
            btn.addEventListener('click', () => handleDeleteClub(btn.dataset.id));
        });
    }

    async function handleSubmit() {
        setClubStatus('');
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

        const isEdit = ManageState.state.editClubId !== null;
        const url = isEdit ? `/api/admin/clubs/${ManageState.state.editClubId}` : '/api/admin/clubs';
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
            console.warn('Unable to save club.', error);
            setClubStatus('Unable to save club.', false);
        }
    }

    function startEdit(clubId) {
        const club = ManageState.state.cachedClubs.find(item => String(item.id) === String(clubId));
        if (!club) return;

        ManageState.setEditClubId(club.id);
        document.getElementById('clubName').value = club.name || '';
        document.getElementById('clubCategory').value = club.category || '';
        document.getElementById('clubDescription').value = club.description || '';
        document.getElementById('clubFormTitle').textContent = 'Update Club';
        document.getElementById('clubFormSubmit').textContent = 'Update Club';
        document.getElementById('clubFormCancel').classList.remove('d-none');
        setClubStatus('Editing club details.', true);
    }

    function resetForm() {
        ManageState.setEditClubId(null);
        document.getElementById('clubForm').reset();
        document.getElementById('clubFormTitle').textContent = 'Create Club';
        document.getElementById('clubFormSubmit').textContent = 'Create Club';
        document.getElementById('clubFormCancel').classList.add('d-none');
    }

    async function handleDeactivate(clubId) {
        setClubStatus('');
        const club = ManageState.state.cachedClubs.find(item => String(item.id) === String(clubId));
        if (!club?.active) return;

        const confirmed = await confirmModal({
            title: 'Deactivate club',
            message: `Are you sure you want to deactivate "${club.name}"?`,
            confirmText: 'Deactivate',
            confirmVariant: 'danger',
            iconClass: 'bi bi-slash-circle'
        });
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
            console.warn('Unable to deactivate club.', error);
            setClubStatus('Unable to deactivate club.', false);
        }
    }

    async function handleActivate(clubId) {
        setClubStatus('');
        const club = ManageState.state.cachedClubs.find(item => String(item.id) === String(clubId));
        if (!club || club.active) return;

        try {
            const response = await apiRequest(`/api/admin/clubs/${clubId}/activate`, {
                method: 'PUT'
            });
            const data = await safeJson(response);
            if (!response.ok) {
                setClubStatus(data?.message || 'Unable to activate club.', false);
                return;
            }
            setClubStatus(data?.message || 'Club activated.', true);
            await loadClubs();
        } catch (error) {
            console.warn('Unable to activate club.', error);
            setClubStatus('Unable to activate club.', false);
        }
    }

    async function handleDeleteClub(clubId) {
        setClubStatus('');
        const club = ManageState.state.cachedClubs.find(item => String(item.id) === String(clubId));
        if (!club) {
            return;
        }

        const confirmed = await confirmModal({
            title: 'Delete club',
            message: `Are you sure you want to delete "${club.name}"?`,
            confirmText: 'Delete',
            confirmVariant: 'danger',
            iconClass: 'bi bi-trash'
        });
        if (!confirmed) {
            return;
        }

        try {
            const response = await apiRequest(`/api/admin/clubs/${clubId}/delete`, {
                method: 'DELETE'
            });
            const data = await safeJson(response);
            if (!response.ok) {
                setClubStatus(data?.message || 'Unable to delete club.', false);
                return;
            }

            if (ManageState.state.editClubId && String(ManageState.state.editClubId) === String(clubId)) {
                resetForm();
            }

            setClubStatus(data?.message || 'Club deleted.', true);
            await loadClubs();
        } catch (error) {
            console.warn('Unable to delete club.', error);
            setClubStatus('Unable to delete club.', false);
        }
    }

    function setClubStatus(message, isSuccess = false) {
        const statusEl = document.getElementById('clubFormStatus');
        if (!statusEl) return;
        statusEl.textContent = message || '';
        statusEl.classList.toggle('text-success', Boolean(message) && isSuccess);
        statusEl.classList.toggle('text-danger', Boolean(message) && !isSuccess);
    }

    return {
        setCallbacks,
        bindClubForm,
        loadClubs
    };
})();

export default ManageClub;
