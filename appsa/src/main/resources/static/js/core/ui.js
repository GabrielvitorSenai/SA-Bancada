/**
 * Funções visuais reutilizadas nas páginas.
 */
window.Smart40 = window.Smart40 || {};

Smart40.UI = {
    toast(message, type = 'success') {
        const el = document.querySelector('#appToast');
        if (!el) {
            alert(message);
            return;
        }

        el.className = `toast ${type}`;
        el.textContent = message;
        el.classList.add('show');

        clearTimeout(window.__toastTimer);
        window.__toastTimer = setTimeout(() => el.classList.remove('show'), 3600);
    },

    confirm(message) {
        return window.confirm(message);
    },

    now() {
        return new Date().toLocaleTimeString('pt-BR', {
            hour: '2-digit',
            minute: '2-digit',
            second: '2-digit'
        });
    },

    colorName(cor) {
        const nomes = {
            0: 'Livre',
            1: 'Preto',
            2: 'Vermelho',
            3: 'Azul'
        };
        return nomes[Number(cor)] || `Cor ${cor}`;
    },

    statusPedido(status) {
        const mapa = {
            1: 'Pendente',
            2: 'Em produção',
            3: 'Concluído'
        };
        return mapa[Number(status)] || 'Indefinido';
    },

    statusEstacao(status) {
        const mapa = {
            0: 'Aguardando',
            1: 'Em operação',
            2: 'Finalizado'
        };
        return mapa[Number(status)] || 'Indefinido';
    },

    formatDate(date) {
        if (!date) return '-';
        try {
            return new Date(date).toLocaleString('pt-BR');
        } catch {
            return date;
        }
    },

    setText(selector, value) {
        const el = document.querySelector(selector);
        if (el) el.textContent = value;
    },

    setConnectionPill(online, text) {
        const pill = document.querySelector('#connectionPill');
        if (!pill) return;

        pill.classList.toggle('online', Boolean(online));
        const label = pill.querySelector('strong');
        const sub = pill.querySelector('small');
        if (label) label.textContent = online ? 'Bancada conectada' : 'Bancada desconectada';
        if (sub) sub.textContent = text || (online ? 'CLPs com leitura ativa' : 'Sem leitura ativa');
    }
};

document.addEventListener('DOMContentLoaded', () => {
    const toggle = document.querySelector('[data-mobile-menu]');
    if (toggle) {
        toggle.addEventListener('click', () => document.body.classList.toggle('sidebar-open'));
    }

    const path = window.location.pathname;
    document.querySelectorAll('.side-nav a').forEach(a => {
        const href = a.getAttribute('href');
        if (href && href !== '/' && path.startsWith(href)) a.classList.add('active');
        if (href === '/' && path === '/') a.classList.add('active');
    });
});
