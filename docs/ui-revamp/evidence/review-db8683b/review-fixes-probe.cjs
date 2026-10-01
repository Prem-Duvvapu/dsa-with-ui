const { chromium } = require(process.env.PLAYWRIGHT_MODULE);
const B = 'http://localhost:5180';
const enc = (v) => Buffer.from(JSON.stringify(v)).toString('base64url');
const pos = (p) => p.locator('p[aria-live="polite"]').innerText();
(async () => {
  const b = await chromium.launch();
  const errors = [];
  const ctx = await b.newContext({ viewport: { width: 1366, height: 768 } });
  await ctx.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
  const p = await ctx.newPage(); p.on('pageerror', (e) => errors.push(String(e)));
  const nav = (url) => p.evaluate((u) => { history.pushState({}, '', u); dispatchEvent(new PopStateEvent('popstate')); }, url);

  // B1: hold the shared link's POST, navigate same-page to defaults, then release
  let releaseHeld;
  await p.route('**/api/problems/two-sum/execute', async (route) => {
    if (route.request().method() !== 'POST') return route.continue();
    await new Promise((r) => { releaseHeld = r; });
    return route.continue();
  });
  await p.goto(`${B}/problem/two-sum?input=${enc({ nums: [3, 4], target: 7 })}`, { waitUntil: 'domcontentloaded' });
  await p.waitForFunction(() => true); await p.waitForTimeout(1200);
  await nav('/problem/two-sum');
  await p.waitForTimeout(600);
  releaseHeld?.(); await p.waitForTimeout(1200);
  console.log('B1 after release ->', (await pos(p)).slice(0, 60), '| url', p.url().replace(B, ''));
  await p.unroute('**/api/problems/two-sum/execute');

  // B2: custom run shown, then same-page rejected link
  await p.goto(`${B}/problem/two-sum?input=${enc({ nums: [3, 4], target: 7 })}`, { waitUntil: 'networkidle' });
  await nav(`/problem/two-sum?input=${enc({ nums: [3, 4], target: 99999 })}`); await p.waitForTimeout(1200);
  const notice = await p.getByRole('status', { name: 'Shared link' }).innerText();
  const decoded = JSON.parse(Buffer.from(new URL(p.url()).searchParams.get('input') || 'e30', 'base64url').toString());
  console.log('B2 notice:', notice.replace(/\s+/g, ' ').slice(0, 120), '| url input', JSON.stringify(decoded), '| echo', (await p.getByTestId('input-summary').innerText()).replace(/\s+/g, ' '));

  // S1: default at step 3, same-page nav without step / step=400
  await p.goto(`${B}/problem/two-sum`, { waitUntil: 'networkidle' });
  for (let i = 0; i < 2; i++) await p.getByRole('button', { name: 'Next', exact: true }).click();
  await nav('/problem/two-sum'); await p.waitForTimeout(700);
  console.log('S1 no step ->', (await pos(p)).slice(0, 40));
  for (let i = 0; i < 2; i++) await p.getByRole('button', { name: 'Next', exact: true }).click();
  await nav('/problem/two-sum?step=400'); await p.waitForTimeout(700);
  console.log('S1 step=400 ->', (await p.getByRole('status', { name: 'Shared link' }).innerText()).slice(0, 70), '|', (await pos(p)).slice(0, 40));

  // B3: flood-fill (stack) companion + Analysis
  await p.goto(`${B}/problem/flood-fill?step=4`, { waitUntil: 'networkidle' }); await p.waitForTimeout(500);
  const companion = await p.locator('.companion-pane').first().getAttribute('aria-label');
  const front = await p.locator('.queue-cell-tag').count();
  await p.getByRole('tab', { name: 'Analysis' }).click(); await p.waitForTimeout(300);
  const regions = await p.locator('section[aria-label]').evaluateAll((els) => els.map((e) => e.getAttribute('aria-label')));
  console.log('B3 flood-fill companion:', companion, '| front tags', front, '| analysis', regions.join(', '));

  // S2: source End key, help + L
  await p.goto(`${B}/problem/bfs-traversal?view=code&step=3`, { waitUntil: 'networkidle' }); await p.waitForTimeout(500);
  const before = await pos(p);
  await p.getByRole('region', { name: 'Java source' }).focus(); await p.keyboard.press('End');
  console.log('S2 End in source keeps step:', (await pos(p)) === before, '|', await p.getByText(/Following execution|Not following/).innerText());
  await p.keyboard.press('?'); await p.waitForTimeout(200);
  await p.keyboard.press('l'); await p.keyboard.press('Home');
  console.log('S2 keys behind help keep step:', (await pos(p)) === before);
  await p.keyboard.press('Escape');

  // S5: geometry at 1000px, drag, leave mid-drag
  await p.setViewportSize({ width: 1000, height: 768 }); await p.waitForTimeout(400);
  const sep = p.getByRole('separator');
  if (await sep.count()) {
    await sep.focus(); await p.keyboard.press('End'); await p.waitForTimeout(150);
    const geo = await p.evaluate(() => { const s = document.querySelector('[role=separator]'); const a = s.previousElementSibling.getBoundingClientRect().width; const c = s.nextElementSibling.getBoundingClientRect().width; return { now: s.getAttribute('aria-valuenow'), text: s.getAttribute('aria-valuetext'), measured: Math.round(a / (a + c) * 100), panes: [a | 0, c | 0] }; });
    console.log('S5 at 1000px End ->', JSON.stringify(geo));
  } else console.log('S5 at 1000px: subviews (container too narrow for split)');
  await p.setViewportSize({ width: 1366, height: 768 }); await p.waitForTimeout(400);
  const box = await p.getByRole('separator').boundingBox();
  await p.mouse.move(box.x + box.width / 2, box.y + 80); await p.mouse.down(); await p.mouse.move(box.x - 150, box.y + 80, { steps: 4 });
  const mid = await p.getByRole('separator').getAttribute('aria-valuenow');
  await p.getByRole('tab', { name: 'Analysis' }).click({ force: true }).catch(() => {});
  await p.mouse.move(100, 300, { steps: 3 }); await p.mouse.up();
  await p.getByRole('tab', { name: 'Code walkthrough' }).click();
  console.log('S5 drag mid', mid, '| after leaving mid-drag, ratio', await p.getByRole('separator').getAttribute('aria-valuenow'), '| stored', await p.evaluate(() => localStorage.getItem('dsa-ui:codeSplit')));

  // S6: tabs
  await p.getByRole('tab', { name: 'Code walkthrough' }).focus(); await p.keyboard.press('ArrowRight');
  const tabs = await p.getByRole('tab').evaluateAll((els) => els.map((e) => `${e.textContent}:${e.tabIndex}:${e.getAttribute('aria-selected')}`));
  const controls = await p.evaluate(() => [...document.querySelectorAll('[role=tab]')].every((t) => document.getElementById(t.getAttribute('aria-controls'))?.getAttribute('role') === 'tabpanel'));
  console.log('S6 tabs', tabs.join(' | '), '| controls->tabpanel', controls, '| main landmarks', await p.getByRole('main').count());

  // S3/S4 narrow subview pause + horizontal source
  await p.setViewportSize({ width: 390, height: 844 }); await p.goto(`${B}/problem/bfs-traversal?view=code`, { waitUntil: 'networkidle' }); await p.waitForTimeout(400);
  await p.getByRole('button', { name: 'Play' }).click(); await p.getByRole('button', { name: 'Diagram', exact: true }).click();
  console.log('S3 subview change paused:', await p.getByRole('button', { name: 'Play' }).count() === 1);
  await p.getByRole('button', { name: 'Source', exact: true }).click();
  await p.getByRole('region', { name: 'Java source' }).evaluate((n) => { n.scrollLeft = 120; n.scrollTop = 60; });
  await p.getByRole('tab', { name: 'Analysis' }).click(); await p.getByRole('tab', { name: 'Code walkthrough' }).click();
  console.log('S4 source scroll after views:', await p.getByRole('region', { name: 'Java source' }).evaluate((n) => [n.scrollLeft, n.scrollTop]));
  const overflow = await p.evaluate(() => document.documentElement.scrollWidth > innerWidth + 1);
  console.log('390 overflow', overflow, '| page errors', errors.length, errors.slice(0, 2));
  await b.close();
})();
