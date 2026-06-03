/* ============================================================
   Dashboard SMART 4.0 — Etapa 3
   Monitoramento + pedidos em cartões + configurador dinâmico.

   Globais (definidos no preview offline):
     window.API_BASE   -> base REST   (padrão '')
     window.ASSET_BASE -> base assets (padrão '/assets')
   ============================================================ */

const API = (window.API_BASE ?? '');
const ASSETS = (window.ASSET_BASE ?? '/assets');

/* ---------- Domínios do enunciado ---------- */
const COR_CSS = { 0: '#e2e8f0', 1: '#1f2937', 2: '#dc2626', 3: '#2563eb' };
const COR_BLOCO_NOME = { 0: 'VAZIO', 1: 'PRETO', 2: 'VERMELHO', 3: 'AZUL' };

// Cor da TAMPA / BLOCO: 1 preto, 2 vermelho, 3 azul.
const TAMPA = {
    0: { nome: '—', css: '#94a3b8' },
    1: { nome: 'PRETO', css: '#1f2937' },
    2: { nome: 'VERMELHO', css: '#dc2626' },
    3: { nome: 'AZUL', css: '#2563eb' }
};
// Cor da LÂMINA: 1 vermelho, 2 azul, 3 amarelo, 4 verde, 5 preto, 6 branco.
const LAMINA = {
    0: { nome: 'NENHUM', css: '#94a3b8' },
    1: { nome: 'VERMELHO', css: '#dc2626' },
    2: { nome: 'AZUL', css: '#2563eb' },
    3: { nome: 'AMARELO', css: '#d97706' },
    4: { nome: 'VERDE', css: '#16a34a' },
    5: { nome: 'PRETO', css: '#1f2937' },
    6: { nome: 'BRANCO', css: '#94a3b8' }
};
const PADRAO_NOME = { 0: 'Nenhum', 1: 'Casa', 2: 'Navio', 3: 'Estrela' };
const POSICAO_NOME = { 1: 'Esquerda', 2: 'Frente', 3: 'Direita' };
const TIPO_NOME = { 1: 'simples', 2: 'duplo', 3: 'triplo' };
const STATUS_INFO = {
    1: { label: 'pendente', classe: 'pendente' },
    2: { label: 'em produção', classe: 'producao' },
    3: { label: 'concluído', classe: 'concluido' }
};

const TOTAL_ESTOQUE = 28;
const TOTAL_EXPEDICAO = 12;

let mapaEstoque = [];
let pedidos = [];
let filtroStatus = 0;

/* ---------- Helpers ---------- */
function obterCorCSS(c) { return COR_CSS[c] || '#ffffff'; }
function imgBloco(c) { return `${ASSETS}/bloco/rBlocoCor${c ?? 0}.png`; }
function imgTampa(c) { return `${ASSETS}/bloco/rTampa${c ?? 0}.png`; }
function imgLamina(p, c) { return `${ASSETS}/laminas/lamina${p}-${c ?? 0}.png`; }
function imgPadrao(pad, pos) { return `${ASSETS}/padroes/rpadrao${pad}-${pos === 2 ? 2 : 1}.png`; }
function tag(nome, css) { return `<span class="cor-tag" style="color:${css}">${nome}</span>`; }

/* ============================================================ */
document.addEventListener('DOMContentLoaded', () => {
    montarBlocos();
    bindEventos();
    atualizarTudo();
    setInterval(carregarMonitoramento, 8000);
});

function bindEventos() {
    document.querySelector('#tipoPedido').addEventListener('change', montarBlocos);
    document.querySelector('#corTampa').addEventListener('change', atualizarPreview);
    document.querySelector('#numeroPedido').addEventListener('input', validarFormulario);
    document.querySelector('#btnLimpar').addEventListener('click', () => limparFormulario(true));
    document.querySelector('#btnAtualizar').addEventListener('click', atualizarTudo);
    document.querySelector('#producaoForm').addEventListener('submit', salvarPedido);
    document.querySelector('#blocoPreviewSel').addEventListener('change', atualizarPreview);

    document.querySelectorAll('.chip').forEach(chip => {
        chip.addEventListener('click', () => {
            document.querySelectorAll('.chip').forEach(c => c.classList.remove('active'));
            chip.classList.add('active');
            filtroStatus = Number(chip.dataset.status);
            renderizarPedidos();
        });
    });
}

async function atualizarTudo() { await Promise.all([carregarMonitoramento(), carregarPedidos()]); }
async function carregarMonitoramento() { await Promise.all([carregarEstoque(), carregarExpedicao()]); validarFormulario(); }

/* ============================================================
   ESTOQUE (28)
   ============================================================ */
