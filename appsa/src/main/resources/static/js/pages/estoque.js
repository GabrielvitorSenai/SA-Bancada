document.addEventListener('DOMContentLoaded', () => {
    carregarEstoque();

    const form = document.querySelector('#estoqueForm');
    if (form) {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            await adicionarEstoque();
        });
    }
});

async function carregarEstoque() {
    const container = document.querySelector('#estoqueGrid');

    if (!container) {
        return;
    }

    try {
        const posicoes = await api.get('/api/estoque/posicoes');

        container.innerHTML = posicoes.map(item => {
            const ocupado = !item.disponivel && Number(item.cor || 0) > 0;

            return `
                <article class="stock-cell ${ocupado ? 'is-busy' : 'is-free'}">
                    <strong>${item.posicao}</strong>
                    <span>${ocupado ? nomeCor(item.cor) : 'Livre'}</span>
                    ${ocupado ? `
                        <button class="btn btn-danger btn-small" onclick="liberarPosicao(${item.posicao})">
                            Liberar
                        </button>
                    ` : ''}
                </article>
            `;
        }).join('');

    } catch (error) {
        console.error(error);
        container.innerHTML = `
            <div class="empty-state error">
                <strong>Erro ao carregar estoque</strong>
                <span>${error.message}</span>
            </div>
        `;
    }
}

async function adicionarEstoque() {
    try {
        const payload = {
            posicao: Number(document.querySelector('#estoquePosicao')?.value),
            cor: Number(document.querySelector('#estoqueCor')?.value),
            quantidade: Number(document.querySelector('#estoqueQuantidade')?.value || 1)
        };

        await api.post('/api/estoque', payload);

        toastSucesso('Estoque adicionado.');
        await carregarEstoque();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao adicionar estoque.');
    }
}

async function liberarPosicao(posicao) {
    try {
        if (!confirm(`Liberar posição ${posicao}?`)) {
            return;
        }

        await api.delete(`/api/estoque/posicao/${posicao}`);

        toastSucesso(`Posição ${posicao} liberada.`);
        await carregarEstoque();

    } catch (error) {
        console.error(error);
        toastErro(error.message || 'Erro ao liberar posição.');
    }
}

function nomeCor(cor) {
    switch (Number(cor)) {
        case 1: return 'Preto';
        case 2: return 'Vermelho';
        case 3: return 'Azul';
        default: return 'Livre';
    }
}
