import { describe, expect, it } from 'vitest';
import { defaultInput, randomizeInput } from './randomizeInput';

const high = 1 - Number.EPSILON;
function seeded(seed) {
  let state = seed >>> 0;
  return () => ((state = (Math.imul(state, 1664525) + 1013904223) >>> 0) / 2 ** 32);
}
// Exact uncached tree enumeration, independent of the production saturated stack and
// capacity binary search. Called only on inputs already generated within the limit.
function calls(coins, amount, n = coins.length) {
  if (n === 0 || amount === 0) return 1;
  return 1 + calls(coins, amount, n - 1) +
    (coins[n - 1] <= amount ? calls(coins, amount - coins[n - 1], n) : 0);
}
const forms = [
  ...['minimum-coins-dp', 'coin-change-2'].flatMap(id => ['canonical', 'memoization', 'recursion'].map(form => [id, form])),
  ...['subset-sum-equal-target', 'count-subsets-with-sum-k'].flatMap(id => ['canonical', 'memoization'].map(form => [id, form]))
];
function fixture(id, form) {
  const coin = id.includes('coin'), count = id === 'count-subsets-with-sum-k';
  const array = coin ? 'coins' : 'nums', scalar = coin ? 'amount' : count ? 'k' : 'target';
  const recursion = form === 'recursion';
  return { fields: [
    { name: array, type: 'INT_ARRAY', defaultValue: coin ? [1, 2, 5] : count ? [1, 2, 2, 3] : [1, 2, 3, 7],
      constraints: { minLength: 1, maxLength: coin ? 8 : 10,
        minValue: coin || count ? 1 : 0, maxValue: coin ? 25 : count ? 15 : 20,
        ...(id === 'coin-change-2' ? { requireDistinct: true } : {}) } },
    { name: scalar, type: 'INT', defaultValue: coin ? id === 'minimum-coins-dp' ? 11 : 5 : count ? 3 : 6,
      constraints: { min: coin ? 1 : 0, max: count ? 60 : 40 } }
  ], workLimits: [{ kind: recursion ? 'SKIP_TAKE_CALLS' : 'CAPACITY_TABLE_CELLS',
    arrayField: array, scalarField: scalar, limit: recursion ? 220 : form === 'canonical' ? 225 : 80,
    reuse: recursion, errorField: form === 'canonical' ? scalar : array,
    message: 'Use a smaller visualizer input.' }] };
}
function assertContract(spec, input) {
  const [arrayField, scalarField] = spec.fields, values = input[arrayField.name], scalar = input[scalarField.name];
  const a = arrayField.constraints, s = scalarField.constraints, rule = spec.workLimits[0];
  expect(values.length).toBeGreaterThanOrEqual(a.minLength);
  expect(values.length).toBeLessThanOrEqual(a.maxLength);
  expect(values.every(value => Number.isInteger(value) && value >= a.minValue && value <= a.maxValue)).toBe(true);
  if (a.requireDistinct) expect(new Set(values).size).toBe(values.length);
  expect(Number.isInteger(scalar) && scalar >= s.min && scalar <= s.max).toBe(true);
  const work = rule.kind === 'CAPACITY_TABLE_CELLS' ? (values.length + 1) * (scalar + 1) : calls(values, scalar);
  expect(work).toBeLessThanOrEqual(rule.limit);
}

describe('coin/subset work-limit declaration fixtures', () => {
  // These fixtures exercise the existing generic work-limit support. Actual served
  // declarations are independently checked by backend and packaged-API evidence.
  it.each(forms)('keeps %s/%s runnable across seeded samples and repeated RNG', (id, form) => {
    const spec = fixture(id, form), before = JSON.stringify(spec);
    for (let seed = 1; seed <= 30; seed++) {
      const rng = seeded(seed);
      for (let draw = 0; draw < 10; draw++) assertContract(spec, randomizeInput(spec, rng));
    }
    for (const value of [0, high]) assertContract(spec, randomizeInput(spec, () => value));
    expect(JSON.stringify(spec)).toBe(before);
    expect(defaultInput(spec)).toEqual(Object.fromEntries(spec.fields.map(field => [field.name, field.defaultValue])));
  });

  it.each(forms)('uses a feasible maximum scalar for %s/%s at the high draw', (id, form) => {
    const spec = fixture(id, form), input = randomizeInput(spec, () => high);
    const [a, s] = spec.fields, rule = spec.workLimits[0], values = input[a.name];
    expect(values).toHaveLength(a.constraints.maxLength);
    assertContract(spec, input);
    if (rule.kind === 'CAPACITY_TABLE_CELLS') {
      expect(input[s.name]).toBe(Math.min(s.constraints.max, Math.floor(rule.limit / (values.length + 1)) - 1));
    } else {
      // High denominations allow the original amount40; no blanket table-area cap.
      expect(input.amount).toBe(40);
      expect(calls(values, 40)).toBeLessThanOrEqual(220);
    }
  });

  it.each(['subset-sum-equal-target', 'count-subsets-with-sum-k'])('retains the scalar zero base case for %s', id => {
    for (const form of ['canonical', 'memoization']) {
      const spec = fixture(id, form), input = randomizeInput(spec, () => 0);
      expect(input[id === 'subset-sum-equal-target' ? 'target' : 'k']).toBe(0);
      expect(input.nums).toEqual([id === 'subset-sum-equal-target' ? 0 : 1]);
      assertContract(spec, input);
    }
  });

  it('defers an amount listed before its distinct array without losing declaration order semantics', () => {
    const spec = fixture('coin-change-2', 'memoization');
    const reversed = { ...spec, fields: [...spec.fields].reverse() };
    const normal = randomizeInput(spec, () => high), scalarFirst = randomizeInput(reversed, () => high);
    expect(scalarFirst).toEqual(normal);
    expect(normal.amount).toBe(7);
    expect(new Set(normal.coins).size).toBe(8);
  });
});