async function carregarEstoque() {
    try {
        const resp = await fetch(`${API}/api/estoque/posicoes`);
        if (!resp.ok) throw 0;
        mapaEstoque = await resp.json();
    } catch (e) {
        mapaEstoque = Array.from({ length: TOTAL_ESTOQUE }, (_, i) => ({ posicao: i + 1, cor: 0, idItem: null }));
    }
    const grid = document.querySelector('#estoqueGrid');
    grid.innerHTML = mapaEstoque.map(p => `
        <div class="cell cor-${p.cor ?? 0}" title="Posição ${p.posicao} — ${COR_BLOCO_NOME[p.cor ?? 0]}">
            <span class="id-pos">#${p.posicao}</span>
            <span class="tag-cor">${COR_BLOCO_NOME[p.cor ?? 0]}</span>
        </div>`).join('');
    const ocupadas = mapaEstoque.filter(p => (p.cor ?? 0) !== 0).length;
    document.querySelector('#estoqueOcupado').textContent = `${ocupadas}/${TOTAL_ESTOQUE}`;
}

/* ============================================================
   EXPEDIÇÃO (12)
   ============================================================ */
async function carregarExpedicao() {
    let registros = [];
    try { const r = await fetch(`${API}/api/expedicao`); if (r.ok) registros = await r.json(); } catch (e) {}
    const porPosicao = {};
    registros.forEach(r => { const p = r.posicaoExpedicao; if (p >= 1 && p <= TOTAL_EXPEDICAO) porPosicao[p] = r; });

    let html = '';
    for (let pos = 1; pos <= TOTAL_EXPEDICAO; pos++) {
        const r = porPosicao[pos];
        html += r
            ? `<div class="exp-cell ocupada"><span class="exp-pos">Pos ${pos}</span><strong>#${r.pedidoId}</strong><small>aguardando retirada</small></div>`
            : `<div class="exp-cell"><span class="exp-pos">Pos ${pos}</span><small>livre</small></div>`;
    }
    document.querySelector('#expedicaoGrid').innerHTML = html;
    document.querySelector('#expedicaoOcupadas').textContent = Object.keys(porPosicao).length;
}

/* ============================================================
   PEDIDOS — cartões
   ============================================================ */
async function carregarPedidos() {
    try { const r = await fetch(`${API}/api/pedidos`); pedidos = r.ok ? await r.json() : []; if (!Array.isArray(pedidos)) pedidos = []; }
    catch (e) { pedidos = []; }
    renderizarPedidos();
    document.querySelector('#totalPedidos').textContent = pedidos.length;
    document.querySelector('#pedidosPendentes').textContent = pedidos.filter(p => p.status === 1).length;
}

function renderizarPedidos() {
    const grid = document.querySelector('#pedidosGrid');
    const lista = filtroStatus === 0 ? pedidos : pedidos.filter(p => p.status === filtroStatus);
    if (!lista.length) { grid.innerHTML = `<div class="empty">Nenhum pedido neste filtro.</div>`; return; }
    grid.innerHTML = lista.map(cardPedido).join('');
}

function cardPedido(p) {
    const info = STATUS_INFO[p.status] || { label: `status ${p.status}`, classe: 'pendente' };
    const tampa = TAMPA[p.corTampa] || { nome: p.corTampa, css: '#334155' };

    // Botão de ação conforme o status.
    let acao = '';
    if (p.status === 1) {
        acao = `<button class="btn btn-primary btn-block" onclick="enviarParaProducao(${p.idPedido})">Enviar para produção</button>`;
    } else if (p.status === 2) {
        acao = `<button class="btn btn-success btn-block" onclick="concluirPedido(${p.idPedido})">Concluir e expedir</button>`;
    }

    // Blocos + lâminas (color-coded).
    const blocos = (p.blocos || []).map((b, i) => {
        const corB = TAMPA[b.corBloco] || { nome: b.corBloco, css: '#334155' };
        const laminas = (b.laminas || [])
            .sort((a, c) => (a.posicao || 0) - (c.posicao || 0))
            .map((l, j) => {
                const cl = LAMINA[l.corLamina] || { nome: l.corLamina, css: '#334155' };
                return `<li>Lâmina ${l.posicao || j + 1} - Cor: ${tag(cl.nome, cl.css)}, Padrão: ${l.padraoLamina ?? 0}</li>`;
            }).join('');
        return `
            <div class="pc-bloco">
                <p class="pc-bloco-h">Bloco ${i + 1} - Cor: ${tag(corB.nome, corB.css)}</p>
                <ul class="pc-laminas">${laminas}</ul>
            </div>`;
    }).join('');

    return `
        <article class="pedido-card">
            <h3 class="pc-title">Pedido: ${p.idPedido ?? '-'}</h3>
            <p class="pc-line"><b>Tipo:</b> ${TIPO_NOME[p.tipoPedido] || '-'}</p>
            <p class="pc-line"><b>Nº ordem:</b> ${p.numeroPedido ?? '-'}</p>
            <p class="pc-line"><b>Cor da tampa:</b> ${tag(tampa.nome, tampa.css)}</p>
            <p class="pc-line"><b>Status:</b> <span class="pc-status ${info.classe}">${info.label}</span></p>
            ${acao}
            ${blocos}
        </article>`;
}

