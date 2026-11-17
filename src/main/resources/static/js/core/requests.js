// Exchange request cards and actions, shared by My Requests, Received Requests, History and
// the dashboard. Which buttons appear comes from the server's `actions` flags.
import { api } from './api.js';
import { confirmDialog, formatDate, html, icon, photo, raw, reasonDialog, statusBadge, toast, toastError, typeBadge } from './ui.js';

export function requestCard(r) {
    const mine = r.viewerRole === 'OWNER';
    const other = mine ? r.requester : r.owner;
    const a = r.actions;
    const meConfirmed = mine ? r.ownerConfirmed : r.requesterConfirmed;
    const otherConfirmed = mine ? r.requesterConfirmed : r.ownerConfirmed;
    const otherFirst = String(other.name || 'the other student').split(' ')[0];
    const progressText = meConfirmed && !otherConfirmed ? `You confirmed. Waiting for ${otherFirst} to confirm`
        : !meConfirmed && otherConfirmed ? `${otherFirst} confirmed. Please confirm once you have handed over / received the item`
            : 'Both of you confirm the handover to complete the exchange';
    const confirmations = r.status === 'ACCEPTED'
        ? raw(html`<p class="request__progress${!meConfirmed && otherConfirmed ? ' request__progress--todo' : ''}">${icon('check-check', 'icon--xs')} ${Number(r.ownerConfirmed) + Number(r.requesterConfirmed)}/2 · ${progressText}</p>`) : '';
    const chat = r.status === 'ACCEPTED' || r.status === 'COMPLETED'
        ? raw(html`<a class="btn btn--secondary btn--sm" href="/messages/${r.id}">${icon('message-circle')}Chat with ${otherFirst}</a>`) : '';
    const contact = other.contactEmail && r.status === 'ACCEPTED'
        ? raw(html`<span>${icon('mail')}<a class="link" href="mailto:${other.contactEmail}">${other.contactEmail}</a></span>`) : '';
    return html`<article class="card request" data-request-id="${r.id}">
        <a class="request__media" href="/items/${r.item.id}">${photo(r.item.coverUrl, r.item.name)}</a>
        <div>
            <div class="cluster">${statusBadge(r.status)}${typeBadge(r.item.listingType)}</div>
            <h3 class="request__title" style="margin-top:6px"><a href="/items/${r.item.id}">${r.item.name}</a></h3>
            <div class="request__meta">
                <span>${icon('user')}${mine ? 'From' : 'Owner:'} ${other.name}</span>
                <span>${icon('calendar')}${formatDate(r.createdAt)}</span>
                <span>${icon('map-pin')}${r.item.pickupLocation}</span>${contact}
                ${r.completedAt ? raw(html`<span>${icon('circle-check')}Completed ${formatDate(r.completedAt)}</span>`) : ''}
            </div>
            ${r.offeredItem ? raw(html`<a class="request__offer" href="/items/${r.offeredItem.id}">${r.offeredItem.coverUrl ? raw(html`<img src="${r.offeredItem.coverUrl}" alt="">`) : ''}
                ${mine ? 'Offers' : 'You offer'}: ${r.offeredItem.name}</a>`) : ''}
            ${r.message ? raw(html`<p class="request__message">“${r.message}”</p>`) : ''}
            ${r.statusReason ? raw(html`<p class="request__message">${icon('info', 'icon--xs')} ${r.statusReason}</p>`) : ''}
        </div>
        <div class="request__actions">
            ${a.accept ? raw(html`<button class="btn btn--primary btn--sm" type="button" data-req-action="accept">${icon('check')}Accept</button>`) : ''}
            ${a.reject ? raw(html`<button class="btn btn--danger btn--sm" type="button" data-req-action="reject">${icon('x')}Decline</button>`) : ''}
            ${a.confirm ? raw(html`<button class="btn btn--primary btn--sm" type="button" data-req-action="confirm">${icon('handshake')}Confirm handover</button>`) : ''}
            ${chat}
            ${a.cancel ? raw(html`<button class="btn btn--secondary btn--sm" type="button" data-req-action="cancel">Cancel request</button>`) : ''}
            ${a.withdraw ? raw(html`<button class="btn btn--ghost btn--sm" type="button" data-req-action="withdraw">Withdraw</button>`) : ''}
            ${confirmations}
        </div>
    </article>`;
}

const ACTIONS = {
    accept: { title: 'Accept this request?', text: 'Your item (and the offered item, for swaps) will be reserved and other pending requests for it will be closed. You can then arrange the handover.', confirmLabel: 'Accept request', url: 'accept', done: 'Request accepted. Arrange the handover with the other student.' },
    reject: { title: 'Decline this request?', text: 'The student will be notified. You can add a short note.', confirmLabel: 'Decline', tone: 'danger', reason: { optional: true, label: 'Note to the student' }, url: 'reject', done: 'Request declined.' },
    cancel: { title: 'Cancel your request?', text: 'The owner will be notified.', confirmLabel: 'Cancel request', tone: 'danger', url: 'cancel', done: 'Request cancelled.' },
    withdraw: { title: 'Withdraw from this exchange?', text: 'The reserved items become available again. Please tell the other student why.', confirmLabel: 'Withdraw', tone: 'danger', reason: { label: 'Reason' }, url: 'cancel', done: 'You withdrew from the exchange.' },
    confirm: { title: 'Confirm the handover?', text: 'Only confirm once the item has changed hands. The exchange completes when both of you confirm.', confirmLabel: 'Yes, confirm', url: 'confirm-completion', done: 'Thanks! Your confirmation was recorded.' },
};

/** Delegated handler for [data-req-action] buttons; calls onDone() after a successful change. */
export function bindRequestActions(container, onDone) {
    container.addEventListener('click', async (e) => {
        const button = e.target.closest('[data-req-action]');
        if (!button) return;
        const id = button.closest('[data-request-id]').dataset.requestId;
        const def = ACTIONS[button.dataset.reqAction];
        let body;
        if (def.reason) {
            const reason = await reasonDialog({ title: def.title, text: def.text, label: def.reason.label, confirmLabel: def.confirmLabel, tone: def.tone || 'primary', optional: def.reason.optional });
            if (reason === null) return;
            body = { reason };
        } else if (!(await confirmDialog({ title: def.title, text: def.text, confirmLabel: def.confirmLabel, tone: def.tone || 'primary' }))) {
            return;
        }
        try {
            const result = await api.patch(`/api/requests/${id}/${def.url}`, body);
            if (def.url === 'confirm-completion') {
                toast(result?.status === 'COMPLETED'
                    ? 'Exchange completed! It now counts towards the community impact.'
                    : 'Your confirmation was recorded. The exchange completes when the other student confirms too.');
            } else {
                toast(def.done);
            }
            document.dispatchEvent(new CustomEvent('ecoswap:refresh-counts'));
            onDone?.();
        } catch (err) {
            toastError(err);
            onDone?.();
        }
    });
}
