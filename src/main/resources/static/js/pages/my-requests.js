// My Requests: requests I sent, filterable by status, with cancel / withdraw / confirm actions.
import { api } from '../core/api.js';
import { bindRequestActions, requestCard } from '../core/requests.js';
import { $, $$, createListController, emptyState } from '../core/ui.js';

export function init() {
    let status = '';
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('send', 'No requests here', 'Find something you need in Explore and send a request.', '<a class="btn btn--primary" href="/explore">Explore items</a>'),
        load: (page) => api.get('/api/requests/sent', { status, page, size: 10 }),
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
