// P6b gate against the real backend: comparison on demand for an Array and a Graph problem,
// labelled independent sides, custom-run scope, end-of-run actions only for complete runs.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node compare-completion-journey.cjs [outDir]
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const out = process.argv[2] || __dirname;
const BASE = 'http://localhost:5180';
const results = [];
const check = (name, ok, detail = '') => results.push({ name, ok: Boolean(ok), detail });

(async () => {
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 1366, height: 768 }, colorScheme: 'dark' });
  await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  const errors = [];
  let executes = 0;
  p.on('pageerror', (e) => errors.push(e.message));
  // Completed requests only: the dev build's StrictMode mounts effects twice, and the first
  // comparison request is aborted (useComparisonTrace) - an attempt is not an execution.
  p.on('requestfinished', (r) => { if (r.method() === 'POST' && r.url().includes('/execute')) executes++; });
  const narration = () => p.locator('p[aria-live="polite"]').first().innerText();
  const end = () => p.getByRole('group', { name: 'End of the run' });

  // ── Array: comparison on demand, independent sides, main run untouched ──
  await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });
  const afterLoad = executes;
  await p.waitForTimeout(500);
  check('no comparison fetch until it is opened', executes === afterLoad, `${executes} executes`);
  await p.keyboard.press('ArrowRight');
  const mainStep = await narration();
  await p.getByRole('button', { name: 'Compare other case' }).click();
  const other = p.getByRole('region', { name: 'Other case', exact: true });
  await other.waitFor();
  check('opening fetches exactly the two comparison runs', executes === afterLoad + 2, `${executes - afterLoad} executes`);
  const otherText = (await other.innerText()).replace(/\s+/g, ' ');
  const defaultText = (await p.getByRole('region', { name: 'Default input', exact: true }).innerText()).replace(/\s+/g, ' ');
  check('each side names its own input', /target/.test(otherText) && /target/.test(defaultText) && otherText !== defaultText);
  await other.getByRole('button', { name: 'Other case: next step' }).click();
  check('sides move independently, and never move the main run',
    /Step 2 of/.test(await other.innerText()) && /Step 1 of/.test(await p.getByRole('region', { name: 'Default input', exact: true }).innerText()) && (await narration()) === mainStep);
  check('scope and ordinal caveat stated', /not matching moments of the algorithm/.test(await p.locator('body').innerText()));
  await p.screenshot({ path: `${out}/p6b-compare-array-1366-dark.png`, fullPage: false });

  // ── Graph: compared as text, no capture strip ──
  await p.goto(`${BASE}/problem/number-of-provinces`, { waitUntil: 'networkidle' });
  const compareGraph = p.getByRole('button', { name: 'Compare other case' });
  check('a Graph problem offers comparison', await compareGraph.count() === 1);
  await compareGraph.click();
  const graphOther = p.getByRole('region', { name: 'Other case', exact: true });
  await graphOther.waitFor();
  check('graph sides read as text with narration, no capture strip',
    /Step 1 of \d+/.test(await graphOther.innerText()) && await p.getByLabel(/Execution capture/).count() === 0);
  await graphOther.scrollIntoViewIfNeeded();
  await p.screenshot({ path: `${out}/p6b-compare-graph-1366-dark.png` });

  // ── Completion: genuine end vs truncated ──
  await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });
  check('no end-of-run actions mid-run', await end().count() === 0);
  await p.keyboard.press('End');
  check('a complete run at its last step offers next moves', await end().count() === 1, (await end().innerText()).replace(/\s+/g, ' '));
  const beforeOther = executes;
  await end().getByRole('button', { name: 'Run the other case' }).click();
  await p.waitForURL(/input=/);
  await p.waitForTimeout(400);
  check('Run the other case runs it once and starts at step 1', executes === beforeOther + 1 && /Step 1 of/.test(await p.locator('body').innerText()));

  // Custom main run → comparison says it is not part of it.
  await p.getByRole('button', { name: 'Compare other case' }).click();
  await p.getByRole('region', { name: 'Other case', exact: true }).waitFor();
  check('a submitted main run is called out as outside the comparison', /The run above uses your own input/.test(await p.locator('body').innerText()));

  await p.goto(`${BASE}/problem/n-queens`, { waitUntil: 'networkidle' });
  await p.getByLabel('Board size (N)').fill('7');
  await p.getByRole('button', { name: /^Run/ }).first().click();
  await p.waitForURL(/input=/);
  await p.waitForTimeout(500);
  await p.keyboard.press('End');
  check('a truncated run at its last visible step is not called finished',
    await end().count() === 0 && /cut short/.test(await p.locator('body').innerText()));
  check('no page errors', errors.length === 0, errors.join('; '));

  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
