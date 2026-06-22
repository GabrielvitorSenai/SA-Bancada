import { requestJson } from '../core/http.js';

export const api = {
    estoque: {
        listarPosicoes: () => requestJson('/api/estoque/posicoes'),
        adicionar: (payload) => requestJson('/api/estoque', {
            method: 'POST',
            body: JSON.stringify(payload)
        }),
        liberarPosicao: (posicao) => requestJson(`/api/estoque/posicao/${posicao}`, {
            method: 'DELETE'
        })
    },

    expedicao: {
        listar: () => requestJson('/api/expedicao')
    },

    pedidos: {
        listar: () => requestJson('/api/pedidos'),
        criar: (payload) => requestJson('/api/pedidos', {
            method: 'POST',
            body: JSON.stringify(payload)
        }),
        produzir: (id) => requestJson(`/api/pedidos/${id}/produzir`, {
            method: 'PUT'
        }),
        concluir: (id) => requestJson(`/api/pedidos/${id}/status`, {
            method: 'PUT'
        })
    },

    clp: {
        iniciarLeituras: (ips) => requestJson('/api/clp/start-readings', {
            method: 'POST',
            body: JSON.stringify(ips)
        }),
        pararLeituras: () => requestJson('/api/clp/stop-readings', {
            method: 'POST'
        }),
        streamUrl: (bancada) => `/api/clp/smartstream/${bancada}`
    }
};
