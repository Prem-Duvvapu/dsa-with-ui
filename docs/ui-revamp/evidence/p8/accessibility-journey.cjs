// P8 keyboard, announcement and Analysis layout regression checks.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node accessibility-journey.cjs <outDir>
// Default journeys use the real backend; the deliberately long-variable stress case is
// explicitly a fixture. An accessibility tree / geometry check is not a screen-reader pass.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const installPacing = require('../pace-executions.cjs')();
const out = process.argv[2] || __dirname;
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';

const viewports = [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]]
  .filter(([width]) => !process.env.P8_WIDTHS || process.env.P8_WIDTHS.split(',').includes(String(width)));
const problems = ['two-sum', 'bfs-traversal', 'longest-common-subsequence'];
const stressProblem = {
  id: 'analysis-layout-probe', title: 'Analysis layout probe', category: 'Arrays',
  difficulty: 'Easy', dsType: 'Stack', traced: true, javaCode: 'return 1;',
  inputSpec: { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 3 }] }
};
const variableName = 'reportedVariable'.repeat(7);

async function geometry(page) {
  return page.evaluate(() => ({
    overflow: document.documentElement.scrollWidth - innerWidth,
    stageTop: document.querySelector('[data-audit="stage"]')?.getBoundingClientRect().top ?? null,
    frameTop: document.querySelector('.shell-stage')?.getBoundingClientRect().top ?? null,
    frameHeight: document.querySelector('.shell-stage')?.getBoundingClientRect().height ?? null
  }));
}

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const rows = [];
  const save = () => fs.writeFileSync(path.join(out, 'accessibility-results.json'), JSON.stringify({
    generated: new Date().toISOString(), browser: 'Chromium', complete: rows.length === viewports.length * 8, rows
  }, null, 2));
  try {
    for (const theme of ['light', 'dark']) {
      for (const [width, height] of viewports) {
        const context = await browser.newContext({ viewport: { width, height }, colorScheme: theme, reducedMotion: 'reduce' });
        await context.addInitScript((theme) => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        const page = await context.newPage();
        await installPacing(page);
        let errors = [];
        page.on('pageerror', e => errors.push(e.message));
        for (const id of problems) {
          errors = [];
          await page.goto(`${base}/problem/${id}`, { waitUntil: 'networkidle' });
          await page.getByRole('button', { name: 'Play', exact: true }).waitFor();
          const initial = await geometry(page);
          const failures = [];
          if (initial.overflow > 0) failures.push(`page overflow ${initial.overflow}px`);
          if (await page.getByText('Showing the checked-in offline sample.', { exact: false }).count()) failures.push('expected real backend, received offline sample');
          const playground = page.getByRole('tab', { name: 'Playground', exact: true });
          await playground.focus();
          await page.keyboard.press('End');
          const focused = await page.getByRole('tab', { name: 'Analysis', exact: true }).evaluate(el => {
            const rect = el.getBoundingClientRect();
            const rail = el.parentElement.getBoundingClientRect();
            return { fullyVisible: rect.left >= rail.left - 1 && rect.right <= rail.right + 1, focused: el === document.activeElement };
          });
          if (!focused.focused || !focused.fullyVisible) failures.push('keyboard-focused Analysis tab is clipped');
          if (await playground.getAttribute('aria-selected') !== 'true') failures.push('arrows selected a view');
          // The source is optional; all three views must gate their ONE narration region.
          for (const view of ['Playground', 'Code walkthrough', 'Analysis']) {
            await page.getByRole('tab', { name: view, exact: true }).click();
            const selectedVisible = await page.getByRole('tab', { name: view, exact: true }).evaluate(el => {
              const tab = el.getBoundingClientRect();
              const rail = el.parentElement.getBoundingClientRect();
              return tab.left >= rail.left - 1 && tab.right <= rail.right + 1;
            });
            if (!selectedVisible) failures.push(`${view}: selected tab is clipped`);
            const narration = page.locator('p[role="status"]');
            await page.getByRole('button', { name: 'Play', exact: true }).click();
            if (await narration.getAttribute('aria-busy') !== 'true') failures.push(`${view}: autoplay announcements are not held`);
            await page.getByRole('button', { name: 'Pause', exact: true }).click();
            if (await narration.getAttribute('aria-busy') !== 'false') failures.push(`${view}: pause does not release the current narration`);
            if (await narration.getAttribute('aria-atomic') !== 'true') failures.push(`${view}: narration is not atomic`);
          }
          await page.getByRole('tab', { name: 'Playground', exact: true }).click();
          if (errors.length) failures.push(...errors);
          rows.push({ kind: 'real-backend', id, theme, width, height, initial, focused, failures });
          save();
          console.log(`${failures.length ? 'FAIL' : 'PASS'} ${id} ${width} ${theme}`);
          if (id === 'two-sum' && [320, 1366].includes(width)) await page.screenshot({ path: path.join(out, `workspace-${width}-${theme}.png`) });
        }
        // Stress the widths independently of catalogue naming and current tracer choices.
        await page.route('**/api/problems**', route => {
          const pathname = new URL(route.request().url()).pathname;
          const body = pathname.endsWith('/execute') ? {
            encoding: 'delta', resolvedInput: { n: 3 }, anchors: { result: 1 },
            steps: [{ stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Stack',
              description: 'Reported long values', variables: { [variableName]: 'value'.repeat(35) }, queueOrStackState: ['item'.repeat(35)] }]
          } : pathname === '/api/problems' ? [stressProblem] : stressProblem;
          return route.fulfill({ json: body });
        });
        await page.goto(`${base}/problem/${stressProblem.id}?view=analysis`, { waitUntil: 'networkidle' });
        const initial = await geometry(page);
        const failures = [];
        const restoredVisible = await page.getByRole('tab', { name: 'Analysis', exact: true }).evaluate(el => {
          const tab = el.getBoundingClientRect();
          const rail = el.parentElement.getBoundingClientRect();
          return tab.left >= rail.left - 1 && tab.right <= rail.right + 1;
        });
        if (!restoredVisible) failures.push('restored Analysis tab is clipped');
        if (initial.overflow > 0) failures.push(`long variable pushes page ${initial.overflow}px sideways`);
        const disclosures = page.locator('main details summary');
        const sizes = await disclosures.evaluateAll(elements => elements.map(el => {
          const r = el.getBoundingClientRect();
          return { width: r.width, height: r.height };
        }));
        if (sizes.length !== 2 || sizes.some(r => r.width < 44 || r.height < 44)) failures.push('value disclosure lacks a 44px target');
        if (sizes[0]?.width > 0) {
          await disclosures.first().click();
          if (!(await page.locator('main pre').first().innerText()).includes('value'.repeat(35))) failures.push('expanded variable does not show the full value');
        } else {
          failures.push('variable disclosure has no reachable width');
        }
        const expanded = await geometry(page);
        if (expanded.overflow > 0) failures.push(`expanded value pushes page ${expanded.overflow}px sideways`);
        rows.push({ kind: 'fixture-long-values', theme, width, height, initial, expanded, sizes, failures });
        save();
        console.log(`${failures.length ? 'FAIL' : 'PASS'} fixture-long-values ${width} ${theme}`);
        if (width === 320) await page.screenshot({ path: path.join(out, `analysis-stress-${theme}.png`) });
        await context.close();
      }
    }
  } finally {
    save();
    await browser.close();
  }
  const failed = rows.filter(row => row.failures.length);
  for (const row of failed) console.log(`FAIL ${row.kind} ${row.id || ''} ${row.width} ${row.theme}: ${row.failures.join('; ')}`);
  console.log(`${rows.length - failed.length}/${rows.length} rows pass`);
  process.exitCode = failed.length ? 1 : 0;
})().catch(error => { console.error(error); process.exitCode = 1; });
