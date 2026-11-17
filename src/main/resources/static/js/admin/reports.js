// Reports: start review, resolve (optionally suspending listing / student) or dismiss, with notes.
import { api } from '../core/api.js';
import { $, $$, createListController, emptyState, formatDate, html, icon, openDialog, raw, reasonDialog, statusBadge, toast, toastError } from '../core/ui.js';

export function init() {
    let status = 'OPEN';
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('flag', 'No reports here', 'Reports from students will appear in this queue.'),
        load: (page) => api.get('/api/admin/reports', { status, page, size: 20 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>Report</th><th>Target</th><th>Reported by</th><th>Status</th><th>Date</th><th></th></tr></thead><tbody>
            ${data.content.map((r) => raw(html`<tr>
                <td data-label="Report"><span class="table__primary">${r.reasonLabel}</span><span class="table__sub">${r.description || 'No details given'}</span>
                    ${r.resolutionNotes ? raw(html`<span class="table__sub">${icon('check', 'icon--xs')} ${r.resolutionNotes}${r.reviewedBy ? ` (${r.reviewedBy})` : ''}</span>`) : ''}</td>
                <td data-label="Target">${r.itemId ? raw(html`<a class="link" href="/items/${r.itemId}">${r.itemName}</a><span class="table__sub">Listing · ${r.itemApprovalStatus}</span>`) : ''}
                    ${r.reportedUserName ? raw(html`<span class="table__sub">Student: ${r.reportedUserName} (${r.reportedUserStatus})</span>`) : ''}</td>
                <td data-label="Reported by">${r.reporterName}</td>
                <td data-label="Status">${statusBadge(r.status)}</td><td data-label="Date">${formatDate(r.createdAt)}</td>
                <td><div class="table__actions">${['OPEN', 'UNDER_REVIEW'].includes(r.status) ? raw(html`
                    ${r.status === 'OPEN' ? raw(html`<button class="btn btn--secondary btn--sm" type="button" data-review="${r.id}">Start review</button>`) : ''}
                    <button class="btn btn--primary btn--sm" type="button" data-resolve="${r.id}" data-item="${r.itemId ?? ''}" data-user="${r.reportedUserId ?? ''}">Resolve</button>
                    <button class="btn btn--ghost btn--sm" type="button" data-dismiss="${r.id}">Dismiss</button>`) : ''}</div></td></tr>`))}
            </tbody></table>`,
    });
    $$('[data-status]').forEach((t) => t.addEventListener('click', () => {
        status = t.dataset.status;
        $$('[data-status]').forEach((x) => x.setAttribute('aria-selected', String(x === t)));
        list.reset();
    }));
    rows.addEventListener('click', async (e) => {
        const review = e.target.closest('[data-review]');
        const resolve = e.target.closest('[data-resolve]');
        const dismiss = e.target.closest('[data-dismiss]');
        try {
            if (review) {
                await api.patch(`/api/admin/reports/${review.dataset.review}/review`);
                toast('Report marked as under review.');
                list.reload();
            } else if (resolve) {
                const result = await openDialog({
                    title: 'Resolve report', confirmLabel: 'Resolve report',
                    body: html`<div class="field"><label for="notes">Resolution notes</label><textarea class="textarea" id="notes" name="notes" required minlength="3" maxlength="500"></textarea></div>
                        ${resolve.dataset.item ? raw('<label class="check" style="margin-top:12px"><input type="checkbox" name="suspendListing"> Suspend the reported listing</label>') : ''}
                        ${resolve.dataset.user ? raw('<label class="check" style="margin-top:8px"><input type="checkbox" name="suspendUser"> Suspend the student\'s account</label>') : ''}`,
                    onSubmit: (data) => api.patch(`/api/admin/reports/${resolve.dataset.resolve}/resolve`, {
                        notes: data.get('notes'), suspendListing: data.has('suspendListing'), suspendUser: data.has('suspendUser') }),
                });
                if (result) { toast('Report resolved.'); list.reload(); }
            } else if (dismiss) {
                const notes = await reasonDialog({ title: 'Dismiss report', text: 'Explain why no action is needed.', label: 'Notes', confirmLabel: 'Dismiss', tone: 'primary' });
                if (notes === null) return;
                await api.patch(`/api/admin/reports/${dismiss.dataset.dismiss}/dismiss`, { reason: notes });
                toast('Report dismissed.');
                list.reload();
            }
        } catch (err) { toastError(err); }
    });
    list.reload();
}
