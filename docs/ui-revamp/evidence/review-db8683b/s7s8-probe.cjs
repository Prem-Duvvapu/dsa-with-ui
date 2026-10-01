const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const B = 'http://localhost:5180';
(async () => {
  const b = await chromium.launch(); const errors = [];
  for (const theme of ['light', 'dark']) {
    const ctx = await b.newContext({ viewport: { width: 1366, height: 768 }, colorScheme: theme });
    await ctx.addInitScript((t) => { localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(t)); }, theme);
    const p = await ctx.newPage(); p.on('pageerror', (e) => errors.push(String(e)));
    // S7a: 500 on execute, open in Analysis and Code
    await p.route('**/api/problems/two-sum/execute', (r) => r.request().method() === 'GET' ? r.fulfill({ status: 500, body: '' }) : r.continue());
    for (const view of ['analysis', 'code']) {
      await p.goto(`${B}/problem/two-sum?view=${view}`, { waitUntil: 'networkidle' });
      console.log(`${theme} S7 ${view}: alerts=`, await p.getByRole('alert').allInnerTexts());
    }
    await p.screenshot({ path: `s7-${theme}.jpg`, type: 'jpeg', quality: 70 });
    await p.unroute('**/api/problems/two-sum/execute');
    // S7b: rejected run while in Analysis
    let release;
    await p.route('**/api/problems/two-sum/execute', async (r) => { if (r.request().method() !== 'POST') return r.continue(); await new Promise((res) => { release = res; }); return r.continue(); });
    await p.goto(`${B}/problem/two-sum`, { waitUntil: 'networkidle' });
    await p.getByLabel('Target sum').fill('99999');
    await p.getByRole('button', { name: 'Run input' }).click();
    await p.getByRole('tab', { name: 'Analysis' }).click();
    release(); await p.waitForTimeout(800);
    const notice = p.getByRole('alert', { name: 'Your input could not run' });
    console.log(`${theme} S7 rejected-in-Analysis notice:`, await notice.count());
    await notice.getByRole('button', { name: 'Fix it in the editor' }).click(); await p.waitForTimeout(500);
    console.log(`${theme}   -> tab`, await p.getByRole('tab', { selected: true }).innerText(), '| focus', await p.evaluate(() => document.activeElement.textContent.slice(0, 30)), '| summary', (await p.getByRole('alert').first().innerText()).replace(/\s+/g, ' ').slice(0, 60));
    await p.unroute('**/api/problems/two-sum/execute');
    // S8
    await p.goto(`${B}/problem/does-not-exist`, { waitUntil: 'networkidle' });
    await p.getByRole('button', { name: 'Help' }).first().click();
    console.log(`${theme} S8 help dialog:`, await p.getByRole('dialog', { name: 'Keyboard shortcuts' }).count());
    await p.keyboard.press('Escape');
    await p.getByRole('button', { name: /Switch problem/ }).first().click();
    await p.getByRole('textbox', { name: /Jump to a problem/ }).fill('Two Sum'); await p.keyboard.press('Enter'); await p.waitForTimeout(800);
    console.log(`${theme} S8 switch ->`, p.url().replace(B, ''));
    await ctx.close();
  }
  console.log('page errors', errors.length, errors.slice(0, 2));
  await b.close();
})();
