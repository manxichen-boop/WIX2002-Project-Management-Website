// Map picker on the Post / Edit item form: search a place, tap the map or use the device
// location to drop a draggable pin. Writes the pin into the hidden pickupLatitude/Longitude fields.
import { createMap, locateMe, pinIcon, placeName, searchPlaces } from './map.js';
import { html, toastError } from './ui.js';

export async function initPickupPicker(form) {
    const root = form.querySelector('[data-pickup-picker]');
    if (!root) return;
    const latInput = form.elements.namedItem('pickupLatitude');
    const lngInput = form.elements.namedItem('pickupLongitude');
    const textInput = form.elements.namedItem('pickupLocation');
    const status = root.querySelector('[data-pin-text]');
    const clearBtn = root.querySelector('[data-clear-pin]');
    const search = root.querySelector('#map-search');
    const results = root.querySelector('#map-search-results');
    const locateBtn = root.querySelector('[data-locate]');
    const categorySelect = form.elements.namedItem('categoryId');

    const start = latInput.value && lngInput.value ? [Number(latInput.value), Number(lngInput.value)] : null;
    let L;
    let map;
    try {
        ({ L, map } = await createMap(root.querySelector('#pickup-map'), start ? { center: start, zoom: 17 } : {}));
    } catch (err) {
        root.querySelector('#pickup-map').innerHTML = html`<p class="map-fallback">${err.message} You can still post the item with the written pickup location.</p>`;
        return;
    }

    const iconKey = () => categorySelect?.selectedOptions[0]?.dataset.icon || null;
    let marker = null;

    const describe = (pos) => `Pin placed at ${pos[0].toFixed(5)}, ${pos[1].toFixed(5)}. Drag it to adjust.`;
    const setPin = async (pos, { fly = true, fillText = true } = {}) => {
        latInput.value = pos[0].toFixed(6);
        lngInput.value = pos[1].toFixed(6);
        if (!marker) {
            marker = L.marker(pos, { draggable: true, icon: pinIcon(L, { iconKey: iconKey(), active: true }), keyboard: true, title: 'Pickup point' }).addTo(map);
            marker.on('dragend', () => setPin([marker.getLatLng().lat, marker.getLatLng().lng], { fly: false }));
        } else {
            marker.setLatLng(pos);
        }
        if (fly) map.flyTo(pos, Math.max(map.getZoom(), 17), { duration: 0.6 });
        status.textContent = describe(pos);
        clearBtn.hidden = false;
        root.classList.add('has-pin');
        // Suggest a written location from the map if the student hasn't typed one yet.
        if (fillText && textInput && !textInput.value.trim()) {
            const name = await placeName(pos);
            if (name && !textInput.value.trim()) textInput.value = name.slice(0, 150);
        }
    };
    const clearPin = () => {
        latInput.value = '';
        lngInput.value = '';
        marker?.remove();
        marker = null;
        status.textContent = 'No pin yet. Tap the map to place one.';
        clearBtn.hidden = true;
        root.classList.remove('has-pin');
    };

    if (start) await setPin(start, { fly: false, fillText: false });
    map.on('click', (e) => setPin([e.latlng.lat, e.latlng.lng], { fly: false }));
    clearBtn.addEventListener('click', clearPin);
    categorySelect?.addEventListener('change', () => marker?.setIcon(pinIcon(L, { iconKey: iconKey(), active: true })));

    locateBtn.addEventListener('click', async () => {
        locateBtn.disabled = true;
        try {
            await setPin(await locateMe());
        } catch (err) {
            toastError(err);
        } finally {
            locateBtn.disabled = false;
        }
    });

    // ---- place search (on Enter or after a short pause; Nominatim allows ~1 request per second)
    let timer;
    let lastQuery = '';
    const closeResults = () => { results.hidden = true; search.setAttribute('aria-expanded', 'false'); };
    const runSearch = async () => {
        const q = search.value.trim();
        if (q.length < 3 || q === lastQuery) return;
        lastQuery = q;
        results.innerHTML = '<li class="map-search__note">Searching…</li>';
        results.hidden = false;
        try {
            const places = await searchPlaces(q);
            results.innerHTML = places.length
                ? places.map((p, i) => html`<li role="option" tabindex="-1" data-i="${i}"><strong>${p.name}</strong><span>${p.label}</span></li>`).join('')
                : '<li class="map-search__note">No places found. Try another name, or tap the map.</li>';
            results._places = places;
            search.setAttribute('aria-expanded', 'true');
        } catch (err) {
            results.innerHTML = html`<li class="map-search__note">${err.message}</li>`;
        }
    };
    search.addEventListener('input', () => { clearTimeout(timer); timer = setTimeout(runSearch, 900); });
    search.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') { e.preventDefault(); clearTimeout(timer); lastQuery = ''; runSearch(); }
        if (e.key === 'Escape') closeResults();
        if (e.key === 'ArrowDown') results.querySelector('[role="option"]')?.focus();
    });
    const choose = (li) => {
        const place = results._places?.[Number(li.dataset.i)];
        if (!place) return;
        closeResults();
        search.value = place.name;
        if (textInput && !textInput.value.trim()) textInput.value = place.name.slice(0, 150);
        setPin(place.pos, { fillText: false });
    };
    results.addEventListener('click', (e) => { const li = e.target.closest('[role="option"]'); if (li) choose(li); });
    results.addEventListener('keydown', (e) => {
        const li = e.target.closest('[role="option"]');
        if (!li) return;
        if (e.key === 'Enter') { e.preventDefault(); choose(li); }
        if (e.key === 'ArrowDown') li.nextElementSibling?.focus();
        if (e.key === 'ArrowUp') (li.previousElementSibling || search).focus();
        if (e.key === 'Escape') { closeResults(); search.focus(); }
    });
    document.addEventListener('click', (e) => { if (!root.querySelector('.map-search').contains(e.target)) closeResults(); });
}
