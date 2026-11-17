// All listings: every listing with approval / availability filters and moderation actions.
import { api } from '../core/api.js';
import { $, createListController, debounce, emptyState, formatDate, html, icon, photo, raw, statusBadge, typeBadge } from '../core/ui.js';
import { actionButtons, openListingDetail, runListingAction } from './moderation.js';

export function init() {
    const q = $('#q');
    const approval = $('#approval');
    const availability = $('#availability');
    const rows = $('#rows');
    q.value = new URLSearchParams(window.location.search).get('q') ?? '';
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('package', 'No listings found', 'Try a different search or filter.'),
        load: (page) => api.get('/api/admin/items', { q: q.value.trim(), approval: approval.value, availability: availability.value, page, size: 20 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>Listing</th><th>Owner</th><th>Status</th><th>Created</th><th><span class="sr-only">Actions</span></th></tr></thead><tbody>
            ${data.content.map((i) => raw(html`<tr>
                <td class="media-cell-td"><div class="media-cell">${photo(i.images[0]?.url, '')}<div><button class="table__primary link" type="button" data-detail="${i.id}" style="text-decoration:none">${i.name}</button>
                    <span class="table__sub">${i.categoryName} · ${typeBadge(i.listingType)}</span></div></div></td>
                <td data-label="Owner">${i.ownerName}<span class="table__sub">${i.ownerEmail}</span></td>
                <td data-label="Status"><div class="cluster" style="gap:4px">${statusBadge(i.approvalStatus)}${statusBadge(i.availabilityStatus)}</div></td>
                <td data-label="Created">${formatDate(i.createdAt)}</td>
                <td><div class="table__actions">${raw(actionButtons(i))}<button class="btn btn--ghost btn--sm" type="button" data-detail="${i.id}" aria-label="Details">${icon('eye')}</button></div></td></tr>`))}
            </tbody></table>`,
    });
    q.addEventListener('input', debounce(() => list.reset(), 350));
    approval.addEventListener('change', () => list.reset());
    availability.addEventListener('change', () => list.reset());
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
