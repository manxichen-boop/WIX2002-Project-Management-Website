// Admin overview: KPI groups, three weekly bar charts (single series each, hover tooltips,
// table view for screen readers / exact values) and the latest audit entries.
import { api } from '../core/api.js';
import { $, errorState, formatDate, formatDateTime, formatNumber, html, humanize, icon, raw } from '../core/ui.js';

const GROUPS = [
    ['People', [['totalUsers', 'Total users', 'users', '/admin/users', 'forest'], ['pendingRegistrations', 'Pending registrations', 'hourglass', '/admin/pending-users', 'warning'],
        ['activeUsers', 'Active users', 'circle-check', '/admin/users?status=ACTIVE', ''], ['suspendedUsers', 'Suspended users', 'ban', '/admin/users?status=SUSPENDED', 'terracotta']]],
    ['Listings', [['totalListings', 'Total listings', 'package', '/admin/listings', 'forest'], ['pendingListings', 'Pending listings', 'hourglass', '/admin/pending-listings', 'warning'],
        ['approvedListings', 'Approved listings', 'circle-check', '/admin/listings', ''], ['rejectedListings', 'Rejected listings', 'circle-x', '/admin/listings', 'terracotta'],
        ['availableItems', 'Available items', 'sprout', '/admin/listings', ''], ['reservedItems', 'Reserved items', 'lock', '/admin/listings', 'forest']]],
    ['Activity', [['completedExchanges', 'Completed exchanges', 'handshake', '/admin/exchanges', ''], ['openReports', 'Open reports', 'flag', '/admin/reports', 'terracotta'],
        ['openTickets', 'Open support tickets', 'life-buoy', '/admin/support', 'warning']]],
];

/** Single-series weekly bar chart as inline SVG. */
function barChart(points, label) {
    const W = 320, H = 170, padL = 26, padB = 26, padT = 18, gap = 6;
    const max = Math.max(1, ...points.map((p) => p.count));
    const ticks = max <= 4 ? Array.from({ length: max + 1 }, (_, i) => i) : [0, Math.round(max / 2), max];
    const bw = (W - padL) / points.length - gap;
    const y = (v) => H - padB - (v / max) * (H - padB - padT);
    const grid = ticks.map((t) => `<line class="chart__grid" x1="${padL}" x2="${W}" y1="${y(t)}" y2="${y(t)}"/><text x="${padL - 6}" y="${y(t) + 4}" text-anchor="end">${t}</text>`).join('');
    const peak = points.reduce((a, p, i) => (p.count > points[a].count ? i : a), 0);
    const bars = points.map((p, i) => {
        const x = padL + gap / 2 + i * (bw + gap);
        const h = Math.max(0, H - padB - y(p.count));
        const r = Math.min(4, h / 2);
        const top = y(p.count);
        // rounded top corners only, anchored to the baseline
        const d = h <= 0 ? '' : `M${x},${H - padB} V${top + r} Q${x},${top} ${x + r},${top} H${x + bw - r} Q${x + bw},${top} ${x + bw},${top + r} V${H - padB} Z`;
        const tip = `Week of ${formatDate(p.weekStart)}: ${p.count} ${label}`;
        return `<g><rect x="${x - gap / 2}" y="${padT}" width="${bw + gap}" height="${H - padB - padT}" fill="transparent" data-tip="${tip}"/>`
            + (d ? `<path class="chart__bar" d="${d}" data-tip="${tip}"/>` : '')
            + (i === peak && p.count > 0 ? `<text class="chart__value" x="${x + bw / 2}" y="${top - 5}" text-anchor="middle">${p.count}</text>` : '')
            + (i % 2 === points.length % 2 ? '' : `<text x="${x + bw / 2}" y="${H - 8}" text-anchor="middle">${new Date(p.weekStart).toLocaleDateString(undefined, { day: 'numeric', month: 'short' })}</text>`)
            + '</g>';
    }).join('');
    return `<svg class="chart" viewBox="0 0 ${W} ${H}" role="img" aria-label="${label} per week, last ${points.length} weeks">${grid}${bars}</svg>`;
}

