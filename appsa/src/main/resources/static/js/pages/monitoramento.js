/**
 * Tela de monitoramento e leitura dos processos do CLP.
 * Usa SSE em /api/clp/smartstream/{bancada} e status em /api/clp/status.
 */
document.addEventListener('DOMContentLoaded', () => {
    const streams = {};
    const bancadas = ['estoque', 'processo', 'montagem', 'expedicao'];

    document.querySelector('#btnStartMonitor').addEventListener('click', iniciarStreams);
    document.querySelector('#btnStopMonitor').addEventListener('click', pararStreams);
    document.querySelector('#btnResetStatus').addEventListener('click', resetarStatus);

    carregarStatus();
    setInterval(carregarStatus, 2000);

    function iniciarStreams() {
        bancadas.forEach(nome => {
            if (streams[nome]) return;

            const source = new EventSource(`/api/clp/smartstream/${nome}`);
            streams[nome] = source;

            source.addEventListener('leitura', event => {
                const el = document.querySelector(`#hex-${nome}`);
                if (el) el.textContent = event.data || '';
            });

            source.addEventListener('aguardando', event => {
                const el = document.querySelector(`#hex-${nome}`);
                if (el) el.textContent = event.data || 'Aguardando dados...';
            });

            source.onerror = () => {
                const el = document.querySelector(`#hex-${nome}`);
                if (el) el.textContent = 'Stream indisponível. Verifique a conexão.';
                source.close();
                delete streams[nome];
            };
        });

        Smart40.UI.toast('Monitoramento iniciado.', 'success');
    }

    function pararStreams() {
        Object.values(streams).forEach(s => s.close());
        Object.keys(streams).forEach(k => delete streams[k]);
        Smart40.UI.toast('Monitoramento pausado.', 'success');
    }

    async function resetarStatus() {
        try {
            await Smart40.API.post('/api/clp/smart/reset-status');
            Smart40.UI.toast('Status zerados.', 'success');
            await carregarStatus();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao resetar status.', 'error');
        }
    }

    async function carregarStatus() {
        try {
            const status = await Smart40.API.get('/api/clp/status');

            atualizarCard('estoque', status.estoqueConectado, status.statusEstoque);
            atualizarCard('processo', status.processoConectado, status.statusProcesso);
            atualizarCard('montagem', status.montagemConectado, status.statusMontagem);
            atualizarCard('expedicao', status.expedicaoConectado, status.statusExpedicao);

            Smart40.UI.setText('#monPedidoCurso', status.pedidoEmCurso ? 'Sim' : 'Não');
            Smart40.UI.setText('#monPedidoFinalizado', status.pedidoFinalizado ? 'Sim' : 'Não');
            Smart40.UI.setText('#monStatusProducao', Number(status.statusProducao) === 1 ? 'Finalizada' : 'Em andamento');

            const online = Boolean(status.estoqueConectado || status.processoConectado || status.montagemConectado || status.expedicaoConectado);
            Smart40.UI.setConnectionPill(online, online ? 'Monitoramento recebendo dados' : 'Sem leituras ativas');
        } catch {}
    }

    function atualizarCard(nome, conectado, status) {
        const card = document.querySelector(`[data-monitor="${nome}"]`);
        if (!card) return;

        card.classList.toggle('online', Boolean(conectado));
        const statusEl = card.querySelector('[data-status]');
        const connEl = card.querySelector('[data-conn]');

        if (statusEl) statusEl.textContent = Smart40.UI.statusEstacao(status);
        if (connEl) connEl.textContent = conectado ? 'Conectado' : 'Desconectado';

        const img = card.querySelector('img');
        if (img) {
            const prefix = {
                estoque: 'Smart40_Estoque',
                processo: 'Smart40_Processo',
                montagem: 'Smart40_Montagem',
                expedicao: 'Smart40_Expedicao'
            }[nome];
            const suffix = conectado ? Number(status || 0) : 0;
            img.src = `/assets/bancada/${prefix}_${suffix}.png`;
        }
    }
});
