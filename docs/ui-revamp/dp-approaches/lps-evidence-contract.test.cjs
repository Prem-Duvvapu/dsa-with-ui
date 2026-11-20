const test = require('node:test');
const assert = require('node:assert/strict');
const fixture = require('./lps-evidence-fixture.cjs');
const fs = require('node:fs');
const path = require('node:path');
const { createRequire } = require('node:module');
// Read-only mutation mode proves the navigation gate matters; never used as runtime evidence.
function validator() {
  if (process.env.LPS_NAV_GUARD_MUTANT !== '1') return require('./lps-evidence-contract.cjs');
  const file = path.join(__dirname, 'lps-evidence-contract.cjs');
  const source = fs.readFileSync(file, 'utf8').replace('validateNavigation(navigation, sampler, assets, sources);', '');
  const module = { exports: {} }; new Function('require', 'module', 'exports', source)(createRequire(file), module, module.exports);
  return module.exports;
}
const { validateLpsEvidenceManifest } = validator();
function proof() {
  const docs = fixture();
  return { recovery: docs['input-recovery-results.json'], sampler: docs['sampler-api-results.json'],
    transport: docs['api-results.json'], navigation: docs['navigation-results.json'], assets: ['/assets/real.js', '/assets/real.css'] };
}
test('complete independent LPS input/source/artifact proof is accepted', () => {
  assert.equal(validateLpsEvidenceManifest(proof()), true);
});
test('legal short dispatch gaps do not invalidate ordered evidence within the backend minute limit', () => {
  const value = proof(), requests = value.navigation.rows[0].warmRequests;
  requests[1].dispatchedAt = new Date(Date.parse(requests[0].dispatchedAt) + 500).toISOString();
  assert.equal(validateLpsEvidenceManifest(value), true);
});
for (const [name, mutate] of [
  ['missing sampler document', p => { delete p.sampler; }],
  ['missing complete API document', p => { delete p.transport; }],
  ['missing recovery document', p => { delete p.recovery; }],
  ['missing navigation document', p => { delete p.navigation; }],
  ['false cold-summary protection', p => { p.navigation.rows[0].coldCanonicalSummaryCannotSupplyUnknownRecursiveEditor = false; }],
  ['false unresolved no-input reset protection', p => { p.navigation.rows[0].coldNoInputResetRetiresOldDraftBeforeLateEdits = false; }],
  ['missing navigation axis', p => { p.navigation.rows.pop(); }],
  ['duplicate navigation axis', p => { p.navigation.rows[0] = { ...p.navigation.rows[1] }; }],
  ['old navigation assets', p => { p.navigation.servedBuild.assets = ['/assets/old.js', '/assets/real.css']; }],
  ['wrong navigation JAR', p => { p.navigation.localPackage = { ...p.navigation.localPackage, sha256: 'f'.repeat(64) }; }],
  ['navigation before package', p => { p.navigation.generated = '2026-11-18T17:00:00.000Z'; }],
  ['missing navigation request', p => { p.navigation.rows[0].warmRequests.pop(); }],
  ['wrong omitted recursive default', p => { p.navigation.rows[0].coldPartialRequests[1].resolvedInput.text = 'notdefault'; }],
  ['borrowed navigation source', p => { p.navigation.rows[0].coldPartialRequests[1].sourceHash = 'f'.repeat(64); }],
  ['unexpected navigation request body', p => { p.navigation.rows[0].warmRequests[3].input.text = 'unrequested'; }],
  ['navigation falsely accepts rejection', p => { p.navigation.rows[0].warmRequests.at(-1).status = 200; }],
  ['missing navigation screenshot', p => { p.navigation.screenshots.pop(); }],
  ['duplicate navigation screenshot', p => { p.navigation.screenshots[0].file = p.navigation.screenshots[1].file; }],
  ['unreviewed navigation screenshot', p => { p.navigation.screenshots[0].inspected = false; }],
  ['inaccurate screenshot viewport', p => { p.navigation.screenshots[0].viewport.width = 390; }],
  ['invalid screenshot rectangle', p => { p.navigation.screenshots[0].targetRect.width = -1; }],
  ['screenshot target behind sticky rail', p => {
    p.navigation.screenshots[0].targetRect.top = 20;
    p.navigation.screenshots[0].targetRect.bottom = 320; // Keep geometry valid; only rail clearance is wrong.
  }],
  ['incomplete actual sampler', p => { p.sampler.complete = false; }],
  ['duplicate sampler profile', p => { p.sampler.rows[0] = { ...p.sampler.rows[1] }; }],
  ['wrong actual request count', p => { p.sampler.requests = 17; }],
  ['wrong sampler answer', p => { p.sampler.rows[0].expected = 999; }],
  ['failed encoding comparison', p => { p.sampler.rows[0].fullDeltaEqual = false; }],
  ['missing actual-sampler proof', p => { p.sampler.rows[0].actualFrontendSampler = false; }],
  ['unsupported long recursive input', p => { p.sampler.rows[0].input.text = 'aaaaaa'; }],
  ['missing input field', p => { delete p.sampler.rows[0].input.text; }],
  ['inaccurate canonical spec', p => { p.sampler.specifications[2].inputSpec.fields[0].constraints.maxLength = 8; }],
  ['wrong approach default', p => { p.sampler.specifications[0].inputSpec.fields[0].defaultValue = 'abcde'; }],
  ['mismatched served source', p => { p.sampler.specifications[0].sourceHash = 'f'.repeat(64); }],
  ['mismatched recovery source', p => { p.recovery.rows[0].sourceHash = 'f'.repeat(64); }],
  ['partial real API scenarios', p => { p.transport.rows.pop(); }],
  ['incorrect real API answer', p => { p.transport.rows[0].answer = '999'; }],
  ['response at byte limit', p => { p.sampler.rows[0].fullBytes = 2_000_000; }],
  ['impossible response size', p => { p.sampler.rows[0].deltaBytes = -1; }],
  ['wrong JAR identity', p => { p.recovery.localPackage = { ...p.recovery.localPackage, sha256: 'f'.repeat(64) }; }],
  ['evidence predates package', p => { p.sampler.generated = '2026-11-18T17:00:00.000Z'; }],
  ['incorrect build assets', p => { p.recovery.servedBuild.assets = ['/assets/other.js', '/assets/other.css']; }],
  ['missing recovery axis', p => { p.recovery.rows.pop(); }],
  ['wrong native Text maxlength claim', p => { p.recovery.rows[0].nativeMaxLengthMatchesSelected = false; }],
  ['unsupported recovery claim', p => { p.recovery.rows[0].noAutomaticTruncation = false; }]
]) test(`LPS proof refuses ${name}`, () => {
  const value = proof(); mutate(value); assert.equal(validateLpsEvidenceManifest(value), false);
});


test('LPS proof refuses one-direction-only requests claiming defaults in both directions', () => {
  const value = proof();
  for (const row of value.navigation.rows) {
    const reverse = row.warmRequests.findIndex(request => request.method === 'POST'
      && request.approach === 'canonical' && Object.keys(request.input).length === 0);
    if (reverse >= 0) row.warmRequests.splice(reverse, 2);
  }
  assert.equal(validateLpsEvidenceManifest(value), false);
});
test('LPS proof refuses a false reverse canonical empty-map dirty-draft reset claim', () => {
  const value = proof();
  value.navigation.rows[0].reverseCanonicalEmptyMapResetsDirtyDraft = false;
  assert.equal(validateLpsEvidenceManifest(value), false);
});
