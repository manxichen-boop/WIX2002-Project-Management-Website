// Account status page: lets students request a new e-mail confirmation link.
import { api } from '../core/api.js';
import { toast, withBusy } from '../core/ui.js';
import { rules, validate } from '../core/validation.js';

export function init() {
    const form = document.getElementById('resend-form');
    if (!form) return;
    form.addEventListener('submit', (e) => {
        e.preventDefault();
        if (!validate(form, { email: [rules.required, rules.email] })) return;
        withBusy(form.querySelector('[type="submit"]'), async () => {
            const result = await api.post('/api/auth/resend-verification', { email: form.email.value.trim() });
            toast(result.message);
            form.reset();
        });
    });
}
