import { state } from '../core/state.js';
import { setText, $ } from '../core/dom.js';

export function renderMetrics() {
    const ocupadas = state.estoque.filter((p) => Number(p.cor ?? 0) !== 0).length;
    const expOcupadas = Array.isArray(state.expedicao) ? state.expedicao.length : 0;

    setText('#metricEstoque', `${ocupadas}/28`);
    setText('#metricExpedicao', `${expOcupadas}/12`);
    setText('#metricPedidos', String(state.pedidos.length));
    setText('#metricClpStatus', state.clp.conectado ? 'Online' : 'Offline');

    const statusCard = $('.metric-card--status');
    statusCard?.classList.toggle('is-online', state.clp.conectado);
}
