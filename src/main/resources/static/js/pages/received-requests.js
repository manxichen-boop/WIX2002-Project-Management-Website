// Received Requests: requests for my items. Accept / decline pending ones, confirm or withdraw accepted ones.
import { api } from '../core/api.js';
import { bindRequestActions, requestCard } from '../core/requests.js';
import { $, $$, createListController, emptyState } from '../core/ui.js';

export function init() {
    let status = 'PENDING';
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('inbox', 'No requests here', 'When students request your items, they show up here.', '<a class="btn btn--primary" href="/items/new">Post an item</a>'),
        load: (page) => api.get('/api/requests/received', { status, page, size: 10 }),
        render: (data) => data.content.map(requestCard).join(''),
    });
    $$('[data-status]').forEach((t) => t.addEventListener('click', () => {
        status = t.dataset.status;
        $$('[data-status]').forEach((x) => x.setAttribute('aria-selected', String(x === t)));
        list.reset();
    }));
    bindRequestActions(rows, () => list.reload());
    list.reload();
}
