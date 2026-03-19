const DashboardUi = (() => {
    let cardToggleBound = false;

    function bindCardToggleActions() {
        if (cardToggleBound) {
            return;
        }
        document.addEventListener('click', event => {
            const button = event.target.closest('button[data-action="toggle-card"]');
            if (!button) {
                return;
            }
            const targetId = button.dataset.target;
            if (!targetId) {
                return;
            }
            toggleCardBody(targetId);
        });
        cardToggleBound = true;
    }

    function toggleCardBody(targetId) {
        const body = document.getElementById(targetId);
        if (!body) {
            return;
        }
        const collapsed = body.classList.toggle('d-none');
        const button = document.querySelector(`button[data-target="${targetId}"]`);
        updateToggleButton(button, !collapsed);
    }

    function updateToggleButton(button, expanded) {
        if (!button) {
            return;
        }
        button.setAttribute('aria-expanded', String(expanded));
        button.title = expanded ? 'Collapse' : 'Expand';
        const icon = button.querySelector('i');
        if (icon) {
            icon.classList.toggle('bi-chevron-up', expanded);
            icon.classList.toggle('bi-chevron-down', !expanded);
        }
    }

    function ensureCardBodyVisible(targetId) {
        const body = document.getElementById(targetId);
        if (!body) {
            return;
        }
        if (body.classList.contains('d-none')) {
            body.classList.remove('d-none');
            const button = document.querySelector(`button[data-target="${targetId}"]`);
            updateToggleButton(button, true);
        }
    }

    function scrollCardIntoView(cardId) {
        const card = document.getElementById(cardId);
        if (card) {
            card.scrollIntoView({ behavior: 'smooth', block: 'start' });
        }
    }

    return {
        bindCardToggleActions,
        toggleCardBody,
        updateToggleButton,
        ensureCardBodyVisible,
        scrollCardIntoView
    };
})();

export default DashboardUi;
