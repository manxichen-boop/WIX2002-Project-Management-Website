// Application entry point (loaded on every page as an ES module).
// 1. wires the shared header (menus, search, logout, live badges)
// 2. shows any flash message
// 3. loads the module for the current page, named by <body data-page="…">.
import { api } from './core/api.js';
import { logout } from './core/auth.js';
import { formatDate, formatDateTime, timeAgo, toast } from './core/ui.js';

const PAGE_MODULES = {
    'home': () => import('./pages/home.js'),
    'explore': () => import('./pages/explore.js'),
    'pickup-map': () => import('./pages/pickup-map.js'),
    'messages': () => import('./pages/messages.js'),
    'item-details': () => import('./pages/item-details.js'),
    'post-item': () => import('./pages/post-item.js'),
    'dashboard': () => import('./pages/dashboard.js'),
    'my-listings': () => import('./pages/my-listings.js'),
    'my-requests': () => import('./pages/my-requests.js'),
    'received-requests': () => import('./pages/received-requests.js'),
    'saved-items': () => import('./pages/saved-items.js'),
    'exchange-history': () => import('./pages/exchange-history.js'),
    'notifications': () => import('./pages/notifications.js'),
    'profile': () => import('./pages/profile.js'),
    'support': () => import('./pages/support.js'),
    'login': () => import('./pages/login.js'),
    'register': () => import('./pages/register.js'),
    'forgot-password': () => import('./pages/forgot-password.js'),
    'account-status': () => import('./pages/account-status.js'),
    'reset-password': () => import('./pages/reset-password.js'),
    'admin/dashboard': () => import('./admin/dashboard.js'),
    'admin/pending-users': () => import('./admin/pending-users.js'),
    'admin/all-users': () => import('./admin/all-users.js'),
    'admin/pending-listings': () => import('./admin/pending-listings.js'),
    'admin/all-listings': () => import('./admin/all-listings.js'),
    'admin/exchanges': () => import('./admin/exchanges.js'),
    'admin/reports': () => import('./admin/reports.js'),
    'admin/support': () => import('./admin/support.js'),
    'admin/categories': () => import('./admin/categories.js'),
    'admin/audit-logs': () => import('./admin/audit-logs.js'),
};

function initHeader() {
    const header = document.querySelector('[data-header]');
    if (!header) return;
    const toggle = header.querySelector('[data-menu-toggle]');
    const searchToggle = header.querySelector('[data-search-toggle]');
    const searchPanel = header.querySelector('[data-search-panel]');

    const closeAll = (except) => {
        header.querySelectorAll('[data-dropdown].is-open').forEach((dd) => {
            if (dd === except) return;
            dd.classList.remove('is-open');
            dd.querySelector('[data-dropdown-trigger]')?.setAttribute('aria-expanded', 'false');
        });
    };

    toggle?.addEventListener('click', () => {
        const open = header.classList.toggle('nav-open');
        toggle.setAttribute('aria-expanded', String(open));
    });

    header.querySelectorAll('[data-dropdown]').forEach((dd) => {
        const trigger = dd.querySelector('[data-dropdown-trigger]');
        trigger.addEventListener('click', (e) => {
            const isLink = trigger.tagName === 'A';
            const touchLike = window.matchMedia('(hover: none), (max-width: 1060px)').matches;
            // Links (Explore) navigate on desktop; on touch the first tap opens the menu.
            if (isLink && !touchLike) return;
            if (isLink && dd.classList.contains('is-open')) return;
            e.preventDefault();
            const open = !dd.classList.contains('is-open');
            closeAll(dd);
            dd.classList.toggle('is-open', open);
            trigger.setAttribute('aria-expanded', String(open));
        });
    });

    searchToggle?.addEventListener('click', () => {
        const open = searchPanel.hidden;
        searchPanel.hidden = !open;
        searchToggle.setAttribute('aria-expanded', String(open));
        if (open) searchPanel.querySelector('input')?.focus();
    });

    document.addEventListener('click', (e) => {
        if (!header.contains(e.target)) {
            closeAll();
            header.classList.remove('nav-open');
            toggle?.setAttribute('aria-expanded', 'false');
            if (searchPanel && !searchPanel.hidden) { searchPanel.hidden = true; searchToggle?.setAttribute('aria-expanded', 'false'); }
        }
    });
    document.addEventListener('keydown', (e) => {
        if (e.key !== 'Escape') return;
        closeAll();
        header.classList.remove('nav-open');
        if (searchPanel) searchPanel.hidden = true;
    });

    document.querySelectorAll('[data-logout]').forEach((b) => b.addEventListener('click', logout));
}

