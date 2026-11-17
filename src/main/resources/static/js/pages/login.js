// Login page: posts credentials as JSON and reacts to the account-status error codes.
import { api } from '../core/api.js';
import { safeNext } from '../core/auth.js';
import { setBusy } from '../core/ui.js';
import { clearErrors, rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const form = document.getElementById('login-form');
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate(form, { email: [rules.required, rules.email], password: [rules.required] })) return;
        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            const result = await api.post('/api/auth/login', {
                email: form.email.value.trim(),
                password: form.password.value,
                rememberMe: form.rememberMe.checked,
            });
            // Students go back to where they were heading; admins always land on their dashboard.
            window.location.assign(result.user.role === 'ADMIN' ? result.redirectUrl : safeNext(result.redirectUrl));
        } catch (err) {
            setBusy(button, false);
            if (err.code === 'ACCOUNT_PENDING') {
                window.location.assign('/register/pending?state=pending');
                return;
            }
            clearErrors(form);
            showApiError(form, err);
            if (err.code === 'INVALID_CREDENTIALS') form.password.select();
        }
    });
}
