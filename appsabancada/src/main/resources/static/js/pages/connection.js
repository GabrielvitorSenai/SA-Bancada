document.addEventListener('DOMContentLoaded', () => {
    const form = document.querySelector('#connectionForm');
    const input = document.querySelector('#ipBase');

    if (input && !input.value) {
        input.value = '10.74.241.10';
    }

    if (form) {
        form.addEventListener('submit', async (event) => {
            event.preventDefault();
            await conectarBancada();
        });
    }

    escreverLog('Aguardando conexão...');
});

function montarIps() {
    const ipBase = document.querySelector('#ipBase')?.value?.trim() || '10.74.241.10';
    const partes = ipBase.split('.');

    if (partes.length !== 4) {
        throw new Error('IP inválido. Exemplo: 10.74.241.10');
    }

    const prefixo = `${partes[0]}.${partes[1]}.${partes[2]}`;

    return {
        estoque: `${prefixo}.10`,
        processo: `${prefixo}.20`,
        montagem: `${prefixo}.30`,
        expedicao: `${prefixo}.40`
    };
}

async function conectarBancada() {
    try {
        const payload = montarIps();

        escreverLog(`IPs gerados: ${JSON.stringify(payload)}`);

        escreverLog('1/3 Testando conexão com os CLPs...');
        const ping = await api.post('/api/clp/smart/ping', payload);
        escreverLog(`Ping: ${JSON.stringify(ping)}`);

        escreverLog('2/3 Sincronizando estoque e expedição...');
        const sync = await api.post('/api/clp/sync-all', payload);
        escreverLog(`Sync: ${JSON.stringify(sync)}`);

        escreverLog('3/3 Iniciando leituras contínuas...');
        const readings = await api.post('/api/clp/start-readings', payload);
        escreverLog(`Leituras: ${JSON.stringify(readings)}`);

        const status = await api.get('/api/clp/status');
        escreverLog(`Status: ${JSON.stringify(status)}`);

        toastSucesso('Bancada conectada com sucesso.');

        setTimeout(() => {
            window.location.href = '/monitoramento';
        }, 900);

    } catch (error) {
        console.error(error);
        escreverLog(`ERRO: ${error.message}`);
        toastErro(error.message || 'Erro ao conectar bancada.');
    }
}

function escreverLog(texto) {
    const log = document.querySelector('#connectionLog');
    const hora = new Date().toLocaleTimeString('pt-BR');

    if (log) {
        log.textContent += `\n[${hora}] ${texto}`;
        log.scrollTop = log.scrollHeight;
    }
}