/**
 * Keeps the notification and exchange-request badges current without a page reload.
 * Polls every 60 s while the tab is visible (lightweight COUNT queries).
 */
function initLiveBadges() {
    const unread = document.querySelectorAll('[data-unread-badge]');
    const requests = document.querySelectorAll('[data-requests-badge]');
    const messages = document.querySelectorAll('[data-messages-badge], [data-messages-count]');
    if (!unread.length) return;
    const refresh = async () => {
        if (document.hidden) return;
        try {
            const counts = await api.get('/api/notifications/counts');
            unread.forEach((b) => { b.textContent = counts.unread > 99 ? '99+' : counts.unread; b.hidden = counts.unread === 0; });
            requests.forEach((b) => { b.textContent = counts.pendingReceivedRequests; });
            messages.forEach((b) => { b.textContent = counts.unreadMessages > 99 ? '99+' : counts.unreadMessages; b.hidden = counts.unreadMessages === 0; });
            document.dispatchEvent(new CustomEvent('ecoswap:counts', { detail: counts }));
        } catch { /* badges are best-effort */ }
    };
    setInterval(refresh, 30000);
    document.addEventListener('ecoswap:refresh-counts', refresh);
    document.addEventListener('visibilitychange', () => { if (!document.hidden) refresh(); });
}

/** Server-rendered timestamps carry ISO values; render them in the visitor's own time zone. */
function formatTimes(root = document) {
    root.querySelectorAll('[data-relative]').forEach((el) => { el.textContent = timeAgo(el.dataset.relative); el.title = formatDateTime(el.dataset.relative); });
    root.querySelectorAll('[data-date]').forEach((el) => { el.textContent = formatDate(el.dataset.date); });
    root.querySelectorAll('[data-datetime]').forEach((el) => { el.textContent = formatDateTime(el.dataset.datetime); });
}

/** Show / hide toggles for password fields: <button data-toggle-password="inputId">. */
function initPasswordToggles() {
    document.querySelectorAll('[data-toggle-password]').forEach((btn) => {
        const input = document.getElementById(btn.dataset.togglePassword);
        btn.addEventListener('click', () => {
            const show = input.type === 'password';
            input.type = show ? 'text' : 'password';
            btn.setAttribute('aria-label', show ? 'Hide password' : 'Show password');
            btn.querySelector('use')?.setAttribute('href', `/img/icons.svg#i-${show ? 'eye-off' : 'eye'}`);
        });
    });
}

function showFlash() {
    const region = document.getElementById('toast-region');
    const params = new URLSearchParams(window.location.search);
    if (region?.dataset.flash) toast(region.dataset.flash);
    if (params.has('loggedOut')) toast('You have been logged out. See you soon!');
    if (params.has('reset')) toast('Your password has been changed. You can now log in.');
}

async function initPage() {
    const key = document.body.dataset.page;
    const loader = PAGE_MODULES[key];
    if (!loader) return;
    const module = await loader();
    await module.init?.();
}

initHeader();
formatTimes();
initPasswordToggles();
initLiveBadges();
showFlash();
initPage().catch((err) => console.error('Page script failed', err));
