// P5c guidance gate against the real backend: keyboard-only Help -> tour; every step's target
// mounted, visible and inside the viewport; focus and inertness after it ends; welcome copy.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node tour-journey.cjs [outDir]
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
    const p = await ctx.newPage();
    const errors = [];
    let executes = 0;
    p.on('pageerror', (e) => errors.push(e.message));
    p.on('request', (r) => { if (r.method() === 'POST' && r.url().includes('/execute')) executes++; });
    await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });

    // First visit: the welcome is up, the page behind it inert, its copy current.
    const welcome = p.getByRole('dialog', { name: /Welcome/ });
    const welcomeText = (await welcome.innerText()).replace(/\s+/g, ' ');
    check(`${tag}: welcome names the switcher and Code walkthrough`, /Switch problem/.test(welcomeText) && /Code walkthrough/.test(welcomeText) && !/list on the left/.test(welcomeText));
    check(`${tag}: page inert behind the welcome`, await p.evaluate(() => Boolean(document.getElementById('workspace-view').closest('[inert]'))));
    const tourButton = welcome.getByRole('button', { name: /tour/i });
    check(`${tag}: welcome offers the tour`, await tourButton.count() === 1);
    const executesBefore = executes;
    await tourButton.focus();
    await p.keyboard.press('Enter');

    const seen = [];
    for (let guard = 0; guard < 20; guard++) {
      const tip = p.locator('[data-testid="tour-guide"] [role="dialog"]');
      if (await tip.count() === 0) break;
      await p.waitForTimeout(150);
      const title = await tip.locator('h3').innerText();
      const progress = await tip.locator('span').first().innerText();
      const geometry = await p.evaluate(() => {
        const spot = document.querySelector('[data-testid="tour-guide"] > div:nth-child(5)').getBoundingClientRect();
        // The target is the data-tour element whose box the spotlight surrounds.
        const match = [...document.querySelectorAll('[data-tour]')].find((el) => {
          const r = el.getBoundingClientRect();
          return r.width > 0 && Math.abs(r.left - 8 - spot.left) < 2 && Math.abs(r.top - 8 - spot.top) < 2
            && Math.abs(r.width + 16 - spot.width) < 2 && Math.abs(r.height + 16 - spot.height) < 2;
        });
        const r = match?.getBoundingClientRect();
        return {
          name: match?.getAttribute('data-tour') ?? null,
          inView: Boolean(r) && r.top >= -1 && r.top < innerHeight && r.left >= -1 && r.right <= innerWidth + 1,
          shown: Boolean(match) && match.getClientRects().length > 0
        };
      });
      seen.push(`${geometry.name}`);
      check(`${tag}: ${progress} "${title}" spotlights a visible target in view`, geometry.name && geometry.shown && geometry.inView, geometry.name ?? 'no target');
      if (seen.length === 2) await p.screenshot({ path: `${out}/p5c-tour-${tag}-${scheme}.png` });
      await p.keyboard.press('ArrowRight');
    }
    check(`${tag}: tour ended`, await p.locator('[data-testid="tour-guide"]').count() === 0, seen.join(' → '));
    const focus = await p.evaluate(() => ({ tag: document.activeElement?.tagName, id: document.activeElement?.id, shown: document.activeElement?.getClientRects().length > 0 }));
    check(`${tag}: focus lands on a visible element afterwards`, focus.tag !== 'BODY' && focus.shown, `${focus.tag}#${focus.id}`);
    check(`${tag}: nothing left inert, nothing executed`, await p.evaluate(() => !document.querySelector('[inert]')) && executes === executesBefore);

    // Keyboard-only replay from Help: `?` opens it, Tab reaches the tour, Escape ends the tour.
    await p.keyboard.press('Shift+Slash');
    const help = p.getByRole('dialog', { name: 'Keyboard shortcuts' });
    check(`${tag}: ? opens help`, await help.count() === 1);
    let reached = false;
    for (let i = 0; i < 8 && !reached; i++) {
      await p.keyboard.press('Tab');
      reached = await p.evaluate(() => /tour/i.test(document.activeElement?.textContent || ''));
    }
    check(`${tag}: Tab reaches "Take the guided tour" inside help`, reached);
    await p.keyboard.press('Enter');
    check(`${tag}: tour opens from help`, await p.locator('[data-testid="tour-guide"]').count() === 1);
    await p.keyboard.press('Escape');
    check(`${tag}: Escape ends the tour without leaving the page inert`, await p.locator('[data-testid="tour-guide"]').count() === 0 && await p.evaluate(() => !document.querySelector('[inert]')));
    check(`${tag}: no page errors`, errors.length === 0, errors.join('; '));
    await ctx.close();
  }
  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
