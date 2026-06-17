import { $, setText } from '../core/dom.js';
import { toast } from '../core/toast.js';
import { state } from '../core/state.js';
import { api } from '../services/api.js';
import { renderMetrics } from './metrics.js';
import { renderEstoque } from './estoque.js';

const BANCADAS = ['estoque', 'processo', 'montagem', 'expedicao'];
const RAW_IDS = {
    estoque: '#rawEstoque',
    processo: '#rawProcesso',
    montagem: '#rawMontagem',
    expedicao: '#rawExpedicao'
};
const STATION_IDS = {
    estoque: '#stationEstoque',
    processo: '#stationProcesso',
    montagem: '#stationMontagem',
    expedicao: '#stationExpedicao'
};

export function initClpPanel() {
    restoreIps();
    bindClpEvents();
    renderClpStatus();
}

function bindClpEvents() {
    $('#ipBase')?.addEventListener('blur', preencherIpsPorBase);
    ['#ipBase', '#ipEstoque', '#ipProcesso', '#ipMontagem', '#ipExpedicao'].forEach((selector) => {
        $(selector)?.addEventListener('change', salvarIps);
    });

    $('#btnConectarClp')?.addEventListener('click', conectarLeituras);
    $('#btnDesconectarClp')?.addEventListener('click', desconectarLeituras);
}

function preencherIpsPorBase() {
    const base = $('#ipBase')?.value.trim();
    if (!base) return;

    const partes = base.split('.');
    const prefixo = partes.length === 4 ? partes.slice(0, 3).join('.') : base;

    // Ajuste os finais caso sua bancada use outra numeração.
    $('#ipEstoque').value = `${prefixo}.10`;
    $('#ipProcesso').value = `${prefixo}.11`;
    $('#ipMontagem').value = `${prefixo}.12`;
    $('#ipExpedicao').value = `${prefixo}.13`;
    salvarIps();
}

function salvarIps() {
    const values = getIps();
    localStorage.setItem('smart40.ips', JSON.stringify(values));
}

function restoreIps() {
    const saved = JSON.parse(localStorage.getItem('smart40.ips') || '{}');
    if ($('#ipBase')) $('#ipBase').value = saved.base ?? '';
    if ($('#ipEstoque')) $('#ipEstoque').value = saved.estoque ?? '';
    if ($('#ipProcesso')) $('#ipProcesso').value = saved.processo ?? '';
    if ($('#ipMontagem')) $('#ipMontagem').value = saved.montagem ?? '';
    if ($('#ipExpedicao')) $('#ipExpedicao').value = saved.expedicao ?? '';
}

function getIps() {
    return {
        base: $('#ipBase')?.value.trim() ?? '',
        estoque: $('#ipEstoque')?.value.trim() ?? '',
        processo: $('#ipProcesso')?.value.trim() ?? '',
        montagem: $('#ipMontagem')?.value.trim() ?? '',
        expedicao: $('#ipExpedicao')?.value.trim() ?? ''
    };
}

async function conectarLeituras() {
    const ips = getIps();
    const payload = {
        estoque: ips.estoque,
        processo: ips.processo,
        montagem: ips.montagem,
        expedicao: ips.expedicao
    };

    if (!payload.estoque) {
        toast('Informe pelo menos o IP do CLP de estoque.', 'error');
        return;
    }

    try {
        salvarIps();
        await api.clp.iniciarLeituras(payload);
        state.clp.conectado = true;
        abrirStreams();
        renderClpStatus();
        toast('Leituras da bancada iniciadas.', 'success');
    } catch (error) {
        state.clp.conectado = false;
        renderClpStatus();
        toast(error.message, 'error');
    }
}

async function desconectarLeituras() {
    try {
        fecharStreams();
        await api.clp.pararLeituras();
        toast('Leituras da bancada paradas.', 'success');
    } catch (error) {
        toast(error.message, 'error');
    } finally {
        state.clp.conectado = false;
        renderClpStatus();
    }
}

function abrirStreams() {
    fecharStreams();

    BANCADAS.forEach((bancada) => {
        const source = new EventSource(api.clp.streamUrl(bancada));

        source.onmessage = (event) => {
            state.clp.ultimoHex[bancada] = event.data;
            renderRaw(bancada, event.data);
            processarHex(bancada, event.data);
        };

        source.onerror = () => {
            // O backend pode ainda estar iniciando. Não fecha imediatamente para permitir reconexão automática do EventSource.
            console.warn(`SSE ${bancada} aguardando reconexão...`);
        };

        state.clp.streams[bancada] = source;
    });
}

function fecharStreams() {
    Object.values(state.clp.streams).forEach((source) => source?.close());
    state.clp.streams = {};
}

function renderRaw(bancada, data) {
    const element = $(RAW_IDS[bancada]);
    if (element) element.textContent = `${bancada}: ${data}`;
}

function processarHex(bancada, hex) {
    const bytes = hex.trim().split(/\s+/).map((item) => Number.parseInt(item, 16));
    if (!bytes.length || Number.isNaN(bytes[0])) return;

    if (bancada === 'estoque') {
        // No DB9 do estoque, as posições ocupadas começam no byte 68.
        const posicoes = bytes.slice(68, 96);
        if (posicoes.length === 28) {
            state.estoque = posicoes.map((cor, index) => ({
                posicao: index + 1,
                cor,
                idItem: null,
                disponivel: Number(cor) === 0
            }));
            renderEstoque();
        }

        // O ClpController pode acrescentar 6 bytes finais com status das estações.
        if (bytes.length >= 6) {
            const last = bytes.slice(-6);
            state.clp.status = {
                estoque: last[0],
                processo: last[1],
                montagem: last[2],
                expedicao: last[3],
                producao: last[4],
                pedidoEmCurso: last[5]
            };
            renderStationStatus();
            renderMetrics();
        }
    }
}

function renderStationStatus() {
    updateStation('estoque', state.clp.status.estoque);
    updateStation('processo', state.clp.status.processo);
    updateStation('montagem', state.clp.status.montagem);
    updateStation('expedicao', state.clp.status.expedicao);
}

function updateStation(name, status) {
    const card = $(STATION_IDS[name]);
    if (!card) return;

    card.classList.remove('is-running', 'is-finished', 'is-error');

    let label = 'Aguardando';
    if (Number(status) === 1) {
        label = 'Em operação';
        card.classList.add('is-running');
    } else if (Number(status) === 2) {
        label = 'Finalizado';
        card.classList.add('is-finished');
    } else if (Number(status) >= 3) {
        label = 'Atenção';
        card.classList.add('is-error');
    }

    card.querySelector('strong').textContent = label;
}

function renderClpStatus() {
    const badge = $('#clpBadge');
    badge?.classList.toggle('is-online', state.clp.conectado);
    setText('#clpBadgeText', state.clp.conectado ? 'Conectado' : 'Desconectado');
    renderMetrics();
}
