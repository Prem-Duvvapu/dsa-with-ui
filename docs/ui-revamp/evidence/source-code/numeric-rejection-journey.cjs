// Real backend regression: rejected overflow must not replace a successful session.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const playwright = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const servedBuildIdentity = require('../served-build-identity.cjs');
const pace = require('../pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const output = process.argv[2];
if (!output) throw Error('An evidence output directory is required');

(async () => {
  const servedBuild = await servedBuildIdentity(base);
  const rows = [];
  for (const engine of ['chromium', 'firefox']) {
    const browser = await playwright[engine].launch();
    try {
      for (const width of [320, 1366]) for (const theme of ['light', 'dark']) {
        const page = await browser.newPage({ viewport: { width, height: 768 }, colorScheme: theme });
        await pace(page);
        await page.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        let requests = 0;
        const errors = [];
        page.on('pageerror', e => errors.push(e.message));
        const endpoint = response => new URL(response.url()).pathname === '/api/problems/count-good-numbers/execute';
        page.on('request', request => { if (endpoint(request)) requests++; });
        const initial = page.waitForResponse(endpoint);
        await page.goto(`${base}/problem/count-good-numbers`);
        assert.equal((await initial).status(), 200);
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        const field = page.getByLabel('Length (N)', { exact: true });
        await field.fill('2');
        const success = page.waitForResponse(endpoint);
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const run = await (await success).json();
        assert.deepEqual(run.resolvedInput, { n: 2 });
        assert.equal(run.steps.at(-1).variables.answer, '20');
        const seek = page.getByRole('slider', { name: 'Seek to step' });
        await seek.fill('2');
        await page.waitForURL(url => url.searchParams.get('step') === '3' && url.searchParams.has('input'));
        const shared = new URL(page.url()).search;
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        await field.fill('4294967297');
        const rejection = page.waitForResponse(endpoint);
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const response = await rejection;
        assert.equal(response.status(), 400);
        const body = await response.json();
        assert.equal(body.error, 'invalid_input');
        assert.match(body.fieldErrors.n, /32-bit/);
        assert.equal(body.steps, undefined);
        const summary = page.getByRole('alert').filter({ hasText: 'This input could not run' });
        await summary.waitFor();
        assert.equal(await summary.evaluate(el => el === document.activeElement), true);
        assert.equal(await field.inputValue(), '4294967297');
        assert.equal(new URL(page.url()).search, shared);
        assert.equal(await seek.inputValue(), '2');
        await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
        const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
        if (await sourceButton.isVisible()) await sourceButton.click();
        const source = page.getByRole('region', { name: 'Java source', exact: true });
        await source.waitFor();
        // Every exact source line still belongs to the successful n=2 execution.
        for (const line of run.code.split('\n').filter(line => line.trim())) {
          assert.ok((await source.textContent()).includes(line.trim()), line);
        }
        assert.equal(await seek.inputValue(), '2');
        assert.equal(new URL(page.url()).searchParams.get('input'), new URLSearchParams(shared).get('input'));
        assert.equal(requests, 3);
        assert.deepEqual(errors, []);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
        rows.push({ engine, version: browser.version(), width, theme, passed: true,
          status: response.status(), fieldError: body.fieldErrors.n, executions: requests,
          previousInput: run.resolvedInput, previousStep: 3, draft: '4294967297', servedBuild });
        await page.close();
      }
    } finally { await browser.close(); }
  }
  fs.mkdirSync(output, { recursive: true });
  fs.writeFileSync(path.join(output, 'browser-results.json'), JSON.stringify({ generated: new Date().toISOString(), rows }, null, 2));
  console.log(JSON.stringify({ passed: rows.length, servedBuild }));
})().catch(error => { console.error(error); process.exitCode = 1; });
