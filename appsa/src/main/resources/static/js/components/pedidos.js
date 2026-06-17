import { $, $$, setHTML } from '../core/dom.js';
import { toast } from '../core/toast.js';
import { state } from '../core/state.js';
import { api } from '../services/api.js';
import { renderMetrics } from './metrics.js';
import { corNome } from './estoque.js';

const TIPO_NOME = { 1: 'Simples', 2: 'Duplo', 3: 'Triplo' };
const STATUS = {
    1: { label: 'Pendente', className: 'status-1' },
    2: { label: 'Em produção', className: 'status-2' },
    3: { label: 'Concluído', className: 'status-3' }
};
const LAMINA_NOME = { 0: 'Nenhuma', 1: 'Vermelho', 2: 'Azul', 3: 'Amarelo', 4: 'Verde', 5: 'Preto', 6: 'Branco' };
const POSICAO_NOME = { 1: 'Esquerda', 2: 'Frente', 3: 'Direita' };
const PADRAO_NOME = { 0: 'Nenhum', 1: 'Casa', 2: 'Navio', 3: 'Estrela' };

export async function carregarPedidos() {
    try {
        const data = await api.pedidos.listar();
        state.pedidos = Array.isArray(data) ? data : [];
    } catch (error) {
        console.error(error);
        state.pedidos = [];
        toast('Não foi possível carregar pedidos.', 'error');
    }

    renderPedidos();
    renderMetrics();
}

export function bindPedidosEvents() {
    $$('.chip').forEach((chip) => {
        chip.addEventListener('click', () => {
            $$('.chip').forEach((item) => item.classList.remove('chip--active'));
            chip.classList.add('chip--active');
            state.filtroStatus = Number(chip.dataset.status || 0);
            renderPedidos();
        });
    });

    document.addEventListener('click', async (event) => {
        const produzirButton = event.target.closest('[data-action="produzir"]');
        const concluirButton = event.target.closest('[data-action="concluir"]');

        if (produzirButton) {
            await produzirPedido(Number(produzirButton.dataset.id));
        }

        if (concluirButton) {
            await concluirPedido(Number(concluirButton.dataset.id));
        }
    });
}

export function renderPedidos() {
    const lista = state.filtroStatus === 0
        ? state.pedidos
        : state.pedidos.filter((pedido) => Number(pedido.status) === state.filtroStatus);

    if (!lista.length) {
        setHTML('#pedidosGrid', '<div class="empty-state">Nenhum pedido encontrado neste filtro.</div>');
        return;
    }

    setHTML('#pedidosGrid', lista.map(cardPedido).join(''));
}

function cardPedido(pedido) {
    const status = STATUS[pedido.status] ?? { label: `Status ${pedido.status}`, className: 'status-1' };
    const blocos = Array.isArray(pedido.blocos) ? pedido.blocos : [];

    return `
        <article class="order-card">
            <h3>Pedido #${pedido.idPedido ?? '-'}</h3>
            <p class="order-line"><b>OP:</b> ${pedido.numeroPedido ?? '-'}</p>
            <p class="order-line"><b>Tipo:</b> ${TIPO_NOME[pedido.tipoPedido] ?? '-'}</p>
            <p class="order-line"><b>Tampa:</b> ${corNome(pedido.corTampa)}</p>
            <p class="order-line"><b>Expedição:</b> ${pedido.posicaoExpedicao ?? '-'}</p>
            <p class="order-line"><b>Status:</b> <span class="status-pill ${status.className}">${status.label}</span></p>

            ${acoesPedido(pedido)}

            <div class="order-blocks">
                ${blocos.map(blocoPedido).join('')}
            </div>
        </article>
    `;
}

function acoesPedido(pedido) {
    if (Number(pedido.status) === 1) {
        return `<button type="button" class="btn btn--primary" data-action="produzir" data-id="${pedido.idPedido}">Enviar para produção</button>`;
    }

    if (Number(pedido.status) === 2) {
        return `<button type="button" class="btn btn--soft" data-action="concluir" data-id="${pedido.idPedido}">Concluir e expedir</button>`;
    }

    return '';
}

function blocoPedido(bloco, index) {
    const laminas = Array.isArray(bloco.laminas) ? bloco.laminas : [];

    return `
        <div class="order-block">
            <b>Bloco ${index + 1}</b> — ${corNome(bloco.corBloco)}
            <ul>
                ${laminas.map((lamina) => `
                    <li>
                        ${POSICAO_NOME[lamina.posicao] ?? 'Posição'}:
                        ${LAMINA_NOME[lamina.corLamina] ?? lamina.corLamina},
                        padrão ${PADRAO_NOME[lamina.padraoLamina] ?? lamina.padraoLamina}
                    </li>
                `).join('')}
            </ul>
        </div>
    `;
}

async function produzirPedido(id) {
    if (!id) return;

    try {
        const pedido = await api.pedidos.produzir(id);
        toast(`Pedido ${pedido?.numeroPedido ?? id} enviado para produção.`, 'success');
        await carregarPedidos();
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function concluirPedido(id) {
    if (!id) return;

    try {
        const pedido = await api.pedidos.concluir(id);
        toast(`Pedido ${pedido?.numeroPedido ?? id} concluído.`, 'success');
        await carregarPedidos();
    } catch (error) {
        toast(error.message, 'error');
    }
}
