const CACHE_VERSION = 'finance-manager-v1';
const APP_SHELL_CACHE = `${CACHE_VERSION}-app-shell`;
const RUNTIME_CACHE = `${CACHE_VERSION}-runtime`;

const APP_SHELL_ASSETS = [
    '/',
    '/index.html',
    '/css/styles.css',
    '/js/main.js',
    '/manifest.webmanifest',
    '/icons/icon-192.png',
    '/icons/icon-512.png',
    '/icons/apple-touch-icon.png',
    '/icons/favicon-32.png'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(APP_SHELL_CACHE).then(cache => cache.addAll(APP_SHELL_ASSETS)).then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(cacheNames => Promise.all(
            cacheNames
                .filter(cacheName => !cacheName.startsWith(CACHE_VERSION))
                .map(cacheName => caches.delete(cacheName))
        )).then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', event => {
    if (event.request.method !== 'GET') {
        return;
    }
    if (!event.request.url.startsWith('http')) {
        return;
    }

    const url = new URL(event.request.url);
    const isApiRequest = url.pathname.startsWith('/expenses')
        || url.pathname.startsWith('/incomes')
        || url.pathname.startsWith('/balance')
        || url.pathname.startsWith('/categories')
        || url.pathname.startsWith('/receipts')
        || url.pathname.startsWith('/auth');

    if (isApiRequest) {
        event.respondWith(networkFirst(event.request));
        return;
    }

    event.respondWith(cacheFirst(event.request));
});

async function cacheFirst(request) {
    const cached = await caches.match(request);
    if (cached) {
        return cached;
    }
    const networkResponse = await fetch(request);
    const runtimeCache = await caches.open(RUNTIME_CACHE);
    runtimeCache.put(request, networkResponse.clone());
    return networkResponse;
}

async function networkFirst(request) {
    try {
        const networkResponse = await fetch(request);
        const runtimeCache = await caches.open(RUNTIME_CACHE);
        runtimeCache.put(request, networkResponse.clone());
        return networkResponse;
    } catch (_) {
        const cached = await caches.match(request);
        if (cached) {
            return cached;
        }
        return new Response(
            JSON.stringify({ message: 'Нет сети. Проверьте интернет и попробуйте снова.' }),
            {
                status: 503,
                headers: { 'Content-Type': 'application/json' }
            }
        );
    }
}
