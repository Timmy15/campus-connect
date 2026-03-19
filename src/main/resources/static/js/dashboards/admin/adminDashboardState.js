const AdminDashboardState = (() => {
    let registrationsTable = null;
    let detailTable = null;
    let eventChart = null;
    let clubChart = null;

    const state = {
        events: [],
        registrations: [],
        selectedEventId: null,
        selectedEventTitle: '',
        eventsRequestId: 0,
        detailsRequestId: 0,
        participationRequestId: 0
    };

    function reset() {
        registrationsTable?.destroy?.();
        detailTable?.destroy?.();
        registrationsTable = null;
        detailTable = null;
        eventChart?.destroy?.();
        clubChart?.destroy?.();
        eventChart = null;
        clubChart = null;
        state.events = [];
        state.registrations = [];
        state.selectedEventId = null;
        state.selectedEventTitle = '';
        state.eventsRequestId = 0;
        state.detailsRequestId = 0;
        state.participationRequestId = 0;
    }

    function nextEventsRequestId() {
        state.eventsRequestId += 1;
        return state.eventsRequestId;
    }

    function nextDetailsRequestId() {
        state.detailsRequestId += 1;
        return state.detailsRequestId;
    }

    function nextParticipationRequestId() {
        state.participationRequestId += 1;
        return state.participationRequestId;
    }

    function isStaleEventsRequest(requestId) {
        return requestId !== state.eventsRequestId;
    }

    function isStaleDetailsRequest(requestId) {
        return requestId !== state.detailsRequestId;
    }

    function isStaleParticipationRequest(requestId) {
        return requestId !== state.participationRequestId;
    }

    function setSelectedEvent(eventId, eventTitle) {
        state.selectedEventId = eventId;
        state.selectedEventTitle = eventTitle || 'Event';
    }

    function setRegistrationsTable(table) {
        registrationsTable = table;
    }

    function getRegistrationsTable() {
        return registrationsTable;
    }

    function setDetailTable(table) {
        detailTable = table;
    }

    function getDetailTable() {
        return detailTable;
    }

    function setEventChart(chart) {
        eventChart = chart;
    }

    function getEventChart() {
        return eventChart;
    }

    function setClubChart(chart) {
        clubChart = chart;
    }

    function getClubChart() {
        return clubChart;
    }

    return {
        state,
        reset,
        nextEventsRequestId,
        nextDetailsRequestId,
        nextParticipationRequestId,
        isStaleEventsRequest,
        isStaleDetailsRequest,
        isStaleParticipationRequest,
        setSelectedEvent,
        setRegistrationsTable,
        getRegistrationsTable,
        setDetailTable,
        getDetailTable,
        setEventChart,
        getEventChart,
        setClubChart,
        getClubChart
    };
})();

export default AdminDashboardState;
