// Production bundle + real backend. No mocked executions or relaxed server limits.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const api = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
const engine = process.env.AUDIT_ENGINE || 'chromium';
const mode = process.env.AUDIT_MODE || 'standard';
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 1);
const out = path.join(__dirname, 'requested-five');
const pace = require('../evidence/pace-executions.cjs')();
const cases = [
  { id: 'longest-increasing-subsequence', input: { nums: [1, 2, 1, 3, 2, 4] }, answer: '4', table: 'canonical' },
  { id: 'stock-transaction-fee', input: { prices: [1, 3, 2, 8, 4, 9], fee: 2 }, answer: '8', table: 'canonical' },
  { id: 'min-insertions-palindrome', input: { text: 'mbadm' }, answer: '2', table: 'tabulation' },
  { id: 'count-partitions-given-diff', input: { nums: [1, 1, 2, 3], d: 1 }, answer: '3', table: 'canonical' },
  { id: 'ninja-and-his-friends', input: { grid: [[3, 1, 1], [2, 5, 1], [1, 5, 5], [2, 1, 1]] }, answer: '24', table: 'tabulation' }
];
assert(['chromium', 'firefox'].includes(engine));
assert(['standard', 'maximum'].includes(mode));
assert([1, 2].includes(zoom) && (zoom === 1 || engine === 'chromium'));
const sizes = process.env.AUDIT_SIZES ? process.env.AUDIT_SIZES.split(',').map(s => s.split('x').map(Number))
  : [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];
