const form = document.querySelector('#producaoForm');
const tipoPedido = document.querySelector('#tipoPedido');
const blocosContainer = document.querySelector('#blocosContainer');
const blocoTemplate = document.querySelector('#blocoTemplate');
const laminaTemplate = document.querySelector('#laminaTemplate');
const alerta = document.querySelector('#alerta');
const pedidosTabela = document.querySelector('#pedidosTabela');

document.addEventListener('DOMContentLoaded', () => {
    montarBlocos();
    carregarPedidos();
});

tipoPedido.addEventListener('change', montarBlocos);
document.querySelector('#btnLimpar').addEventListener('click', limparFormulario);
document.querySelector('#btnAtualizarPedidos').addEventListener('click', carregarPedidos);
form.addEventListener('submit', salvarPedido);

function montarBlocos() {
    const quantidade = Number(tipoPedido.value);
    blocosContainer.innerHTML = '';

    for (let index = 1; index <= quantidade; index++) {
        const bloco = blocoTemplate.content.cloneNode(true);
        const card = bloco.querySelector('.bloco-card');

        card.dataset.indice = index;
        card.querySelector('h4').textContent = `Bloco ${index}`;
        card.querySelector('.andar').value = index;

        const listaLaminas = card.querySelector('.laminas-list');
        card.querySelector('.adicionar-lamina').addEventListener('click', () => adicionarLamina(listaLaminas));

        adicionarLamina(listaLaminas);
        blocosContainer.appendChild(bloco);
    }
}

function adicionarLamina(listaLaminas) {
    const total = listaLaminas.querySelectorAll('.lamina-row').length;

    if (total >= 3) {
        mostrarAlerta('Cada bloco pode ter no máximo 3 lâminas.', 'error');
        return;
    }

    const lamina = laminaTemplate.content.cloneNode(true);
    const row = lamina.querySelector('.lamina-row');

    row.querySelector('.posicao').value = total + 1;
    row.querySelector('.remover-lamina').addEventListener('click', () => {
        if (listaLaminas.querySelectorAll('.lamina-row').length === 1) {
            mostrarAlerta('Cada bloco precisa ter pelo menos 1 lâmina.', 'error');
            return;
        }
        row.remove();
        reordenarLaminas(listaLaminas);
    });

    listaLaminas.appendChild(lamina);
}

function reordenarLaminas(listaLaminas) {
    listaLaminas.querySelectorAll('.lamina-row').forEach((row, index) => {
        row.querySelector('.posicao').value = index + 1;
    });
}

async function salvarPedido(event) {
    event.preventDefault();

    if (!form.checkValidity()) {
        form.reportValidity();
        return;
    }

    const pedido = montarPayload();

    try {
        const response = await fetch('/api/pedidos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(pedido)
        });

        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.erro || data.message || 'Não foi possível salvar o pedido.');
        }

        mostrarAlerta(`Pedido ${data.numeroPedido || pedido.numeroPedido} salvo com sucesso!`, 'success');
        limparFormulario(false);
        await carregarPedidos();
    } catch (error) {
        mostrarAlerta(error.message, 'error');
    }
}

function montarPayload() {
    const blocos = [...document.querySelectorAll('.bloco-card')].map(card => ({
        andar: numero(card.querySelector('.andar').value),
        corBloco: numero(card.querySelector('.corBloco').value),
        laminas: [...card.querySelectorAll('.lamina-row')].map(row => ({
            posicao: numero(row.querySelector('.posicao').value),
            corLamina: numero(row.querySelector('.corLamina').value),
            padraoLamina: numero(row.querySelector('.padraoLamina').value)
        }))
    }));

    return {
        numeroPedido: numero(document.querySelector('#numeroPedido').value),
        tipoPedido: numero(document.querySelector('#tipoPedido').value),
        corTampa: numero(document.querySelector('#corTampa').value),
        status: numero(document.querySelector('#status').value),
        posicaoExpedicao: numero(document.querySelector('#posicaoExpedicao').value),
        blocos
    };
}

