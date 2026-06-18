document.addEventListener('DOMContentLoaded', () => {
    carregarStatusClp();
    iniciarStreams();

    setInterval(carregarStatusClp, 2000);
});

async function carregarStatusClp() {
    try {
        const status = await api.get('/api/clp/status');

        preencher('#statusEstoque', traduzirStatus(status.statusEstoque));
        preencher('#statusProcesso', traduzirStatus(status.statusProcesso));
        preencher('#statusMontagem', traduzirStatus(status.statusMontagem));
        preencher('#statusExpedicao', traduzirStatus(status.statusExpedicao));
        preencher('#statusProducao', traduzirStatusProducao(status.statusProducao));
        preencher('#pedidoEmCurso', status.pedidoEmCurso ? 'Sim' : 'Não');
        preencher('#pedidoFinalizado', status.pedidoFinalizado ? 'Sim' : 'Não');

        marcarConexao('#connEstoque', status.estoqueConectado);
        marcarConexao('#connProcesso', status.processoConectado);
        marcarConexao('#connMontagem', status.montagemConectado);
        marcarConexao('#connExpedicao', status.expedicaoConectado);

    } catch (error) {
        console.warn('Erro ao carregar status do CLP:', error);
    }
}

function iniciarStreams() {
    ['estoque', 'processo', 'montagem', 'expedicao'].forEach(nome => {
        const alvo = document.querySelector(`#stream-${nome}`);

        if (!alvo) {
            return;
        }

        const source = new EventSource(`/api/clp/smartstream/${nome}`);

        source.addEventListener('leitura', event => {
            alvo.textContent = event.data;
        });

        source.addEventListener('aguardando', event => {
            alvo.textContent = event.data;
        });

        source.onerror = () => {
            alvo.textContent = 'Stream desconectado. Tentando reconectar...';
        };
    });
}

async function resetarStatus() {
    try {
        await api.post('/api/clp/smart/reset-status');
        toastSucesso('Status resetados.');
        await carregarStatusClp();
    } catch (error) {
        toastErro(error.message || 'Erro ao resetar status.');
    }
}

async function alterarReadOnly(value) {
    try {
        await api.post(`/api/clp/smart/readonly?value=${value}`);
        toastSucesso(`Modo somente leitura: ${value ? 'ativado' : 'desativado'}.`);
    } catch (error) {
        toastErro(error.message || 'Erro ao alterar modo readOnly.');
    }
}

function preencher(seletor, valor) {
    const el = document.querySelector(seletor);
    if (el) {
        el.textContent = valor;
    }
}

function marcarConexao(seletor, conectado) {
    const el = document.querySelector(seletor);

    if (!el) {
        return;
    }

    el.textContent = conectado ? 'Conectado' : 'Sem leitura';
    el.classList.toggle('is-online', !!conectado);
    el.classList.toggle('is-offline', !conectado);
}

function traduzirStatus(status) {
    switch (Number(status)) {
        case 0: return 'Aguardando';
        case 1: return 'Em operação';
        case 2: return 'Finalizado';
        default: return 'Indefinido';
    }
}

function traduzirStatusProducao(status) {
    switch (Number(status)) {
        case 0: return 'Em andamento';
        case 1: return 'Finalizada';
        default: return 'Aguardando';
    }
}
