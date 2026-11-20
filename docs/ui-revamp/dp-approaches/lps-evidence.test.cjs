const test = require('node:test');
const assert = require('node:assert/strict');
const { spawnSync } = require('node:child_process');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const { cases, maximum, oracle } = require('./lps-cases.cjs');
test('LPS evidence requires its explicit registered profile', () => {
  const result = spawnSync(process.execPath, ['-e', "require('./dp-audit-profile.cjs')"], {
    cwd: __dirname, env: { ...process.env, DP_AUDIT_FAMILY: 'lps' }
  });
  assert.equal(result.status, 0, result.stderr.toString());
});
function stringControls() {
  const source = fs.readFileSync(path.join(__dirname, 'linear-grid-journey.cjs'), 'utf8');
  const context = { cases, assert };
  vm.runInNewContext(source.slice(source.indexOf('async function edit('), source.indexOf('function assertAnswer('))
    + '\nthis.editInput = edit; this.checkDraft = assertDraft;', context);
  const values = new Map([['Text', '']]);
  const page = {
    getByRole(role, options) {
      assert.equal(role, 'button'); assert.equal(options.name, 'Edit input');
      return { isVisible: async () => false };
    },
    getByLabel(label, options) {
      assert.equal(label, 'Text', 'A one-field owner cannot silently borrow LCS first/second fields'); assert.equal(options.exact, true);
      return { fill: async value => values.set(label, value), inputValue: async () => values.get(label) };
    }
  };
  return { ...context, page, values };
}
test('existing string editor helper fills the single real Text field', async () => {
  const { editInput, page, values } = stringControls(); const c = cases[0];
  await editInput(page, c.id, c.input); assert.equal(values.get('Text'), 'bbbab'); assert.equal(values.size, 1);
});
test('existing draft reader checks the single field and notices a wrong retained value', async () => {
  const { checkDraft, page, values } = stringControls(); const c = cases[0];
  values.set('Text', c.input.text); await checkDraft(page, c.id, c.input);
  values.set('Text', c.draftStringValue); await assert.rejects(checkDraft(page, c.id, c.input));
});
test('direct palindrome reference preserves empty/base, order and repeated-character answers', () => {
  const id = cases[0].id;
  for (const [text, expected] of [['', 0], ['a', 1], ['bbbab', 4], ['cbbd', 2], ['abcde', 1], ['abcba', 5],
    ['aaaaa', 5], ['aabab', 3], ['character', 5], ['abcdefghijkl', 1]]) {
    assert.equal(oracle(id, { text }), expected);
    assert.equal(oracle(id, { text: [...text].reverse().join('') }), expected);
  }
  assert.throws(() => oracle('invented', { text: '' }));
});
test('authored one-field drafts preserve defaults and change the independent comparison answer', () => {
  const c = cases[0]; assert.equal(String(oracle(c.id, c.input)), c.answer);
  assert.equal(oracle(c.id, { [c.draftStringField]: c.draftStringValue }), 5);
  assert.notEqual(String(oracle(c.id, { text: c.draftStringValue })), c.answer);
  for (const form of ['recursion', 'memoization', 'canonical']) assert.deepEqual(c.defaults[form], { text: 'bbbab' });
  for (const input of [c.input, ...Object.values(c.extra)]) {
    assert.deepEqual(Object.keys(input), ['text']); assert(/^[a-z]+$/.test(input.text)); assert(input.text.length <= 5);
  }
  assert.equal(c.rejected.text.length, 6);
});
function shape(text, memo) {
  const reversed = [...text].reverse().join(''), known = new Map(); let calls = 0, branches = 0;
  function visit(i, j) {
    calls++; const key = `${i},${j}`;
    if (memo && known.has(key)) return known.get(key);
    let value = 0;
    if (i !== 0 && j !== 0) {
      branches++;
      value = text[i - 1] === reversed[j - 1] ? 1 + visit(i - 1, j - 1) : Math.max(visit(i - 1, j), visit(i, j - 1));
    }
    if (memo) known.set(key, value);
    return value;
  }
  const answer = visit(text.length, text.length);
  return { calls, events: 3 * calls + branches + 2, answer };
}
test('reverse-correlated stress witnesses pin real call and event maxima separately', () => {
  const c = cases[0];
  assert.deepEqual(shape(maximum(c.id, 'recursion').text, false), { calls: 155, events: 552, answer: 1 });
  assert.deepEqual(shape(maximum(c.id, 'memoization').text, true), { calls: 107, events: 383, answer: 2 });
  assert.deepEqual(shape('abcdefgh', true), { calls: 107, events: 380, answer: 1 });
  assert.equal(maximum(c.id, 'canonical').text.length, 12);
  assert.throws(() => maximum('invented', 'recursion')); assert.throws(() => maximum(c.id, 'invented'));
});
test('actual sampler API helpers keep two-prefix LCS states distinct from prefix palindrome length', () => {
  const { callCount, prefixOracle } = require('./lps-api.cjs');
  assert.equal(callCount({ text: 'abcde' }), 155);
  assert.equal(prefixOracle('ab', 'ba'), 1);
  // For text=abba, the two length2 prefixes are both ab. Their LCS is2 although
  // ab itself has no length2 palindromic subsequence. Do not use LPS(prefix) here.
  assert.equal(prefixOracle('ab', 'ab'), 2);
  assert.equal(oracle(cases[0].id, { text: 'ab' }), 1);
});
