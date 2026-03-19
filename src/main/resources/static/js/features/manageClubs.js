import ManageState from './manage/manageState.js';
import ManageClub from './manage/manageClub.js';
import ManageEvent from './manage/manageEvent.js';

export function renderManageClubs() {
    ManageState.reset();
    const appRoot = document.getElementById('app-root');

    appRoot.innerHTML = `
        <div class="container-fluid">
            <div class="row mb-4 align-items-center">
                <div class="col">
                    <h2 class="fw-bold">Manage Clubs</h2>
                    <p class="text-muted mb-0">Create, update, or deactivate clubs and manage events.</p>
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

            <div class="row g-4 mt-1" id="eventManagementSection">
                <div class="col-lg-4">
                    <div class="card p-4 h-100">
                        <h5 class="fw-semibold mb-3" id="eventFormTitle">Create Event</h5>
                        <form id="eventForm">
                            <div class="mb-3">
                                <label class="form-label" for="eventClubSelect">Club</label>
                                <select class="form-select" id="eventClubSelect"></select>
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventTitle">Event Title</label>
                                <input type="text" class="form-control" id="eventTitle">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventLocation">Location</label>
                                <input type="text" class="form-control" id="eventLocation">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventCapacity">Capacity</label>
                                <input type="number" class="form-control" id="eventCapacity">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventStartTime">Start Time</label>
                                <input type="datetime-local" class="form-control" id="eventStartTime">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventEndTime">End Time (optional)</label>
                                <input type="datetime-local" class="form-control" id="eventEndTime">
                            </div>
                            <div class="mb-3">
                                <label class="form-label" for="eventDescription">Description</label>
                                <textarea class="form-control" id="eventDescription" rows="3"></textarea>
                            </div>
                            <div class="d-flex gap-2">
                                <button type="submit" class="btn btn-primary flex-grow-1" id="eventFormSubmit">Create Event</button>
                                <button type="button" class="btn btn-outline-secondary d-none" id="eventFormCancel">Cancel</button>
                            </div>
                            <div class="small mt-2" id="eventFormStatus"></div>
                        </form>
                    </div>
                </div>

                <div class="col-lg-8">
                    <div class="card p-4">
                        <div class="d-flex flex-wrap justify-content-between align-items-center mb-3 gap-2">
                            <h5 class="fw-semibold mb-0">Club Events</h5>
                            <span class="text-muted small" id="eventCount">Select a club</span>
                        </div>
                        <div id="eventTableWrap" class="table-responsive"></div>
                    </div>
                </div>
            </div>
        </div>
    `;

    ManageClub.setCallbacks({
        onClubsUpdated: () => ManageEvent.handleClubsUpdated()
    });
    ManageClub.bindClubForm();
    ManageEvent.bindEventForm();
    ManageClub.loadClubs();
}