function chartCard(title, sub, points, label) {
    const total = points.reduce((s, p) => s + p.count, 0);
    return html`<section class="card chart-card" aria-label="${title}">
        <div class="spread"><div><h3>${title}</h3><p>${sub}</p></div><strong class="stat-card__value" style="font-size:1.3rem">${formatNumber(total)}</strong></div>
        <div class="chart-wrap" style="position:relative">${raw(barChart(points, label))}<div class="chart-tip" hidden></div></div>
        <details style="margin-top:8px"><summary class="text-xs text-secondary" style="cursor:pointer">View as table</summary>
            <table class="table" style="margin-top:6px"><thead><tr><th>Week starting</th><th>${label}</th></tr></thead>
            <tbody>${points.map((p) => raw(html`<tr><td>${formatDate(p.weekStart)}</td><td>${p.count}</td></tr>`))}</tbody></table></details>
    </section>`;
}

/** Hover / focus tooltip for any [data-tip] mark inside the charts. */
function bindTooltips(root) {
    root.addEventListener('pointermove', (e) => {
        const mark = e.target.closest('[data-tip]');
        const wrap = e.target.closest('.chart-wrap');
        root.querySelectorAll('.chart-tip').forEach((t) => { if (!wrap || !wrap.contains(t)) t.hidden = true; });
        if (!mark || !wrap) return;
        const tip = wrap.querySelector('.chart-tip');
        const box = wrap.getBoundingClientRect();
        tip.textContent = mark.dataset.tip;
        tip.hidden = false;
        tip.style.cssText = `position:absolute;left:${Math.min(e.clientX - box.left + 12, box.width - 170)}px;top:${e.clientY - box.top - 34}px;`
            + 'background:var(--color-forest);color:#fff;font-size:12px;padding:6px 10px;border-radius:8px;pointer-events:none;white-space:nowrap;box-shadow:var(--shadow-md)';
    });
    root.addEventListener('pointerleave', () => root.querySelectorAll('.chart-tip').forEach((t) => { t.hidden = true; }));
}

export async function init() {
    const kpis = $('#kpis');
    const charts = $('#charts');
    const recent = $('#recent');
    kpis.innerHTML = '<div class="admin-kpis">' + Array.from({ length: 8 }, () => '<div class="skeleton" style="height:78px;border-radius:16px"></div>').join('') + '</div>';
    try {
        const d = await api.get('/api/admin/dashboard');
        kpis.innerHTML = GROUPS.map(([title, items]) => html`<div class="kpi-group"><p class="kpi-group__title">${title}</p><div class="admin-kpis">
            ${items.map(([key, label, ic, href, tone]) => raw(html`<a class="stat-card" href="${href}"><span class="stat-card__icon ${tone ? `stat-card__icon--${tone}` : ''}">${icon(ic)}</span>
            <div><div class="stat-card__value">${formatNumber(d.kpis[key])}</div><div class="stat-card__label">${label}</div></div></a>`))}
        </div></div>`).join('');
        kpis.removeAttribute('aria-busy');
        charts.innerHTML = [
            chartCard('New registrations', 'Students who signed up, per week', d.registrations, 'registrations'),
            chartCard('New listings', 'Listings submitted, per week', d.newListings, 'listings'),
            chartCard('Completed exchanges', 'Both sides confirmed, per week', d.completedExchanges, 'exchanges'),
        ].join('');
        bindTooltips(charts);
        recent.innerHTML = d.recentActions.length
            ? html`<ul class="list">${d.recentActions.map((a) => raw(html`<li class="list__item"><div class="list__main">
                <span class="list__title">${humanize(a.action)} · ${humanize(a.targetType)} #${a.targetId}</span>
                <span class="list__sub">${a.adminName}${a.reason ? ` · ${a.reason}` : ''}</span></div>
                <span class="list__sub nowrap">${formatDateTime(a.createdAt)}</span></li>`))}</ul>`
            : '<p class="text-secondary text-sm">No admin actions yet.</p>';
    } catch (err) {
        kpis.innerHTML = errorState(err.message);
        kpis.querySelector('[data-retry]')?.addEventListener('click', init);
    }
}
