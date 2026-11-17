// Audit logs: read-only, filterable record of every admin action.
import { api } from '../core/api.js';
import { $, createListController, emptyState, formatDateTime, html, humanize, raw } from '../core/ui.js';

const ACTIONS = ['REGISTRATION_APPROVED', 'REGISTRATION_REJECTED', 'ACCOUNT_SUSPENDED', 'ACCOUNT_REACTIVATED', 'LISTING_APPROVED', 'LISTING_REJECTED',
    'LISTING_CHANGES_REQUESTED', 'LISTING_SUSPENDED', 'LISTING_ARCHIVED', 'REPORT_REVIEW_STARTED', 'REPORT_RESOLVED', 'REPORT_DISMISSED',
    'SUPPORT_REPLIED', 'SUPPORT_STATUS_CHANGED', 'CATEGORY_CREATED', 'CATEGORY_UPDATED', 'CATEGORY_ACTIVATED', 'CATEGORY_DEACTIVATED'];

export function init() {
    const action = $('#action');
    const target = $('#target');
    action.insertAdjacentHTML('beforeend', ACTIONS.map((a) => `<option value="${a}">${humanize(a)}</option>`).join(''));
    const list = createListController({
        container: $('#rows'), pager: $('#pager'),
        empty: emptyState('scroll-text', 'No matching entries', 'Admin actions are recorded here automatically.'),
        load: (page) => api.get('/api/admin/audit-logs', { action: action.value, targetType: target.value, page, size: 25 }),
        render: (data) => html`<table class="table table--stack"><thead><tr><th>When</th><th>Admin</th><th>Action</th><th>Target</th><th>Reason</th></tr></thead><tbody>
            ${data.content.map((l) => raw(html`<tr><td data-label="When" class="nowrap">${formatDateTime(l.createdAt)}</td><td data-label="Admin">${l.adminName}<span class="table__sub">ID ${l.adminId}</span></td>
                <td data-label="Action"><span class="table__primary">${humanize(l.action)}</span></td><td data-label="Target">${humanize(l.targetType)} #${l.targetId}</td>
                <td data-label="Reason">${l.reason || '—'}</td></tr>`))}</tbody></table>`,
    });
    action.addEventListener('change', () => list.reset());
    target.addEventListener('change', () => list.reset());
    list.reload();
}
