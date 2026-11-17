// Pending registrations queue: approve or reject (with a reason) each new student.
import { api } from '../core/api.js';
import { $, createListController, debounce, emptyState, formatDate, html, icon, raw } from '../core/ui.js';
import { moderate } from './moderation.js';

function table(data) {
    return html`<table class="table table--stack"><thead><tr><th>Student</th><th>University</th><th>Registered</th><th>E-mail verified</th><th><span class="sr-only">Actions</span></th></tr></thead><tbody>
    ${data.content.map((u) => raw(html`<tr>
        <td data-label="Student"><span class="table__primary">${u.fullName}</span><span class="table__sub">${u.email}</span></td>
        <td data-label="University">${u.university}</td>
        <td data-label="Registered">${formatDate(u.createdAt)}</td>
        <td data-label="E-mail verified">${u.emailVerified ? raw('<span class="badge badge--success">Verified</span>') : raw('<span class="badge badge--neutral">Not yet</span>')}</td>
        <td><div class="table__actions">
            <button class="btn btn--primary btn--sm" type="button" data-approve="${u.id}" data-name="${u.fullName}">${icon('check')}Approve</button>
            <button class="btn btn--danger btn--sm" type="button" data-reject="${u.id}" data-name="${u.fullName}">${icon('circle-x')}Reject</button>
        </div></td></tr>`))}
    </tbody></table>`;
}

export function init() {
    const q = $('#q');
    const rows = $('#rows');
    const list = createListController({
        container: rows, pager: $('#pager'),
        empty: emptyState('circle-check', 'No registrations waiting', 'New sign-ups will appear here for review.'),
        load: (page) => api.get('/api/admin/users/pending', { q: q.value.trim(), page, size: 20 }),
        render: table,
    });
    q.addEventListener('input', debounce(() => list.reset(), 350));
    rows.addEventListener('click', async (e) => {
        const approve = e.target.closest('[data-approve]');
        const reject = e.target.closest('[data-reject]');
        let done = null;
        if (approve) {
            done = await moderate(`/api/admin/users/${approve.dataset.approve}/approve`, {
                title: `Approve ${approve.dataset.name}?`, text: 'They will be notified and can start using Eco Swape straight away.',
                confirmLabel: 'Approve', success: 'Registration approved.' });
        } else if (reject) {
            done = await moderate(`/api/admin/users/${reject.dataset.reject}/reject`, {
                title: `Reject ${reject.dataset.name}?`, text: 'The student will see your reason and won\'t be able to log in.',
                confirmLabel: 'Reject registration', tone: 'danger', reason: {}, success: 'Registration rejected.' });
        }
        if (done) list.reload();
    });
    list.reload();
}
