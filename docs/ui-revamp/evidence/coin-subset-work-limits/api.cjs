// Real served metadata and actual frontend sampling, checked with independent answers.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { sourceHash, packageSnapshot, root } = require('../paired-input/api.cjs');

const owners = ['minimum-coins-dp', 'coin-change-2', 'subset-sum-equal-target', 'count-subsets-with-sum-k'];
const high = 1 - Number.EPSILON;
const isCoin = id => id === 'minimum-coins-dp' || id === 'coin-change-2';
const forms = id => isCoin(id) ? ['canonical', 'memoization', 'recursion'] : ['canonical', 'memoization'];
const fields = id => isCoin(id) ? ['coins', 'amount'] : ['nums', id === 'subset-sum-equal-target' ? 'target' : 'k'];

function rng(profile) {
  if (profile === 'high') return () => high;
  if (profile === 'low') return () => 0;
  let state = 617;
  return () => { state = (Math.imul(state, 1664525) + 1013904223) >>> 0; return state / 2 ** 32; };
}

// Expand the actual uncached recurrence, independently of both production counters.
function calls(coins, amount, stop = Infinity) {
  let total = 0;
  function visit(n, left) {
    if (total >= stop) return;
    total++;
    if (n === 0 || left === 0) return;
    visit(n - 1, left);
    if (coins[n - 1] <= left) visit(n, left - coins[n - 1]);
  }
  visit(coins.length, amount);
  return total;
}

function work(rule, input) {
  const values = input[rule.arrayField], scalar = input[rule.scalarField];
  if (rule.kind === 'CAPACITY_TABLE_CELLS') return (values.length + 1) * (scalar + 1);
  assert.equal(rule.kind, 'SKIP_TAKE_CALLS');
  assert.equal(rule.reuse, true);
  return calls(values, scalar, rule.limit + 1);
}

function oracle(id, input) {
  const [arrayName, scalarName] = fields(id), values = input[arrayName], target = input[scalarName];
  if (id === 'minimum-coins-dp') {
    // Breadth-first paths in the amount graph: each edge adds exactly one coin.
    const queue = [[0, 0]], visited = new Set([0]);
    for (let head = 0; head < queue.length; head++) {
      const [sum, depth] = queue[head];
      if (sum === target) return depth;
      for (const coin of values) {
        const next = sum + coin;
        if (next <= target && !visited.has(next)) { visited.add(next); queue.push([next, depth + 1]); }
      }
    }
    return -1;
  }
  if (id === 'coin-change-2') {
    // Enumerate complete denomination quantity vectors; order is never a permutation.
    let count = 0;
    function quantities(index, remaining) {
      if (index === values.length) { if (remaining === 0) count++; return; }
      for (let quantity = 0; quantity * values[index] <= remaining; quantity++)
        quantities(index + 1, remaining - quantity * values[index]);
    }
    quantities(0, target);
    return count;
  }
  let count = 0;
  for (let mask = 0; mask < 2 ** values.length; mask++) {
    let sum = 0;
    for (let i = 0; i < values.length; i++) if (mask & (1 << i)) sum += values[i];
    if (sum === target) count++;
  }
  return id === 'subset-sum-equal-target' ? count > 0 : count;
}

function assertSpec(id, form, spec) {
  const [arrayName, scalarName] = fields(id);
  assert.equal(spec.workLimits?.length, 1);
  const rule = spec.workLimits[0], recursion = form === 'recursion';
  assert.equal(rule.kind, recursion ? 'SKIP_TAKE_CALLS' : 'CAPACITY_TABLE_CELLS');
  assert.equal(rule.arrayField, arrayName); assert.equal(rule.scalarField, scalarName);
  assert.equal(rule.limit, recursion ? 220 : form === 'canonical' ? 225 : 80);
  assert.equal(rule.reuse, recursion);
  assert.equal(rule.errorField, form === 'canonical' ? scalarName : arrayName);
  assert.equal(typeof rule.message, 'string'); assert(rule.message.trim());
  const array = spec.fields.find(field => field.name === arrayName);
  const scalar = spec.fields.find(field => field.name === scalarName);
  assert.equal(array.type, 'INT_ARRAY'); assert.equal(scalar.type, 'INT');
  assert.equal(array.constraints.minLength, 1);
  assert.equal(array.constraints.maxLength, isCoin(id) ? 8 : 10);
  assert.equal(scalar.constraints.min, isCoin(id) ? 1 : 0);
  assert.equal(scalar.constraints.max, id === 'count-subsets-with-sum-k' ? 60 : 40);
  assert.equal(array.constraints.minValue, id === 'subset-sum-equal-target' ? 0 : 1);
  if (id === 'coin-change-2') assert.equal(array.constraints.requireDistinct, true);
  return rule;
}

