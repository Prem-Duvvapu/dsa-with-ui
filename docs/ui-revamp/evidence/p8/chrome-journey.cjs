// Native disclosures and compact chrome. Replays freshly captured real backend
// responses for repeatable layout checks without competing with the renderer sweep.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const out = process.argv[2] || __dirname;
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const widths = [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];
const ids = ['two-sum', 'bfs-traversal', 'longest-common-subsequence'];

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const capture = async (url) => {
    const response = await fetch(api + url);
    if (!response.ok) throw new Error(`Capture failed: ${url} ${response.status}`);
    return response.json();
  };
  const catalogue = await capture('/api/problems');
  const details = {}, traces = {};
  for (const id of ids) {
    details[id] = await capture(`/api/problems/${id}`);
    traces[id] = await capture(`/api/problems/${id}/execute`);
    await new Promise(resolve => setTimeout(resolve, 1100));
  }
  const browser = await chromium.launch();
  const rows = [];
  const save = (complete = false) => fs.writeFileSync(path.join(out, 'chrome-results.json'), JSON.stringify({
    generated: new Date().toISOString(), browser: 'Chromium', responseSource: 'fresh real-backend capture, replayed', complete, rows
  }, null, 2));
  try {
    for (const theme of ['light', 'dark']) for (const [width, height] of widths) {
      const context = await browser.newContext({ viewport: { width, height }, reducedMotion: 'reduce', colorScheme: theme });
      await context.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const page = await context.newPage();
      let executions = 0;
      await page.route('**/api/problems**', async route => {
        const url = new URL(route.request().url());
        const match = url.pathname.match(/^\/api\/problems\/([^/]+)(\/execute)?$/);
        if (url.pathname === '/api/problems') return route.fulfill({ json: catalogue });
        if (!match || !details[match[1]]) return route.fulfill({ status: 404, json: {} });
        if (match[2]) executions += 1;
        return route.fulfill({ json: match[2] ? traces[match[1]] : details[match[1]] });
      });
      for (const id of ids) {
        await page.goto(`${base}/problem/${id}`, { waitUntil: 'networkidle' });
        await page.getByRole('button', { name: 'Play', exact: true }).waitFor();
        const failures = [];
        const geometry = await page.evaluate(() => ({
          overflow: document.documentElement.scrollWidth - innerWidth,
          stageTop: document.querySelector('[data-audit="stage"]')?.getBoundingClientRect().top,
          frameTop: document.querySelector('.shell-stage')?.getBoundingClientRect().top,
          frameHeight: document.querySelector('.shell-stage')?.getBoundingClientRect().height,
          headerHeight: document.querySelector('header')?.getBoundingClientRect().height
        }));
        if (geometry.overflow > 0) failures.push(`overflow ${geometry.overflow}`);
        if (await page.getByRole('banner').count() !== 1) failures.push('duplicate page banners');
        const before = executions;
        const network = page.locator('summary[aria-label="Learning network"]');
        await network.focus();
        await page.keyboard.press('Space');
        const links = page.getByRole('navigation', { name: 'Learning network', exact: true }).getByRole('link');
        if (await links.count() !== 5) failures.push('network links unavailable');
        const hrefs = await links.evaluateAll(els => els.map(el => el.getAttribute('href')));
        if (!hrefs.includes('https://hld-with-ui.vercel.app/')) failures.push('HLD link lost');
        await links.last().focus();
        await page.keyboard.press('Escape');
        if (await network.evaluate(el => el.parentElement.open || document.activeElement !== el)) failures.push('network Escape/focus');
        if (await page.getByRole('navigation', { name: 'Learning network', exact: true }).count()) failures.push('closed network exposed');
        const menu = page.locator('summary').filter({ hasText: /^Menu$/ });
        if (await menu.isVisible()) {
          await menu.focus();
          await page.keyboard.press('Space');
          if (!await menu.evaluate(el => el.parentElement.open)) failures.push('phone Menu Space blocked');
          if (!await page.getByRole('button', { name: 'Play', exact: true }).count()) failures.push('Menu starts playback');
          await page.getByRole('navigation', { name: 'Workspace', exact: true }).getByRole('link', { name: 'All algorithms' }).focus();
          await page.keyboard.press('Escape');
          if (await menu.evaluate(el => el.parentElement.open || document.activeElement !== el)) failures.push('phone Menu Escape/focus');
        }
        for (const control of [network, page.getByRole('button', { name: /^Theme:/ })]) {
          const box = await control.boundingBox();
          if (!box || box.width < 43.9 || box.height < 43.9) failures.push('chrome target below 44px');
        }
        if (executions !== before) failures.push('disclosures execute a run');
        rows.push({ id, theme, width, height, geometry, failures }); save();
        console.log(`${failures.length ? 'FAIL' : 'PASS'} ${id} ${width} ${theme} frame=${geometry.frameTop}`);
        if (id === 'two-sum' && [320, 1366].includes(width)) await page.screenshot({ path: path.join(out, `compact-${width}-${theme}.png`) });
      }
      for (const pathname of ['/', '/problem/does-not-exist']) {
        await page.goto(base + pathname, { waitUntil: 'networkidle' });
        const overflow = await page.evaluate(() => document.documentElement.scrollWidth - innerWidth);
        rows.push({ pathname, theme, width, height, failures: overflow > 0 ? [`overflow ${overflow}`] : [] }); save();
      }
      await context.close();
    }
    save(true);
    if (rows.some(row => row.failures.length)) process.exitCode = 1;
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
