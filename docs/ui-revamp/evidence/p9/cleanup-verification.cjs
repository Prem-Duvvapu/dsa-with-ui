// Consolidates this cleanup's evidence; incomplete/duplicate/mixed-build reports fail.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { execFileSync } = require('node:child_process');
const identity = require('../served-build-identity.cjs');
const read = file => JSON.parse(fs.readFileSync(file, 'utf8'));
const key = row => JSON.stringify([row.id ?? row.pathname, row.theme, row.width, row.height]);

function baselineCaseCount(log, relative) {
  // A stderr header also names the file; only the completed suite line has a count.
  const line = log.split('\n').find(line => line.includes(` ${relative} `) && /\((\d+) tests?\)/.test(line));
  const count = line?.match(/\((\d+) tests?\)/)?.[1];
  assert(count, `No baseline test count for ${relative}`);
  return Number(count);
}

function compareRows(before, after, expected, content) {
  for (const report of [before, after]) {
    assert.equal(report.complete, true, 'Incomplete browser report');
    assert.equal(report.rows.length, expected, 'Missing browser rows');
    assert.equal(new Set(report.rows.map(key)).size, expected, 'Duplicate browser rows');
    for (const row of report.rows) {
      assert.deepEqual(row.failures ?? [], [], 'Browser failures');
      if (row[content] == null) {
        // Library/not-found chrome rows only check overflow/errors, not stage geometry.
        assert(content === 'geometry' && row.pathname, 'Missing measured content');
      }
    }
  }
  const previous = new Map(before.rows.map(row => [key(row), row]));
  for (const row of after.rows) {
    assert(previous.has(key(row)), 'Different browser matrix');
    assert.deepEqual(row[content], previous.get(key(row))[content], `Layout changed: ${key(row)}`);
  }
  const comparable = after.rows.filter(row => row[content] != null).length;
  return { rows: expected, comparable, identical: comparable };
}

function rendererSummary(report) {
  assert.equal(report.complete, true, 'Incomplete renderer report');
  assert.equal(new Set(report.picks.map(pick => pick.id)).size, report.picks.length, 'Duplicate picks');
  const expected = new Set(report.picks.flatMap(({ id, dsType }) =>
    [['1366x768', 'dark'], ['390x844', 'light']].flatMap(([viewport, scheme]) =>
      ['default', 'alternate'].map(variant => JSON.stringify([id, dsType, viewport, scheme, variant])))));
  assert.equal(report.rows.length, expected.size, 'Missing renderer rows');
  const seen = new Set();
  for (const row of report.rows) {
    const id = JSON.stringify([row.id, row.dsType, row.viewport, row.scheme, row.variant]);
    assert(expected.has(id) && !seen.has(id), 'Unknown or duplicate renderer row');
    seen.add(id);
    assert.equal(row.ok, true, `Failed renderer ${id}`);
    assert.deepEqual(row.problems, []);
    assert(row.steps > 0, 'No steps');
    assert.deepEqual(Object.keys(row.positions).sort(), ['final', 'first', 'middle']);
    for (const point of Object.values(row.positions)) {
      assert(point.overflow <= 0 && point.drawn && !point.unsupported, 'Bad renderer position');
    }
  }
  return { browser: report.browser, rows: report.rows.length, families: new Set(report.picks.map(pick => pick.dsType)).size,
    problems: report.picks.length, positionChecks: report.rows.length * 3 };
}

function verifyReachability(before, after) {
  assert.deepEqual(after.unreachableFiles, [], 'Orphan production source remains');
  assert.deepEqual(after.productionModules.map(module => module.file), before.productionModules.map(module => module.file),
    'Cleanup changed production module reachability');
  const remaining = new Set(after.sourceFiles);
  assert.deepEqual(before.sourceFiles.filter(file => !remaining.has(file)), before.unreachableFiles,
    'Deleted a production-loaded source');
  return { before: before.sourceFiles.length, after: after.sourceFiles.length,
    retired: before.unreachableFiles.length, productionLoaded: after.productionModules.length };
}

