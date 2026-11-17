// Item details: photo gallery, request dialog (with offered item for swaps), save and report.
import { api } from '../core/api.js';
import { toggleSave } from '../core/items.js';
import { cachedLocation, createMap, distanceKm, formatDistance, formatWalk, locateMe, pinIcon, routeLine, youIcon } from '../core/map.js';
import { $, $$, html, icon, openDialog, raw, toast, toastError } from '../core/ui.js';

export function init() {
    const main = $('#main');
    const itemId = main.dataset.itemId;
    const isSwap = main.dataset.listingType === 'SWAP';

    // Gallery thumbnails
    $$('.gallery__thumbs button').forEach((thumb) => thumb.addEventListener('click', () => {
        $('#gallery-main').src = thumb.dataset.src;
        $$('.gallery__thumbs button').forEach((b) => b.setAttribute('aria-current', String(b === thumb)));
    }));

    $('[data-request]')?.addEventListener('click', () => openRequestDialog(itemId, isSwap));

    const save = $('[data-save]');
    save?.addEventListener('click', async () => {
        const saved = await toggleSave(save);
        save.querySelector('[data-save-label]').textContent = saved ? 'Saved' : 'Save';
    });

    $('[data-report]')?.addEventListener('click', () => openReportDialog(itemId));
    initPickupMap();
}

/** Pickup point map with the distance from the visitor (location stays in the browser). */
async function initPickupMap() {
    const card = $('.pickup-card');
    const el = $('#item-map');
    if (!card || !el) return;
    const pickup = [Number(card.dataset.lat), Number(card.dataset.lng)];
    let L;
    let map;
    try {
        ({ L, map } = await createMap(el, { center: pickup, zoom: 16 }));
    } catch (err) {
        el.innerHTML = html`<p class="map-fallback">${err.message}</p>`;
        return;
    }
    L.marker(pickup, { icon: pinIcon(L, { iconKey: card.dataset.icon, label: card.dataset.place, active: true }), title: card.dataset.place }).addTo(map);

    let you = null;
    let line = null;
    const show = (pos) => {
        const km = distanceKm(pos, pickup);
        card.querySelector('[data-distance]').textContent = formatDistance(km);
        card.querySelector('[data-walk]').textContent = formatWalk(km).replace('≈ ', '');
        card.querySelector('[data-distance-note]').textContent = 'Straight-line distance; the walking time is an estimate.';
        you?.remove();
        line?.remove();
        you = L.marker(pos, { icon: youIcon(L), interactive: false, keyboard: false }).addTo(map);
        line = routeLine(L, pos, pickup, { active: true }).addTo(map);
        map.fitBounds(L.latLngBounds([pos, pickup]), { padding: [60, 60], maxZoom: 17 });
    };
    const saved = cachedLocation();
    if (saved) show(saved);
    const btn = card.querySelector('[data-locate]');
    btn?.addEventListener('click', async () => {
        btn.disabled = true;
        try { show(await locateMe()); } catch (err) { toastError(err); } finally { btn.disabled = false; }
    });
}

async function openRequestDialog(itemId, isSwap) {
    let offerHtml = '';
    if (isSwap) {
        const mine = await api.get('/api/items/mine/available');
        if (!mine.length) {
            await openDialog({
                title: 'You need an item to offer',
                text: 'Swaps are one-for-one. Post an item and wait for it to be approved, then you can offer it here. Giveaways and upcycling materials can be requested without offering anything.',
                confirmLabel: '', cancelLabel: 'Close',
                body: html`<a class="btn btn--primary btn--block" href="/items/new">${icon('package-plus')}Post an item</a>`,
            });
            return;
        }
        offerHtml = html`<fieldset class="field" style="border:0;padding:0;margin:0 0 16px">
            <legend class="field__label" style="margin-bottom:8px">Which of your items do you offer?</legend>
            <div class="offer-list">${mine.map((m, i) => raw(html`<label class="offer-option">
                <input type="radio" name="offeredItemId" value="${m.id}" ${i === 0 ? raw('checked') : ''} required>
                ${m.coverUrl ? raw(html`<img src="${m.coverUrl}" alt="">`) : raw('<span class="media-cell__ph"></span>')}
                <span><strong>${m.name}</strong><br><span class="text-xs text-secondary">${m.categoryName} · ${m.conditionLabel}</span></span>
            </label>`))}</div></fieldset>`;
    }
    const result = await openDialog({
        title: isSwap ? 'Request a swap' : 'Request this item',
        text: 'The owner will be notified and can accept or decline. You can cancel while it is pending.',
        confirmLabel: 'Send request',
        body: offerHtml + html`<div class="field"><label for="req-message">Message to the owner <span class="optional">(optional)</span></label>
            <textarea class="textarea" id="req-message" name="message" maxlength="500" placeholder="Hi! I'd love this for my course. I can meet at the library any weekday afternoon."></textarea></div>`,
        onSubmit: (data) => api.post(`/api/items/${itemId}/requests`, {
            offeredItemId: data.get('offeredItemId') ? Number(data.get('offeredItemId')) : null,
            message: (data.get('message') || '').trim() || null,
        }),
    });
    if (result) {
        toast('Request sent! We will notify you when the owner replies.');
        setTimeout(() => window.location.reload(), 900);
    }
}

async function openReportDialog(itemId) {
    const reasons = [
        ['INAPPROPRIATE_LISTING', 'Inappropriate listing'], ['MISLEADING_DESCRIPTION', 'Misleading description'],
        ['PROHIBITED_CONTENT', 'Prohibited item (e.g. for sale, unsafe)'], ['SUSPICIOUS_USER', 'Suspicious owner'], ['OTHER', 'Something else'],
    ];
    const result = await openDialog({
        title: 'Report this listing',
        text: 'Reports go to the Eco Swape admin team. The owner is not told who reported them.',
        confirmLabel: 'Send report', tone: 'danger',
        body: html`<div class="field"><label for="rep-reason">Reason</label>
            <select class="select" id="rep-reason" name="reason" required>${reasons.map(([v, l]) => raw(html`<option value="${v}">${l}</option>`))}</select></div>
            <div class="field" style="margin-top:14px"><label for="rep-desc">What's wrong? <span class="optional">(optional)</span></label>
            <textarea class="textarea" id="rep-desc" name="description" maxlength="1000"></textarea></div>`,
        onSubmit: (data) => api.post('/api/reports', { itemId: Number(itemId), reason: data.get('reason'), description: data.get('description') || null }),
    });
    if (result) toast('Thanks, your report was sent to the admin team.');
}

