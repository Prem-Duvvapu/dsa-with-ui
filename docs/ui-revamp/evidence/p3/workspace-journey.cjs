const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const pos = (p) => p.locator('p[aria-live="polite"]').innerText();
(async () => {
  const b = await chromium.launch();
  const ctx = await b.newContext({ viewport: { width: 1366, height: 768 } });
  await ctx.addInitScript(() => { localStorage.setItem('dsa-ui:seenWelcome', 'true');  });
  const p = await ctx.newPage();
  const errors = []; p.on('pageerror', (e) => errors.push(String(e)));
  const posts = []; p.on('request', (r) => { if (r.method() === 'POST') posts.push(r.url().split('/api/')[1]); });

  // Array: Two Sum custom run
  await p.goto('http://localhost:5180/problem/two-sum', { waitUntil: 'networkidle' });
  await p.getByRole('button', { name: 'Edit input' }).click();
  console.log('Edit input focus ->', await p.evaluate(() => document.activeElement.textContent));
  await p.getByLabel('Target sum').fill('18');
  console.log('changes-not-run shown:', await p.getByText('Changes not run').isVisible());
  await p.getByRole('button', { name: 'Run input' }).click();
  await p.waitForTimeout(800);
  console.log('Array run -> focus', await p.evaluate(() => document.activeElement.textContent), '| echo', (await p.getByTestId('input-summary').innerText()).replace(/\s+/g, ' '), '|', await pos(p));
  await p.getByRole('button', { name: 'Next', exact: true }).click();
  await p.getByRole('button', { name: 'Next', exact: true }).click();
  const before = posts.length;
  for (const tab of ['Code walkthrough', 'Analysis', 'Playground']) {
    await p.getByRole('tab', { name: tab }).click();
    console.log(`  view ${tab}:`, await pos(p));
  }
  console.log('  draft after views:', await p.getByLabel('Target sum').inputValue(), '| extra POSTs:', posts.length - before, '| url', p.url());
  // keyboard on tabs
  await p.getByRole('tab', { name: 'Playground' }).focus();
  await p.keyboard.press('ArrowRight');
  console.log('  ArrowRight on tab -> focus', await p.evaluate(() => document.activeElement.textContent), '| step', (await pos(p)).slice(0, 40));
  await p.keyboard.press('Enter');
  console.log('  Enter -> selected', await p.getByRole('tab', { name: 'Code walkthrough' }).getAttribute('aria-selected'));
  const shared = p.url();
  const q = await ctx.newPage();
  await q.goto(shared, { waitUntil: 'networkidle' }); await q.waitForTimeout(600);
  console.log('Reload shared ->', q.url() === shared, '| tab', await q.getByRole('tab', { selected: true }).innerText(), '|', await pos(q));

  // Graph + Queue: BFS custom run (start vertex 2)
  await p.goto('http://localhost:5180/problem/bfs-traversal', { waitUntil: 'networkidle' });
  await p.getByLabel('Start vertex').fill('2').catch(async () => { const f = p.locator('#try-input input[type=number]').last(); await f.fill('2'); });
  await p.getByRole('button', { name: 'Run input' }).click(); await p.waitForTimeout(800);
  console.log('Graph run ->', (await p.getByTestId('input-summary').innerText()).replace(/\s+/g, ' ').slice(0, 60), '| queue pane:', await p.getByText('Queue', { exact: true }).count(), '|', (await pos(p)).slice(0, 60));
  await p.screenshot({ path: 'p3-graph-custom.jpg', type: 'jpeg', quality: 70 });

  // 2D DP: LCS via Other case
  await p.goto('http://localhost:5180/problem/longest-common-subsequence', { waitUntil: 'networkidle' });
  await p.getByRole('button', { name: /Other case/ }).click(); await p.waitForTimeout(800);
  console.log('DP other case ->', (await p.getByTestId('input-summary').innerText()).replace(/\s+/g, ' '), '|', (await pos(p)).slice(0, 60));

  await p.goto('http://localhost:5180/problem/nope-nope', { waitUntil: 'networkidle' });
  console.log('Unknown id ->', await p.locator('h1').innerText());
  console.log('POSTs:', posts.join(' ; '));
  console.log('page errors:', errors.length, errors.slice(0, 3));
  await b.close();
})();
