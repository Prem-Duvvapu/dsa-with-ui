// Bounded deployment check against independently verified local default cases.
// This is not whole-project acceptance, nor a production load test.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const local = process.env.LOCAL_BACKEND_URL || 'http://127.0.0.1:8923';
const publicSite = process.env.PUBLIC_SITE_URL || 'https://dsa-with-ui.vercel.app';
const cases = require('./requested-five/api-results.json').rows.filter(row => row.label === 'default');
async function get(base, route, input) {
  if (base === publicSite && route.includes('/execute')) await new Promise(resolve => setTimeout(resolve, 1100));
  const response = await fetch(base + route, {
    signal: AbortSignal.timeout(30000),
    ...(input === undefined ? {} : { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input) })
  });
  assert.equal(response.status, 200, `${base}${route}: HTTP${response.status}`);
  return response.json();
}

(async () => {
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const stats = await get(publicSite, '/api/problems/stats');
  assert.equal(stats.catalogued, 431);
  assert.equal(stats.traced, 431);
  assert.equal(stats.untraced, 0);
  assert.deepEqual(stats.duplicateIds, {});
  assert.deepEqual(stats.orphanedTracerIds, []);
  const rows = [];
  for (const expected of cases) {
    const detailRoute = `/api/problems/${expected.problemId}?approach=${expected.approachId}`;
    const known = await get(local, detailRoute);
    const served = await get(publicSite, detailRoute);
    for (const key of ['approachId', 'javaCode', 'inputSpec', 'dsType', 'complexity', 'defaultApproachId'])
      assert.deepEqual(served[key], known[key], `${expected.problemId}/${expected.approachId}: deployed ${key} differs`);
    const route = `/api/problems/${expected.problemId}/execute?approach=${expected.approachId}`;
    const full = await get(publicSite, `${route}&encoding=full`, expected.input);
    const delta = await get(publicSite, `${route}&encoding=delta`, expected.input);
    const decodedFull = decodeTrace(full);
    const decodedDelta = decodeTrace(delta);
    const normalized = decodedFull.map((step, index) => Object.fromEntries(
      [...new Set([...Object.keys(step), ...Object.keys(decodedDelta[index] ?? {})])]
        .map(key => [key, step[key] === undefined ? null : step[key]])));
    assert.deepEqual(normalized, decodedDelta, `${expected.problemId}/${expected.approachId}: full/delta mismatch`);
    assert.equal(full.problemId, expected.problemId);
    assert.equal(full.approachId, expected.approachId);
    assert.equal(full.truncated, false);
    assert.equal(delta.truncated, false);
    assert.deepEqual(full.resolvedInput, expected.input);
    assert.equal(full.code, known.javaCode);
    assert.equal(full.dsType, known.dsType);
    assert.deepEqual(full.complexity, known.complexity);
    assert.equal(String(decodedFull.at(-1)?.variables?.answer), expected.answer);
    rows.push({ problemId: expected.problemId, approachId: expected.approachId,
      knownDefaultAnswer: expected.answer, sourceHash: crypto.createHash('sha256').update(full.code).digest('hex'),
      detailMatchesCurrentLocalBackend: true, identityAndInputMatch: true, fullDeltaEqual: true,
      complete: true, stepCount: full.stepCount });
    console.log(`PASS PUBLIC ${expected.problemId}/${expected.approachId}`);
  }
  const canonicalSourceRows = [];
  const core = path.resolve(__dirname, '../../../backend/src/main/resources/solutions/core');
  for (const filename of fs.readdirSync(core).filter(name => name.endsWith('.java')).sort()) {
    const id = filename.slice(0, -5);
    const route = `/api/problems/${id}`;
    const known = await get(local, route);
    const served = await get(publicSite, route);
    assert.equal(typeof known.javaCode, 'string');
    assert(known.javaCode.trim().length > 0);
    assert.equal(served.javaCode, known.javaCode, `${id}: deployed repaired source differs`);
    canonicalSourceRows.push({ problemId: id,
      sourceHash: crypto.createHash('sha256').update(served.javaCode).digest('hex'),
      matchesCurrentLocalSource: true });
    console.log(`PASS PUBLIC repaired canonical source ${id}`);
  }
  fs.writeFileSync(path.join(__dirname, 'input-guidance', 'public-requested-five.json'), JSON.stringify({
    generated: new Date().toISOString(), publicSite, localBackendSource: 'b3a3497 (unchanged backend)',
    scope: '17 offered default executions for the owner-requested five problems and the repaired canonical core source listings; not all inputs, all catalogue deployment or rollback gates',
    stats, rows, canonicalSourceRows
  }, null, 2) + '\n');
})().catch(error => { console.error(error); process.exitCode = 1; });
