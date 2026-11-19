// Synthetic unit-test documents only. Never written as runtime verification evidence.
const { cases, maximum, oracle } = require('./lcs-cases.cjs');
module.exports = function lcsEvidenceFixture() {
  const c = cases[0], forms = ['recursion', 'memoization', 'canonical'];
  const source = Object.fromEntries(forms.map((form, index) => [form, String(index + 1).repeat(64)]));
  const localPackage = { packagedAt: '2026-11-19T17:00:00.000Z', sha256: 'a'.repeat(64) };
  const generated = '2026-11-19T17:01:00.000Z', servedBuild = { assets: ['/assets/real.js', '/assets/real.css'] };
  const specifications = forms.map(form => ({ form, sourceHash: source[form], inputSpec: { fields:
    ['first', 'second'].map(name => ({ name, type: 'STRING', label: name === 'first' ? 'First string' : 'Second string',
      constraints: { minLength: 1, maxLength: form === 'recursion' ? 4 : form === 'memoization' ? 8 : 12, pattern: '[a-z]+' },
      defaultValue: c.defaults[form][name] })) } }));
  const sampled = forms.flatMap(form => ['high', 'low', 'seeded'].map(profile => {
    const length = profile === 'high' ? form === 'recursion' ? 4 : form === 'memoization' ? 8 : 10 : 1;
    const input = { first: 'a'.repeat(length), second: 'a'.repeat(length) };
    return { form, profile, input, expected: oracle(c.id, input), steps: 10, fullBytes: 1000, deltaBytes: 800,
      sourceHash: source[form], actualFrontendSampler: true, fullDeltaEqual: true, independentAnswerAndKnownCells: true };
  }));
  const transportRows = forms.flatMap(approachId => [['default', c.defaults[approachId]], ['maximum', maximum(c.id, approachId)],
    ...(approachId === 'memoization' ? Object.entries(c.memoExtra) : []), ...Object.entries(c.extra)].map(([label, input]) => ({
    problemId: c.id, approachId, label, input, answer: String(oracle(c.id, input)), sourceHash: source[approachId],
    fullBytes: 1000, deltaBytes: 800, independentAnswerMatches: true, fullDeltaEqual: true, sourceAndIdentityMatch: true
  })));
  const recoveryRows = ['chromium', 'firefox'].flatMap(engine => [320, 1366].flatMap(width => ['light', 'dark'].map(theme => ({
    problemId: c.id, engine, width, theme, sourceHash: source.recursion, passed: true, bareRecursiveLinkUsesOwnDefaults: true,
    actualRandomizeDraftOnly: true, generatedInputRunsAndShares: true, longRetainedStringWarning: true, noAutomaticTruncation: true,
    rejectedDraftRunStepAndLinkKept: true, errorSummaryFocused: true, viewsAndSourceKeysPreserveRun: true, actualCallStackRegion: true,
    explicitOwnExampleDraftOnly: true, successfulRecovery: true, noOverflowOrErrors: true
  }))));
  let dispatched = Date.parse(localPackage.packagedAt);
  const request = (approach, input, method = 'POST', status = 200) => ({
    dispatchedAt: new Date(dispatched += 1100).toISOString(), method, approach,
    input: method === 'GET' ? null : input, status,
    resolvedInput: status === 200 ? { ...c.defaults[approach], ...input } : null,
    sourceHash: status === 200 ? source[approach] : null, stepCount: status === 200 ? 10 : null
  });
  const screenshots = [];
  const navigationRows = ['chromium', 'firefox'].flatMap(engine => [[320, 'dark'], [1366, 'light']].map(([width, theme]) => {
    const files = ['editor', 'source'].map(kind => `${engine}-${width}-${theme}-navigation-${kind}.png`);
    for (const [index, file] of files.entries()) screenshots.push({ file, engine, width, theme,
      phase: index === 0 ? 'editor-and-input-echo' : 'committed-code-source', scope: 'viewport', inspected: true,
      viewport: { width, height: width === 320 ? 568 : 768 }, scroll: { x: 0, y: 600 },
      targetRect: { top: 100, left: 16, right: width - 16, bottom: 400, width: width - 32, height: 300 },
      railRect: { top: 0, left: 0, right: width, bottom: 72, width, height: 72 },
      inputRects: index === 0 ? ['mn', 'zz'].map((value, i) => ({ id: `field-${i}`, value,
        rect: { top: 160 + i * 70, left: 24, right: width - 24, bottom: 200 + i * 70, width: width - 48, height: 40 } })) : [],
      ...(index === 0 ? { expectedDraft: { first: 'mn', second: 'zz' }, expectedCommittedInput: { first: 'mn', second: 'acd' } }
        : { expectedSourceHash: source.recursion }) });
    return { engine, width, theme, passed: true, screenshots: files,
      warmRequests: [request('canonical', {}, 'GET'), request('canonical', { first: 'abc', second: 'ac' }),
        request('recursion', {}), request('recursion', { first: 'xy' }), request('canonical', { first: 'uv' }),
        request('recursion', { first: 'xy' }), request('canonical', { first: 'uv' }), request('recursion', { first: 'mn' }),
        request('recursion', { first: 'waytoolong' }, 'POST', 400)],
      coldPartialRequests: [request('canonical', {}, 'GET'), request('recursion', { first: 'xy' })],
      coldNoInputRequests: [request('canonical', {}, 'GET'), request('canonical', { first: 'abc', second: 'ac' }), request('recursion', {})],
      bareRequestedDefaults: true, partialDefaultsBothDirections: true, backForward: true,
      realPostBodyDelayPreservesLaterSecondEdit: true, rejectedLinkKeepsCommittedIdentity: true, currentSourceAndStep: true,
      coldCanonicalSummaryCannotSupplyUnknownRecursiveEditor: true, coldDetailReleaseFillsOnlyRequestedDefaults: true,
      coldNoInputResetRetiresOldDraftBeforeLateEdits: true, noOverflowOrPageErrors: true };
  }));
  return {
    'api-results.json': { generated, realBackend: true, rows: transportRows },
    'sampler-api-results.json': { generated, realBackend: true, complete: true, localPackage, samplerHash: 'e'.repeat(64), requests: 18, specifications, rows: sampled },
    'input-recovery-results.json': { generated, realBackend: true, buildMode: 'production-preview', localPackage, servedBuild, rows: recoveryRows },
    'navigation-results.json': { generated: '2026-11-19T17:03:00.000Z', realBackend: true,
      buildMode: 'production-preview', localPackage: { ...localPackage },
      servedBuild: { assets: [...servedBuild.assets] }, rows: navigationRows, screenshots }
  };
};
