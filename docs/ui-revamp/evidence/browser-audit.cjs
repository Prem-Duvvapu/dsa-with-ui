#!/usr/bin/env node
/*
 * Real-browser layout audit for the UI revamp.
 *
 * Not part of CI and not a project dependency: it drives the running dev servers
 * (backend 8923, Vite 5180) with a Playwright Chromium, captures screenshots and
 * measures the layout gates in docs/ui-revamp/IMPLEMENTATION_HANDOFF.md section 5.
 *
 *   npx -y -p playwright@1.63.0 node docs/ui-revamp/evidence/browser-audit.cjs \
 *     --out /tmp/dsa-audit --label p0
 *
 * Playwright is resolved from PLAYWRIGHT_MODULE, then from the normal module path.
 * Output: <out>/<label>-<surface>-<viewport>-<theme>.jpg plus <out>/<label>.json.
 */
const fs = require('fs');
const path = require('path');

function loadPlaywright() {
  const candidates = [process.env.PLAYWRIGHT_MODULE, 'playwright'].filter(Boolean);
  for (const candidate of candidates) {
    try { return require(candidate); } catch { /* try next */ }
  }
  throw new Error('Playwright not found. Run through `npx -p playwright` or set PLAYWRIGHT_MODULE.');
}

const args = Object.fromEntries(process.argv.slice(2).reduce((pairs, arg, i, all) => {
  if (arg.startsWith('--')) pairs.push([arg.slice(2), all[i + 1]]);
  return pairs;
}, []));
const BASE = args.base || 'http://localhost:5180';
const OUT = args.out || 'browser-audit';
const LABEL = args.label || 'audit';
const SURFACES = (args.surfaces || 'library,array,graph,dp2d').split(',');
const VIEWPORTS = (args.viewports || '390x844,1366x768').split(',');
const THEMES = (args.themes || 'light,dark').split(',');
const WORKSPACE = args.workspace || null; // 'next' opts into the flagged workspace

const PATHS = {
  library: '/',
  array: '/problem/two-sum',
  graph: '/problem/bfs-traversal',
  dp2d: '/problem/longest-common-subsequence',
  ...(args.paths ? JSON.parse(args.paths) : {})
};

async function measure(page) {
  return page.evaluate(() => {
    const box = (selector) => {
      const el = document.querySelector(selector);
      if (!el) return null;
      const r = el.getBoundingClientRect();
      return { top: Math.round(r.top + window.scrollY), left: Math.round(r.left), width: Math.round(r.width), height: Math.round(r.height) };
    };
    const firstRow = document.querySelector('[data-audit="result-row"], #algorithm-library li');
    const firstRowBox = firstRow ? firstRow.getBoundingClientRect() : null;
    return {
      title: document.title,
      viewport: { width: window.innerWidth, height: window.innerHeight },
      document: { scrollWidth: document.documentElement.scrollWidth, scrollHeight: document.documentElement.scrollHeight },
      horizontalOverflow: document.documentElement.scrollWidth > window.innerWidth + 1,
      search: box('input[aria-label="Search algorithms"]'),
      filtersToggle: box('[data-audit="filters-toggle"]'),
      firstResult: firstRowBox ? { top: Math.round(firstRowBox.top), bottom: Math.round(firstRowBox.bottom) } : null,
      stage: box('[data-audit="stage"] .shell-stage') || box('.shell-stage'),
      card: box('[data-audit="stage"]'),
      canvasRegion: box('[data-tour="canvas"]'),
      controls: box('[data-tour="controls"]'),
      narration: box('[role="status"][aria-live="polite"]'),
      viewRail: box('[role="tablist"][aria-label="Learning views"]'),
      alerts: Array.from(document.querySelectorAll('[role="alert"]')).map((el) => el.textContent.trim().slice(0, 120))
    };
  });
}

(async () => {
  const { chromium } = loadPlaywright();
  fs.mkdirSync(OUT, { recursive: true });
  const browser = await chromium.launch();
  const results = { base: BASE, label: LABEL, browser: `chromium ${browser.version()}`, capturedAt: new Date().toISOString(), runs: [] };

  for (const viewport of VIEWPORTS) {
    const [width, height] = viewport.split('x').map(Number);
    for (const theme of THEMES) {
      const context = await browser.newContext({ viewport: { width, height }, colorScheme: theme === 'dark' ? 'dark' : 'light' });
      await context.addInitScript(({ chosen, workspace }) => {
        try {
          localStorage.setItem('dsa-ui:theme', JSON.stringify(chosen));
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          if (workspace) localStorage.setItem('dsa-ui:workspace', JSON.stringify(workspace));
        } catch { /* storage denied */ }
      }, { chosen: theme, workspace: WORKSPACE });
      for (const surface of SURFACES) {
        const page = await context.newPage();
        const errors = [];
        page.on('pageerror', (e) => errors.push(String(e)));
        page.on('console', (m) => { if (m.type() === 'error') errors.push(m.text()); });
        await page.goto(BASE + PATHS[surface], { waitUntil: 'networkidle' });
        await page.waitForTimeout(600);
        const metrics = await measure(page);
        const file = `${LABEL}-${surface}-${viewport}-${theme}.jpg`;
        await page.screenshot({ path: path.join(OUT, file), type: 'jpeg', quality: 70 });
        results.runs.push({ ...metrics, surface, path: PATHS[surface], viewport, theme, screenshot: file, errors });
        await page.close();
      }
      await context.close();
    }
  }
  await browser.close();
  fs.writeFileSync(path.join(OUT, `${LABEL}.json`), JSON.stringify(results, null, 2));
  for (const run of results.runs) {
    console.log([run.surface, run.viewport, run.theme,
      `search@${run.search?.top ?? '-'}`, `firstRow@${run.firstResult?.top ?? '-'}-${run.firstResult?.bottom ?? '-'}`,
      `card@${run.card?.top ?? '-'}`, `diagram@${run.stage ? `${run.stage.top} ${run.stage.width}x${run.stage.height}` : '-'}`,
      `overflowX=${run.horizontalOverflow}`, `errors=${run.errors.length}`].join('  '));
  }
})().catch((error) => { console.error(error); process.exit(1); });