const themes = process.env.AUDIT_THEMES ? process.env.AUDIT_THEMES.split(',') : ['light', 'dark'];
const main = page => page.getByRole('slider', { name: 'Seek to step', exact: true });
const selector = page => page.getByRole('combobox', { name: 'Solution approach', exact: true });
const share = input => Buffer.from(JSON.stringify(input)).toString('base64url');
let decodeTrace;
async function ready(page) {
  await selector(page).waitFor();
  await page.waitForFunction(() => !document.querySelector('select[id]')?.disabled);
}
async function capture(page, action, status = 200) {
  const waiting = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute'));
  await action();
  const response = await waiting;
  assert.equal(response.status(), status);
  const raw = await response.json();
  const run = status === 200 ? { ...raw, steps: decodeTrace(raw) } : raw;
  await ready(page);
  if (status === 200) await page.waitForFunction(id => {
    const shown = new URL(location.href).searchParams.get('approach');
    return shown === id || (id === 'canonical' && shown === null);
  }, run.approachId);
  return run;
}
async function selectRun(page, form) {
  const link = page.url();
  await selector(page).selectOption(form);
  assert.equal(page.url(), link, 'Preparing an approach changed the share link');
  return capture(page, () => page.getByRole('button', { name: /^Run (recursion|memoization|tabulation)/ }).click());
}
async function safe(page, errors) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide horizontal overflow');
  assert.deepEqual(errors, []);
  assert(await page.locator('[data-recursion-diagram]').count() <= 1, 'Duplicate primary canvases');
}
async function maximumInput(c, form) {
  const response = await fetch(`${api}/api/problems/${c.id}?approach=${form}`);
  assert.equal(response.status, 200);
  const detail = await response.json();
  const field = detail.inputSpec.fields[0], bounds = field.constraints;
  const n = bounds.maxLength ?? bounds.maxRows;
  switch (c.id) {
    case 'longest-increasing-subsequence': return { nums: Array.from({ length: n }, (_, i) => i) };
    case 'stock-transaction-fee': return { prices: Array.from({ length: n }, (_, i) => i % 2 ? 100 : 0), fee: 0 };
    case 'min-insertions-palindrome': return { text: 'abcdefghijkl'.slice(0, n) };
    case 'count-partitions-given-diff': return { nums: Array(n).fill(1), d: 0 };
    case 'ninja-and-his-friends': return { grid: Array.from({ length: n }, () => Array(bounds.maxCols).fill(100)) };
    default: throw Error(c.id);
  }
}
(async () => {
  ({ decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js')));
  fs.mkdirSync(out, { recursive: true });
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = zoom === 2 ? await require('../evidence/native-browser-zoom.cjs')(types.chromium) : await types[engine].launch();
  let browserVersion = zoom === 2 ? null : browser.version();
  const rows = [];
  try {
    for (const c of cases) for (const [width, height] of sizes) for (const theme of themes)
      for (const initial of mode === 'maximum' ? ['recursion', 'memoization', ...(c.table === 'tabulation' ? ['tabulation'] : [])] : ['recursion']) {
        const page = await browser.newPage({ viewport: { width, height }, colorScheme: theme });
        if (browserVersion === null) {
          const session = await page.context().newCDPSession(page);
          try { browserVersion = (await session.send('Browser.getVersion')).product.split('/').at(-1); }
          finally { await session.detach(); }
        }
        page.setDefaultTimeout(20000);
        await page.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        await pace(page);
        const errors = []; let requests = 0;
        page.on('pageerror', error => errors.push(error.message));
        page.on('request', r => { if (new URL(r.url()).pathname.endsWith('/execute')) requests++; });
        const input = mode === 'maximum' ? await maximumInput(c, initial) : c.input;
        let run = await capture(page, () => page.goto(`${base}/problem/${c.id}?approach=${initial}&input=${share(input)}&step=3`));
        assert.equal(run.approachId, initial);
        assert.deepEqual(run.resolvedInput, input);
        assert(!run.truncated);
        if (mode === 'standard') assert.equal(run.steps.at(-1).variables.answer, c.answer);
        await page.waitForFunction(() => document.querySelector('input[aria-label="Seek to step"]')?.value === '2');
        if (zoom === 2) await browser.setZoom(page, 2);
        assert.equal(await page.evaluate(() => innerWidth), Math.round(width / zoom));
        if (mode === 'maximum') {
          await main(page).fill(String(run.stepCount - 1));
          if (initial === 'tabulation') {
            assert.equal(run.dsType, 'DpTable');
            assert.equal(await page.locator('table.dp-table').count(), 1);
          } else {
            const tree = page.locator('[data-recursion-diagram]');
            assert.equal(await tree.count(), 1);
            const calls = Number(run.steps.at(-1).variables.calls);
            assert.equal(await tree.locator('g').count(), calls);
            assert(calls <= 220);
            const root = tree.locator('[data-recursion-root]');
            assert(await root.evaluate(element => {
              const box = element.getBoundingClientRect(), area = element.closest('[data-testid="recursion-tree-stage"]').getBoundingClientRect();
              return box.right > area.left && box.left < area.right && box.bottom > area.top && box.top < area.bottom;
            }), 'Maximum-input root outside local viewport');
          }
          if (initial === 'memoization') {
            await page.getByText('Memo table', { exact: true }).click();
            if (c.id === 'ninja-and-his-friends') assert(await page.getByText('Showing memo slice: row 0. Other slices remain cached.').isVisible());
          }
        } else {
          const before = requests;
          await selector(page).selectOption('memoization');
          assert.equal(requests, before, 'Selecting alone executed');
          run = await capture(page, () => page.getByRole('button', { name: 'Run memoization', exact: true }).click());
          assert.equal(run.steps.at(-1).variables.answer, c.answer);
          assert.deepEqual(run.resolvedInput, input);
          assert(await page.locator('#stage-title').evaluate(e => document.activeElement === e), 'Run focus did not reach stage');
          await main(page).fill('2');
          await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
          const beforeViews = requests;
          for (const view of ['Code walkthrough', 'Analysis', 'Playground']) {
            await page.getByRole('tab', { name: view, exact: true }).click();
            if (view === 'Code walkthrough') {
              const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
              if (await sourceButton.isVisible()) await sourceButton.click();
              const code = await page.getByRole('region', { name: 'Java source', exact: true }).textContent();
              assert(code.includes('public class Solution') && code.includes('private int solve') && code.includes('memo'),
                'Shown memoized source is not the complete selected executable');
            }
            assert.equal(await main(page).inputValue(), '2');
            await safe(page, errors);
          }
          assert.equal(requests, beforeViews, 'A presentation change executed');
          if (width === 390 && theme === 'dark' && zoom === 1) {
            await page.getByRole('button', { name: 'Compare approaches', exact: true }).click();
            const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
            await panel.getByLabel('Approach 1', { exact: true }).selectOption('memoization');
            await panel.getByLabel('Approach 2', { exact: true }).selectOption('recursion');
            await panel.getByRole('button', { name: 'Run comparison', exact: true }).click();
            await page.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
            assert.equal(await main(page).inputValue(), '2');
            await safe(page, errors);
            await page.getByRole('button', { name: 'Hide approach comparison', exact: true }).click();
          }
          const tabulated = await selectRun(page, c.table);
          assert.equal(tabulated.steps.at(-1).variables.answer, c.answer);
          assert.equal(tabulated.dsType, 'DpTable');
          if (width === 320 && theme === 'dark' && zoom === 1) {
            await main(page).fill('2');
            await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
            const reload = await capture(page, () => page.reload());
            assert.equal(reload.approachId, c.table);
            assert.deepEqual(reload.resolvedInput, input);
            await page.waitForFunction(() => document.querySelector('input[aria-label="Seek to step"]')?.value === '2');
            await page.getByRole('button', { name: 'Edit input', exact: true }).click();
            assert(await page.locator('#try-input-title').evaluate(e => document.activeElement === e));
            if (c.id === 'ninja-and-his-friends') {
              await page.getByRole('button', { name: 'Add a column', exact: true }).click();
              await page.getByRole('button', { name: 'Add a column', exact: true }).click();
              await selector(page).selectOption('memoization');
            } else if (c.id === 'min-insertions-palindrome') await page.getByRole('textbox', { name: 'Text', exact: true }).fill('UPPER');
            else await page.getByLabel('Position 1 value', { exact: true }).fill(c.id === 'count-partitions-given-diff' ? '0' : '1000');
            const oldLink = page.url();
            await capture(page, () => page.getByRole('button', { name: 'Run input', exact: true }).click(), 400);
            assert.equal(page.url(), oldLink, 'Rejected input changed the shown share link');
            assert(await page.getByRole('alert', { name: 'This input could not run' }).evaluate(e => document.activeElement === e));
          }
        }
        await safe(page, errors);
        if (engine === 'chromium' && zoom === 1 && width === 1366 && theme === 'light'
          && (mode === 'standard' || c.id === 'ninja-and-his-friends')) {
          await page.locator('#stage-title').scrollIntoViewIfNeeded();
          await page.screenshot({ path: path.join(out, `${engine}-${mode}-${c.id}-${initial}-${zoom}.png`) });
        }
        if (mode === 'maximum' && engine === 'chromium' && width === 320 && theme === 'light'
          && c.id === 'ninja-and-his-friends' && initial === 'memoization') {
          await page.getByText('Showing memo slice: row 0. Other slices remain cached.').scrollIntoViewIfNeeded();
          await page.screenshot({ path: path.join(out, 'chromium-maximum-ninja-memo-phone-light.png') });
        }
        rows.push({ problemId: c.id, width, height, theme, nativeZoom: zoom, mode, initial,
          input, answer: run.steps.at(-1).variables.answer, calls: run.steps.at(-1).variables.calls,
          steps: run.stepCount, noOverflowOrErrors: true, requests,
          ...(mode === 'standard' ? { allThreeExecuted: true, inputAndStepRestored: true, viewsDoNotExecute: true,
            comparisonChecked: width === 390 && theme === 'dark' && zoom === 1,
            reloadAndRefusalChecked: width === 320 && theme === 'dark' && zoom === 1 } : {
              allCallsRenderedAndRootVisible: initial !== 'tabulation', fullTableRendered: initial === 'tabulation' }) });
        console.log(`PASS ${engine} ${mode} ${c.id} ${width}×${height} ${theme} ${initial} zoom${zoom}`);
        if (zoom === 2) await browser.closePage(page);
        else await page.close();
      }
    fs.writeFileSync(path.join(out, `${engine}-${mode}-zoom${zoom}.json`), JSON.stringify({ generated: new Date().toISOString(),
      realBackend: true, buildMode: 'production-preview', engine, browserVersion, servedBuild, rows }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
