// Pure validation of JSON evidence. A partial or internally mismatched proof cannot
// promote the LPS candidate, even when the surrounding viewport matrix is complete.
const assert = require('node:assert/strict');
const { cases, maximum, oracle } = require('./lps-cases.cjs');
const forms = ['recursion', 'memoization', 'canonical'];
const hash = value => typeof value === 'string' && /^[a-f0-9]{64}$/.test(value);
const boundedBytes = value => Number.isSafeInteger(value) && value > 0 && value < 2_000_000;
const date = value => typeof value === 'string' && Number.isFinite(Date.parse(value));
function inputFits(input, form) {
  assert.deepEqual(Object.keys(input).sort(), ['text']);
  const maximum = form === 'recursion' ? 5 : form === 'memoization' ? 8 : 12;
  for (const value of Object.values(input)) assert(typeof value === 'string' && /^[a-z]+$/.test(value) && value.length <= maximum);
}
function navigationRequests() {
  const request = (approach, input, method = 'POST', status = 200) => ({ approach, input: method === 'GET' ? null : input, method, status });
  return {
    warmRequests: [request('canonical', {}, 'GET'), request('canonical', { text: 'abc' }),
      request('recursion', {}), request('recursion', {}), request('canonical', {}),
      request('recursion', {}), request('canonical', { text: 'cbd' }),
      request('recursion', {}), request('canonical', { text: 'cbd' }), request('recursion', { text: 'abc' }),
      request('recursion', { text: 'abcdef' }, 'POST', 400)],
    coldPartialRequests: [request('canonical', {}, 'GET'), request('recursion', {})],
    coldNoInputRequests: [request('canonical', {}, 'GET'), request('canonical', { text: 'abc' }), request('recursion', {})]
  };
}
function rectangle(value) {
  assert(value && ['top', 'right', 'bottom', 'left', 'width', 'height'].every(key => Number.isFinite(value[key])));
  assert(value.width > 0 && value.height > 0);
  assert(Math.abs(value.right - value.left - value.width) <= 1 && Math.abs(value.bottom - value.top - value.height) <= 1);
}
function validateNavigation(navigation, sampler, assets, sources) {
  assert(navigation.realBackend === true && navigation.buildMode === 'production-preview' && date(navigation.generated));
  assert.deepEqual(navigation.localPackage, sampler.localPackage); assert.deepEqual(navigation.servedBuild?.assets, assets);
  const packagedAt = Date.parse(sampler.localPackage.packagedAt), generated = Date.parse(navigation.generated);
  assert(generated >= packagedAt);
  const axes = ['chromium', 'firefox'].flatMap(engine => [[320, 'dark'], [1366, 'light']].map(([width, theme]) => `${engine}:${width}:${theme}`));
  assert.equal(navigation.rows?.length, 4);
  assert.deepEqual(navigation.rows.map(row => `${row.engine}:${row.width}:${row.theme}`).sort(), axes.sort());
  assert.equal(navigation.screenshots?.length, 8);
  const files = new Set(), expectedRequests = navigationRequests(), windowRequests = []; let lastDispatch = null;
  for (const row of navigation.rows) {
    assert.equal(row.problemId, cases[0].id);
    for (const flag of ['passed', 'bareRequestedDefaults', 'partialDefaultsBothDirections', 'reverseCanonicalEmptyMapResetsDirtyDraft', 'backForward',
      'realPostBodyDelayPreservesLaterTextEdit', 'rejectedLinkKeepsCommittedIdentity', 'currentSourceAndStep',
      'coldCanonicalSummaryCannotSupplyUnknownRecursiveEditor', 'coldDetailReleaseFillsOnlyRequestedDefaults',
      'coldNoInputResetRetiresOldDraftBeforeLateEdits', 'noOverflowOrPageErrors']) assert.equal(row[flag], true);
    for (const [key, expected] of Object.entries(expectedRequests)) {
      assert.equal(row[key]?.length, expected.length);
      row[key].forEach((actual, index) => {
        const wanted = expected[index];
        for (const field of ['method', 'approach', 'status', 'input']) assert.deepEqual(actual[field], wanted[field]);
        assert(date(actual.dispatchedAt)); const dispatched = Date.parse(actual.dispatchedAt);
        assert(dispatched >= packagedAt && dispatched <= generated);
        if (lastDispatch !== null) assert(dispatched >= lastDispatch, 'Execution timestamps are out of order');
        lastDispatch = dispatched;
        // A nominal1100ms scheduler can catch up after event-loop delays. Validate
        // the actual server policy, rather than inventing a minimum packet gap.
        while (windowRequests.length && windowRequests[0] <= dispatched - 60000) windowRequests.shift();
        windowRequests.push(dispatched); assert(windowRequests.length <= 60);
        if (wanted.status === 200) {
          assert.deepEqual(actual.resolvedInput, { ...cases[0].defaults[wanted.approach], ...wanted.input });
          assert.equal(actual.sourceHash, sources.get(wanted.approach));
          assert(Number.isSafeInteger(actual.stepCount) && actual.stepCount >= 3 && actual.stepCount <= 5000);
        } else {
          assert.equal(actual.resolvedInput, null); assert.equal(actual.sourceHash, null); assert.equal(actual.stepCount, null);
        }
      });
    }
    const names = ['editor', 'source'].map(kind => `${row.engine}-${row.width}-${row.theme}-navigation-${kind}.png`);
    assert.deepEqual(row.screenshots, names);
    for (const [index, file] of names.entries()) {
      const matches = navigation.screenshots.filter(shot => shot.file === file); assert.equal(matches.length, 1);
      assert(!files.has(file)); files.add(file); const shot = matches[0];
      assert.equal(shot.engine, row.engine); assert.equal(shot.width, row.width); assert.equal(shot.theme, row.theme);
      assert.equal(shot.phase, index === 0 ? 'editor-and-input-echo' : 'committed-code-source');
      assert.equal(shot.scope, 'viewport'); assert.equal(shot.inspected, true);
      assert.deepEqual(shot.viewport, { width: row.width, height: row.width === 320 ? 568 : 768 });
      assert(Number.isFinite(shot.scroll?.x) && shot.scroll.x >= 0 && Number.isFinite(shot.scroll?.y) && shot.scroll.y >= 0);
      rectangle(shot.targetRect); rectangle(shot.railRect);
      assert(shot.targetRect.top >= shot.railRect.bottom - 1 && shot.targetRect.top < shot.viewport.height);
      assert(shot.targetRect.left >= -1 && shot.targetRect.right <= shot.viewport.width + 1);
      if (index === 0) {
        assert.deepEqual(shot.expectedDraft, { text: 'xyxy' });
        assert.deepEqual(shot.expectedCommittedInput, { text: 'abc' });
        assert.equal(shot.inputRects?.length, 1); assert.deepEqual(shot.inputRects.map(input => input.value), ['xyxy']);
        for (const input of shot.inputRects) { assert.equal(typeof input.id, 'string'); rectangle(input.rect); }
      } else assert.equal(shot.expectedSourceHash, sources.get('recursion'));
    }
  }
  assert.equal(files.size, 8);
}
function validateLpsEvidenceManifest(value) {
  try {
    // These are JSON documents; normalize realms as well as prototypes for the VM
    // fixtures used by the inventory guard's tests.
    const { sampler, recovery, transport, navigation, assets } = JSON.parse(JSON.stringify(value));
    const c = cases[0];
    assert(transport.realBackend === true && date(transport.generated) && Array.isArray(transport.rows));
    const expected = forms.flatMap(form => [['default', c.defaults[form]], ['maximum', maximum(c.id, form)],
      ...(form === 'memoization' ? Object.entries(c.memoExtra) : []), ...Object.entries(c.extra)]
      .map(([label, input]) => ({ key: `${form}/${label}`, input })));
    assert.equal(transport.rows.length, expected.length);
    assert.deepEqual(transport.rows.map(row => `${row.approachId}/${row.label}`).sort(), expected.map(row => row.key).sort());
    const sources = new Map();
    for (const row of transport.rows) {
      assert.equal(row.problemId, c.id); assert(forms.includes(row.approachId)); assert(hash(row.sourceHash));
      assert.deepEqual(row.input, expected.find(candidate => candidate.key === `${row.approachId}/${row.label}`).input);
      inputFits(row.input, row.approachId); assert.equal(row.answer, String(oracle(c.id, row.input)));
      assert(row.fullDeltaEqual === true && row.independentAnswerMatches === true && row.sourceAndIdentityMatch === true);
      assert(boundedBytes(row.fullBytes) && boundedBytes(row.deltaBytes));
      if (sources.has(row.approachId)) assert.equal(row.sourceHash, sources.get(row.approachId));
      else sources.set(row.approachId, row.sourceHash);
    }
    assert.equal(new Set(sources.values()).size, 3);
    assert(sampler.realBackend === true && sampler.complete === true && sampler.requests === 18);
    assert(date(sampler.generated) && date(sampler.localPackage?.packagedAt) && hash(sampler.localPackage?.sha256) && hash(sampler.samplerHash));
    const packagedAt = Date.parse(sampler.localPackage.packagedAt);
    assert(Date.parse(sampler.generated) >= packagedAt && Date.parse(transport.generated) >= packagedAt);
    assert.equal(sampler.specifications?.length, 3);
    assert.deepEqual(sampler.specifications.map(spec => spec.form).sort(), [...forms].sort());
    for (const specification of sampler.specifications) {
      assert.equal(specification.sourceHash, sources.get(specification.form));
      const fields = specification.inputSpec?.fields; assert(Array.isArray(fields));
      assert.deepEqual(fields.map(field => field.name), ['text']);
      for (const field of fields) {
        assert.equal(field.type, 'STRING'); assert.equal(field.constraints.minLength, 1);
        assert.equal(field.constraints.maxLength, specification.form === 'recursion' ? 5 : specification.form === 'memoization' ? 8 : 12);
        assert.equal(field.constraints.pattern, '[a-z]+'); assert.equal(field.defaultValue, c.defaults[specification.form][field.name]);
      }
    }
    assert.equal(sampler.rows?.length, 9);
    const profiles = forms.flatMap(form => ['high', 'low', 'seeded'].map(profile => `${form}/${profile}`));
    assert.deepEqual(sampler.rows.map(row => `${row.form}/${row.profile}`).sort(), profiles.sort());
    for (const row of sampler.rows) {
      inputFits(row.input, row.form); assert.equal(row.expected, oracle(c.id, row.input));
      assert.equal(row.sourceHash, sources.get(row.form));
      assert(row.actualFrontendSampler === true && row.fullDeltaEqual === true && row.independentAnswerAndKnownCells === true);
      assert(boundedBytes(row.fullBytes) && boundedBytes(row.deltaBytes));
      assert(Number.isSafeInteger(row.steps) && row.steps > 0 && row.steps <= 5000);
    }
    assert(recovery.realBackend === true && recovery.buildMode === 'production-preview' && date(recovery.generated));
    assert(Date.parse(recovery.generated) >= packagedAt); assert.deepEqual(recovery.localPackage, sampler.localPackage);
    assert(Array.isArray(assets) && assets.some(asset => asset.endsWith('.js')) && assets.some(asset => asset.endsWith('.css')));
    assert.deepEqual(recovery.servedBuild?.assets, assets);
    const axes = ['chromium', 'firefox'].flatMap(engine => [320, 1366].flatMap(width => ['light', 'dark'].map(theme => `${engine}:${width}:${theme}`)));
    assert.equal(recovery.rows?.length, 8);
    assert.deepEqual(recovery.rows.map(row => `${row.engine}:${row.width}:${row.theme}`).sort(), axes.sort());
    for (const row of recovery.rows) {
      assert.equal(row.problemId, c.id); assert.equal(row.sourceHash, sources.get('recursion'));
      for (const flag of ['passed', 'bareRecursiveLinkUsesOwnDefaults', 'actualRandomizeDraftOnly', 'generatedInputRunsAndShares',
        'longRetainedStringWarning', 'noAutomaticTruncation', 'rejectedDraftRunStepAndLinkKept', 'errorSummaryFocused',
        'viewsAndSourceKeysPreserveRun', 'actualCallStackRegion', 'explicitOwnExampleDraftOnly', 'successfulRecovery', 'nativeMaxLengthMatchesSelected', 'noOverflowOrErrors'])
        assert.equal(row[flag], true);
    }
    validateNavigation(navigation, sampler, assets, sources);
    return true;
  } catch { return false; }
}
module.exports = { validateLpsEvidenceManifest };