function assertInput(spec, input) {
  assert.deepEqual(Object.keys(input).sort(), spec.fields.map(field => field.name).sort());
  for (const field of spec.fields) {
    const value = input[field.name], c = field.constraints;
    if (field.type === 'INT_ARRAY') {
      assert(Array.isArray(value)); assert(value.length >= c.minLength && value.length <= c.maxLength);
      assert(value.every(v => Number.isInteger(v) && v >= c.minValue && v <= c.maxValue));
      if (c.requireDistinct) assert.equal(new Set(value).size, value.length);
    } else {
      assert.equal(field.type, 'INT'); assert(Number.isInteger(value) && value >= c.min && value <= c.max);
    }
  }
  for (const rule of spec.workLimits) assert(work(rule, input) <= rule.limit);
}

function assertRun(run, steps, detail, input) {
  assert.equal(run.problemId, detail.id); assert.equal(run.approachId, detail.approachId);
  assert.equal(run.dsType, detail.dsType); assert.equal(run.code, detail.javaCode);
  assert.deepEqual(run.resolvedInput, input); assert.deepEqual(run.complexity, detail.complexity);
  assert.equal(run.truncated, false); assert(steps.length > 0); assert.equal(run.stepCount, steps.length);
  const anchors = Object.values(run.anchors), lineCount = run.code.split('\n').length;
  for (const [index, step] of steps.entries()) {
    assert.equal(step.stepNumber, index + 1); assert.equal(step.dsType, run.dsType);
    assert(anchors.includes(step.activeLine)); assert(step.activeLine > 0 && step.activeLine <= lineCount);
  }
  const expected = oracle(run.problemId, input), rawAnswer = steps.at(-1).variables.answer;
  const actual = typeof expected === 'boolean' ? rawAnswer === true || rawAnswer === 'true' : Number(rawAnswer);
  if (typeof expected === 'boolean') assert([true, false, 'true', 'false'].includes(rawAnswer));
  assert.equal(actual, expected);
  if (run.approachId === 'recursion') assert.equal(Number(steps.at(-1).variables.calls), calls(input.coins, input.amount));
  return { expected, actual };
}

function boundary(id, form, accepted) {
  const [arrayName, scalarName] = fields(id);
  if (form === 'recursion') return accepted ? { coins: [23, 6, 7, 15, 3, 11], amount: 23 }
    : { coins: [7, 23, 25, 6, 5, 20], amount: 31 };
  const length = form === 'canonical' || !accepted ? 8 : 7;
  return { [arrayName]: Array.from({ length }, (_, index) => id === 'coin-change-2' ? index + 1 : 1),
    [scalarName]: form === 'canonical' ? accepted ? 24 : 25 : accepted ? 9 : 8 };
}

function semantic(id) {
  if (isCoin(id)) return { coins: [2], amount: 3 };
  if (id === 'subset-sum-equal-target') return { nums: [0, 0, 2], target: 0 };
  return { nums: [1, 2, 2, 3], k: 3 };
}

