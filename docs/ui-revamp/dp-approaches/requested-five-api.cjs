// Independent transport evidence using the production decoder and normal execution rate limit.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { isDeepStrictEqual } = require('node:util');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const out = path.join(__dirname, 'requested-five');
const problems = ['longest-increasing-subsequence', 'stock-transaction-fee', 'min-insertions-palindrome', 'count-partitions-given-diff', 'ninja-and-his-friends'];
async function request(route, input) {
  if (route.includes('/execute')) await new Promise(resolve => setTimeout(resolve, 1100));
  const response = await fetch(api + route, input === undefined ? undefined : {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input)
  });
  const text = await response.text();
  assert.equal(response.status, 200, `${route}: ${text}`);
  return { body: JSON.parse(text), bytes: Buffer.byteLength(text) };
}
function maximum(problem, constraints) {
  const n = constraints.maxLength ?? constraints.maxRows;
  switch (problem) {
    case 'longest-increasing-subsequence': return { nums: Array.from({ length: n }, (_, i) => i) };
    case 'stock-transaction-fee': return { prices: Array.from({ length: n }, (_, i) => i % 2 ? 100 : 0), fee: 0 };
    case 'min-insertions-palindrome': return { text: 'abcdefghijkl'.slice(0, n) };
    case 'count-partitions-given-diff': return { nums: Array(n).fill(1), d: 0 };
    case 'ninja-and-his-friends': return { grid: Array.from({ length: n }, () => Array(constraints.maxCols).fill(100)) };
    default: throw Error(problem);
  }
}
(async () => {
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const stats = (await request('/api/problems/stats')).body;
  assert.equal(stats.catalogued, 431); assert.equal(stats.traced, 431);
  assert.deepEqual(stats.duplicateIds, {}); assert.deepEqual(stats.orphanedTracerIds, []);
  const rows = [];
  for (const problem of problems) {
    const options = (await request(`/api/problems/${problem}`)).body.approaches;
    for (const option of options) {
      const detail = (await request(`/api/problems/${problem}?approach=${option.id}`)).body;
      for (const [label, input] of [['default', undefined], ...(option.id !== 'canonical'
        ? [['maximum', maximum(problem, option.inputSpec.fields[0].constraints)]] : [])]) {
        const route = `/api/problems/${problem}/execute?approach=${option.id}`;
        const full = await request(`${route}&encoding=full`, input);
        const delta = await request(`${route}&encoding=delta`, input);
        const decoded = decodeTrace(delta.body);
        const normalized = decodeTrace(full.body).map((step, i) => Object.fromEntries(
          [...new Set([...Object.keys(step), ...Object.keys(decoded[i] ?? {})])].map(key => [key, step[key] === undefined ? null : step[key]])));
        assert(isDeepStrictEqual(normalized, decoded), `${problem}/${option.id}/${label}: full/delta mismatch`);
        assert(!full.body.truncated && !delta.body.truncated);
        assert(full.bytes < 2_000_000 && delta.bytes < 2_000_000);
        assert.equal(full.body.approachId, option.id);
        assert.equal(full.body.code, detail.javaCode);
        assert.equal(full.body.dsType, detail.dsType);
        assert.deepEqual(full.body.complexity, detail.complexity);
        if (input) assert.deepEqual(full.body.resolvedInput, input);
        const final = normalized.at(-1);
        assert.deepEqual(final.callStack, []);
        assert.equal(typeof final.variables.answer, 'string');
        if (label === 'maximum' && final.variables.calls != null) assert(Number(final.variables.calls) <= 220);
        rows.push({ problemId: problem, approachId: option.id, label, input: full.body.resolvedInput,
          answer: final.variables.answer, calls: final.variables.calls ?? null, cacheHits: final.variables.cacheHits ?? null,
          states: final.variables.computedStates ?? null, steps: full.body.stepCount,
          fullBytes: full.bytes, deltaBytes: delta.bytes, fullDeltaEqual: true, sourceAndIdentityMatch: true });
        console.log(`PASS API ${problem}/${option.id}/${label}: ${full.body.stepCount} steps, ${full.bytes} bytes`);
      }
    }
  }
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'api-results.json'), JSON.stringify({ generated: new Date().toISOString(), realBackend: true, stats, rows }, null, 2) + '\n');
})().catch(error => { console.error(error); process.exitCode = 1; });
