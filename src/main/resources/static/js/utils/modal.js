import { escapeHtml } from './dom.js';

function resolveModalContainer() {
    return document.getElementById('modal-container');
}

export function confirmModal(options = {}) {
    const {
        title = 'Please confirm',
        message = 'Are you sure you want to continue?',
        confirmText = 'Confirm',
        cancelText = 'Cancel',
        confirmVariant = 'primary',
        iconClass = ''
    } = options;

    const container = resolveModalContainer();
    if (!container || !globalThis.bootstrap?.Modal) {
        throw new Error('Modal container or Bootstrap modal not available.');
    }

    const modalId = `cc-modal-${Date.now()}-${Math.floor(Math.random() * 10000)}`;
    const safeTitle = escapeHtml(title);
    const safeMessage = escapeHtml(message);
    const safeConfirmText = escapeHtml(confirmText);
    const safeCancelText = escapeHtml(cancelText);
    const safeIconClass = iconClass ? escapeHtml(iconClass) : '';
    const iconHtml = safeIconClass ? `<i class="${safeIconClass} me-2"></i>` : '';

    const markup = `
        <div class="modal fade" id="${modalId}" tabindex="-1" aria-labelledby="${modalId}-label" aria-hidden="true">
            <div class="modal-dialog modal-dialog-centered">
                <div class="modal-content">
                    <div class="modal-header">
                        <h5 class="modal-title" id="${modalId}-label">${iconHtml}${safeTitle}</h5>
                        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
                    </div>
                    <div class="modal-body">
                        <p class="mb-0">${safeMessage}</p>
                    </div>
                    <div class="modal-footer">
                        <button type="button" class="btn btn-outline-secondary" data-bs-dismiss="modal">${safeCancelText}</button>
                        <button type="button" class="btn btn-${escapeHtml(confirmVariant)}" data-role="confirm">${safeConfirmText}</button>
                    </div>
                </div>
            </div>
        </div>
    `;

    container.insertAdjacentHTML('beforeend', markup);
    const modalEl = document.getElementById(modalId);
    if (!modalEl) {
        throw new Error('Unable to create confirmation modal.');
    }

    const confirmButton = modalEl.querySelector('[data-role="confirm"]');
    if (!confirmButton) {
        modalEl.remove();
        throw new Error('Confirmation modal missing confirm button.');
    }

    return new Promise(resolve => {
        let resolved = false;
        const modal = new globalThis.bootstrap.Modal(modalEl);

        const resolveOnce = (value) => {
            if (resolved) {
                return;
            }
            resolved = true;
            resolve(value);
        };

        modalEl.addEventListener('hidden.bs.modal', () => {
            modal.dispose();
            modalEl.remove();
            if (!resolved) {
                resolveOnce(false);
            }
        }, { once: true });

        confirmButton.addEventListener('click', () => {
            resolveOnce(true);
            modal.hide();
        }, { once: true });

        modal.show();
    });
}
