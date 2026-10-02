const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const [out, tag, scheme, w, h] = process.argv.slice(2);
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: +w, height: +h }, colorScheme: scheme });
  await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  const errs = []; p.on('pageerror', e => errs.push(e.message)); p.on('console', m => m.type() === 'error' && errs.push(m.text()));
  for (const id of ['num-provinces', 'rotting-oranges']) {
    await p.goto(`http://localhost:5180/problem/${id}?view=code`, { waitUntil: 'networkidle' });
    await p.waitForTimeout(1200);
    for (let i = 0; i < 3; i++) await p.keyboard.press('ArrowRight');
    await p.waitForTimeout(400);
    const info = await p.evaluate(() => {
      const src = document.querySelector('[aria-label="Java source"]');
      const counts = {};
      for (const c of ['tokKeyword', 'tokType', 'tokString', 'tokNumber', 'tokComment']) counts[c] = src ? src.querySelectorAll(`[class*="${c}"]`).length : -1;
      const kw = src?.querySelector('[class*="tokKeyword"]');
      return { counts, kwColor: kw && getComputedStyle(kw).color, overflow: document.documentElement.scrollWidth - innerWidth };
    });
    console.log(tag, id, JSON.stringify(info));
    const pane = await p.$('[aria-label="Java source"]');
    await (w < 600 && pane ? pane.screenshot({ path: `${out}/${tag}-${id}.png` }) : p.screenshot({ path: `${out}/${tag}-${id}.png` }));
  }
  console.log('errors', errs);
  await b.close();
})();
