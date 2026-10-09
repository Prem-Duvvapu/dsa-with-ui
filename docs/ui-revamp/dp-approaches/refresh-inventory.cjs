// Refresh measured facts only. Human recurrence classifications are reviewed separately.
// Usage: BACKEND_URL=http://localhost:8923 node refresh-inventory.cjs
const fs = require('fs');
const path = require('path');
const { createHash } = require('crypto');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
// Java immutable maps may serialize keys in a different order after a restart. Keep
// the prior document's ordering, never its values, so refreshed facts produce clean diffs.
function ordered(value, previous) {
  if (Array.isArray(value)) return value.map((item, i) => ordered(item, previous?.[i]));
  if (!value || typeof value !== 'object') return value;
  const priorKeys = previous && typeof previous === 'object' ? Object.keys(previous) : [];
  const keys = [...priorKeys.filter(k => Object.hasOwn(value, k)), ...Object.keys(value).filter(k => !priorKeys.includes(k)).sort()];
  return Object.fromEntries(keys.map(k => [k, ordered(value[k], previous?.[k])]));
}
async function get(route) {
  const response = await fetch(`${api}${route}`);
  if (!response.ok) throw Error(`${route}: HTTP ${response.status}`);
  return response.json();
}
// Evidence is problem-keyed: never borrow another problem's completed pilot matrix.
function candidateUiVerified(problemId) {
  const documents = ['chromium', 'firefox', 'zoom-200'].map(tag => {
    const file = path.join(__dirname, `${problemId}-${tag}-results.json`);
    return fs.existsSync(file) ? JSON.parse(fs.readFileSync(file, 'utf8')) : null;
  });
  const list = documents[0]?.servedBuild?.assets;
  const assets = JSON.stringify(list);
  return Boolean(Array.isArray(list) && list.some(a => a.endsWith('.js')) && list.some(a => a.endsWith('.css'))
    && documents.every((doc, index) => doc?.problemId === problemId
    && doc.engine === (index === 1 ? 'firefox' : 'chromium')
    && doc.buildMode === 'production-preview' && doc.rows?.length === (index === 2 ? 8 : 10)
    && JSON.stringify(doc.servedBuild?.assets) === assets
    && new Set(doc.rows.map(r => `${r.width}x${r.height}:${r.theme}`)).size === doc.rows.length
    && doc.rows.every(r => (index === 2 ? ['640x1136', '780x1688', '1366x768', '1440x900']
      : ['320x568', '390x844', '768x1024', '1366x768', '1440x900']).includes(`${r.width}x${r.height}`)
      && ['light', 'dark'].includes(r.theme) && r.nativeZoom === (index === 2 ? 2 : 1))
    && doc.rows.every(r => r.noOverflowOrErrors && r.visibleMaximumTreeRoots
      && r.nativeComparisonKeyboardAndFocus && r.sameCommittedInputAndIndependentSliders
      && r.otherCaseUsesShownApproach && r.refusedArrayKeptWithOldLink && r.sharedInputThenStepRestored)));
}
(async () => {
  const inventoryPath = path.join(__dirname, 'inventory.json');
  const previous = fs.existsSync(inventoryPath) ? JSON.parse(fs.readFileSync(inventoryPath, 'utf8')) : null;
  const evidencePath = path.join(__dirname, 'd3-results.json');
  const ui = fs.existsSync(evidencePath) ? JSON.parse(fs.readFileSync(evidencePath, 'utf8')) : null;
  const verifiedPilot = ui?.rows?.length === 10 && ui.rows.every(r => r.allThreeExecuted && r.noOverflowOrErrors)
    && ui?.largestInputGeometry?.length === 4 && ui.largestInputGeometry.every(r => r.svgWidth === r.declaredWidth && r.height <= 480);
  const comparisonPath = path.join(__dirname, 'd4-results.json');
  const comparison = fs.existsSync(comparisonPath) ? JSON.parse(fs.readFileSync(comparisonPath, 'utf8')) : null;
  const verifiedTeachingComparison = verifiedPilot && comparison?.rows?.length === 10
    && comparison.rows.every(r => r.openingPairAndViewsDoNotFetch && r.sameInputSnapshotAndMainPreserved
      && r.realCountersAndAnswers && r.independentPositionsAndKeys && r.refusalKeepsMainAndSuccessfulSide && r.noOverflowOrErrors);
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
      alternativeStatus: detail.approaches?.length > 1 ? (problem.id === 'climbing-stairs' && verifiedPilot
        ? verifiedTeachingComparison ? 'backend-and-UI-teaching-comparison-pilot-verified; family rollout pending'
          : 'backend-and-UI-pilot-verified; family rollout pending' : candidateUiVerified(problem.id)
            ? 'backend-and-UI-candidate-verified; family rollout pending' : 'backend-pilot; UI pending')
        : reviewed.get(problem.id).plannedForms.length ? 'planned' : 'requires-different-formulation',
      alternativeSafetyBounds: detail.approaches?.length > 1
        ? 'Published alternative bounds below; consult this problem\'s measurement evidence in the DP plan. Evidence for another problem does not certify these bounds.'
        : 'Not measured yet; canonical caps do not authorize exponential recursion.',
      ...(detail.approaches?.length > 1 ? { publishedAlternativeInputSpecs:
        detail.approaches.filter(a => !a.isDefault).map(a => ({ id: a.id, inputSpec: a.inputSpec })) } : {}) });
  }
  const stats = await get('/api/problems/stats');
  fs.writeFileSync(inventoryPath, JSON.stringify(ordered({
    generated: new Date().toISOString(), stats, candidates: rows.length,
    safetyNotes: 'Preserve declared input rules and joint DP table budgets; measure every new approach independently.', rows
  }, previous), null, 2) + '\n');
  console.log(`Classified ${rows.length} candidates; ${stats.traced}/${stats.catalogued} canonical tracers`);
})().catch(error => { console.error(error); process.exitCode = 1; });
