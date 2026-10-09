const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const pace = require('../evidence/pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const assert = (value, message) => { if (!value) throw Error(message); };

(async () => {
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const browser = await chromium.launch();
  const rows = [];
  try {
    for (const config of [{ width: 320, theme: 'dark' }, { width: 1366, theme: 'light' }]) {
      for (const selected of [{ problemId: 'two-sum', approachId: 'canonical' },
        { problemId: 'climbing-stairs', approachId: 'memoization' }]) {
        const page = await browser.newPage({ viewport: { width: config.width, height: 900 }, colorScheme: config.theme });
        await page.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, config.theme);
        await pace(page);
        let releaseInitial;
        let markInitialReady;
        const initialReady = new Promise(resolve => { markInitialReady = resolve; });
        const isExecution = url => new URL(url).pathname.endsWith('/execute');
        const holdInitial = async route => {
          if (route.request().method() !== 'GET' || releaseInitial) return route.fallback();
          const response = await route.fetch();
          const gate = new Promise(resolve => { releaseInitial = resolve; });
          markInitialReady();
          await gate;
          await route.fulfill({ response });
        };
        await page.route(isExecution, holdInitial);
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        const responses = [];
        page.on('response', response => {
          if (response.request().method() === 'POST' && new URL(response.url()).pathname.endsWith('/execute')) {
            responses.push(response.json().then(data => ({ data, input: response.request().postDataJSON() })));
          }
        });
        const suffix = selected.approachId === 'canonical' ? '' : `?approach=${selected.approachId}`;
        await page.goto(`${base}/problem/${selected.problemId}${suffix}`);
        await initialReady;
        const inputToggle = page.getByRole('button', { name: 'Compare other case', exact: true });
        await inputToggle.waitFor();
        assert(await inputToggle.isDisabled(), 'Comparison offered before any executable committed');
        assert(responses.length === 0, 'Input comparison executed during initial pending load');
        releaseInitial();
        await page.unroute(isExecution, holdInitial);
        const seek = page.getByRole('slider', { name: 'Seek to step', exact: true });
        await seek.waitFor();
        if (selected.approachId !== 'canonical') {
          await page.waitForFunction(() => document.querySelector('[role="status"][aria-label="Solution approach"]')?.textContent.includes('Showing: Memoization'));
        }
        await seek.fill('2');
        await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
        const link = page.url();
        const before = responses.length;
        await page.getByRole('button', { name: 'Compare other case', exact: true }).click();
        const panel = page.getByRole('region', { name: 'Comparison with the other case', exact: true });
        await panel.getByRole('heading', { name: /^Other case/ }).waitFor();
        const actual = await Promise.all(responses.slice(before));
        assert(actual.length === 2, 'Comparison did not execute exactly two runs');
        assert(actual.every(({ data }) => data.problemId === selected.problemId && data.approachId === selected.approachId
          && data.truncated === false && data.steps.length > 0), 'Wrong returned executable or trace status');
        assert(actual.every(({ data, input }) => Object.entries(input).every(([key, value]) =>
          JSON.stringify(data.resolvedInput[key]) === JSON.stringify(value))), 'Returned input differs from submitted comparison');
        const localNext = panel.getByRole('button', { name: 'Other case: next step', exact: true });
        await localNext.focus();
        await page.keyboard.press('ArrowRight');
        await page.keyboard.press('l');
        await page.keyboard.press('End');
        assert(await seek.inputValue() === '2' && page.url() === link, 'Comparison keys moved or shared the main run');
        await localNext.click();
        assert(await panel.getByRole('slider', { name: 'Other case: step', exact: true }).inputValue() === '2', 'Local next did not move its run');
        assert(await panel.getByRole('slider', { name: 'Default input: step', exact: true }).inputValue() === '1', 'Comparison positions synchronized');
        await page.keyboard.press('Control+k');
        await page.getByRole('dialog').waitFor();
        await page.keyboard.press('Escape');
        await page.getByRole('button', { name: 'Hide comparison', exact: true }).click();

        // Fault injection modifies ONLY status/steps on genuine responses. This is not
        // evidence of a real backend budget breach or a valid fabricated algorithm run.
        let fault = 'truncated';
        await page.route(url => new URL(url).pathname.endsWith('/execute'), async route => {
          if (route.request().method() !== 'POST') return route.continue();
          const response = await route.fetch();
          const data = await response.json();
          await route.fulfill({ response, json: fault === 'truncated' ? { ...data, truncated: true } : { ...data, steps: [] } });
        });
        await page.getByRole('button', { name: 'Compare other case', exact: true }).click();
        await panel.getByRole('heading', { name: /^Other case/ }).waitFor();
        assert(await panel.getByText(/Incomplete: this trace was cut short/).count() === 2, 'Incomplete warnings missing');
        assert(await seek.inputValue() === '2' && page.url() === link, 'Incomplete comparison changed main session');
        await page.getByRole('button', { name: 'Hide comparison', exact: true }).click();
        fault = 'empty';
        await page.getByRole('button', { name: 'Compare other case', exact: true }).click();
        await panel.getByRole('alert').waitFor();
        assert(await panel.getByRole('heading', { name: /^Other case/ }).count() === 0, 'Empty trace displayed as a successful comparison');
        assert(await panel.getByRole('button', { name: /Retry/ }).isVisible(), 'No explicit retry after invalid trace');
        assert(await seek.inputValue() === '2' && page.url() === link, 'Invalid comparison changed main session');
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide overflow');
        assert(errors.length === 0, `Browser errors: ${errors}`);
        await Promise.all(responses);
        rows.push({ ...config, ...selected, productionPreview: true, realPairIdentityAndInputs: true,
          initialExecutableGuard: true,
          independentPositions: true, localKeyboardAndGlobalSwitcher: true, mainLinkAndStepRetained: true,
          injectedTruncationWarnings: true, injectedEmptyTraceRefused: true, noOverflowOrErrors: true });
        console.log(`PASS ${config.width} ${config.theme} ${selected.problemId}/${selected.approachId}`);
        await page.close();
      }
    }
    fs.writeFileSync(path.join(__dirname, 'd4-input-comparison-results.json'), JSON.stringify({ generated: new Date().toISOString(),
      engine: 'Chromium', servedBuild, rows, scope: 'Real canonical/memoized input comparisons on production preview; truncation and empty-trace checks are explicitly injected faults, not actual budget breaches.' }, null, 2) + '\n');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
