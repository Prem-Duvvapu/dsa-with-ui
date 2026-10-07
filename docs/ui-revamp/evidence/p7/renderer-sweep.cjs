// P7 renderer coverage sweep against the real backend. For every dsType in the live catalogue,
// three representative problems (first, middle, last catalogued) run with their default input and
// their declared alternate input, at 1366x768 dark and 390x844 light, inspected at the first,
// middle and final step: stage drawn (not "No visualization"/"Nothing to draw"), no page-wide
// horizontal overflow, no page errors. Writes renderer-manifest.json beside this script.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node renderer-sweep.cjs [outDir]
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const installPacing = require('../pace-executions.cjs')();
const out = process.argv[2] || __dirname;
const BASE = 'http://localhost:5180';
const API = 'http://localhost:8923';
const b64url = (obj) => Buffer.from(JSON.stringify(obj)).toString('base64').replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const catalogue = await (await fetch(`${API}/api/problems`)).json();
  const byType = {};
  for (const p of catalogue) (byType[p.dsType] ||= []).push(p.id);
  const picks = [];
  for (const [dsType, ids] of Object.entries(byType)) {
    const chosen = [...new Set([ids[0], ids[Math.floor(ids.length / 2)], ids[ids.length - 1]])];
    for (const id of chosen) picks.push({ dsType, id });
  }
  const browser = await chromium.launch();
  const rows = [];
  const save = (complete = false) => fs.writeFileSync(`${out}/renderer-manifest.json`, JSON.stringify({
    generated: new Date().toISOString(), complete, picks, rows
  }, null, 2));
  try {
  for (const [width, height, scheme] of [[1366, 768, 'dark'], [390, 844, 'light']]) {
    const ctx = await browser.newContext({ viewport: { width, height }, colorScheme: scheme });
    await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
    const p = await ctx.newPage();
    await installPacing(p);
    let errors = [];
    p.on('pageerror', (e) => errors.push(e.message));
    const shotTaken = new Set();
    for (const { dsType, id } of picks) {
      const detail = await (await fetch(`${API}/api/problems/${id}`)).json();
      const variants = [['default', '']];
      if (detail.alternateInput) variants.push(['alternate', `?input=${b64url(detail.alternateInput)}`]);
      for (const [variant, query] of variants) {
        errors = [];
        const row = { dsType, id, variant, viewport: `${width}x${height}`, scheme, positions: {}, ok: true, problems: [] };
        try {
          await p.goto(`${BASE}/problem/${id}${query}`, { waitUntil: 'networkidle', timeout: 30000 });
          await p.waitForSelector('[data-audit="stage"]', { timeout: 15000 });
          await p.waitForFunction(() => /Step 1 of \d+/.test(document.body.innerText) || /did not load|not yet traced|Nothing to draw/.test(document.body.innerText), null, { timeout: 15000 });
          const body = await p.locator('body').innerText();
          const total = Number((body.match(/Step \d+ of (\d+)/) || [])[1] || 0);
          row.steps = total;
          if (!total) { row.ok = false; row.problems.push('no steps'); }
          const slider = p.getByRole('slider').first();
          for (const [name, value] of [['first', 0], ['middle', Math.floor((total - 1) / 2)], ['final', Math.max(0, total - 1)]]) {
            if (total) {
              if (name === 'first') await p.keyboard.press('Home');
              else if (name === 'final') await p.keyboard.press('End');
              else await slider.fill(String(value));
              await p.waitForTimeout(120);
            }
            const probe = await p.evaluate(() => {
              const stage = document.querySelector('[data-audit="stage"]');
              const r = stage?.getBoundingClientRect();
              const text = stage?.innerText ?? '';
              return {
                overflow: document.documentElement.scrollWidth - innerWidth,
                stageHeight: Math.round(r?.height ?? 0),
                unsupported: /No visualization|Nothing to draw/.test(text),
                // Drawn = the canvas frame holds a drawing or text. An empty structure that says
                // "empty" is drawn honestly; a frame with nothing in it is not.
                drawn: (() => {
                  const frame = stage?.querySelector('.shell-stage') ?? stage;
                  return Boolean(frame && (frame.querySelector('svg, canvas, img') || frame.innerText.trim().length > 0));
                })()
              };
            });
            row.positions[name] = probe;
            if (probe.overflow > 0) { row.ok = false; row.problems.push(`${name}: page overflow ${probe.overflow}px`); }
            if (probe.unsupported) { row.ok = false; row.problems.push(`${name}: unsupported/nothing drawn`); }
            if (!probe.drawn) { row.ok = false; row.problems.push(`${name}: no drawn elements found`); }
          }
          const key = `${dsType}-${width}`;
          if (!shotTaken.has(key) || !row.ok) {
            shotTaken.add(key);
            await p.screenshot({ path: `${out}/sweep-${dsType}-${id}-${variant}-${width}.png` });
          }
        } catch (e) {
          row.ok = false; row.problems.push(`exception: ${e.message.split('\n')[0]}`);
        }
        if (errors.length) { row.ok = false; row.problems.push(`page errors: ${errors.join('; ')}`); }
        rows.push(row);
        save();
        console.log(`${row.ok ? 'PASS' : 'FAIL'}  ${row.viewport} ${scheme} ${dsType} ${id} ${variant} (${row.steps ?? '?'} steps)${row.problems.length ? '  ' + row.problems.join(' | ') : ''}`);
      }
    }
    await ctx.close();
  }
  save(true);
  } finally {
    await browser.close();
  }
  const failed = rows.filter((r) => !r.ok);
  console.log(`\n${rows.length - failed.length}/${rows.length} pass across ${Object.keys(byType).length} dsTypes, ${picks.length} problems`);
  process.exitCode = failed.length ? 1 : 0;
})().catch(error => { console.error(error); process.exitCode = 1; });
