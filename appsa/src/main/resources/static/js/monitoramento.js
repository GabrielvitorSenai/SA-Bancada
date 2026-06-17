/* ============================================================
   Monitoramento SMART 4.0
   - Controla o ClpController: ping, start/stop, SSE, readOnly, reset.
   - Gestão de estoque (28 posições): adicionar / remover blocos.
   ============================================================ */

const API = (window.API_BASE ?? '');

/* Estações: chave usada no body do /start-readings e na rota do /smartstream.
   flagByte  = offset do byte de StatusEstacao (ocupado/aguardando/manual/emergencia)
   opByte    = offset do byte de StatusOP (cancel/finish/start) */
const STATIONS = {
    estoque:   { label: 'Estoque',   flagByte: 100, opByte: 98 },
    processo:  { label: 'Processo',  flagByte: 6,   opByte: 4 },
    montagem:  { label: 'Montagem',  flagByte: 6,   opByte: 4 },
    expedicao: { label: 'Expedição', flagByte: 34,  opByte: 32 },
};

const COR_NOME = { 0: 'VAZIO', 1: 'PRETO', 2: 'VERMELHO', 3: 'AZUL' };
const STATUS_LABEL = { 0: 'Aguardando', 1: 'Em operação', 2: 'Concluído' };
const STATUS_CLASS = { 0: 's-idle', 1: 's-run', 2: 's-done' };

const TOTAL_ESTOQUE = 28;

const sources = {};       // EventSource por estação
let online = {};          // resultado do último ping
let streaming = false;

/* ============================================================ */
document.addEventListener('DOMContentLoaded', () => {
    montarEstacoes();
    restaurarIps();
    bindEventos();
    carregarReadOnly();
    carregarEstoque();
});

function bindEventos() {
    document.querySelector('#btnPing').addEventListener('click', pingClps);
    document.querySelector('#btnStart').addEventListener('click', iniciarLeitura);
    document.querySelector('#btnStop').addEventListener('click', pararLeitura);
    document.querySelector('#btnReset').addEventListener('click', zerarStatus);
    document.querySelector('#chkReadOnly').addEventListener('change', toggleReadOnly);
    document.querySelector('#btnRecarregarEstoque').addEventListener('click', carregarEstoque);

    document.querySelectorAll('.ip-grid input').forEach(inp =>
        inp.addEventListener('change', salvarIps));

    // Estoque: delegação de clique nas células
    document.querySelector('#estoqueGrid').addEventListener('click', onCellClick);

    // Modal
    document.querySelector('#modalCancelar').addEventListener('click', fecharModal);
    document.querySelector('#modalConfirmar').addEventListener('click', confirmarAdicao);
    document.querySelector('#modalBackdrop').addEventListener('click', e => {
        if (e.target.id === 'modalBackdrop') fecharModal();
    });
}

/* ---------- IPs (lembrados no navegador) ---------- */
function getIps() {
    return {
        estoque:   document.querySelector('#ip-estoque').value.trim(),
        processo:  document.querySelector('#ip-processo').value.trim(),
        montagem:  document.querySelector('#ip-montagem').value.trim(),
        expedicao: document.querySelector('#ip-expedicao').value.trim(),
    };
}
function ipsPreenchidos(ips) {
    return Object.fromEntries(Object.entries(ips).filter(([, v]) => v));
}
function salvarIps() {
    try { localStorage.setItem('smart40.ips', JSON.stringify(getIps())); } catch (e) {}
}
function restaurarIps() {
    let saved = {};
    try { saved = JSON.parse(localStorage.getItem('smart40.ips') || '{}'); } catch (e) {}
    Object.entries(saved).forEach(([k, v]) => {
        const el = document.querySelector(`#ip-${k}`);
        if (el && v) el.value = v;
    });
}

/* ============================================================
   PING — POST /smart/ping
   ============================================================ */
async function pingClps() {
    const ips = ipsPreenchidos(getIps());
    if (!Object.keys(ips).length) { toast('Informe ao menos um IP.', 'error'); return; }
    try {
        const r = await fetch(`${API}/smart/ping`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(ips)
        });
        if (!r.ok) throw new Error('Falha no ping (HTTP ' + r.status + ')');
        online = await r.json();
        Object.keys(STATIONS).forEach(st => marcarDot(st, online[st]));
        toast('Ping concluído.', 'info');
    } catch (e) { toast(e.message, 'error'); }
}

