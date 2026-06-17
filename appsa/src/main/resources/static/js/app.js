import { toast } from './core/toast.js';
import { $ } from './core/dom.js';
import { initNavigation } from './components/navigation.js';
import { bindEstoqueEvents, carregarEstoque } from './components/estoque.js';
import { bindPedidosEvents, carregarPedidos } from './components/pedidos.js';
import { initPedidoForm } from './components/pedidoForm.js';
import { initClpPanel } from './components/clp.js';
import { api } from './services/api.js';
import { state } from './core/state.js';
import { renderMetrics } from './components/metrics.js';

// Entrada principal do frontend.
// Mantém a inicialização explícita para facilitar manutenção e apresentação do código.
document.addEventListener('DOMContentLoaded', async () => {
    initNavigation();
    bindEstoqueEvents();
    bindPedidosEvents();
    initPedidoForm();
    initClpPanel();

    $('#btnAtualizarTudo')?.addEventListener('click', atualizarTudo);

    await atualizarTudo();
    setInterval(atualizarMonitoramentoLeve, 8000);
});

async function atualizarTudo() {
    await Promise.allSettled([
        carregarEstoque(),
        carregarPedidos(),
        carregarExpedicao()
    ]);

    renderMetrics();
}

async function atualizarMonitoramentoLeve() {
    await Promise.allSettled([
        carregarEstoque(),
        carregarExpedicao()
    ]);
    renderMetrics();
}

async function carregarExpedicao() {
    try {
        const data = await api.expedicao.listar();
        state.expedicao = Array.isArray(data) ? data : [];
    } catch (error) {
        // Algumas versões do backend ainda não possuem /api/expedicao.
        // Neste caso a tela continua funcionando e apenas mostra 0/12.
        state.expedicao = [];
    }
}

window.smart40 = {
    atualizarTudo,
    toast
};
