import { $, setHTML } from '../core/dom.js';
import { toast } from '../core/toast.js';
import { state } from '../core/state.js';
import { api } from '../services/api.js';
import { renderMetrics } from './metrics.js';

const COR_NOME = { 0: 'Livre', 1: 'Preto', 2: 'Vermelho', 3: 'Azul' };

export async function carregarEstoque() {
    try {
        const data = await api.estoque.listarPosicoes();
        state.estoque = Array.isArray(data) ? data : [];
    } catch (error) {
        console.error(error);
        state.estoque = Array.from({ length: 28 }, (_, index) => ({
            posicao: index + 1,
            cor: 0,
            idItem: null,
            disponivel: true
        }));
        toast('Não foi possível carregar o estoque. Usando visualização vazia.', 'error');
    }

    renderEstoque();
    renderMetrics();
}

export function renderEstoque() {
    renderMapaPrincipal();
    renderGerenciador();
}

function cellTemplate(posicao) {
    const cor = Number(posicao.cor ?? 0);
    const selected = state.posicaoSelecionada === posicao.posicao ? 'is-selected' : '';

    return `
        <button type="button"
                class="stock-cell color-${cor} ${selected}"
                data-stock-position="${posicao.posicao}"
                title="Posição ${posicao.posicao} — ${COR_NOME[cor] ?? cor}">
            <span class="stock-cell__pos">#${posicao.posicao}</span>
            <span class="stock-cell__label">${COR_NOME[cor] ?? cor}</span>
        </button>
    `;
}

function renderMapaPrincipal() {
    setHTML('#estoqueGrid', state.estoque.map(cellTemplate).join(''));
}

function renderGerenciador() {
    setHTML('#estoqueManagerGrid', state.estoque.map(cellTemplate).join(''));

    const select = $('#stockPosition');
    if (!select) return;

    const selected = state.posicaoSelecionada ?? Number(select.value || 0);
    select.innerHTML = `
        <option value="">Selecione uma posição</option>
        ${state.estoque.map((posicao) => {
            const cor = Number(posicao.cor ?? 0);
            return `
                <option value="${posicao.posicao}" ${selected === posicao.posicao ? 'selected' : ''}>
                    Posição ${posicao.posicao} — ${COR_NOME[cor] ?? cor}
                </option>
            `;
        }).join('')}
    `;
}

export function bindEstoqueEvents() {
    document.addEventListener('click', (event) => {
        const cell = event.target.closest('[data-stock-position]');
        if (!cell) return;

        state.posicaoSelecionada = Number(cell.dataset.stockPosition);
        const select = $('#stockPosition');
        if (select) select.value = String(state.posicaoSelecionada);
        renderEstoque();
        document.querySelector('#gerenciarEstoque')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });

    $('#stockPosition')?.addEventListener('change', (event) => {
        state.posicaoSelecionada = Number(event.target.value || 0) || null;
        renderEstoque();
    });

    $('#btnStockAdd')?.addEventListener('click', adicionarEstoque);
    $('#btnStockRemove')?.addEventListener('click', liberarPosicao);
}

async function adicionarEstoque() {
    const posicao = Number($('#stockPosition')?.value || 0);
    const cor = Number($('#stockColor')?.value || 0);
    const quantidade = Number($('#stockQuantity')?.value || 1);

    if (!posicao) return toast('Selecione uma posição.', 'error');
    if (!cor) return toast('Selecione a cor do bloco.', 'error');
    if (quantidade < 1) return toast('Quantidade inválida.', 'error');

    try {
        // Envia os dois formatos para funcionar tanto com Entity Estoque quanto com DTO próprio.
        await api.estoque.adicionar({
            cor,
            quantidade,
            posicao,
            posicaoEstoque: { posicao }
        });

        toast(`Bloco adicionado na posição ${posicao}.`, 'success');
        $('#stockColor').value = '';
        $('#stockQuantity').value = '1';
        await carregarEstoque();
    } catch (error) {
        toast(error.message, 'error');
    }
}

async function liberarPosicao() {
    const posicao = Number($('#stockPosition')?.value || 0);
    if (!posicao) return toast('Selecione uma posição para liberar.', 'error');
    if (!confirm(`Liberar a posição ${posicao}?`)) return;

    try {
        await api.estoque.liberarPosicao(posicao);
        toast(`Posição ${posicao} liberada.`, 'success');
        await carregarEstoque();
    } catch (error) {
        toast(error.message, 'error');
    }
}

export function corNome(cor) {
    return COR_NOME[Number(cor ?? 0)] ?? `Cor ${cor}`;
}
