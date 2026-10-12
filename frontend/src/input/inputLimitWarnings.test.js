import { describe, expect, it } from 'vitest';
import { inputLimitWarnings } from './inputLimitWarnings';

const check = (type, value, constraints, label = 'Input') => inputLimitWarnings({ fields: [
  { name: 'value', label, type, constraints }
] }, { value }).map(w => w.message);

describe('declared input-limit hints, not a second validator', () => {
  it.each(['INT_ARRAY', 'LINKED_LIST', 'BINARY_TREE'])('explains preserved %s collection bounds', type => {
    expect(check(type, [1, 2, 3], { minLength: 1, maxLength: 2 }, 'Array'))
      .toEqual(['Array has 3 values; maximum 2.']);
    expect(check(type, [], { minLength: 1, maxLength: 2 }))
      .toEqual(['Input has 0 values; minimum 1.']);
  });

  it('handles string size using the declared bound rather than a problem-id rule', () => {
    expect(check('STRING', 'abcdefgh', { maxLength: 7 }))
      .toEqual(['Input has 8 characters; maximum 7.']);
    expect(check('STRING', 'a', { minLength: 2 })).toEqual(['Input has 1 character; minimum 2.']);
  });

  it('does not turn zero bounds, zero values or absent bounds into guessed defaults', () => {
    expect(check('INT', 1, { max: 0 })).toEqual(['Input is 1; maximum 0.']);
    expect(check('INT', -1, { min: 0 })).toEqual(['Input is -1; minimum 0.']);
    expect(check('INT', 0, { min: 0, max: 0 })).toEqual([]);
    expect(check('INT_ARRAY', [1], { maxLength: 0 })).toEqual(['Input has 1 value; maximum 0.']);
    expect(check('INT_ARRAY', [9999], {})).toEqual([]);
  });

  it('reports declared element ranges without treating empty fields or null nodes as zero', () => {
    expect(check('BINARY_TREE', [null, '', 0], { minValue: 1, maxValue: 2 }))
      .toEqual(['Input contains values below 1.']);
    expect(check('INT_ARRAY', [-2, 5], { minValue: -1, maxValue: 4 }))
      .toEqual(['Input contains values below -1.', 'Input contains values above 4.']);
  });

  it('checks every grid row rather than guessing dimensions from only its first row', () => {
    expect(check('INT_GRID', [[0], [0, 1, 2]], { maxRows: 1, minCols: 2, maxCols: 2 }))
      .toEqual(['Input has 2 rows; maximum 1.', 'Input has a row with 3 columns; maximum 2.',
        'Input has a row with 1 column; minimum 2.']);
    expect(check('INT_GRID', [[0, 3]], { minValue: 0, maxValue: 2 }))
      .toEqual(['Input contains values above 2.']);
  });

  it('leaves missing, malformed, grammar and joint-budget input to the server', () => {
    expect(inputLimitWarnings(null, null)).toEqual([]);
    expect(check('INT', '', { min: 1 })).toEqual([]);
    expect(check('INT_ARRAY', 'not an array', { maxLength: 2 })).toEqual([]);
    expect(check('INT_GRID', [null], { maxCols: 2 })).toEqual([]);
    expect(check('STRING', ')', { pattern: '[a-z()+-]*', maxLength: 5 })).toEqual([]);
    // Passing these individual hints must never be represented as accepted input.
    expect(check('GRAPH', { vertices: 100 }, { maxVertices: 2 })).toEqual([]);
  });

  it('never alters the draft or spec and reads only fields the selected spec declares', () => {
    const values = Object.freeze({ nums: Object.freeze([1, 2, 3]), extra: [1, 2, 3] });
    const spec = Object.freeze({ fields: [Object.freeze({ name: 'nums', type: 'INT_ARRAY',
      constraints: Object.freeze({ maxLength: 2 }) })] });
    expect(inputLimitWarnings(spec, values)).toEqual([{ field: 'nums', message: 'nums has 3 values; maximum 2.' }]);
    expect(values.nums).toEqual([1, 2, 3]);
  });
});
