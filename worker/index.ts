import { assets } from '../shared/assets.ts';
import { convert, decimal } from '../shared/decimal.ts';
import { displayPlaces } from '../shared/display-precision.ts';
interface Env { ASSETS: { fetch(request: Request): Promise<Response> } }
interface Quote { code: string; rate: string; date: string; source: string; fetchedAt: string; cached?: boolean }
const json = (body: unknown, status = 200) => Response.json(body, { status, headers: { 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff' } });
export async function quote(code: string, request: Request): Promise<Quote> {
  const asset = assets.find(a => a.code === code);
  if (!asset) throw new Error('Unsupported asset');
  if (code === 'USD') return { code, rate: '1', date: new Date().toISOString().slice(0, 10), source: 'USD reference base', fetchedAt: new Date().toISOString() };
  const parent = code === 'SAT' ? 'BTC' : code === 'DROP' ? 'XRP' : code;
  const cacheKey = new Request(new URL('/_rates/' + parent, request.url));
  const cache = (globalThis as any).caches?.default;
  let result: Quote | undefined;
  try {
    const saved = await cache?.match(cacheKey);
    if (saved) result = { ...await saved.json() as Quote, cached: true };
  } catch (error) {
    console.warn('Rate cache read failed', { asset: parent, error: String(error) });
  }
  if (!result) {
    const endpoint = `https://api.frankfurter.dev/v2/rate/USD/${parent}?expand=providers${asset.kind === 'crypto' ? '&providers=ESTV' : ''}`;
    const response = await fetch(endpoint, { signal: AbortSignal.timeout(10000), headers: { Accept: 'application/json' }, redirect: 'manual' });
    if (!response.ok) throw new Error(`Frankfurter returned HTTP ${response.status}`);
    const raw = await response.text();
    if (raw.length > 65536) throw new Error('Invalid provider response');
    const data = JSON.parse(raw);
    if (data.base !== 'USD' || data.quote !== parent || !/^\d{4}-\d{2}-\d{2}$/.test(data.date)) throw new Error('Invalid provider pair');
    const age = Date.now() - Date.parse(data.date + 'T00:00:00Z');
    if (!Number.isFinite(age) || age < -86400000 || age > 7 * 86400000) throw new Error('No recent reference rate available');
    const rate = String(data.rate);
    if (decimal(rate)[0] <= 0n) throw new Error('Invalid provider rate');
    if (asset.kind === 'crypto' && !data.providers?.some((p: any) => p.key === 'ESTV' && !p.excluded)) throw new Error('Missing crypto reference source');
    result = { code: parent, rate, date: data.date, source: asset.kind === 'crypto' ? 'Swiss Federal Tax Administration (ESTV) via Frankfurter' : 'Frankfurter official-source reference rates', fetchedAt: new Date().toISOString() };
    try {
      await cache?.put(cacheKey, Response.json(result, { headers: { 'Cache-Control': 'public, max-age=900' } }));
    } catch (error) {
      console.warn('Rate cache write failed', { asset: parent, error: String(error) });
    }
  }
  return { ...result, code, rate: code === 'SAT' ? convert(result.rate, '1', '100000000', 24) : code === 'DROP' ? convert(result.rate, '1', '1000000', 24) : result.rate };
}
export default {
  async fetch(request: Request, env: Env): Promise<Response> {
    const url = new URL(request.url);
    if (!url.pathname.startsWith('/api/')) return env.ASSETS.fetch(request);
    if (request.method !== 'GET') return json({ error: 'Method not allowed' }, 405);
    if (url.pathname === '/api/health') return json({ status: 'ok' });
    if (url.pathname === '/api/assets') return json({ assets });
    if (url.pathname !== '/api/convert') return json({ error: 'Not found' }, 404);
    const from = url.searchParams.get('from') || 'USD', to = url.searchParams.get('to') || 'EUR', amount = url.searchParams.get('amount') || '1';
    try { decimal(amount); if (![from, to].every(code => assets.some(a => a.code === code))) throw new Error('Unsupported asset'); }
    catch (error) { return json({ error: (error as Error).message }, 400); }
    try {
      const quotes = await Promise.all([quote(from, request), quote(to, request)]);
      const target = assets.find(a => a.code === to)!;
      const places = displayPlaces(target);
      return json({ amount, from, to, result: convert(amount, quotes[0].rate, quotes[1].rate, places), quotes, referenceOnly: true });
    } catch (error) {
      console.error('Conversion failed', { from, to, error: String(error) });
      return json({ error: 'A recent reference rate is unavailable. Please try another pair or try again later.' }, 503);
    }
  }
};
