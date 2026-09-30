const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1366, height: 768 } });
  await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
  const p = await ctx.newPage();
  const posts = [];
  p.on('request', (r) => { if (r.method() === 'POST') posts.push(`${r.url().replace('http://localhost:5180', '')} ${r.postData()}`); });
  const input = Buffer.from(JSON.stringify({ nums: [3, 4], target: 7 })).toString('base64url');
  await p.goto(`http://localhost:5180/problem/two-sum?input=${input}&step=2`, { waitUntil: 'networkidle' });
  await p.waitForTimeout(600);
  console.log('#1 restored:', await p.getByLabel('Playback position').innerText(), '| posts', posts);
  posts.length = 0;
  await p.getByRole('button', { name: /^Previous:/ }).click();
  await p.waitForTimeout(1200);
  console.log('#1 after Previous ->', p.url(), '| POSTs to new problem:', posts.length ? posts : 'none', '|', await p.getByLabel('Playback position').innerText());

  // Library geometry and boundary colours
  const m = await b.newContext({ viewport: { width: 390, height: 844 } });
  for (const theme of ['light', 'dark']) {
    const q = await m.newPage();
    await q.addInitScript((t) => localStorage.setItem('dsa-ui:theme', JSON.stringify(t)), theme);
    await q.goto('http://localhost:5180/?category=Graphs&q=bfs', { waitUntil: 'networkidle' });
    const chip = await q.getByRole('button', { name: /Remove filter/ }).first().boundingBox();
    const colours = await q.evaluate(() => {
      const s = document.querySelector('#library-search').parentElement;
      return { searchBorder: getComputedStyle(s).borderTopColor, panel: getComputedStyle(s).backgroundColor, page: getComputedStyle(document.querySelector('main').parentElement).backgroundColor };
    });
    console.log(`${theme}: chip ${chip.width.toFixed(0)}x${chip.height.toFixed(0)}`, colours);
    await q.screenshot({ path: `followup-library-390-${theme}.jpg`, type: 'jpeg', quality: 70 });
    await q.close();
  }
  await b.close();
})();
