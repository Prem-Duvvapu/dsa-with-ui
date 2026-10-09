// Measure actual, bounded API executions. No raised limits or fabricated timing claims.
const fs = require('fs');
const path = require('path');
const assert = require('node:assert/strict');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const delay = ms => new Promise(resolve => setTimeout(resolve, ms));
let next = 0;
async function get(route) {
  const response = await fetch(`${api}${route}`);
  assert.equal(response.status, 200, route);
  return response.json();
}
async function execute(id, input, encoding) {
  await delay(Math.max(0, next - Date.now()));
  next = Date.now() + 1100;
  const response = await fetch(`${api}/api/problems/frog-jump/execute?approach=${id}&encoding=${encoding}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input)
  });
  const raw = await response.text();
  const body = JSON.parse(raw);
  assert.equal(response.status, 200, `${id}/${encoding}: ${raw.slice(0, 180)}`);
  assert.equal(body.problemId, 'frog-jump');
  assert.equal(body.approachId, id);
  assert.equal(body.truncated, false);
  assert.deepEqual(body.resolvedInput, input);
  return { body, bytes: Buffer.byteLength(raw) };
}
// Enumerate forward jump paths, independently of backward memo/table states.
function reference(heights, at = 0, spent = 0) {
  if (at === heights.length - 1) return spent;
  let best = Infinity;
  for (let d = 1; d <= 2 && at + d < heights.length; d++)
    best = Math.min(best, reference(heights, at + d, spent + Math.abs(heights[at + d] - heights[at])));
  return best;
}
function naiveCalls(index) { return index === 0 ? 1 : 1 + naiveCalls(index - 1) + (index > 1 ? naiveCalls(index - 2) : 0); }
(async () => {
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const { buildRecursionTree } = await import(path.resolve(__dirname, '../../../frontend/src/trace/recursionTree.js'));
  const problem = await get('/api/problems/frog-jump');
  assert.equal(problem.defaultApproachId, 'canonical');
  assert.deepEqual(problem.approaches.map(a => a.id), ['recursion', 'memoization', 'canonical']);
  const fixture = { problem, runs: {} };
  const rows = [];
  for (const option of problem.approaches) {
    const id = option.id;
    const max = option.inputSpec.fields[0].constraints.maxLength;
    const lengths = id === 'recursion' ? Array.from({ length: max - 1 }, (_, i) => i + 2) : [2, max];
    const inputs = [problem.inputSpec.fields[0].defaultValue, ...lengths.map(length =>
      Array.from({ length }, (_, i) => i % 2 === 0 ? 0 : 999)), Array(7).fill(5)];
    for (let i = 0; i < inputs.length; i++) {
      const heights = inputs[i];
      const input = { heights };
      const full = await execute(id, input, 'full');
      const delta = await execute(id, input, 'delta');
      const steps = decodeTrace(delta.body);
      // Normalize absent vs explicit-null empty fields; retain every actual value and frame.
      const normalized = value => JSON.parse(JSON.stringify(value, (_, item) => item === null ? undefined : item));
      assert.deepEqual(normalized(steps), normalized(full.body.steps));
      const vars = steps.at(-1).variables;
      assert.equal(vars.answer, String(reference(heights)));
      assert.deepEqual(steps.at(-1).callStack, []);
      if (i === 0) fixture.runs[id] = full.body;
      let tree = null;
      if (id !== 'canonical') {
        assert.equal(vars.calls, String(id === 'recursion' ? naiveCalls(heights.length - 1) : 2 * heights.length - 2));
        assert.equal(vars.cacheHits, String(id === 'recursion' ? 0 : heights.length - 2));
        assert.equal(vars.computedStates, String(heights.length));
        tree = buildRecursionTree(steps, steps.length - 1);
        assert.equal(tree.truncated, false, 'Every allowed call must fit the existing 220-node renderer cap');
        assert.equal(tree.nodes.length, Number(vars.calls));
      }
      rows.push({ approach: id, heights, steps: steps.length, answer: vars.answer,
        calls: vars.calls ?? null, evaluations: vars.stateEvaluations ?? null,
        computedStates: vars.computedStates ?? null, cacheHits: vars.cacheHits ?? null,
        fullJsonBytes: full.bytes, deltaJsonBytes: delta.bytes, fullDeltaEqual: true,
        maxSteps: full.body.maxSteps, maxBytes: full.body.maxBytes, truncated: false,
        renderedCalls: tree?.nodes.length ?? null, depth: Math.max(...steps.map(s => s.callStack?.length ?? 0)) });
      console.log(`PASS ${id} length${heights.length} answer${vars.answer} events${steps.length}`);
    }
  }
  const output = { generated: new Date().toISOString(), stats: await get('/api/problems/stats'), rows,
    scope: 'Frog Jump only. Real API/full-delta bytes and call/step/stack budgets; not latency, all-DP or device certification.' };
  fs.writeFileSync(path.join(__dirname, 'frog-jump-measurements.json'), JSON.stringify(output, null, 2) + '\n');
  // Integration fixtures are generated from complete real responses, never a hand-written synthetic trace.
  const fixturePath = path.resolve(__dirname, '../../../frontend/src/test/frogApproaches.json');
  fs.mkdirSync(path.dirname(fixturePath), { recursive: true });
  fs.writeFileSync(fixturePath, JSON.stringify(fixture, null, 2) + '\n');
})().catch(error => { console.error(error); process.exitCode = 1; });
