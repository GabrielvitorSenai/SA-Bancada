import { $, $$, setHTML, setText } from '../core/dom.js';

const COR_CSS = { 0: '#e2e8f0', 1: '#1f2937', 2: '#dc2626', 3: '#2563eb' };
const LAMINA_CSS = { 0: '#94a3b8', 1: '#dc2626', 2: '#2563eb', 3: '#d97706', 4: '#16a34a', 5: '#111827', 6: '#f8fafc' };
const COR_NOME = { 1: 'Preto', 2: 'Vermelho', 3: 'Azul' };
const TIPO_NOME = { 1: 'Simples', 2: 'Duplo', 3: 'Triplo' };

export function atualizarPreview() {
    const tipo = Number($('#tipoPedido')?.value || 1);
    const corTampa = Number($('#corTampa')?.value || 0);
    const cards = $$('.block-card');

    setText('#previewTipo', TIPO_NOME[tipo] ?? 'Pedido');

    const layers = [];
    const resumo = [];

    if (corTampa > 0) {
        layers.push(`<div class="preview-layer preview-layer--tampa" style="background:${COR_CSS[corTampa]}"></div>`);
        resumo.push(`<b>Tampa:</b> ${COR_NOME[corTampa]}`);
    }

    cards.slice().reverse().forEach((card) => {
        const andar = Number(card.querySelector('.andar')?.value || 0);
        const corBloco = Number(card.querySelector('.corBloco')?.value || 0);
        const laminas = Array.from(card.querySelectorAll('.lamina-row'));

        const laminaHtml = laminas.map((row) => {
            const posicao = Number(row.querySelector('.posicao')?.value || 0);
            const cor = Number(row.querySelector('.corLamina')?.value || 0);
            if (!posicao || !cor) return '';
            return `<span class="preview-lamina pos-${posicao}" style="background:${LAMINA_CSS[cor]}"></span>`;
        }).join('');

        layers.push(`
            <div class="preview-layer" style="background:${COR_CSS[corBloco] ?? '#94a3b8'}">
                ${laminaHtml}
            </div>
        `);

        resumo.push(`<b>Andar ${andar}:</b> bloco ${COR_NOME[corBloco] ?? '-'}`);
    });

    setHTML('#produtoPreview', `<div class="preview-stack">${layers.join('')}</div>`);
    setHTML('#previewResumo', resumo.join('<br>'));
}
