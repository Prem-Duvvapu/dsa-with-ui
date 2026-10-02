const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const [out, tag, scheme, w, h, welcome] = process.argv.slice(2);
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: +w, height: +h }, colorScheme: scheme });
  if (welcome !== 'welcome') await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  const errs = []; p.on('pageerror', e => errs.push(e.message)); p.on('console', m => m.type() === 'error' && errs.push(m.text()));
  for (const [name, url] of [['library', '/'], ['graph', '/problem/num-provinces'], ['dp', '/problem/longest-common-subsequence'], ['code', '/problem/num-provinces?view=code']]) {
    await p.goto('http://localhost:5180' + url, { waitUntil: 'networkidle' });
    await p.waitForTimeout(1000);
    if (name === 'graph') { for (let i = 0; i < 4; i++) await p.keyboard.press('ArrowRight'); await p.waitForTimeout(500); }
    const overflow = await p.evaluate(() => document.documentElement.scrollWidth - window.innerWidth);
    const bg = await p.evaluate(() => getComputedStyle(document.body).backgroundColor + ' / scheme ' + getComputedStyle(document.documentElement).colorScheme);
    console.log(tag, name, 'overflow', overflow, bg);
    await p.screenshot({ path: `${out}/${tag}-${name}.png` });
  }
  console.log('errors', errs);
  await b.close();
})();