function marcarDot(station, isOnline) {
    const dot = document.querySelector(`#dot-${station}`);
    if (!dot) return;
    dot.classList.remove('online', 'offline');
    if (isOnline === true) dot.classList.add('online');
    else if (isOnline === false) dot.classList.add('offline');
}

/* ============================================================
   START / STOP — POST /start-readings , /stop-readings
   ============================================================ */
async function iniciarLeitura() {
    const ips = ipsPreenchidos(getIps());
    if (!Object.keys(ips).length) { toast('Informe ao menos um IP.', 'error'); return; }

    // 1) ping primeiro: só abrimos SSE de estações online (evita NPE no servidor)
    try {
        const rp = await fetch(`${API}/smart/ping`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(ips)
        });
        online = rp.ok ? await rp.json() : {};
    } catch (e) { online = {}; }
    Object.keys(STATIONS).forEach(st => marcarDot(st, online[st]));

    const onlineIps = Object.fromEntries(Object.entries(ips).filter(([k]) => online[k]));
    if (!Object.keys(onlineIps).length) {
        toast('Nenhum CLP respondeu. Verifique os IPs e a rede.', 'error');
        return;
    }

    // 2) inicia as threads de leitura no backend
    try {
        const r = await fetch(`${API}/start-readings`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(onlineIps)
        });
        if (!r.ok) throw new Error('Falha ao iniciar (HTTP ' + r.status + ')');
    } catch (e) { toast(e.message, 'error'); return; }

    // 3) abre o SSE só das estações online
    Object.keys(onlineIps).forEach(abrirStream);

    streaming = true;
    document.querySelector('#btnStart').disabled = true;
    document.querySelector('#btnStop').disabled = false;
    setStreamPill(true);
    toast('Leitura iniciada.', 'success');
}

async function pararLeitura() {
    Object.keys(sources).forEach(fecharStream);
    try {
        await fetch(`${API}/stop-readings`, { method: 'POST' });
    } catch (e) { /* segue mesmo assim */ }
    streaming = false;
    document.querySelector('#btnStart').disabled = false;
    document.querySelector('#btnStop').disabled = true;
    setStreamPill(false);
    toast('Leitura interrompida.', 'info');
}

function setStreamPill(on) {
    const pill = document.querySelector('#streamPill');
    pill.textContent = on ? 'Streams ativos' : 'Streams parados';
    pill.classList.toggle('pill-on', on);
    pill.classList.toggle('pill-off', !on);
}

/* ============================================================
   SSE — GET /smartstream/{estacao}
   ============================================================ */
function abrirStream(station) {
    fecharStream(station);
    const es = new EventSource(`${API}/smartstream/${station}`);
    es.addEventListener('leitura', ev => onFrame(station, ev.data));
    es.onerror = () => { marcarDot(station, false); };
    sources[station] = es;
}

function fecharStream(station) {
    if (sources[station]) { sources[station].close(); delete sources[station]; }
}

function onFrame(station, hex) {
    const bytes = hexToBytes(hex);
    if (!bytes.length) return;

    marcarDot(station, true);

    // O stream 'estoque' carrega, nos 6 últimos bytes, o status global de produção.
    if (station === 'estoque' && bytes.length >= 6) {
        const tail = bytes.slice(bytes.length - 6);
        atualizarStatusGlobal(tail);
    }

    atualizarEstacao(station, bytes);
}

/* 6 bytes finais do stream estoque:
   [statusEstoque, statusProcesso, statusMontagem, statusExpedicao, statusProducao, pedidoEmCurso] */
function atualizarStatusGlobal(t) {
    setStatusCard('g-estoque',   STATUS_LABEL[t[0]] ?? t[0], STATUS_CLASS[t[0]]);
    setStatusCard('g-processo',  STATUS_LABEL[t[1]] ?? t[1], STATUS_CLASS[t[1]]);
    setStatusCard('g-montagem',  STATUS_LABEL[t[2]] ?? t[2], STATUS_CLASS[t[2]]);
    setStatusCard('g-expedicao', STATUS_LABEL[t[3]] ?? t[3], STATUS_CLASS[t[3]]);
    setStatusCard('g-producao',  t[4] === 1 ? 'Finalizada' : 'Em andamento', t[4] === 1 ? 's-done' : 's-idle');
    setStatusCard('g-pedido',    t[5] === 1 ? 'SIM' : 'não', t[5] === 1 ? 's-on' : 's-idle');
}

