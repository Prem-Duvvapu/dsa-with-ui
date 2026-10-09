const { test } = require('node:test');
const assert = require('node:assert/strict');
const identity = require('./served-build-identity.cjs');

test('identifies served same-origin JS/CSS without trusting a local build or external asset', async t => {
  t.mock.method(global, 'fetch', async () => ({ ok: true, text: async () =>
    '<script src="/assets/index-real.js"></script><link href="/assets/index-real.css"><link href="/assets/index-real.css"><script src="https://elsewhere.invalid/assets/foreign.js"></script>' }));
  assert.deepEqual(await identity('http://127.0.0.1:5180/'), {
    assets: ['/assets/index-real.css', '/assets/index-real.js']
  });
});
test('refuses an HTTP failure or a non-production HTML response', async t => {
  t.mock.method(global, 'fetch', async () => ({ ok: false, status: 403 }));
  await assert.rejects(identity('http://127.0.0.1:5180/'), /HTTP 403/);
  global.fetch = async () => ({ ok: true, text: async () => '<script src="/src/main.jsx"></script>' });
  await assert.rejects(identity('http://127.0.0.1:5180/'), /production JS\/CSS/);
});
