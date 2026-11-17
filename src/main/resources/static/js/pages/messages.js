// Messages: inbox of exchange chats + the open thread. New messages arrive by polling every
// 3 seconds while the tab is visible; the inbox refreshes every 15 seconds.
import { api } from '../core/api.js';
import { $, confirmDialog, html, icon, photo, raw, statusBadge, timeAgo, toast, toastError } from '../core/ui.js';

const THREAD_POLL_MS = 3000;
const INBOX_POLL_MS = 15000;

const timeFmt = new Intl.DateTimeFormat(undefined, { hour: 'numeric', minute: '2-digit' });
const dayFmt = new Intl.DateTimeFormat(undefined, { weekday: 'long', day: 'numeric', month: 'short', year: 'numeric' });

export function init() {
    const chat = $('[data-chat]');
    const list = $('#chat-list');
    const pane = $('#chat-thread');
    const state = { conversations: [], openId: Number($('#main').dataset.openRequest) || null, lastId: 0, firstId: null, thread: null, sending: false };
    let threadTimer = null;

    // ------------------------------------------------------------------ inbox
    const renderInbox = () => {
        if (!state.conversations.length) {
            list.innerHTML = html`<li class="chat__placeholder">${icon('message-circle')}<strong>No chats yet</strong>
                <span>A chat opens when an owner accepts a request: yours, or one you accepted.</span>
                <a class="btn btn--primary btn--sm" href="/explore">Explore items</a></li>`;
            return;
        }
        list.innerHTML = state.conversations.map((c) => html`<li>
            <a class="convo${c.requestId === state.openId ? ' is-active' : ''}${c.unread ? ' has-unread' : ''}" href="/messages/${c.requestId}" data-open="${c.requestId}">
                <span class="convo__media">${photo(c.item.coverUrl, '')}<span class="convo__avatar">${c.other.avatarUrl ? raw(html`<img src="${c.other.avatarUrl}" alt="">`) : c.other.initials}</span></span>
                <span class="convo__body">
                    <span class="convo__top"><strong>${c.other.fullName}</strong>${c.lastMessage ? raw(html`<time>${timeAgo(c.lastMessage.createdAt)}</time>`) : ''}</span>
                    <span class="convo__item">${c.item.name}</span>
                    <span class="convo__last">${c.lastMessage ? `${c.lastMessage.mine ? 'You: ' : ''}${c.lastMessage.preview}` : 'Say hello and agree on a pickup time'}</span>
                </span>
                ${c.unread ? raw(html`<span class="convo__badge" aria-label="${`${c.unread} unread`}">${c.unread > 99 ? '99+' : c.unread}</span>`) : ''}
            </a></li>`).join('');
    };

    const loadInbox = async () => {
        try {
            state.conversations = await api.get('/api/chats');
            renderInbox();
        } catch (err) {
            list.innerHTML = html`<li class="chat__placeholder">${err.message}</li>`;
        }
    };

    list.addEventListener('click', (e) => {
        const a = e.target.closest('[data-open]');
        if (!a || e.ctrlKey || e.metaKey) return;
        e.preventDefault();
        open(Number(a.dataset.open), true);
    });

    // ------------------------------------------------------------------ thread
    const bubble = (m) => html`<div class="bubble${m.mine ? ' bubble--mine' : ''}" data-id="${m.id}">${m.body}<span class="bubble__meta">${timeFmt.format(new Date(m.createdAt))}${m.mine && m.readAt ? ' · Seen' : ''}</span></div>`;
    const dayKey = (iso) => new Date(iso).toDateString();
    const withDays = (messages, prevIso) => {
        let prev = prevIso ? dayKey(prevIso) : null;
        return messages.map((m) => {
            const d = dayKey(m.createdAt);
            const sep = d !== prev ? html`<div class="thread__day">${dayFmt.format(new Date(m.createdAt))}</div>` : '';
            prev = d;
            return `${sep}${bubble(m)}`;
        }).join('');
    };

    const handoverBar = (c) => {
        if (c.status === 'COMPLETED') return html`<div class="thread__handover is-done"><span>${icon('circle-check')} Exchange completed. Thank you for reusing!</span></div>`;
        if (c.status !== 'ACCEPTED') return html`<div class="thread__handover"><span>${icon('info')} This exchange is ${c.status.toLowerCase()}. The chat is read-only.</span></div>`;
        const otherConfirmed = c.iAmOwner ? c.requesterConfirmed : c.ownerConfirmed;
        const text = c.confirmedByMe
            ? `You confirmed the handover. Waiting for ${c.other.firstName} to confirm.`
            : otherConfirmed ? `${c.other.firstName} confirmed the handover. Confirm too once the item has changed hands.`
                : 'After you meet, both of you confirm the handover to complete the exchange.';
        return html`<div class="thread__handover${!c.confirmedByMe && otherConfirmed ? ' is-todo' : ''}"><span>${icon('handshake')} ${text}</span>
            ${c.confirmedByMe ? '' : raw(html`<button class="btn btn--primary btn--sm" type="button" data-confirm-handover>${icon('check')}Confirm handover</button>`)}</div>`;
    };

    const renderThread = (t) => {
        const c = t.conversation;
        state.thread = c;
        pane.innerHTML = html`
            <header class="thread__head">
                <button class="icon-btn thread__back" type="button" aria-label="Back to conversations" data-back>${icon('arrow-left')}</button>
                <span class="convo__media">${photo(c.item.coverUrl, '')}</span>
                <div class="thread__who"><strong>${c.other.fullName}</strong>
                    <a href="/items/${c.item.id}">${c.item.name}${c.offeredItem ? ` ⇄ ${c.offeredItem.name}` : ''}</a></div>
                ${statusBadge(c.status)}
            </header>
            ${handoverBar(c)}
            <div class="thread__messages" id="thread-messages">
                ${t.hasOlder ? raw(html`<button class="btn btn--ghost btn--sm thread__older" type="button" data-older>Load earlier messages</button>`) : ''}
                ${t.messages.length ? raw(withDays(t.messages)) : raw(html`<p class="thread__empty" data-empty>${icon('map-pin', 'icon--xs')} Pickup point: ${c.item.pickupLocation}.<br>Say hello to ${c.other.firstName} and agree on a time.</p>`)}
            </div>
            ${c.canSend ? raw(html`<form class="composer" id="composer" autocomplete="off">
                <label class="sr-only" for="composer-text">Message to ${c.other.firstName}</label>
                <textarea id="composer-text" rows="1" maxlength="2000" placeholder="Write a message…" required></textarea>
                <button class="btn btn--primary" type="submit" aria-label="Send message">${icon('send')}</button>
            </form>`) : raw(html`<p class="composer__closed">${icon('lock', 'icon--xs')} This chat is read-only because the exchange is ${c.status.toLowerCase()}.</p>`)}`;
        state.firstId = t.messages[0]?.id ?? null;
        state.lastId = t.messages.at(-1)?.id ?? 0;
        state.lastIso = t.messages.at(-1)?.createdAt ?? null;
        scrollDown(true);
        bindComposer();
    };

    const box = () => $('#thread-messages');
    const scrollDown = (force = false) => {
        const el = box();
        if (!el) return;
        const nearBottom = el.scrollHeight - el.scrollTop - el.clientHeight < 140;
        if (force || nearBottom) el.scrollTop = el.scrollHeight;
    };

    const appendMessages = (messages) => {
        const fresh = messages.filter((m) => m.id > state.lastId && !box()?.querySelector(`[data-id="${m.id}"]`));
        if (!fresh.length) return;
        box().querySelector('[data-empty]')?.remove();
        box().insertAdjacentHTML('beforeend', withDays(fresh, state.lastIso));
        state.lastId = fresh.at(-1).id;
        state.lastIso = fresh.at(-1).createdAt;
        scrollDown(fresh.some((m) => m.mine));
    };

    const poll = async () => {
        if (!state.openId || document.hidden) return;
        try {
            const t = await api.get(`/api/chats/${state.openId}`, { after: state.lastId });
            appendMessages(t.messages);
            const c = t.conversation;
            const prev = state.thread;
            // Refresh the header bar when the handover state changed (e.g. the other student confirmed).
            if (prev && (prev.status !== c.status || prev.ownerConfirmed !== c.ownerConfirmed || prev.requesterConfirmed !== c.requesterConfirmed)) {
                const t2 = await api.get(`/api/chats/${state.openId}`);
                renderThread(t2);
            }
            // "Seen" receipts on my messages.
            if (t.messages.length === 0 && state.thread) markSeen();
        } catch { /* transient; try again on the next tick */ }
    };
    const markSeen = async () => {
        // Cheap way to refresh read receipts: re-read the last page every ~15 s.
        const now = Date.now();
        if (state.seenAt && now - state.seenAt < 15000) return;
        state.seenAt = now;
        try {
            const t = await api.get(`/api/chats/${state.openId}`);
            const byId = new Map(t.messages.map((m) => [m.id, m]));
            box()?.querySelectorAll('.bubble--mine').forEach((b) => {
                const m = byId.get(Number(b.dataset.id));
                if (m?.readAt && !b.querySelector('.bubble__meta').textContent.includes('Seen')) b.querySelector('.bubble__meta').textContent += ' · Seen';
            });
        } catch { /* ignore */ }
    };

    const open = async (requestId, push) => {
        state.openId = requestId;
        state.seenAt = 0;
        chat.classList.add('has-thread');
        if (push) history.pushState({ requestId }, '', `/messages/${requestId}`);
        pane.innerHTML = '<div class="chat__placeholder">Loading…</div>';
        try {
            renderThread(await api.get(`/api/chats/${requestId}`));
            // Opening the thread marked it read: update the inbox badge and the header counters.
            const c = state.conversations.find((x) => x.requestId === requestId);
            if (c) c.unread = 0;
            renderInbox();
            document.dispatchEvent(new CustomEvent('ecoswap:refresh-counts'));
        } catch (err) {
            pane.innerHTML = html`<div class="chat__placeholder">${icon('circle-alert')}<strong>${err.message}</strong></div>`;
        }
        clearInterval(threadTimer);
        threadTimer = setInterval(poll, THREAD_POLL_MS);
    };

    const bindComposer = () => {
        const form = $('#composer');
        if (!form) return;
        const text = $('#composer-text');
        const grow = () => { text.style.height = 'auto'; text.style.height = `${Math.min(text.scrollHeight, 160)}px`; };
        text.addEventListener('input', grow);
        text.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' && !e.shiftKey && !e.isComposing) { e.preventDefault(); form.requestSubmit(); }
        });
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            const body = text.value.trim();
            if (!body || state.sending) return;
            state.sending = true;
            const button = form.querySelector('button');
            button.disabled = true;
            try {
                const m = await api.post(`/api/chats/${state.openId}/messages`, { body });
                text.value = '';
                grow();
                appendMessages([m]);
                const c = state.conversations.find((x) => x.requestId === state.openId);
                if (c) { c.lastMessage = { preview: body.slice(0, 90), createdAt: m.createdAt, mine: true }; renderInbox(); }
            } catch (err) {
                toastError(err);
            } finally {
                state.sending = false;
                button.disabled = false;
                text.focus();
            }
        });
        text.focus({ preventScroll: true });
    };

    pane.addEventListener('click', async (e) => {
        if (e.target.closest('[data-back]')) {
            chat.classList.remove('has-thread');
            state.openId = null;
            clearInterval(threadTimer);
            history.pushState({}, '', '/messages');
            renderInbox();
            return;
        }
        if (e.target.closest('[data-older]')) {
            const btn = e.target.closest('[data-older]');
            btn.disabled = true;
            try {
                const older = await api.get(`/api/chats/${state.openId}/older`, { before: state.firstId });
                const el = box();
                const prevHeight = el.scrollHeight;
                btn.remove();
                el.insertAdjacentHTML('afterbegin', (older.length === 50 ? html`<button class="btn btn--ghost btn--sm thread__older" type="button" data-older>Load earlier messages</button>` : '') + withDays(older));
                el.scrollTop = el.scrollHeight - prevHeight;
                if (older.length) state.firstId = older[0].id;
            } catch (err) { toastError(err); btn.disabled = false; }
            return;
        }
        if (e.target.closest('[data-confirm-handover]')) {
            const ok = await confirmDialog({ title: 'Confirm the handover?', text: 'Only confirm once the item has changed hands. The exchange completes when both of you confirm.', confirmLabel: 'Yes, confirm' });
            if (!ok) return;
            try {
                const r = await api.patch(`/api/requests/${state.openId}/confirm-completion`);
                toast(r?.status === 'COMPLETED' ? 'Exchange completed! It now counts towards the community impact.'
                    : 'Your confirmation was recorded. The exchange completes when the other student confirms too.');
                renderThread(await api.get(`/api/chats/${state.openId}`));
                loadInbox();
            } catch (err) { toastError(err); }
        }
    });

    window.addEventListener('popstate', () => {
        const m = location.pathname.match(/^\/messages\/(\d+)$/);
        if (m) open(Number(m[1]), false);
        else { chat.classList.remove('has-thread'); state.openId = null; clearInterval(threadTimer); renderInbox(); }
    });
    document.addEventListener('visibilitychange', () => { if (!document.hidden) { poll(); loadInbox(); } });
    setInterval(() => { if (!document.hidden) loadInbox(); }, INBOX_POLL_MS);

    loadInbox().then(() => { if (state.openId) open(state.openId, false); });
}
