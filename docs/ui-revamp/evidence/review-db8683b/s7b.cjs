const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const B = 'http://localhost:5180';
(async () => {
  const b = await chromium.launch(); const ctx = await b.newContext({ viewport: { width: 390, height: 844 } });
  await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
  const p = await ctx.newPage();
  await p.route('**/api/problems/bfs-traversal/execute', (r) => r.fulfill({ status: 500, body: '' }));
  for (const view of ['playground', 'code', 'analysis']) {
    await p.goto(`${B}/problem/bfs-traversal?view=${view}`, { waitUntil: 'networkidle' });
    console.log(`S7 ${view} (390px):`, JSON.stringify(await p.getByRole('alert').allInnerTexts()));
  }
  await p.screenshot({ path: 's7-analysis-390.jpg', type: 'jpeg', quality: 70 });
  await b.close();
})();
