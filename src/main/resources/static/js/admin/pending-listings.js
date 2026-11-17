// Pending listings: review cards with photos and the full description, plus decision buttons.
import { api } from '../core/api.js';
import { $, createListController, emptyState, formatDate, html, icon, raw, typeBadge } from '../core/ui.js';
import { actionButtons, openListingDetail, runListingAction } from './moderation.js';

function card(item) {
    const photos = item.images.length
        ? item.images.slice(0, 4).map((img) => raw(html`<img src="${img.url}" alt="Photo of ${item.name}" data-detail="${item.id}">`))
        : raw('<span class="media-cell__ph"></span>');
    return html`<article class="card review-card">
        <div class="review-card__photos">${photos}</div>
        <div>
            <div class="cluster">${typeBadge(item.listingType)}<span class="badge badge--neutral badge--plain">${item.categoryName}</span><span class="badge badge--neutral badge--plain">${item.conditionLabel}</span></div>
            <h3 style="margin-top:8px">${item.name}</h3>
            <p class="review-card__desc">${item.description}</p>
            <div class="request__meta" style="margin-top:10px">
                <span>${icon('user')}${item.ownerName} · ${item.ownerEmail}</span><span>${icon('map-pin')}${item.pickupLocation}</span>
                <span>${icon('clock')}Submitted ${formatDate(item.updatedAt)}</span>
            </div>
        </div>
        <div class="review-card__actions">${raw(actionButtons(item, ''))}
            <button class="btn btn--ghost" type="button" data-detail="${item.id}">${icon('eye')}Full details</button></div>
    </article>`;
}

export function init() {
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('circle-check', 'Queue is clear', 'New and resubmitted listings will appear here for review.'),
        load: (page) => api.get('/api/admin/items/pending', { page, size: 10 }),
        render: (data) => data.content.map(card).join(''),
    });
    rows.addEventListener('click', async (e) => {
        const action = e.target.closest('[data-action]');
        const detail = e.target.closest('[data-detail]');
        if (action) {
            if (await runListingAction(action.dataset.action, action.dataset.id)) list.reload();
        } else if (detail) {
            openListingDetail(detail.dataset.detail);
        }
    });
    list.reload();
}
