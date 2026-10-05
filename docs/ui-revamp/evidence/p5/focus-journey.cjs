// P5b Focus-mode gate against the real backend (Vite 5180 -> Java 8923):
// custom run + dirty draft -> Focus -> resize / theme / dialog -> exit -> inspect draft, run, URL.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node focus-journey.cjs [outDir]
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
    let executes = 0;
    const errors = [];
    p.on('request', (r) => { if (r.method() === 'POST' && r.url().includes('/execute')) executes++; });
    p.on('pageerror', (e) => errors.push(e.message));
    const narration = () => p.locator('p[aria-live="polite"]').first().innerText();

    await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });
    // A custom run, so the run is not the defaults.
    await p.getByLabel('Target sum').fill('13');
    await p.getByRole('button', { name: /^Run/ }).first().click();
    await p.waitForURL(/input=/);
    await p.waitForTimeout(400);
    await p.keyboard.press('ArrowRight');
    await p.keyboard.press('ArrowRight');
    const step = await narration();
    const url = p.url();
    // A dirty draft on top of it.
    await p.getByLabel('Target sum').fill('21');
    const executesBefore = executes;

    const stageTopBefore = await p.locator('[data-audit="stage"]').evaluate((e) => Math.round(e.getBoundingClientRect().top + scrollY));
    await p.getByRole('button', { name: 'Focus', exact: true }).click();
    check(`${tag}: Exit focus has focus`, await p.evaluate(() => document.activeElement?.textContent?.trim() === 'Exit focus'));
    const stage = await p.locator('[data-audit="stage"]').evaluate((e) => { const r = e.getBoundingClientRect(); return { top: Math.round(r.top + scrollY), height: Math.round(r.height), width: Math.round(r.width) }; });
    check(`${tag}: stage starts higher in Focus`, stage.top < stageTopBefore, `y${stageTopBefore} → y${stage.top}, ${stage.width}×${stage.height}`);
    const cardBottom = await p.locator('#workspace-view section').first().evaluate((e) => Math.round(e.getBoundingClientRect().bottom + scrollY));
    check(`${tag}: the Focus card fits the viewport (controls reachable without scrolling)`, cardBottom <= height, `card bottom y${cardBottom} of ${height}`);
    check(`${tag}: editor and tabs hidden`, !(await p.getByLabel('Target sum').isVisible()) && !(await p.getByRole('tab', { name: 'Code walkthrough' }).isVisible()));
    check(`${tag}: narration, playback and Exit focus visible`, await p.getByRole('button', { name: 'Exit focus' }).isVisible()
      && await p.getByRole('button', { name: /^(Play|Pause)$/ }).isVisible() && (await narration()) === step);
    await p.screenshot({ path: `${out}/p5b-focus-${tag}-${scheme}.png` });

    // Resize, theme, a dialog - none of them may run anything or leave Focus.
    await p.setViewportSize({ width: width > 600 ? 900 : 360, height });
    await p.emulateMedia({ colorScheme: scheme === 'dark' ? 'light' : 'dark' });
    await p.keyboard.press('Control+k');
    await p.keyboard.press('Escape');
    await p.keyboard.press('Shift+Slash');
    const helpOpen = await p.getByRole('dialog', { name: 'Keyboard shortcuts' }).count();
    await p.keyboard.press('Escape');
    check(`${tag}: help opened over Focus and its Escape kept Focus`, helpOpen === 1 && await p.getByRole('button', { name: 'Exit focus' }).isVisible());
    await p.keyboard.press('Escape');
    check(`${tag}: second Escape leaves Focus, focus back on Focus`, await p.evaluate(() => document.activeElement?.textContent?.trim() === 'Focus'));
    await p.setViewportSize({ width, height });

    check(`${tag}: draft kept`, (await p.getByLabel('Target sum').inputValue()) === '21' && await p.getByText('Changes not run').isVisible());
    check(`${tag}: same run, step and URL; no execution`, (await narration()) === step && p.url() === url && executes === executesBefore, `${executes - executesBefore} extra`);
    check(`${tag}: no horizontal overflow`, await p.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
    check(`${tag}: no page errors`, errors.length === 0, errors.join('; '));
    await ctx.close();
  }
  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
