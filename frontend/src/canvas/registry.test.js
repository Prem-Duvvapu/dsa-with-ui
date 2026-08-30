import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { CANVAS_BY_DSTYPE } from './registry';

const contract = JSON.parse(readFileSync(
  resolve(process.cwd(), '../contracts/ds-types.json'),
  'utf8'
));

describe('canvas registry contract', () => {
  it('has exactly one routing entry for every backend dsType', () => {
    const backendWireValues = contract.map(({ wire }) => wire).sort();
    const registeredWireValues = Object.keys(CANVAS_BY_DSTYPE).sort();

    expect(registeredWireValues).toEqual(backendWireValues);
    for (const dsType of backendWireValues) {
      expect(CANVAS_BY_DSTYPE[dsType], `${dsType} has no canvas`).toBeTypeOf('function');
    }
  });
});
