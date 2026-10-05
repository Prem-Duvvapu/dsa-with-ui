// Re-check of the renderer sweep's phone failures: the "End of the run" row (P6b) pushed the page
// sideways at 390px when "Next: <title>" was long. Final step of each failing problem, both inputs.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const out = process.argv[2] || __dirname;
const API = 'http://localhost:8923';
const b64url = (o) => Buffer.from(JSON.stringify(o)).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
(async () => {
  const ids = ['longest-palindromic-subsequence', 'divide-two-numbers-bitwise', 'number-substrings-all-three-chars', 'children-sum-property'];
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'light' });
  await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  let failures = 0;
  for (const id of ids) {
    const detail = await (await fetch(`${API}/api/problems/${id}`)).json();
    for (const [variant, q] of [['default', ''], ['alternate', detail.alternateInput ? `?input=${b64url(detail.alternateInput)}` : null]]) {
      if (q === null) continue;
      await p.goto(`http://localhost:5180/problem/${id}${q}`, { waitUntil: 'networkidle', timeout: 60000 });
      await p.waitForFunction(() => /Step 1 of \d+/.test(document.body.innerText), null, { timeout: 30000 });
      await p.keyboard.press('End');
      await p.waitForTimeout(200);
      const r = await p.evaluate(() => ({ overflow: document.documentElement.scrollWidth - innerWidth, row: Boolean(document.querySelector('[aria-label="End of the run"]')) }));
      const ok = r.overflow <= 0;
      if (!ok) failures++;
      console.log(`${ok ? 'PASS' : 'FAIL'}  390 light ${id} ${variant} final: overflow ${r.overflow}px, end-of-run row ${r.row ? 'shown' : 'absent'}`);
      if (id === 'number-substrings-all-three-chars' && variant === 'alternate') {
        await p.locator('[aria-label="End of the run"]').scrollIntoViewIfNeeded();
        await p.screenshot({ path: `${out}/p7-completion-wrap-390-light.png` });
      }
    }
  }
  await browser.close();
  process.exitCode = failures ? 1 : 0;
})();
