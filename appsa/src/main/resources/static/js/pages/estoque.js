/**
 * Tela de gerenciamento do estoque.
 * Conversa com:
 * GET    /api/estoque/posicoes
 * POST   /api/estoque
 * DELETE /api/estoque/posicao/{posicao}
 */
document.addEventListener('DOMContentLoaded', () => {
    const grid = document.querySelector('#estoqueGrid');
    const selectPos = document.querySelector('#estoquePosicao');
    const selectCor = document.querySelector('#estoqueCor');
    const inputQtd = document.querySelector('#estoqueQuantidade');
    const btnAdicionar = document.querySelector('#btnAdicionarEstoque');
    const btnRemover = document.querySelector('#btnRemoverEstoque');
    const btnReload = document.querySelector('#btnReloadEstoque');

    let posicaoSelecionada = null;
    let mapaAtual = [];

    preencherSelectPosicoes();
    carregarEstoque();

    btnReload.addEventListener('click', carregarEstoque);
    btnAdicionar.addEventListener('click', adicionar);
    btnRemover.addEventListener('click', remover);
    selectPos.addEventListener('change', () => selecionar(Number(selectPos.value)));

    async function carregarEstoque() {
        try {
            mapaAtual = await Smart40.API.get('/api/estoque/posicoes');
            renderizarMapa();
            atualizarMetricas();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao carregar estoque.', 'error');
            grid.innerHTML = `<div class="empty">Erro ao carregar estoque.</div>`;
        }
    }

    function renderizarMapa() {
        grid.innerHTML = mapaAtual.map(item => {
            const pos = Number(item.posicao);
            const cor = Number(item.cor || 0);
            const livre = Boolean(item.disponivel) || cor === 0;
            const selected = posicaoSelecionada === pos ? 'selected' : '';

            return `
                <button class="cell cor-${cor} ${selected}" type="button" data-pos="${pos}">
                    <span class="id-pos">${pos}</span>
                    <span class="tag-cor">${livre ? 'Livre' : Smart40.UI.colorName(cor)}</span>
                </button>
            `;
        }).join('');

        grid.querySelectorAll('[data-pos]').forEach(cell => {
            cell.addEventListener('click', () => selecionar(Number(cell.dataset.pos)));
        });
    }

    function selecionar(posicao) {
        posicaoSelecionada = posicao;
        selectPos.value = String(posicao);

        const item = mapaAtual.find(p => Number(p.posicao) === posicao);
        if (item && Number(item.cor || 0) > 0) {
            selectCor.value = String(item.cor);
        }

        renderizarMapa();
    }

    async function adicionar() {
        try {
            const posicao = Number(selectPos.value);
            const cor = Number(selectCor.value);
            const quantidade = Number(inputQtd.value || 1);

            if (!posicao) throw new Error('Selecione uma posição.');
            if (!cor) throw new Error('Selecione uma cor.');

            await Smart40.API.post('/api/estoque', { posicao, cor, quantidade });

            Smart40.UI.toast(`Posição ${posicao} atualizada.`, 'success');
            posicaoSelecionada = posicao;
            await carregarEstoque();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao adicionar estoque.', 'error');
        }
    }

    async function remover() {
        try {
            const posicao = Number(selectPos.value || posicaoSelecionada);
            if (!posicao) throw new Error('Selecione uma posição.');

            if (!Smart40.UI.confirm(`Liberar a posição ${posicao} do estoque?`)) return;

            await Smart40.API.delete(`/api/estoque/posicao/${posicao}`);

            Smart40.UI.toast(`Posição ${posicao} liberada.`, 'success');
            posicaoSelecionada = null;
            await carregarEstoque();
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao remover estoque.', 'error');
        }
    }

    function preencherSelectPosicoes() {
        selectPos.innerHTML = '<option value="">Selecione</option>' + Array.from({ length: 28 }, (_, i) => {
            const pos = i + 1;
            return `<option value="${pos}">Posição ${pos}</option>`;
        }).join('');
    }

    function atualizarMetricas() {
        const ocupadas = mapaAtual.filter(p => Number(p.cor || 0) > 0 && !p.disponivel).length;
        Smart40.UI.setText('#metricEstoqueOcupado', ocupadas);
        Smart40.UI.setText('#metricEstoqueLivre', 28 - ocupadas);
        Smart40.UI.setText('#metricEstoqueAtualizado', Smart40.UI.now());
    }
});
