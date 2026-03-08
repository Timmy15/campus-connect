import { apiRequest } from '../utils/api.js';
import { escapeHtml } from '../utils/dom.js';

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
                <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                    <h5 class="fw-semibold mb-0">Available Clubs</h5>
                    <span class="text-muted small" id="clubBrowseCount">Loading...</span>
                </div>
                <div class="row g-2 align-items-center mb-3">
                    <div class="col-md-6">
                        <div class="input-group input-group-sm">
                            <span class="input-group-text">Search</span>
                            <input type="text" class="form-control" id="clubSearchInput" placeholder="Search by club name">
                        </div>
                    </div>
                    <div class="col-md-4">
                        <select class="form-select form-select-sm" id="clubCategoryFilter">
                            <option value="">All categories</option>
                        </select>
                    </div>
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
    const searchInput = document.getElementById('clubSearchInput');
    const categorySelect = document.getElementById('clubCategoryFilter');

    try {
        const response = await apiRequest('/api/clubs');
        if (!response.ok) {
            countEl.textContent = 'Unable to load clubs.';
            emptyEl.classList.remove('d-none');
            return;
        }
        const clubs = await response.json();
        const clubState = {
            allClubs: clubs,
            filtered: clubs
        };

        searchInput.addEventListener('input', () => applyClubFilters(clubState));
        categorySelect.addEventListener('change', () => applyClubFilters(clubState));
        populateClubCategories(clubState, categorySelect);
        applyClubFilters(clubState);
    } catch (error) {
        console.warn('Failed to load clubs.', error);
        countEl.textContent = 'Unable to load clubs.';
        emptyEl.classList.remove('d-none');
    }
}

function applyClubFilters(state) {
    const countEl = document.getElementById('clubBrowseCount');
    const grid = document.getElementById('clubBrowseGrid');
    const emptyEl = document.getElementById('clubBrowseEmpty');
    const searchInput = document.getElementById('clubSearchInput');
    const categorySelect = document.getElementById('clubCategoryFilter');

    const term = (searchInput.value || '').trim().toLowerCase();
    const category = (categorySelect.value || '').trim().toLowerCase();

    const filtered = state.allClubs.filter(club => {
        const name = (club.name || '').toLowerCase();
        const clubCategory = normalizeCategory(club.category).toLowerCase();
        const matchesTerm = !term || name.includes(term);
        const matchesCategory = !category || clubCategory === category;
        return matchesTerm && matchesCategory;
    });

    state.filtered = filtered;
    countEl.textContent = `${filtered.length} club${filtered.length === 1 ? '' : 's'}`;
    if (!filtered.length) {
        grid.innerHTML = '';
        emptyEl.textContent = state.allClubs.length ? 'No clubs match your search.' : 'No active clubs yet.';
        emptyEl.classList.remove('d-none');
        return;
    }
    emptyEl.classList.add('d-none');

    grid.innerHTML = filtered.map(club => `
        <div class="col-md-6 col-xl-4">
            <div class="card h-100 p-3">
                <div class="d-flex justify-content-between align-items-start mb-2">
                    <h6 class="fw-semibold mb-0">${escapeHtml(club.name)}</h6>
                    <span class="badge bg-primary-subtle text-primary">${escapeHtml(normalizeCategory(club.category))}</span>
                </div>
                <p class="text-muted small mb-0">${escapeHtml(club.description || 'No description yet.')}</p>
            </div>
        </div>
    `).join('');
}

function populateClubCategories(state, selectEl) {
    const categories = new Set();
    state.allClubs.forEach(club => {
        categories.add(normalizeCategory(club.category));
    });

    const options = Array.from(categories).sort().map(category => `
        <option value="${escapeHtml(category.toLowerCase())}">${escapeHtml(category)}</option>
    `);

    selectEl.innerHTML = `
        <option value="">All categories</option>
        ${options.join('')}
    `;
}

function normalizeCategory(value) {
    const trimmed = (value || '').trim();
    return trimmed ? trimmed : 'Uncategorized';
}
