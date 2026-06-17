import { $, $$ } from '../core/dom.js';

export function initNavigation() {
    $('[data-nav-toggle]')?.addEventListener('click', () => {
        $('[data-topnav]')?.classList.toggle('is-open');
    });

    $$('[data-nav-link]').forEach((link) => {
        link.addEventListener('click', () => {
            $('[data-topnav]')?.classList.remove('is-open');
            setActive(link);
        });
    });

    window.addEventListener('scroll', syncActiveByScroll, { passive: true });
    syncActiveByScroll();
}

function setActive(activeLink) {
    $$('[data-nav-link]').forEach((link) => link.classList.toggle('is-active', link === activeLink));
}

function syncActiveByScroll() {
    const links = $$('[data-nav-link]');
    let current = links[0];

    links.forEach((link) => {
        const target = document.querySelector(link.getAttribute('href'));
        if (target && target.getBoundingClientRect().top < 160) {
            current = link;
        }
    });

    if (current) setActive(current);
}
