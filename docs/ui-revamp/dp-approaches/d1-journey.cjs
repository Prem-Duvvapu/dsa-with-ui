const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const pace = require('../evidence/pace-executions.cjs')();
async function json(route, status = 200, options) {
  const response = await fetch(`${api}${route}`, options);
  if (response.status !== status) throw Error(`${route}: HTTP ${response.status}, expected ${status}`);
  return response.json();
}
function assert(condition, message) { if (!condition) throw Error(message); }
(async () => {
  const stats = await json('/api/problems/stats');
  const problems = await json('/api/problems');
  let inspected = 0;
  for (const problem of problems) {
    const detail = await json(`/api/problems/${problem.id}`);
    assert(detail.approaches.length === 1, `${problem.id}: options advertised without implementations`);
    const selected = detail.approaches[0];
    assert(selected.id === detail.approachId && selected.id === detail.defaultApproachId && selected.isDefault,
      `${problem.id}: inconsistent default identity`);
    assert(selected.dsType === detail.dsType, `${problem.id}: wrong renderer`);
    inspected++;
  }
  const detail = await json('/api/problems/climbing-stairs');
  for (const encoding of ['full', 'delta']) {
    const run = await json(`/api/problems/climbing-stairs/execute?approach=tabulation&encoding=${encoding}`);
    assert(run.approachId === 'tabulation' && run.dsType === 'DpTable' && run.code === detail.javaCode,
      `Execution source/identity mismatch: ${encoding}`);
  }
  const rejected = await json('/api/problems/climbing-stairs/execute?approach=memoization', 400);
  assert(rejected.error === 'unavailable_approach', 'Unknown approach substituted');
  const badEncoding = await json('/api/problems/climbing-stairs/execute?encoding=arbitrary', 400);
  assert(badEncoding.error === 'unsupported_encoding', 'Unsupported encoding accepted');
  const custom = await json('/api/problems/climbing-stairs/execute?approach=tabulation', 200, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ n: 7 })
  });
  assert(custom.resolvedInput.n === 7 && custom.approachId === 'tabulation', 'Custom input/approach mismatch');
  const browser = await chromium.launch();
  const rows = [];
  try {
    for (const width of [320, 1366]) for (const theme of ['light', 'dark']) {
      const page = await browser.newPage({ viewport: { width, height: 768 }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      const errors = [];
      page.on('pageerror', error => errors.push(error.message));
      const response = page.waitForResponse(r => new URL(r.url()).pathname === '/api/problems/climbing-stairs/execute');
      await page.goto(`${base}/problem/climbing-stairs?view=code`);
      const run = await (await response).json();
      const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
      if (await sourceButton.isVisible()) await sourceButton.click();
      const source = page.getByRole('region', { name: 'Java source', exact: true });
      await source.waitFor();
      assert((await source.textContent()).includes('climbStairs'), 'Old workspace stopped displaying source');
      const seek = page.getByRole('slider', { name: 'Seek to step' });
      await seek.fill(String(run.stepCount - 1));
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      assert(await seek.inputValue() === String(run.stepCount - 1), 'View switch lost run position');
      const overflow = await page.evaluate(() => document.documentElement.scrollWidth - innerWidth);
      assert(overflow <= 0 && errors.length === 0, `Browser errors/overflow: ${errors}, ${overflow}`);
      rows.push({ width, theme, steps: run.stepCount, approachId: run.approachId, passed: true });
      await page.close();
    }
    fs.writeFileSync(path.join(__dirname, 'd1-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, stats, inspectedCanonicalDetails: inspected,
      fullAndDeltaChecked: true, customInputChecked: true, unavailableApproachRefused: true,
      unknownEncodingRefused: true, rows
    }, null, 2) + '\n');
    console.log(`PASS ${inspected} canonical details, API refusal/selection checks and ${rows.length} browser rows`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
