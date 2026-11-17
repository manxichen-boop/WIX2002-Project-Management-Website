// UI primitives shared by all pages: safe HTML templating, toasts, dialogs, states, pagination,
// formatting and status badges. No page builds these by hand.

const ICONS = '/img/icons.svg';

/** Escapes a value for safe insertion into HTML. */
export function esc(value) {
    return String(value ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

/**
 * Markup that is already safe. Rendering it (innerHTML, join, template literals) uses toString(),
 * and nesting one html`` result inside another does not escape it a second time.
 */
class SafeHtml {
    constructor(value) { this.value = value; this.__raw = true; }
    toString() { return String(this.value); }
}

/** Tagged template: interpolated values are escaped unless they are SafeHtml (html`` / raw()). */
export function html(strings, ...values) {
    const out = strings.reduce((acc, s, i) => {
        if (i >= values.length) return acc + s;
        const v = values[i];
        const text = Array.isArray(v) ? v.map((x) => (x?.__raw ? x.value : esc(x))).join('') : v?.__raw ? v.value : esc(v);
        return acc + s + text;
    }, '');
    return new SafeHtml(out);
}
export const raw = (value) => new SafeHtml(value ?? '');

export function icon(name, cls = '') {
    return raw(`<svg class="icon ${cls}" aria-hidden="true" focusable="false"><use href="${ICONS}#i-${name}"></use></svg>`);
}

export const $ = (sel, root = document) => root.querySelector(sel);
export const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

// ---------- Toasts ----------
export function toast(message, type = 'success') {
    const region = document.getElementById('toast-region');
    if (!region) return;
    const el = document.createElement('div');
    el.className = `toast${type === 'error' ? ' toast--error' : ''}`;
    el.innerHTML = html`${icon(type === 'error' ? 'circle-alert' : 'circle-check')}<span>${message}</span>
        <button class="toast__close" type="button" aria-label="Dismiss">${icon('x', 'icon--sm')}</button>`;
    const remove = () => { el.classList.add('is-leaving'); setTimeout(() => el.remove(), 200); };
    el.querySelector('.toast__close').addEventListener('click', remove);
    region.append(el);
    setTimeout(remove, type === 'error' ? 7000 : 4500);
}
export const toastError = (err) => toast(err?.message || 'Something went wrong. Please try again.', 'error');

// ---------- Dialogs ----------
let lastFocus = null;

/**
 * Opens a dialog. `body` is trusted HTML (build it with html``). Resolves with the submitted
 * FormData (or true for plain confirmations), or null when dismissed.
 */
export function openDialog({ title, text = '', body = '', confirmLabel = 'Confirm', cancelLabel = 'Cancel', tone = 'primary', wide = false, onSubmit }) {
    return new Promise((resolve) => {
        const root = document.getElementById('modal-root');
        lastFocus = document.activeElement;
        const wrapper = document.createElement('div');
        wrapper.className = 'modal';
        const btnClass = tone === 'danger' ? 'btn--danger-solid' : 'btn--primary';
        wrapper.innerHTML = html`
            <div class="modal__backdrop" data-close></div>
            <form class="modal__dialog${wide ? ' modal__dialog--wide' : ''}" role="dialog" aria-modal="true" aria-labelledby="modal-title" novalidate>
                <button class="icon-btn modal__close" type="button" aria-label="Close" data-close>${icon('x')}</button>
                <h2 class="modal__title" id="modal-title">${title}</h2>
                ${text ? raw(html`<p class="modal__text">${text}</p>`) : ''}
                ${body ? raw(`<div class="modal__body">${body}</div>`) : ''}
                <div class="modal__error alert alert--danger" hidden></div>
                <div class="modal__actions">
                    ${cancelLabel ? raw(html`<button class="btn btn--secondary" type="button" data-close>${cancelLabel}</button>`) : ''}
                    ${confirmLabel ? raw(html`<button class="btn ${btnClass}" type="submit">${confirmLabel}</button>`) : ''}
                </div>
            </form>`;
        root.append(wrapper);
        document.body.classList.add('has-modal');
        const form = wrapper.querySelector('form');
        const close = (result) => {
            wrapper.remove();
            document.body.classList.remove('has-modal');
            document.removeEventListener('keydown', onKey);
            lastFocus?.focus?.();
            resolve(result);
        };
        const onKey = (e) => {
            if (e.key === 'Escape') close(null);
            if (e.key === 'Tab') trapFocus(e, form);
        };
        document.addEventListener('keydown', onKey);
        wrapper.querySelectorAll('[data-close]').forEach((b) => b.addEventListener('click', () => close(null)));
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            if (!form.reportValidity()) return;
            const data = new FormData(form);
            if (!onSubmit) return close(data.entries().next().done ? true : data);
            const submit = form.querySelector('[type="submit"]');
            setBusy(submit, true);
            try {
                const result = await onSubmit(data, form);
                close(result ?? true);
            } catch (err) {
                const box = form.querySelector('.modal__error');
                box.textContent = err.message;
                box.hidden = false;
                setBusy(submit, false);
            }
        });
        (form.querySelector('textarea, input, select') || form.querySelector('[type="submit"]'))?.focus();
    });
}

