const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const pace = require('../evidence/pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const zoom = Number(process.env.AUDIT_NATIVE_ZOOM || 2);
function assert(value, message) { if (!value) throw Error(message); }
async function focused(locator) { return locator.evaluate(e => document.activeElement === e); }
async function visibleFocus(locator) {
  return locator.evaluate(e => {
    const r = e.getBoundingClientRect();
    const style = getComputedStyle(e);
    const rail = document.querySelector('[role="tablist"]')?.getBoundingClientRect();
    return { top: r.top, bottom: r.bottom, railBottom: rail?.bottom ?? 0, viewport: innerHeight,
      outlineWidth: style.outlineWidth, outlineStyle: style.outlineStyle,
      centerUncovered: e.contains(document.elementFromPoint(r.x + r.width / 2, r.y + r.height / 2)) };
  });
}
(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = zoom === 1 ? await chromium.launch()
    : await require('../evidence/native-browser-zoom.cjs')(chromium);
  const rows = [];
  try {
    const widths = (process.env.AUDIT_WIDTHS || '640,1366').split(',').map(Number);
    const themes = (process.env.AUDIT_THEMES || 'light,dark').split(',');
    for (const width of widths) for (const theme of themes) {
      const page = await browser.newPage({ viewport: { width, height: 768 }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      await pace(page);
      const errors = [];
      page.on('pageerror', e => errors.push(e.message));
      await page.goto(`${base}/problem/climbing-stairs`);
      await page.getByRole('combobox', { name: 'Solution approach' }).waitFor();
      await page.waitForFunction(() => !document.querySelector('select')?.disabled);
      if (zoom !== 1) await browser.setZoom(page, zoom);
      await page.getByLabel('Stairs', { exact: true }).fill('7');
      await Promise.all([
        page.waitForResponse(r => r.request().method() === 'POST' && new URL(r.url()).pathname.endsWith('/execute')),
        page.getByRole('button', { name: 'Run input', exact: true }).click()
      ]);
      await page.waitForFunction(() => new URL(location.href).searchParams.has('input'));
      const main = page.getByRole('slider', { name: 'Seek to step', exact: true });
      await main.fill('2');
      await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
      const link = page.url();
      const open = page.getByRole('button', { name: 'Compare approaches', exact: true });
      await open.focus(); // Setup only: the opening and subsequent controls are native keyboard actions.
      await page.keyboard.press('Tab');
      await page.keyboard.press('Shift+Tab');
      assert(await focused(open), 'Comparison disclosure is not a reachable tab stop');
      await page.keyboard.press('Enter');
      const panel = page.getByRole('region', { name: 'Approach comparison', exact: true });
      const first = panel.getByLabel('Approach 1', { exact: true });
      const second = panel.getByLabel('Approach 2', { exact: true });
      const run = panel.getByRole('button', { name: 'Run comparison', exact: true });
      for (const control of [first, second, run]) {
        await page.keyboard.press('Tab');
        assert(await focused(control), 'Comparison controls are missing from native tab order');
      }
      const runFocus = await visibleFocus(run);
      assert(runFocus.centerUncovered && parseFloat(runFocus.outlineWidth) >= 2, `Hidden run focus: ${JSON.stringify(runFocus)}`);
      await page.keyboard.press('Enter');
      await panel.getByText('Both complete runs recorded the same answer.', { exact: true }).waitFor();
      assert(await focused(run), 'Running the comparison lost keyboard focus');
      const tab = panel.getByRole('slider', { name: 'Tabulation: step', exact: true });
      const recursion = panel.getByRole('slider', { name: 'Recursion: step', exact: true });
      await page.keyboard.press('Tab');
      assert(await focused(tab), 'Cannot reach first comparison slider after running');
      await page.keyboard.press('ArrowRight');
      assert(await tab.inputValue() === '2' && await recursion.inputValue() === '1'
        && await main.inputValue() === '2' && page.url() === link, 'Keyboard slider changed another run');
      await page.keyboard.press('Tab');
      assert(await focused(recursion), 'Second comparison slider missing from tab order');
      await page.keyboard.press('ArrowRight');
      assert(await recursion.inputValue() === '2' && await tab.inputValue() === '2', 'Second slider changed the first');
      const backwardFocus = [];
      // Exercise upward keyboard scrolling underneath the sticky rail, not only downward tabs.
      await page.evaluate(() => window.scrollTo(0, document.documentElement.scrollHeight));
      for (const control of [tab, run, second, first]) {
        await page.keyboard.press('Shift+Tab');
        assert(await focused(control), 'Backward comparison tab order changed');
        const focus = await visibleFocus(control);
        assert(focus.centerUncovered && focus.top >= focus.railBottom,
          `Sticky rail obscures backward keyboard focus: ${JSON.stringify(focus)}`);
        backwardFocus.push(focus);
      }
      await page.keyboard.press('Control+k');
      await page.getByRole('dialog').waitFor();
      await page.keyboard.press('Escape');
      assert(await focused(first), 'Switcher did not restore comparison focus');
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page overflow');
      assert(errors.length === 0, `Browser errors: ${errors}`);
      rows.push({ width, height: 768, theme, nativeZoom: zoom,
        cssViewport: await page.evaluate(() => ({ width: innerWidth, height: innerHeight, devicePixelRatio })),
        nativeActivationAndTabOrder: true, retainedRunFocus: true, independentSlidersAndMainLink: true,
        runFocus, backwardFocus, globalSwitcherAndFocusReturn: true, noOverflowOrErrors: true });
      console.log(`PASS keyboard ${width} ${theme} ${zoom * 100}%`);
      if (browser.closePage) await browser.closePage(page);
      else await page.close();
    }
    fs.writeFileSync(path.join(__dirname, `d4-keyboard-${zoom * 100}-results.json`), JSON.stringify({
      generated: new Date().toISOString(), engine: 'Chromium', buildMode: 'production-preview', servedBuild, rows,
      scope: 'Targeted D4 pilot native keyboard/focus checks; setup uses pointer/fill, not an end-to-end keyboard-only or screen-reader certification'
    }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