async function enviarParaProducao(id) {
    if (!id) return;
    try {
        const r = await fetch(`${API}/api/pedidos/${id}/produzir`, { method: 'PUT' });
        const d = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(d.erro || 'Não foi possível enviar para produção.');
        toast(`Pedido ${d.numeroPedido || id} enviado para a fila de produção!`, 'success');
        atualizarTudo();
    } catch (e) { toast(e.message, 'error'); }
}
window.enviarParaProducao = enviarParaProducao;

async function concluirPedido(id) {
    if (!id) return;
    try {
        const r = await fetch(`${API}/api/pedidos/${id}/status`, { method: 'PUT' });
        const d = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(d.erro || 'Não foi possível concluir o pedido.');
        toast(`Pedido ${d.numeroPedido || id} concluído e enviado à expedição.`, 'success');
        atualizarTudo();
    } catch (e) { toast(e.message, 'error'); }
}
window.concluirPedido = concluirPedido;

/* ============================================================
   CONFIGURADOR
   ============================================================ */
function montarBlocos() {
    const qtd = Number(document.querySelector('#tipoPedido').value);
    const container = document.querySelector('#blocosContainer');
    const tpl = document.querySelector('#blocoTemplate');
    container.innerHTML = '';
    for (let i = 1; i <= qtd; i++) {
        const frag = tpl.content.cloneNode(true);
        const card = frag.querySelector('.bloco-card');
        card.dataset.indice = i;
        card.querySelector('h4').textContent = `Bloco ${i}`;
        card.querySelector('.andar').value = i;
        const lista = card.querySelector('.laminas-list');
        card.querySelector('.adicionar-lamina').addEventListener('click', () => adicionarLamina(lista));
        card.querySelector('.corBloco').addEventListener('change', () => { atualizarPreview(); validarFormulario(); });
        adicionarLamina(lista);
        container.appendChild(frag);
    }
    atualizarSeletorPreview(qtd);
    atualizarPreview();
    validarFormulario();
}

function atualizarSeletorPreview(qtd) {
    const sel = document.querySelector('#blocoPreviewSel');
    sel.innerHTML = '';
    for (let i = 1; i <= qtd; i++) { const o = document.createElement('option'); o.value = i; o.textContent = `Bloco ${i}`; sel.appendChild(o); }
    document.querySelector('#blocoPreviewWrap').classList.toggle('hidden', qtd < 2);
}

function adicionarLamina(lista) {
    if (lista.querySelectorAll('.lamina-row').length >= 3) { toast('Cada bloco pode ter no máximo 3 lâminas.', 'error'); return; }
    const tpl = document.querySelector('#laminaTemplate');
    const frag = tpl.content.cloneNode(true);
    const row = frag.querySelector('.lamina-row');
    row.querySelector('.posicao').value = lista.querySelectorAll('.lamina-row').length + 1;
    row.querySelectorAll('select, input').forEach(el => el.addEventListener('change', atualizarPreview));
    row.querySelector('.remover-lamina').addEventListener('click', () => {
        if (lista.querySelectorAll('.lamina-row').length === 1) { toast('Cada bloco precisa de pelo menos 1 lâmina.', 'error'); return; }
        row.remove(); atualizarPreview();
    });
    lista.appendChild(frag);
    atualizarPreview();
}

