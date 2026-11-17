// Exchange monitoring: read-only list of all requests with status / search filters and details.
import { api } from '../core/api.js';
import { $, createListController, debounce, emptyState, formatDate, formatDateTime, html, icon, openDialog, raw, statusBadge, toastError, typeBadge } from '../core/ui.js';

export function init() {
    const q = $('#q');
    const status = $('#status');
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('arrow-left-right', 'No exchanges found', 'Requests between students will appear here.'),
        load: (page) => api.get('/api/admin/exchanges', { q: q.value.trim(), status: status.value, page, size: 20 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>ID</th><th>Item</th><th>Owner</th><th>Requester</th><th>Offered item</th><th>Status</th><th>Requested</th><th>Completed</th><th></th></tr></thead><tbody>
            ${data.content.map((r) => raw(html`<tr>
                <td data-label="ID">#${r.id}</td>
                <td data-label="Item"><span class="table__primary">${r.item.name}</span><span class="table__sub">${typeBadge(r.item.listingType)}</span></td>
                <td data-label="Owner">${r.owner.name}</td><td data-label="Requester">${r.requester.name}</td>
                <td data-label="Offered item">${r.offeredItem ? r.offeredItem.name : '—'}</td>
                <td data-label="Status">${statusBadge(r.status)}</td>
                <td data-label="Requested">${formatDate(r.createdAt)}</td><td data-label="Completed">${r.completedAt ? formatDate(r.completedAt) : '—'}</td>
                <td><button class="btn btn--ghost btn--sm" type="button" data-detail="${r.id}">${icon('eye')}Details</button></td></tr>`))}
            </tbody></table>`,
    });
    q.addEventListener('input', debounce(() => list.reset(), 350));
    status.addEventListener('change', () => list.reset());
    rows.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-detail]');
        if (!btn) return;
        try {
            const r = await api.get(`/api/admin/exchanges/${btn.dataset.detail}`);
            await openDialog({
                title: `Exchange #${r.id}`, confirmLabel: '', cancelLabel: 'Close', wide: true,
                body: html`<div class="cluster" style="margin-bottom:14px">${statusBadge(r.status)}${typeBadge(r.item.listingType)}</div>
                <dl class="dl">
                    <dt>Item</dt><dd><a class="link" href="/items/${r.item.id}">${r.item.name}</a> (${r.item.categoryName})</dd>
                    <dt>Owner</dt><dd>${r.owner.name} · ${r.owner.contactEmail} · ${r.owner.university}</dd>
                    <dt>Requester</dt><dd>${r.requester.name} · ${r.requester.contactEmail} · ${r.requester.university}</dd>
                    <dt>Offered item</dt><dd>${r.offeredItem ? raw(html`<a class="link" href="/items/${r.offeredItem.id}">${r.offeredItem.name}</a>`) : '—'}</dd>
                    <dt>Message</dt><dd>${r.message || '—'}</dd><dt>Note</dt><dd>${r.statusReason || '—'}</dd>
                    <dt>Confirmations</dt><dd>Owner ${r.ownerConfirmed ? '✓' : '—'} · Requester ${r.requesterConfirmed ? '✓' : '—'}</dd>
                    <dt>Requested</dt><dd>${formatDateTime(r.createdAt)}</dd><dt>Last update</dt><dd>${formatDateTime(r.updatedAt)}</dd>
                    <dt>Completed</dt><dd>${r.completedAt ? formatDateTime(r.completedAt) : '—'}</dd>
                </dl>
                <p class="text-xs text-secondary" style="margin-top:14px">To intervene, suspend the listing in All listings or the student in All users (or resolve a report).</p>`,
            });
        } catch (err) { toastError(err); }
    });
    list.reload();
}