function setStatusCard(id, texto, classe) {
    const card = document.querySelector('#' + id);
    if (!card) return;
    card.querySelector('strong').textContent = texto;
    card.classList.remove('s-idle', 's-run', 's-done', 's-on');
    if (classe) card.classList.add(classe);
}

/* Detalhe por estação: flags de StatusEstacao + StatusOP + frame bruto */
function atualizarEstacao(station, bytes) {
    const cfg = STATIONS[station];
    const fb = bytes[cfg.flagByte] ?? 0;   // ocupado/aguardando/manual/emergencia
    const ob = bytes[cfg.opByte] ?? 0;     // cancel(0x01)/finish(0x02)/start(0x04)

    setFlag(station, 'ocupado',    (fb & 0x01) !== 0);
    setFlag(station, 'aguardando', (fb & 0x02) !== 0);
    setFlag(station, 'manual',     (fb & 0x04) !== 0);
    setFlag(station, 'emergencia', (fb & 0x08) !== 0);
    setFlag(station, 'start',      (ob & 0x04) !== 0);
    setFlag(station, 'finish',     (ob & 0x02) !== 0);

    const raw = document.querySelector(`#raw-${station}`);
    if (raw) raw.textContent = bytes.slice(0, 48).map(b => b.toString(16).padStart(2, '0').toUpperCase()).join(' ')
        + (bytes.length > 48 ? ' …' : '');

    const meta = document.querySelector(`#meta-${station}`);
    if (meta) meta.textContent = `${bytes.length} bytes · ${new Date().toLocaleTimeString()}`;
}

function setFlag(station, nome, ativo) {
    const el = document.querySelector(`#flag-${station}-${nome}`);
    if (el) el.classList.toggle('on', ativo);
}

/* Monta os cartões de estação no DOM */
function montarEstacoes() {
    const grid = document.querySelector('#estacoesGrid');
    grid.innerHTML = Object.entries(STATIONS).map(([key, cfg]) => `
        <div class="estacao-card">
            <div class="estacao-head">
                <h4>${cfg.label}</h4>
                <span class="dot" id="dot-${key}" title="Conexão"></span>
            </div>
            <div class="flags">
                <span class="flag" id="flag-${key}-ocupado">Ocupado</span>
                <span class="flag" id="flag-${key}-aguardando">Aguardando</span>
                <span class="flag" id="flag-${key}-manual">Manual</span>
                <span class="flag alarme" id="flag-${key}-emergencia">Emergência</span>
                <span class="flag" id="flag-${key}-start">Start OP</span>
                <span class="flag" id="flag-${key}-finish">Finish OP</span>
            </div>
            <div class="raw" id="raw-${key}">aguardando leitura…</div>
            <div class="estacao-meta" id="meta-${key}">—</div>
        </div>`).join('');
}

/* ============================================================
   READ-ONLY — GET/POST /smart/readonly
   ============================================================ */
async function carregarReadOnly() {
    try {
        const r = await fetch(`${API}/smart/readonly`);
        if (r.ok) document.querySelector('#chkReadOnly').checked = (await r.json()) === true;
    } catch (e) { /* silencioso */ }
}
async function toggleReadOnly(e) {
    const value = e.target.checked;
    try {
        const r = await fetch(`${API}/smart/readonly?value=${value}`, { method: 'POST' });
        if (!r.ok) throw new Error('Falha ao alterar modo.');
        toast(`Modo somente-leitura: ${value ? 'ligado' : 'desligado'}.`, 'info');
    } catch (err) { toast(err.message, 'error'); e.target.checked = !value; }
}

/* ============================================================
   RESET STATUS — POST /smart/reset-status
   ============================================================ */
async function zerarStatus() {
    try {
        const r = await fetch(`${API}/smart/reset-status`, { method: 'POST' });
        if (!r.ok) throw new Error('Falha ao zerar status.');
        ['estoque', 'processo', 'montagem', 'expedicao'].forEach(s =>
            setStatusCard('g-' + s, STATUS_LABEL[0], STATUS_CLASS[0]));
        toast('Status zerados.', 'success');
    } catch (e) { toast(e.message, 'error'); }
}

/* ============================================================
   GESTÃO DE ESTOQUE — /api/estoque/...
   ============================================================ */
