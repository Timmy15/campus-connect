const DashboardFormatters = (() => {
    function resolveRegisteredCount(event) {
        return Number.isFinite(event?.registeredCount) ? event.registeredCount : 0;
    }

    function formatCapacity(event) {
        return typeof event?.capacity === 'number' ? event.capacity : 'N/A';
    }

    function formatRemaining(event) {
        if (typeof event?.capacityRemaining === 'number') {
            return event.capacityRemaining;
        }
        if (typeof event?.capacity === 'number') {
            return Math.max(event.capacity - resolveRegisteredCount(event), 0);
        }
        return 'N/A';
    }

    function formatFillPercent(event) {
        const capacity = event?.capacity;
        if (typeof capacity !== 'number' || capacity <= 0) {
            return 'N/A';
        }
        const count = resolveRegisteredCount(event);
        const percent = Math.min(Math.round((count / capacity) * 100), 100);
        return `${percent}%`;
    }

    function formatEventDate(value) {
        if (!value) {
            return 'TBD';
        }
        return value.replace('T', ' ').substring(0, 16);
    }

    function escapeRegex(value) {
        return String(value).replaceAll(/[.*+?^${}()|[\]\\]/g, String.raw`\$&`);
    }

    return {
        resolveRegisteredCount,
        formatCapacity,
        formatRemaining,
        formatFillPercent,
        formatEventDate,
        escapeRegex
    };
})();

export default DashboardFormatters;
