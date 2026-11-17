// Registration page: client checks for quick feedback; the server re-validates everything.
import { api } from '../core/api.js';
import { setBusy } from '../core/ui.js';
import { rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const form = document.getElementById('register-form');
    const matches = (value) => (value === form.password.value ? '' : 'Passwords do not match.');
    const accepted = (value) => (value ? '' : 'Please accept the community guidelines to continue.');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        const ok = validate(form, {
            fullName: [rules.required, rules.minLength(2), rules.maxLength(120)],
            email: [rules.required, rules.email],
            university: [rules.required, rules.minLength(2), rules.maxLength(150)],
            password: [rules.required, rules.password],
            confirmPassword: [rules.required, matches],
            acceptTerms: [accepted],
        });
        if (!ok) return;
        const button = form.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            await api.post('/api/auth/register', {
                fullName: form.fullName.value.trim(),
                email: form.email.value.trim(),
                university: form.university.value.trim(),
                password: form.password.value,
                confirmPassword: form.confirmPassword.value,
                acceptTerms: form.acceptTerms.checked,
            });
            window.location.assign('/register/pending?state=registered');
        } catch (err) {
            setBusy(button, false);
            if (err.code === 'EMAIL_TAKEN') err.fieldErrors = [{ field: 'email', message: err.message }];
            showApiError(form, err);
        }
    });
}
