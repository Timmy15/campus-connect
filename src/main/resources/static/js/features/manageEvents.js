import { renderManageClubs } from './manageClubs.js';

export function renderManageEvents() {
    renderManageClubs();
    const section = document.getElementById('eventManagementSection');
    if (section) {
        section.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }
}
