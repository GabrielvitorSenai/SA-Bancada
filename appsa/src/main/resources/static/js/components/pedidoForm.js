import { $, $$ } from '../core/dom.js';
import { toast } from '../core/toast.js';
import { state } from '../core/state.js';
import { api } from '../services/api.js';
import { carregarPedidos } from './pedidos.js';
import { carregarEstoque } from './estoque.js';
import { atualizarPreview } from './preview.js';

const COR_NOME = { 1: 'Preto', 2: 'Vermelho', 3: 'Azul' };

export function initPedidoForm() {
    montarBlocos();
    bindPedidoFormEvents();
    atualizarPreview();
    validarEstoqueDoPedido();
}

function bindPedidoFormEvents() {
    $('#tipoPedido')?.addEventListener('change', () => {
        montarBlocos();
        validarEstoqueDoPedido();
    });

    $('#corTampa')?.addEventListener('change', atualizarPreview);
    $('#btnClearOrder')?.addEventListener('click', limparFormulario);
    $('#pedidoForm')?.addEventListener('submit', salvarPedido);

    document.addEventListener('input', (event) => {
        if (event.target.closest('#pedidoForm')) {
            atualizarPreview();
            validarEstoqueDoPedido();
        }
    });

    document.addEventListener('change', (event) => {
        if (event.target.closest('#pedidoForm')) {
            atualizarPreview();
            validarEstoqueDoPedido();
        }
    });
}

function montarBlocos() {
    const quantidade = Number($('#tipoPedido')?.value || 1);
    const container = $('#blocosContainer');
    const template = $('#blocoTemplate');
    if (!container || !template) return;

    container.innerHTML = '';

    for (let i = 1; i <= quantidade; i++) {
        const fragment = template.content.cloneNode(true);
        const card = fragment.querySelector('.block-card');
        const lista = fragment.querySelector('.laminas-list');

        card.querySelector('h4').textContent = `Bloco ${i}`;
        card.querySelector('.andar').value = i;
        card.querySelector('.add-lamina').addEventListener('click', () => adicionarLamina(lista));

        adicionarLamina(lista);
        container.appendChild(fragment);
    }

    atualizarPreview();
}

function adicionarLamina(lista) {
    if (!lista) return;
    if (lista.querySelectorAll('.lamina-row').length >= 3) {
        toast('Cada bloco pode ter no máximo 3 lâminas.', 'error');
        return;
    }

    const template = $('#laminaTemplate');
    const fragment = template.content.cloneNode(true);
    const row = fragment.querySelector('.lamina-row');

    row.querySelector('.posicao').value = lista.querySelectorAll('.lamina-row').length + 1;
    row.querySelector('.btn-remove-lamina').addEventListener('click', () => {
        if (lista.querySelectorAll('.lamina-row').length <= 1) {
            toast('Cada bloco precisa de pelo menos uma lâmina.', 'error');
            return;
        }
        row.remove();
        atualizarPreview();
        validarEstoqueDoPedido();
    });

    lista.appendChild(fragment);
    atualizarPreview();
}

async function salvarPedido(event) {
    event.preventDefault();

    const form = $('#pedidoForm');
    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    if (!validarEstoqueDoPedido()) {
        toast('Corrija o estoque antes de salvar o pedido.', 'error');
        return;
    }

    try {
        const pedido = await api.pedidos.criar(montarPayload());
        toast(`Pedido ${pedido?.numeroPedido ?? ''} salvo com sucesso.`, 'success');
        limparFormulario(false);
        await Promise.all([carregarPedidos(), carregarEstoque()]);
    } catch (error) {
        toast(error.message, 'error');
    }
}

function montarPayload() {
    const blocos = $$('.block-card').map((card) => ({
        andar: num(card.querySelector('.andar')?.value),
        corBloco: num(card.querySelector('.corBloco')?.value),
        laminas: Array.from(card.querySelectorAll('.lamina-row')).map((row) => ({
            posicao: num(row.querySelector('.posicao')?.value),
            corLamina: num(row.querySelector('.corLamina')?.value),
            padraoLamina: num(row.querySelector('.padraoLamina')?.value)
        }))
    }));

    return {
        numeroPedido: num($('#numeroPedido')?.value),
        tipoPedido: num($('#tipoPedido')?.value),
        corTampa: num($('#corTampa')?.value),
        status: 1,
        posicaoExpedicao: num($('#posicaoExpedicao')?.value),
        blocos
    };
}

function validarEstoqueDoPedido() {
    const warning = $('#inventoryWarning');
    const btn = $('#btnSaveOrder');
    if (!warning || !btn) return true;

    const necessario = {};
    $$('.block-card .corBloco').forEach((select) => {
        const cor = Number(select.value || 0);
        if (cor > 0) necessario[cor] = (necessario[cor] || 0) + 1;
    });

    const disponivel = {};
    state.estoque.forEach((posicao) => {
        const cor = Number(posicao.cor ?? 0);
        if (cor > 0) disponivel[cor] = (disponivel[cor] || 0) + 1;
    });

    const faltam = Object.entries(necessario)
        .filter(([cor, qtd]) => (disponivel[cor] || 0) < qtd)
        .map(([cor]) => COR_NOME[cor] ?? `Cor ${cor}`);

    const valido = faltam.length === 0;
    warning.classList.toggle('is-hidden', valido);
    warning.textContent = valido ? '' : `Estoque insuficiente para: ${faltam.join(', ')}.`;
    btn.disabled = !valido;

    return valido;
}

function limparFormulario(showToast = true) {
    $('#pedidoForm')?.reset();
    if ($('#tipoPedido')) $('#tipoPedido').value = '1';
    montarBlocos();
    validarEstoqueDoPedido();
    if (showToast) toast('Formulário limpo.', 'success');
}

function num(value) {
    return Number.parseInt(value, 10) || 0;
}
