// Homepage: "More" rotates the category chips around the green circle, counter-clockwise like a
// carousel: the main categories travel up and over the top and leave at the bottom left, while the
// other categories come in from behind the More chip. "Back" turns the circle the other way.
// Without JavaScript the More chip is a plain link to Explore.

const DURATION = 1500;                     // ms for one full turn
const BELT = ['furniture', 'clothing', 'books', 'electronics', 'accessories'];   // slots S1..S5 along the circle
const ALT_SLOT = { 1: 'books', 2: 'clothing', 3: 'electronics', 4: 'furniture', 5: 'accessories' }; // .cat-chip--slotN → slot
const SHIFT = BELT.length;                 // how many slots the circle turns

const easeInOut = (t) => (t < 0.5 ? 4 * t * t * t : 1 - (-2 * t + 2) ** 3 / 2);
const clamp01 = (v) => Math.min(1, Math.max(0, v));

export function init() {
    const art = document.querySelector('.hero__art');
    const toggle = art?.querySelector('[data-chip-more]');
    const alt = art ? [...art.querySelectorAll('[data-chip-alt]')] : [];
    if (!toggle || alt.length === 0) return;
    const main = [...art.querySelectorAll('.cat-chip--main')];
    const phone = window.matchMedia('(max-width: 640px)');
    const reduced = window.matchMedia('(prefers-reduced-motion: reduce)');
    let busy = false;

    const setState = (open) => {
        art.classList.toggle('is-more', open);
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Back to the main categories' : 'Show more categories');
        alt.forEach((a) => { a.tabIndex = open ? 0 : -1; a.setAttribute('aria-hidden', String(!open)); });
        main.forEach((a) => { a.tabIndex = open ? -1 : 0; a.setAttribute('aria-hidden', String(open)); });
    };

    toggle.addEventListener('click', (e) => {
        e.preventDefault();
        if (busy) return;
        const open = !art.classList.contains('is-more');
        art.classList.add('chips-ready');   // switch off the entrance animation for good
        if (phone.matches || reduced.matches) { setState(open); return; }
        busy = true;
        orbit(open).finally(() => { busy = false; });
    });

    /** Turns the circle: open = counter-clockwise (main out, extra in), otherwise clockwise. */
    function orbit(open) {
        const box = art.getBoundingClientRect();
        const W = box.width;
        const H = box.height;
        const center = (el) => {
            const r = el.getBoundingClientRect();
            return { x: r.left - box.left + r.width / 2, y: r.top - box.top + r.height / 2, w: r.width, h: r.height };
        };
        // Slot centres, measured from the real layout (main chips sit on their slots, extra chips are laid out but hidden).
        const slot = {};
        main.forEach((c) => { const key = BELT.find((k) => c.classList.contains(`cat-chip--${k}`)); if (key) slot[key] = center(c); });
        alt.forEach((c) => {
            const n = Number([...c.classList].find((k) => k.startsWith('cat-chip--slot'))?.slice(14));
            if (ALT_SLOT[n] && !slot[ALT_SLOT[n]]) slot[ALT_SLOT[n]] = center(c);
        });
        const start = center(toggle);

        // The circle the chips travel on (fitted to the arch in the hero picture).
        const cx = 0.5 * W;
        const cy = 0.62 * H;
        const rx = 0.44 * W;
        const ry = 0.58 * H;
        const angleOf = (p) => Math.atan2((p.y - cy) / ry, (p.x - cx) / rx);
        const onCircle = (a) => ({ x: cx + rx * Math.cos(a), y: cy + ry * Math.sin(a) });

        // Anchors along the path: 0 = entry (behind More), 1..5 = slots, 6 = exit (bottom left).
        const anchors = [];
        const slotAngles = BELT.map((k) => angleOf(slot[k]));
        // New chips come out from behind the More/Back chip; old ones leave past the bottom-left slot.
        const entry = angleOf(start);
        const exit = slotAngles[4] + 0.8 * (slotAngles[4] - slotAngles[3]);
        const entryOn = onCircle(entry);
        anchors.push({ a: entry, dx: start.x - entryOn.x, dy: start.y - entryOn.y });
        BELT.forEach((k, i) => { const p = onCircle(slotAngles[i]); anchors.push({ a: slotAngles[i], dx: slot[k].x - p.x, dy: slot[k].y - p.y }); });
        anchors.push({ a: exit, dx: 0, dy: 0 });
        const last = anchors.length - 1;

        // Position on the path for a continuous index u (extrapolates beyond both ends).
        const at = (u) => {
            let a; let dx = 0; let dy = 0;
            if (u <= 0) {
                a = anchors[0].a + u * (anchors[1].a - anchors[0].a);
            } else if (u >= last) {
                a = anchors[last].a + (u - last) * (anchors[last].a - anchors[last - 1].a);
            } else {
                const i = Math.floor(u);
                const t = u - i;
                const p = anchors[i];
                const q = anchors[i + 1];
                a = p.a + (q.a - p.a) * t;
                dx = p.dx + (q.dx - p.dx) * t;
                dy = p.dy + (q.dy - p.dy) * t;
            }
            const c = onCircle(a);
            return { x: c.x + dx, y: c.y + dy };
        };
        const visibility = (u) => clamp01((u + 0.1) / 0.75) * clamp01((last + 0.3 - u) / 0.8);

        // Each chip's start and end index on the path.
        const movers = [];
        main.forEach((c) => {
            const i = BELT.findIndex((k) => c.classList.contains(`cat-chip--${k}`)) + 1;
            if (i > 0) movers.push({ el: c, from: open ? i : i + SHIFT, to: open ? i + SHIFT : i });
        });
        alt.forEach((c) => {
            const n = Number([...c.classList].find((k) => k.startsWith('cat-chip--slot'))?.slice(14));
            const i = BELT.indexOf(ALT_SLOT[n]) + 1;
            if (i > 0) movers.push({ el: c, from: open ? i - SHIFT : i, to: open ? i : i - SHIFT });
        });
        const size = { w: start.w, h: start.h };

        art.classList.add('is-orbiting');
        // The More/Back faces swap while the circle turns.
        setState(open);

        return new Promise((resolve) => {
            const t0 = performance.now();
            const frame = (now) => {
                const t = clamp01((now - t0) / DURATION);
                const k = easeInOut(t);
                movers.forEach((m) => {
                    const u = m.from + (m.to - m.from) * k;
                    const p = at(u);
                    const v = visibility(u);
                    m.el.style.left = `${((p.x - size.w / 2) / W) * 100}%`;
                    m.el.style.top = `${((p.y - size.h / 2) / H) * 100}%`;
                    m.el.style.opacity = String(v);
                    m.el.style.transform = `scale(${0.78 + 0.22 * v})`;
                });
                if (t < 1) {
                    requestAnimationFrame(frame);
                } else {
                    movers.forEach((m) => { m.el.style.left = ''; m.el.style.top = ''; m.el.style.opacity = ''; m.el.style.transform = ''; });
                    art.classList.remove('is-orbiting');
                    resolve();
                }
            };
            requestAnimationFrame(frame);
        });
    }
}
