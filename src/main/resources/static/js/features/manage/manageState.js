const ManageState = (() => {
    const state = {
        cachedClubs: [],
        cachedEvents: [],
        editClubId: null,
        editEventId: null,
        selectedClubId: null
    };

    function reset() {
        state.cachedClubs = [];
        state.cachedEvents = [];
        state.editClubId = null;
        state.editEventId = null;
        state.selectedClubId = null;
    }

    function setClubs(clubs) {
        state.cachedClubs = Array.isArray(clubs) ? clubs : [];
    }

    function setEvents(events) {
        state.cachedEvents = Array.isArray(events) ? events : [];
    }

    function setEditClubId(id) {
        state.editClubId = id ?? null;
    }

    function setEditEventId(id) {
        state.editEventId = id ?? null;
    }

    function setSelectedClubId(id) {
        state.selectedClubId = id ?? null;
    }

    return {
        state,
        reset,
        setClubs,
        setEvents,
        setEditClubId,
        setEditEventId,
        setSelectedClubId
    };
})();

export default ManageState;
