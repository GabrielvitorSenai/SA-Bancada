const api = {
    async get(url) {
        return request(url, { method: 'GET' });
    },

    async post(url, body) {
        return request(url, {
            method: 'POST',
            body: body ? JSON.stringify(body) : null
        });
    },

    async put(url, body) {
        return request(url, {
            method: 'PUT',
            body: body ? JSON.stringify(body) : null
        });
    },

    async delete(url) {
        return request(url, { method: 'DELETE' });
    }
};

async function request(url, options = {}) {
    const response = await fetch(url, {
        headers: {
            'Content-Type': 'application/json'
        },
        ...options
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
