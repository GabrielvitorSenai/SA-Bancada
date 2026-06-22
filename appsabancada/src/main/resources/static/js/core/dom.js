// Pequenos helpers para reduzir repetição de querySelector.
export const $ = (selector, root = document) => root.querySelector(selector);
export const $$ = (selector, root = document) => Array.from(root.querySelectorAll(selector));

export function on(selector, event, handler, root = document) {
    const element = $(selector, root);
    if (element) element.addEventListener(event, handler);
}

export function setText(selector, value) {
    const element = $(selector);
    if (element) element.textContent = value;
}

export function setHTML(selector, value) {
    const element = $(selector);
    if (element) element.innerHTML = value;
}
