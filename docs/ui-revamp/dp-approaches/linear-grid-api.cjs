const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { createHash } = require('node:crypto');
const { family, cases, maximum, oracle } = require('./dp-audit-profile.cjs');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const out = path.join(__dirname, family);
async function request(route, input) {
  if (route.includes('/execute')) await new Promise(resolve => setTimeout(resolve, 1100));
  const response = await fetch(api + route, input === undefined ? undefined : {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(input)
  });
  const text = await response.text();
  assert.equal(response.status, 200, `${route}: ${text}`);
  return { body: JSON.parse(text), bytes: Buffer.byteLength(text) };
}
(async () => {
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const { buildRecursionTree } = await import(path.resolve(__dirname, '../../../frontend/src/trace/recursionTree.js'));
  const stats = (await request('/api/problems/stats')).body;
  assert.equal(stats.catalogued, 431); assert.equal(stats.traced, 431);
  assert.deepEqual(stats.duplicateIds, {}); assert.deepEqual(stats.orphanedTracerIds, []);
  const rows = [];
  for (const c of cases) for (const form of ['recursion', 'memoization', 'canonical']) {
    const {id}=c;
    const detail = (await request(`/api/problems/${id}?approach=${form}`)).body;
    for (const [label, input] of [['default', undefined], ['maximum', maximum(id, form)], ...(c.zero&&form==='memoization'?[['zero',c.zero]]:[])]) {
      const route = `/api/problems/${id}/execute?approach=${form}`;
      const full = await request(`${route}&encoding=full`, input);
      const delta = await request(`${route}&encoding=delta`, input);
      const decoded = decodeTrace(delta.body);
      const normalized = decodeTrace(full.body).map((step, i) => Object.fromEntries(
        [...new Set([...Object.keys(step), ...Object.keys(decoded[i] ?? {})])].map(key => [key, step[key] === undefined ? null : step[key]])));
      assert.deepEqual(normalized, decoded, `${id}/${form}/${label}: full/delta mismatch`);
      assert(!full.body.truncated && !delta.body.truncated);
      assert(full.bytes < 2_000_000 && delta.bytes < 2_000_000);
      for(const body of [full.body,delta.body]){
        assert.equal(body.problemId,id);assert.equal(body.approachId,form);
        assert.equal(body.code,detail.javaCode);assert.equal(body.dsType,detail.dsType);
        assert.deepEqual(body.complexity,detail.complexity);
        assert.deepEqual(body.resolvedInput,input??full.body.resolvedInput);
      }
      const final = normalized.at(-1);
      assert.deepEqual(final.callStack, []);
      assert.equal(final.variables.answer, String(oracle(id, full.body.resolvedInput)));
      if (final.variables.calls != null) assert(Number(final.variables.calls) <= 220);
      let rootCount=null;
      if(final.variables.calls!=null){
        const tree=buildRecursionTree(normalized,normalized.length-1);
        assert(!tree.truncated);assert.equal(tree.nodes.length,Number(final.variables.calls));
        rootCount=tree.nodes.filter(n=>n.parentId===null).length;assert.equal(rootCount,c.roots??1);
      }
      rows.push({ problemId: id, approachId: form, label, input: full.body.resolvedInput,
        sourceHash: createHash('sha256').update(full.body.code).digest('hex'),
        answer: final.variables.answer, calls: final.variables.calls ?? null,
        cacheHits: final.variables.cacheHits ?? null, states: final.variables.computedStates ?? null,
        rootCount,
        steps: full.body.stepCount, fullBytes: full.bytes, deltaBytes: delta.bytes,
        independentAnswerMatches: true, fullDeltaEqual: true, sourceAndIdentityMatch: true });
      console.log(`PASS ${id}/${form}/${label}: ${full.body.stepCount} steps ${full.bytes} bytes`);
    }
  }
  fs.mkdirSync(out, { recursive: true });
  fs.writeFileSync(path.join(out, 'api-results.json'), JSON.stringify({ generated: new Date().toISOString(), realBackend: true, stats, rows }, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
