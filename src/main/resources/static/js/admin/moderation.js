// Shared moderation helpers for the admin pages: confirm/reason dialogs wired to PATCH endpoints,
// and listing review rendering (used by pending-listings and all-listings).
import { api } from '../core/api.js';
import { confirmDialog, formatDate, formatDateTime, html, humanize, icon, openDialog, raw, reasonDialog, statusBadge, toast, toastError, typeBadge } from '../core/ui.js';

/**
 * Runs a moderation action. With `reason` set, a reason dialog is shown first (required unless
 * reason.optional). Returns the API response, or null when the admin cancelled.
 */
export async function moderate(url, { title, text, confirmLabel, tone = 'primary', reason = null, success }) {
    let body;
    if (reason) {
        const value = await reasonDialog({ title, text, label: reason.label || 'Reason (shown to the student)',
            confirmLabel, tone, optional: reason.optional, placeholder: reason.placeholder || '' });
        if (value === null) return null;
        body = { reason: value };
    } else {
        const ok = await confirmDialog({ title, text, confirmLabel, tone });
        if (!ok) return null;
    }
    try {
        const result = await api.patch(url, body);
        toast(success);
        return result;
    } catch (err) {
        toastError(err);
        return null;
    }
}

const LISTING_ACTIONS = {
    approve: { label: 'Approve', tone: 'primary', icon: 'check', success: 'Listing approved and now live in Explore.',
        title: 'Approve this listing?', text: 'It will appear in Explore and the owner will be notified.' },
    'request-changes': { label: 'Request changes', tone: 'primary', icon: 'pencil', success: 'Change request sent to the owner.',
        title: 'Ask the owner for changes', text: 'The listing stays hidden until the owner edits and resubmits it.',
        reason: { label: 'What should the owner change?', placeholder: 'e.g. Please add a clearer photo of the cover.' } },
    reject: { label: 'Reject', tone: 'danger', icon: 'circle-x', success: 'Listing rejected.',
        title: 'Reject this listing?', text: 'The listing stays hidden. The owner sees your reason and can edit and resubmit.', reason: {} },
    suspend: { label: 'Suspend', tone: 'danger', icon: 'ban', success: 'Listing suspended.',
        title: 'Suspend this listing?', text: 'It disappears from Explore; pending requests are closed and any reservation is released.', reason: {} },
    archive: { label: 'Archive (prohibited)', tone: 'danger', icon: 'archive', success: 'Listing archived.',
        title: 'Archive as prohibited content?', text: 'Removes the listing from circulation permanently.', reason: {} },
};

/** Which actions make sense for a listing in its current state. */
export function listingActions(item) {
    if (item.availabilityStatus === 'COMPLETED') return [];
    const s = item.approvalStatus;
    if (s === 'PENDING_APPROVAL') return ['approve', 'request-changes', 'reject'];
    if (s === 'APPROVED') return item.availabilityStatus === 'ARCHIVED' ? [] : ['suspend', 'archive'];
    if (s === 'SUSPENDED') return ['approve', 'archive'];
    return ['approve', 'archive'];   // REJECTED / NEEDS_CHANGES: admin may still approve
}

export function actionButtons(item, size = 'btn--sm') {
    return listingActions(item).map((a) => {
        const def = LISTING_ACTIONS[a];
        const cls = def.tone === 'danger' ? 'btn--danger' : a === 'approve' ? 'btn--primary' : 'btn--secondary';
        return html`<button class="btn ${cls} ${size}" type="button" data-action="${a}" data-id="${item.id}">${icon(def.icon)}${def.label}</button>`;
    }).join('');
}

export async function runListingAction(action, id) {
    const def = LISTING_ACTIONS[action];
    return moderate(`/api/admin/items/${id}/${action}`, { ...def, confirmLabel: def.label });
}

/** Full listing review dialog: photos, details, owner and decision history. */
export async function openListingDetail(id) {
    let item;
    try { item = await api.get(`/api/admin/items/${id}`); } catch (err) { toastError(err); return; }
    const history = item.history.length
        ? raw(html`<ul class="history-list">${item.history.map((h) => raw(html`<li><strong>${humanize(h.decision)}</strong> by ${h.adminName}
            · ${formatDateTime(h.createdAt)}${h.reason ? raw(html`<br><span class="text-secondary">${h.reason}</span>`) : ''}</li>`))}</ul>`)
        : raw('<p class="text-sm text-secondary">No decisions yet.</p>');
    await openDialog({
        title: item.name, wide: true, confirmLabel: '', cancelLabel: 'Close',
        body: html`<div class="split">
            <div class="review-card__photos">${item.images.map((img) => raw(html`<a href="${img.url}" target="_blank" rel="noopener"><img src="${img.url}" alt="Photo of ${item.name}"></a>`))}</div>
            <div class="stack">
                <div class="cluster">${typeBadge(item.listingType)}${statusBadge(item.approvalStatus)}${statusBadge(item.availabilityStatus)}</div>
                <p style="white-space:pre-wrap">${item.description}</p>
                <dl class="dl">
                    <dt>Category</dt><dd>${item.categoryName}</dd><dt>Condition</dt><dd>${item.conditionLabel}</dd>
                    <dt>Pickup</dt><dd>${item.pickupLocation}</dd><dt>Weight</dt><dd>${item.approximateWeight ? `${item.approximateWeight} kg` : '—'}</dd>
                    <dt>Owner</dt><dd>${item.ownerName}<br><span class="text-xs text-secondary">${item.ownerEmail} · ${item.ownerUniversity}</span></dd>
                    <dt>Submitted</dt><dd>${formatDate(item.createdAt)}</dd>
                </dl>
                ${item.rejectionReason ? raw(html`<div class="alert alert--warning">${icon('info')}<div><strong>Current note</strong>${item.rejectionReason}</div></div>`) : ''}
                <h3 style="font-size:1rem">Approval history</h3>${history}
            </div></div>`,
    });
}
