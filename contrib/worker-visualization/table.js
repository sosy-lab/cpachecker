// Filter state
const filterState = {
    messageTypes: new Set(['POST_CONDITION', 'VIOLATION_CONDITION', 'FOUND_RESULT']),
    blockId: '',
    contentSearch: '',
    currentView: 'table'
};

// Initialize
document.addEventListener('DOMContentLoaded', function() {
    initializeFilters();
    initializeViewToggle();
    initializeModal();
    updateVisibleMessages();
});

function initializeViewToggle() {
    const tableViewBtn = document.getElementById('tableViewBtn');
    const timelineViewBtn = document.getElementById('timelineViewBtn');
    const tableViewContainer = document.getElementById('tableViewContainer');
    const timelineViewContainer = document.getElementById('timelineViewContainer');

    tableViewBtn.addEventListener('click', function() {
        filterState.currentView = 'table';
        tableViewBtn.classList.add('active');
        timelineViewBtn.classList.remove('active');
        tableViewContainer.classList.remove('hidden');
        timelineViewContainer.classList.add('hidden');
    });

    timelineViewBtn.addEventListener('click', function() {
        filterState.currentView = 'timeline';
        timelineViewBtn.classList.add('active');
        tableViewBtn.classList.remove('active');
        timelineViewContainer.classList.remove('hidden');
        tableViewContainer.classList.add('hidden');
    });
}

function initializeFilters() {
    // Message type filters
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.addEventListener('click', function() {
            const type = this.dataset.filterType;
            this.classList.toggle('active');

            if (this.classList.contains('active')) {
                filterState.messageTypes.add(type);
            } else {
                filterState.messageTypes.delete(type);
            }

            applyFilters();
        });
    });

    // Block ID filter
    const blockFilter = document.getElementById('blockFilter');
    blockFilter.addEventListener('input', function() {
        filterState.blockId = this.value.toLowerCase();
        applyFilters();
    });

    document.getElementById('clearBlockFilter').addEventListener('click', function() {
        blockFilter.value = '';
        filterState.blockId = '';
        applyFilters();
    });

    // Content search
    const contentSearch = document.getElementById('contentSearch');
    contentSearch.addEventListener('input', function() {
        filterState.contentSearch = this.value.toLowerCase();
        applyFilters();
    });

    document.getElementById('clearContentSearch').addEventListener('click', function() {
        contentSearch.value = '';
        filterState.contentSearch = '';
        applyFilters();
    });

    // Reset button
    document.getElementById('resetFiltersBtn').addEventListener('click', function() {
        filterState.messageTypes = new Set(['POST_CONDITION', 'VIOLATION_CONDITION', 'FOUND_RESULT']);
        filterState.blockId = '';
        filterState.contentSearch = '';

        document.querySelectorAll('.filter-btn').forEach(btn => btn.classList.add('active'));
        blockFilter.value = '';
        contentSearch.value = '';

        applyFilters();
    });
}

function applyFilters() {
    // Filter table view
    const rows = document.querySelectorAll('#messageTable tbody tr');

    rows.forEach(row => {
        const cells = row.querySelectorAll('.message-cell');
        let showRow = false;

        cells.forEach(cell => {
            const messageType = cell.dataset.type;
            const senderId = cell.querySelector('[data-sender]')?.dataset.sender || '';
            const content = cell.textContent.toLowerCase();

            // Check filters
            const typeMatch = !messageType || filterState.messageTypes.has(messageType);
            const blockMatch = !filterState.blockId || senderId.toLowerCase().includes(filterState.blockId);
            const contentMatch = !filterState.contentSearch || content.includes(filterState.contentSearch);

            if (messageType && typeMatch && blockMatch && contentMatch) {
                cell.style.display = '';
                showRow = true;
            } else if (messageType) {
                cell.style.display = 'none';
            }
        });

        // Show/hide row
        row.style.display = showRow ? '' : 'none';
    });

    // Filter timeline view
    const timelineCards = document.querySelectorAll('.timeline-message-card');
    timelineCards.forEach(card => {
        const messageType = card.dataset.type;
        const senderId = card.dataset.sender || '';
        const content = card.textContent.toLowerCase();

        const typeMatch = filterState.messageTypes.has(messageType);
        const blockMatch = !filterState.blockId || senderId.toLowerCase().includes(filterState.blockId);
        const contentMatch = !filterState.contentSearch || content.includes(filterState.contentSearch);

        if (typeMatch && blockMatch && contentMatch) {
            card.style.display = '';
        } else {
            card.style.display = 'none';
        }
    });

    // Hide timeline rows that have no visible messages
    const timelineRows = document.querySelectorAll('.timeline-row');
    timelineRows.forEach(row => {
        const visibleCards = Array.from(row.querySelectorAll('.timeline-message-card'))
            .filter(card => card.style.display !== 'none');
        row.style.display = visibleCards.length > 0 ? '' : 'none';
    });

    updateVisibleMessages();
}

function updateVisibleMessages() {
    const visibleRows = document.querySelectorAll('#messageTable tbody tr:not([style*="display: none"])');
    let visibleCount = 0;

    visibleRows.forEach(row => {
        const visibleCells = row.querySelectorAll('.message-cell:not([style*="display: none"])');
        visibleCount += visibleCells.length;
    });

    document.getElementById('visibleMessages').textContent = visibleCount;
}

function initializeModal() {
    const modal = document.getElementById('graphModal');
    const floatingBtn = document.getElementById('floatingGraphBtn');
    const closeBtn = document.getElementById('modalClose');
    const overlay = document.getElementById('modalOverlay');

    floatingBtn.addEventListener('click', () => {
        modal.classList.add('active');
    });

    closeBtn.addEventListener('click', () => {
        modal.classList.remove('active');
    });

    overlay.addEventListener('click', () => {
        modal.classList.remove('active');
    });

    // Close on Escape key
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && modal.classList.contains('active')) {
            modal.classList.remove('active');
        }
    });
}