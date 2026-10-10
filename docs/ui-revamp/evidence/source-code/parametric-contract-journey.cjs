// Production-preview / real-backend check of paired ranges and rejected shared reruns.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const { pathToFileURL } = require('url');
const { isDeepStrictEqual } = require('util');
const root = path.resolve(__dirname, '../../../..');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const out = process.argv[2] || '/tmp/parametric-browser';
const pace = require('../pace-executions.cjs')();
const ids = fs.readdirSync(path.join(root, 'backend/src/main/resources/solutions/core'))
  .filter(name => name.endsWith('.java')).map(name => name.slice(0, -5)).filter(id =>
    JSON.parse(fs.readFileSync(path.join(root, `backend/src/test/resources/golden/${id}.json`), 'utf8')).steps[0].dsType === 'SearchSpace').sort();
const assert = (truth, message) => { if (!truth) throw Error(message); };
(async () => {
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  fs.mkdirSync(out, { recursive: true });
  const browser = await chromium.launch();
  const rows = [];
  const customRows = [];
  try {
    for (const theme of ['light', 'dark']) for (const width of [320, 390, 1366]) {
      const context = await browser.newContext({ viewport: { width, height: width === 320 ? 568 : width === 390 ? 844 : 768 },
        colorScheme: theme, reducedMotion: 'reduce' });
      await context.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const page = await context.newPage();
      await pace(page);
      let errors = [];
      page.on('pageerror', error => errors.push(error.message));
      for (const id of ids) {
        errors = [];
        const pending = page.waitForResponse(response => new URL(response.url()).pathname === `/api/problems/${id}/execute`);
        await page.goto(`${base}/problem/${id}`);
        const response = await pending;
        assert(response.ok(), `${id}: HTTP ${response.status()}`);
        const run = await response.json();
        const steps = decodeTrace(run);
        assert(!run.truncated && run.steps.length > 1, `${id}: incomplete run`);
        const slider = page.getByRole('slider', { name: 'Seek to step' });
        await slider.waitFor();
        const inspected = [0, Math.floor(steps.length / 2), steps.length - 1];
        for (const index of inspected) {
          await slider.fill(String(index));
          const vars = steps[index].variables;
          const low = Number(vars.low), high = Number(vars.high);
          assert(Number.isFinite(low) && Number.isFinite(high), `${id}: missing paired bounds at ${index}`);
          const range = page.getByTestId('search-range');
          await range.filter({ hasText: `[${low}, ${high}]` }).waitFor();
          assert((await range.textContent()).includes(`${Math.max(0, high - low + 1)} left`), `${id}: wrong remaining count`);
          if (vars.mid !== undefined) assert(await page.getByTestId('search-mid').textContent() === `probing ${vars.mid}`, `${id}: wrong probe`);
          else assert(await page.getByTestId('search-mid').count() === 0, `${id}: stale probe`);
        }
        const final = steps.at(-1).variables;
        assert(Number(final.low) > Number(final.high), `${id}: final interval not exhausted`);
        assert(await page.getByTestId('search-live').count() === 0, `${id}: exhausted range still drawn live`);
        assert(await page.getByTestId('search-result').textContent() === `Result: ${final.answer}`, `${id}: result mismatch`);
        assert(await page.getByRole('group', { name: 'End of the run' }).count() === 1, `${id}: completed run not acknowledged`);
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${id}: page overflow`);
        assert(errors.length === 0, `${id}: ${errors.join(', ')}`);
        rows.push({ id, width, theme, inspected: inspected.map(i => i + 1), answer: final.answer, range: [final.low, final.high], passed: true });
        if (id === 'book-allocation' && [320, 1366].includes(width)) {
          // Seeking focuses the slider and scrolls phones down to playback. Capture the
          // actual range/result, not a viewport containing only those controls.
          await page.getByTestId('search-range').scrollIntoViewIfNeeded();
          await page.waitForTimeout(80);
          await page.screenshot({ path: path.join(out, `range-${width}-${theme}.png`) });
        }
        console.log(`PASS range ${id} ${width} ${theme}`);
      }
      await context.close();
    }
    const context = await browser.newContext({ viewport: { width: 390, height: 844 }, colorScheme: 'light' });
    await context.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
    const page = await context.newPage();
    await pace(page);
    const cases = [
      { id: 'smallest-divisor', input: { nums: [1, 2], threshold: 2 }, answer: '2', field: 'threshold' },
      { id: 'matrix-median', input: { matrix: [[1, 2]] }, answer: '1', field: 'matrix' }
    ];
    for (const test of cases) {
      const endpoint = response => new URL(response.url()).pathname === `/api/problems/${test.id}/execute`;
      const restored = page.waitForResponse(response => endpoint(response) && response.request().method() === 'POST');
      const shared = Buffer.from(JSON.stringify(test.input)).toString('base64url');
      await page.goto(`${base}/problem/${test.id}?input=${shared}&step=3&view=code`);
      const response = await restored;
      assert(response.ok(), `${test.id}: shared run HTTP ${response.status()}`);
      const run = await response.json();
      assert(isDeepStrictEqual(run.resolvedInput, test.input), `${test.id}: wrong shared input`);
      assert(decodeTrace(run).at(-1).variables.answer === test.answer, `${test.id}: wrong independent answer`);
      const slider = page.getByRole('slider', { name: 'Seek to step' });
      await page.waitForFunction(() => document.querySelector('input[aria-label="Seek to step"]')?.value === '2');
      const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
      if (await sourceButton.isVisible()) await sourceButton.click();
      const source = page.getByRole('region', { name: 'Java source', exact: true });
      const beforeSource = await source.textContent();
      const beforeSearch = new URL(page.url()).search;
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      await page.getByRole('button', { name: 'Edit input', exact: true }).click();
      if (test.field === 'threshold') await page.getByLabel('Threshold', { exact: true }).fill('1');
      else {
        await page.getByRole('button', { name: 'Cell row 1, column 1, value 1', exact: true }).click();
        await page.getByRole('button', { name: 'Cell row 1, column 1, value 2', exact: true }).click();
      }
      const rejected = page.waitForResponse(endpoint);
      await page.getByRole('button', { name: 'Run input', exact: true }).click();
      const invalid = await rejected;
      assert(invalid.status() === 400, `${test.id}: invalid run HTTP ${invalid.status()}`);
      const body = await invalid.json();
      assert(typeof body.fieldErrors?.[test.field] === 'string', `${test.id}: missing field error`);
      const alert = page.getByRole('alert').filter({ hasText: 'This input could not run' });
      await alert.waitFor();
      assert(await alert.evaluate(element => element === document.activeElement), `${test.id}: missing error focus`);
      const current = new URL(page.url()).searchParams;
      const previous = new URLSearchParams(beforeSearch);
      assert(current.get('input') === previous.get('input') && current.get('step') === previous.get('step'), `${test.id}: failed rerun changed share link`);
      assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), `${test.id}: editor overflow`);
      await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
      if (await sourceButton.isVisible()) await sourceButton.click();
      assert(await source.textContent() === beforeSource && await slider.inputValue() === '2', `${test.id}: rejected run changed prior source/step`);
      customRows.push({ id: test.id, input: test.input, answer: test.answer, rejectedField: test.field, passed: true,
        checks: ['shared input restored before step3', 'independent answer', '400 field error', 'error focus', 'prior source/step/shared input retained', 'no phone overflow'] });
      console.log(`PASS shared/rejected ${test.id}`);
    }
    await context.close();
    fs.writeFileSync(path.join(out, 'contract-results.json'), JSON.stringify({ generated: new Date().toISOString(),
      browser: 'Chromium', browserVersion: browser.version(), realBackend: true, rows, customRows, complete: true }, null, 2));
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