function atualizarPreview() {
    const corTampa = Number(document.querySelector('#corTampa').value || 0);
    document.querySelector('#tampaSwatch').style.background = obterCorCSS(corTampa);
    document.querySelector('#tampaNome').textContent = TAMPA[corTampa].nome;

    const idx = Number(document.querySelector('#blocoPreviewSel').value || 1);
    const card = document.querySelector(`.bloco-card[data-indice="${idx}"]`) || document.querySelector('.bloco-card');
    const stage = document.querySelector('#produtoStage');
    stage.innerHTML = '';
    if (!card) return;

    const corBloco = Number(card.querySelector('.corBloco').value || 0);
    stage.appendChild(camada(imgBloco(corBloco), 1));

    const laminas = [];
    card.querySelectorAll('.lamina-row').forEach(row => {
        const pos = Number(row.querySelector('.posicao').value || 0);
        const cor = Number(row.querySelector('.corLamina').value || 0);
        const pad = Number(row.querySelector('.padraoLamina').value || 0);
        if (pos >= 1 && pos <= 3 && cor > 0) {
            stage.appendChild(camada(imgLamina(pos, cor), 2 + pos));
            if (pad > 0) stage.appendChild(camada(imgPadrao(pad, pos), 6 + pos));
        }
        laminas.push({ pos, cor, pad });
    });
    if (corTampa > 0) stage.appendChild(camada(imgTampa(corTampa), 20));

    const desc = laminas.filter(l => l.cor > 0).map(l => `${POSICAO_NOME[l.pos]}: ${LAMINA[l.cor].nome}${l.pad ? ' · ' + PADRAO_NOME[l.pad] : ''}`);
    document.querySelector('#previewMeta').innerHTML = `
        <div>Bloco: <b>${TAMPA[corBloco].nome}</b></div>
        <div>Tampa: <b>${TAMPA[corTampa].nome}</b></div>
        <div>Lâminas: <b>${desc.length ? desc.join(' | ') : 'nenhuma'}</b></div>`;
}

function camada(src, z) {
    const img = document.createElement('img');
    img.src = src; img.style.zIndex = z;
    img.onerror = () => { img.style.display = 'none'; };
    return img;
}

function validarFormulario() {
    const numero = document.querySelector('#numeroPedido').value.trim();
    const btn = document.querySelector('#btnSalvar');
    let valido = numero !== '';

    const necessario = {};
    document.querySelectorAll('.bloco-card .corBloco').forEach(inp => {
        const cor = Number(inp.value || 0);
        if (cor > 0) necessario[cor] = (necessario[cor] || 0) + 1;
    });
    const disponivel = {};
    mapaEstoque.forEach(p => { const c = p.cor ?? 0; if (c > 0) disponivel[c] = (disponivel[c] || 0) + 1; });

    const faltam = [];
    Object.entries(necessario).forEach(([cor, qtd]) => { if ((disponivel[cor] || 0) < qtd) faltam.push(COR_BLOCO_NOME[cor]); });

    const aviso = document.querySelector('#avisoEstoque');
    if (faltam.length) { aviso.textContent = `⚠️ Estoque indisponível para: ${faltam.join(', ')}.`; aviso.classList.remove('hidden'); valido = false; }
    else aviso.classList.add('hidden');

    btn.disabled = !valido;
}

async function salvarPedido(event) {
    event.preventDefault();
    const form = document.querySelector('#producaoForm');
    if (!form.checkValidity()) { form.reportValidity(); return; }
    try {
        const resp = await fetch(`${API}/api/pedidos`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(montarPayload())
        });
        const d = await resp.json().catch(() => ({}));
        if (!resp.ok) throw new Error(d.erro || d.message || 'Não foi possível salvar o pedido.');
        toast('Pedido cadastrado com sucesso e enviado para a fila de produção!', 'success');
        limparFormulario(false);
        atualizarTudo();
    } catch (e) { toast(e.message, 'error'); }
}

function num(v) { return Number.parseInt(v, 10); }
function montarPayload() {
    const blocos = [...document.querySelectorAll('.bloco-card')].map(card => ({
        andar: num(card.querySelector('.andar').value),
        corBloco: num(card.querySelector('.corBloco').value),
        laminas: [...card.querySelectorAll('.lamina-row')].map(row => ({
            posicao: num(row.querySelector('.posicao').value),
            corLamina: num(row.querySelector('.corLamina').value),
            padraoLamina: num(row.querySelector('.padraoLamina').value)
        }))
    }));
    return {
        numeroPedido: num(document.querySelector('#numeroPedido').value),
        tipoPedido: num(document.querySelector('#tipoPedido').value),
        corTampa: num(document.querySelector('#corTampa').value),
        status: 1,
        posicaoExpedicao: num(document.querySelector('#posicaoExpedicao').value),
        blocos
    };
}

function limparFormulario(mostrar) {
    document.querySelector('#producaoForm').reset();
    document.querySelector('#tipoPedido').value = '1';
    montarBlocos();
    if (mostrar) toast('Formulário limpo.', 'success');
}

let toastTimer;
function toast(msg, tipo) {
    const el = document.querySelector('#toast');
    el.textContent = msg; el.className = `toast ${tipo} show`;
    clearTimeout(toastTimer); toastTimer = setTimeout(() => el.classList.remove('show'), 4200);
}
