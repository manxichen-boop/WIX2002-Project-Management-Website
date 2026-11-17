// Shared Fetch client. Every page module talks to the backend through these helpers so that
// CSRF headers, JSON parsing, error shapes and authentication failures are handled in one place.

const csrfToken = document.querySelector('meta[name="_csrf"]')?.content;
const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.content ?? 'X-CSRF-TOKEN';

/** Error carrying the backend's ApiError body ({status, code, message, fieldErrors}). */
export class ApiError extends Error {
    constructor(status, body) {
        super(body?.message || 'Something went wrong. Please try again.');
        this.status = status;
        this.code = body?.code || 'UNKNOWN';
        this.fieldErrors = body?.fieldErrors || [];
    }
}

async function request(method, url, { body, query, signal } = {}) {
    const target = new URL(url, window.location.origin);
    if (query) {
        Object.entries(query).forEach(([k, v]) => {
            if (v !== undefined && v !== null && v !== '') target.searchParams.set(k, v);
        });
    }
    const headers = { Accept: 'application/json' };
    const options = { method, headers, credentials: 'same-origin', signal };
    if (method !== 'GET' && csrfToken) headers[csrfHeader] = csrfToken;
    if (body instanceof FormData) {
        options.body = body;                       // browser sets the multipart boundary
    } else if (body !== undefined) {
        headers['Content-Type'] = 'application/json';
        options.body = JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(target, options);
    } catch (e) {
        if (e.name === 'AbortError') throw e;
        throw new ApiError(0, { code: 'NETWORK', message: 'You appear to be offline. Check your connection and try again.' });
    }

    const isJson = response.headers.get('content-type')?.includes('application/json');
    const data = isJson ? await response.json().catch(() => null) : null;

    if (!response.ok) {
        // Session expired or never logged in: send the user to login and come back afterwards.
        if (response.status === 401 && !url.startsWith('/api/auth/')) {
            const next = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.assign(`/login?next=${next}`);
        }
        throw new ApiError(response.status, data);
    }
    return data;
}

export const api = {
    get: (url, query, signal) => request('GET', url, { query, signal }),
    post: (url, body) => request('POST', url, { body }),
    put: (url, body) => request('PUT', url, { body }),
    patch: (url, body) => request('PATCH', url, { body }),
    delete: (url) => request('DELETE', url),
};
