const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const B = 'http://localhost:5180';
const enc = (v) => Buffer.from(JSON.stringify(v)).toString('base64url');
(async () => {
  const b = await chromium.launch(); const ctx = await b.newContext({ viewport: { width: 1366, height: 768 } });
  await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
  const p = await ctx.newPage(); const t0 = Date.now(); const log = (m) => console.log(`${Date.now() - t0}ms ${m}`);
  let release; let held = false;
  p.on('request', (r) => { if (r.url().includes('/execute')) log(`REQ ${r.method()} ${r.postData() || ''}`); });
  p.on('requestfailed', (r) => { if (r.url().includes('/execute')) log(`FAILED ${r.method()} ${r.failure()?.errorText}`); });
  p.on('requestfinished', (r) => { if (r.url().includes('/execute')) log(`DONE ${r.method()}`); });
  await p.route('**/api/problems/two-sum/execute', async (route) => {
    if (route.request().method() !== 'POST') return route.continue();
    held = true; log('holding POST');
    await new Promise((r) => { release = r; });
    log('releasing POST'); return route.continue().catch((e) => log('continue failed ' + e.message));
  });
  await p.goto(`${B}/problem/two-sum?input=${enc({ nums: [3, 4], target: 7 })}`, { waitUntil: 'domcontentloaded' });
  await p.waitForFunction(() => document.querySelector('p[aria-live="polite"]'));
  for (let i = 0; i < 30 && !held; i++) await p.waitForTimeout(100);
  log('nav to defaults');
  await p.evaluate(() => { history.pushState({}, '', '/problem/two-sum'); dispatchEvent(new PopStateEvent('popstate')); });
  await p.waitForTimeout(800);
  log('screen before release: ' + (await p.locator('p[aria-live="polite"]').innerText()).slice(0, 90));
  release?.(); await p.waitForTimeout(1500);
  log('screen after release: ' + (await p.locator('p[aria-live="polite"]').innerText()).slice(0, 90) + ' | url ' + p.url().replace(B, ''));
  await b.close();
})();
