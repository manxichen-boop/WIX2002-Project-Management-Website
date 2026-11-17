// Shared item-card rendering and save/unsave behaviour (Explore, saved items, dashboard).
import { api } from './api.js';
import { html, icon, photo, raw, toast, toastError, typeBadge } from './ui.js';

/** Renders one listing card. `opts.showSave` adds the heart button (students only). */
export function itemCard(item, { showSave = false, extra = '' } = {}) {
    return html`<article class="item-card">
        <div class="item-card__media">
            ${photo(item.coverUrl, item.name)}
            <span class="item-card__type">${typeBadge(item.listingType)}</span>
            ${showSave ? raw(html`<button class="item-card__save" type="button" data-save="${item.id}" aria-pressed="${item.saved ? 'true' : 'false'}"
                aria-label="${item.saved ? 'Remove from saved' : 'Save item'}">${icon('heart')}</button>`) : ''}
        </div>
        <div class="item-card__body">
            <h3 class="item-card__title"><a href="/items/${item.id}">${item.name}</a></h3>
            <p class="item-card__meta">${icon('tag')}<span>${item.categoryName} · ${item.conditionLabel}</span></p>
            <p class="item-card__meta">${icon('map-pin')}<span>${item.pickupLocation}</span></p>
            ${raw(extra)}
        </div>
    </article>`;
}

/** Toggles a favourite through the API and updates the button state. */
export async function toggleSave(button) {
    const id = button.dataset.save;
    const saved = button.getAttribute('aria-pressed') === 'true';
    button.disabled = true;
    try {
        if (saved) await api.delete(`/api/favorites/${id}`);
        else await api.post(`/api/favorites/${id}`);
        button.setAttribute('aria-pressed', String(!saved));
        button.setAttribute('aria-label', saved ? 'Save item' : 'Remove from saved');
        toast(saved ? 'Removed from saved items.' : 'Saved. Find it later under Saved Items.');
        return !saved;
    } catch (err) {
        if (err.status !== 401) toastError(err);
        return saved;
    } finally {
        button.disabled = false;
    }
}

/** Delegated click handler for every [data-save] button inside a container. */
export function bindSaveButtons(container, onChange) {
    container.addEventListener('click', async (e) => {
        const button = e.target.closest('[data-save]');
        if (!button) return;
        e.preventDefault();
        const nowSaved = await toggleSave(button);
        onChange?.(button.dataset.save, nowSaved);
    });
}
