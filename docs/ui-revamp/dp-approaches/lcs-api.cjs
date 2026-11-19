const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { cases, oracle } = require('./lcs-cases.cjs');
const { root, sourceHash, packageSnapshot } = require('../evidence/paired-input/api.cjs');
const id = cases[0].id;
const high = 1 - Number.EPSILON;
function seeded() { let state = 173; return () => { state = (Math.imul(state, 1664525) + 1013904223) >>> 0; return state / 2 ** 32; }; }
function callCount({ first, second }) {
  function count(i, j) {
    if (i === 0 || j === 0) return 1;
    return first[i - 1] === second[j - 1] ? 1 + count(i - 1, j - 1) : 1 + count(i - 1, j) + count(i, j - 1);
  }
  return count(first.length, second.length);
}
async function main() {
  const base = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
  const out = path.join(__dirname, 'lcs'); fs.mkdirSync(out, { recursive: true });
  const localPackage = packageSnapshot(), samplerFile = path.join(root, 'frontend/src/input/randomizeInput.js');
  const samplerHash = sourceHash(fs.readFileSync(samplerFile));
  const { randomizeInput } = await import(pathToFileURL(samplerFile).href);
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  const rows = [], specifications = []; let requests = 0;
  async function request(route, input) {
    if (route.includes('/execute')) { requests++; await new Promise(resolve => setTimeout(resolve, 1100)); }
    const response = await fetch(base + route, input === undefined ? undefined : { method: 'POST',
      headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input) });
    const text = await response.text(); assert.equal(response.status, 200, `${route}: ${text.slice(0, 400)}`);
    return { body: JSON.parse(text), bytes: Buffer.byteLength(text) };
  }
  for (const form of ['recursion', 'memoization', 'canonical']) {
    const detail = (await request(`/api/problems/${id}?approach=${form}`)).body;
    const spec = (await request(`/api/problems/${id}/input-spec?approach=${form}`)).body;
    assert.deepEqual(spec, detail.inputSpec); assert.equal(detail.approachId, form);
    assert.deepEqual(spec.fields.map(field => field.name), ['first', 'second']);
    for (const field of spec.fields) {
      assert.equal(field.type, 'STRING'); assert.equal(field.constraints.minLength, 1);
      assert.equal(field.constraints.maxLength, form === 'recursion' ? 4 : form === 'memoization' ? 8 : 12);
      assert.equal(field.constraints.pattern, '[a-z]+');
    }
    const defaults = Object.fromEntries(spec.fields.map(field => [field.name, field.defaultValue]));
    assert.deepEqual(defaults, cases[0].defaults[form]);
    specifications.push({ form, inputSpec: spec, sourceHash: sourceHash(detail.javaCode) });
    for (const [profile, rng] of [['high', () => high], ['low', () => 0], ['seeded', seeded()]]) {
      const input = randomizeInput(spec, rng);
      for (const field of spec.fields) {
        const text = input[field.name]; assert(/^[a-z]+$/.test(text));
        assert(text.length >= field.constraints.minLength && text.length <= field.constraints.maxLength);
      }
      const route = `/api/problems/${id}/execute?approach=${form}`;
      const full = await request(`${route}&encoding=full`, input), delta = await request(`${route}&encoding=delta`, input);
      const decoded = decodeTrace(delta.body), steps = decodeTrace(full.body);
      const normalized = steps.map((step, index) => Object.fromEntries([...new Set([...Object.keys(step), ...Object.keys(decoded[index] ?? {})])]
        .map(key => [key, step[key] === undefined ? null : step[key]])));
      assert.deepEqual(normalized, decoded);
      for (const run of [full.body, delta.body]) {
        assert.equal(run.problemId, id); assert.equal(run.approachId, form); assert.equal(run.code, detail.javaCode);
        assert.equal(run.dsType, detail.dsType); assert.deepEqual(run.complexity, detail.complexity);
        assert.deepEqual(run.resolvedInput, input); assert.equal(run.truncated, false);
      }
      const expected = oracle(id, input), final = steps.at(-1); assert.equal(final.variables.answer, String(expected));
      assert.deepEqual(final.callStack, []);
      if (form === 'recursion') assert.equal(Number(final.variables.calls), callCount(input));
      if (form !== 'recursion') {
        assert.equal(final.dpTable.cells.length, input.first.length + 1);
        assert.equal(final.dpTable.cells[0].length, input.second.length + 1);
        for (const [i, row] of final.dpTable.cells.entries()) for (const [j, cell] of row.entries()) {
          if (cell.value === '·') { assert.equal(form, 'memoization'); assert.equal(cell.state, 'void'); }
          else assert.equal(Number(cell.value), oracle(id, { first: input.first.slice(0, i), second: input.second.slice(0, j) }));
        }
      }
      assert(full.bytes < 2_000_000 && delta.bytes < 2_000_000);
      rows.push({ form, profile, input, expected, steps: steps.length, fullBytes: full.bytes, deltaBytes: delta.bytes,
        sourceHash: sourceHash(full.body.code), actualFrontendSampler: true, fullDeltaEqual: true, independentAnswerAndKnownCells: true });
      console.log(`PASS LCS actual sampler ${form}/${profile}`);
    }
  }
  assert.equal(rows.length, 9); assert.equal(requests, 18); assert.deepEqual(packageSnapshot(), localPackage);
  assert.equal(sourceHash(fs.readFileSync(samplerFile)), samplerHash);
  fs.writeFileSync(path.join(out, 'sampler-api-results.json'), JSON.stringify({ generated: new Date().toISOString(),
    realBackend: true, complete: true, localPackage, samplerHash, requests, specifications, rows,
    limits: 'Three actual sampler profiles per LCS form; source compilation and exhaustive semantics are separate backend gates. Not all inputs, human/devices/performance/public rollout or project acceptance.' }, null, 2));
}
module.exports = { callCount };
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
