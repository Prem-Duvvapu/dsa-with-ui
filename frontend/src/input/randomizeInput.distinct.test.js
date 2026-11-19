import { describe, expect, it } from 'vitest';
import { defaultInput, randomizeInput, randomizeValue } from './randomizeInput';

const high = 1 - Number.EPSILON;
const distinct = (constraints = {}, type = 'INT_ARRAY') => ({ name: 'coins', type,
  defaultValue: [1, 2, 5], constraints: { minLength: 1, maxLength: 8,
    minValue: 1, maxValue: 25, requireDistinct: true, ...constraints } });
function seeded(seed) {
  let state = seed >>> 0;
  return () => ((state = (Math.imul(state, 1664525) + 1013904223) >>> 0) / 2 ** 32);
}
function assertDistinct(values, min, max) {
  expect(new Set(values).size).toBe(values.length);
  expect(values.every(value => Number.isInteger(value) && value >= min && value <= max)).toBe(true);
}

describe('constructive distinct-list sampling', () => {
  it.each([0, high])('never emits duplicate denominations under repeated RNG %s', value => {
    let draws = 0;
    const result = randomizeValue(distinct({ minLength: 8 }), () => { draws++; return value; });
    expect(result).toHaveLength(8);
    assertDistinct(result, 1, 25);
    expect(draws).toBe(9); // One length draw, then exactly one draw per selected value.
  });

  it.each(['INT_ARRAY', 'LINKED_LIST'])('fills the entire small signed domain for %s', type => {
    for (const value of [0, 0.5, high]) {
      const result = randomizeValue(distinct({ minLength: 5, maxLength: 5,
        minValue: -2, maxValue: 2 }, type), () => value);
      expect([...result].sort((a, b) => a - b)).toEqual([-2, -1, 0, 1, 2]);
    }
  });

  it('intersects sampled length with cardinality without changing declared bounds', () => {
    const field = distinct({ maxLength: 8, minValue: -1, maxValue: 1 });
    const before = JSON.stringify(field);
    const result = randomizeValue(field, () => high);
    expect(result).toHaveLength(3);
    assertDistinct(result, -1, 1);
    expect(JSON.stringify(field)).toBe(before);
  });

  it('retains sorted distinct semantics and permits the integer zero', () => {
    const result = randomizeValue(distinct({ minLength: 5, maxLength: 5,
      minValue: -2, maxValue: 2, requireSorted: true }), () => high);
    expect(result).toEqual([-2, -1, 0, 1, 2]);
  });

  it.each([0, high])('samples the full signed int32 domain without allocating the domain: %s', value => {
    let draws = 0;
    const result = randomizeValue(distinct({ minLength: 10, maxLength: 10,
      minValue: -2147483648, maxValue: 2147483647 }), () => { draws++; return value; });
    expect(result).toHaveLength(10);
    assertDistinct(result, -2147483648, 2147483647);
    expect(draws).toBe(11);
  });

  it('honors a minimum beyond the readability preference when the domain permits it', () => {
    const result = randomizeValue(distinct({ minLength: 12, maxLength: 20 }), () => high);
    expect(result).toHaveLength(12);
    assertDistinct(result, 1, 25);
  });

  it('keeps an empty-only domain when the minimum length is omitted', () => {
    const field = distinct({ minLength: undefined, maxLength: 0 });
    expect(randomizeValue(field, () => high)).toEqual([]);
    expect(randomizeInput({ fields: [field] }, () => high)).toEqual({ coins: [] });
  });

  it('does not let an omitted maximum override a distinct minimum above ten', () => {
    const field = distinct({ minLength: 12, maxLength: undefined });
    const result = randomizeValue(field, () => high);
    expect(result).toHaveLength(12);
    assertDistinct(result, 1, 25);
  });

  it.each([
    { minLength: 4, maxLength: 8, minValue: -1, maxValue: 1 },
    { minLength: 1, maxLength: 8, minValue: 3, maxValue: 2 },
    { minValue: 1.5 }, { maxValue: Infinity }, { minValue: '1' },
    { minLength: 1.5 }, { maxLength: -1 }
  ])('rejects impossible or malformed distinct constraints before drawing: %j', constraints => {
    let draws = 0;
    expect(() => randomizeValue(distinct(constraints), () => { draws++; return high; })).toThrow(RangeError);
    expect(draws).toBe(0);
  });

  it('validates a later distinct field before any earlier field can consume RNG', () => {
    let draws = 0;
    const spec = { fields: [{ name: 'first', type: 'INT', constraints: { min: 0, max: 1 } },
      distinct({ minLength: 4, minValue: 1, maxValue: 3 })] };
    expect(() => randomizeInput(spec, () => { draws++; return high; })).toThrow(RangeError);
    expect(draws).toBe(0);
  });

  it('intersects a paired shared length with the distinct member domain', () => {
    const spec = { fields: [distinct({ minValue: 1, maxValue: 3 }),
      { name: 'values', type: 'INT_ARRAY', constraints: { minLength: 1, maxLength: 8,
        minValue: 5, maxValue: 9 } }], equalLengthGroups: [['coins', 'values']] };
    const result = randomizeInput(spec, () => high);
    expect(result.coins).toHaveLength(3);
    expect(result.values).toEqual([9, 9, 9]);
    assertDistinct(result.coins, 1, 3);
  });

  it('refuses a paired minimum with no feasible distinct sample before drawing', () => {
    let draws = 0;
    const spec = { fields: [distinct({ minValue: 1, maxValue: 3 }),
      { name: 'values', type: 'INT_ARRAY', constraints: { minLength: 4, maxLength: 8,
        minValue: 5, maxValue: 9 } }], equalLengthGroups: [['coins', 'values']] };
    expect(() => randomizeInput(spec, () => { draws++; return high; })).toThrow(RangeError);
    expect(draws).toBe(0);
  });

  it('preserves deterministic seeded results and never mutates the spec or its defaults', () => {
    const spec = { fields: [distinct({ minLength: 8 })] };
    const before = JSON.stringify(spec);
    expect(randomizeInput(spec, seeded(71))).toEqual(randomizeInput(spec, seeded(71)));
    for (let seed = 1; seed <= 80; seed++) {
      const result = randomizeInput(spec, seeded(seed));
      assertDistinct(result.coins, 1, 25);
    }
    expect(defaultInput(spec)).toEqual({ coins: [1, 2, 5] });
    expect(JSON.stringify(spec)).toBe(before);
  });

  it('retains repeated values for an unflagged list and sparse-tree null semantics', () => {
    const field = distinct({ minLength: 4, maxLength: 4, requireDistinct: false });
    expect(randomizeValue(field, () => high)).toEqual([25, 25, 25, 25]);
    expect(randomizeValue({ type: 'BINARY_TREE', constraints: {
      minLength: 4, maxLength: 4, minValue: 0, maxValue: 0
    } }, () => 0)).toEqual([0, null, null, null]);
  });
});
