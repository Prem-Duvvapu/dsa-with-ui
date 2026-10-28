import { describe, expect, it } from 'vitest';
import { defaultInput, randomizeInput } from './randomizeInput';

const high = 1 - Number.EPSILON;
function seeded(seed) {
  let state = seed >>> 0;
  return () => ((state = (1664525 * state + 1013904223) >>> 0) / 4294967296);
}
function rule(kind = 'CAPACITY_TABLE_CELLS', reuse = false, limit = 225) {
  return { kind, arrayField: 'weights', scalarField: 'capacity', limit, reuse,
    errorField: kind === 'CAPACITY_TABLE_CELLS' ? 'capacity' : 'weights', message: 'Choose a smaller visualizer input.' };
}
function spec(workLimit = rule(), overrides = {}) {
  return { fields: [
    { name: 'weights', type: 'INT_ARRAY', defaultValue: [1, 2, 4, 5], constraints: {
      minLength: 1, maxLength: 8, minValue: 1, maxValue: 20
    } },
    { name: 'values', type: 'INT_ARRAY', defaultValue: [5, 4, 8, 6], constraints: {
      minLength: 1, maxLength: 8, minValue: 1, maxValue: 100
    } },
    { name: 'capacity', type: 'INT', defaultValue: 5, constraints: { min: 1, max: 30 } }
  ], equalLengthGroups: [['weights', 'values']], workLimits: [workLimit], ...overrides };
}

// Deliberately recursive and exact: independent from the production iterative,
// saturated counter and binary search. Inputs are small enough to enumerate calls.
function calls(weights, capacity, reuse, n = weights.length) {
  if (n === 0 || capacity === 0) return 1;
  return 1 + calls(weights, capacity, reuse, n - 1) +
    (weights[n - 1] <= capacity ? calls(weights, capacity - weights[n - 1], reuse, reuse ? n : n - 1) : 0);
}
function cost(input, workLimit) {
  return workLimit.kind === 'CAPACITY_TABLE_CELLS'
    ? (input.weights.length + 1) * (input.capacity + 1)
    : calls(input.weights, input.capacity, workLimit.reuse);
}
function fixedWeights(inputSpec, weights, capacityDraw = high) {
  for (const field of inputSpec.fields.filter(field => field.type === 'INT_ARRAY')) {
    field.constraints.minLength = weights.length;
    field.constraints.maxLength = weights.length;
  }
  const draws = [0]; // The common length is chosen once, even at a fixed boundary.
  for (const field of inputSpec.fields.filter(field => field.type === 'INT_ARRAY')) {
    draws.push(...(field.name === 'weights' ? weights.map(weight => (weight - 1) / 20) : weights.map(() => 0)));
  }
  draws.push(capacityDraw);
  let cursor = 0;
  const generated = randomizeInput(inputSpec, () => {
    if (cursor === draws.length) throw new Error('Unexpected extra random draw');
    return draws[cursor++];
  });
  expect(generated.weights).toEqual(weights);
  expect(generated.values).toEqual(weights.map(() => 1));
  return generated;
}
function freezeDeep(value) {
  if (value && typeof value === 'object') {
    Object.values(value).forEach(freezeDeep);
    Object.freeze(value);
  }
  return value;
}

