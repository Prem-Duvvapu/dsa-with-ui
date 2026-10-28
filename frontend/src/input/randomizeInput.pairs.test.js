import { describe, expect, it } from 'vitest';
import { defaultInput, randomizeInput, randomizeValue } from './randomizeInput';

function seeded(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

function sequence(values) {
  let index = 0;
  return () => {
    if (index === values.length) throw new Error('Unexpected extra random draw');
    return values[index++];
  };
}

function list(name, constraints = {}, defaultValue = [1, 2]) {
  return { name, type: 'INT_ARRAY', defaultValue, constraints };
}

function freezeDeep(value) {
  if (value && typeof value === 'object') {
    Object.values(value).forEach(freezeDeep);
    Object.freeze(value);
  }
  return value;
}

describe('randomizeInput declared equalLengthGroups', () => {
  const fields = [
    list('weights', { minLength: 1, maxLength: 10, minValue: 1, maxValue: 20 }),
    list('values', { minLength: 1, maxLength: 10, minValue: 1, maxValue: 100 }),
    { name: 'capacity', type: 'INT', constraints: { min: 1, max: 20 }, defaultValue: 5 }
  ];

  it('repairs the recorded generated-length mismatch by sampling one declared common length', () => {
    const draws = [0.125, 0.1, 0.1, 0.25, 0.3, 0.4, 0.5, 0];
    expect(randomizeInput({ fields }, sequence(draws))).toEqual({
      weights: [3, 3], values: [31, 41, 51], capacity: 1
    });
    const generated = randomizeInput({ fields, equalLengthGroups: [['weights', 'values']] }, sequence(draws));
    expect(generated.weights).toHaveLength(2);
    expect(generated.values).toHaveLength(2);
    expect(generated.weights).toEqual([3, 3]);
    expect(generated.values).toEqual([26, 31]);
  });

  it.each([false, true])('honors member bounds and own value domains across seeds, reverse fields=%s', (reverse) => {
    const members = [
      list('items', { minLength: 2, maxLength: 7, minValue: -8, maxValue: -2 }),
      list('prices', { minLength: 4, maxLength: 12, minValue: 20, maxValue: 40 })
    ];
    const spec = { fields: reverse ? [...members].reverse() : members, equalLengthGroups: [['items', 'prices']] };
    const lengths = new Set();
    for (let seed = 1; seed <= 100; seed++) {
      const rng = seeded(seed);
      for (let draw = 0; draw < 10; draw++) {
        const { items, prices } = randomizeInput(spec, rng);
        expect(items).toHaveLength(prices.length);
        expect(items.length).toBeGreaterThanOrEqual(4);
        expect(items.length).toBeLessThanOrEqual(7);
        expect(items.every(value => Number.isInteger(value) && value >= -8 && value <= -2)).toBe(true);
        expect(prices.every(value => Number.isInteger(value) && value >= 20 && value <= 40)).toBe(true);
        lengths.add(items.length);
      }
    }
    expect([...lengths].sort()).toEqual([4, 5, 6, 7]);
  });

  it('supports three-member groups and independent disjoint pairs without sharing the wrong length', () => {
    const spec = { fields: [
      list('a', { minLength: 1, maxLength: 4 }),
      list('d', { minLength: 6, maxLength: 8 }),
      list('b', { minLength: 2, maxLength: 5 }),
      list('e', { minLength: 7, maxLength: 9 }),
      list('c', { minLength: 3, maxLength: 6 })
    ], equalLengthGroups: [['c', 'a', 'b'], ['d', 'e']] };
    for (let seed = 1; seed <= 100; seed++) {
      const result = randomizeInput(spec, seeded(seed));
      expect(result.a.length).toBe(result.b.length);
      expect(result.a.length).toBe(result.c.length);
      expect(result.a.length).toBeGreaterThanOrEqual(3);
      expect(result.a.length).toBeLessThanOrEqual(4);
      expect(result.d.length).toBe(result.e.length);
      expect(result.d.length).toBeGreaterThanOrEqual(7);
      expect(result.d.length).toBeLessThanOrEqual(8);
    }
  });

  it('uses the readability cap without shrinking declared validation maxima', () => {
    const spec = freezeDeep({ fields: [
      list('a', { minLength: 2, maxLength: 30 }),
      list('b', { minLength: 4, maxLength: 25 })
    ], equalLengthGroups: [['a', 'b']] });
    const result = randomizeInput(spec, () => 1 - Number.EPSILON);
    expect(result.a).toHaveLength(10);
    expect(result.b).toHaveLength(10);
    expect(spec.fields.map(field => field.constraints.maxLength)).toEqual([30, 25]);
  });

  it('permits a common minimum above ten, including a member without a declared maximum', () => {
    const spec = { fields: [
      list('a', { minLength: 12, maxLength: 20 }),
      list('b', { minLength: 1 })
    ], equalLengthGroups: [['a', 'b']] };
    const result = randomizeInput(spec, () => 0);
    expect(result.a).toHaveLength(12);
    expect(result.b).toHaveLength(12);
  });

  it('preserves generated index order rather than sorting, padding, truncating or copying defaults', () => {
    const spec = { fields: [
      list('a', { minLength: 2, maxLength: 2, minValue: 1, maxValue: 9 }, [7, 7]),
      list('b', { minLength: 2, maxLength: 2, minValue: 10, maxValue: 19 }, [17, 17])
    ], equalLengthGroups: [['a', 'b']] };
    expect(randomizeInput(spec, sequence([0, 0.9, 0.1, 0.7, 0.2]))).toEqual({
      a: [9, 1], b: [17, 12]
    });
  });

  it('keeps per-field sorted and distinct generation constraints', () => {
    const spec = { fields: [
      list('a', { minLength: 3, maxLength: 6, minValue: -50, maxValue: 50, requireSorted: true }),
      list('b', { minLength: 4, maxLength: 6, minValue: 0, maxValue: 1000, requireDistinct: true })
    ], equalLengthGroups: [['a', 'b']] };
    for (let seed = 1; seed <= 40; seed++) {
      const result = randomizeInput(spec, seeded(seed));
      expect(result.a).toHaveLength(result.b.length);
      expect(result.a).toEqual([...result.a].sort((a, b) => a - b));
      expect(new Set(result.b).size).toBe(result.b.length);
    }
  });

  it('supports a zero-length intersection only when every member allows empty arrays', () => {
    const spec = { fields: [
      list('a', { minLength: 0, maxLength: 0 }),
      list('b', { minLength: 0, maxLength: 6 })
    ], equalLengthGroups: [['a', 'b']] };
    expect(randomizeInput(spec, () => 0.9)).toEqual({ a: [], b: [] });
  });

  it('allows an empty-only group with omitted minima, preserving readable nonempty samples otherwise', () => {
    const spec = { fields: [list('a', { maxLength: 0 }), list('b')], equalLengthGroups: [['a', 'b']] };
    expect(randomizeInput(spec, () => 0.9)).toEqual({ a: [], b: [] });
    const nonempty = randomizeInput({ fields: [list('a'), list('b')],
      equalLengthGroups: [['a', 'b']]
    }, () => 0);
    expect(nonempty.a).toHaveLength(1);
    expect(nonempty.b).toHaveLength(1);
  });

  it('leaves frozen fields, constraints, defaults and RNG object properties untouched', () => {
    const spec = freezeDeep({ fields, equalLengthGroups: [['weights', 'values']] });
    const before = JSON.stringify(spec);
    const rng = Object.freeze(seeded(13));
    const result = randomizeInput(spec, rng);
    expect(result.weights).toHaveLength(result.values.length);
    expect(JSON.stringify(spec)).toBe(before);
    expect(defaultInput(spec)).toEqual({ weights: [1, 2], values: [1, 2], capacity: 5 });
    expect(result.weights).not.toBe(spec.fields[0].defaultValue);
  });

  it('keeps ungrouped deterministic behavior identical to independent field generation', () => {
    const spec = { fields };
    const rng = seeded(83);
    const expected = Object.fromEntries(fields.map(field => [field.name, randomizeValue(field, rng)]));
    expect(randomizeInput(spec, seeded(83))).toEqual(expected);
    expect(randomizeInput({ ...spec, equalLengthGroups: [] }, seeded(83))).toEqual(expected);
  });

  it.each([
    null, undefined, {}, 'weights,values',
    [null], ['weights'], [[]], [['weights']], [['weights', 'weights']],
    [['weights', 'missing']], [['weights', 'capacity']], [['weights', 12]],
    [['weights', '']], [['weights', 'values'], ['values', 'other']]
  ].map(equalLengthGroups => ({ equalLengthGroups })))(
    'rejects explicit malformed declarations $equalLengthGroups before generating values', ({ equalLengthGroups }) => {
    let calls = 0;
    expect(() => randomizeInput({ fields: [...fields, list('other')], equalLengthGroups }, () => {
      calls++;
      return 0.5;
    })).toThrow(RangeError);
    expect(calls).toBe(0);
    }
  );

  it.each([
    [{ minLength: 6, maxLength: 8 }, { minLength: 1, maxLength: 5 }],
    [{ minLength: 5, maxLength: 4 }, {}],
    [{ minLength: -1, maxLength: 5 }, {}],
    [{ minLength: 1.5, maxLength: 5 }, {}],
    [{ minLength: 1, maxLength: Infinity }, {}]
  ])('refuses invalid or impossible declared length bounds %j', (first, second) => {
    expect(() => randomizeInput({ fields: [list('a', first), list('b', second)],
      equalLengthGroups: [['a', 'b']]
    }, seeded(2))).toThrow(RangeError);
  });
});
