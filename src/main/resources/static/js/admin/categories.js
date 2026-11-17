// Category management: create, edit, activate / deactivate, delete unused categories.
import { api } from '../core/api.js';
import { $, confirmDialog, createListController, emptyState, html, icon, openDialog, raw, statusBadge, toast, toastError } from '../core/ui.js';

const ICONS = ['books', 'electronics', 'clothing', 'accessories', 'furniture', 'stationery', 'sports', 'household', 'reusable', 'more'];

function form(c = {}) {
    return html`<div class="form">
        <div class="field"><label for="c-name">Name</label><input class="input" id="c-name" name="name" required minlength="2" maxlength="80" value="${c.name ?? ''}"></div>
        <div class="field"><label for="c-desc">Description <span class="optional">(optional)</span></label><input class="input" id="c-desc" name="description" maxlength="255" value="${c.description ?? ''}"></div>
        <div class="form-row">
            <div class="field"><label for="c-icon">Icon</label><select class="select" id="c-icon" name="iconKey">
                ${ICONS.map((i) => raw(html`<option value="${i}" ${(c.iconKey ?? 'more') === i ? raw('selected') : ''}>${i}</option>`))}</select></div>
            <div class="field"><label for="c-order">Display order</label><input class="input" id="c-order" name="displayOrder" type="number" min="0" max="9999" value="${c.displayOrder ?? 100}"></div>
        </div></div>`;
}

const payload = (data) => ({ name: data.get('name'), description: data.get('description') || null, iconKey: data.get('iconKey'), displayOrder: Number(data.get('displayOrder')) });

export function init() {
    const rows = $('#rows');
    let current = [];
    const list = createListController({
        container: rows,
        empty: emptyState('folder-tree', 'No categories', 'Create the first category.'),
        load: async () => {
            current = await api.get('/api/admin/categories');
            return { content: current, page: 0, size: current.length, totalElements: current.length, totalPages: 1 };
        },
        render: () => html`<table class="table table--stack"><thead><tr><th>Category</th><th>Slug</th><th>Listings</th><th>Order</th><th>Status</th><th></th></tr></thead><tbody>
            ${current.map((c) => raw(html`<tr>
                <td class="media-cell-td"><div class="media-cell"><img src="/img/categories/${c.iconKey}.webp" alt="" style="object-fit:contain;background:none">
                    <div><span class="table__primary">${c.name}</span><span class="table__sub">${c.description || ''}</span></div></div></td>
                <td data-label="Slug">${c.slug}</td><td data-label="Listings">${c.itemCount}</td><td data-label="Order">${c.displayOrder}</td>
                <td data-label="Status">${statusBadge(c.active ? 'ACTIVE' : 'ARCHIVED', c.active ? 'Active' : 'Inactive')}</td>
                <td><div class="table__actions">
                    <button class="btn btn--secondary btn--sm" type="button" data-edit="${c.id}">${icon('pencil')}Edit</button>
                    <button class="btn btn--ghost btn--sm" type="button" data-toggle="${c.id}" data-active="${c.active}">${c.active ? 'Deactivate' : 'Activate'}</button>
                    ${c.itemCount === 0 ? raw(html`<button class="btn btn--danger btn--sm" type="button" data-delete="${c.id}" aria-label="Delete ${c.name}">${icon('trash-2')}</button>`) : ''}
                </div></td></tr>`))}
            </tbody></table>`,
    });

    $('[data-new]').addEventListener('click', async () => {
        const r = await openDialog({ title: 'New category', body: form(), confirmLabel: 'Create', onSubmit: (d) => api.post('/api/admin/categories', payload(d)) });
        if (r) { toast('Category created.'); list.reload(); }
    });
    rows.addEventListener('click', async (e) => {
        const edit = e.target.closest('[data-edit]');
        const toggle = e.target.closest('[data-toggle]');
        const del = e.target.closest('[data-delete]');
        try {
            if (edit) {
                const c = current.find((x) => String(x.id) === edit.dataset.edit);
                const r = await openDialog({ title: `Edit ${c.name}`, body: form(c), confirmLabel: 'Save', onSubmit: (d) => api.put(`/api/admin/categories/${c.id}`, payload(d)) });
                if (r) { toast('Category saved.'); list.reload(); }
            } else if (toggle) {
                const active = toggle.dataset.active === 'true';
                await api.patch(`/api/admin/categories/${toggle.dataset.toggle}/${active ? 'deactivate' : 'activate'}`);
                toast(active ? 'Category deactivated: it is hidden from new listings and filters.' : 'Category activated.');
                list.reload();
            } else if (del) {
                if (!(await confirmDialog({ title: 'Delete this category?', text: 'It has never been used by a listing, so it can be deleted permanently.', confirmLabel: 'Delete', tone: 'danger' }))) return;
                await api.delete(`/api/admin/categories/${del.dataset.delete}`);
                toast('Category deleted.');
                list.reload();
            }
        } catch (err) { toastError(err); }
    });
    list.reload();
}
