export async function apiRequest(url, options = null) {
    const requestOptions = options && typeof options === 'object' ? options : null;
    const token = localStorage.getItem('cc.token');
    const headers = { 'Content-Type': 'application/json' };
    if (requestOptions?.headers) {
        Object.assign(headers, requestOptions.headers);
    }
    if (token) {
        headers.Authorization = `Bearer ${token}`;
    }
    const fetchOptions = requestOptions ? { ...requestOptions, headers } : { headers };
    if (fetchOptions.cache === undefined) {
        fetchOptions.cache = 'no-store';
    }
    return fetch(url, fetchOptions);
}

export async function safeJson(response) {
    if (!response) {
        return null;
    }
    const contentType = response.headers?.get('content-type') || '';
    if (!contentType.includes('application/json')) {
        return null;
    }
    const text = await response.text();
    if (!text) {
        return null;
    }
    return JSON.parse(text);
}
