const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const pace = require('../evidence/pace-executions.cjs')();
function assert(value, message) { if (!value) throw Error(message); }
async function execute(page, action, approach, status = 200) {
  const [response] = await Promise.all([
    page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute')),
    action()
  ]);
  assert(response.status() === status, `${approach}: HTTP ${response.status()}`);
  const run = await response.json();
  if (status === 200) {
    assert(run.approachId === approach && !run.truncated && run.code.includes('public class Solution'), `${approach}: wrong/partial executable`);
    await page.waitForFunction(id => document.querySelector('select')?.value === id
      && !document.querySelector('select')?.disabled, approach);
  }
  return run;
}
async function ready(page, approach = 'tabulation') {
  await page.getByRole('combobox', { name: 'Solution approach' }).waitFor();
  await page.waitForFunction(id => document.querySelector('select')?.value === id
    && !document.querySelector('select')?.disabled, approach);
}
async function source(page) {
  await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
  const button = page.getByRole('button', { name: 'Source', exact: true });
  if (await button.isVisible()) await button.click();
  const region = page.getByRole('region', { name: 'Java source', exact: true });
  await region.waitFor();
  return region.textContent();
}
async function overflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide overflow');
}
(async () => {
  const browser = await chromium.launch();
  const rows = [];
  const geometry = [];
  try {
    const widths = process.env.AUDIT_WIDTHS ? process.env.AUDIT_WIDTHS.split(',').map(Number) : [320, 390, 768, 1366, 1440];
    const themes = process.env.AUDIT_THEMES ? process.env.AUDIT_THEMES.split(',') : ['light', 'dark'];
    for (const width of widths) for (const theme of themes) {
      const page = await browser.newPage({ viewport: { width, height: 900 }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      const errors = [];
      let requests = 0;
      page.on('pageerror', e => errors.push(e.message));
      page.on('request', r => { if (new URL(r.url()).pathname.endsWith('/execute')) requests++; });
      await page.goto(`${base}/problem/climbing-stairs`);
      await ready(page);
      const selector = page.getByRole('combobox', { name: 'Solution approach' });
      const height = (await selector.boundingBox()).height;
      assert(height >= 44, 'Approach selector is smaller than 44px');
      await page.getByLabel('Stairs', { exact: true }).fill('7');
      let memoLink;
      for (const approach of ['recursion', 'memoization', 'tabulation']) {
        const before = requests;
        await selector.selectOption(approach);
        assert(requests === before, 'Selection executed instead of preparing');
        const run = await execute(page, () => page.getByRole('button', { name: `Run ${approach}`, exact: true }).click(), approach);
        assert(run.resolvedInput.n === 7 && run.steps.at(-1).variables.answer === '21', 'Wrong pilot answer/input');
        const text = await source(page);
        assert(text.includes(approach === 'memoization' ? 'Integer[] memo' : approach === 'recursion' ? 'return ways(n)' : 'int[] ways'), 'Displayed source belongs to another approach');
        const afterRun = requests;
        await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
        const complexity = page.getByRole('region', { name: 'Algorithm complexity' });
        assert((await complexity.textContent()).includes(approach === 'recursion' ? 'O(2^N)' : 'O(N)'), 'Wrong complexity');
        await page.getByRole('slider', { name: 'Seek to step' }).fill('2');
        assert(requests === afterRun, 'View or seek re-executed');
        await overflow(page);
        if (approach === 'memoization') {
          await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
          memoLink = page.url();
        }
        await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      }
      const priorLink = page.url();
      await page.getByLabel('Stairs', { exact: true }).fill('20');
      await selector.selectOption('recursion');
      await execute(page, () => page.getByRole('button', { name: 'Run recursion', exact: true }).click(), 'recursion', 400);
      const error = page.getByRole('alert', { name: 'This input could not run', exact: true });
      await error.waitFor();
      // Focus is a passive React effect after the error DOM commit, not part of the HTTP response.
      await page.waitForFunction(() => document.activeElement?.getAttribute('role') === 'alert', null, { timeout: 3000 });
      assert(await error.evaluate(e => e === document.activeElement), '400 did not focus summary');
      assert(page.url() === priorLink && await page.getByLabel('Stairs', { exact: true }).inputValue() === '20', 'Rejection changed link or clamped draft');
      await page.goto(memoLink);
      await ready(page, 'memoization');
      assert(await page.getByRole('slider', { name: 'Seek to step' }).inputValue() === '2', 'Link restored wrong step');
      assert((await page.getByRole('region', { name: 'Algorithm complexity' }).textContent()).includes('O(N)'), 'Link restored wrong Analysis metadata');
      await page.getByText('Memo table', { exact: true }).click();
      assert(await page.getByRole('table').count() === 1, 'Memo cache is not accessible');
      await overflow(page);
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();

      if ((width === 390 && theme === 'dark') || (width === 1366 && theme === 'light')) {
        for (const [approach, n] of [['recursion', 10], ['memoization', 30]]) {
          await selector.selectOption(approach);
          await page.getByLabel('Stairs', { exact: true }).fill(String(n));
          const action = page.getByRole('button', { name: `Run ${approach}`, exact: true });
          const run = await execute(page, () => action.count().then(count => count ? action.click()
            : page.getByRole('button', { name: 'Run input', exact: true }).click()), approach);
          await page.getByRole('slider', { name: 'Seek to step' }).fill(String(run.stepCount - 1));
          const tree = page.getByTestId('derived-recursion-tree');
          await tree.waitFor();
          assert(await tree.locator('g[data-state="current"]').count() === 0, 'Finished tree still claims an active call');
          const dimensions = await tree.evaluate(el => {
            const stage = el.parentElement;
            stage.scrollLeft = 0;
            const start = el.getBoundingClientRect().left - stage.getBoundingClientRect().left;
            stage.scrollLeft = stage.scrollWidth;
            const end = el.getBoundingClientRect().right - stage.getBoundingClientRect().right;
            const svg = el.querySelector('svg');
            return { start, end, svgWidth: svg.getBoundingClientRect().width, declaredWidth: Number(svg.getAttribute('width')),
              scrollWidth: stage.scrollWidth, clientWidth: stage.clientWidth, height: stage.getBoundingClientRect().height };
          });
          geometry.push({ width, theme, approach, n, ...dimensions });
          console.log(`Geometry ${width} ${approach}: ${JSON.stringify(dimensions)}`);
          assert(dimensions.start >= -1 && dimensions.end <= 20, `Tree ends unreachable: ${JSON.stringify(dimensions)}`);
          assert(dimensions.svgWidth === dimensions.declaredWidth && dimensions.scrollWidth > dimensions.clientWidth,
            `Wide tree squeezed/clipped instead of locally scrolling: ${JSON.stringify(dimensions)}`);
          assert(dimensions.height <= 480, 'Deep tree displaces the controls by more than 480px');
          await overflow(page);
        }
      }
      assert(errors.length === 0, `Browser errors: ${errors}`);
      if (width === 390 && theme === 'dark' || width === 1366 && theme === 'light') {
        await page.evaluate(() => {
          document.activeElement?.blur();
          window.scrollTo(0, 0);
          const tree = document.querySelector('[data-testid="derived-recursion-tree"]');
          const stage = tree?.parentElement;
          const root = tree?.querySelector('g rect');
          if (stage && root) {
            stage.scrollTop = 0;
            stage.scrollLeft += root.getBoundingClientRect().left - stage.getBoundingClientRect().left - stage.clientWidth / 2 + 44;
          }
        });
        await page.screenshot({ path: path.join(__dirname, `d3-${width}-${theme}.png`), fullPage: true });
      }
      rows.push({ width, theme, realBackend: true, selectorHeight: height, allThreeExecuted: true,
        selectionDoesNotExecute: true, sourceComplexityBound: true, viewSeekDoesNotExecute: true,
        rejectionPreservesRunLinkDraft: true, sharedApproachInputStepRestored: true, noOverflowOrErrors: true });
      console.log(`PASS ${width} ${theme}`);
      await page.close();
    }
    fs.writeFileSync(path.join(__dirname, 'd3-results.json'), JSON.stringify({ generated: new Date().toISOString(),
      engine: 'Chromium', rows, largestInputGeometry: geometry, scope: 'Climbing Stairs D3 pilot, not all DP rollout/human or cross-engine gates'
    }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
