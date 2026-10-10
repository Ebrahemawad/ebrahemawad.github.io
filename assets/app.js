const themeButton=document.querySelector('.theme');
function updateTheme(dark){document.body.classList.toggle('dark',dark);themeButton.setAttribute('aria-label',dark?'Switch to light theme':'Switch to dark theme');}
try{updateTheme(localStorage.getItem('ebrahem-theme')==='dark');}catch{}
themeButton.addEventListener('click',()=>{const dark=!document.body.classList.contains('dark');updateTheme(dark);try{localStorage.setItem('ebrahem-theme',dark?'dark':'light');}catch{}});
document.querySelectorAll('[data-filter]').forEach(button=>button.addEventListener('click',()=>{
 document.querySelectorAll('[data-filter]').forEach(b=>{b.classList.toggle('active',b===button);b.setAttribute('aria-pressed',String(b===button));});
 let count=0;document.querySelectorAll('[data-tool]').forEach(card=>{const show=button.dataset.filter==='All'||card.dataset.tool===button.dataset.filter;card.hidden=!show;if(show)count++;});
 document.querySelector('#project-count').textContent=`SHOWING ${count} PROJECTS`;
}));
const dialog=document.querySelector('#image-dialog');
document.querySelectorAll('[data-zoom]').forEach(button=>button.addEventListener('click',()=>{dialog.querySelector('img').src=button.dataset.zoom;dialog.showModal();document.body.classList.add('modal-open');}));
dialog.querySelector('.close-dialog').addEventListener('click',()=>dialog.close());
dialog.addEventListener('click',event=>{if(event.target===dialog)dialog.close();});
dialog.addEventListener('close',()=>document.body.classList.remove('modal-open'));
