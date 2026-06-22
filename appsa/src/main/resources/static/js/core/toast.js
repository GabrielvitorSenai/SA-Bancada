import { $ } from './dom.js';

let toastTimer;

export function toast(message, type = 'success') {
    const element = $('#toast');
    if (!element) return;

    element.textContent = message;
    element.className = `toast ${type} is-visible`;

    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
        element.classList.remove('is-visible');
    }, 4200);
}
