// Forgot password: always shows the same confirmation, whether or not the account exists.
import { api } from '../core/api.js';
import { setBusy } from '../core/ui.js';
import { rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const form = document.getElementById('forgot-form');
    const success = document.querySelector('[data-success]');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate(form, { email: [rules.required, rules.email] })) return;
        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            const result = await api.post('/api/auth/forgot-password', { email: form.email.value.trim() });
            success.textContent = result.message;
            success.hidden = false;
            form.hidden = true;
        } catch (err) {
            showApiError(form, err);
        } finally {
            setBusy(button, false);
        }
    });
}
