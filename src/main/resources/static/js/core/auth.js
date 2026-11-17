// Authentication helpers used by the header and the auth pages.
import { api } from './api.js';
import { toastError } from './ui.js';

export async function logout() {
    try {
        await api.post('/api/auth/logout');
    } catch (err) {
        if (err.status !== 401) { toastError(err); return; }
    }
    window.location.assign('/?loggedOut');
}

/** Only allows same-site relative redirects after login (prevents open redirects). */
export function safeNext(defaultPath) {
    const next = new URLSearchParams(window.location.search).get('next');
    return next && next.startsWith('/') && !next.startsWith('//') ? next : defaultPath;
}
