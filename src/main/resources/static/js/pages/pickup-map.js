// Pickup map: every approved, available listing that has a map pin, with distance and an
// approximate walking time from the visitor once they share their location.
import { api } from '../core/api.js';
import { cachedLocation, createMap, distanceKm, formatDistance, formatWalk, locateMe, pinIcon, routeLine, youIcon } from '../core/map.js';
import { $, $$, debounce, html, icon, toastError } from '../core/ui.js';

const NEAR_KM = 5;

export async function init() {
    const list = $('#pm-list');
    const search = $('#pm-search');
    const filterToggle = $('[data-filter-toggle]');
    const filters = $('#pm-filters');
    const locateBtn = $('[data-locate]');

    let L;
    let map;
    try {
        ({ L, map } = await createMap($('#pm-map')));
    } catch (err) {
        $('#pm-map').innerHTML = html`<p class="map-fallback">${err.message}</p>`;
    }

    let items = [];
    try {
        items = await api.get('/api/items/map');
    } catch (err) {
        list.innerHTML = html`<li class="pickup-empty">${err.message}</li>`;
        return;
    }

    const state = { q: '', category: '', me: cachedLocation(), activeId: null };
    const markers = new Map();
    let youMarker = null;
    let routes = [];

    const pos = (it) => [Number(it.pickupLatitude), Number(it.pickupLongitude)];
    const visible = () => {
        const q = state.q.toLowerCase();
        return items
            .filter((it) => !state.category || it.categorySlug === state.category)
            .filter((it) => !q || [it.name, it.pickupLocation, it.categoryName].some((v) => v?.toLowerCase().includes(q)))
            .map((it) => ({ ...it, km: state.me ? distanceKm(state.me, pos(it)) : null }))
            .sort((a, b) => (a.km ?? 0) - (b.km ?? 0));
    };

    const renderList = (rows) => {
        if (!items.length) {
            list.innerHTML = html`<li class="pickup-empty">${icon('map-pin')}<strong>No pinned pickup points yet</strong>
                <span>When students pin their pickup spot on the map, their listings appear here.</span>
                <a class="btn btn--primary btn--sm" href="/explore">Explore all items</a></li>`;
            return;
        }
        if (!rows.length) {
            list.innerHTML = html`<li class="pickup-empty">${icon('search')}<strong>No pickup points match</strong><span>Try another place, item or category.</span></li>`;
            return;
        }
        list.innerHTML = rows.slice(0, 60).map((it) => html`
            <li class="pickup-row${it.id === state.activeId ? ' is-active' : ''}" data-id="${it.id}">
                <button class="pickup-row__main" type="button" aria-label="${`Show ${it.name} on the map`}">
                    <span class="pickup-row__art"><img src="${it.coverUrl || `/img/categories/${it.categoryIconKey}.webp`}" alt="" loading="lazy"></span>
                    <span class="pickup-row__body">
                        <strong>${it.name}</strong>
                        <span class="pickup-row__meta">${icon('map-pin', 'icon--xs')}${it.pickupLocation}</span>
                        <span class="pickup-row__meta">${icon('footprints', 'icon--xs')}${it.km == null ? it.categoryName : `${formatWalk(it.km)} · ${formatDistance(it.km)}`}</span>
                    </span>
                </button>
                <a class="pickup-row__go" href="/items/${it.id}" aria-label="${`Open ${it.name}`}">${icon('chevron-right')}</a>
            </li>`).join('');
    };

    const renderSummary = (rows) => {
        const near = state.me ? rows.filter((r) => r.km <= NEAR_KM) : rows;
        $('[data-sum-count]').textContent = `${near.length} Pickup Point${near.length === 1 ? '' : 's'}`;
        $('[data-sum-count-label]').textContent = state.me ? `Within ${NEAR_KM} km of you` : 'On the map';
        $('[data-sum-closest]').textContent = state.me && rows.length ? `From ${formatDistance(rows[0].km)}` : 'Share location';
    };

    const renderMap = (rows) => {
        if (!map) return;
        const ids = new Set(rows.map((r) => r.id));
        markers.forEach((m, id) => { if (!ids.has(id)) { m.remove(); markers.delete(id); } });
        rows.forEach((it) => {
            const label = it.km == null ? it.pickupLocation : formatDistance(it.km);
            const icn = pinIcon(L, { iconKey: it.categoryIconKey, label, active: it.id === state.activeId });
            if (markers.has(it.id)) {
                markers.get(it.id).setIcon(icn);
            } else {
                const m = L.marker(pos(it), { icon: icn, title: it.name, riseOnHover: true })
                    .addTo(map)
                    .bindPopup(html`<div class="map-popup"><strong>${it.name}</strong><span>${it.categoryName} · ${it.pickupLocation}</span>
                        <a class="btn btn--primary btn--sm" href="/items/${it.id}">View item</a></div>`.toString())
                    .on('click', () => select(it.id, false));
                markers.set(it.id, m);
            }
        });
        routes.forEach((r) => r.remove());
        routes = [];
        if (state.me) {
            if (!youMarker) youMarker = L.marker(state.me, { icon: youIcon(L), interactive: false, keyboard: false, zIndexOffset: 1000 }).addTo(map);
            youMarker.setLatLng(state.me);
            // Routes to the three closest points (or the selected one), like the design.
            const targets = state.activeId ? rows.filter((r) => r.id === state.activeId) : rows.slice(0, 3);
            targets.forEach((t) => routes.push(routeLine(L, state.me, pos(t), { active: t.id === state.activeId }).addTo(map)));
        }
    };

    const fit = (rows) => {
        if (!map) return;
        const pts = rows.map(pos);
        if (state.me) pts.push(state.me);
        if (pts.length === 1) map.setView(pts[0], 16);
        else if (pts.length > 1) map.fitBounds(L.latLngBounds(state.me ? [state.me, ...rows.slice(0, 3).map(pos)] : pts), { padding: [70, 70], maxZoom: 16 });
    };

    const render = ({ refit = false } = {}) => {
        const rows = visible();
        renderList(rows);
        renderSummary(rows);
        renderMap(rows);
        if (refit) fit(rows);
    };

    const select = (id, pan = true) => {
        state.activeId = state.activeId === id && pan ? null : id;
        render();
        const it = items.find((i) => i.id === id);
        if (map && it && state.activeId) {
            if (pan) map.flyTo(pos(it), Math.max(map.getZoom(), 16), { duration: 0.6 });
            markers.get(id)?.openPopup();
        }
        list.querySelector('.pickup-row.is-active')?.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    };

    list.addEventListener('click', (e) => {
        const btn = e.target.closest('.pickup-row__main');
        if (btn) select(Number(btn.closest('.pickup-row').dataset.id));
    });
    search.addEventListener('input', debounce(() => { state.q = search.value.trim(); render({ refit: true }); }, 250));
    filterToggle.addEventListener('click', () => {
        filters.hidden = !filters.hidden;
        filterToggle.setAttribute('aria-expanded', String(!filters.hidden));
        filterToggle.classList.toggle('is-active', !filters.hidden);
    });
    filters.addEventListener('click', (e) => {
        const chip = e.target.closest('[data-category]');
        if (!chip) return;
        $$('[data-category]', filters).forEach((c) => c.classList.toggle('is-active', c === chip));
        state.category = chip.dataset.category;
        state.activeId = null;
        render({ refit: true });
    });
    locateBtn.addEventListener('click', async () => {
        locateBtn.disabled = true;
        try {
            state.me = await locateMe();
            render({ refit: true });
        } catch (err) {
            toastError(err);
        } finally {
            locateBtn.disabled = false;
        }
    });

    render({ refit: true });
}
