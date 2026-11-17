// Post / edit a listing: validation, photo previews (drag & drop), multipart submission.
import { api } from '../core/api.js';
import { initPickupPicker } from '../core/pickup-picker.js';
import { $, html, icon, setBusy } from '../core/ui.js';
import { rules, setFieldError, showApiError, validate } from '../core/validation.js';

const MAX_PHOTOS = 6;
const MAX_BYTES = 5 * 1024 * 1024;
const TYPES = ['image/jpeg', 'image/png', 'image/webp'];

export function init() {
    const form = $('#item-form');
    const itemId = $('#main').dataset.itemId;
    const grid = $('#photo-grid');
    const input = $('#photos');
    const drop = $('[data-drop]');
    const newFiles = [];            // File objects added in this session
    const removed = new Set();      // ids of existing images to delete

    initPickupPicker(form);

    const counter = $('[data-count="description"]');
    const updateCount = () => { counter.textContent = form.description.value.length; };
    form.description.addEventListener('input', updateCount);
    updateCount();

    const photoCount = () => grid.querySelectorAll('.thumb').length;
    const refreshCover = () => grid.querySelectorAll('.thumb').forEach((t, i) => {
        t.querySelector('.thumb__cover')?.remove();
        if (i === 0) t.insertAdjacentHTML('beforeend', '<span class="thumb__cover">Cover</span>');
    });

    const addFiles = (files) => {
        setFieldError(form, 'photos', '');
        for (const file of files) {
            if (!TYPES.includes(file.type)) { setFieldError(form, 'photos', `"${file.name}" is not a JPG, PNG or WebP image.`); continue; }
            if (file.size > MAX_BYTES) { setFieldError(form, 'photos', `"${file.name}" is larger than 5 MB.`); continue; }
            if (photoCount() >= MAX_PHOTOS) { setFieldError(form, 'photos', `You can add up to ${MAX_PHOTOS} photos.`); break; }
            newFiles.push(file);
            const url = URL.createObjectURL(file);
            const thumb = document.createElement('div');
            thumb.className = 'thumb';
            thumb.dataset.newIndex = String(newFiles.length - 1);
            thumb.innerHTML = html`<img src="${url}" alt=""><button class="thumb__remove" type="button" aria-label="Remove photo">${icon('x')}</button>`;
            grid.append(thumb);
        }
        refreshCover();
        input.value = '';
    };

    input.addEventListener('change', () => addFiles([...input.files]));
    ['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.add('is-dragover'); }));
    ['dragleave', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.remove('is-dragover'); }));
    drop.addEventListener('drop', (e) => addFiles([...e.dataTransfer.files]));

    grid.addEventListener('click', (e) => {
        const btn = e.target.closest('.thumb__remove');
        if (!btn) return;
        const thumb = btn.closest('.thumb');
        if (thumb.dataset.existing) removed.add(thumb.dataset.existing);
        if (thumb.dataset.newIndex) newFiles[Number(thumb.dataset.newIndex)] = null;
        thumb.remove();
        refreshCover();
    });

    const hasPhoto = () => (photoCount() > 0 ? '' : 'Add at least one photo of the item.');
    const weight = (v) => (!v || (Number(v) > 0 && Number(v) <= 500) ? '' : 'Enter a weight between 0.01 and 500 kg.');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const ok = validate(form, {
            name: [rules.required, rules.minLength(3), rules.maxLength(120)],
            categoryId: [rules.required],
            condition: [rules.required],
            description: [rules.required, rules.minLength(10), rules.maxLength(2000)],
            pickupLocation: [rules.required, rules.minLength(2), rules.maxLength(150)],
            approximateWeight: [weight],
        });
        const photosError = hasPhoto();
        if (photosError) setFieldError(form, 'photos', photosError);
        if (!ok || photosError) return;

        const body = new FormData();
        ['name', 'categoryId', 'description', 'condition', 'pickupLocation', 'approximateWeight', 'pickupLatitude', 'pickupLongitude'].forEach((f) => {
            const value = form.elements.namedItem(f).value.trim();
            if (value) body.append(f, value);
        });
        body.append('listingType', form.listingType.value);
        newFiles.filter(Boolean).forEach((file) => body.append('photos', file));
        removed.forEach((id) => body.append('removeImageIds', id));

        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            if (itemId) {
                await api.put(`/api/items/${itemId}`, body);
                window.location.assign('/my/listings?updated');
            } else {
                await api.post('/api/items', body);
                window.location.assign('/my/listings?submitted');
            }
        } catch (err) {
            setBusy(button, false);
            showApiError(form, err);
        }
    });
}