function trapFocus(e, container) {
    const focusable = $$('button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])', container).filter((el) => !el.disabled);
    if (!focusable.length) return;
    const first = focusable[0], last = focusable[focusable.length - 1];
    if (e.shiftKey && document.activeElement === first) { e.preventDefault(); last.focus(); }
    else if (!e.shiftKey && document.activeElement === last) { e.preventDefault(); first.focus(); }
}

export const confirmDialog = (opts) => openDialog({ ...opts });

/** Dialog that asks for a reason (required unless optional=true). Resolves with the text or null. */
export async function reasonDialog({ title, text, label = 'Reason', confirmLabel = 'Confirm', tone = 'danger', optional = false, placeholder = '' }) {
    const result = await openDialog({
        title, text, confirmLabel, tone,
        body: html`<div class="field">
            <label for="dlg-reason">${label}${optional ? raw(' <span class="optional">(optional)</span>') : ''}</label>
            <textarea class="textarea" id="dlg-reason" name="reason" maxlength="500" ${optional ? '' : raw('required minlength="3"')} placeholder="${placeholder}"></textarea>
        </div>`,
    });
    if (result === null) return null;
    return (result.get?.('reason') ?? '').trim();
}

// ---------- Busy state ----------
export function setBusy(button, busy) {
    if (!button) return;
    button.classList.toggle('is-loading', busy);
    button.disabled = busy;
    button.setAttribute('aria-busy', String(busy));
}

/** Runs an async action with the button in a busy state; shows errors as toasts. */
export async function withBusy(button, action) {
    setBusy(button, true);
    try {
        return await action();
    } catch (err) {
        toastError(err);
        return undefined;
    } finally {
        setBusy(button, false);
    }
}

// ---------- States ----------
export function emptyState(iconName, title, text, action = '') {
    return html`<div class="state">
        <span class="state__icon">${icon(iconName)}</span>
        <h3>${title}</h3><p>${text}</p>${raw(action)}
    </div>`;
}

export function errorState(message, retryLabel = 'Try again') {
    return html`<div class="state state--error">
        <span class="state__icon">${icon('triangle-alert')}</span>
        <h3>We couldn't load this</h3><p>${message}</p>
        <button class="btn btn--secondary" type="button" data-retry>${icon('refresh-cw')}${retryLabel}</button>
    </div>`;
}

export function cardSkeletons(count = 6) {
    return Array.from({ length: count }, () => `<div class="card skeleton-card" aria-hidden="true">
        <div class="skeleton skeleton--media"></div><div class="skeleton skeleton--title" style="margin-top:14px"></div>
        <div class="skeleton skeleton--text"></div><div class="skeleton skeleton--text" style="width:45%"></div></div>`).join('');
}

export function rowSkeletons(count = 5) {
    return `<div aria-hidden="true">${Array.from({ length: count }, () => '<div class="skeleton skeleton-row"></div>').join('')}</div>`;
}

/**
 * Generic list loader: shows skeletons, fetches, renders, handles empty/error with retry.
 * `load(page)` must return a PageResponse; `render(pageResponse)` returns HTML for the items.
 */
export function createListController({ container, pager, load, render, empty, skeleton = rowSkeletons() }) {
    let page = 0;
    const run = async () => {
        container.setAttribute('aria-busy', 'true');
        container.innerHTML = skeleton;
        if (pager) pager.innerHTML = '';
        try {
            const data = await load(page);
            if (!data.content.length) {
                container.innerHTML = empty;
            } else {
                container.innerHTML = render(data);
                if (pager) renderPagination(pager, data, (p) => { page = p; run(); container.scrollIntoView({ behavior: 'smooth', block: 'start' }); });
            }
            return data;
        } catch (err) {
            container.innerHTML = errorState(err.message);
            container.querySelector('[data-retry]')?.addEventListener('click', run);
            return null;
        } finally {
            container.removeAttribute('aria-busy');
        }
    };
    return {
        reload: run,
        reset() { page = 0; return run(); },
    };
}

