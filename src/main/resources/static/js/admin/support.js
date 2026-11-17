// Admin support: search / filter tickets, read the conversation, reply and change status.
import { api } from '../core/api.js';
import { $, createListController, debounce, emptyState, formatDate, formatDateTime, html, icon, openDialog, raw, statusBadge, toast, toastError } from '../core/ui.js';

const STATUSES = [['OPEN', 'Open'], ['IN_PROGRESS', 'In progress'], ['WAITING_FOR_USER', 'Waiting for student'], ['RESOLVED', 'Resolved'], ['CLOSED', 'Closed']];

export function init() {
    const q = $('#q');
    const status = $('#status');
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('life-buoy', 'No tickets found', 'Student support tickets will appear here.'),
        load: (page) => api.get('/api/admin/support/tickets', { q: q.value.trim(), status: status.value, page, size: 20 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>Ticket</th><th>Student</th><th>Status</th><th>Updated</th><th></th></tr></thead><tbody>
            ${data.content.map((t) => raw(html`<tr>
                <td data-label="Ticket"><span class="table__primary">${t.subject}</span><span class="table__sub">${t.referenceCode}</span></td>
                <td data-label="Student">${t.studentName}<span class="table__sub">${t.studentEmail}</span></td>
                <td data-label="Status">${statusBadge(t.status, t.status === 'WAITING_FOR_USER' ? 'Waiting for student' : null)}</td>
                <td data-label="Updated">${formatDate(t.updatedAt)}</td>
                <td><button class="btn btn--secondary btn--sm" type="button" data-open="${t.id}">${icon('message-square')}Open</button></td></tr>`))}
            </tbody></table>`,
    });
    q.addEventListener('input', debounce(() => list.reset(), 350));
    status.addEventListener('change', () => list.reset());
    rows.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-open]');
        if (btn) openTicket(btn.dataset.open, list);
    });
    list.reload();
}

async function openTicket(id, list) {
    let t;
    try { t = await api.get(`/api/admin/support/tickets/${id}`); } catch (err) { toastError(err); return; }
    const thread = [{ senderName: t.studentName, staff: false, message: t.message, createdAt: t.createdAt }, ...t.replies];
    const result = await openDialog({
        title: t.subject, wide: true, confirmLabel: 'Save', cancelLabel: 'Close',
        body: html`<p class="text-sm text-secondary" style="margin-bottom:12px"><strong>${t.referenceCode}</strong> · ${t.studentName} (${t.studentEmail})</p>
            ${t.attachmentUrl ? raw(html`<p style="margin-bottom:12px"><a class="link" href="${t.attachmentUrl}" target="_blank" rel="noopener">${icon('paperclip', 'icon--sm')} ${t.attachmentName}</a></p>`) : ''}
            <div class="thread">${thread.map((m) => raw(html`<div class="bubble${m.staff ? ' bubble--staff' : ''}"><span class="bubble__meta">${m.staff ? m.senderName : m.senderName} · ${formatDateTime(m.createdAt)}</span><p>${m.message}</p></div>`))}</div>
            <div class="form-row" style="margin-top:16px">
                <div class="field"><label for="t-status">Status</label><select class="select" id="t-status" name="status">
                    ${STATUSES.map(([v, l]) => raw(html`<option value="${v}" ${v === t.status ? raw('selected') : ''}>${l}</option>`))}</select></div>
            </div>
            <div class="field" style="margin-top:12px"><label for="t-reply">Reply to the student <span class="optional">(optional)</span></label>
                <textarea class="textarea" id="t-reply" name="message" maxlength="4000"></textarea></div>`,
        onSubmit: async (data) => {
            const message = (data.get('message') || '').trim();
            if (message) await api.post(`/api/admin/support/tickets/${id}/reply`, { message });
            if (data.get('status') !== t.status || !message) await api.patch(`/api/admin/support/tickets/${id}/status`, { status: data.get('status') });
            return true;
        },
    });
    if (result) { toast('Ticket updated.'); list.reload(); }
}
