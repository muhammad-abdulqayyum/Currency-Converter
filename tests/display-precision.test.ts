import { test } from 'node:test';
import assert from 'node:assert/strict';
import { displayPlaces, minimumDisplayUnit, displayConversionResult } from '../shared/display-precision.ts';
import type { Asset } from '../shared/assets.ts';

const asset = (code: string, kind: Asset['kind']): Pick<Asset, 'code' | 'kind'> => ({ code, kind });
const display = (amount: string, to: Pick<Asset, 'code' | 'kind'>, result: string, fromRate = '1', toRate = '1') =>
  displayConversionResult({ amount, result, quotes: [{ rate: fromRate }, { rate: toRate }] }, to);

test('world uses international currency minor units', () => {
  for (const code of ['USD', 'EUR', 'GBP']) assert.equal(displayPlaces(asset(code, 'world')), 2);
  for (const code of ['JPY', 'KRW', 'IRR', 'VND', 'XOF']) assert.equal(displayPlaces(asset(code, 'world')), 0);
  for (const code of ['BHD', 'KWD', 'JOD', 'OMR', 'LYD']) assert.equal(displayPlaces(asset(code, 'world')), 3);
  assert.equal(displayPlaces(asset('XAU', 'metal')), 8);
  assert.equal(displayPlaces(asset('XAG', 'metal')), 8);
  for (const code of ['BTC', 'ETH', 'XRP', 'LTC', 'BCH']) assert.equal(displayPlaces(asset(code, 'crypto')), 8);
  for (const code of ['SAT', 'DROP']) assert.equal(displayPlaces(asset(code, 'crypto')), 0);
});

test('minimum units are formatted without floating point', () => {
  assert.equal(minimumDisplayUnit(0), '1');
  assert.equal(minimumDisplayUnit(2), '0.01');
  assert.equal(minimumDisplayUnit(3), '0.001');
  assert.equal(minimumDisplayUnit(8), '0.00000001');
  assert.throws(() => minimumDisplayUnit(-1));
});

test('world conversion honors exact zero and BHD / JPY / IRR precision', () => {
  assert.equal(display('0', asset('USD', 'world'), '0'), '0');
  assert.equal(display('0.005', asset('USD', 'world'), '0.01'), '< 0.01');
  assert.equal(display('0.01', asset('USD', 'world'), '0.01'), '0.01');
  assert.equal(display('0.005', asset('BHD', 'world'), '0.005'), '0.005');
  assert.equal(display('0.0005', asset('BHD', 'world'), '0.001'), '< 0.001');
  assert.equal(display('0.5', asset('JPY', 'world'), '1'), '< 1');
  assert.equal(display('0.9', asset('IRR', 'world'), '1'), '< 1');
  assert.equal(display('1', asset('JPY', 'world'), '1'), '1');
});

test('metals and parent cryptos show a strict small positive bound', () => {
  for (const code of ['XAU', 'XAG', 'XPT', 'XPD']) {
    assert.equal(display('0.0000000098', asset(code, 'metal'), '0.00000001'), '< 0.00000001');
    assert.equal(display('0.00000001', asset(code, 'metal'), '0.00000001'), '0.00000001');
  }
  for (const code of ['BTC', 'ETH', 'XRP', 'LTC', 'BCH']) {
    assert.equal(display('0.000000001', asset(code, 'crypto'), '0'), '< 0.00000001');
  }
  assert.equal(display('1', asset('XAU', 'metal'), '0', '200000000', '1'), '< 0.00000001');
});

test('SAT and DROP use whole-unit bounds', () => {
  for (const code of ['SAT', 'DROP']) {
    assert.equal(display('0.5', asset(code, 'crypto'), '1'), '< 1');
    assert.equal(display('1', asset(code, 'crypto'), '1'), '1');
    assert.equal(display('0', asset(code, 'crypto'), '0'), '0');
  }
});

test('unexpected quotes fail safely without changing the rounded result', () => {
  const to = asset('USD', 'world');
  assert.equal(displayConversionResult({ amount: '1', result: '2', quotes: [] }, to), '2');
  assert.equal(display('1', to, '2', '0', '1'), '2');
  assert.equal(display('oops', to, '2'), '2');
});
