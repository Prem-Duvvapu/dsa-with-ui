// Bounded workspace layout acceptance; real default responses are captured then replayed
// unchanged. This checks presentation, not new custom executions or every tracer answer.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const browsers = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const identity = require('../served-build-identity.cjs');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const engine = process.env.AUDIT_ENGINE || 'chromium';
const out = process.argv[2];
assert(out, 'Provide an evidence output directory');
assert(['chromium', 'firefox'].includes(engine));
const widths = [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];
const ids = ['two-sum', 'bfs-traversal', 'longest-common-subsequence',
  'longest-substring-without-repeating', 'frog-jump'];

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const capture = async url => {
    const response = await fetch(api + url);
    assert(response.ok, `Capture failed: ${url} ${response.status}`);
    return response.json();
  };
  const servedBuild = await identity(base);
  const catalog = await capture('/api/problems');
  const longest = catalog.reduce((a, b) => a.title.length >= b.title.length ? a : b);
  if (!ids.includes(longest.id)) ids.push(longest.id);
  const details = {}, traces = {};
  for (const id of ids) {
    details[id] = await capture(`/api/problems/${id}`);
    traces[id] = await capture(`/api/problems/${id}/execute?encoding=delta`);
    assert(traces[id].steps?.length && !traces[id].truncated, `Incomplete capture ${id}`);
    await new Promise(resolve => setTimeout(resolve, 1100));
  }
  const browser = await browsers[engine].launch();
  const rows = [];
  const save = (complete = false) => fs.writeFileSync(path.join(out, 'layout-results.json'), JSON.stringify({
    generated: new Date().toISOString(), browser: engine, servedBuild, complete,
    responseSource: 'Fresh real-backend default captures replayed unchanged; no custom runs',
    longestTitle: { id: longest.id, title: longest.title }, rows
  }, null, 2));
  try {
    for (const theme of ['light', 'dark']) for (const [width, height] of widths) {
      const context = await browser.newContext({ viewport: { width, height }, colorScheme: theme, reducedMotion: 'reduce' });
      await context.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const page = await context.newPage();
      let executions = 0, errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.route('**/api/problems**', async route => {
        const url = new URL(route.request().url());
        if (url.pathname === '/api/problems') return route.fulfill({ json: catalog });
        const match = url.pathname.match(/^\/api\/problems\/([^/]+)(\/execute)?$/);
        assert(match && details[match[1]], `Unexpected request ${url.pathname}`);
        if (match[2]) {
          assert.equal(route.request().method(), 'GET', 'Layout action submitted an input');
          executions += 1;
        }
        return route.fulfill({ json: match[2] ? traces[match[1]] : details[match[1]] });
      });
      for (const id of ids) {
        errors = [];
        await page.goto(`${base}/problem/${id}`, { waitUntil: 'networkidle' });
        await page.getByRole('button', { name: 'Play', exact: true }).waitFor();
        const failures = [];
        const check = (ok, message) => { if (!ok) failures.push(message); };
        const geometry = await page.evaluate(() => {
          const rect = selector => {
            const box = document.querySelector(selector)?.getBoundingClientRect();
            return box ? { x: box.x, y: box.y, width: box.width, height: box.height } : null;
          };
          return { overflow: document.documentElement.scrollWidth - innerWidth,
            context: rect('section[aria-labelledby="problem-title"]'),
            title: rect('#problem-title'), card: rect('section[aria-labelledby="stage-title"]'),
            stage: rect('[data-audit="stage"]'), frame: rect('.shell-stage'),
            rail: rect('[role="tablist"]') };
        });
        check(geometry.overflow <= 0, 'Initial page overflow');
        // A bounded card-start milestone is NOT the handoff's useful-frame target.
        // Keep BOTH measurements; remaining frame-position exceptions stay open.
        if (width === 1366 && id !== longest.id) {
          check(geometry.card?.y <= 280, `Stage card starts at ${geometry.card?.y}, target <=280`);
        }
        const family = await page.locator('[data-audit="stage"]').getAttribute('data-family');
        const minimum = width <= 640 ? (family === 'spatial' ? 340 : 300) : (family === 'spatial' ? 440 : 360);
        check(geometry.frame?.height >= minimum, `Useful frame shrunk below ${minimum}`);
        const stageActions = [];
        for (const name of ['Edit input', 'Show code', 'Focus']) {
          const control = page.getByRole('button', { name, exact: true });
          const box = await control.boundingBox();
          const fontSize = await control.evaluate(el => parseFloat(getComputedStyle(el).fontSize));
          stageActions.push({ name, ...box, fontSize });
          check(box.width >= 43.9 && box.height >= 43.9 && fontSize >= 14, `${name}: setup target/text too small`);
        }
        if (width <= 640) {
          check(Math.max(...stageActions.map(a => a.y)) - Math.min(...stageActions.map(a => a.y)) <= 1,
            'Phone setup actions consume multiple rows');
        }
        const before = executions;
        const approachHelp = page.locator('details').filter({ has: page.locator('summary').filter({ hasText: /^About / }) });
        if (await approachHelp.count()) {
          await approachHelp.locator('summary').click();
          check(await approachHelp.evaluate(el => el.open), 'Approach explanation did not open');
          check(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Expanded approach overflow');
          if (width >= 1100) {
            const setup = await approachHelp.evaluate(el => ({ width: el.getBoundingClientRect().width, parentWidth: el.parentElement.getBoundingClientRect().width }));
            check(setup.width >= setup.parentWidth - 2, 'Expanded approach trapped beside controls');
          }
          await approachHelp.locator('summary').click();
        }
        const slider = page.getByRole('slider', { name: 'Seek to step' });
        const index = Math.min(2, Number(await slider.getAttribute('max')));
        await slider.fill(String(index));
        await page.waitForFunction(step => new URLSearchParams(location.search).get('step') === String(step), index + 1);
        const input = new URL(page.url()).searchParams.get('input');
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        const draftFields = page.locator('section[aria-labelledby="try-input-title"] input');
        const first = draftFields.first();
        const type = await first.getAttribute('type');
        const value = await first.inputValue();
        await first.fill(type === 'number' ? String(Number(value) + 1) : value + 'z');
        const snapshotDraft = () => draftFields.evaluateAll(fields => fields.map(el => ({ type: el.type, value: el.value, checked: el.checked })));
        const draftSnapshot = await snapshotDraft();
        for (const label of ['Code walkthrough', 'Analysis', 'Playground']) {
          await page.getByRole('tab', { name: label, exact: true }).click();
          await page.waitForTimeout(100);
          check(await slider.inputValue() === String(index), `Step lost in ${label}`);
          check(new URL(page.url()).searchParams.get('input') === input, `Input URL lost in ${label}`);
          check(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `Overflow in ${label}`);
        }
        check(JSON.stringify(await snapshotDraft()) === JSON.stringify(draftSnapshot), 'Edited draft lost across views');
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        const heading = page.getByRole('heading', { name: 'Try your own input', exact: true });
        check(await heading.evaluate(el => document.activeElement === el), 'Edit input did not focus editor heading');
        const field = page.getByRole('textbox', { name: 'Preset name' });
        await page.getByRole('button', { name: 'Save input', exact: true }).click();
        const saveOverflow = await page.evaluate(() => document.documentElement.scrollWidth - innerWidth);
        check(saveOverflow <= 0, `Save form page overflow ${saveOverflow}`);
        await field.fill('a');
        await page.getByRole('button', { name: 'Confirm save' }).click();
        await page.getByRole('button', { name: 'Save input', exact: true }).click();
        await field.fill('A long saved-input name that must stay readable and never widen the page');
        await page.getByRole('button', { name: 'Confirm save' }).click();
        const targets = [];
        // Keyboard modality is required for :focus-visible; programmatic focus after
        // a mouse click alone is not evidence of the keyboard ring a learner sees.
        await page.keyboard.press('Tab');
        for (const name of ['Load a', 'Remove preset a']) {
          const button = page.getByRole('button', { name, exact: true });
          await button.focus();
          // The existing reduced-motion rule still allows a 0.01ms transition. Sample
          // settled paint, not an intermediate outline-offset immediately after focus.
          await button.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
          const box = await button.boundingBox();
          const focus = await button.evaluate(el => {
            const css = getComputedStyle(el);
            const r = el.getBoundingClientRect(), rail = document.querySelector('[role="tablist"]').getBoundingClientRect();
            return { outlineStyle: css.outlineStyle, outlineWidth: parseFloat(css.outlineWidth),
              outlineOffset: parseFloat(css.outlineOffset), beneathRail: r.top >= rail.bottom };
          });
          targets.push({ name, width: box.width, height: box.height, focus });
          check(box.width >= 43.9 && box.height >= 43.9, `${name}: below44 target ${box.width}x${box.height}`);
          check(focus.outlineStyle !== 'none' && focus.outlineWidth >= 2 && focus.outlineOffset <= -focus.outlineWidth,
            `${name}: focus ring clipped by saved-input group`);
          check(focus.beneathRail, `${name}: focus obscured by sticky rail`);
        }
        check(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Long preset overflow');
        await page.getByRole('button', { name: 'Remove preset a', exact: true }).click();
        check(await page.getByRole('button', { name: 'Load a', exact: true }).count() === 0, 'Preset removal failed');
        const statement = page.locator('section[aria-labelledby="problem-title"] details');
        if (await statement.count()) {
          await statement.locator('summary').click();
          check(await statement.evaluate(el => el.open), 'Statement did not open');
          check(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Expanded statement overflow');
          const reading = await statement.boundingBox();
          if (width >= 1100) check(reading.width >= geometry.context.width - 2, 'Expanded statement trapped in narrow context column');
          await statement.locator('summary').click();
        }
        check(executions === before, 'Presentation or saving/removing executed again');
        check(errors.length === 0, `Page errors: ${errors.join('; ')}`);
        rows.push({ id, theme, width, height, geometry, stageActions, draftSnapshot, saveOverflow, targets, executions, failures }); save();
        console.log(`${failures.length ? 'FAIL' : 'PASS'} ${id} ${width} ${theme} card=${geometry.card?.y} frame=${geometry.frame?.y}${failures.length ? ': ' + failures.join('; ') : ''}`);
        if (id === 'two-sum' && [320, 1366].includes(width)) {
          await page.evaluate(() => scrollTo(0, 0));
          await page.screenshot({ path: path.join(out, `${id}-${width}-${theme}.png`) });
        }
      }
      await context.close();
    }
    assert.deepEqual(await identity(base), servedBuild, 'Served bundle changed during journey');
    save(true);
    process.exitCode = rows.some(row => row.failures.length) ? 1 : 0;
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
