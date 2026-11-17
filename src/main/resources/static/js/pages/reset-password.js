// Reset password with the single-use token from the e-mailed link.
import { api } from '../core/api.js';
import { setBusy } from '../core/ui.js';
import { rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const form = document.getElementById('reset-form');
    if (!form) return;
    const matches = (value) => (value === form.password.value ? '' : 'Passwords do not match.');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate(form, { password: [rules.required, rules.password], confirmPassword: [rules.required, matches] })) return;
        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            await api.post('/api/auth/reset-password', {
                token: form.token.value, password: form.password.value, confirmPassword: form.confirmPassword.value,
            });
            window.location.assign('/login?reset');
        } catch (err) {
            setBusy(button, false);
            showApiError(form, err);
        }
    });
}
