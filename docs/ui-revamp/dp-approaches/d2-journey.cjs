// D2 backend pilot evidence. D3 selector/restoration UI is intentionally not claimed here.
const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');
const { isDeepStrictEqual } = require('util');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const pace = require('../evidence/pace-executions.cjs')();
function assert(condition, message) { if (!condition) throw Error(message); }
async function request(route, status = 200, body) {
  if (route.includes('/execute')) await new Promise(resolve => setTimeout(resolve, 1100));
  const response = await fetch(`${api}${route}`, body === undefined ? undefined : {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
  });
  const text = await response.text();
  assert(response.status === status, `${route}: HTTP ${response.status}, expected ${status}: ${text}`);
  return { json: JSON.parse(text), bytes: Buffer.byteLength(text) };
}
(async () => {
  const { decodeTrace } = await import(pathToFileURL(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js')));
  const stats = (await request('/api/problems/stats')).json;
  const cases = [];
  for (const [approach, maximum] of [['recursion', 10], ['memoization', 30], ['tabulation', 30]]) {
    const detail = (await request(`/api/problems/climbing-stairs?approach=${approach}`)).json;
    for (const n of [1, 5, maximum]) {
      const route = `/api/problems/climbing-stairs/execute?approach=${approach}`;
      const full = await request(`${route}&encoding=full`, 200, { n });
      const delta = await request(`${route}&encoding=delta`, 200, { n });
      // Exercise the production JS decoder, including empty-stack clearing and memo snapshots.
      // Jackson omits null graph/table fields in full JSON. The decoder materializes
      // those as null; normalize only ABSENT top-level fields, never [] or memo cells.
      const decoded = decodeTrace(delta.json);
      const fullSteps = decodeTrace(full.json).map((step, i) => Object.fromEntries(
        [...new Set([...Object.keys(step), ...Object.keys(decoded[i] ?? {})])]
          .map(key => [key, step[key] === undefined ? null : step[key]])));
      assert(isDeepStrictEqual(fullSteps, decoded), `${approach}, n=${n}: full/delta differ`);
      assert(!full.json.truncated && full.bytes < 2_000_000, `${approach}, n=${n}: exceeded full-response budget`);
      assert(full.json.code === detail.javaCode && full.json.approachId === approach
        && full.json.dsType === detail.dsType && isDeepStrictEqual(full.json.complexity, detail.complexity),
      `${approach}, n=${n}: metadata/source do not belong to selected executable`);
      const last = full.json.steps.at(-1);
      assert(last.callStack.length === 0 && last.variables.answer, `${approach}: incomplete return`);
      cases.push({ approach, n, steps: full.json.stepCount, fullBytes: full.bytes, deltaBytes: delta.bytes,
        answer: last.variables.answer, calls: last.variables.calls ?? null, cacheHits: last.variables.cacheHits ?? null,
        computedStates: last.variables.computedStates ?? null, fullDeltaEqual: true });
    }
    const rejected = (await request(`/api/problems/climbing-stairs/execute?approach=${approach}`, 400, { n: maximum + 1 })).json;
    assert(rejected.fieldErrors?.n, `${approach}: max+1 not refused as field validation`);
  }
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
      const [response] = await Promise.all([
        page.waitForResponse(r => new URL(r.url()).pathname === '/api/problems/climbing-stairs/execute'),
        page.goto(`${base}/problem/climbing-stairs?view=code`)
      ]);
      const run = await response.json();
      const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
      if (await sourceButton.isVisible()) await sourceButton.click();
      const source = page.getByRole('region', { name: 'Java source', exact: true });
      await source.waitFor();
      assert((await source.textContent()).includes('public class Solution'), 'Canonical source not complete');
      assert(run.approachId === 'tabulation', 'Existing UI silently changed canonical algorithm');
      const seek = page.getByRole('slider', { name: 'Seek to step' });
      await seek.fill(String(run.stepCount - 1));
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      assert(await seek.inputValue() === String(run.stepCount - 1), 'View switch lost position');
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page overflow');
      assert(errors.length === 0, `Page errors: ${errors}`);
      rows.push({ width, theme, defaultApproach: run.approachId, passed: true });
      await page.close();
    }
    fs.writeFileSync(path.join(__dirname, 'd2-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, stats, cases, boundaryRefusals: 3,
      productionDecoderRoundTrips: cases.length, rows, scope: 'D2 backend pilot; D3 UI pending'
    }, null, 2) + '\n');
    console.log(`PASS ${cases.length} production-decoder round trips, 3 boundary refusals, ${rows.length} canonical browser rows`);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
