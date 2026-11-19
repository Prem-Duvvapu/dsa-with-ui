const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { cases, maximum, oracle } = require('./lcs-cases.cjs');
test('LCS evidence requires its explicit registered profile', () => {
  const result = spawnSync(process.execPath, ['-e', "require('./dp-audit-profile.cjs')"], {
    cwd: __dirname, env: { ...process.env, DP_AUDIT_FAMILY: 'lcs' }
  });
  assert.equal(result.status, 0, result.stderr.toString());
});
function stringControls() {
  const source = fs.readFileSync(path.join(__dirname, 'linear-grid-journey.cjs'), 'utf8');
  const context = { cases, assert };
  vm.runInNewContext(source.slice(source.indexOf('async function edit('), source.indexOf('function assertAnswer('))
    + '\nthis.editInput = edit; this.checkDraft = assertDraft;', context);
  const values = new Map([['First string', ''], ['Second string', '']]);
  const page = {
    getByRole(role, options) {
      assert.equal(role, 'button'); assert.equal(options.name, 'Edit input');
      return { isVisible: async () => false };
    },
    getByLabel(label, options) {
      assert(values.has(label), `Unknown accessible string label ${label}`); assert.equal(options.exact, true);
      return { fill: async value => values.set(label, value), inputValue: async () => values.get(label) };
    }
  };
  return { ...context, page, values };
}
test('LCS editor helper fills both labelled text fields without array operations', async () => {
  const { editInput, page, values } = stringControls(); const c = cases[0];
  await editInput(page, c.id, c.input);
  assert.equal(values.get('First string'), c.input.first); assert.equal(values.get('Second string'), c.input.second);
});
test('LCS draft helper reads both strings and detects a wrong retained value', async () => {
  const { checkDraft, page, values } = stringControls(); const c = cases[0];
  values.set('First string', c.input.first); values.set('Second string', c.input.second);
  await checkDraft(page, c.id, c.input);
  values.set('First string', 'wrong'); await assert.rejects(checkDraft(page, c.id, c.input));
});
test('independent subsequence reference preserves empty, zero, order and repeated characters', () => {
  const id = cases[0].id;
  for (const [first, second, answer] of [['', 'abc', 0], ['abc', '', 0], ['abc', 'xyz', 0], ['abcde', 'ace', 3],
    ['aggtab', 'gxtxayb', 4], ['abab', 'baba', 3], ['aa', 'a', 1], ['abc', 'cba', 1]]) {
    assert.equal(oracle(id, { first, second }), answer);
    assert.equal(oracle(id, { first: second, second: first }), answer);
  }
  assert.throws(() => oracle('invented', { first: '', second: '' }));
});
test('all authored standard cases fit recursion and a dirty string changes the answer', () => {
  const c = cases[0]; assert.equal(String(oracle(c.id, c.input)), c.answer);
  for (const input of [c.input, ...Object.values(c.extra)]) {
    assert(input.first.length <= 4 && input.second.length <= 4);
    assert(/^[a-z]+$/.test(input.first) && /^[a-z]+$/.test(input.second));
  }
  assert.notEqual(String(oracle(c.id, { ...c.input, [c.draftStringField]: c.draftStringValue })), c.answer);
  assert.equal(c.rejected.first.length, 5);
});
test('stress fixtures use exact disjoint bounds and preserve canonical defaults', () => {
  const c = cases[0];
  const count = (m, n) => m === 0 || n === 0 ? 1 : 1 + count(m - 1, n) + count(m, n - 1);
  const recursion = maximum(c.id, 'recursion'); assert.equal(count(recursion.first.length, recursion.second.length), 139);
  assert.equal(maximum(c.id, 'memoization').first.length, 8);
  assert.equal(maximum(c.id, 'canonical').first.length, 12);
  assert.deepEqual(c.defaults.canonical, { first: 'abcde', second: 'ace' });
  assert.throws(() => maximum('invented', 'recursion')); assert.throws(() => maximum(c.id, 'invented'));
});
