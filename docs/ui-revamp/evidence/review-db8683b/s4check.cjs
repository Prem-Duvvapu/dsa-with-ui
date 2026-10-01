const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const B = 'http://localhost:5180';
(async () => {
  const b = await chromium.launch();
  for (const [w, h, id] of [[390, 844, 'bfs-traversal'], [1366, 600, 'longest-common-subsequence']]) {
    const ctx = await b.newContext({ viewport: { width: w, height: h } });
    await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
    const p = await ctx.newPage();
    await p.goto(`${B}/problem/${id}?view=code`, { waitUntil: 'networkidle' }); await p.waitForTimeout(400);
    if (await p.getByRole('button', { name: 'Source', exact: true }).count()) await p.getByRole('button', { name: 'Source', exact: true }).click();
    const src = p.getByRole('region', { name: 'Java source' });
    const dims = await src.evaluate((n) => ({ sw: n.scrollWidth, cw: n.clientWidth, sh: n.scrollHeight, ch: n.clientHeight }));
    await src.hover(); await p.mouse.wheel(60, 80); await p.waitForTimeout(200);
    const set = await src.evaluate((n) => [n.scrollLeft, n.scrollTop]);
    await p.getByRole('tab', { name: 'Analysis' }).click(); await p.getByRole('tab', { name: 'Code walkthrough' }).click();
    if (await p.getByRole('button', { name: 'Source', exact: true }).count()) await p.getByRole('button', { name: 'Source', exact: true }).click();
    const after = await p.getByRole('region', { name: 'Java source' }).evaluate((n) => [n.scrollLeft, n.scrollTop]);
    console.log(`${w}px ${id}: dims ${JSON.stringify(dims)} | scrolled ${set} | after views ${after}`);
    await ctx.close();
  }
  await b.close();
})();
