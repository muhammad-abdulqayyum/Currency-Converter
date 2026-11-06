import { NgTemplateOutlet } from '@angular/common';
import { Component, signal } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { assets, Asset } from '../../shared/assets';
const searchAliases: Partial<Record<string, string>> = {
  USD: 'US Dollar',
};

@Component({
  selector: 'app-root', standalone: true, imports: [FormsModule, NgTemplateOutlet],
  styles: [`
    .footer-credit { display: flex; flex-wrap: wrap; justify-content: flex-end; align-items: center; gap: 4px 12px; line-height: 1.6; }
    .footer-author { display: inline-flex; align-items: center; gap: 6px; }
    .footer-credit .heritage-flag { flex: none; object-fit: contain; }
    .footer-credit a { color: #244e43; white-space: nowrap; }
    @media (max-width: 600px) {
      footer { justify-content: center; }
      .footer-brand { display: none; }
      .footer-credit { justify-content: center; text-align: center; }
    }
  `],
  template: `
  <ng-template #assetIcon let-asset>
    <span class="asset-icon" [class.artwork]="asset.kind !== 'world'" aria-hidden="true">
      @if (asset.kind === 'crypto') {
        <img [src]="cryptoIcon(asset.code)" alt="">
        @if (asset.code === 'SAT' || asset.code === 'DROP') {
          <span class="crypto-unit">{{asset.code.toLowerCase()}}</span>
        }
      } @else if (asset.kind === 'metal') {
        <img [src]="'icons/' + asset.code.toLowerCase() + '.png'" alt="">
      } @else {
        @for (flag of worldIcons(asset.icon); track flag) {
          <img class="world-flag" [src]="flag" alt="">
        }
      }
    </span>
  </ng-template>
  <header><a href="/" class="brand"><span class="logo">◎</span><span>UNIVERSAL<small>CURRENCY CONVERTER</small></span></a><span class="web-label">WORLD · METALS · CRYPTO</span></header>
  <main><div class="intro"><span class="eyebrow">A WORLD OF VALUE, CONNECTED</span><h1>Different currencies.<br><em>One clear conversion.</em></h1><p>Explore world currencies, precious metals and cryptocurrency in one place.</p></div>
  <section class="converter" aria-label="Currency converter"><div class="card-top"><h2>Make the connection</h2><span class="badge">REFERENCE RATES</span></div>
  <form (ngSubmit)="calculate()"><label for="amount">Amount to convert</label><div class="amount"><input id="amount" name="amount" inputmode="decimal" [(ngModel)]="amount" (ngModelChange)="invalidate()" maxlength="49" autocomplete="off" required><span>{{from}}</span></div>
  <div class="pair"><button type="button" class="asset" (click)="openPicker('from')"><ng-container [ngTemplateOutlet]="assetIcon" [ngTemplateOutletContext]="{$implicit: asset(from)}"></ng-container><span><small>FROM</small><strong>{{from}}</strong><span class="asset-name">{{asset(from).name}}</span></span><span class="chevron">⌄</span></button>
  <button type="button" class="swap" aria-label="Swap currencies" (click)="swap()">⇄</button>
  <button type="button" class="asset" (click)="openPicker('to')"><ng-container [ngTemplateOutlet]="assetIcon" [ngTemplateOutletContext]="{$implicit: asset(to)}"></ng-container><span><small>TO</small><strong>{{to}}</strong><span class="asset-name">{{asset(to).name}}</span></span><span class="chevron">⌄</span></button></div>
  <button class="convert" type="submit" [disabled]="loading()">{{loading() ? 'Getting reference rates…' : 'Convert now'}} <span>↗</span></button></form>
  <div class="result" aria-live="polite">@if(error()){<p class="error" role="alert">{{error()}}</p>} @else if(result()){<small>{{result().amount}} {{result().from}} equals</small><div class="value">{{result().result}} <span>{{result().to}}</span></div>@for(q of result().quotes; track $index){<p class="quote">{{q.code}} · {{q.date}} · {{q.source}} {{q.cached ? '· cached' : ''}}</p>}} @else {<span class="result-placeholder">Your next perspective starts here.</span>}</div>
  <p class="note">Dated reference values, not live trading quotes. Metal units are troy ounces. Rates may have different effective dates; provider coverage varies.</p></section>
  <div class="features"><div><span>01</span><strong>Across borders</strong><p>World currencies in one searchable collection.</p></div><div><span>02</span><strong>Beyond currencies</strong><p>Gold, silver, platinum and palladium.</p></div><div><span>03</span><strong>Into digital</strong><p>Crypto and smaller denominations.</p></div></div>
  </main><footer>
    <div class="footer-brand">Universal Currency Converter</div>
    <div class="footer-credit"><div class="footer-author"><div>Built by Muhammad Hafiz Abdul-Qayyum</div>
      <img class="heritage-flag" src="icons/heritage_flag.png" width="28" height="19" alt="Black American Heritage Flag"></div>
      <div>
      <a href="https://www.linkedin.com/in/mabdulqayyum">LinkedIn</a> ·
      <a href="https://github.com/muhammad-abdulqayyum/Currency-Converter">Source code</a></div>
    </div>
  </footer>
  @if(picking()) {<div class="overlay" (click)="closePicker()"><section class="picker" role="dialog" aria-modal="true" aria-labelledby="picker-title" (click)="$event.stopPropagation()" (keydown.escape)="closePicker()"><div class="card-top"><h2 id="picker-title">Choose an asset</h2><button type="button" class="close" aria-label="Close asset picker" (click)="closePicker()">×</button></div><input class="search" aria-label="Search assets" placeholder="Search name or code…" [(ngModel)]="search"><div class="tabs">@for(k of kinds;track k){<button type="button" [class.active]="kind === k" (click)="kind=k"><img class="category-icon" [src]="k === 'world' ? 'icons/category_world.png' : k === 'metal' ? 'icons/category_metals.png' : 'icons/category_crypto.png'" width="20" height="20" alt="">{{k === 'world' ? 'World' : k === 'metal' ? 'Metals' : 'Crypto'}}</button>}</div><div class="asset-list">@for(a of filtered();track a.code){<button type="button" class="asset" (click)="select(a.code)"><ng-container [ngTemplateOutlet]="assetIcon" [ngTemplateOutletContext]="{$implicit: a}"></ng-container><span><strong>{{a.code}}</strong><span class="asset-name">{{a.name}}{{a.kind === 'metal' ? ' · troy oz' : ''}}</span></span><span class="chevron">↗</span></button>} @empty {<p>No matching assets in this category.</p>}</div></section></div>}
  `
})
class App {
  amount='100'; from='USD'; to='EUR'; assets=assets; kinds=['world','metal','crypto']; kind='world'; search='';
  picking=signal<''|'from'|'to'>(''); loading=signal(false); error=signal(''); result=signal<any>(null); private version=0;
  worldIcons(icon: string): string[] {
    const flags = icon.match(/[\u{1F1E6}-\u{1F1FF}]{2}/gu) ?? [];
    if (!flags.length) return ['icons/category_world.png'];
    return flags.map(flag => {
      const region = Array.from(flag, letter =>
        String.fromCharCode(letter.codePointAt(0)! - 0x1F1E6 + 97)).join('');
      return 'flags/' + region + '.svg';
    });
  }
  cryptoIcon(code: string): string {
    const coin = code === 'SAT' ? 'BTC' : code === 'DROP' ? 'XRP' : code;
    return 'icons/' + coin.toLowerCase() + '.png';
  }
  asset(code:string):Asset {return assets.find(a=>a.code===code)!;}
  filtered() {
    const query = this.search.trim().toLowerCase();
    return assets.filter(asset => asset.kind === this.kind &&
      (`${asset.code} ${asset.name}`.toLowerCase().includes(query) ||
        searchAliases[asset.code]?.toLowerCase().includes(query)));
  }
  invalidate(){this.version++;this.result.set(null);this.error.set('');this.loading.set(false);}
  openPicker(side:'from'|'to'){this.picking.set(side);this.kind=this.asset(this[side]).kind;this.search='';setTimeout(()=>document.querySelector<HTMLInputElement>('.search')?.focus());}
  closePicker(){this.picking.set('');}
  select(code:string){const side=this.picking();if(side)this[side]=code;this.closePicker();this.invalidate();}
  swap(){[this.from,this.to]=[this.to,this.from];this.invalidate();}
  async calculate(){const version=++this.version;this.loading.set(true);this.error.set('');this.result.set(null);try{const response=await fetch('/api/convert?'+new URLSearchParams({amount:this.amount,from:this.from,to:this.to}),{signal:AbortSignal.timeout(25000)});const body=await response.json();if(!response.ok)throw new Error(body.error || 'Conversion unavailable');if(version===this.version)this.result.set(body);}catch(error){if(version===this.version)this.error.set(error instanceof Error?error.message:'Unable to connect. Please try again.');}finally{if(version===this.version)this.loading.set(false);}}
}
bootstrapApplication(App).catch(console.error);
