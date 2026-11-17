// Exchange History: accepted and completed exchanges on either side.
import { api } from '../core/api.js';
import { bindRequestActions, requestCard } from '../core/requests.js';
import { $, createListController, emptyState } from '../core/ui.js';

export function init() {
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('history', 'No exchanges yet', 'Accepted and completed swaps and giveaways will be listed here.'),
        load: (page) => api.get('/api/requests/history', { page, size: 10 }),
        render: (data) => data.content.map(requestCard).join(''),
    });
    bindRequestActions(rows, () => list.reload());
    list.reload();
}
