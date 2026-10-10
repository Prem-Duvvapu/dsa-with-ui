// P7 input-contract journey on a phone (390x844, light) against the real backend: one problem
// per field type (INT, STRING, INT_ARRAY, LINKED_LIST, BINARY_TREE, INT_GRID, GRAPH). Each is
// edited with real keystrokes/clicks, run, and (where the type allows) given an invalid value whose
// server error must appear tied to the field while the previous run stays on screen.
// Usage: PLAYWRIGHT_MODULE=<path to playwright> node inputs-journey.cjs [outDir]
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('node:fs');
const assert = require('node:assert/strict');
const installPacing = require('../pace-executions.cjs')();
const identity = require('../served-build-identity.cjs');
const out = process.argv[2] || __dirname;
const BASE = 'http://localhost:5180';
const results = [];
const check = (name, ok, detail = '') => results.push({ name, ok: Boolean(ok), detail });

(async () => {
  fs.mkdirSync(out, { recursive: true });
  const servedBuild = await identity(BASE);
  const browser = await chromium.launch();
  const ctx = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'light', hasTouch: true, isMobile: true });
  await ctx.addInitScript(() => { try { localStorage.setItem('dsa-ui:seenWelcome', 'true'); } catch (e) {} });
  const p = await ctx.newPage();
  await installPacing(p);
  const errors = [];
  p.on('pageerror', (e) => errors.push(e.message));
  const responses = [];
  const run = async () => {
    const response = p.waitForResponse(response => response.request().method() === 'POST'
      && new URL(response.url()).pathname.endsWith('/execute'));
    await p.getByRole('button', { name: /^Run input$/ }).click();
    const result = await response;
    assert([200, 400].includes(result.status()), `Unexpected input response ${result.status()}`);
    const body = await result.json(); // Body decode must finish before navigating away.
    responses.push({ pathname: new URL(result.url()).pathname, status: result.status(),
      resolvedInput: body.resolvedInput, fieldErrors: body.fieldErrors,
      steps: body.steps?.length, truncated: body.truncated });
    await p.waitForFunction(() => [...document.querySelectorAll('button')]
      .some(button => button.textContent.trim() === 'Run input' && !button.disabled));
  };
  const counter = async () => ((await p.locator('body').innerText()).match(/Step 1 of (\d+)/) || [])[1];
  const overflow = () => p.evaluate(() => document.documentElement.scrollWidth - innerWidth);
  const ranNew = async (before) => { await p.waitForTimeout(900); return (await p.locator('body').innerText()).includes('Changes not run') === false && p.url() !== before; };
  const errorFor = async (label) => p.evaluate((name) => {
    const el = [...document.querySelectorAll('label')].find((l) => l.textContent.trim() === name);
    const target = el?.htmlFor ? document.getElementById(el.htmlFor) : el?.closest('[role="group"]');
    const ids = (target?.getAttribute('aria-describedby') || '').split(' ').filter(Boolean);
    // Only the field's error element, not its help text.
    return ids.filter((id) => id.endsWith('-error')).map((id) => document.getElementById(id)?.textContent).join(' ');
  }, label);

  // INT: type a negative number from empty with real keystrokes; then empty → server error.
  await p.goto(`${BASE}/problem/two-sum`, { waitUntil: 'networkidle' });
  const target = p.getByLabel('Target sum');
  await target.click(); await target.press('Control+a'); await target.press('Backspace');
  await target.type('-5');
  check('INT: a negative number can be typed from empty', await target.inputValue() === '-5');
  await target.press('Control+a'); await target.press('Backspace');
  check('INT: an emptied field stays empty (no invented 0)', await target.inputValue() === '');
  let url = p.url();
  await run();
  await p.waitForTimeout(900);
  check('INT: empty is rejected by the server, error tied to the field, URL unchanged',
    /whole number/.test(await errorFor('Target sum')) && p.url() === url, await errorFor('Target sum'));
  await target.fill('13'); url = p.url(); await run();
  check('INT: a valid value runs', await ranNew(url));

  // STRING: whitespace and Unicode.
  await p.goto(`${BASE}/problem/shortest-palindrome`, { waitUntil: 'networkidle' });
  const stringInput = p.locator('input[type="text"]').first();
  await stringInput.fill('  Ünïcödé, ok  '); url = p.url(); await run(); await p.waitForTimeout(900);
  const stringError = await stringInput.evaluate((el) => (el.getAttribute('aria-describedby') || '').split(' ')
    .filter((id) => id.endsWith('-error')).map((id) => document.getElementById(id)?.textContent).join(' '));
  check('STRING: whitespace/Unicode either runs or is refused with a field error, never silently',
    p.url() !== url || stringError.length > 0, stringError || 'ran');
  await stringInput.fill('abcd'); url = p.url(); await run();
  check('STRING: a plain value runs', await ranNew(url));

  // INT_ARRAY: add, negative, duplicate, remove.
  await p.goto(`${BASE}/problem/largest-element`, { waitUntil: 'networkidle' });
  await p.getByRole('group', { name: 'Array' }).getByRole('button', { name: /^Add$/ }).click();
  const chips = p.getByRole('group', { name: 'Array' }).getByLabel(/Position \d+ value/);
  const n = await chips.count();
  await chips.nth(n - 1).fill('-7');
  await chips.nth(0).fill(await chips.nth(1).inputValue());
  await p.getByRole('button', { name: `Remove position ${n - 1}` }).click();
  url = p.url(); await run();
  check('INT_ARRAY: add / negative / duplicate / remove, then run', await ranNew(url), `${await chips.count()} values`);

  // LINKED_LIST.
  await p.goto(`${BASE}/problem/reverse-linked-list`, { waitUntil: 'networkidle' });
  const listChips = p.getByLabel(/Position \d+ value/);
  await listChips.first().fill('42'); url = p.url(); await run();
  check('LINKED_LIST: edit a node and run', await ranNew(url) && /42/.test(await p.locator('[data-audit="stage"]').innerText()));

  // BINARY_TREE: make a position null (sparse tree), run.
  await p.goto(`${BASE}/problem/tree-preorder`, { waitUntil: 'networkidle' });
  const nullable = p.getByRole('button', { name: /click to clear/ });
  const lastNullable = nullable.nth((await nullable.count()) - 1);
  await lastNullable.click();
  url = p.url(); await run();
  check('BINARY_TREE: clear a node to null (sparse) and run', await ranNew(url));

  // INT_GRID: add a row and a column, toggle a cell, run.
  await p.goto(`${BASE}/problem/flood-fill`, { waitUntil: 'networkidle' });
  await p.getByRole('button', { name: 'Add a row' }).first().click();
  await p.getByRole('button', { name: 'Add a column' }).first().click();
  await p.getByRole('button', { name: /^Cell row 1, column 1/ }).first().click();
  url = p.url(); await run(); await p.waitForTimeout(900);
  const gridError = await errorFor('Image');
  check('INT_GRID: rectangular edit runs or is refused with a tied error', p.url() !== url || gridError.length > 0, gridError || 'ran');

  // GRAPH: an endpoint outside the vertex range → error; isolated vertices allowed.
  await p.goto(`${BASE}/problem/graph-intro`, { waitUntil: 'networkidle' });
  await p.getByLabel('Vertices').fill('6');
  await p.getByLabel('Edge 1 to vertex').fill('9');
  url = p.url(); await run(); await p.waitForTimeout(900);
  const graphError = await errorFor('Undirected graph');
  check('GRAPH: an out-of-range endpoint is refused, error tied to the field', graphError.length > 0 && p.url() === url, graphError);
  await p.getByLabel('Edge 1 to vertex').fill('2'); url = p.url(); await run();
  check('GRAPH: isolated vertices (6 vertices) with a valid edge run', await ranNew(url));

  check('no horizontal overflow after all edits', (await overflow()) <= 0);
  check('no page errors', errors.length === 0, errors.join('; '));
  await p.screenshot({ path: `${out}/p7-inputs-graph-390-light.png` });
  assert.deepEqual(await identity(BASE), servedBuild, 'Served bundle changed during input checks');
  fs.writeFileSync(`${out}/input-results.json`, JSON.stringify({
    generated: new Date().toISOString(), complete: true, servedBuild,
    source: 'Real backend; Chromium phone/touch emulation, not a real virtual keyboard',
    results, responses
  }, null, 2));
  await browser.close();
  for (const r of results) console.log(`${r.ok ? 'PASS' : 'FAIL'}  ${r.name}${r.detail ? `  (${r.detail})` : ''}`);
  process.exitCode = results.every((r) => r.ok) ? 0 : 1;
})();
