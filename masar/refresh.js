(function(){
'use strict';
const BASE='/masar/';
const BAR_ID='masar-update-banner';
let lastCheck=0;
let busy=false;
let touchStart=null;
const getCurrentSignature=()=>{
  const style=document.querySelector('style')?.textContent||'';
  const scripts=Array.from(document.scripts).map(x=>x.textContent||'').filter(x=>x.length>1000);
  const main=scripts.sort((a,b)=>b.length-a.length)[0]||'';
  return {style,main};
};
function latestSignature(html){
  const doc=new DOMParser().parseFromString(html,'text/html');
  const style=doc.querySelector('style')?.textContent||'';
  const main=Array.from(doc.scripts).map(x=>x.textContent||'').filter(x=>x.length>1000).sort((a,b)=>b.length-a.length)[0]||'';
  return {style,main};
}
function banner(){
  if(document.getElementById(BAR_ID))return;
  const el=document.createElement('div');el.id=BAR_ID;
  el.setAttribute('role','status');
  el.style.cssText='position:fixed;left:12px;right:12px;bottom:14px;z-index:99999;background:#173d86;color:#fff;border-radius:12px;padding:12px 16px;display:flex;justify-content:space-between;align-items:center;gap:10px;box-shadow:0 10px 32px #173d8655;font-family:Cairo,Tahoma,Arial,sans-serif;font-size:13px;';
  const msg=document.createElement('span');msg.textContent='في تحديث جديد لمسار. حدّث التطبيق لعرض آخر نسخة.';
  const btn=document.createElement('button');btn.type='button';btn.textContent='تحديث الآن';btn.style.cssText='padding:9px 12px;border-radius:8px;background:#fff;color:#173d86;border:0;font-weight:800;white-space:nowrap;';
  btn.addEventListener('click',()=>refresh());
  el.append(msg,btn);document.body.appendChild(el);
}
async function check(force=false){
 if(busy || !navigator.onLine || (!force&&Date.now()-lastCheck<60000))return;
 lastCheck=Date.now();busy=true;
 try{
   const current=getCurrentSignature();
   const response=await fetch(BASE+'index.html?update_check='+Date.now(),{cache:'no-store',headers:{'Accept':'text/html'}});
   if(!response.ok)return;
   const remote=latestSignature(await response.text());
   if(remote.main&&current.main&&(remote.main!==current.main||remote.style!==current.style))banner();
 }catch(e){console.warn('Masar update check unavailable:',e)}
 finally{busy=false}
}
function refresh(){
 const url=new URL(BASE,location.origin);
 url.searchParams.set('refresh',Date.now().toString());
 location.replace(url.toString());
}
window.MasarRefresh={refresh,check};
let touchStartAt=0;
document.addEventListener('touchstart',e=>{
 if(e.touches.length!==1||window.scrollY>3)return;
 const t=e.touches[0];touchStart={x:t.clientX,y:t.clientY,target:e.target};
 touchStartAt=Date.now();
},{passive:true});
document.addEventListener('touchend',e=>{
 if(!touchStart||!e.changedTouches.length)return;
 const old=touchStart;touchStart=null;
 const t=e.changedTouches[0];
 const dx=t.clientX-old.x,dy=t.clientY-old.y;
 if(window.scrollY>3||dy<120||Math.abs(dx)>65||Date.now()-touchStartAt>2200)return;
 if(old.target.closest('input,select,textarea,button,a,[contenteditable],.tablewrap'))return;
 refresh();
},{passive:true});
document.addEventListener('visibilitychange',()=>{
 if(!document.hidden)void check(true);
});
window.addEventListener('online',()=>void check(true));
window.addEventListener('focus',()=>void check());
setInterval(()=>{if(!document.hidden)void check()},120000);
window.addEventListener('load',()=>{
 // On first load, a fresh network copy becomes authoritative; recheck for deploys.
 setTimeout(()=>void check(),20000);
});
})();