export function renderPagination(target, data, onPage) {
    if (data.totalPages <= 1) { target.innerHTML = ''; return; }
    const current = data.page;
    const pages = new Set([0, data.totalPages - 1, current - 1, current, current + 1].filter((p) => p >= 0 && p < data.totalPages));
    const sorted = [...pages].sort((a, b) => a - b);
    let buttons = '';
    sorted.forEach((p, i) => {
        if (i > 0 && p - sorted[i - 1] > 1) buttons += '<span class="pagination__info">…</span>';
        buttons += `<button type="button" data-page="${p}" ${p === current ? 'aria-current="page"' : ''}>${p + 1}</button>`;
    });
    target.innerHTML = html`<nav class="pagination" aria-label="Pagination">
        <button type="button" data-page="${current - 1}" ${current === 0 ? raw('disabled') : ''} aria-label="Previous page">${icon('chevron-left', 'icon--sm')}</button>
        ${raw(buttons)}
        <button type="button" data-page="${current + 1}" ${current >= data.totalPages - 1 ? raw('disabled') : ''} aria-label="Next page">${icon('chevron-right', 'icon--sm')}</button>
    </nav>`;
    target.querySelectorAll('[data-page]').forEach((b) => b.addEventListener('click', () => onPage(Number(b.dataset.page))));
}

// ---------- Formatting ----------
const dateFmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric' });
const dateTimeFmt = new Intl.DateTimeFormat(undefined, { day: 'numeric', month: 'short', year: 'numeric', hour: 'numeric', minute: '2-digit' });
const rel = new Intl.RelativeTimeFormat(undefined, { numeric: 'auto' });

export const formatDate = (iso) => (iso ? dateFmt.format(new Date(iso)) : '—');
export const formatDateTime = (iso) => (iso ? dateTimeFmt.format(new Date(iso)) : '—');
export function timeAgo(iso) {
    if (!iso) return '';
    const seconds = Math.round((new Date(iso) - Date.now()) / 1000);
    const units = [['year', 31536000], ['month', 2592000], ['week', 604800], ['day', 86400], ['hour', 3600], ['minute', 60]];
    for (const [unit, s] of units) if (Math.abs(seconds) >= s) return rel.format(Math.round(seconds / s), unit);
    return 'just now';
}
export const formatNumber = (n) => new Intl.NumberFormat().format(n ?? 0);
export const humanize = (value) => String(value ?? '').toLowerCase().replace(/_/g, ' ').replace(/^\w/, (c) => c.toUpperCase());

// ---------- Status badges ----------
const TONES = {
    PENDING_APPROVAL: 'warning', ACTIVE: 'success', REJECTED: 'danger', SUSPENDED: 'danger',
    APPROVED: 'success', NEEDS_CHANGES: 'warning',
    AVAILABLE: 'success', RESERVED: 'info', COMPLETED: 'neutral', ARCHIVED: 'neutral',
    PENDING: 'warning', ACCEPTED: 'info', CANCELLED: 'neutral', CLOSED: 'neutral',
    OPEN: 'warning', UNDER_REVIEW: 'info', RESOLVED: 'success', DISMISSED: 'neutral',
    IN_PROGRESS: 'info', WAITING_FOR_USER: 'warning',
};
const LABELS = { PENDING_APPROVAL: 'Pending approval', NEEDS_CHANGES: 'Changes requested', WAITING_FOR_USER: 'Waiting for you', UNDER_REVIEW: 'Under review', IN_PROGRESS: 'In progress' };

export function statusBadge(status, labelOverride) {
    return html`<span class="badge badge--${TONES[status] || 'neutral'}">${labelOverride || LABELS[status] || humanize(status)}</span>`;
}

export function typeBadge(type) {
    const labels = { SWAP: 'Swap', GIVEAWAY: 'Giveaway', UPCYCLING: 'Upcycling' };
    return html`<span class="badge badge--plain badge--${String(type).toLowerCase()}">${labels[type] || type}</span>`;
}

export function photo(url, alt = '') {
    return url
        ? html`<img src="${url}" alt="${alt}" loading="lazy">`
        : html`<div class="photo-placeholder" role="img" aria-label="No photo">${icon('image-plus')}</div>`;
}

export function debounce(fn, wait = 300) {
    let t;
    return (...args) => { clearTimeout(t); t = setTimeout(() => fn(...args), wait); };
}