async function main() {
  const [beforeRenderer, afterRenderer, chromiumRenderer] = process.argv.slice(2);
  assert(beforeRenderer && afterRenderer, 'Pass before and after renderer-manifest.json paths');
  const before = read(path.join(__dirname, 'reachability-before.json'));
  const after = read(path.join(__dirname, 'reachability-after.json'));
  const editorBefore = read(path.join(__dirname, 'before/editor/editor-results.json'));
  const editorAfter = read(path.join(__dirname, 'after/editor/editor-results.json'));
  const servedBuild = await identity(process.env.FRONTEND_URL || 'http://127.0.0.1:5180');
  for (const [audit, editor] of [[before, editorBefore], [after, editorAfter]]) {
    assert.deepEqual(editor.servedBuild.assets.map(file => file.slice(1)).sort(), audit.assets.sort(), 'Mixed editor/build evidence');
  }
  assert.deepEqual(servedBuild, editorAfter.servedBuild, 'Final preview no longer serves the verified build');
  const root = path.resolve(__dirname, '../../../..');
  assert(after.baseCommit && before.baseCommit === after.baseCommit, 'Missing or mismatched baseline commit');
  // Compare the whole package to its baseline, including staged and committed changes.
  // An ordinary unstaged diff loses the retired-test inventory as soon as we stage it.
  execFileSync('git', ['diff', '--exit-code', after.baseCommit, '--', 'backend', 'frontend/package.json', 'frontend/package-lock.json'],
    { cwd: root, encoding: 'utf8' });
  const deletedTests = execFileSync('git', ['diff', '--name-only', '--diff-filter=D', after.baseCommit], { cwd: root, encoding: 'utf8' })
    .trim().split('\n').filter(file => /frontend\/src\/.*\.test\.[jt]sx?$/.test(file));
  const baseline = fs.readFileSync('/tmp/dsa-cleanup-baseline-frontend.log', 'utf8');
  const retiredTests = deletedTests.map(file => {
    const relative = file.slice('frontend/'.length);
    return { file, count: baselineCaseCount(baseline, relative) };
  });
  const mutation = fs.readFileSync('/tmp/dsa-cleanup-mutation-red.log', 'utf8');
  const failures = mutation.split('\n').filter(line => line.startsWith(' FAIL ')).map(line => line.slice(6));
  assert.equal(failures.length, 27, 'Mutation proof is incomplete');
  const rendererBefore = read(beforeRenderer), rendererAfter = read(afterRenderer);
  assert.deepEqual(rendererBefore.picks, rendererAfter.picks, 'Different renderer representatives');
  const rendererChromium = chromiumRenderer ? read(chromiumRenderer) : null;
  if (rendererChromium) {
    assert.equal(rendererChromium.browser, 'chromium', 'Extra sweep must be Chromium');
    assert.deepEqual(rendererBefore.picks, rendererChromium.picks, 'Different Chromium representatives');
  }
  const inputs = read(path.join(__dirname, 'after/inputs/input-results.json'));
  assert.equal(inputs.complete, true, 'Incomplete input journey');
  assert.equal(inputs.results.length, 14, 'Missing input checks');
  assert(inputs.results.every(result => result.ok), 'Failed input check');
  assert.deepEqual(inputs.servedBuild, servedBuild, 'Mixed input/build evidence');
  for (const response of inputs.responses) {
    assert([200, 400].includes(response.status), 'Invalid input response');
    if (response.status === 200) assert(response.steps > 0 && response.truncated === false, 'Incomplete custom run');
    else assert(Object.keys(response.fieldErrors ?? {}).length > 0, 'Rejected input lacks field errors');
  }
  const baselineTests = Number(baseline.match(/Tests\s+(\d+) passed \(\d+\)/)?.[1]);
  const frontendLog = fs.readFileSync('/tmp/dsa-cleanup-final-verified-frontend.log', 'utf8');
  const frontendTests = Number(frontendLog.match(/Tests\s+(\d+) passed \(\d+\)/)?.[1]);
  assert(baselineTests && frontendTests, 'Missing clean frontend suite summary');
  const retiredTestCount = retiredTests.reduce((sum, test) => sum + test.count, 0);
  const excluded = [['/tmp/dsa-cleanup-after-renderers/renderer-manifest.json', 'Backend connection stopped mid-sweep'],
    ['/tmp/dsa-cleanup-after-renderers-final/renderer-manifest.json', 'Chromium closed mid-sweep; isolated heap alternate did not reproduce']]
    .map(([file, reason]) => {
      const attempt = read(file);
      assert.equal(attempt.complete, false, 'Do not exclude a complete attempt');
      return { reason, complete: false, observedRows: attempt.rows.length,
        failures: attempt.rows.filter(row => !row.ok).map(({ id, variant, problems }) => ({ id, variant, problems })) };
    });
  const report = {
    generated: new Date().toISOString(), baseCommit: after.baseCommit, branch: 'chore/retire-legacy-ui',
    publication: 'Owner authorized commit/push/PR/merge on 2026-10-10, conditional on clean verification and CI', servedBuild,
    reachability: verifyReachability(before, after),
    editorParity: compareRows(editorBefore, editorAfter, 40, 'snapshot'),
    chromeParity: compareRows(read(path.join(__dirname, 'before/chrome/chrome-results.json')),
      read(path.join(__dirname, 'after/chrome/chrome-results.json')), 50, 'geometry'),
    rendererBefore: rendererSummary(rendererBefore), rendererAfter: rendererSummary(rendererAfter),
    ...(rendererChromium ? { rendererAfterChromium: rendererSummary(rendererChromium) } : {}),
    inputJourney: { checks: inputs.results.length, responses: inputs.responses.length },
    frontend: { baselineTests, finalTests: frontendTests, retiredTests: retiredTestCount,
      addedTests: frontendTests - baselineTests + retiredTestCount },
    unchanged: ['Backend and golden fixtures', 'Frontend dependencies and lockfile'],
    retiredTests, retiredTestCount, excludedAttempts: excluded,
    mutationProof: { failures, restored: true },
    limits: ['Production-loaded modules, not unused exports or every CSS selector',
      'Renderer drawing/overflow/error checks, not answer correctness or every input',
      'Editor defaults are fresh real responses replayed unchanged, not new custom executions',
      'No real device, virtual keyboard, screen reader or learner certification']
  };
  fs.writeFileSync(path.join(__dirname, 'verification.json'), JSON.stringify(report, null, 2) + '\n');
  // Retain the exact generated JSON, not the entire temporary screenshot galleries.
  fs.copyFileSync(beforeRenderer, path.join(__dirname, 'before/renderer-manifest.json'));
  fs.copyFileSync(afterRenderer, path.join(__dirname, 'after/renderer-manifest.json'));
  if (chromiumRenderer) fs.copyFileSync(chromiumRenderer, path.join(__dirname, 'after/renderer-chromium-manifest.json'));
  console.log(JSON.stringify(report, null, 2));
}

module.exports = { compareRows, rendererSummary, verifyReachability, baselineCaseCount };
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
