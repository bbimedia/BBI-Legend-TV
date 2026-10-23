const assert = require('node:assert/strict');
const {newer, releaseInfo} = require('../public/updates.js');
assert.equal(newer('v1.10.0','1.2.0'), true);
assert.equal(newer('v2.0.0','1.99.99'), true);
assert.equal(newer('v1.2.0','1.2.0'), false);
assert.equal(newer('v1.1.9','1.2.0'), false);
for (const tag of ['v1.3.0-beta','garbage','1.2','v01.2.0']) assert.equal(newer(tag,'1.2.0'),false);
assert.equal(releaseInfo({tag_name:'v1.3.0',prerelease:true}),null);
assert.equal(releaseInfo({tag_name:'v1.3.0',draft:true}),null);
assert.equal(releaseInfo({tag_name:'v1.3.0',html_url:'https://evil.example'}).url,
  'https://github.com/bbimedia/BBI-Legend-TV/releases/tag/v1.3.0');
console.log('Update version and release validation passed.');

const vm = require('node:vm');
const fs = require('node:fs');
const code = fs.readFileSync(require.resolve('../public/updates.js'), 'utf8');
function client(saved = {}, fail = false) {
  const nodes = new Map(); let calls = 0; let persisted = saved;
  const querySelector = key => {
    if (!nodes.has(key)) { const classes = new Set(['hidden']); nodes.set(key, {
      textContent:'', checked:false, disabled:false, events:{},
      classList:{add:x=>classes.add(x),toggle:(x,on)=>on?classes.add(x):classes.delete(x),contains:x=>classes.has(x)},
      addEventListener(name,fn){this.events[name]=fn;}
    }); } return nodes.get(key);
  };
  const context = {document:{querySelector}, localStorage:{getItem:()=>JSON.stringify(saved),setItem:(_k,v)=>persisted=JSON.parse(v)},
    AbortController, setTimeout, clearTimeout, setInterval:()=>{},
    fetch:async()=>{calls++;if(fail)throw Error('offline');return {ok:true,json:async()=>({tag_name:'v1.3.0'})};}};
  vm.runInNewContext(code,context);
  return {nodes, calls:()=>calls, saved:()=>persisted, flush:()=>new Promise(resolve=>setImmediate(resolve))};
}
(async()=>{
  const fresh=client();await fresh.flush();
  assert.equal(fresh.calls(),1);
  assert.equal(fresh.nodes.get('#update-banner').classList.contains('hidden'),false);
  fresh.nodes.get('#update-dismiss').events.click();
  assert.equal(fresh.saved().dismissed,'v1.3.0');
  const cached=client(fresh.saved());await cached.flush();assert.equal(cached.calls(),0);
  assert.equal(cached.nodes.get('#update-banner').classList.contains('hidden'),true);
  const off=client({enabled:false});await off.flush();assert.equal(off.calls(),0);
  await off.nodes.get('#update-check').events.click();await off.flush();assert.equal(off.calls(),1);
  const offline=client({},true);await offline.flush();
  assert.equal(offline.nodes.get('#update-check').disabled,false);
  assert.match(offline.nodes.get('#update-status').textContent,/Chưa kiểm tra/);
  assert.equal(offline.nodes.get('#update-banner').classList.contains('hidden'),true);
  console.log('Automatic/manual checks, cache, dismiss, opt-out and offline behavior passed.');
})().catch(e=>{console.error(e);process.exitCode=1;});
