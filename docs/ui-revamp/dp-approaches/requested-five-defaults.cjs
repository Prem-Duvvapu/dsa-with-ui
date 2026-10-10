// Exercise preserved fourth options and LIS's untouched draft against tighter recursive bounds.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const pace = require('../evidence/pace-executions.cjs')();
(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = await chromium.launch();
  const rows = [];
  try {
    for (const id of ['longest-increasing-subsequence', 'min-insertions-palindrome', 'ninja-and-his-friends'])
      for (const [width, height] of [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]])
        for (const theme of ['light', 'dark']) {
          const page = await browser.newPage({ viewport: { width, height }, colorScheme: theme });
          await pace(page);
          await page.addInitScript(theme => {
            localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
          }, theme);
          const errors = []; let executions = 0;
          page.on('pageerror', error => errors.push(error.message));
          page.on('request', r => { if (new URL(r.url()).pathname.endsWith('/execute')) executions++; });
          const waiting = page.waitForResponse(r => new URL(r.url()).pathname.endsWith('/execute') && r.status() === 200);
          await page.goto(`${base}/problem/${id}`);
          const run = await (await waiting).json();
          assert.equal(run.approachId, 'canonical');
          assert.equal(run.dsType, 'DpTable');
          const select = page.getByRole('combobox', { name: 'Solution approach', exact: true });
          await select.waitFor();
          assert.equal(await select.inputValue(), 'canonical');
          assert.equal(await select.locator('option').count(), id === 'longest-increasing-subsequence' ? 3 : 4);
          const label = await select.locator('option:checked').textContent();
          assert(id === 'longest-increasing-subsequence' ? label === 'Tabulation' : label.includes('(existing)'));
          await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
          const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
          if (await sourceButton.isVisible()) await sourceButton.click();
          assert(await page.getByRole('region', { name: 'Java source', exact: true }).isVisible());
          await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
          assert.equal(executions, 1);
          assert.equal(new URL(page.url()).searchParams.get('approach'), null);
          if (id === 'longest-increasing-subsequence') {
            await page.getByRole('tab', { name: 'Playground', exact: true }).click();
            await page.getByRole('button', { name: 'Edit input', exact: true }).click();
            await select.selectOption('recursion');
            assert.equal(await page.getByLabel('Position 8 value', { exact: true }).inputValue(), '18', 'Untouched canonical draft was silently shortened');
            const oldLink = page.url();
            const waiting = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute'));
            await page.getByRole('button', { name: 'Run input', exact: true }).click();
            assert.equal((await waiting).status(), 400);
            await page.getByRole('alert', { name: 'This input could not run' }).waitFor();
            assert.equal(page.url(), oldLink);
            assert.equal(await page.getByLabel('Position 8 value', { exact: true }).inputValue(), '18');
            assert(await page.getByRole('alert', { name: 'This input could not run' }).evaluate(e => document.activeElement === e));
          }
          assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
          assert.deepEqual(errors, []);
          rows.push({ problemId: id, width, height, theme, defaultApproach: 'canonical', label, noOverflowOrErrors: true,
            viewsDoNotExecute: true, untouchedDraftPreservedAndRefused: id === 'longest-increasing-subsequence' });
          console.log(`PASS preserved default ${id} ${width}×${height} ${theme}`);
          await page.close();
        }
    fs.writeFileSync(path.join(__dirname, 'requested-five', 'preserved-defaults.json'), JSON.stringify({ generated: new Date().toISOString(),
      realBackend: true, buildMode: 'production-preview', engine: 'chromium', browserVersion: browser.version(), servedBuild, rows }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
