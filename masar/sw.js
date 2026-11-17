// Masar app shell: always prefer the newest online UI.
const CACHE='masar-app-shell-v2';
const SHELL=['/masar/','/masar/index.html','/masar/manifest.webmanifest','/masar/app-icon.svg','/masar/masar-logo.svg'];
self.addEventListener('install',event=>{
  event.waitUntil(caches.open(CACHE).then(c=>c.addAll(SHELL)).catch(()=>{}).then(()=>self.skipWaiting()));
});
self.addEventListener('activate',event=>{
  event.waitUntil(Promise.all([
    caches.keys().then(keys=>Promise.all(keys.filter(k=>k.startsWith('masar-app-shell-')&&k!==CACHE).map(k=>caches.delete(k)))),
    self.clients.claim()
  ]));
});
self.addEventListener('fetch',event=>{
  const request=event.request;
  if(request.method!=='GET')return;
  const url=new URL(request.url);
  if(url.origin!==self.location.origin||!url.pathname.startsWith('/masar/'))return;
  // Network-first and no HTTP-cache reuse for every online Masar page or app script.
  const main=url.pathname==='/masar/'||url.pathname==='/masar/index.html'||request.mode==='navigate';
  const safeAsset=SHELL.includes(url.pathname)||url.pathname==='/masar/refresh.js'||url.pathname==='/masar/driver-orders-pdf.js';
  event.respondWith(
    fetch(new Request(request,{cache:'no-store'})).then(response=>{
      if(response.ok&&(main||safeAsset)){
        const key=main?'/masar/index.html':url.pathname;
        event.waitUntil(caches.open(CACHE).then(cache=>cache.put(key,response.clone())).catch(()=>{}));
      }
      return response;
    }).catch(async()=>{
      const cache=await caches.open(CACHE);
      const saved=await cache.match(main?'/masar/index.html':url.pathname);
      if(saved)return saved;
      return Response.error();
    })
  );
});