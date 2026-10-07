// Real-backend source/highlight journey. PLAYWRIGHT_MODULE may point to a local install.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const root = path.resolve(__dirname, '../../../..');
const out = process.argv[2] || __dirname;
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const pace = require('../pace-executions.cjs')();
const ids = fs.readdirSync(path.join(root, 'backend/src/main/resources/solutions/dp'))
  .filter(name => name.endsWith('.java')).map(name => name.slice(0, -5)).sort();
const representatives = ['longest-common-subsequence', 'shortest-common-supersequence',
  'longest-string-chain', 'stock-transaction-fee', 'longest-bitonic-subsequence', 'palindrome-partitioning-2'];
const placeholder = /initialiseBaseCases|evaluateTransitionCandidates|extractAnswer|reconstructChosenSolution/;

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const details = {};
  for (const id of ids) {
    const response = await fetch(`${api}/api/problems/${id}`);
    if (!response.ok) throw Error(`${id}: HTTP ${response.status}`);
    details[id] = await response.json();
    if (!details[id].javaCode.includes('public class Solution') || placeholder.test(details[id].javaCode)) {
      throw Error(`${id}: complete solution is not served`);
    }
  }
  const browser = await chromium.launch();
  const rows = [];
  const save = complete => fs.writeFileSync(path.join(out, 'results.json'), JSON.stringify({
    generated: new Date().toISOString(), browser: 'Chromium', realBackend: true,
    complete, completeSources: ids, rows
  }, null, 2));
  try {
    for (const theme of ['light', 'dark']) for (const width of [320, 1366]) {
      const context = await browser.newContext({ viewport: { width, height: width === 320 ? 568 : 768 },
        colorScheme: theme, reducedMotion: 'reduce' });
      await context.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const page = await context.newPage();
      await pace(page);
      let errors = [];
      page.on('pageerror', error => errors.push(error.message));
      for (const id of representatives) {
        errors = [];
        // Source detail can render before a paced execution has completed.
        // Await the real response explicitly; networkidle is not a run-ready signal.
        const executionResponse = page.waitForResponse(response =>
          new URL(response.url()).pathname === `/api/problems/${id}/execute`, { timeout: 30000 });
        await page.goto(`${base}/problem/${id}?view=code`, { waitUntil: 'networkidle' });
        const response = await executionResponse;
        if (!response.ok()) throw Error(`${id}: execution HTTP ${response.status()}`);
        const execution = await response.json();
        await page.getByRole('slider', { name: 'Seek to step' }).waitFor();
        const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
        if (await sourceButton.isVisible()) await sourceButton.click();
        const source = page.getByRole('region', { name: 'Java source', exact: true });
        await source.waitFor();
        const failures = [];
        if (!execution || !execution.steps?.length || execution.truncated) throw Error(`${id}: no complete live run`);
        if (execution.code !== details[id].javaCode) failures.push('detail and execution source disagree');
        const visibleLines = await source.locator('div[data-active-line], div[class*="line_"]').count();
        const text = await source.textContent();
        if (!text.includes('public class Solution') || placeholder.test(text)) failures.push('placeholder or incomplete displayed source');
        const slider = page.getByRole('slider', { name: 'Seek to step' });
        const total = Number(await slider.getAttribute('max')) + 1;
        const selected = [0, Math.floor(total / 2), total - 1];
        for (const index of selected) {
          await slider.fill(String(index));
          await page.waitForTimeout(80);
          const row = source.locator('[data-active-line="true"]');
          const expectedLine = execution.steps[index].activeLine;
          if (await row.count() !== 1) failures.push(`step ${index + 1}: active row missing/duplicated`);
          else {
            const displayed = await row.locator('span[class*="lineText"]').textContent();
            const expected = execution.code.split('\n')[expectedLine - 1];
            if (displayed.trim() !== expected.trim()) failures.push(`step ${index + 1}: wrong highlighted statement`);
          }
        }
        // Source owns scrolling keys; returning to it after Analysis keeps the same step.
        const retained = await slider.inputValue();
        await source.focus();
        await page.keyboard.press('End');
        if (await slider.inputValue() !== retained) failures.push('source End sought the trace');
        await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
        await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
        if (await sourceButton.isVisible()) await sourceButton.click();
        if (await page.getByRole('slider', { name: 'Seek to step' }).inputValue() !== retained) failures.push('view switch lost step');
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - innerWidth);
        if (overflow > 0) failures.push(`page overflow ${overflow}`);
        failures.push(...errors);
        rows.push({ id, theme, width, steps: total, inspected: selected.map(i => i + 1), visibleLines, failures });
        save(false);
        console.log(`${failures.length ? 'FAIL' : 'PASS'} ${id} ${width} ${theme}`);
        if (id === 'longest-string-chain') await page.screenshot({ path: path.join(out, `source-${width}-${theme}.png`) });
      }
      await context.close();
    }
    save(true);
    if (rows.some(row => row.failures.length)) process.exitCode = 1;
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
