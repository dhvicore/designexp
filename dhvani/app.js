(() => {
'use strict';
const slides = [...document.querySelectorAll('.slide')];
const dots = document.getElementById('dots');
const outline = document.getElementById('outlineDialog');
const sources = document.getElementById('sourcesDialog');
const audio = document.getElementById('sampleAudio');
const record = document.getElementById('demoRecord');
let current = Math.max(0, Math.min(slides.length - 1, (Number(location.hash.slice(1)) || 1) - 1));
let demos = [], selectedDemo = null;
slides.forEach((slide, index) => {
 const button = document.createElement('button');
 button.setAttribute('aria-label', `Go to slide ${index + 1}: ${slide.dataset.title}`);
 button.title = slide.dataset.title; button.onclick = () => show(index); dots.append(button);
 const item = document.createElement('button');
 const number = document.createElement('span'); number.textContent = String(index + 1).padStart(2,'0');
 item.append(number, document.createTextNode(slide.dataset.title));
 item.onclick = () => { outline.close(); show(index); };
 document.getElementById('outlineList').append(item);
});
function show(index, updateHash = true) {
 const next = Math.max(0, Math.min(slides.length - 1, index));
 if (next !== current) audio.pause();
 current = next;
 slides.forEach((slide, i) => {
  slide.classList.toggle('active', i === current);
  slide.setAttribute('aria-hidden', String(i !== current));
  slide.inert = i !== current;
  if (i === current) slide.scrollTop = 0;
 });
 [...dots.children].forEach((button,i) => {
  button.classList.toggle('active', i === current);
  button.setAttribute('aria-current', i === current ? 'step' : 'false');
 });
 [...document.getElementById('outlineList').children].forEach((button,i) => button.classList.toggle('active',i === current));
 document.getElementById('chapterNumber').textContent = String(current + 1).padStart(2,'0');
 document.getElementById('chapterName').textContent = slides[current].dataset.title;
 document.getElementById('slideCount').textContent = `${String(current + 1).padStart(2,'0')} / ${slides.length}`;
 document.getElementById('progressFill').style.width = `${(current + 1) / slides.length * 100}%`;
 document.getElementById('prev').disabled = current === 0;
 document.getElementById('next').disabled = current === slides.length - 1;
 document.title = `${slides[current].dataset.title} — Syllabeat`;
 if (updateHash) history.replaceState(null,'',`#${current + 1}`);
}
document.getElementById('prev').onclick = () => show(current - 1);
document.getElementById('next').onclick = () => show(current + 1);
document.querySelectorAll('[data-next]').forEach(button => button.onclick = () => show(current + 1));
document.getElementById('outlineBtn').onclick = () => outline.showModal();
document.getElementById('sourcesBtn').onclick = () => sources.showModal();
document.querySelectorAll('[data-close]').forEach(button => button.onclick = () => button.closest('dialog').close());
[outline,sources].forEach(dialog => dialog.addEventListener('click', event => { if(event.target === dialog) {const rect = dialog.getBoundingClientRect(); if(event.clientX < rect.left || event.clientX > rect.right || event.clientY < rect.top || event.clientY > rect.bottom) dialog.close();} }));
async function fullscreen() {
 try { if(document.fullscreenElement) await document.exitFullscreen(); else await document.documentElement.requestFullscreen(); }
 catch { document.getElementById('fullscreenBtn').title = 'Use your browser’s fullscreen command'; }
}
document.getElementById('fullscreenBtn').onclick = fullscreen;
document.getElementById('printBtn').onclick = () => { audio.pause(); window.print(); };
document.addEventListener('keydown', event => {
 if(outline.open || sources.open || /INPUT|TEXTAREA|SELECT|AUDIO|BUTTON/.test(event.target.tagName) || event.ctrlKey || event.metaKey || event.altKey) return;
 if(event.key === 'ArrowRight' || event.code === 'Space' || event.key === 'PageDown') { event.preventDefault(); show(current + 1); }
 if(event.key === 'ArrowLeft' || event.key === 'PageUp') { event.preventDefault(); show(current - 1); }
 if(event.key === 'Home') { event.preventDefault(); show(0); }
 if(event.key === 'End') { event.preventDefault(); show(slides.length - 1); }
 if(event.key.toLowerCase() === 'f') fullscreen();
});
let touchStart = null;
document.getElementById('deck').addEventListener('touchstart', event => { if(!event.target.closest('audio,button,a')) touchStart = {x:event.changedTouches[0].clientX,y:event.changedTouches[0].clientY}; },{passive:true});
document.getElementById('deck').addEventListener('touchend', event => {
 if(!touchStart) return;
 const dx = event.changedTouches[0].clientX - touchStart.x;
 const dy = event.changedTouches[0].clientY - touchStart.y;
 if(Math.abs(dx) > 65 && Math.abs(dx) > Math.abs(dy)*1.6) show(current + (dx < 0 ? 1 : -1));
 touchStart = null;
},{passive:true});
window.addEventListener('hashchange', () => show((Number(location.hash.slice(1)) || 1) - 1,false));
function choose(category) {
 const demo = demos.find(item => item.category === category);
 if(!demo) return;
 audio.pause(); selectedDemo = demo;
 audio.src = demo.src; audio.load();
 document.getElementById('demoTitle').textContent = `${category} · Rolle’s Theorem`;
 document.getElementById('demoLyric').textContent = 'Press play to hear the selected recording.';
 document.querySelectorAll('[data-category]').forEach(button => button.classList.toggle('selected',button.dataset.category === category));
}
document.querySelectorAll('[data-category]').forEach(button => button.onclick = () => choose(button.dataset.category));
audio.addEventListener('play',() => record.classList.add('playing'));
audio.addEventListener('pause',() => record.classList.remove('playing'));
audio.addEventListener('ended',() => { record.classList.remove('playing'); document.getElementById('demoLyric').textContent = 'Ready for another listen? Choose a different rhythm.'; });
audio.addEventListener('timeupdate',() => {
 if(!selectedDemo) return;
 const time = audio.currentTime * 1000;
 const cue = selectedDemo.cues.find(item => time >= item.startMs && time < item.endMs);
 document.getElementById('demoLyric').textContent = cue ? cue.text : (audio.currentTime < 1 ? 'Your lesson starts here…' : '♪ Instrumental / between lines');
});
audio.addEventListener('error',() => { document.getElementById('demoLyric').textContent = 'Audio could not load. Please reload the presentation and try again.'; });
fetch('demos.json').then(response => {if(!response.ok) throw Error('Audio catalog unavailable'); return response.json();}).then(data => { demos=data; choose('English'); }).catch(() => {document.getElementById('demoLyric').textContent='Audio samples are unavailable. The Android app includes these recordings offline.';});
show(current,false);
})();
