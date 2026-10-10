const { test } = require('node:test');
const assert = require('node:assert/strict');
const { compareRows, rendererSummary, verifyReachability, baselineCaseCount } = require('./cleanup-verification.cjs');
test('reads a completed baseline count instead of an earlier stderr header', () => {
  assert.equal(baselineCaseCount('stderr | src/Example.test.jsx > case\n ✓ src/Example.test.jsx  (6 tests) 123ms', 'src/Example.test.jsx'), 6);
  assert.throws(() => baselineCaseCount('stderr | src/Example.test.jsx > case', 'src/Example.test.jsx'), /No baseline/);
});
const row = { id: 'fixture', theme: 'dark', width: 390, height: 844, snapshot: { width: 300 } };
const browser = { complete: true, rows: [row] };
test('accepts identical complete matrices', () => assert.deepEqual(compareRows(browser, browser, 1, 'snapshot'), { rows: 1, comparable: 1, identical: 1 }));
test('rejects missing editor measurements instead of comparing two undefined snapshots', () => {
  const missing = { complete: true, rows: [{ ...row, snapshot: undefined }] };
  assert.throws(() => compareRows(missing, missing, 1, 'snapshot'), /Missing measured/);
});
test('rejects incomplete matrices', () => assert.throws(() => compareRows(browser, { ...browser, complete: false }, 1, 'snapshot'), /Incomplete/));
test('rejects duplicate rows', () => assert.throws(() => compareRows({ complete: true, rows: [row, row] }, browser, 2, 'snapshot'), /Duplicate/));
test('rejects geometry changes', () => assert.throws(() => compareRows(browser, { ...browser, rows: [{ ...row, snapshot: { width: 301 } }] }, 1, 'snapshot'), /Layout changed/));
test('rejects differing matrices and browser failures', () => {
  assert.throws(() => compareRows(browser, { ...browser, rows: [{ ...row, id: 'other' }] }, 1, 'snapshot'), /Different/);
  assert.throws(() => compareRows(browser, { ...browser, rows: [{ ...row, failures: ['overflow'] }] }, 1, 'snapshot'), /Browser failures/);
});
const picks = [{ id: 'fixture', dsType: 'Array' }];
const positions = Object.fromEntries(['first', 'middle', 'final'].map(name => [name, { overflow: 0, drawn: true, unsupported: false }]));
const renderer = { complete: true, picks, rows: [['1366x768', 'dark'], ['390x844', 'light']].flatMap(([viewport, scheme]) =>
  ['default', 'alternate'].map(variant => ({ ...picks[0], viewport, scheme, variant, ok: true, problems: [], steps: 3, positions }))) };
test('accepts complete renderer coverage', () => assert.deepEqual(rendererSummary(renderer), { browser: undefined, rows: 4, families: 1, problems: 1, positionChecks: 12 }));
test('rejects missing, duplicate, failed or empty renderer rows', () => {
  assert.throws(() => rendererSummary({ ...renderer, rows: renderer.rows.slice(1) }), /Missing/);
  assert.throws(() => rendererSummary({ ...renderer, rows: [renderer.rows[0], ...renderer.rows.slice(0, 3)] }), /duplicate/);
  assert.throws(() => rendererSummary({ ...renderer, rows: [{ ...renderer.rows[0], ok: false }, ...renderer.rows.slice(1)] }), /Failed/);
  assert.throws(() => rendererSummary({ ...renderer, rows: [{ ...renderer.rows[0], steps: 0 }, ...renderer.rows.slice(1)] }), /No steps/);
});
const before = { sourceFiles: ['dead', 'live'], productionModules: [{ file: 'live' }], unreachableFiles: ['dead'] };
const after = { sourceFiles: ['live'], productionModules: [{ file: 'live' }], unreachableFiles: [] };
test('accepts only deletion of unreachable source', () => assert.deepEqual(verifyReachability(before, after), { before: 2, after: 1, retired: 1, productionLoaded: 1 }));
test('rejects an orphan or a changed production graph', () => {
  assert.throws(() => verifyReachability(before, { ...after, unreachableFiles: ['live'] }), /Orphan/);
  assert.throws(() => verifyReachability(before, { ...after, productionModules: [] }), /production module/);
});
