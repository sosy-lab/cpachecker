/*
 * This file is part of CPAchecker,
 * a tool for configurable software verification:
 * https://cpachecker.sosy-lab.org
 *
 * SPDX-FileCopyrightText: 2007-2020 Dirk Beyer <https://www.sosy-lab.org>
 * SPDX-FileCopyrightText: 2018 Lokesh Nandanwar
 *
 * SPDX-License-Identifier: Apache-2.0
 */

const ALL_TYPES = [
    'POST_CONDITION',
    'VIOLATION_CONDITION',
    'RESULT',
    'WITNESS',
    'EXCEPTION',
];

const filterState = {
    messageTypes: new Set(ALL_TYPES),
    blockId: '',
    contentSearch: '',
    hideRepeats: false,
};

document.addEventListener('DOMContentLoaded', function () {
    initializeFilters();
    initializeViewToggle();
    initializeModal();
    initializeDetailView();
    initializeExpandAll();
    initializeGraphView();
    applyFilters();
});

const VIEWS = ['table', 'timeline', 'graph'];

/**
 * Every message card of the table and timeline views. The graph view's detail
 * panel holds clones of these cards; those are excluded, because they must not
 * be filtered away and must not be counted twice.
 */
function allCards() {
    const detail = document.getElementById('graphDetail');
    return Array.from(
        document.querySelectorAll('.message-card, .timeline-message-card')
    ).filter((card) => !detail || !detail.contains(card));
}

function currentView() {
    return VIEWS.find((view) => document.getElementById(view + 'ViewBtn').classList.contains('active'));
}

function initializeViewToggle() {
    VIEWS.forEach((view) => {
        document.getElementById(view + 'ViewBtn').addEventListener('click', () => {
            VIEWS.forEach((other) => {
                document.getElementById(other + 'ViewBtn').classList.toggle('active', other === view);
                document
                    .getElementById(other + 'ViewContainer')
                    .classList.toggle('hidden', other !== view);
            });
        });
    });
}

function initializeExpandAll() {
    const expandAll = document.getElementById('expandAll');
    expandAll.addEventListener('change', function () {
        document
            .querySelectorAll('details.message-content')
            .forEach((details) => (details.open = this.checked));
    });
}

