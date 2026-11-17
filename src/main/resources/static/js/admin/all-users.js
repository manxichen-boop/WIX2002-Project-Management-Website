// All users: search, filter by status, suspend or reactivate accounts.
import { api } from '../core/api.js';
import { $, createListController, debounce, emptyState, formatDate, html, icon, raw, statusBadge } from '../core/ui.js';
import { moderate } from './moderation.js';

function actions(u) {
    const name = u.fullName;
    if (u.accountStatus === 'ACTIVE') return html`<button class="btn btn--danger btn--sm" type="button" data-suspend="${u.id}" data-name="${name}">${icon('ban')}Suspend</button>`;
    if (u.accountStatus === 'SUSPENDED' || u.accountStatus === 'REJECTED') return html`<button class="btn btn--secondary btn--sm" type="button" data-activate="${u.id}" data-name="${name}">${icon('rotate-ccw')}Reactivate</button>`;
    return html`<a class="btn btn--secondary btn--sm" href="/admin/pending-users">Review</a>`;
}

export function init() {
    const q = $('#q');
    const status = $('#status');
    const rows = $('#rows');
    status.value = new URLSearchParams(window.location.search).get('status') ?? '';
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('users', 'No students found', 'Try a different search or status filter.'),
        load: (page) => api.get('/api/admin/users', { q: q.value.trim(), status: status.value, page, size: 20 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>Student</th><th>University</th><th>Status</th><th>Joined</th><th><span class="sr-only">Actions</span></th></tr></thead><tbody>
            ${data.content.map((u) => raw(html`<tr>
                <td data-label="Student"><span class="table__primary">${u.fullName}</span><span class="table__sub">${u.email}</span></td>
                <td data-label="University">${u.university}</td>
                <td data-label="Status">${statusBadge(u.accountStatus)}${u.statusReason ? raw(html`<span class="table__sub">${u.statusReason}</span>`) : ''}</td>
                <td data-label="Joined">${formatDate(u.createdAt)}</td>
                <td><div class="table__actions">${raw(actions(u))}</div></td></tr>`))}
            </tbody></table>`,
    });
    q.addEventListener('input', debounce(() => list.reset(), 350));
    status.addEventListener('change', () => list.reset());
    rows.addEventListener('click', async (e) => {
        const suspend = e.target.closest('[data-suspend]');
        const activate = e.target.closest('[data-activate]');
        let done = null;
        if (suspend) {
            done = await moderate(`/api/admin/users/${suspend.dataset.suspend}/suspend`, {
                title: `Suspend ${suspend.dataset.name}?`, text: 'They are signed out immediately and cannot log in until reactivated.',
                confirmLabel: 'Suspend account', tone: 'danger', reason: {}, success: 'Account suspended.' });
        } else if (activate) {
            done = await moderate(`/api/admin/users/${activate.dataset.activate}/activate`, {
                title: `Reactivate ${activate.dataset.name}?`, text: 'They will be able to log in and use Eco Swape again.',
                confirmLabel: 'Reactivate', success: 'Account reactivated.' });
        }
        if (done) list.reload();
    });
    list.reload();
}
