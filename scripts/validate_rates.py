#!/usr/bin/env python3
"""Read-only live feed check; never writes real prices into the app package."""
import datetime
import decimal
import json
import pathlib
import urllib.request

ROOT = pathlib.Path(__file__).resolve().parents[1]
CODES = [row.split('\t')[0] for row in (ROOT / 'project/app/src/main/assets/currencies.tsv').read_text().splitlines()]
SPECIAL = ['XAU', 'XAG', 'XPT', 'XPD', 'BTC', 'ETH', 'XRP', 'LTC', 'BCH']
with urllib.request.urlopen('https://api.frankfurter.dev/v2/rates?base=USD&expand=providers', timeout=30) as response:
    rates = {row['quote']: row for row in json.load(response, parse_float=decimal.Decimal)}
today = datetime.datetime.now(datetime.timezone.utc).date()
for code in CODES + SPECIAL:
    row = rates[code]
    assert row['base'] == 'USD' and row['rate'] > 0, code
    assert 0 <= (today - datetime.date.fromisoformat(row['date'])).days <= 7, code
    if code in ['BTC', 'ETH', 'XRP', 'LTC', 'BCH']:
        assert any(p['key'] == 'ESTV' and not p.get('excluded', False) for p in row['providers']), code
print(f'PASS: {len(CODES)} world currencies, four metals and five crypto assets have positive, recent USD rates.')