function initializeFilters() {
    document.querySelectorAll('.filter-btn').forEach((btn) => {
        btn.addEventListener('click', function () {
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

    const blockFilter = document.getElementById('blockFilter');
    blockFilter.addEventListener('input', function () {
        filterState.blockId = this.value.toLowerCase();
        applyFilters();
    });
    document.getElementById('clearBlockFilter').addEventListener('click', function () {
        blockFilter.value = '';
        filterState.blockId = '';
        applyFilters();
    });

    const contentSearch = document.getElementById('contentSearch');
    contentSearch.addEventListener('input', function () {
        filterState.contentSearch = this.value.toLowerCase();
        applyFilters();
    });
    document.getElementById('clearContentSearch').addEventListener('click', function () {
        contentSearch.value = '';
        filterState.contentSearch = '';
        applyFilters();
    });

    const hideRepeats = document.getElementById('hideRepeats');
    hideRepeats.addEventListener('change', function () {
        filterState.hideRepeats = this.checked;
        applyFilters();
    });

    document.getElementById('resetFiltersBtn').addEventListener('click', function () {
        filterState.messageTypes = new Set(ALL_TYPES);
        filterState.blockId = '';
        filterState.contentSearch = '';
        filterState.hideRepeats = false;

        document.querySelectorAll('.filter-btn').forEach((btn) => btn.classList.add('active'));
        blockFilter.value = '';
        contentSearch.value = '';
        hideRepeats.checked = false;

        applyFilters();
    });
}

function matches(card) {
    if (!filterState.messageTypes.has(card.dataset.type)) {
        return false;
    }
    if (filterState.hideRepeats && card.dataset.repeat === 'yes') {
        return false;
    }
    const sender = (card.dataset.sender || '').toLowerCase();
    if (filterState.blockId && !sender.includes(filterState.blockId)) {
        return false;
    }
    if (
        filterState.contentSearch &&
        !card.textContent.toLowerCase().includes(filterState.contentSearch)
    ) {
        return false;
    }
    return true;
}

function applyFilters() {
    let visibleCount = 0;

    allCards().forEach((card) => {
        const visible = matches(card);
        card.classList.toggle('hidden', !visible);
        if (visible) {
            visibleCount += 1;
        }
    });

    // A table cell is empty when all its cards are filtered out; a row is empty
    // when all its cells are.
    // The child combinators matter: the cards contain tables of their own, and a
    // descendant selector would hide their rows as "rows without any message".
    document.querySelectorAll('#messageTable > tbody > tr').forEach((row) => {
        let rowHasContent = false;
        row.querySelectorAll(':scope > .message-cell').forEach((cell) => {
            const hasContent = cell.querySelector('.message-card:not(.hidden)') !== null;
            cell.classList.toggle('filtered-out', !hasContent);
            rowHasContent = rowHasContent || hasContent;
        });
        row.classList.toggle('hidden', !rowHasContent);
    });

    document.querySelectorAll('.timeline-row').forEach((row) => {
        const hasContent = row.querySelector('.timeline-message-card:not(.hidden)') !== null;
        row.classList.toggle('hidden', !hasContent);
    });

    // Each message is rendered twice (table view and timeline view).
    document.getElementById('visibleMessages').textContent = Math.round(visibleCount / 2);

    if (window.refreshGraphView) {
        window.refreshGraphView();
    }
}

/**
 * The graph view: a clickable block graph next to a detail panel.
 *
 * It owns no message data of its own. Everything it shows is read from, or
 * cloned out of, the cards the timeline view already contains, so switching a
 * filter keeps all three views consistent and costs no extra markup.
 */
function initializeGraphView() {
    const svg = document.getElementById('blockGraph');
    const dataElement = document.getElementById('blockGraphData');
    const detail = document.getElementById('graphDetail');
    const slider = document.getElementById('graphTime');
    const positionLabel = document.getElementById('graphPosition');
    if (!svg || !dataElement) {
        return; // Graphviz was unavailable; the view shows an explanation instead.
    }
    const blocks = JSON.parse(dataElement.textContent);

    const everyEntry = Array.from(
        document.querySelectorAll('#timelineViewContainer .timeline-message-card')
    ).map((card) => ({
        card,
        sender: card.dataset.sender,
        type: card.dataset.type,
        time: card.closest('.timeline-row')?.dataset.timestamp ?? '0',
        id: card.querySelector('.card-id')?.textContent.trim() ?? '',
    }));

    let entries = everyEntry;
    let selected = null;
    let playing = null;

    // Resolved once into a map rather than queried by attribute selector, which
    // would need the block ids to be escaped for use inside a selector.
    const nodeByBlock = new Map();
    svg.querySelectorAll('.node[data-block]').forEach((node) => {
        nodeByBlock.set(node.dataset.block, node);
    });

    function nodeFor(blockId) {
        return nodeByBlock.get(blockId);
    }

    function clearHighlights() {
        svg.querySelectorAll('.node, .edge').forEach((element) => {
            element.classList.remove('sel', 'pred', 'succ', 'active', 'flow');
        });
    }

    function highlightNeighbourhood(blockId) {
        const info = blocks[blockId];
        if (!info) {
            return;
        }
        nodeFor(blockId)?.classList.add('sel');
        info.predecessors.forEach((id) => nodeFor(id)?.classList.add('pred'));
        info.successors.forEach((id) => nodeFor(id)?.classList.add('succ'));
        svg.querySelectorAll('.edge').forEach((edge) => {
            if (edge.dataset.from === blockId || edge.dataset.to === blockId) {
                edge.classList.add('sel');
            }
        });
    }

    /** The blocks a message of this type is delivered to. */
    function receiversOf(entry) {
        const info = blocks[entry.sender];
        if (!info) {
            return [];
        }
        if (entry.type === 'POST_CONDITION') {
            return info.successors;
        }
        if (entry.type === 'VIOLATION_CONDITION') {
            return info.predecessors;
        }
        return Object.keys(blocks);
    }

    function element(tag, className, text) {
        const node = document.createElement(tag);
        if (className) {
            node.className = className;
        }
        if (text !== undefined) {
            node.textContent = text;
        }
        return node;
    }

    function factsTable(blockId) {
        const info = blocks[blockId];
        const own = entries.filter((entry) => entry.sender === blockId);
        const counts = {};
        own.forEach((entry) => (counts[entry.type] = (counts[entry.type] || 0) + 1));
        const rows = [
            ['predecessors', info.predecessors.join(', ') || 'none (root)'],
            ['successors', info.successors.join(', ') || 'none'],
            ['entry -> exit', `${info.entry} -> ${info.exit}`],
            ['violation-condition location', String(info.violationConditionLocation)],
            [
                'messages',
                Object.keys(counts).length
                    ? Object.entries(counts)
                          .map(([type, count]) => `${count} ${type.replace(/_/g, ' ')}`)
                          .join(', ')
                    : 'none in the current filter',
            ],
        ];
        if (own.length) {
            rows.push(['first / last', `${own[0].time} ms / ${own[own.length - 1].time} ms`]);
        }
        const table = element('table', 'kv-table');
        rows.forEach(([key, value]) => {
            const tr = document.createElement('tr');
            tr.append(element('th', null, key), element('td', null, value));
            table.append(tr);
        });
        return table;
    }

    function renderBlock(blockId, highlightEntry) {
        selected = blockId;
        detail.replaceChildren();
        detail.append(element('h3', 'graph-detail-title', blockId + (blocks[blockId]?.isRoot ? '  (root)' : '')));
        if (!blocks[blockId]) {
            detail.append(element('p', 'graph-hint', 'This sender is not a block of the graph.'));
            return;
        }
        detail.append(factsTable(blockId));

        const code = blocks[blockId].code;
        if (code) {
            const details = element('details', 'graph-section');
            details.append(element('summary', null, 'Code'));
            details.append(element('pre', 'graph-code', code));
            detail.append(details);
        }

        const own = entries.filter((entry) => entry.sender === blockId);
        if (!own.length) {
            detail.append(element('p', 'graph-hint', 'No messages from this block.'));
            return;
        }

        // The last message of every type, expanded: that is the block's current
        // contribution to the analysis.
        const latest = new Map();
        own.forEach((entry) => latest.set(entry.type, entry));
        detail.append(element('h4', 'graph-section-title', 'Latest message per type'));
        latest.forEach((entry) => {
            const clone = entry.card.cloneNode(true);
            clone.classList.remove('hidden');
            clone.querySelector('details.message-content')?.setAttribute('open', '');
            detail.append(clone);
        });

        detail.append(element('h4', 'graph-section-title', `All messages (${own.length})`));
        const list = element('ol', 'graph-message-list');
        own.forEach((entry, index) => {
            const item = element('li', entry.card.dataset.type.toLowerCase().replace(/_/g, '-'));
            if (highlightEntry === entry) {
                item.classList.add('current');
            }
            item.append(element('span', 'msg-time', `${entry.time} ms`));
            item.append(element('span', 'msg-id', entry.id));
            item.append(element('span', 'msg-type', entry.type.replace(/_/g, ' ')));
            if (entry.card.dataset.repeat === 'yes') {
                item.append(element('span', 'msg-repeat', 'repeat'));
            }
            item.addEventListener('click', () => showMessage(entries.indexOf(entry)));
            item.dataset.index = String(index);
            list.append(item);
        });
        detail.append(list);
    }

    function showMessage(index) {
        if (index < 0 || index >= entries.length) {
            return;
        }
        slider.value = String(index);
        const entry = entries[index];
        clearHighlights();
        highlightNeighbourhood(entry.sender);
        nodeFor(entry.sender)?.classList.add('active');
        const receivers = new Set(receiversOf(entry));
        receivers.forEach((id) => nodeFor(id)?.classList.add('flow'));
        svg.querySelectorAll('.edge').forEach((edge) => {
            if (edge.dataset.from === entry.sender && receivers.has(edge.dataset.to)) {
                edge.classList.add('flow');
            }
            if (edge.dataset.to === entry.sender && receivers.has(edge.dataset.from)) {
                edge.classList.add('flow');
            }
        });
        positionLabel.textContent =
            `${index + 1} / ${entries.length}  -  ${entry.id} ` +
            `${entry.type.replace(/_/g, ' ')} from ${entry.sender} at ${entry.time} ms`;
        renderBlock(entry.sender, entry);
        const current = detail.querySelector('.graph-message-list .current');
        if (current && current.scrollIntoView) {
            current.scrollIntoView({ block: 'nearest' });
        }
    }

    svg.querySelectorAll('.node').forEach((node) => {
        node.addEventListener('click', () => {
            stopPlaying();
            clearHighlights();
            highlightNeighbourhood(node.dataset.block);
            renderBlock(node.dataset.block);
        });
    });

    function stopPlaying() {
        if (playing) {
            clearInterval(playing);
            playing = null;
            document.getElementById('graphPlay').innerHTML = '&#9654;';
        }
    }

    document.getElementById('graphPlay').addEventListener('click', function () {
        if (playing) {
            stopPlaying();
            return;
        }
        this.innerHTML = '&#9632;';
        playing = setInterval(() => {
            const next = Number(slider.value) + 1;
            if (next >= entries.length) {
                stopPlaying();
            } else {
                showMessage(next);
            }
        }, 400);
    });
    document.getElementById('graphPrev').addEventListener('click', () => {
        stopPlaying();
        showMessage(Number(slider.value) - 1);
    });
    document.getElementById('graphNext').addEventListener('click', () => {
        stopPlaying();
        showMessage(Number(slider.value) + 1);
    });
    slider.addEventListener('input', () => {
        stopPlaying();
        showMessage(Number(slider.value));
    });

    document.addEventListener('keydown', (e) => {
        // e.target is not necessarily an Element (it can be the document), so ask
        // for the tag name instead of using .matches().
        const tag = (e.target.tagName || '').toLowerCase();
        if (currentView() !== 'graph' || tag === 'input' || tag === 'textarea') {
            return;
        }
        if (e.key === 'ArrowRight') {
            stopPlaying();
            showMessage(Number(slider.value) + 1);
        } else if (e.key === 'ArrowLeft') {
            stopPlaying();
            showMessage(Number(slider.value) - 1);
        }
    });

    const zoom = document.getElementById('graphZoom');
    zoom.addEventListener('input', () => {
        svg.style.width = zoom.value + '%';
    });

    /** Re-read which messages are visible; called after every filter change. */
    window.refreshGraphView = function () {
        entries = everyEntry.filter((entry) => !entry.card.classList.contains('hidden'));
        slider.max = String(Math.max(entries.length - 1, 0));
        slider.disabled = entries.length === 0;
        if (!entries.length) {
            positionLabel.textContent = 'no message matches the current filter';
        } else {
            positionLabel.textContent = `${entries.length} messages - drag to replay`;
        }
        svg.querySelectorAll('.node').forEach((node) => {
            const count = entries.filter((entry) => entry.sender === node.dataset.block).length;
            node.classList.toggle('idle', count === 0);
        });
        if (selected) {
            renderBlock(selected);
        }
    };
    window.refreshGraphView();
}

function initializeModal() {
    document.getElementById('floatingGraphBtn').addEventListener('click', () => {
        document.getElementById('graphModal').classList.add('active');
    });

    // Every overlay and close button names the modal it dismisses.
    document.querySelectorAll('[data-close]').forEach((element) => {
        element.addEventListener('click', () => {
            document.getElementById(element.dataset.close).classList.remove('active');
        });
    });

    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            document
                .querySelectorAll('.modal.active')
                .forEach((modal) => modal.classList.remove('active'));
        }
    });
}

/**
 * The columns of the table view are too narrow to read a decoded state in, so
 * the content of a card can be shown in a full-width modal instead.
 */
function initializeDetailView() {
    const modal = document.getElementById('detailModal');
    const title = document.getElementById('detailTitle');
    const body = document.getElementById('detailBody');

    function open(card) {
        const content = card.querySelector('.content-body');
        if (!content) {
            return;
        }
        const sender = card.querySelector('.card-sender')?.textContent.trim() || '';
        const id = card.querySelector('.card-id')?.textContent.trim() || '';
        title.textContent = `${sender} ${id} - ${card.dataset.type.replace(/_/g, ' ')}`;
        body.replaceChildren(content.cloneNode(true));
        modal.classList.add('active');
    }

    document.addEventListener('click', (e) => {
        const button = e.target.closest('.card-expand');
        if (button) {
            e.preventDefault();
            open(button.closest('.message-card, .timeline-message-card'));
        }
    });

    // Only the header reacts, so that double-clicking inside the content still
    // selects a word instead of opening the viewer.
    document.addEventListener('dblclick', (e) => {
        const header = e.target.closest('.card-header');
        if (header) {
            open(header.closest('.message-card, .timeline-message-card'));
        }
    });
}
