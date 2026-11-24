'use strict';
(function(root){
const valid=x=>typeof x==='number'&&Number.isFinite(x);
const values=a=>a.filter(valid).sort((a,b)=>a-b);
function quantile(a,p){if(!a.length)return null;let i=(a.length-1)*p,l=Math.floor(i);return a[l]+(a[Math.ceil(i)]-a[l])*(i-l)}
function stats(a){let x=values(a),n=x.length;if(!n)return {n:0,mean:null,median:null,p95:null,sd:null,min:null,max:null};let mean=x.reduce((s,v)=>s+v,0)/n;return {n,mean,median:quantile(x,.5),p95:quantile(x,.95),sd:n>1?Math.sqrt(x.reduce((s,v)=>s+(v-mean)**2,0)/(n-1)):null,min:x[0],max:x[n-1]}}
function summary(c){let s=c.samples||[], attempts=s.flatMap(r=>[...(r.lat||[]),r.down,r.up]);let done=attempts.filter(x=>x!==undefined);return {down:stats(s.map(r=>r.down)),up:stats(s.map(r=>r.up)),lat:stats(s.flatMap(r=>r.lat||[])),fail:done.length?100*done.filter(x=>x===null).length/done.length:null,attempts:done.length}}
function assess(c,t){if(!c)return {quality:'Aucune campagne',checks:[]};let s=summary(c),checks=[['Débit descendant',s.down.median,t.minDown,'min'],['Débit montant',s.up.median,t.minUp,'min'],['Latence HTTP p95',s.lat.p95,t.maxLat,'max'],['Échecs HTTP (%)',s.fail,t.maxFail,'max']].map(([name,value,limit,dir])=>({name,value,limit,state:!valid(value)||!valid(limit)?'indéterminé':(dir==='min'?value>=limit:value<=limit)?'conforme':'hors seuil'}));let quality=c.status!=='complete'||s.down.n<3||s.up.n<3||s.lat.n<9?'Mesures insuffisantes ou campagne interrompue':checks.some(x=>x.state==='hors seuil')?'Au moins un seuil dépassé':checks.every(x=>x.state==='conforme')?'Conforme aux seuils utilisateur':'Indéterminé';return {quality,checks,s}}
function linkCheck(input){let u;try{u=new URL(input.trim())}catch{return ['Adresse absolue invalide. Indiquer https://…']}if(!['http:','https:'].includes(u.protocol))return ['Protocole non HTTP(S) : ne pas ouvrir sans vérification.'];let out=[];if(u.protocol!=='https:')out.push('HTTP non chiffré');if(u.username||u.password)out.push('Identifiants intégrés à l’adresse (risque de tromperie)');if(u.hostname.includes('xn--'))out.push('Nom internationalisé : contrôler les caractères');if(/^\d+\.\d+\.\d+\.\d+$/.test(u.hostname)||u.hostname.includes(':'))out.push('Adresse IP littérale');if(u.hostname.split('.').length>4)out.push('Nombreux sous-domaines : vérifier le véritable domaine');if(u.port)out.push('Port inhabituel');return ['Hôte analysé : '+u.hostname,...(out.length?out:['Aucun indice heuristique détecté. La légitimité reste inconnue.'])]}
const range=(x,a,b)=>x==null||valid(x)&&x>=a&&x<=b;
function validRF(r){return r&&typeof r.place==='string'&&['LTE','NR'].includes(r.rat)&&typeof r.time==='string'&&Number.isFinite(Date.parse(r.time))&&range(r.rsrp,-160,-30)&&range(r.rsrq,-50,10)&&range(r.sinr,-40,60)&&range(r.pci,0,r.rat==='NR'?1007:503)&&[r.rsrp,r.rsrq,r.sinr].some(valid)}
function validCampaign(c){return c&&typeof c.place==='string'&&typeof c.time==='string'&&Number.isFinite(Date.parse(c.time))&&['local','cf'].includes(c.endpoint)&&['standard','live','stable'].includes(c.profile)&&['complete','stopped','error'].includes(c.status)&&Array.isArray(c.samples)&&c.samples.length<=10&&c.samples.every(r=>r&&Array.isArray(r.lat)&&r.lat.length<=10&&r.lat.every(x=>range(x,0,60000))&&range(r.down,0,1000000)&&range(r.up,0,1000000))}
function auditCode(source){
const rules=[
['SQL-DYNAMIC','élevé',/\b(?:query|exec|mysqli_query|pg_query)\s*\([^;]*(?:\$_(?:GET|POST|REQUEST|COOKIE)|\.\s*\$|"[^"\n]*\$)/i,'Revoir la construction de la requête ; utiliser des paramètres liés (PDO prepare/execute) et des listes autorisées pour les identifiants.'],
['SQL-INPUT','à vérifier',/\b(?:SELECT|INSERT|UPDATE|DELETE)\b[^;]*(?:\$_(?:GET|POST|REQUEST|COOKIE)|\.\s*\$)/i,'Vérifier le flux de données utilisateur vers SQL ; ne pas concaténer une entrée non fiable.'],
['PHP-EXEC','élevé',/\b(?:eval|shell_exec|system|passthru|popen|proc_open)\s*\(/i,'Éviter l’exécution dynamique ; préférer des API dédiées et des arguments validés.'],
['XSS-INPUT','élevé',/\b(?:echo|print)\s+[^;]*\$_(?:GET|POST|REQUEST|COOKIE)/i,'Échapper les sorties selon le contexte HTML, URL ou JavaScript.'],
['DESERIALIZE','à vérifier',/\bunserialize\s*\(/i,'Éviter la désérialisation de données non fiables ; préférer JSON et validation de schéma.'],
['WEAK-HASH','à vérifier',/\b(?:md5|sha1)\s*\(/i,'Pour les mots de passe, utiliser password_hash et password_verify.'],
['DEBUG','à vérifier',/\bdisplay_errors\b[^;]*(?:true|["']1["'])/i,'Désactiver l’affichage des erreurs en production, conserver les journaux privés.'],
['SECRET','élevé',/\b(?:password|passwd|api[_-]?key|secret|token)\s*=\s*["'][^"']{6,}["']/i,'Vérifier les secrets codés en dur ; déplacer vers un coffre ou variable protégée et renouveler si exposé.']
];let results=[];String(source).slice(0,250000).split(/\r?\n/).forEach((line,i)=>{for(const [id,severity,re,fix] of rules)if(re.test(line)&&results.length<250)results.push({id,severity,line:i+1,fix})});return results;
}
const api={auditCode,valid,stats,summary,assess,linkCheck,validRF,validCampaign};if(typeof module!=='undefined')module.exports=api;else root.Engine=api;
})(globalThis);