async function carregarEstoque() {
    let mapa = [];
    try {
        const r = await fetch(`${API}/api/estoque/posicoes`);
        if (r.ok) mapa = await r.json();
    } catch (e) { /* renderiza vazio */ }

    if (!Array.isArray(mapa) || !mapa.length) {
        mapa = Array.from({ length: TOTAL_ESTOQUE }, (_, i) => ({ posicao: i + 1, cor: 0, idItem: null }));
    }
    mapa.sort((a, b) => (a.posicao || 0) - (b.posicao || 0));

    const grid = document.querySelector('#estoqueGrid');
    grid.innerHTML = mapa.map(p => {
        const cor = p.cor ?? 0;
        const ocupada = cor > 0;
        return `
        <div class="cell cor-${cor}" data-pos="${p.posicao}" data-cor="${cor}"
             title="Posição ${p.posicao} — ${COR_NOME[cor] || cor}">
            <span class="id-pos">#${p.posicao}</span>
            <span class="tag-cor">${COR_NOME[cor] || cor}</span>
            <span class="acao">${ocupada ? '🗑' : '＋'}</span>
        </div>`;
    }).join('');

    const ocupadas = mapa.filter(p => (p.cor ?? 0) > 0).length;
    document.querySelector('#estoqueContador').textContent = `${ocupadas}/${TOTAL_ESTOQUE} ocupadas`;
}

function onCellClick(e) {
    const cell = e.target.closest('.cell');
    if (!cell) return;
    const pos = Number(cell.dataset.pos);
    const cor = Number(cell.dataset.cor);
    if (cor > 0) removerBloco(pos, cor);
    else abrirModal(pos);
}

/* ----- Adicionar: POST /api/estoque ----- */
let posicaoAtual = null;

function abrirModal(pos) {
    posicaoAtual = pos;
    document.querySelector('#modalTitulo').textContent = `Adicionar bloco — Posição ${pos}`;
    document.querySelector('#modalCor').value = '1';
    document.querySelector('#modalQtd').value = '10';
    document.querySelector('#modalBackdrop').classList.remove('hidden');
}
function fecharModal() {
    document.querySelector('#modalBackdrop').classList.add('hidden');
    posicaoAtual = null;
}
async function confirmarAdicao() {
    if (posicaoAtual == null) return;
    const cor = Number(document.querySelector('#modalCor').value);
    const quantidade = Number(document.querySelector('#modalQtd').value);
    if (!quantidade || quantidade < 1) { toast('Quantidade deve ser ≥ 1.', 'error'); return; }

    try {
        const r = await fetch(`${API}/api/estoque`, {
            method: 'POST', headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ cor, quantidade, posicao: posicaoAtual })
        });
        const d = await r.json().catch(() => ({}));
        if (!r.ok) throw new Error(d.erro || d.message || 'Não foi possível adicionar o bloco.');
        toast(`Bloco ${COR_NOME[cor]} adicionado na posição ${posicaoAtual}.`, 'success');
        fecharModal();
        carregarEstoque();
    } catch (e) { toast(e.message, 'error'); }
}

/* ----- Remover: DELETE /api/estoque/posicao/{posicao} ----- */
async function removerBloco(pos, cor) {
    if (!confirm(`Remover o bloco ${COR_NOME[cor] || cor} da posição ${pos}?`)) return;
    try {
        const r = await fetch(`${API}/api/estoque/posicao/${pos}`, { method: 'DELETE' });
        if (!r.ok && r.status !== 204) {
            const d = await r.json().catch(() => ({}));
            throw new Error(d.erro || 'Não foi possível remover o bloco.');
        }
        toast(`Posição ${pos} liberada.`, 'success');
        carregarEstoque();
    } catch (e) { toast(e.message, 'error'); }
}

/* ============================================================
   Helpers
   ============================================================ */
function hexToBytes(hex) {
    if (!hex) return [];
    return hex.trim().split(/\s+/).filter(Boolean).map(h => parseInt(h, 16) & 0xFF);
}

let toastTimer;
function toast(msg, tipo = 'info') {
    const el = document.querySelector('#toast');
    el.textContent = msg;
    el.className = `toast ${tipo} show`;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.remove('show'), 4000);
}

// Encerra os streams ao sair da página (libera as threads no servidor).
window.addEventListener('beforeunload', () => {
    Object.keys(sources).forEach(fecharStream);
});
