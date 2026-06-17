// Base vazia mantém chamadas relativas ao mesmo host do Spring Boot.
export const API_BASE = window.API_BASE ?? '';

export async function requestJson(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, {
        headers: {
            'Content-Type': 'application/json',
            ...(options.headers ?? {})
        },
        ...options
    });

    const isNoContent = response.status === 204;
    const body = isNoContent ? null : await response.json().catch(() => null);

    if (!response.ok) {
        const message = body?.erro || body?.message || body?.error || body || `Erro HTTP ${response.status}`;
        throw new Error(typeof message === 'string' ? message : JSON.stringify(message));
    }

    return body;
}
