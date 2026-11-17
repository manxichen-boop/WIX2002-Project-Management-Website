// Client-side validation mirrors the backend rules for fast feedback. The backend re-validates
// everything; these helpers also display the field errors the API returns.

export const rules = {
    required: (v) => (String(v ?? '').trim() ? '' : 'This field is required.'),
    email: (v) => (/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(String(v).trim()) ? '' : 'Enter a valid e-mail address.'),
    password: (v) => {
        const s = String(v ?? '');
        if (s.length < 8) return 'Use at least 8 characters.';
        if (new TextEncoder().encode(s).length > 72) return 'Use 72 characters or fewer.';
        if (!/[A-Za-z]/.test(s) || !/\d/.test(s)) return 'Include at least one letter and one number.';
        return '';
    },
    maxLength: (n) => (v) => (String(v ?? '').length <= n ? '' : `Use ${n} characters or fewer.`),
    minLength: (n) => (v) => (String(v ?? '').trim().length >= n ? '' : `Use at least ${n} characters.`),
};

function fieldFor(form, name) {
    const input = form.elements.namedItem(name);
    const el = input instanceof RadioNodeList ? input[0] : input;
    return el?.closest('.field') ?? null;
}

export function setFieldError(form, name, message) {
    const field = fieldFor(form, name);
    if (!field) return false;
    field.classList.toggle('has-error', Boolean(message));
    let error = field.querySelector('.field__error');
    if (!error) {
        error = document.createElement('p');
        error.className = 'field__error';
        error.id = `${name}-error`;
        field.append(error);
    }
    error.textContent = message || '';
    error.hidden = !message;
    const input = field.querySelector('input, select, textarea');
    if (input) {
        input.setAttribute('aria-invalid', message ? 'true' : 'false');
        if (message) input.setAttribute('aria-describedby', error.id);
    }
    return true;
}

export function clearErrors(form) {
    form.querySelectorAll('.field.has-error').forEach((f) => f.classList.remove('has-error'));
    form.querySelectorAll('.field__error').forEach((e) => { e.textContent = ''; e.hidden = true; });
    form.querySelectorAll('[aria-invalid="true"]').forEach((i) => i.setAttribute('aria-invalid', 'false'));
    const banner = form.querySelector('[data-form-error]');
    if (banner) banner.hidden = true;
}

/** Validates `{ fieldName: [rule, …] }`; shows messages and focuses the first invalid field. */
export function validate(form, schema) {
    clearErrors(form);
    let firstInvalid = null;
    Object.entries(schema).forEach(([name, checks]) => {
        const input = form.elements.namedItem(name);
        const value = input?.type === 'checkbox' ? (input.checked ? 'yes' : '') : input?.value;
        const message = checks.map((check) => check(value, form)).find(Boolean);
        if (message) {
            setFieldError(form, name, message);
            firstInvalid ??= input instanceof RadioNodeList ? input[0] : input;
        }
    });
    firstInvalid?.focus();
    return !firstInvalid;
}

/** Shows an ApiError on the form: field errors next to fields, the rest in the form banner. */
export function showApiError(form, error) {
    let placed = false;
    (error.fieldErrors || []).forEach((fe) => { placed = setFieldError(form, fe.field, fe.message) || placed; });
    const banner = form.querySelector('[data-form-error]');
    if (banner && (!placed || !error.fieldErrors?.length)) {
        banner.textContent = error.message;
        banner.hidden = false;
        banner.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
    return placed || Boolean(banner);
}
