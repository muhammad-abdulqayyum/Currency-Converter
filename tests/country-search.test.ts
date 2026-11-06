import assert from 'node:assert/strict';
import test from 'node:test';
import dictionary from '../shared/countries/country-currencies.json' with { type: 'json' };
import { countryCurrencies, normalizeSearch } from '../web/src/country-search.ts';

const search = (query: string) => [...countryCurrencies(normalizeSearch(query))].sort();

test('matches country names, complete ISO codes and accent variants', () => {
  for (const query of ['Italy', 'IT', 'ita', 'Spain', 'España', 'Espana', ' ES ', 'ESP']) {
    assert.deepEqual(search(query), ['EUR'], query);
  }
  for (const query of ['United States', 'US', 'USA', 'Puerto Rico', 'PRI']) {
    assert.deepEqual(search(query), ['USD'], query);
  }
  for (const query of ['North Ireland', 'Northern Ireland']) assert.deepEqual(search(query), ['GBP']);
  assert.deepEqual(search('Ireland'), ['EUR']);
  assert.deepEqual(search('PAN'), ['PAB', 'USD']);
  for (const query of ['Palestine', 'Palestinian Territories', 'PS', 'PSE']) {
    assert.deepEqual(search(query), ['ILS', 'JOD', 'USD'], query);
  }
  assert.deepEqual(search(''), []);
});

test('covers every ISO entry and keeps shared currency matches unique', () => {
  assert.equal(Object.keys(dictionary).length, 249);
  for (const [alpha2, country] of Object.entries(dictionary)) {
    for (const code of [alpha2, country.alpha3]) {
      assert.ok(country.currencies.every(currency => search(code).includes(currency)), code);
    }
  }
  assert.equal(search('land').filter(code => code === 'EUR').length, 1);
});
