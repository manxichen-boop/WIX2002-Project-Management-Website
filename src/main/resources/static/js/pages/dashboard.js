// Student dashboard: live counters, requests waiting for an answer, recent notifications.
import { api } from '../core/api.js';
import { bindRequestActions, requestCard } from '../core/requests.js';
import { $, emptyState, errorState, formatNumber, html, icon, raw, rowSkeletons, timeAgo } from '../core/ui.js';

const STATS = [
    ['activeListings', 'Active listings', 'package', '/my/listings', ''],
    ['listingsInReview', 'Listings in review', 'hourglass', '/my/listings', 'stat-card__icon--warning'],
    ['pendingSent', 'Requests sent', 'send', '/my/requests', 'stat-card__icon--forest'],
    ['pendingReceived', 'Requests received', 'inbox', '/my/received-requests', 'stat-card__icon--terracotta'],
    ['acceptedExchanges', 'Accepted exchanges', 'handshake', '/my/history', ''],
    ['completedExchanges', 'Completed exchanges', 'circle-check', '/my/history', 'stat-card__icon--forest'],
    ['savedItems', 'Saved items', 'heart', '/my/saved', 'stat-card__icon--terracotta'],
    ['unreadNotifications', 'Unread notifications', 'bell', '/notifications', 'stat-card__icon--warning'],
];

export async function init() {
    const stats = $('#stats');
    const awaiting = $('#awaiting');
    const recent = $('#recent');
    stats.innerHTML = STATS.map(() => '<div class="skeleton" style="height:84px;border-radius:16px"></div>').join('');
    awaiting.innerHTML = rowSkeletons(2);
    recent.innerHTML = rowSkeletons(3);

    const load = async () => {
        try {
            const s = await api.get('/api/dashboard/summary');
            stats.innerHTML = STATS.map(([key, label, ic, href, tone]) => html`<a class="stat-card" href="${href}">
                <span class="stat-card__icon ${tone}">${icon(ic)}</span>
                <div><div class="stat-card__value">${formatNumber(s.counts[key])}</div><div class="stat-card__label">${label}</div></div></a>`).join('');
            stats.removeAttribute('aria-busy');
            awaiting.innerHTML = s.awaitingYourAnswer.length
                ? s.awaitingYourAnswer.map(requestCard).join('')
                : emptyState('inbox', 'Nothing waiting', 'Requests for your items will appear here.');
            recent.innerHTML = s.recentNotifications.length
                ? html`<ul class="list" style="padding:4px 24px 12px">${s.recentNotifications.map((n) => raw(html`<li class="list__item">
                    ${n.read ? '' : raw('<span class="dot-unread" aria-label="Unread"></span>')}
                    <div class="list__main"><a class="list__title" href="${n.linkUrl || '/notifications'}">${n.title}</a>
                    <span class="list__sub">${n.message}</span></div><span class="list__sub nowrap">${timeAgo(n.createdAt)}</span></li>`))}</ul>`
                : emptyState('bell', 'No notifications yet', 'Updates about your listings and exchanges appear here.');
        } catch (err) {
            stats.innerHTML = '';
            awaiting.innerHTML = errorState(err.message);
            awaiting.querySelector('[data-retry]')?.addEventListener('click', load);
        }
    };
    bindRequestActions(awaiting, load);
    await load();
}