function numero(valor) {
    return Number.parseInt(valor, 10);
}

function limparFormulario(exibirMensagem = true) {
    form.reset();
    tipoPedido.value = '1';
    document.querySelector('#status').value = '1';
    montarBlocos();

    if (exibirMensagem) {
        mostrarAlerta('Formulário limpo.', 'success');
    }
}

async function carregarPedidos() {
    pedidosTabela.innerHTML = '<tr><td colspan="7" class="empty">Carregando pedidos...</td></tr>';

    try {
        const response = await fetch('/api/pedidos');
        const pedidos = await response.json().catch(() => []);

        if (!response.ok) {
            throw new Error(pedidos.erro || 'Não foi possível carregar os pedidos.');
        }

        renderizarPedidos(Array.isArray(pedidos) ? pedidos : []);
        atualizarResumo(Array.isArray(pedidos) ? pedidos : []);
    } catch (error) {
        pedidosTabela.innerHTML = `<tr><td colspan="7" class="empty">${escapeHtml(error.message)}</td></tr>`;
        atualizarResumo([]);
    }
}

function renderizarPedidos(pedidos) {
    if (!pedidos.length) {
        pedidosTabela.innerHTML = '<tr><td colspan="7" class="empty">Nenhum pedido cadastrado ainda.</td></tr>';
        return;
    }

    pedidosTabela.innerHTML = pedidos.map(pedido => `
        <tr>
            <td>${pedido.idPedido ?? '-'}</td>
            <td>${pedido.numeroPedido ?? '-'}</td>
            <td>${descricaoTipo(pedido.tipoPedido)}</td>
            <td>${pedido.corTampa ?? '-'}</td>
            <td>${pedido.blocos?.length ?? 0}</td>
            <td>${badgeStatus(pedido.status)}</td>
            <td>
                ${pedido.status === 3
                    ? '<span class="empty">Finalizado</span>'
                    : `<button type="button" class="btn btn-secondary" onclick="finalizarPedido(${pedido.idPedido})">Finalizar</button>`}
            </td>
        </tr>
    `).join('');
}

async function finalizarPedido(idPedido) {
    if (!idPedido) return;

    try {
        const response = await fetch(`/api/pedidos/${idPedido}/status`, { method: 'PUT' });
        const data = await response.json().catch(() => ({}));

        if (!response.ok) {
            throw new Error(data.erro || 'Não foi possível finalizar o pedido.');
        }

        mostrarAlerta(`Pedido ${data.numeroPedido || idPedido} finalizado.`, 'success');
        await carregarPedidos();
    } catch (error) {
        mostrarAlerta(error.message, 'error');
    }
}

function atualizarResumo(pedidos) {
    document.querySelector('#totalPedidos').textContent = pedidos.length;
    document.querySelector('#pedidosProducao').textContent = pedidos.filter(p => p.status !== 3).length;
    document.querySelector('#pedidosFinalizados').textContent = pedidos.filter(p => p.status === 3).length;
}

function descricaoTipo(tipo) {
    const tipos = {
        1: 'Simples',
        2: 'Duplo',
        3: 'Triplo'
    };
    return tipos[tipo] || tipo || '-';
}

function badgeStatus(status) {
    const mapa = {
        1: ['Aberto', 'aberto'],
        2: ['Em produção', 'producao'],
        3: ['Finalizado', 'finalizado']
    };
    const [texto, classe] = mapa[status] || [`Status ${status ?? '-'}`, 'aberto'];
    return `<span class="status ${classe}">${texto}</span>`;
}

function mostrarAlerta(mensagem, tipo) {
    alerta.textContent = mensagem;
    alerta.className = `alert ${tipo}`;
    alerta.classList.remove('hidden');

    window.clearTimeout(mostrarAlerta.timeout);
    mostrarAlerta.timeout = window.setTimeout(() => {
        alerta.classList.add('hidden');
    }, 5000);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

window.finalizarPedido = finalizarPedido;
