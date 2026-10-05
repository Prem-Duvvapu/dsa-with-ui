// P5a switcher journey against the real backend (Vite 5180 -> Java 8923).
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node switcher-journey.cjs [outDir]
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const out = process.argv[2] || __dirname;
const BASE = 'http://localhost:5180';
const results = [];
const check = (name, ok, detail = '') => { results.push({ name, ok: Boolean(ok), detail }); };

async function page(browser, { width, height, scheme }) {
  const ctx = await browser.newContext({ viewport: { width, height }, colorScheme: scheme });
  await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  p.executes = 0;
  p.errors = [];
  p.on('request', (r) => { if (r.method() === 'POST' && r.url().includes('/execute')) p.executes++; });
  p.on('pageerror', (e) => p.errors.push(e.message));
  return { ctx, p };
}
const narration = (p) => p.locator('p[aria-live="polite"]').first().innerText();
const dialogOpen = (p) => p.locator('[role="dialog"][aria-label="Command palette"]').count();
const inDialog = (p) => p.locator('[role="dialog"][aria-label="Command palette"]');

(async () => {
  const browser = await chromium.launch();

  // ── Desktop, dark ───────────────────────────────────────────────────────
  {
    const { ctx, p } = await page(browser, { width: 1366, height: 768, scheme: 'dark' });
    await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });
    await p.keyboard.press('ArrowRight');
    const step = await narration(p);
    const executesAtStart = p.executes;

    const trigger = p.getByRole('button', { name: /Switch problem/ }).first();
    await trigger.focus();
    await p.keyboard.press('Enter');
    check('desktop: Enter on Switch problem opens the dialog', await dialogOpen(p) === 1);
    check('desktop: query has focus', await p.evaluate(() => document.activeElement?.getAttribute('role') === 'combobox'));
    check('desktop: page behind is inert', await p.evaluate(() => Boolean(document.getElementById('workspace-view').closest('[inert]'))));
    for (let i = 0; i < 6; i++) await p.keyboard.press('Tab');
    check('desktop: Tab stays inside the dialog', await p.evaluate(() => Boolean(document.activeElement.closest('[role="dialog"]'))));
    await inDialog(p).getByRole('combobox').focus();
    await p.keyboard.type('binary');
    const options = await inDialog(p).getByRole('option').count();
    for (let i = 0; i < options - 1; i++) await p.keyboard.press('ArrowDown');
    const visible = await p.evaluate(() => {
      const list = document.querySelector('[role="listbox"]').getBoundingClientRect();
      const active = document.querySelector('[role="option"][aria-selected="true"]').getBoundingClientRect();
      return active.top >= list.top - 1 && active.bottom <= list.bottom + 1;
    });
    check('desktop: active option scrolled into view after ArrowDown to the end', visible, `${options} options`);
    await p.screenshot({ path: `${out}/p5a-switcher-1366-dark.png` });
    await p.keyboard.press('Escape');
    check('desktop: Escape closes', await dialogOpen(p) === 0);
    check('desktop: focus returns to Switch problem', await p.evaluate(() => /Switch problem/.test(document.activeElement?.textContent || '')));
    check('desktop: nothing left inert', await p.evaluate(() => !document.querySelector('[inert]')));
    check('desktop: run and step unchanged after cancel', (await narration(p)) === step && p.executes === executesAtStart);

    await p.keyboard.press('Control+k');
    await p.keyboard.type('two sum');
    const firstOption = await inDialog(p).getByRole('option').first().innerText();
    await p.keyboard.press('Enter');
    check('desktop: current problem marked, choosing it closes without a rerun',
      /Current/.test(firstOption) && await dialogOpen(p) === 0 && (await narration(p)) === step && p.executes === executesAtStart, firstOption.replace(/\s+/g, ' '));

    await p.keyboard.press('Control+k');
    await p.keyboard.type('sum');
    await inDialog(p).getByRole('option', { name: /in the library/ }).click();
    await p.waitForURL(/\/\?q=sum/);
    const libraryQuery = await p.locator('#library-search').inputValue();
    check('desktop: View all lands in the library with the query', libraryQuery === 'sum', p.url());
    await p.keyboard.press('Control+k');
    check('library: Ctrl+K focuses the library query, no dialog', await dialogOpen(p) === 0
      && await p.evaluate(() => document.activeElement?.id === 'library-search'));
    check('desktop: no page errors', p.errors.length === 0, p.errors.join('; '));
    await ctx.close();
  }

  // ── Phone, light ────────────────────────────────────────────────────────
  {
    const { ctx, p } = await page(browser, { width: 390, height: 844, scheme: 'light' });
    await p.goto(`${BASE}/problem/number-of-provinces`, { waitUntil: 'networkidle' });
    await p.getByText('Menu', { exact: true }).click();
    await p.locator('details[open]').getByRole('button', { name: /Switch problem/ }).click();
    check('phone: Menu > Switch problem opens the dialog', await dialogOpen(p) === 1);
    await p.keyboard.type('island');
    const overflow = await p.evaluate(() => document.documentElement.scrollWidth - innerWidth);
    check('phone: no horizontal overflow with the dialog open', overflow <= 0, `${overflow}px`);
    const minHeight = await p.evaluate(() => Math.min(...[...document.querySelectorAll('[role="option"]')].map((o) => o.getBoundingClientRect().height)));
    check('phone: options are at least 44px tall', minHeight >= 44, `${minHeight}px`);
    await p.screenshot({ path: `${out}/p5a-switcher-390-light.png` });
    await p.keyboard.press('Escape');
    const focus = await p.evaluate(() => ({ text: document.activeElement?.textContent?.trim(), shown: document.activeElement?.getClientRects().length > 0 }));
    check('phone: focus returns to a visible control', focus.shown, focus.text);
    check('phone: no page errors', p.errors.length === 0, p.errors.join('; '));
    await ctx.close();
  }

  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
