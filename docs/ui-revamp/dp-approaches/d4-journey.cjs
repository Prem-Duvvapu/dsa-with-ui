const fs = require('fs');
const path = require('path');
const browserTypes = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const engine = process.env.AUDIT_ENGINE || 'chromium';
if (!['chromium', 'firefox'].includes(engine)) throw Error(`Unsupported audit engine: ${engine}`);
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 1);
if (![1, 2].includes(zoom) || zoom !== 1 && engine !== 'chromium') throw Error('Native zoom audit supports Chromium at 200% only');
const height = Number(process.env.AUDIT_HEIGHT || 900);
const outputTag = process.env.AUDIT_OUTPUT_TAG || '';
if (outputTag && !/^[a-z0-9-]+$/.test(outputTag)) throw Error('Invalid audit output tag');
const prefix = (engine === 'chromium' ? 'd4' : `d4-${engine}`) + (outputTag ? `-${outputTag}` : '');
const pace = require('../evidence/pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
function assert(value, message) { if (!value) throw Error(message); }
async function ready(page) {
  await page.getByRole('combobox', { name: 'Solution approach' }).waitFor();
  await page.waitForFunction(() => !document.querySelector('select')?.disabled);
}
async function submit(page) {
  const n = Number(await page.getByLabel('Stairs', { exact: true }).inputValue());
  const [response] = await Promise.all([
    page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute')),
    page.getByRole('button', { name: 'Run input', exact: true }).click()
  ]);
  assert(response.status() === 200, `Main submission HTTP ${response.status()}`);
  await ready(page);
  await page.waitForFunction(n => {
    const value = new URL(location.href).searchParams.get('input');
    return value && JSON.parse(atob(value.replace(/-/g, '+').replace(/_/g, '/'))).n === n;
  }, n);
}
async function compare(page, panel) {
  const responses = [];
  const listener = response => {
    if (response.request().method() === 'POST' && new URL(response.url()).pathname.endsWith('/execute')) {
      responses.push((async () => {
        try { return { status: response.status(), body: await response.json() }; }
        catch (error) { return { status: response.status(), body: null, decodeError: error.message }; }
      })());
    }
  };
  page.on('response', listener);
  await panel.getByRole('button', { name: 'Run comparison', exact: true }).click();
  await page.waitForFunction(() => document.querySelector('#approach-comparison button')?.getAttribute('aria-busy') === 'false');
  await page.waitForFunction(() => document.querySelectorAll('#approach-comparison section').length === 2);
  page.off('response', listener);
  return Promise.all(responses);
}
(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = zoom === 1 ? await browserTypes[engine].launch()
    : await require('../evidence/native-browser-zoom.cjs')(browserTypes.chromium);
  const rows = [];
  try {
    const widths = process.env.AUDIT_WIDTHS ? process.env.AUDIT_WIDTHS.split(',').map(Number) : [320, 390, 768, 1366, 1440];
    const heights = process.env.AUDIT_HEIGHTS ? process.env.AUDIT_HEIGHTS.split(',').map(Number) : widths.map(() => height);
    assert(heights.length === widths.length && [...widths, ...heights].every(n => Number.isInteger(n) && n > 0),
      'Audit widths/heights must be paired positive pixel sizes');
    const themes = process.env.AUDIT_THEMES ? process.env.AUDIT_THEMES.split(',') : ['light', 'dark'];
    for (const [index, width] of widths.entries()) for (const theme of themes) {
      const height = heights[index];
      const page = await browser.newPage({ viewport: { width, height }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      let requests = 0;
      const errors = [];
      page.on('request', r => { if (new URL(r.url()).pathname.endsWith('/execute')) requests++; });
      page.on('pageerror', e => errors.push(e.message));
      await page.goto(`${base}/problem/climbing-stairs`);
      await ready(page);
      const nativeZoom = zoom === 1 ? 1 : await browser.setZoom(page, zoom);
      const viewport = await page.evaluate(() => ({ width: innerWidth, height: innerHeight,
        devicePixelRatio, cssZoom: getComputedStyle(document.documentElement).zoom }));
      assert(zoom === 1 || viewport.width === Math.round(width / zoom)
        && viewport.cssZoom === '1', 'Native zoom did not reflow the CSS viewport as expected');
      await page.getByLabel('Stairs', { exact: true }).fill('7');
      await submit(page);
      await page.getByRole('slider', { name: 'Seek to step', exact: true }).fill('2');
      await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
      const mainLink = page.url();
      await page.getByLabel('Stairs', { exact: true }).fill('9');
      await page.getByRole('combobox', { name: 'Solution approach' }).selectOption('memoization');
      const before = requests;
      await page.getByText('How the shown approach works', { exact: true }).click();
      assert((await page.getByText('Showing Tabulation. This explanation describes the displayed execution, not a prepared selection.').count()) === 1, 'Teaching follows candidate instead of displayed run');
      assert(await page.getByText('ways(k) = ways(k-1) + ways(k-2)', { exact: true }).isVisible(), 'Recurrence unavailable');
      await page.getByRole('button', { name: 'Compare approaches', exact: true }).click();
      const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
      assert(requests === before, 'Opening comparison executed');
      const first = await compare(page, panel);
      assert(first.length === 2 && first.every(r => r.status === 200 && r.body.resolvedInput.n === 7), 'Comparison changed input or failed');
      const recursive = first.find(r => r.body.approachId === 'recursion').body;
      assert(recursive.steps.at(-1).variables.calls === '41' && recursive.steps.at(-1).variables.answer === '21', 'Wrong recursion counters/answer');
      await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
      assert(await page.getByLabel('Stairs', { exact: true }).inputValue() === '9' && page.url() === mainLink, 'Comparison changed main draft/link');
      await panel.getByRole('slider', { name: 'Recursion: step', exact: true }).fill('4');
      assert(await panel.getByRole('slider', { name: 'Tabulation: step', exact: true }).inputValue() === '1', 'Positions synchronized by ordinal');
      const runButton = panel.getByRole('button', { name: 'Run comparison', exact: true });
      await runButton.focus();
      await page.keyboard.press('ArrowRight');
      assert(await page.getByRole('slider', { name: 'Seek to step', exact: true }).inputValue() === '2', 'Comparison keyboard moved main playback');
      const after = requests;
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      assert(await panel.getByRole('slider', { name: 'Recursion: step', exact: true }).inputValue() === '4' && requests === after, 'View switch lost/refetched comparison');
      await panel.getByLabel('Approach 2', { exact: true }).selectOption('memoization');
      assert(requests === after && await panel.getByRole('region', { name: 'Recursion comparison', exact: true }).count() === 0, 'Pair preparation executed or relabelled old result');
      const second = await compare(page, panel);
      const memo = second.find(r => r.body.approachId === 'memoization').body;
      const stats = memo.steps.at(-1).variables;
      assert(stats.answer === '21' && stats.calls === '13' && stats.cacheHits === '5' && stats.computedStates === '8', 'Wrong memo metrics');
      assert((await panel.getByRole('region', { name: 'Tabulation comparison' }).textContent()).includes('not instrumented'), 'Invented tabulation counters');
      const targets = await panel.locator('select, button, input').evaluateAll(elements => elements.map(e => e.getBoundingClientRect().height));
      assert(targets.every(h => h >= 44), 'Comparison target below 44px');
      const boxes = await panel.locator('section').evaluateAll(elements => elements.map(e => {
        const r = e.getBoundingClientRect(); return { x: r.x, y: r.y, width: r.width };
      }));
      assert(viewport.width >= 768 || boxes[1].y > boxes[0].y, 'Phone summaries squeezed side by side');
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page overflow');
      let screenshot = null;
      if (width === 390 && theme === 'dark' || width === 1366 && theme === 'light') {
        await page.evaluate(() => { document.activeElement?.blur(); window.scrollTo(0, 0); });
        const target = path.join(__dirname, `${prefix}-${width}-${theme}.png`);
        if (browser.screenshot) screenshot = await browser.screenshot(page, target);
        else await page.screenshot({ path: target, fullPage: true });
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Screenshot altered the page layout');
      }
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      await page.getByLabel('Stairs', { exact: true }).fill('20');
      await submit(page); // Candidate memoization: legal n20, committed independently of comparison.
      const shownLink = page.url();
      await panel.getByLabel('Approach 2', { exact: true }).selectOption('recursion');
      const refused = await compare(page, panel);
      assert(refused.length === 2 && refused.every(r => r.body) && refused.some(r => r.status === 400)
        && refused.some(r => r.status === 200), `Expected independent refusal/success: ${JSON.stringify(refused)}`);
      assert(await panel.getByRole('alert').count() === 1 && await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).count() === 0, 'Refusal labelled complete');
      assert(page.url() === shownLink && await page.getByLabel('Stairs', { exact: true }).inputValue() === '20', 'Refusal changed main identity/draft');
      assert(errors.length === 0, `Browser errors: ${errors}`);
      rows.push({ width, height, theme, nativeZoom, viewport, screenshot, realBackend: true, openingPairAndViewsDoNotFetch: true,
        sameInputSnapshotAndMainPreserved: true, realCountersAndAnswers: true, independentPositionsAndKeys: true,
        refusalKeepsMainAndSuccessfulSide: true, targets, noOverflowOrErrors: true });
      console.log(`PASS ${width} ${theme}`);
      if (browser.closePage) await browser.closePage(page);
      else await page.close();
    }
    fs.writeFileSync(path.join(__dirname, `${prefix}-results.json`), JSON.stringify({ generated: new Date().toISOString(), engine: engine === 'chromium' ? 'Chromium' : 'Firefox',
      buildMode: process.env.AUDIT_BUILD_MODE || 'production-preview', servedBuild, rows,
      scope: `D4 Climbing Stairs pilot, physical viewports ${widths.map((w, i) => `${w}x${heights[i]}`).join(', ')}, ${zoom * 100}% native browser zoom; noOverflowOrErrors checks page-wide overflow and uncaught page errors, not every console/network message; not all-family, human or broad accessibility certification` }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
