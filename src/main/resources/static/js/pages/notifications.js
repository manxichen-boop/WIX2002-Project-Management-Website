// Notifications: all / unread, mark one or all as read, open the related page.
import { api } from '../core/api.js';
import { $, $$, createListController, emptyState, html, icon, raw, timeAgo, formatDateTime, toast, toastError } from '../core/ui.js';

const ICONS = {
    REQUEST_RECEIVED: 'inbox', REQUEST_ACCEPTED: 'circle-check', REQUEST_REJECTED: 'circle-x', REQUEST_CANCELLED: 'rotate-ccw',
    REQUEST_CLOSED: 'ban', EXCHANGE_CONFIRMATION: 'handshake', EXCHANGE_COMPLETED: 'sparkles', LISTING_APPROVED: 'circle-check',
    LISTING_REJECTED: 'circle-x', LISTING_CHANGES_REQUESTED: 'pencil', LISTING_SUSPENDED: 'ban', LISTING_SUBMITTED: 'package-plus',
    REGISTRATION_SUBMITTED: 'user-plus', REGISTRATION_APPROVED: 'sparkles', SUPPORT_REPLY: 'message-square',
    SUPPORT_STATUS_CHANGED: 'life-buoy', SUPPORT_TICKET_CREATED: 'life-buoy', REPORT_SUBMITTED: 'flag',
};

export function init() {
    let unreadOnly = false;
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('bell', "You're all caught up", 'New updates about your listings and exchanges appear here.'),
        load: (page) => api.get('/api/notifications', { unreadOnly, page, size: 15 }),
        render: (data) => data.content.map((n) => html`<div class="notification${n.read ? '' : ' is-unread'}" data-id="${n.id}">
            <span class="notification__icon">${icon(ICONS[n.type] || 'bell')}</span>
            <div class="notification__body">
                <p class="notification__title">${n.linkUrl ? raw(html`<a href="${n.linkUrl}" data-open>${n.title}</a>`) : n.title}</p>
                <p class="notification__text">${n.message}</p>
                <p class="notification__time" title="${formatDateTime(n.createdAt)}">${timeAgo(n.createdAt)}</p>
            </div>
            ${n.read ? '' : raw(html`<button class="btn btn--ghost btn--sm" type="button" data-read aria-label="Mark as read">${icon('check')}</button>`)}
        </div>`).join(''),
    });

    $$('[data-status]').forEach((t) => t.addEventListener('click', () => {
        unreadOnly = t.dataset.status === 'unread';
        $$('[data-status]').forEach((x) => x.setAttribute('aria-selected', String(x === t)));
        list.reset();
    }));
    rows.addEventListener('click', async (e) => {
        const row = e.target.closest('[data-id]');
        if (!row) return;
        if (e.target.closest('[data-read]')) {
            try { await api.patch(`/api/notifications/${row.dataset.id}/read`); list.reload(); } catch (err) { toastError(err); }
        } else if (e.target.closest('[data-open]') && row.classList.contains('is-unread')) {
            api.patch(`/api/notifications/${row.dataset.id}/read`).catch(() => {});   // navigation continues
        }
    });
    $('[data-read-all]').addEventListener('click', async () => {
        try {
            const r = await api.patch('/api/notifications/read-all');
            toast(r.message);
            $$('[data-unread-badge]').forEach((b) => { b.hidden = true; });
            list.reload();
        } catch (err) { toastError(err); }
    });
    list.reload();
}
