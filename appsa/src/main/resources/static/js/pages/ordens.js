document.addEventListener('DOMContentLoaded', () => {
    carregarOrdens();
    setInterval(carregarOrdens, 5000);
});

async function carregarOrdens() {
    const container = document.querySelector('#ordensGrid');

    if (!container) {
        return;
    }

    try {
        const pedidos = await api.get('/api/pedidos');

        if (!pedidos || pedidos.length === 0) {
            container.innerHTML = `
                <div class="empty-state">
                    <strong>Nenhuma ordem encontrada</strong>
                    <span>Crie um pedido para iniciar o fluxo de produção.</span>
                </div>
            `;
            return;
        }

        pedidos.sort((a, b) => Number(b.numeroPedido || 0) - Number(a.numeroPedido || 0));

        container.innerHTML = pedidos.map(pedido => {
            const id = pedido.idPedido || pedido.id;
            const status = Number(pedido.status || 1);

            return `
                <article class="order-card status-${status}">
                    <div class="order-card__top">
                        <span class="badge">${traduzirStatusPedido(status)}</span>
                        <strong>OP ${pedido.numeroPedido || '-'}</strong>
                    </div>

                    <div class="order-card__body">
                        <p><span>ID:</span> <strong>${id}</strong></p>
                        <p><span>Tipo:</span> <strong>${pedido.tipoPedido || '-'} andar(es)</strong></p>
                        <p><span>Tampa:</span> <strong>${nomeCor(pedido.corTampa)}</strong></p>
                        <p><span>Expedição:</span> <strong>${pedido.posicaoExpedicao || '-'}</strong></p>
                    </div>

                    <div class="order-card__actions">
                        ${status === 1 ? `
                            <button class="btn btn-primary" onclick="enviarParaProducao(${id})">
                                Enviar produção
                            </button>
                        ` : ''}

                        ${status !== 3 ? `
                            <button class="btn btn-success" onclick="finalizarPedido(${id})">
                                Finalizar / Expedir
                            </button>
                        ` : ''}

                        ${status === 1 ? `
                            <button class="btn btn-danger" onclick="removerPedido(${id})">
                                Excluir
                            </button>
                        ` : ''}
                    </div>
                </article>
            `;
        }).join('');

    } catch (error) {
        console.error(error);
        container.innerHTML = `
            <div class="empty-state error">
                <strong>Erro ao carregar ordens</strong>
                <span>${error.message}</span>
            </div>
        `;
    }
}

async function enviarParaProducao(idPedido) {
    try {
        if (!confirm('Enviar este pedido para produção?')) {
            return;
        }

        await api.put(`/api/pedidos/${idPedido}/produzir`);

        toastSucesso('Pedido enviado para produção.');
        await carregarOrdens();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao enviar para produção.');
    }
}

async function finalizarPedido(idPedido) {
    try {
        if (!confirm('Finalizar este pedido e enviar para a expedição?')) {
            return;
        }

        await api.put(`/api/pedidos/${idPedido}/status`);

        toastSucesso('Pedido finalizado e enviado para expedição.');
        await carregarOrdens();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao finalizar pedido.');
    }
}

async function removerPedido(idPedido) {
    try {
        if (!confirm('Excluir este pedido pendente?')) {
            return;
        }

        await api.delete(`/api/pedidos/${idPedido}`);

        toastSucesso('Pedido excluído.');
        await carregarOrdens();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao excluir pedido.');
    }
}

function traduzirStatusPedido(status) {
    switch (Number(status)) {
        case 1: return 'Pendente';
        case 2: return 'Em produção';
        case 3: return 'Concluído';
        default: return 'Indefinido';
    }
}

function nomeCor(cor) {
    switch (Number(cor)) {
        case 1: return 'Preto';
        case 2: return 'Vermelho';
        case 3: return 'Azul';
        default: return '-';
    }
}
