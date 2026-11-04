import { test } from 'node:test';
import assert from 'node:assert/strict';
import { convert, decimal } from '../shared/decimal.ts';
test('decimal arithmetic preserves cents and half-up rounding',()=>{assert.equal(convert('0.1','1','3',2),'0.3');assert.equal(convert('1.005','1','1',2),'1.01');assert.equal(convert('100','0.8','1',2),'125');});
test('crypto and denominations retain precision',()=>{assert.equal(convert('1','1','0.00001234',8),'0.00001234');assert.equal(convert('0.00001234','1','100000000',24),'1234');});
test('invalid amounts and rates are rejected',()=>{for(const input of ['-1','NaN','Infinity','1e9','', '1.2.3'])assert.throws(()=>decimal(input));assert.throws(()=>convert('1','0','1'));});
