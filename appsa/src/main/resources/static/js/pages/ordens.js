/**
 * Tela Ordens de Produção.
 * Produzir: PUT /api/pedidos/{id}/produzir
 * Finalizar: PUT /api/pedidos/{id}/status
 */
document.addEventListener('DOMContentLoaded', () => {
    const grid = document.querySelector('#ordensGrid');
    const btnReload = document.querySelector('#btnReloadOrdens');
    let pedidos = [];
    let filtro = 0;

    btnReload.addEventListener('click', carregarOrdens);

    document.querySelectorAll('.chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            filtro = Number(chip.dataset.status || 0);
            renderizar();
        });
    });

    carregarOrdens();
    setInterval(carregarOrdens, 5000);

    async function carregarOrdens() {
        try {
            pedidos = await Smart40.API.get('/api/pedidos');
            renderizar();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao carregar ordens.', 'error');
            grid.innerHTML = '<div class="empty">Erro ao carregar ordens.</div>';
        }
    }

    function renderizar() {
        const lista = filtro ? pedidos.filter(p => Number(p.status || 1) === filtro) : pedidos;

        if (!lista.length) {
            grid.innerHTML = '<div class="empty">Nenhuma ordem encontrada.</div>';
            atualizarMetricas([]);
            return;
        }

        lista.sort((a, b) => Number(b.idPedido || 0) - Number(a.idPedido || 0));
        grid.innerHTML = lista.map(cardPedido).join('');

        grid.querySelectorAll('[data-action="produzir"]').forEach(btn => {
            btn.addEventListener('click', () => enviarParaProducao(btn.dataset.id));
        });

        grid.querySelectorAll('[data-action="finalizar"]').forEach(btn => {
            btn.addEventListener('click', () => finalizarPedido(btn.dataset.id));
        });

        atualizarMetricas(pedidos);
    }

    function cardPedido(pedido) {
        const id = pedido.idPedido || pedido.id;
        const status = Number(pedido.status || 1);
        const numero = pedido.numeroPedido || id;
        const blocos = pedido.blocos || [];

        const action = status === 1
            ? `<button class="btn btn-primary" data-action="produzir" data-id="${id}">Enviar para produção</button>`
            : status === 2
                ? `<button class="btn btn-warning" data-action="finalizar" data-id="${id}">Finalizar e expedir</button>`
                : `<span class="status-badge status-3">Expedido</span>`;

        return `
            <article class="order-card status-border-${status}">
                <div class="order-card__top">
                    <h3>Pedido #${numero}</h3>
                    <span class="status-badge status-${status}">${Smart40.UI.statusPedido(status)}</span>
                </div>

                <p class="order-line"><b>ID:</b> ${id}</p>
                <p class="order-line"><b>Tipo:</b> ${pedido.tipoPedido} andar(es)</p>
                <p class="order-line"><b>Tampa:</b> ${Smart40.UI.colorName(pedido.corTampa)}</p>
                <p class="order-line"><b>Posição expedição:</b> ${pedido.posicaoExpedicao || '-'}</p>

                <div class="mini-blocos">
                    ${blocos.map(b => `<span class="mini-bloco cor-${b.corBloco || 0}">A${b.andar}: ${Smart40.UI.colorName(b.corBloco)}</span>`).join('')}
                </div>

                <div class="order-actions">${action}</div>
            </article>
        `;
    }

    async function enviarParaProducao(id) {
        try {
            await Smart40.API.put(`/api/pedidos/${id}/produzir`);
            Smart40.UI.toast('Pedido enviado para produção.', 'success');
            await carregarOrdens();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao enviar para produção.', 'error');
        }
    }

    async function finalizarPedido(id) {
        try {
            if (!Smart40.UI.confirm('Deseja finalizar este pedido e enviar para a expedição?')) {
                return;
            }

            await Smart40.API.put(`/api/pedidos/${id}/status`);

            Smart40.UI.toast('Pedido finalizado e enviado para a expedição.', 'success');
            await carregarOrdens();
            await carregarStatusRapido();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao finalizar pedido.', 'error');
        }
    }

    async function carregarStatusRapido() {
        try {
            const status = await Smart40.API.get('/api/clp/status');
            const online = Boolean(status.estoqueConectado || status.processoConectado || status.montagemConectado || status.expedicaoConectado);
            Smart40.UI.setConnectionPill(online, status.pedidoFinalizado ? 'Pedido finalizado na bancada' : 'Status atualizado');
        } catch {}
    }

    function atualizarMetricas(lista) {
        Smart40.UI.setText('#ordensPendentes', lista.filter(p => Number(p.status || 1) === 1).length);
        Smart40.UI.setText('#ordensProducao', lista.filter(p => Number(p.status || 1) === 2).length);
        Smart40.UI.setText('#ordensConcluidas', lista.filter(p => Number(p.status || 1) === 3).length);
        Smart40.UI.setText('#ordensAtualizada', Smart40.UI.now());
    }
});