async function main() {
  const base = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
  const out = path.resolve(process.env.COIN_SUBSET_EVIDENCE_DIR || __dirname);
  const localPackage = packageSnapshot();
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  const samplerFile = path.join(root, 'frontend/src/input/randomizeInput.js');
  const { randomizeInput } = await import(pathToFileURL(samplerFile).href);
  const samplerHash = sourceHash(fs.readFileSync(samplerFile));
  let next = 0, executionRequests = 0;
  async function request(route, input, expected = 200) {
    if (route.includes('/execute')) {
      executionRequests++; const due = Math.max(Date.now(), next); next = due + 1100;
      if (due > Date.now()) await new Promise(resolve => setTimeout(resolve, due - Date.now()));
    }
    const response = await fetch(base + route, input === undefined ? undefined : {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input)
    });
    const text = await response.text(); assert.equal(response.status, expected, `${route}: ${text.slice(0, 400)}`);
    return { body: JSON.parse(text), bytes: Buffer.byteLength(text) };
  }
  const rows = [], refusals = [], specifications = [], frontendMetadataControls = [], unchangedScopeControls = [];
  for (const id of owners) for (const form of forms(id)) {
    const detail = (await request(`/api/problems/${id}?approach=${form}`)).body;
    const spec = (await request(`/api/problems/${id}/input-spec?approach=${form}`)).body;
    assert.equal(detail.approachId, form); assert.deepEqual(detail.inputSpec, spec);
    const rule = assertSpec(id, form, spec);
    specifications.push({ id, form, workLimits: spec.workLimits, sourceHash: sourceHash(detail.javaCode) });
    for (const [label, workLimits] of [['unknown-kind', [{ ...rule, kind: 'UNKNOWN_WORK' }]],
      ['duplicate-scalar', [rule, { ...rule }]], ['wrong-scalar-type', [{ ...rule, scalarField: rule.arrayField }]]]) {
      let draws = 0;
      assert.throws(() => randomizeInput({ ...spec, workLimits }, () => { draws++; return 0.5; }));
      assert.equal(draws, 0); frontendMetadataControls.push({ id, form, label, rejectedBeforeRng: true });
    }
    const scenarios = ['seed', 'high', 'low'].map(profile => ({ label: `actual-generator-${profile}`, input: randomizeInput(spec, rng(profile)) }));
    scenarios.push({ label: 'semantic', input: semantic(id) }, { label: 'accepted-boundary', input: boundary(id, form, true) });
    for (const scenario of scenarios) {
      const { label, input } = scenario; assertInput(spec, input);
      if (label === 'accepted-boundary') assert.equal(work(rule, input), rule.limit);
      const route = `/api/problems/${id}/execute?approach=${form}`;
      const full = await request(`${route}&encoding=full`, input), delta = await request(`${route}&encoding=delta`, input);
      const fullSteps = decodeTrace(full.body), deltaSteps = decodeTrace(delta.body);
      const normalized = fullSteps.map((step, index) => Object.fromEntries([...new Set([...Object.keys(step), ...Object.keys(deltaSteps[index] ?? {})])]
        .map(key => [key, step[key] === undefined ? null : step[key]])));
      assert.deepEqual(normalized, deltaSteps);
      const answer = assertRun(full.body, fullSteps, detail, input); assertRun(delta.body, deltaSteps, detail, input);
      assert(full.bytes < 2_000_000 && delta.bytes < 2_000_000);
      rows.push({ id, form, label, input, work: work(rule, input), limit: rule.limit, ...answer,
        steps: fullSteps.length, fullBytes: full.bytes, deltaBytes: delta.bytes, sourceHash: sourceHash(full.body.code),
        actualSampler: label.startsWith('actual-generator'), independentWorkAndAnswer: true,
        fullDeltaDecodedEqual: true, complete: true });
      console.log(`PASS coin/subset API ${id}/${form}/${label}`);
    }
    const input = boundary(id, form, false), measured = work(rule, input);
    assert.equal(measured, form === 'canonical' ? 234 : rule.limit + 1);
    for (const encoding of ['full', 'delta']) {
      const failure = await request(`/api/problems/${id}/execute?approach=${form}&encoding=${encoding}`, input, 400);
      assert.equal(typeof failure.body.fieldErrors?.[rule.errorField], 'string'); assert.equal(failure.body.steps, undefined);
      refusals.push({ id, form, input, encoding, work: measured, limit: rule.limit, field: rule.errorField, expected400: true, noSteps: true });
    }
  }
  // Derived-target parity branches retain their own guard order. Do not infer a direct
  // scalar rule or reject the valid odd-total terminal answer merely for a large sum.
  const partition = (await request('/api/problems/partition-equal-subset-sum?approach=memoization')).body;
  assert.equal(partition.inputSpec.workLimits, undefined);
  const odd = { nums: [...Array(9).fill(20), 19] }, even = { nums: Array(10).fill(20) };
  for (const encoding of ['full', 'delta']) {
    const result = await request(`/api/problems/partition-equal-subset-sum/execute?approach=memoization&encoding=${encoding}`, odd);
    const steps = decodeTrace(result.body); assert.equal(steps.length, 1); assert.equal(result.body.truncated, false);
    assert.equal(String(steps[0].variables.answer), 'false'); assert.equal(Number(steps[0].variables.calls), 0);
    assert.deepEqual(result.body.resolvedInput, odd); assert.equal(result.body.code, partition.javaCode);
    const rejected = await request(`/api/problems/partition-equal-subset-sum/execute?approach=memoization&encoding=${encoding}`, even, 400);
    assert.equal(typeof rejected.body.fieldErrors?.nums, 'string'); assert.equal(rejected.body.steps, undefined);
    unchangedScopeControls.push({ id: partition.id, form: 'memoization', encoding, oddInput: odd, oddCompleteWithoutCalls: true,
      evenInput: even, evenDerivedTableStillRefused: true, workMetadataStillAbsent: true });
  }
  for (const id of ['subset-sum-equal-target', 'count-subsets-with-sum-k']) {
    const spec = (await request(`/api/problems/${id}/input-spec?approach=recursion`)).body;
    assert.equal(spec.workLimits, undefined); assert.equal(spec.fields.find(field => field.name === 'nums').constraints.maxLength, 6);
    unchangedScopeControls.push({ id, form: 'recursion', sixItemBoundUnchanged: true, workMetadataStillAbsent: true });
  }
  assert.equal(rows.length, 50); assert.equal(refusals.length, 20); assert.equal(executionRequests, 124);
  assert.deepEqual(packageSnapshot(), localPackage); assert.equal(sourceHash(fs.readFileSync(samplerFile)), samplerHash);
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'api-results.json'), JSON.stringify({ generated: new Date().toISOString(), realBackend: true,
    complete: true, localPackage, samplerHash, specifications, executionRequests, rows, refusals, frontendMetadataControls, unchangedScopeControls,
    limits: 'Ten declared direct-target/amount specifications; actual sampler profiles, semantic cases and exact boundaries. Not derived-target/parity budgets, all-input/source compilation, real devices, screen readers, performance, public rollout or whole-project acceptance.' }, null, 2));
}

module.exports = { root, owners, forms, fields, high, rng, calls, work, oracle, assertSpec, assertInput, assertRun, boundary, semantic, sourceHash, packageSnapshot };
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
