// Real backend + served production bundle. No injected successful traces or relaxed rate limit.
const fs = require('fs');
const path = require('path');
const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const engine = process.env.AUDIT_ENGINE || 'chromium';
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 1);
const tag = process.env.AUDIT_OUTPUT_TAG || (zoom === 2 ? 'zoom-200' : engine);
assert(['chromium', 'firefox'].includes(engine));
assert([1, 2].includes(zoom) && (zoom === 1 || engine === 'chromium'));
assert(/^[a-z0-9-]+$/.test(tag));
const pace = require('../evidence/pace-executions.cjs')();
let decodeTrace;
const pairs = process.env.AUDIT_SIZES ? process.env.AUDIT_SIZES.split(',').map(p => p.split('x').map(Number))
  : [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];
assert(pairs.every(p => p.length === 2 && p.every(n => Number.isInteger(n) && n > 0)));
const themes = ['light', 'dark'];
const heightsOf = async page => page.getByRole('spinbutton').evaluateAll(es => es.map(e => Number(e.value)));
const main = page => page.getByRole('slider', { name: 'Seek to step', exact: true });
async function ready(page) {
  await page.getByRole('combobox', { name: 'Solution approach' }).waitFor();
  await page.waitForFunction(() => !document.querySelector('select')?.disabled);
}
async function draft(page, heights) {
  while (await page.getByRole('spinbutton').count() < heights.length)
    await page.getByRole('button', { name: 'Add', exact: true }).click();
  while (await page.getByRole('spinbutton').count() > heights.length) {
    const length = await page.getByRole('spinbutton').count();
    await page.getByRole('button', { name: `Remove position ${length}`, exact: true }).click();
  }
  for (let i = 0; i < heights.length; i++) await page.getByLabel(`Position ${i + 1} value`, { exact: true }).fill(String(heights[i]));
  assert.deepEqual(await heightsOf(page), heights);
}
async function submit(page, expectedStatus = 200) {
  const response = page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute'));
  await page.getByRole('button', { name: 'Run input', exact: true }).click();
  const actual = await response;
  assert.equal(actual.status(), expectedStatus);
  const raw = await actual.json();
  const body = expectedStatus === 200 ? { ...raw, steps: decodeTrace(raw) } : raw;
  await ready(page);
  if (expectedStatus === 200) await page.waitForFunction(expected => {
    const input = new URL(location.href).searchParams.get('input');
    if (!input) return false;
    return JSON.stringify(JSON.parse(atob(input.replace(/-/g, '+').replace(/_/g, '/'))).heights) === JSON.stringify(expected);
  }, body.resolvedInput.heights);
  return body;
}
async function safe(page, errors) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide horizontal overflow');
  assert.deepEqual(errors, [], 'Uncaught browser errors');
  assert.equal(await page.locator('[data-recursion-diagram]').count() <= 1, true, 'Duplicate primary recursion canvases');
}
(async () => {
  ({ decodeTrace } = await import(path.resolve(__dirname, '../../../frontend/src/trace/decodeTrace.js')));
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = zoom === 1 ? await types[engine].launch()
    : await require('../evidence/native-browser-zoom.cjs')(types.chromium);
  const rows = [];
  try {
    for (const [width, height] of pairs) for (const theme of themes) {
      const page = await browser.newPage({ viewport: { width, height }, colorScheme: theme });
      page.setDefaultTimeout(15000);
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      let requests = 0;
      const errors = [], executions = [];
      page.on('pageerror', error => errors.push(error.message));
      page.on('request', r => { if (new URL(r.url()).pathname.endsWith('/execute')) requests++; });
      page.on('response', r => {
        if (new URL(r.url()).pathname.endsWith('/execute')) executions.push((async () => {
          const raw = await r.json();
          return { status: r.status(), method: r.request().method(),
            body: r.status() === 200 ? { ...raw, steps: decodeTrace(raw) } : raw };
        })().catch(error => ({ status: r.status(), decodeError: error.message })));
      });
      await page.goto(`${base}/problem/frog-jump?approach=canonical`);
      await ready(page);
      assert.equal(await page.getByRole('combobox', { name: 'Solution approach' }).inputValue(), 'canonical');
      assert((await page.getByRole('status', { name: 'Solution approach' }).textContent()).includes('Showing: Tabulation'));
      if (zoom === 2) await browser.setZoom(page, 2);
      const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight, dpr: devicePixelRatio,
        cssZoom: getComputedStyle(document.documentElement).zoom }));
      assert.equal(viewport.width, Math.round(width / zoom));
      assert.equal(viewport.cssZoom, '1');
      await draft(page, Array(7).fill(5));
      const originalLink = page.url(), beforePrepare = requests;
      await page.getByRole('combobox', { name: 'Solution approach' }).selectOption('memoization');
      assert.equal(page.url(), originalLink);
      assert.equal(requests, beforePrepare, 'Preparing an approach executes');
      const memo = await submit(page);
      assert.equal(memo.approachId, 'memoization');
      assert.equal(memo.steps.at(-1).variables.answer, '0');
      assert.equal(memo.steps.at(-1).variables.cacheHits, '5');
      assert(await page.locator('#stage-title').evaluate(e => document.activeElement === e), 'Successful run did not focus stage');
      await main(page).fill('2');
      await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
      await page.getByRole('button', { name: 'Edit input', exact: true }).click();
      assert(await page.locator('#try-input-title').evaluate(e => document.activeElement === e), 'Edit input did not focus editor');
      await page.getByLabel('Position 1 value', { exact: true }).fill('9');
      const beforeCompare = requests, committedLink = page.url();
      const open = page.getByRole('button', { name: 'Compare approaches', exact: true });
      await open.focus(); // Setup; subsequent opening/tab/slider actions use native keyboard events.
      await page.keyboard.press('Shift+Tab');
      await page.keyboard.press('Tab');
      assert(await open.evaluate(e => document.activeElement === e));
      await page.keyboard.press('Enter');
      const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
      assert.equal(requests, beforeCompare, 'Opening comparison executes');
      assert((await panel.textContent()).includes('Stair heights length 2–10, values 0–999'));
      const controls = [panel.getByLabel('Approach 1', { exact: true }), panel.getByLabel('Approach 2', { exact: true }),
        panel.getByRole('button', { name: 'Run comparison', exact: true })];
      for (const control of controls) {
        await page.keyboard.press('Tab');
        assert(await control.evaluate(e => document.activeElement === e), 'Native comparison tab order');
      }
      const run = controls.at(-1);
      assert(await run.evaluate(e => parseFloat(getComputedStyle(e).outlineWidth) >= 2), 'No visible keyboard focus ring');
      await page.keyboard.press('Enter');
      await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
      assert(await run.evaluate(e => document.activeElement === e), 'Comparison loses submit focus');
      await page.keyboard.press('Tab');
      const memoSlider = panel.getByRole('slider', { name: 'Memoization: step', exact: true });
      const recSlider = panel.getByRole('slider', { name: 'Recursion: step', exact: true });
      assert(await memoSlider.evaluate(e => document.activeElement === e));
      await page.keyboard.press('ArrowRight');
      assert.equal(await memoSlider.inputValue(), '2');
      assert.equal(await recSlider.inputValue(), '1');
      assert.equal(await main(page).inputValue(), '2');
      assert.equal(page.url(), committedLink);
      assert.equal(await page.getByLabel('Position 1 value').inputValue(), '9');
      await page.keyboard.press('Control+k');
      await page.getByRole('dialog').waitFor();
      await page.keyboard.press('Escape');
      assert(await memoSlider.evaluate(e => document.activeElement === e), 'Switcher loses comparison focus');
      const targetSizes = await panel.locator('button,select,input').evaluateAll(es => es.map(e => {
        const r = e.getBoundingClientRect(); return { width: r.width, height: r.height };
      }));
      assert(targetSizes.every(r => r.width >= 44 && r.height >= 44), 'Comparison target below44px');
      const beforeViews = requests;
      await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
      assert((await page.getByRole('tabpanel').textContent()).includes('Integer[] memo'), 'Complete memoized source absent');
      await safe(page, errors);
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      assert.equal(requests, beforeViews, 'View change executes');
      assert.equal(await memoSlider.inputValue(), '2');
      await safe(page, errors);
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      assert.equal(await page.getByLabel('Position 1 value').inputValue(), '9', 'View change overwrote draft');
      // The separate default/other-case comparison remains selected-approach aware and isolated.
      const beforeOther = executions.length;
      await page.getByRole('button', { name: 'Compare other case', exact: true }).click();
      const inputCompare = page.getByRole('group', { name: 'Compare the default input against the other case', exact: true });
      await inputCompare.waitFor();
      const otherResponses = await Promise.all(executions.slice(beforeOther));
      assert.equal(otherResponses.length, 2);
      assert(otherResponses.every(r => r.status === 200 && r.body.approachId === 'memoization'));
      assert.deepEqual(otherResponses.map(r => r.body.steps.at(-1).variables.answer).sort(), ['30', '40']);
      assert.equal(await main(page).inputValue(), '2');
      assert.equal(page.url(), committedLink);
      await page.getByRole('button', { name: 'Hide comparison', exact: true }).click();
      await page.getByRole('button', { name: 'Hide approach comparison', exact: true }).click();
      await draft(page, Array(11).fill(5));
      const goodLink = page.url();
      await page.getByRole('combobox', { name: 'Solution approach' }).selectOption('recursion');
      assert.equal((await heightsOf(page)).length, 11);
      assert(await page.getByRole('button', { name: 'Add', exact: true }).isDisabled());
      await submit(page, 400);
      assert(await page.getByRole('alert').filter({ hasText: 'This input could not run' }).evaluate(e => document.activeElement === e), '400 error summary not focused');
      assert.equal(page.url(), goodLink, 'Refusal changed successful link');
      assert.equal((await heightsOf(page)).length, 11, 'Refusal shrank draft');
      assert((await page.getByRole('status', { name: 'Solution approach' }).textContent()).includes('showing: Memoization'));
      // Largest advertised recursion: every actual call is rendered, with local scroll.
      await draft(page, Array.from({ length: 10 }, (_, i) => i % 2 ? 999 : 0));
      const recursion = await submit(page);
      assert.equal(recursion.steps.at(-1).variables.calls, '143');
      assert.equal(recursion.steps.at(-1).variables.answer, '999');
      await main(page).fill(String(recursion.stepCount - 1));
      const svg = page.locator('[data-recursion-diagram]');
      await page.getByRole('img', { name: 'Recursion tree, 143 calls explored', exact: true }).waitFor();
      assert.equal(await svg.locator('g[data-state]').count(), 143);
      const stage = page.getByRole('region', { name: 'Recursive calls', exact: true });
      const geometry = await stage.evaluate(e => ({ width: e.clientWidth, scrollWidth: e.scrollWidth,
        height: e.clientHeight, scrollHeight: e.scrollHeight, overflow: getComputedStyle(e).overflow }));
      assert(geometry.scrollWidth > geometry.width && geometry.height <= 480 && geometry.overflow === 'auto');
      const visibleRoot = () => stage.evaluate(e => {
        const r = e.querySelector('g[data-state]').getBoundingClientRect(), s = e.getBoundingClientRect();
        return r.right > s.left && r.left < s.right && r.bottom > s.top && r.top < s.bottom;
      });
      assert(await visibleRoot(), 'The complete recursion tree exists but its viewport looks empty');
      await safe(page, errors);
      // Largest memoized input:38 genuine calls,18 hits,20 known costs.
      await page.getByRole('combobox', { name: 'Solution approach' }).selectOption('memoization');
      await draft(page, Array.from({ length: 20 }, (_, i) => i % 2 ? 999 : 0));
      const largestMemo = await submit(page);
      assert.equal(largestMemo.steps.at(-1).variables.calls, '38');
      assert.equal(largestMemo.steps.at(-1).variables.cacheHits, '18');
      assert.equal(largestMemo.steps.at(-1).variables.answer, '999');
      await main(page).fill(String(largestMemo.stepCount - 1));
      await page.getByRole('img', { name: 'Recursion tree, 38 calls explored', exact: true }).waitFor();
      assert(await visibleRoot(), 'The maximum memo tree root is outside its visible viewport');
      await safe(page, errors);
      const maximumLink = page.url(), beforeMaximumViews = requests;
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      await page.getByText('Memo table', { exact: true }).click();
      assert((await page.getByRole('tabpanel').textContent()).includes('memo energy'), 'Largest memo table absent');
      await safe(page, errors);
      await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
      assert((await page.getByRole('tabpanel').textContent()).includes('Integer[] memo'));
      await safe(page, errors);
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      assert.equal(page.url(), maximumLink);
      assert.equal(requests, beforeMaximumViews, 'Maximum-input view changes execute');
      let screenshot = null;
      if (width === pairs[0][0] && theme === 'dark' || width === pairs.at(-1)[0] && theme === 'light') {
        const target = path.join(__dirname, `frog-jump-${tag}-${width}-${theme}.png`);
        await page.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
        if (browser.screenshot) screenshot = await browser.screenshot(page, target);
        else { await page.screenshot({ path: target, fullPage: true }); screenshot = path.basename(target); }
      }
      const share = new URL(page.url());
      share.searchParams.set('view', 'code');
      share.searchParams.set('step', '3');
      await Promise.all(executions); // Finish body reads before retiring this document.
      await page.goto(share.href);
      await ready(page);
      await page.waitForFunction(() => document.querySelector('input[aria-label="Seek to step"]')?.value === '2');
      assert((await page.getByRole('tabpanel').textContent()).includes('Integer[] memo'));
      assert.equal(await page.getByRole('combobox', { name: 'Solution approach' }).inputValue(), 'memoization');
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      await page.getByText('Memo table', { exact: true }).click();
      assert((await page.getByRole('tabpanel').textContent()).includes('Unknown is not zero.'), 'Unknown memo cells explained incorrectly');
      await safe(page, errors);
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      assert.deepEqual(await heightsOf(page), largestMemo.resolvedInput.heights);
      assert.equal(await main(page).inputValue(), '2');
      await safe(page, errors);
      const complete = await Promise.all(executions);
      assert(complete.every(r => !r.decodeError && [200, 400].includes(r.status)),
        `Unexpected execution/decode result: ${JSON.stringify(complete.filter(r => r.decodeError || ![200, 400].includes(r.status)))}`);
      assert(complete.filter(r => r.status === 200).every(r => r.body.truncated === false));
      const comparisonRuns = complete.filter(r => r.method === 'POST'
        && JSON.stringify(r.body.resolvedInput?.heights) === JSON.stringify(Array(7).fill(5)));
      assert(comparisonRuns.some(r => r.body.approachId === 'recursion' && r.body.steps.at(-1).variables.answer === '0'));
      rows.push({ width, height, theme, nativeZoom: zoom, viewport, screenshot, requests,
        legacyCanonicalLink: true, preparedSelectionDoesNotExecute: true, zeroMemoHits: true,
        nativeComparisonKeyboardAndFocus: true, sameCommittedInputAndIndependentSliders: true,
        otherCaseUsesShownApproach: true, viewsDoNotExecute: true, refusedArrayKeptWithOldLink: true,
        recursion143CallsVisible: true, memo20HeightsComplete: true, visibleMaximumTreeRoots: true, sharedInputThenStepRestored: true,
        targetSizes, largestRecursionGeometry: geometry, noOverflowOrErrors: true });
      console.log(`PASS Frog Jump ${width}x${height} ${theme} ${zoom * 100}% ${engine}`);
      if (browser.closePage) await browser.closePage(page); else await page.close();
    }
    fs.writeFileSync(path.join(__dirname, `frog-jump-${tag}-results.json`), JSON.stringify({
      generated: new Date().toISOString(), problemId: 'frog-jump', engine, buildMode: 'production-preview',
      servedBuild, rows, scope: 'Frog Jump repeated real-backend scenarios; not54-problem, real-device, screen-reader, learner or performance certification. Keyboard focus setup is programmatic; subsequent comparison actions use native keys. Native zoom uses real tab zoom, not CSS/device emulation. Overflow/error checks cover page width and uncaught page errors, not all console/network messages.'
    }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
