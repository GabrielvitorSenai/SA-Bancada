/**
 * Tela de criação de pedido.
 * Payload enviado para /api/pedidos segue as entidades Pedido, Bloco e Lamina.
 */
document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#pedidoForm');
    const tipoPedido = document.querySelector('#tipoPedido');
    const blocosContainer = document.querySelector('#blocosContainer');
    const btnAddLamina = document.querySelector('#btnAddLamina');
    const preview = document.querySelector('#pedidoPreview');

    tipoPedido.addEventListener('change', renderBlocos);
    btnAddLamina.addEventListener('click', adicionarLaminaAoUltimoBloco);
    form.addEventListener('input', atualizarPreview);
    form.addEventListener('submit', salvarPedido);

    document.querySelector('#numeroPedido').value = gerarNumeroPedido();
    renderBlocos();
    atualizarPreview();

    function renderBlocos() {
        const qtd = Number(tipoPedido.value || 1);
        blocosContainer.innerHTML = '';

        for (let andar = 1; andar <= qtd; andar++) {
            blocosContainer.insertAdjacentHTML('beforeend', blocoTemplate(andar));
        }

        atualizarPreview();
    }

    function blocoTemplate(andar) {
        return `
            <article class="bloco-card" data-bloco="${andar}">
                <div class="bloco-head">
                    <h3>Bloco / Andar ${andar}</h3>
                    <span class="muted">Cor do bloco</span>
                </div>

                <label class="field">
                    <span>Cor do bloco</span>
                    <select data-field="corBloco" data-andar="${andar}">
                        <option value="1">Preto</option>
                        <option value="2">Vermelho</option>
                        <option value="3">Azul</option>
                    </select>
                </label>

                <div class="laminas-list" data-laminas="${andar}">
                    ${laminaTemplate(andar, 1)}
                    ${laminaTemplate(andar, 2)}
                    ${laminaTemplate(andar, 3)}
                </div>
            </article>
        `;
    }

    function laminaTemplate(andar, posicao) {
        const nomes = { 1: 'Esquerda', 2: 'Frente', 3: 'Direita' };

        return `
            <div class="lamina-row" data-lamina-row>
                <label class="field">
                    <span>Posição</span>
                    <select data-field="posicao" data-andar="${andar}">
                        <option value="${posicao}">${nomes[posicao] || posicao}</option>
                    </select>
                </label>

                <label class="field">
                    <span>Cor da lâmina</span>
                    <select data-field="corLamina" data-andar="${andar}">
                        <option value="0">Sem lâmina</option>
                        <option value="1">Preto</option>
                        <option value="2">Vermelho</option>
                        <option value="3">Azul</option>
                    </select>
                </label>

                <label class="field">
                    <span>Padrão</span>
                    <select data-field="padraoLamina" data-andar="${andar}">
                        <option value="0">Sem padrão</option>
                        <option value="1">Casa</option>
                        <option value="2">Navio</option>
                        <option value="3">Estrela</option>
                    </select>
                </label>
            </div>
        `;
    }

    function adicionarLaminaAoUltimoBloco() {
        Smart40.UI.toast('Cada bloco já possui as 3 posições de lâmina disponíveis.', 'success');
    }

    async function salvarPedido(event) {
        event.preventDefault();

        try {
            const payload = montarPayload();
            const pedido = await Smart40.API.post('/api/pedidos', payload);

            Smart40.UI.toast(`Pedido #${pedido.numeroPedido || pedido.idPedido} criado.`, 'success');
            window.location.href = '/ordens';
        } catch (e) {
            Smart40.UI.toast(e.message || 'Erro ao criar pedido.', 'error');
        }
    }

    function montarPayload() {
        const qtdBlocos = Number(tipoPedido.value || 1);

        const pedido = {
            numeroPedido: Number(document.querySelector('#numeroPedido').value),
            tipoPedido: qtdBlocos,
            corTampa: Number(document.querySelector('#corTampa').value),
            posicaoExpedicao: Number(document.querySelector('#posicaoExpedicao').value),
            status: 1,
            blocos: []
        };

        if (!pedido.numeroPedido) throw new Error('Informe o número do pedido.');
        if (!pedido.corTampa) throw new Error('Informe a cor da tampa.');
        if (!pedido.posicaoExpedicao) throw new Error('Informe a posição de expedição.');

        for (let andar = 1; andar <= qtdBlocos; andar++) {
            const card = document.querySelector(`[data-bloco="${andar}"]`);
            const corBloco = Number(card.querySelector('[data-field="corBloco"]').value);
            const laminas = [];

            card.querySelectorAll('[data-lamina-row]').forEach(row => {
                const posicao = Number(row.querySelector('[data-field="posicao"]').value);
                const corLamina = Number(row.querySelector('[data-field="corLamina"]').value);
                const padraoLamina = Number(row.querySelector('[data-field="padraoLamina"]').value);

                laminas.push({ posicao, corLamina, padraoLamina });
            });

            pedido.blocos.push({ andar, corBloco, laminas });
        }

        return pedido;
    }

    function atualizarPreview() {
        const qtd = Number(tipoPedido.value || 1);
        const corTampa = Number(document.querySelector('#corTampa').value || 0);
        const numero = document.querySelector('#numeroPedido').value || '-';
        const posExp = document.querySelector('#posicaoExpedicao').value || '-';

        preview.innerHTML = `
            <div class="preview-meta">
                <strong>Pedido #${numero}</strong><br>
                Tipo: ${qtd} andar(es)<br>
                Tampa: ${Smart40.UI.colorName(corTampa)}<br>
                Posição expedição: ${posExp}
            </div>
        `;
    }

    function gerarNumeroPedido() {
        const d = new Date();
        return Number(`${d.getHours()}${d.getMinutes()}${d.getSeconds()}`.padStart(6, '0'));
    }
});
