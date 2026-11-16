const starterCharacters=[
 {name:"Aiko",age:21,role:"The thoughtful class representative",personality:"Kind, observant and quietly brave",symbol:"葵"},
 {name:"Ren",age:24,role:"The wandering musician",personality:"Playful, independent and perceptive",symbol:"蓮"},
 {name:"Mika",age:22,role:"The ambitious young inventor",personality:"Confident, curious and loyal",symbol:"美"},
 {name:"Yuna",age:23,role:"The starlight café owner",personality:"Warm, witty and creative",symbol:"結"},
 {name:"Kaito",age:25,role:"The city mapmaker",personality:"Calm, adventurous and mysterious",symbol:"海"},
 {name:"Hana",age:21,role:"The botanical researcher",personality:"Gentle, clever and determined",symbol:"花"}
];
let characters=[...starterCharacters];
const places=["Moonlit Academy","Silverleaf Market","Lantern Street","Royal Archive","Whispering Forest","Harbor of Glass"];
let selectedPlace=places[0];
const $=s=>document.querySelector(s);
function switchView(name){document.querySelectorAll(".view").forEach(v=>v.classList.toggle("active",v.id==="view-"+name));document.querySelectorAll(".nav").forEach(b=>b.classList.toggle("active",b.dataset.view===name));$(".sidebar").classList.remove("open");window.scrollTo({top:0,behavior:"smooth"});}
document.querySelectorAll("[data-view]").forEach(b=>b.addEventListener("click",()=>switchView(b.dataset.view)));
$("#menuBtn").addEventListener("click",()=>$(".sidebar").classList.toggle("open"));
function renderCharacters(filter=""){const grid=$("#characterGrid");grid.innerHTML="";characters.filter(c=>(c.name+" "+c.role+" "+c.personality).toLowerCase().includes(filter.toLowerCase())).forEach((c,i)=>{const card=document.createElement("article");card.className="character-card";const avatar=document.createElement("div");avatar.className="avatar";avatar.textContent=c.symbol||"✦";const h=document.createElement("h3");h.textContent=c.name+" · "+c.age;const role=document.createElement("p");role.textContent=c.role;const desc=document.createElement("p");desc.textContent=c.personality;const btn=document.createElement("button");btn.className="text-btn";btn.textContent="Start a story →";btn.addEventListener("click",()=>{switchView("story");$("#chatLog").insertAdjacentHTML("beforeend",'<div class="notice">Selected '+escapeHtml(c.name)+'. AI character connection will be enabled with the secure backend.</div>');});card.append(avatar,h,role,desc,btn);grid.append(card);});}
function escapeHtml(s){return String(s).replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));}
$("#searchCharacters").addEventListener("input",e=>renderCharacters(e.target.value));
function renderPlaces(){const list=$("#placeList");places.forEach(p=>{const b=document.createElement("button");b.className="place";b.innerHTML='<strong>'+escapeHtml(p)+'</strong><span>'+(p===selectedPlace?"CURRENT LOCATION":"TRAVEL →")+'</span>';b.addEventListener("click",()=>{selectedPlace=p;renderPlaces();});list.append(b);});}
$("#createCharacter").addEventListener("click",()=>$("#characterDialog").showModal());
$("#characterForm").addEventListener("submit",e=>{e.preventDefault();const name=$("#newName").value.trim(),age=Number($("#newAge").value),personality=$("#newPersonality").value.trim();if(!name||age<21)return;characters.push({name,age,role:"Original character",personality:personality||"Personality waiting to be discovered",symbol:"✧"});$("#characterDialog").close();e.target.reset();renderCharacters($("#searchCharacters").value);});
$("#chatForm").addEventListener("submit",e=>{e.preventDefault();const input=$("#chatInput"),value=input.value.trim();if(!value)return;const wrap=document.createElement("div");wrap.className="message";const who=document.createElement("span");who.className="message-name";who.textContent="You";const p=document.createElement("p");p.textContent=value;wrap.append(who,p);$("#chatLog").append(wrap);input.value="";const notice=document.createElement("div");notice.className="notice";notice.textContent="Your message is shown in this frontend preview. The secure online AI backend is the next milestone; no fake AI reply is being generated.";$("#chatLog").append(notice);$("#chatLog").scrollTop=$("#chatLog").scrollHeight;});
renderCharacters();renderPlaces();