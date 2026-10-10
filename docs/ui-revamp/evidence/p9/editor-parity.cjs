// Fresh backend defaults replayed unchanged. Compare editor geometry/computed styles
// before/after retiring the panel variant; view changes must not execute again.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const identity = require('../served-build-identity.cjs');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const out = process.argv[2];
assert(out, 'Pass a fresh evidence output directory');
const ids = ['two-sum', 'bfs-traversal', 'longest-common-subsequence', 'frog-jump'];
const sizes = [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const servedBuild = await identity(base);
  async function capture(url) {
    const response = await fetch(api + url);
    assert.equal(response.status, 200, url);
    return response.json();
  }
  const catalogue = await capture('/api/problems');
  const details = {}, traces = {};
  for (const id of ids) {
    details[id] = await capture(`/api/problems/${id}`);
    traces[id] = await capture(`/api/problems/${id}/execute`);
    await new Promise(resolve => setTimeout(resolve, 1100));
  }
  const browser = await chromium.launch();
  const rows = [];
  const save = complete => fs.writeFileSync(path.join(out, 'editor-results.json'), JSON.stringify({
    generated: new Date().toISOString(), servedBuild, complete,
    responseSource: 'Fresh real-backend default responses, replayed without alteration', rows
  }, null, 2));
  try {
    for (const theme of ['light', 'dark']) for (const [width, height] of sizes) {
      const context = await browser.newContext({ viewport: { width, height }, colorScheme: theme, reducedMotion: 'reduce' });
      await context.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const page = await context.newPage();
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      let executions = 0;
      await page.route('**/api/problems**', route => {
        const url = new URL(route.request().url());
        if (url.pathname === '/api/problems') return route.fulfill({ json: catalogue });
        const match = url.pathname.match(/^\/api\/problems\/([^/]+)(\/execute)?$/);
        assert(match && ids.includes(match[1]), `Unexpected request: ${url}`);
        assert.equal(route.request().method(), 'GET', 'This parity probe never submits an invented custom run');
        if (match[2]) executions++;
        return route.fulfill({ json: match[2] ? traces[match[1]] : details[match[1]] });
      });
      for (const id of ids) {
        await page.goto(`${base}/problem/${id}`, { waitUntil: 'networkidle' });
        const editor = page.getByRole('region', { name: 'Try your own input', exact: true });
        await editor.getByRole('button', { name: 'Run input', exact: true }).waitFor();
        const before = executions;
        const field = editor.locator('input').first();
        const original = await field.inputValue();
        // Same value plus an input event exercises continuity without changing the run.
        await field.fill(original);
        await page.getByRole('slider', { name: 'Seek to step', exact: true }).fill('2');
        const shared = await page.evaluate(() => new URL(location.href).searchParams.get('step'));
        for (const name of ['Code walkthrough', 'Analysis', 'Playground']) {
          await page.getByRole('tab', { name, exact: true }).click();
          assert.equal(await page.getByRole('slider', { name: 'Seek to step', exact: true }).inputValue(), '2');
        }
        assert.equal(await field.inputValue(), original);
        assert.equal(executions, before, 'View changes reran the algorithm');
        assert.equal(await page.evaluate(() => new URL(location.href).searchParams.get('step')), shared);
        const snapshot = await editor.evaluate(root => {
          const style = element => {
            const c = getComputedStyle(element), r = element.getBoundingClientRect();
            return Object.fromEntries(['display', 'flexDirection', 'flexWrap', 'gap', 'overflow', 'fontSize',
              'fontWeight', 'fontFamily', 'lineHeight', 'color', 'backgroundColor', 'borderTopColor',
              'borderTopWidth', 'borderRadius', 'paddingTop', 'paddingRight', 'paddingBottom', 'paddingLeft',
              'minHeight', 'textTransform', 'letterSpacing'].map(key => [key, c[key]])
              .concat([['width', r.width], ['height', r.height]]));
          };
          const fields = [...root.querySelectorAll('input')];
          const buttons = [...root.querySelectorAll('button')];
          const run = buttons.find(button => button.textContent.trim() === 'Run input');
          return { overflow: document.documentElement.scrollWidth - innerWidth,
            editor: style(root), form: style(run.parentElement.parentElement),
            labels: [...root.querySelectorAll('label')].map(style),
            fields: fields.map(style), buttons: buttons.map(button => ({ text: button.textContent.trim(), ...style(button) })),
            fieldBeforeRun: Boolean(fields[0].compareDocumentPosition(run) & Node.DOCUMENT_POSITION_FOLLOWING) };
        });
        assert(snapshot.fieldBeforeRun);
        assert.equal(snapshot.overflow, 0);
        assert.deepEqual(errors, []);
        rows.push({ id, theme, width, height, snapshot });
        save(false);
        console.log(`PASS ${id} ${width} ${theme}`);
      }
      await context.close();
    }
    save(true);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
