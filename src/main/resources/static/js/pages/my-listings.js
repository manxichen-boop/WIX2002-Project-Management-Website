// My Listings: moderation status, rejection reasons, incoming requests, edit and archive.
import { api } from '../core/api.js';
import { $, $$, confirmDialog, createListController, emptyState, html, icon, photo, raw, statusBadge, toast, toastError, typeBadge } from '../core/ui.js';

function row(l) {
    const i = l.item;
    const moderation = i.approvalStatus === 'APPROVED' ? '' : raw(statusBadge(i.approvalStatus));
    const availability = i.approvalStatus === 'APPROVED' || i.availabilityStatus === 'ARCHIVED' ? raw(statusBadge(i.availabilityStatus)) : '';
    return html`<article class="card listing-row">
        <a class="request__media" href="/items/${i.id}">${photo(i.coverUrl, i.name)}</a>
        <div>
            <div class="cluster">${typeBadge(i.listingType)}${moderation}${availability}</div>
            <h2 class="request__title" style="margin-top:6px"><a href="/items/${i.id}">${i.name}</a></h2>
            <div class="request__meta"><span>${icon('tag')}${i.categoryName}</span><span>${icon('map-pin')}${i.pickupLocation}</span>
                ${l.pendingRequests ? raw(html`<span><a class="link" href="/my/received-requests">${icon('inbox')}${l.pendingRequests} pending request${l.pendingRequests > 1 ? 's' : ''}</a></span>`) : ''}</div>
            ${l.rejectionReason ? raw(html`<div class="alert alert--warning listing-row__reason">${icon('info')}<div><strong>Admin feedback</strong>${l.rejectionReason}</div></div>`) : ''}
        </div>
        <div class="request__actions">
            ${l.editable ? raw(html`<a class="btn btn--secondary btn--sm" href="/items/${i.id}/edit">${icon('pencil')}${['REJECTED', 'NEEDS_CHANGES'].includes(i.approvalStatus) ? 'Edit & resubmit' : 'Edit'}</a>`) : ''}
            ${l.archivable ? raw(html`<button class="btn btn--danger btn--sm" type="button" data-archive="${i.id}" data-name="${i.name}">${icon('archive')}Archive</button>`) : ''}
        </div>
    </article>`;
}

export function init() {
    const container = $('#listings');
    let tab = 'active';
    const params = new URLSearchParams(window.location.search);
    if (params.has('submitted')) toast('Listing submitted! It will appear in Explore once an administrator approves it.');
    if (params.has('updated')) toast('Listing updated.');
    if (params.has('submitted') || params.has('updated')) history.replaceState(null, '', '/my/listings');

    const list = createListController({
        container, pager: $('#pager'),
        empty: emptyState('package', 'Nothing here yet', 'Post something you no longer need. Someone on campus probably needs it.',
            '<a class="btn btn--primary" href="/items/new">Post an item</a>'),
        load: (page) => api.get('/api/items/mine', { tab, page, size: 10 }),
        render: (data) => data.content.map(row).join(''),
    });

    $$('[data-tab]').forEach((t) => t.addEventListener('click', () => {
        tab = t.dataset.tab;
        $$('[data-tab]').forEach((x) => x.setAttribute('aria-selected', String(x === t)));
        list.reset();
    }));

    container.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-archive]');
        if (!btn) return;
        const ok = await confirmDialog({
            title: 'Archive this listing?',
            text: `"${btn.dataset.name}" will be removed from Explore and any pending requests will be closed. Your exchange history is kept.`,
            confirmLabel: 'Archive listing', tone: 'danger',
        });
        if (!ok) return;
        try {
            await api.delete(`/api/items/${btn.dataset.archive}`);
            toast('Listing archived.');
            list.reload();
        } catch (err) { toastError(err); }
    });

    list.reload();
}
