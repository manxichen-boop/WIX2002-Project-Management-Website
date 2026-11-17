// Help & Support: submit tickets (optional attachment), list them, read and reply to the conversation.
import { api } from '../core/api.js';
import { $, createListController, emptyState, formatDate, formatDateTime, html, icon, openDialog, raw, setBusy, statusBadge, toast } from '../core/ui.js';
import { rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const form = $('#ticket-form');
    const tickets = $('#tickets');
    const list = createListController({
        container: tickets, pager: $('#pager'),
        empty: emptyState('life-buoy', 'No tickets yet', 'Questions or problems? Send us a message and we will help.'),
        load: (page) => api.get('/api/support/tickets/mine', { page, size: 8 }),
        render: (data) => data.content.map((t) => html`<button class="ticket" type="button" data-ticket="${t.id}">
            <div><strong>${t.subject}</strong><span>${t.referenceCode} · updated ${formatDate(t.updatedAt)}</span></div>${statusBadge(t.status)}</button>`).join(''),
    });

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate(form, { subject: [rules.required, rules.minLength(4), rules.maxLength(150)], message: [rules.required, rules.minLength(10), rules.maxLength(4000)] })) return;
        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            const ticket = await api.post('/api/support/tickets', new FormData(form));
            form.reset();
            toast(`Ticket sent. Your reference number is ${ticket.referenceCode}.`);
            list.reset();
        } catch (err) { showApiError(form, err); } finally { setBusy(button, false); }
    });

    tickets.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-ticket]');
        if (btn) openTicket(btn.dataset.ticket, list);
    });
    list.reload();
    const deepLink = new URLSearchParams(window.location.search).get('ticket');
    if (deepLink) openTicket(deepLink, list);
}

async function openTicket(id, list) {
    const t = await api.get(`/api/support/tickets/${id}`);
    const thread = [{ senderName: 'You', staff: false, message: t.message, createdAt: t.createdAt }, ...t.replies];
    const result = await openDialog({
        title: `${t.subject}`, wide: true,
        confirmLabel: t.status === 'CLOSED' ? '' : 'Send reply', cancelLabel: 'Close',
        body: html`<p class="text-sm text-secondary" style="margin-bottom:14px">Reference <strong>${t.referenceCode}</strong> · ${raw(statusBadge(t.status))}</p>
            ${t.attachmentUrl ? raw(html`<p style="margin-bottom:14px"><a class="link" href="${t.attachmentUrl}" target="_blank" rel="noopener">${icon('paperclip', 'icon--sm')} ${t.attachmentName}</a></p>`) : ''}
            <div class="thread">${thread.map((m) => raw(html`<div class="bubble${m.staff ? ' bubble--staff' : ''}"><span class="bubble__meta">${m.staff ? 'Eco Swape team' : m.senderName === 'You' ? 'You' : m.senderName} · ${formatDateTime(m.createdAt)}</span><p>${m.message}</p></div>`))}</div>
            ${t.status === 'CLOSED' ? raw('<p class="alert alert--info" style="margin-top:14px">This ticket is closed. Open a new one if you need more help.</p>')
                : raw(html`<div class="field" style="margin-top:16px"><label for="reply">Your reply</label><textarea class="textarea" id="reply" name="message" maxlength="4000" required minlength="2"></textarea></div>`)}`,
        onSubmit: (data) => api.post(`/api/support/tickets/${id}/reply`, { message: data.get('message') }),
    });
    if (result) { toast('Reply sent.'); list.reload(); }
}
