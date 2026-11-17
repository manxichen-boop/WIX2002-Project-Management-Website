// Explore Items: filters, search, sort and pagination, all reflected in the URL so results
// are shareable and the back button works.
import { api } from '../core/api.js';
import { bindSaveButtons, itemCard } from '../core/items.js';
import { $, $$, cardSkeletons, createListController, debounce, emptyState, formatNumber } from '../core/ui.js';

export function init() {
    const form = $('#filters');
    const q = $('#q');
    const sort = $('#sort');
    const results = $('#results');
    const count = $('#result-count');
    const loggedInStudent = Boolean(document.querySelector('[data-requests-badge]'));
    let type = '';

    // Initial state from the URL (?q=&category=&type=&condition=&location=&sort=)
    const params = new URLSearchParams(window.location.search);
    q.value = params.get('q') ?? '';
    form.category.value = params.get('category') ?? '';
    form.condition.value = params.get('condition') ?? '';
    form.location.value = params.get('location') ?? '';
    sort.value = params.get('sort') ?? 'newest';
    type = params.get('type') ?? '';
    const setType = (value) => {
        type = value;
        $$('[data-type]', form).forEach((chip) => chip.setAttribute('aria-pressed', String(chip.dataset.type === value)));
    };
    setType(type);

    const query = () => ({
        q: q.value.trim(), category: form.category.value, condition: form.condition.value,
        type, location: form.location.value.trim(), sort: sort.value,
    });

    const list = createListController({
        container: results,
        pager: $('#pager'),
        skeleton: cardSkeletons(6),
        empty: emptyState('search', 'No items match your search',
            'Try a different keyword or fewer filters. New items are approved every day.',
            '<button class="btn btn--secondary" type="button" data-clear>Clear all filters</button>'),
        load: async (page) => {
            const data = await api.get('/api/items', { ...query(), page, size: 12 });
            count.textContent = data.totalElements === 1 ? '1 item available' : `${formatNumber(data.totalElements)} items available`;
            return data;
        },
        render: (data) => data.content.map((item) => itemCard(item, { showSave: loggedInStudent })).join(''),
    });

    const apply = () => {
        const state = Object.fromEntries(Object.entries(query()).filter(([k, v]) => v && !(k === 'sort' && v === 'newest')));
        history.replaceState(null, '', `/explore${Object.keys(state).length ? `?${new URLSearchParams(state)}` : ''}`);
        form.classList.remove('is-open');
        list.reset();
    };
    const reset = () => {
        form.reset();
        q.value = '';
        sort.value = 'newest';
        setType('');
        apply();
    };

    form.addEventListener('submit', (e) => { e.preventDefault(); apply(); });
    form.category.addEventListener('change', apply);
    form.condition.addEventListener('change', apply);
    $$('[data-type]', form).forEach((chip) => chip.addEventListener('click', () => { setType(chip.dataset.type); apply(); }));
    $('[data-reset]', form).addEventListener('click', reset);
    q.addEventListener('input', debounce(apply, 350));
    sort.addEventListener('change', apply);
    results.addEventListener('click', (e) => { if (e.target.closest('[data-clear]')) reset(); });
    $('[data-open-filters]').addEventListener('click', () => form.classList.add('is-open'));
    $('[data-close-filters]').addEventListener('click', () => form.classList.remove('is-open'));
    bindSaveButtons(results);

    list.reload();
}
