// P6a history gate against the real backend: n-queens n=7 (3,090 steps, truncated).
// Bounded DOM, paging without seeking, final-step reachability, seek sync, view re-entry, rerun reset.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node history-journey.cjs [outDir]
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const out = process.argv[2] || __dirname;
const BASE = 'http://localhost:5180';
const results = [];
const check = (name, ok, detail = '') => results.push({ name, ok: Boolean(ok), detail });

(async () => {
  const browser = await chromium.launch();
  for (const [width, height, scheme] of [[1366, 768, 'dark'], [390, 844, 'light']]) {
    const tag = `${width}`;
    const ctx = await browser.newContext({ viewport: { width, height }, colorScheme: scheme });
    await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
    const p = await ctx.newPage();
    const errors = [];
    let executes = 0;
    p.on('pageerror', (e) => errors.push(e.message));
    p.on('request', (r) => { if (r.method() === 'POST' && r.url().includes('/execute')) executes++; });
    const narration = () => p.locator('p[aria-live="polite"]').first().innerText();
    const history = p.locator('details', { has: p.getByText('Execution history', { exact: false }) }).last();

    await p.goto(`${BASE}/problem/n-queens`, { waitUntil: 'networkidle' });
    await p.getByLabel('Board size (N)').fill('7');
    await p.getByRole('button', { name: /^Run/ }).first().click();
    await p.waitForURL(/input=/);
    await p.waitForTimeout(500);
    const total = Number((await history.locator('summary').innerText()).match(/of (\d+)/)[1]);
    check(`${tag}: a near-limit run is loaded`, total > 3000, `${total} steps`);
    const before = executes;

    await history.locator('summary').click();
    const list = history.getByRole('list');
    const count = await list.getByRole('button').count();
    check(`${tag}: one page of entries, not the whole run`, count === 50, `${count} entries`);
    check(`${tag}: range and total shown`, /Steps 1–50 of \d+/.test(await history.innerText()));

    // Page to the end without seeking.
    const stepBefore = await narration();
    const nextPage = history.getByRole('button', { name: 'Next page' });
    let pages = 0;
    const t0 = Date.now();
    while (await nextPage.isEnabled() && pages < 200) { await nextPage.click(); pages++; }
    const perPage = Math.round((Date.now() - t0) / Math.max(1, pages));
    check(`${tag}: paging never seeks or runs`, (await narration()) === stepBefore && executes === before, `${pages} pages, ~${perPage}ms per page incl. automation`);
    const last = list.getByRole('button').last();
    const lastText = await last.innerText();
    check(`${tag}: the final step is reachable`, lastText.startsWith(`Step ${total}`), lastText.split('\n')[0]);
    const t1 = Date.now();
    await last.click();
    await p.waitForFunction((n) => document.body.innerText.includes(`Step ${n} of ${n}`), total);
    check(`${tag}: choosing it seeks the session (narration, counter, URL)`, (await narration()).length > 0
      && /step=/.test(p.url()) && await last.getAttribute('aria-current') === 'step', `${Date.now() - t1}ms to counter update`);
    check(`${tag}: still only one page in the DOM`, await list.getByRole('button').count() <= 50);
    await p.screenshot({ path: `${out}/p6a-history-${tag}-${scheme}.png` });

    // Jump to current from another page.
    await history.getByRole('button', { name: 'Previous page' }).click();
    await history.getByRole('button', { name: 'Jump to current step' }).click();
    check(`${tag}: Jump to current returns to the current step's page`, await list.locator('[aria-current="step"]').count() === 1);

    // View re-entry keeps the run and step.
    await p.getByRole('tab', { name: 'Code walkthrough' }).click();
    await p.getByRole('tab', { name: 'Playground' }).click();
    check(`${tag}: view switch keeps step and runs nothing`, (await p.locator('body').innerText()).includes(`Step ${total} of ${total}`) && executes === before);

    // A new run resets the list to that run.
    await p.getByLabel('Board size (N)').fill('4');
    await p.getByRole('button', { name: /^Run/ }).first().click();
    await p.waitForFunction(() => /Steps 1–50 of \d+/.test(document.body.innerText) && !/of 3\d{3}/.test(document.body.innerText));
    check(`${tag}: a new run starts the list again`, /Steps 1–50 of/.test(await history.innerText()));
    check(`${tag}: no horizontal overflow, no page errors`, await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth) && errors.length === 0, errors.join('; '));
    await ctx.close();
  }
  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
