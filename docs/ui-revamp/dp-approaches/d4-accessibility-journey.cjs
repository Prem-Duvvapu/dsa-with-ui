const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const pace = require('../evidence/pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
function assert(value, message) { if (!value) throw Error(message); }
(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = await chromium.launch();
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const rows = [];
  try {
    for (const config of [
      { width: 680, height: 360, theme: 'light', deniedStorage: false },
      { width: 390, height: 900, theme: 'dark', deniedStorage: false },
      { width: 390, height: 900, theme: 'light', deniedStorage: true }
    ]) {
      const page = await browser.newPage({ viewport: { width: config.width, height: config.height },
        colorScheme: config.theme, reducedMotion: 'reduce' });
      await page.addInitScript(config => {
        if (config.deniedStorage) {
          for (const name of ['localStorage', 'sessionStorage']) Object.defineProperty(window, name, {
            configurable: true, get() { throw new DOMException('Storage denied', 'SecurityError'); }
          });
        } else {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(config.theme));
        }
      }, config);
      await pace(page);
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.goto(`${base}/problem/climbing-stairs`);
      if (config.deniedStorage) await page.getByRole('button', { name: 'Start exploring', exact: true }).click();
      const selector = page.getByRole('combobox', { name: 'Solution approach' });
      await selector.waitFor();
      await page.waitForFunction(() => !document.querySelector('select')?.disabled);
      await selector.selectOption('memoization');
      const [response] = await Promise.all([
        page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute')),
        page.getByRole('button', { name: 'Run memoization', exact: true }).click()
      ]);
      assert(response.status() === 200, `Memo request HTTP ${response.status()}`);
      const steps = decodeTrace(await response.json());
      const hit = steps.findIndex(s => s.variables?.event === 'cache-hit');
      assert(hit > 0, 'No actual cache hit in pilot');
      await page.waitForFunction(() => document.querySelector('[role="status"][aria-label="Solution approach"]')?.textContent.includes('Showing: Memoization'));
      await page.getByText('How the shown approach works', { exact: true }).click();
      const seek = page.getByRole('slider', { name: 'Seek to step', exact: true });
      await seek.fill(String(hit - 1));
      assert(!await page.getByText('Return the recorded memo value without expanding children.', { exact: true }).isVisible(), 'Cache explanation invented before hit');
      await seek.fill(String(hit));
      assert(await page.getByText('Return the recorded memo value without expanding children.', { exact: true }).isVisible(), 'Cache explanation missing at actual hit');
      await page.getByRole('button', { name: 'Compare approaches', exact: true }).click();
      const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
      const button = panel.getByRole('button', { name: 'Run comparison', exact: true });
      await button.click();
      await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
      await button.focus();
      await page.keyboard.press('Tab');
      await page.keyboard.press('Shift+Tab');
      const outline = await button.evaluate(e => ({ width: getComputedStyle(e).outlineWidth, style: getComputedStyle(e).outlineStyle }));
      assert(parseFloat(outline.width) >= 2 && outline.style !== 'none', 'No visible keyboard focus ring');
      await page.keyboard.press('Control+k');
      await page.getByRole('dialog').waitFor();
      await page.keyboard.press('Escape');
      assert(await page.getByRole('dialog').count() === 0, 'Switcher did not close');
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide overflow');
      assert(errors.length === 0, `Browser errors: ${errors}`);
      rows.push({ ...config, productionPreview: true, reducedMotion: true, realCacheEventTeaching: true,
        comparisonWorks: true, visibleFocus: outline, globalSwitcherShortcut: true, noOverflowOrErrors: true });
      console.log(`PASS ${config.width}x${config.height} ${config.theme} storageDenied=${config.deniedStorage}`);
      await page.close();
    }
    fs.writeFileSync(path.join(__dirname, 'd4-accessibility-results.json'), JSON.stringify({ generated: new Date().toISOString(),
      engine: 'Chromium', servedBuild, rows, scope: 'Targeted production-preview checks; not screen-reader, real-device, second-engine or 200% real-browser-zoom certification' }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
