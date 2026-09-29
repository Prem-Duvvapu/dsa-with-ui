const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
(async () => {
  const b = await chromium.launch(); const ctx = await b.newContext({ viewport: { width: 1366, height: 768 } });
  await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
  const p = await ctx.newPage();
  const errors = []; p.on('pageerror', e => errors.push(String(e)));
  await p.goto('http://localhost:5180/problem/does-not-exist', { waitUntil: 'networkidle' });
  console.log('B1 unknown id ->', p.url(), '|', await p.locator('h1').first().innerText());
  await p.screenshot({ path: 'p1-notfound.jpg', type: 'jpeg', quality: 70 });
  await p.goto('http://localhost:5180/problem/two-sum', { waitUntil: 'networkidle' });
  await p.getByRole('button', { name: 'Edit input' }).click();
  await p.getByLabel('Target sum').fill('18');
  console.log('dirty badge visible:', await p.getByText('Changes not run').isVisible());
  await p.getByRole('button', { name: 'Run with this input' }).click();
  await p.waitForTimeout(800);
  await p.getByRole('button', { name: 'Done editing' }).click();
  console.log('B2 summary:', (await p.getByTestId('input-summary').innerText()).replace(/\s+/g, ' '));
  await p.getByRole('button', { name: 'Edit input' }).click();
  console.log('B3 reopened target =', await p.getByLabel('Target sum').inputValue());
  const goodUrl = p.url();
  await p.getByLabel('Target sum').fill('99999');
  await p.getByRole('button', { name: 'Run with this input' }).click();
  await p.waitForTimeout(800);
  console.log('B4 url unchanged after 400:', p.url() === goodUrl, '| summary alert:', (await p.getByRole('alert').first().innerText()).replace(/\s+/g,' '));
  console.log('   focused element:', await p.evaluate(() => document.activeElement?.getAttribute('role') + ' ' + document.activeElement?.textContent.slice(0, 40)));
  await p.screenshot({ path: 'p1-rejected.jpg', type: 'jpeg', quality: 70 });
  // Reload the shared good link at a later step
  await p.getByRole('button', { name: 'Next', exact: true }).click();
  await p.getByRole('button', { name: 'Next', exact: true }).click();
  await p.waitForTimeout(300);
  const shared = p.url();
  console.log('shared url:', shared);
  const q = await ctx.newPage();
  await q.goto(shared, { waitUntil: 'networkidle' });
  await q.waitForTimeout(500);
  console.log('reload -> position:', await q.getByLabel('Playback position').innerText(), '| editor target:', await q.getByLabel('Target sum').inputValue(), '| step text:', (await q.locator('[aria-live=polite]').first().innerText()).slice(0, 80), '| url same:', q.url() === shared);
  // Out-of-range step
  await q.goto('http://localhost:5180/problem/two-sum?step=400', { waitUntil: 'networkidle' });
  await q.waitForTimeout(500);
  console.log('step 400 notice:', await q.getByRole('status', { name: 'Shared link' }).innerText(), '| url', q.url());
  // Rejected shared input
  const bad = Buffer.from(JSON.stringify({ nums: [2, 7], target: 99999 })).toString('base64url');
  await q.goto('http://localhost:5180/problem/two-sum?input=' + bad + '&step=3', { waitUntil: 'networkidle' });
  await q.waitForTimeout(600);
  console.log('bad link notice:', await q.getByRole('status', { name: 'Shared link' }).innerText(), '| url', q.url());
  await q.screenshot({ path: 'p1-badlink.jpg', type: 'jpeg', quality: 70 });
  console.log('page errors:', errors.length);
  await b.close();
})();
