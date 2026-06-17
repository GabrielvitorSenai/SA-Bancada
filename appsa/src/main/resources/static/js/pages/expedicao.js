/**
 * Tela de Expedição.
 * Lista as posições do banco e remove limpando banco + memória do CLP.
 */
document.addEventListener('DOMContentLoaded', () => {
    const btnReload = document.querySelector('#btnReloadExp');
    const grid = document.querySelector('#expedicaoGrid');

    btnReload.addEventListener('click', carregarTudo);

    carregarTudo();
    setInterval(carregarTudo, 3000);

    async function carregarTudo() {
        await Promise.allSettled([
            carregarExpedicao(),
            carregarStatusClp()
        ]);
    }

    async function carregarExpedicao() {
        try {
            const registros = await Smart40.API.get('/api/expedicao');
            const porPos = {};

            (registros || []).forEach(r => {
                const p = Number(r.posicaoExpedicao);
                if (p >= 1 && p <= 12) porPos[p] = r;
            });

            grid.innerHTML = Array.from({ length: 12 }, (_, i) => {
                const pos = i + 1;
                const r = porPos[pos];

                if (!r) {
                    return `
                        <article class="exp-cell" data-pos="${pos}">
                            <div>
                                <strong>Pos ${pos}</strong><br>
                                <small>Livre</small>
                            </div>
                        </article>
                    `;
                }

                return `
                    <article class="exp-cell busy" data-pos="${pos}">
                        <div>
                            <strong>Pos ${pos}</strong><br>
                            <small>Pedido/OP ${r.pedidoId ?? '-'}</small><br>
                            <small>${Smart40.UI.formatDate(r.dataSaida)}</small>
                        </div>

                        <button class="btn btn-danger btn-mini" data-action="limpar-exp" data-pos="${pos}">
                            Limpar
                        </button>
                    </article>
                `;
            }).join('');

            grid.querySelectorAll('[data-action="limpar-exp"]').forEach(btn => {
                btn.addEventListener('click', () => removerPosicaoExpedicao(Number(btn.dataset.pos)));
            });

            const ocupadas = Object.keys(porPos).length;
            Smart40.UI.setText('#expOcupadas', ocupadas);
            Smart40.UI.setText('#expLivres', 12 - ocupadas);
            Smart40.UI.setText('#expAtualizada', Smart40.UI.now());

        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao carregar expedição.', 'error');
            grid.innerHTML = '<div class="empty">Erro ao carregar expedição.</div>';
        }
    }

    async function removerPosicaoExpedicao(posicao) {
        try {
            if (!posicao) {
                Smart40.UI.toast('Posição inválida.', 'error');
                return;
            }

            if (!Smart40.UI.confirm(`Deseja limpar a posição ${posicao} da expedição?`)) {
                return;
            }

            await Smart40.API.delete(`/api/expedicao/posicao/${posicao}`);

            Smart40.UI.toast(`Posição ${posicao} removida da expedição.`, 'success');
            await carregarTudo();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao remover posição da expedição.', 'error');
        }
    }

    async function carregarStatusClp() {
        try {
            const status = await Smart40.API.get('/api/clp/status');

            Smart40.UI.setText('#statusPedidoFinalizado', status.pedidoFinalizado ? 'Sim' : 'Não');
            Smart40.UI.setText('#statusExpedicao', Smart40.UI.statusEstacao(status.statusExpedicao));
            Smart40.UI.setText('#statusProducao', Number(status.statusProducao) === 1 ? 'Finalizada' : 'Em andamento');

            const online = Boolean(status.expedicaoConectado);
            Smart40.UI.setConnectionPill(online, online ? 'Expedição com leitura ativa' : 'Expedição desconectada');
        } catch {
            Smart40.UI.setText('#statusPedidoFinalizado', '-');
            Smart40.UI.setText('#statusExpedicao', '-');
            Smart40.UI.setText('#statusProducao', '-');
        }
    }
});
