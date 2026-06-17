/**
 * Smart40 API helper.
 * Mantém todas as chamadas centralizadas e trata erro JSON/texto.
 */
window.Smart40 = window.Smart40 || {};

Smart40.API = {
    async json(url, options = {}) {
        return request(url, options);
    },

    async get(url) {
        return request(url, { method: 'GET' });
    },

    async post(url, body) {
        return request(url, {
            method: 'POST',
            body: body === undefined ? null : JSON.stringify(body)
        });
    },

    async put(url, body) {
        return request(url, {
            method: 'PUT',
            body: body === undefined ? null : JSON.stringify(body)
        });
    },

    async delete(url) {
        return request(url, { method: 'DELETE' });
    }
};

async function request(url, options = {}) {
    const headers = new Headers(options.headers || {});

    if (!headers.has('Content-Type') && options.body !== null) {
        headers.set('Content-Type', 'application/json');
    }

    const response = await fetch(url, {
        credentials: 'same-origin',
        ...options,
        headers
    });

    if (response.status === 204) {
        return null;
    }

    const contentType = response.headers.get('content-type') || '';
    let data;

    if (contentType.includes('application/json')) {
        data = await response.json();
    } else {
        data = await response.text();
    }

    if (!response.ok) {
        const mensagem = data?.detalhe || data?.message || data?.erro || data || 'Erro na requisição.';
        throw new Error(mensagem);
    }

    return data;
}
