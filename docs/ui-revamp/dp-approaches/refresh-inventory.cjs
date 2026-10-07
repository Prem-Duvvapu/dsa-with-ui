// Refresh measured facts only. Human recurrence classifications are reviewed separately.
// Usage: BACKEND_URL=http://localhost:8923 node refresh-inventory.cjs
const fs = require('fs');
const path = require('path');
const { createHash } = require('crypto');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
async function get(route) {
  const response = await fetch(`${api}${route}`);
  if (!response.ok) throw Error(`${route}: HTTP ${response.status}`);
  return response.json();
}
(async () => {
  const reviewed = new Map(fs.readFileSync(path.join(__dirname, 'classifications.tsv'), 'utf8')
    .split(/\r?\n/).filter(line => line && !line.startsWith('#')).map(line => {
      const [id, canonicalImplementation, state, transition, resultReconstruction, plannedForms, notes] = line.split('\t');
      if (!notes || !transition || !canonicalImplementation) throw Error(`Incomplete classification: ${id}`);
      return [id, { canonicalImplementation, state, transition, resultReconstruction,
        plannedForms: plannedForms ? plannedForms.split(',') : [], notes }];
    }));
  const catalog = await get('/api/problems');
  const candidates = catalog.filter(p => p.category === 'Dynamic Programming'
    || p.id === 'count-palindromic-subsequences').sort((a, b) => a.id.localeCompare(b.id));
  if (reviewed.size !== candidates.length || candidates.some(p => !reviewed.has(p.id))) {
    throw Error('Candidate inventory and reviewed classifications differ; classify new entries before refreshing');
  }
  const rows = [];
  for (const problem of candidates) {
    const detail = await get(`/api/problems/${problem.id}`);
    rows.push({ id: problem.id, category: problem.category, dsType: detail.dsType,
      ...reviewed.get(problem.id), codeDigest: createHash('sha256').update(detail.javaCode).digest('hex'),
      canonicalInputSpec: detail.inputSpec, canonicalComplexity: detail.complexity,
      alternativeStatus: detail.approaches?.length > 1 ? 'backend-pilot; UI pending'
        : reviewed.get(problem.id).plannedForms.length ? 'planned' : 'requires-different-formulation',
      alternativeSafetyBounds: detail.approaches?.length > 1
        ? 'Published pilot bounds below; measured Climbing Stairs traces are in d2-results.json. Not a rollout-wide safety claim.'
        : 'Not measured yet; canonical caps do not authorize exponential recursion.',
      ...(detail.approaches?.length > 1 ? { publishedAlternativeInputSpecs:
        detail.approaches.filter(a => !a.isDefault).map(a => ({ id: a.id, inputSpec: a.inputSpec })) } : {}) });
  }
  const stats = await get('/api/problems/stats');
  fs.writeFileSync(path.join(__dirname, 'inventory.json'), JSON.stringify({
    generated: new Date().toISOString(), stats, candidates: rows.length,
    safetyNotes: 'Preserve declared input rules and joint DP table budgets; measure every new approach independently.', rows
  }, null, 2) + '\n');
  console.log(`Classified ${rows.length} candidates; ${stats.traced}/${stats.catalogued} canonical tracers`);
})().catch(error => { console.error(error); process.exitCode = 1; });
