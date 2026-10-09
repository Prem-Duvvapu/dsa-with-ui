// Real pilot input boundaries, independent answer references, and retained-run failure.
const fs = require('fs');
const path = require('path');
const browsers = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const engine = process.env.AUDIT_ENGINE || 'chromium';
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 1);
if (!['chromium', 'firefox'].includes(engine) || ![1, 2].includes(zoom)
    || zoom === 2 && engine !== 'chromium') throw Error('Unsupported boundary audit browser/zoom');
const pace = require('../evidence/pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
function assert(value, message) { if (!value) throw Error(message); }
function ways(n) { let a = 1, b = 1; for (let k = 2; k <= n; k++) [a, b] = [b, a + b]; return b; }
async function post(page, action) {
  const [response] = await Promise.all([
    page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute')),
    action()
  ]);
  return { status: response.status(), body: await response.json() };
}
async function compare(page, panel) {
  const pending = [];
  const listen = response => {
    if (response.request().method() === 'POST' && new URL(response.url()).pathname.endsWith('/execute'))
      pending.push(response.json());
  };
  page.on('response', listen);
  try {
    await panel.getByRole('button', { name: 'Run comparison', exact: true }).click();
    await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
    const results = await Promise.all(pending);
    assert(results.length === 2, 'Comparison did not issue precisely two requests');
    return results;
  } finally { page.off('response', listen); }
}
(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const { decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js'));
  const browser = zoom === 2 ? await require('../evidence/native-browser-zoom.cjs')(browsers.chromium)
    : await browsers[engine].launch();
  const rows = [];
  try {
    const widths = (process.env.AUDIT_WIDTHS || (zoom === 2 ? '640,1366' : '320,1366')).split(',').map(Number);
    const themes = (process.env.AUDIT_THEMES || 'light,dark').split(',');
    for (const width of widths) for (const theme of themes) {
      const page = await browser.newPage({ viewport: { width, height: 768 }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.goto(`${base}/problem/climbing-stairs`);
      await page.getByRole('combobox', { name: 'Solution approach' }).waitFor();
      await page.waitForFunction(() => !document.querySelector('select')?.disabled);
      if (zoom === 2) await browser.setZoom(page, zoom);
      for (const n of [1, 10, 30]) {
        await page.getByRole('combobox', { name: 'Solution approach' }).selectOption('memoization');
        await page.getByLabel('Stairs', { exact: true }).fill(String(n));
        const main = await post(page, () => page.getByRole('button', { name: 'Run input', exact: true }).click());
        assert(main.status === 200 && main.body.approachId === 'memoization' && main.body.resolvedInput.n === n
          && !main.body.truncated, 'Boundary main run was refused, substituted or cut short');
        await page.waitForFunction(n => {
          const value = new URL(location.href).searchParams.get('input');
          return value && JSON.parse(atob(value.replace(/-/g, '+').replace(/_/g, '/'))).n === n;
        }, n);
        const seek = page.getByRole('slider', { name: 'Seek to step', exact: true });
        await seek.fill('1');
        await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '2');
        const link = page.url();
        if (!await page.getByRole('region', { name: 'Approach comparison', exact: true }).isVisible())
          await page.getByRole('button', { name: 'Compare approaches', exact: true }).click();
        const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
        await panel.getByLabel('Approach 2', { exact: true }).selectOption(n === 30 ? 'tabulation' : 'recursion');
        const results = await compare(page, panel);
        const recorded = results.map(body => {
          const steps = decodeTrace(body);
          const stats = steps.at(-1).variables;
          assert(body.problemId === 'climbing-stairs' && body.resolvedInput.n === n && !body.truncated,
            'Comparison changed its common input or was cut short');
          assert(stats.answer === String(ways(n)), 'Answer differs from independent iterative reference');
          if (body.approachId === 'memoization') {
            assert(stats.calls === String(2 * n - 1) && stats.cacheHits === String(Math.max(0, n - 2))
              && stats.computedStates === String(n === 1 ? 1 : n + 1), 'Memo boundary counters are wrong');
          } else if (body.approachId === 'recursion') {
            assert(stats.calls === String(2 * ways(n) - 1) && stats.cacheHits === '0', 'Recursive boundary counters are wrong');
          } else assert(body.approachId === 'tabulation' && stats.calls == null, 'Invented tabulation calls');
          const side = panel.getByRole('region', { name: `${body.approachLabel} comparison`, exact: true });
          return { approach: body.approachId, events: steps.length, answer: stats.answer,
            calls: stats.calls ?? null, cacheHits: stats.cacheHits ?? null, computedStates: stats.computedStates ?? null,
            slider: side.getByRole('slider') };
        });
        // Every comparison seek is local, including the last event of the largest trace.
        for (const record of recorded) await record.slider.fill(String(record.events));
        assert(await seek.inputValue() === '1' && page.url() === link, 'Boundary comparison changed the main step/link');
        if (n === 1) {
          const memo = panel.getByRole('region', { name: 'Memoization comparison', exact: true });
          assert(await memo.locator('dt').filter({ hasText: /^Cache hits$/ }).evaluate(e => e.nextElementSibling?.textContent) === '0',
            'Actual zero cache hits treated as absent');
        }
        let rejectedDraftRetained = null;
        if (n === 30) {
          await page.getByLabel('Stairs', { exact: true }).fill('31');
          const rejected = await post(page, () => page.getByRole('button', { name: 'Run input', exact: true }).click());
          assert(rejected.status === 400, 'Out-of-range draft was not rejected');
          const error = page.getByRole('alert', { name: 'This input could not run', exact: true });
          await error.waitFor();
          assert(await error.evaluate(e => document.activeElement === e), '400 did not focus input error summary');
          assert(page.url() === link && await seek.inputValue() === '1', 'Rejected draft changed the main shared run');
          const repeated = await compare(page, panel);
          assert(repeated.every(body => body.resolvedInput.n === 30) && await page.getByLabel('Stairs', { exact: true }).inputValue() === '31'
            && page.url() === link && await error.isVisible(), 'Comparison adopted or erased the rejected draft');
          rejectedDraftRetained = true;
        }
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Boundary page overflow');
        assert(errors.length === 0, `Boundary browser errors: ${errors}`);
        rows.push({ width, height: 768, theme, nativeZoom: zoom, n, realBackend: true,
          comparisons: recorded.map(({ slider, ...record }) => record), independentReferenceAnswers: true,
          mainStepAndLinkRetained: true, rejectedDraftRetained, noOverflowOrErrors: true });
        console.log(`PASS boundary ${n} ${width} ${theme} ${engine} ${zoom * 100}%`);
      }
      if (browser.closePage) await browser.closePage(page);
      else await page.close();
    }
    fs.writeFileSync(path.join(__dirname, `d4-boundaries-${engine}-${zoom * 100}-results.json`), JSON.stringify({
      generated: new Date().toISOString(), engine, buildMode: 'production-preview', servedBuild, rows,
      scope: 'D4 Climbing Stairs pilot only: smallest input, recursive ceiling, memo/tabulation ceiling, and a genuine 400 retained-run journey'
    }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