describe('randomizeInput declared workLimits', () => {
  it.each([
    ['0/1 tabulation', rule()],
    ['0/1 memoization', { ...rule('CAPACITY_TABLE_CELLS', false, 80), errorField: 'weights' }],
    ['unbounded tabulation', rule()],
    ['unbounded memoization', { ...rule('CAPACITY_TABLE_CELLS', false, 80), errorField: 'weights' }],
    ['0/1 recursion', rule('SKIP_TAKE_CALLS', false, 220)],
    ['unbounded recursion', rule('SKIP_TAKE_CALLS', true, 220)]
  ])('generates feasible %s inputs across many seeds without changing field domains', (_name, workLimit) => {
    const inputSpec = spec(workLimit);
    for (let seed = 1; seed <= 60; seed++) {
      const rng = seeded(seed);
      for (let draw = 0; draw < 15; draw++) {
        const generated = randomizeInput(inputSpec, rng);
        expect(generated.weights).toHaveLength(generated.values.length);
        expect(generated.weights.length).toBeGreaterThanOrEqual(1);
        expect(generated.weights.length).toBeLessThanOrEqual(8);
        expect(generated.weights.every(weight => Number.isInteger(weight) && weight >= 1 && weight <= 20)).toBe(true);
        expect(generated.values.every(value => Number.isInteger(value) && value >= 1 && value <= 100)).toBe(true);
        expect(generated.capacity).toBeGreaterThanOrEqual(1);
        expect(generated.capacity).toBeLessThanOrEqual(30);
        expect(cost(generated, workLimit)).toBeLessThanOrEqual(workLimit.limit);
      }
    }
  });

  it('samples the largest feasible table capacity: 225 cells fits, 234 does not', () => {
    const generated = fixedWeights(spec(), Array(8).fill(20));
    expect(generated.capacity).toBe(24);
    expect((8 + 1) * (generated.capacity + 1)).toBe(225);
    expect((8 + 1) * (generated.capacity + 2)).toBe(234);
  });

  it.each([
    [false, 80, [1, 1, 1, 2, 4, 4], 7, 80],
    [false, 80, [1, 1, 1, 2, 3, 3], 6, 81],
    [true, 220, [1, 2], 19, 220],
    [true, 220, [2, 2, 4, 1], 9, 221]
  ])('honors the exact call boundary reuse=%s limit=%s weights=%j', (reuse, limit, weights, ceiling, count) => {
    const workLimit = rule('SKIP_TAKE_CALLS', reuse, limit), inputSpec = spec(workLimit);
    inputSpec.fields[2].constraints.max = ceiling;
    expect(calls(weights, ceiling, reuse)).toBe(count);
    const feasible = Array.from({ length: ceiling }, (_, index) => index + 1)
      .filter(capacity => calls(weights, capacity, reuse) <= limit);
    expect(fixedWeights(inputSpec, weights).capacity).toBe(Math.max(...feasible));
  });

  it('counts ordered weights faithfully instead of sorting them', () => {
    expect(fixedWeights(spec(rule('SKIP_TAKE_CALLS', true, 220)), [1, 3]).capacity).toBe(23);
    expect(fixedWeights(spec(rule('SKIP_TAKE_CALLS', true, 220)), [3, 1]).capacity).toBe(22);
  });

  it.each([false, true])('retains cheap eight-item capacity30 recursion with reuse=%s', (reuse) => {
    const generated = fixedWeights(spec(rule('SKIP_TAKE_CALLS', reuse, 220)), Array(8).fill(20));
    expect(generated.capacity).toBe(30);
    expect(calls(generated.weights, generated.capacity, reuse)).toBeLessThanOrEqual(220);
  });

  it('defers capacity generation even when it appears before both paired arrays', () => {
    const inputSpec = spec(rule('SKIP_TAKE_CALLS', true, 220));
    inputSpec.fields = [inputSpec.fields[2], inputSpec.fields[1], inputSpec.fields[0]];
    const generated = fixedWeights(inputSpec, [3, 1]);
    expect(generated.capacity).toBe(22);
  });

  it('restricts the shared length before sampling when a fixed table capacity permits only six items', () => {
    const inputSpec = spec();
    inputSpec.fields[2].constraints.min = 30;
    const generated = randomizeInput(inputSpec, () => high);
    expect(generated.weights).toHaveLength(6);
    expect(generated.values).toHaveLength(6);
    expect(generated.capacity).toBe(30);
    expect(cost(generated, inputSpec.workLimits[0])).toBeLessThanOrEqual(225);
  });

  it.each(['CAPACITY_TABLE_CELLS', 'SKIP_TAKE_CALLS'])(
    'honors a declared zero capacity and its actual base cost for %s', kind => {
      const inputSpec = spec(rule(kind, false, 1));
      inputSpec.fields[0].constraints.minLength = 0;
      inputSpec.fields[1].constraints.minLength = 0;
      inputSpec.fields[2].constraints = { min: 0, max: 0 };
      const generated = randomizeInput(inputSpec, () => high);
      expect(generated.capacity).toBe(0);
      expect(cost(generated, inputSpec.workLimits[0])).toBe(1);
      if (kind === 'CAPACITY_TABLE_CELLS') {
        expect(generated.weights).toEqual([]);
        expect(generated.values).toEqual([]);
      }
    }
  );

  it('rejects ambiguous multiple limits for the same scalar before consuming randomness', () => {
    const inputSpec = spec(rule('SKIP_TAKE_CALLS', true, 220));
    inputSpec.workLimits.push(rule('CAPACITY_TABLE_CELLS', false, 60));
    let draws = 0;
    expect(() => randomizeInput(inputSpec, () => { draws++; return 0; })).toThrow(RangeError);
    expect(draws).toBe(0);
  });

  it('leaves metadata, defaults and frozen RNG properties unchanged', () => {
    const inputSpec = freezeDeep(spec(rule('SKIP_TAKE_CALLS', true, 220)));
    const before = JSON.stringify(inputSpec), defaults = defaultInput(inputSpec);
    const generated = randomizeInput(inputSpec, Object.freeze(seeded(11)));
    expect(cost(generated, inputSpec.workLimits[0])).toBeLessThanOrEqual(220);
    expect(JSON.stringify(inputSpec)).toBe(before);
    expect(defaultInput(inputSpec)).toEqual(defaults);
  });

  it('preserves deterministic sampling exactly when work limits are absent or empty', () => {
    const inputSpec = spec();
    delete inputSpec.workLimits;
    const draws = [0.125, 0.1, 0.1, 0.25, 0.3, 0.4];
    for (const declaration of [inputSpec, { ...inputSpec, workLimits: [] }]) {
      let cursor = 0;
      expect(randomizeInput(declaration, () => draws[cursor++])).toEqual({ weights: [3, 3], values: [26, 31], capacity: 13 });
    }
  });

  it.each([
    null, undefined, {}, [null], [rule('UNKNOWN')],
    [{ ...rule(), arrayField: 'missing' }], [{ ...rule(), arrayField: 'capacity' }],
    [{ ...rule(), scalarField: 'weights' }], [{ ...rule(), errorField: 'missing' }],
    [{ ...rule(), limit: 0 }], [{ ...rule(), limit: 1.5 }], [{ ...rule(), limit: 2147483648 }],
    [{ ...rule(), reuse: 'false' }], [{ ...rule(), reuse: true }], [{ ...rule(), message: '' }]
  ].map(workLimits => ({ workLimits })))(
    'rejects malformed work limit $workLimits before consuming randomness', ({ workLimits }) => {
      let draws = 0;
      expect(() => randomizeInput(spec(rule(), { workLimits }), () => { draws++; return 0; })).toThrow(RangeError);
      expect(draws).toBe(0);
    }
  );

  it.each([
    ['nonpositive recursive weights', inputSpec => { inputSpec.workLimits = [rule('SKIP_TAKE_CALLS', false, 80)]; inputSpec.fields[0].constraints.minValue = 0; }],
    ['noninteger recursive weights', inputSpec => { inputSpec.workLimits = [rule('SKIP_TAKE_CALLS', true, 220)]; inputSpec.fields[0].constraints.minValue = 0.5; }],
    ['invalid scalar minimum', inputSpec => { inputSpec.fields[2].constraints.min = -1; }],
    ['undeclared scalar maximum', inputSpec => { delete inputSpec.fields[2].constraints.max; }],
    ['undeclared array minimum', inputSpec => { delete inputSpec.fields[0].constraints.minLength; }],
    ['undeclared array maximum', inputSpec => { delete inputSpec.fields[0].constraints.maxLength; }],
    ['impossible table minimum', inputSpec => { inputSpec.workLimits[0].limit = 1; }],
    ['impossible recursive minimum', inputSpec => { inputSpec.workLimits = [rule('SKIP_TAKE_CALLS', true, 1)]; }]
  ])('rejects %s without generating an invalid draft', (_name, change) => {
    const inputSpec = spec(); change(inputSpec);
    let draws = 0;
    expect(() => randomizeInput(inputSpec, () => { draws++; return 0; })).toThrow(RangeError);
    expect(draws).toBe(0);
  });
});
