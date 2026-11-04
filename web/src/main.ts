import { Component, signal } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { assets, Asset } from '../../shared/assets';
@Component({
  selector: 'app-root', standalone: true, imports: [FormsModule],
  template: `
  <header><a href="/" class="brand"><span class="logo">◎</span><span>UNIVERSAL<small>CURRENCY CONVERTER</small></span></a><span class="web-label">WORLD · METALS · CRYPTO</span></header>
  <main><div class="intro"><span class="eyebrow">A WORLD OF VALUE, CONNECTED</span><h1>Different currencies.<br><em>One clear conversion.</em></h1><p>Explore world currencies, precious metals and cryptocurrency in one place.</p></div>
  <section class="converter" aria-label="Currency converter"><div class="card-top"><h2>Make the connection</h2><span class="badge">REFERENCE RATES</span></div>
  <form (ngSubmit)="calculate()"><label for="amount">Amount to convert</label><div class="amount"><input id="amount" name="amount" inputmode="decimal" [(ngModel)]="amount" (ngModelChange)="invalidate()" maxlength="49" autocomplete="off" required><span>{{from}}</span></div>
  <div class="pair"><button type="button" class="asset" (click)="openPicker('from')"><span class="asset-icon" [class.metal]="asset(from).kind === 'metal'">{{asset(from).icon}}</span><span><small>FROM</small><strong>{{from}}</strong><span class="asset-name">{{asset(from).name}}</span></span><span class="chevron">⌄</span></button>
  <button type="button" class="swap" aria-label="Swap currencies" (click)="swap()">⇄</button>
  <button type="button" class="asset" (click)="openPicker('to')"><span class="asset-icon" [class.metal]="asset(to).kind === 'metal'">{{asset(to).icon}}</span><span><small>TO</small><strong>{{to}}</strong><span class="asset-name">{{asset(to).name}}</span></span><span class="chevron">⌄</span></button></div>
  <button class="convert" type="submit" [disabled]="loading()">{{loading() ? 'Getting reference rates…' : 'Convert now'}} <span>↗</span></button></form>
  <div class="result" aria-live="polite">@if(error()){<p class="error" role="alert">{{error()}}</p>} @else if(result()){<small>{{result().amount}} {{result().from}} equals</small><div class="value">{{result().result}} <span>{{result().to}}</span></div>@for(q of result().quotes; track $index){<p class="quote">{{q.code}} · {{q.date}} · {{q.source}} {{q.cached ? '· cached' : ''}}</p>}} @else {<span class="result-placeholder">Your next perspective starts here.</span>}</div>
  <p class="note">Dated reference values, not live trading quotes. Metal units are troy ounces. Rates may have different effective dates; provider coverage varies.</p></section>
  <div class="features"><div><span>01</span><strong>Across borders</strong><p>World currencies in one searchable collection.</p></div><div><span>02</span><strong>Beyond currencies</strong><p>Gold, silver, platinum and palladium.</p></div><div><span>03</span><strong>Into digital</strong><p>Crypto and smaller denominations.</p></div></div>
  </main><footer>Universal Currency Converter<span>Clarity in every conversion.</span></footer>
  @if(picking()) {<div class="overlay" (click)="closePicker()"><section class="picker" role="dialog" aria-modal="true" aria-labelledby="picker-title" (click)="$event.stopPropagation()" (keydown.escape)="closePicker()"><div class="card-top"><h2 id="picker-title">Choose an asset</h2><button type="button" class="close" aria-label="Close asset picker" (click)="closePicker()">×</button></div><input class="search" aria-label="Search assets" placeholder="Search name or code…" [(ngModel)]="search"><div class="tabs">@for(k of kinds;track k){<button type="button" [class.active]="kind === k" (click)="kind=k">{{k === 'world' ? 'World' : k === 'metal' ? 'Metals' : 'Crypto'}}</button>}</div><div class="asset-list">@for(a of filtered();track a.code){<button type="button" class="asset" (click)="select(a.code)"><span class="asset-icon" [class.metal]="a.kind === 'metal'">{{a.icon}}</span><span><strong>{{a.code}}</strong><span class="asset-name">{{a.name}}{{a.kind === 'metal' ? ' · troy oz' : ''}}</span></span><span class="chevron">↗</span></button>} @empty {<p>No matching assets in this category.</p>}</div></section></div>}
  `
})
class App {
  amount='100'; from='USD'; to='EUR'; assets=assets; kinds=['world','metal','crypto']; kind='world'; search='';
  picking=signal<''|'from'|'to'>(''); loading=signal(false); error=signal(''); result=signal<any>(null); private version=0;
  asset(code:string):Asset {return assets.find(a=>a.code===code)!;}
  filtered(){const q=this.search.toLowerCase();return assets.filter(a=>a.kind===this.kind && (a.code+' '+a.name).toLowerCase().includes(q));}
  invalidate(){this.version++;this.result.set(null);this.error.set('');this.loading.set(false);}
  openPicker(side:'from'|'to'){this.picking.set(side);this.kind=this.asset(this[side]).kind;this.search='';setTimeout(()=>document.querySelector<HTMLInputElement>('.search')?.focus());}
  closePicker(){this.picking.set('');}
  select(code:string){const side=this.picking();if(side)this[side]=code;this.closePicker();this.invalidate();}
  swap(){[this.from,this.to]=[this.to,this.from];this.invalidate();}
  async calculate(){const version=++this.version;this.loading.set(true);this.error.set('');this.result.set(null);try{const response=await fetch('/api/convert?'+new URLSearchParams({amount:this.amount,from:this.from,to:this.to}),{signal:AbortSignal.timeout(25000)});const body=await response.json();if(!response.ok)throw new Error(body.error || 'Conversion unavailable');if(version===this.version)this.result.set(body);}catch(error){if(version===this.version)this.error.set(error instanceof Error?error.message:'Unable to connect. Please try again.');}finally{if(version===this.version)this.loading.set(false);}}
}
bootstrapApplication(App).catch(console.error);
