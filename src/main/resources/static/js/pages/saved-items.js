// Saved Items: favourites with live availability; un-saving removes the card.
import { api } from '../core/api.js';
import { bindSaveButtons, itemCard } from '../core/items.js';
import { $, cardSkeletons, createListController, emptyState, statusBadge } from '../core/ui.js';

export function init() {
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'), skeleton: cardSkeletons(4),
        empty: emptyState('heart', 'Nothing saved yet', 'Tap the heart on any listing to keep it here for later.', '<a class="btn btn--primary" href="/explore">Explore items</a>'),
        load: (page) => api.get('/api/favorites', { page, size: 12 }),
        render: (data) => data.content.map((item) => {
            const available = item.approvalStatus === 'APPROVED' && item.availabilityStatus === 'AVAILABLE';
            return itemCard(item, { showSave: true, extra: `<div class="item-card__foot">${available ? statusBadge('AVAILABLE') : statusBadge(item.availabilityStatus === 'AVAILABLE' ? 'ARCHIVED' : item.availabilityStatus, 'No longer available')}</div>` });
        }).join(''),
    });
    bindSaveButtons(rows, (_, saved) => { if (!saved) list.reload(); });
    list.reload();
}
