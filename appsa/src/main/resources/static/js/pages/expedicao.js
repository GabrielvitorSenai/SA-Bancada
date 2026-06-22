document.addEventListener('DOMContentLoaded', () => {
    carregarExpedicao();
    carregarStatusClp();

    setInterval(() => {
        carregarExpedicao();
        carregarStatusClp();
    }, 3000);
});

async function carregarExpedicao() {
    const container = document.querySelector('#expedicaoGrid');

    if (!container) {
        return;
    }

    try {
        const expedicoes = await api.get('/api/expedicao');

        if (!expedicoes || expedicoes.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <strong>Nenhuma posição ocupada</strong>
                    <span>A expedição ainda não possui pedidos armazenados.</span>
                </div>
            `;
            return;
        }

        expedicoes.sort((a, b) => Number(a.posicaoExpedicao || 0) - Number(b.posicaoExpedicao || 0));

        container.innerHTML = expedicoes.map(item => {
            const posicao = item.posicaoExpedicao;
            const pedidoId = item.pedidoId ?? '-';

            return `
                <article class="expedicao-card">
                    <div class="expedicao-card__header">
                        <span class="badge badge-success">Posição ${posicao}</span>
                        <strong>Pedido/OP ${pedidoId}</strong>
                    </div>

                    <div class="expedicao-card__body">
                        <p>
                            <span>Data de saída:</span>
                            <strong>${formatarData(item.dataSaida)}</strong>
                        </p>
                    </div>

                    <div class="expedicao-card__actions">
                        <button class="btn btn-danger" onclick="removerPosicaoExpedicao(${posicao})">
                            Remover posição
                        </button>
                    </div>
                </article>
            `;
        }).join('');

    } catch (error) {
        console.error(error);
        container.innerHTML = `
            <div class="empty-state error">
                <strong>Erro ao carregar expedição</strong>
                <span>${error.message}</span>
            </div>
        `;
    }
}

async function removerPosicaoExpedicao(posicao) {
    try {
        if (!confirm(`Limpar a posição ${posicao} da expedição?`)) {
            return;
        }

        await api.delete(`/api/expedicao/posicao/${posicao}`);

        toastSucesso(`Posição ${posicao} removida da expedição.`);

        await carregarExpedicao();
        await carregarStatusClp();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao remover posição da expedição.');
    }
}

async function carregarStatusClp() {
    try {
        const status = await api.get('/api/clp/status');

        setTexto('#statusPedidoFinalizado', status.pedidoFinalizado ? 'Sim' : 'Não');
        setTexto('#statusExpedicao', traduzirStatus(status.statusExpedicao));
        setTexto('#statusProducao', traduzirStatusProducao(status.statusProducao));

    } catch (error) {
        console.warn('Status CLP indisponível:', error);
    }
}

function setTexto(seletor, texto) {
    const el = document.querySelector(seletor);
    if (el) {
        el.textContent = texto;
    }
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

function formatarData(data) {
    if (!data) {
        return '-';
    }

    try {
        return new Date(data).toLocaleString('pt-BR');
    } catch {
        return data;
    }
}
