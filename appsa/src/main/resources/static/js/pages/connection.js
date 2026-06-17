/**
 * Tela de conexão.
 * Usuário informa apenas o IP do primeiro CLP, exemplo 10.74.241.10.
 * O front gera automaticamente: .10, .20, .30 e .40.
 */
document.addEventListener('DOMContentLoaded', () => {
    const inputBase = document.querySelector('#baseIp');
    const btnPing = document.querySelector('#btnPing');
    const btnConnect = document.querySelector('#btnConnect');
    const btnStop = document.querySelector('#btnStop');
    const consoleEl = document.querySelector('#connectionLog');

    inputBase.value = sessionStorage.getItem('smart40.baseIp') || '10.74.241.10';

    renderIps();
    carregarStatusClp();

    inputBase.addEventListener('input', renderIps);
    btnPing.addEventListener('click', pingar);
    btnConnect.addEventListener('click', conectar);
    btnStop.addEventListener('click', parar);

    setInterval(carregarStatusClp, 3000);

    function gerarIps() {
        const base = inputBase.value.trim();
        const partes = base.split('.');

        if (partes.length !== 4) {
            throw new Error('IP base inválido. Use o formato 10.74.241.10');
        }

        const prefixo = `${partes[0]}.${partes[1]}.${partes[2]}`;

        return {
            estoque: `${prefixo}.10`,
            processo: `${prefixo}.20`,
            montagem: `${prefixo}.30`,
            expedicao: `${prefixo}.40`
        };
    }

    function renderIps() {
        try {
            const ips = gerarIps();
            Object.entries(ips).forEach(([nome, ip]) => {
                Smart40.UI.setText(`#ip-${nome}`, ip);
            });
        } catch {
            ['estoque', 'processo', 'montagem', 'expedicao'].forEach(nome => {
                Smart40.UI.setText(`#ip-${nome}`, '-');
            });
        }
    }

    async function pingar() {
        try {
            const ips = gerarIps();
            log(`Testando porta 102: ${JSON.stringify(ips)}`);
            const result = await Smart40.API.post('/api/clp/smart/ping', ips);
            renderPing(result);
            log(`Ping retorno: ${JSON.stringify(result)}`);
            Smart40.UI.toast('Teste de conexão finalizado.', 'success');
        } catch (e) {
            Smart40.UI.toast(e.message, 'error');
            log(`ERRO: ${e.message}`);
        }
    }

    async function conectar() {
        try {
            const ips = gerarIps();
            sessionStorage.setItem('smart40.baseIp', inputBase.value.trim());
            log(`Conectando: ${JSON.stringify(ips)}`);

            const result = await Smart40.API.post('/api/clp/start-readings', ips);
            log(`Resposta: ${JSON.stringify(result)}`);

            Smart40.UI.toast(result.mensagem || 'Leituras iniciadas.', result.sucesso ? 'success' : 'error');
            await carregarStatusClp();
        } catch (e) {
            Smart40.UI.toast(e.message, 'error');
            log(`ERRO: ${e.message}`);
        }
    }

    async function parar() {
        try {
            const result = await Smart40.API.post('/api/clp/stop-readings');
            log(`Parada: ${JSON.stringify(result)}`);
            Smart40.UI.toast('Leituras interrompidas.', 'success');
            await carregarStatusClp();
        } catch (e) {
            Smart40.UI.toast(e.message, 'error');
        }
    }

    async function carregarStatusClp() {
        try {
            const status = await Smart40.API.get('/api/clp/status');
            const online = Boolean(status.estoqueConectado || status.processoConectado || status.montagemConectado || status.expedicaoConectado);
            Smart40.UI.setConnectionPill(online, online ? `Ativas: ${(status.leiturasAtivas || []).join(', ')}` : 'Sem leitura ativa');
            renderStatus(status);
        } catch {
            Smart40.UI.setConnectionPill(false, 'Endpoint /api/clp/status indisponível');
        }
    }

    function renderPing(result) {
        Object.entries(result || {}).forEach(([nome, ok]) => {
            const card = document.querySelector(`[data-station-card="${nome}"]`);
            if (card) {
                card.classList.toggle('online', Boolean(ok));
                const strong = card.querySelector('strong');
                if (strong) strong.textContent = ok ? 'Porta 102 aberta' : 'Offline';
            }
        });
    }

    function renderStatus(status) {
        const map = {
            estoque: status.estoqueConectado,
            processo: status.processoConectado,
            montagem: status.montagemConectado,
            expedicao: status.expedicaoConectado
        };

        Object.entries(map).forEach(([nome, ok]) => {
            const card = document.querySelector(`[data-station-card="${nome}"]`);
            if (card) {
                card.classList.toggle('online', Boolean(ok));
                const strong = card.querySelector('strong');
                if (strong) strong.textContent = ok ? 'Leitura ativa' : 'Aguardando';
            }
        });
    }

    function log(message) {
        consoleEl.textContent += `[${Smart40.UI.now()}] ${message}\n`;
        consoleEl.scrollTop = consoleEl.scrollHeight;
    }
});
