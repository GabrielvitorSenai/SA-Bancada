// Estado central simples. Evita variáveis espalhadas pelos módulos.
export const state = {
    estoque: [],
    expedicao: [],
    pedidos: [],
    filtroStatus: 0,
    posicaoSelecionada: null,
    clp: {
        conectado: false,
        streams: {},
        ultimoHex: {
            estoque: '',
            processo: '',
            montagem: '',
            expedicao: ''
        },
        status: {
            estoque: 0,
            processo: 0,
            montagem: 0,
            expedicao: 0,
            producao: 0,
            pedidoEmCurso: 0
        }
    }
};
