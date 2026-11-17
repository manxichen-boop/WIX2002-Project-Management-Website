// Map helpers shared by the item form, item details and the pickup map.
// Leaflet (vendored under /vendor/leaflet) is loaded only on pages that show a map.
// Tiles and place search come from OpenStreetMap; distances are straight-line ("as the crow flies").

let leafletPromise = null;

/** Loads Leaflet once and resolves with the global L. */
export function loadLeaflet() {
    if (window.L) return Promise.resolve(window.L);
    leafletPromise ??= new Promise((resolve, reject) => {
        const css = document.createElement('link');
        css.rel = 'stylesheet';
        css.href = '/vendor/leaflet/leaflet.css';
        document.head.append(css);
        const script = document.createElement('script');
        script.src = '/vendor/leaflet/leaflet.js';
        script.onload = () => resolve(window.L);
        script.onerror = () => { leafletPromise = null; reject(new Error('The map could not be loaded.')); };
        document.head.append(script);
    });
    return leafletPromise;
}

/** Default view from application.yml (ecoswap.map.center / zoom), e.g. the university campus. */
export function defaultView() {
    const [lat, lng] = (document.body.dataset.mapCenter || '3.1209,101.6538').split(',').map(Number);
    return { center: [lat, lng], zoom: Number(document.body.dataset.mapZoom) || 15 };
}

/** Creates a themed map in `el`. Scroll-wheel zoom is off until the map is clicked (no scroll traps). */
export async function createMap(el, { center, zoom, interactive = true } = {}) {
    const L = await loadLeaflet();
    const view = defaultView();
    const map = L.map(el, {
        center: center ?? view.center, zoom: zoom ?? view.zoom,
        zoomControl: false, scrollWheelZoom: false, attributionControl: true,
        dragging: interactive, touchZoom: interactive, doubleClickZoom: interactive, keyboard: interactive, tap: interactive,
    });
    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
        maxZoom: 19, className: 'eco-tiles',
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright" target="_blank" rel="noopener">OpenStreetMap</a>',
    }).addTo(map);
    map.attributionControl.setPrefix(false);
    if (interactive) {
        L.control.zoom({ position: 'topright' }).addTo(map);
        map.on('click focus', () => map.scrollWheelZoom.enable());
        map.on('mouseout', () => map.scrollWheelZoom.disable());
    }
    // Maps inside layouts that resize (sidebars, tabs, dialogs) need a nudge.
    new ResizeObserver(() => map.invalidateSize()).observe(el);
    return { L, map };
}

const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

/** Green teardrop pin with the category artwork (or a plain leaf) and an optional label bubble. */
export function pinIcon(L, { iconKey, label, active = false } = {}) {
    const art = iconKey ? `<img src="/img/categories/${esc(iconKey)}.webp" alt="">` : '<span class="map-pin__dot"></span>';
    const bubble = label ? `<span class="map-pin__label">${esc(label)}</span>` : '';
    return L.divIcon({
        className: 'map-pin-wrap',
        html: `<span class="map-pin${active ? ' is-active' : ''}"><span class="map-pin__head">${art}</span></span>${bubble}`,
        iconSize: [46, 58], iconAnchor: [23, 56], popupAnchor: [0, -52],
    });
}

/** Pulsing "You" marker. */
export function youIcon(L) {
    return L.divIcon({
        className: 'map-you-wrap',
        html: '<span class="map-you"><span class="map-you__pulse"></span><span class="map-you__dot"></span><span class="map-you__label">You</span></span>',
        iconSize: [24, 24], iconAnchor: [12, 12],
    });
}

/** Gently curved dashed line between two points (looks like a walking route without pretending to be one). */
export function routeLine(L, from, to, { active = false } = {}) {
    const [la1, ln1] = from;
    const [la2, ln2] = to;
    const mid = [(la1 + la2) / 2, (ln1 + ln2) / 2];
    const bend = 0.18;
    const ctrl = [mid[0] - (ln2 - ln1) * bend, mid[1] + (la2 - la1) * bend];
    const pts = [];
    for (let t = 0; t <= 1.0001; t += 0.05) {
        const a = (1 - t) * (1 - t);
        const b = 2 * (1 - t) * t;
        const c = t * t;
        pts.push([a * la1 + b * ctrl[0] + c * la2, a * ln1 + b * ctrl[1] + c * ln2]);
    }
    return L.polyline(pts, { className: `map-route${active ? ' is-active' : ''}`, weight: active ? 4 : 3, dashArray: '2 9', lineCap: 'round', interactive: false });
}

