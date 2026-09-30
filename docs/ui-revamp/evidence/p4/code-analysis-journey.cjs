const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const pos = (p) => p.locator('p[aria-live="polite"]').innerText();
(async () => {
  const b = await chromium.launch();
  const errors = [];
  for (const [w, h] of [[1366, 768], [390, 844]]) {
    const ctx = await b.newContext({ viewport: { width: w, height: h } });
    await ctx.addInitScript(() => { localStorage.setItem('dsa-ui:seenWelcome', 'true');  });
    const p = await ctx.newPage(); p.on('pageerror', (e) => errors.push(String(e)));
    let execs = 0; p.on('request', (r) => { if (r.url().includes('/execute')) execs += 1; });
    await p.goto('http://localhost:5180/problem/bfs-traversal?step=5', { waitUntil: 'networkidle' });
    await p.getByRole('tab', { name: 'Code walkthrough' }).click(); await p.waitForTimeout(400);
    const sep = p.getByRole('separator');
    const split = await sep.count();
    console.log(`${w}: code split=${split} | step`, (await pos(p)).slice(0, 50));
    if (split) {
      const box = await sep.boundingBox();
      await p.mouse.move(box.x + box.width / 2, box.y + 60); await p.mouse.down(); await p.mouse.move(box.x - 200, box.y + 60, { steps: 5 }); await p.mouse.up();
      console.log('  after drag aria-valuenow', await sep.getAttribute('aria-valuenow'));
      await sep.focus(); await p.keyboard.press('ArrowRight');
      console.log('  after ArrowRight', await sep.getAttribute('aria-valuenow'), '| step', (await pos(p)).slice(0, 30));
      const panes = await p.evaluate(() => [...document.querySelectorAll('[role="separator"] ~ div, [role="separator"]')].map(() => 0));
      const widths = await p.evaluate(() => { const s = document.querySelector('[role="separator"]'); return [s.previousElementSibling.getBoundingClientRect().width | 0, s.nextElementSibling.getBoundingClientRect().width | 0]; });
      console.log('  pane widths diagram/source', widths);
    } else {
      await p.getByRole('button', { name: 'Diagram', exact: true }).click();
      console.log('  subview diagram shown', await p.locator('[data-audit="stage"]').count(), '| step', (await pos(p)).slice(0, 30));
    }
    await p.screenshot({ path: `p4a-code-${w}.jpg`, type: 'jpeg', quality: 70, fullPage: true });
    await p.getByRole('tab', { name: 'Analysis' }).click(); await p.waitForTimeout(300);
    console.log('  analysis regions:', await p.locator('section[aria-label]').evaluateAll((els) => els.map((e) => e.getAttribute('aria-label')).join(', ')));
    await p.screenshot({ path: `p4a-analysis-${w}.jpg`, type: 'jpeg', quality: 70, fullPage: true });
    const overflow = await p.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1);
    console.log('  overflow', overflow, '| executes', execs);
    await p.goto('http://localhost:5180/problem/fibonacci-recursion?view=analysis&step=6', { waitUntil: 'networkidle' }); await p.waitForTimeout(500);
    console.log('  fib analysis stack:', (await p.getByRole('region', { name: 'Call stack' }).innerText()).replace(/\s+/g, ' ').slice(0, 120));
    await ctx.close();
  }
  console.log('errors', errors);
  await b.close();
})();
