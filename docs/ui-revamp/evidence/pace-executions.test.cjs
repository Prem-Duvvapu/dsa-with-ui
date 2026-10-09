const { test } = require('node:test');
const assert = require('node:assert/strict');
test('execution pacer includes query-bearing approach/encoding URLs only', async () => {
  let matcher;
  await require('./pace-executions.cjs')()({ route: async value => { matcher = value; } });
  const matches = value => {
    const url = new URL(value);
    if (typeof matcher === 'function') return matcher(url);
    // Exercise the old literal glob too: the failure must be its query behavior,
    // not the implementation's choice of a function versus a string.
    const pattern = matcher.split('*').map(part => part.replace(/[.+?^${}()|[\]\\]/g, '\\$&')).join('.*');
    return new RegExp(`^${pattern}$`).test(url.href);
  };
  for (const suffix of ['', '?approach=recursion', '?encoding=delta&approach=memoization']) {
    assert.equal(matches(`http://localhost:5180/api/problems/climbing-stairs/execute${suffix}`), true);
  }
  assert.equal(matches('http://localhost:5180/api/problems/climbing-stairs'), false);
  assert.equal(matches('http://localhost:5180/api/problems/climbing-stairs/execute-extra'), false);
});