/** Great-circle distance in kilometres. */
export function distanceKm([lat1, lng1], [lat2, lng2]) {
    const R = 6371;
    const rad = (d) => (d * Math.PI) / 180;
    const dLat = rad(lat2 - lat1);
    const dLng = rad(lng2 - lng1);
    const h = Math.sin(dLat / 2) ** 2 + Math.cos(rad(lat1)) * Math.cos(rad(lat2)) * Math.sin(dLng / 2) ** 2;
    return 2 * R * Math.asin(Math.sqrt(h));
}

export const formatDistance = (km) => (km < 1 ? `${Math.max(10, Math.round((km * 1000) / 10) * 10)} m` : `${km < 10 ? km.toFixed(1) : Math.round(km)} km`);

/** Rough walking time: straight line × 1.3 for real paths, at 4.8 km/h. */
export function walkMinutes(km) {
    return Math.max(1, Math.round(((km * 1.3) / 4.8) * 60));
}

export const formatWalk = (km) => {
    const min = walkMinutes(km);
    return min < 90 ? `≈ ${min} min walk` : `≈ ${Math.round(min / 60)} h walk`;
};

const LOC_KEY = 'ecoswap:my-location';

/** Last known position from this browser session, if any (never sent to the server). */
export function cachedLocation() {
    try {
        const saved = JSON.parse(sessionStorage.getItem(LOC_KEY) || 'null');
        if (saved && Date.now() - saved.at < 15 * 60 * 1000) return saved.pos;
    } catch { /* storage unavailable */ }
    return null;
}

/** Asks the browser for the visitor's position. Rejects with a friendly message. */
export function locateMe() {
    return new Promise((resolve, reject) => {
        if (!navigator.geolocation) { reject(new Error('Your browser cannot share its location.')); return; }
        navigator.geolocation.getCurrentPosition(
            (p) => {
                const pos = [p.coords.latitude, p.coords.longitude];
                try { sessionStorage.setItem(LOC_KEY, JSON.stringify({ pos, at: Date.now() })); } catch { /* ignore */ }
                resolve(pos);
            },
            (err) => reject(new Error(err.code === 1
                ? 'Location permission was blocked. Allow it in your browser’s site settings to see distances.'
                : 'Your location could not be found right now. Please try again.')),
            { enableHighAccuracy: true, timeout: 12000, maximumAge: 60000 },
        );
    });
}

/** Place search with OpenStreetMap Nominatim (biased towards the default map area). */
export async function searchPlaces(query) {
    const { center } = defaultView();
    const params = new URLSearchParams({
        format: 'jsonv2', q: query, limit: '6', addressdetails: '0',
        viewbox: `${center[1] - 0.6},${center[0] + 0.6},${center[1] + 0.6},${center[0] - 0.6}`, bounded: '0',
    });
    const res = await fetch(`https://nominatim.openstreetmap.org/search?${params}`, { headers: { Accept: 'application/json' } });
    if (!res.ok) throw new Error('Place search is unavailable right now.');
    const rows = await res.json();
    return rows.map((r) => ({ name: r.name || r.display_name.split(',')[0], label: r.display_name, pos: [Number(r.lat), Number(r.lon)] }));
}

/** Short place name for a point ("Faculty of Engineering, Jalan Universiti"), or null. */
export async function placeName([lat, lng]) {
    try {
        const params = new URLSearchParams({ format: 'jsonv2', lat: String(lat), lon: String(lng), zoom: '18' });
        const res = await fetch(`https://nominatim.openstreetmap.org/reverse?${params}`, { headers: { Accept: 'application/json' } });
        if (!res.ok) return null;
        const r = await res.json();
        const a = r.address || {};
        const parts = [r.name, a.road, a.suburb || a.neighbourhood || a.city_district].filter(Boolean);
        const unique = [...new Set(parts)];
        return unique.slice(0, 2).join(', ') || (r.display_name ? r.display_name.split(',').slice(0, 2).join(',') : null);
    } catch {
        return null;
    }
}

/** Link that opens walking directions in the visitor's map app. */
export function directionsUrl([lat, lng]) {
    return `https://www.google.com/maps/dir/?api=1&destination=${lat},${lng}&travelmode=walking`;
}
