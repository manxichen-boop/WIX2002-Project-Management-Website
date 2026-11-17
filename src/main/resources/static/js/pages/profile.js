// Profile: update name / university, profile photo and password.
import { api } from '../core/api.js';
import { $, html, setBusy, toast, toastError, withBusy } from '../core/ui.js';
import { clearErrors, rules, showApiError, validate } from '../core/validation.js';

export function init() {
    const profileForm = $('#profile-form');
    profileForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (!validate(profileForm, { fullName: [rules.required, rules.minLength(2)], university: [rules.required, rules.minLength(2)] })) return;
        const button = profileForm.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            await api.put('/api/profile', { fullName: profileForm.fullName.value.trim(), university: profileForm.university.value.trim() });
            toast('Profile updated.');
        } catch (err) { showApiError(profileForm, err); } finally { setBusy(button, false); }
    });

    const passwordForm = $('#password-form');
    passwordForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const matches = (v) => (v === passwordForm.newPassword.value ? '' : 'Passwords do not match.');
        if (!validate(passwordForm, { currentPassword: [rules.required], newPassword: [rules.required, rules.password], confirmPassword: [rules.required, matches] })) return;
        const button = passwordForm.querySelector('[type="submit"]');
        setBusy(button, true);
        try {
            await api.patch('/api/profile/password', {
                currentPassword: passwordForm.currentPassword.value, newPassword: passwordForm.newPassword.value,
                confirmPassword: passwordForm.confirmPassword.value,
            });
            passwordForm.reset();
            clearErrors(passwordForm);
            toast('Password changed.');
        } catch (err) { showApiError(passwordForm, err); } finally { setBusy(button, false); }
    });

    const input = $('#photo-input');
    const avatar = $('#avatar');
    input.addEventListener('change', async () => {
        const file = input.files[0];
        if (!file) return;
        if (file.size > 5 * 1024 * 1024) { toastError({ message: 'Choose an image of 5 MB or less.' }); return; }
        const body = new FormData();
        body.append('photo', file);
        try {
            const me = await api.post('/api/profile/photo', body);
            avatar.innerHTML = html`<img src="${me.avatarUrl}?t=${Date.now()}" alt="">`;
            toast('Profile photo updated.');
            setTimeout(() => window.location.reload(), 700);
        } catch (err) { toastError(err); } finally { input.value = ''; }
    });

    const remove = $('[data-remove-photo]');
    remove?.addEventListener('click', () => withBusy(remove, async () => {
        await api.delete('/api/profile/photo');
        toast('Profile photo removed.');
        setTimeout(() => window.location.reload(), 700);
    }));
}
