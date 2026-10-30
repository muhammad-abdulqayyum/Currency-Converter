import { test } from 'node:test';
import assert from 'node:assert/strict';
import worker from '../worker/index.ts';
const env={ASSETS:{fetch:async()=>new Response('frontend')}};
const call=(path:string)=>worker.fetch(new Request('https://example.com'+path),env);
test('API validates methods, inputs and routing',async()=>{
 assert.equal((await call('/api/health')).status,200);
 assert.equal((await call('/api/unknown')).status,404);
 assert.equal((await call('/api/convert?amount=-1')).status,400);
 assert.equal((await call('/api/convert?to=BAD')).status,400);
 assert.equal((await worker.fetch(new Request('https://example.com/api/convert',{method:'POST'}),env)).status,405);
 assert.equal(await (await call('/')).text(),'frontend');
});
test('API returns decimal-safe results and source metadata',async()=>{
 const original=globalThis.fetch;
 globalThis.fetch=async()=>Response.json({base:'USD',quote:'EUR',rate:0.9,date:new Date().toISOString().slice(0,10),providers:[{key:'ECB'}]});
 try {const response=await call('/api/convert?from=USD&to=EUR&amount=1.25');assert.equal(response.status,200);const result=await response.json();assert.equal(result.result,'1.13');assert.equal(result.quotes[1].code,'EUR');assert.equal(result.referenceOnly,true);}finally{globalThis.fetch=original;}
});
test('upstream failure, stale data and wrong pairs never become prices',async()=>{
 const original=globalThis.fetch;
 try {for(const data of [{base:'EUR',quote:'EUR',rate:1,date:'2026-01-01'},{base:'USD',quote:'EUR',rate:1,date:'2020-01-01'}]){globalThis.fetch=async()=>Response.json(data);assert.equal((await call('/api/convert')).status,503);}
 globalThis.fetch=async()=>{throw new Error('offline')};assert.equal((await call('/api/convert')).status,503);
 }finally{globalThis.fetch=original;}
});
