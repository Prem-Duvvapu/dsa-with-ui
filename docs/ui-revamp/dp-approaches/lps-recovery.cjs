const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { root, sourceHash, packageSnapshot } = require('../evidence/paired-input/api.cjs');
const { cases, oracle } = require('./lps-cases.cjs');
const id = cases[0].id, high = 1 - Number.EPSILON;
const picker = page => page.getByRole('combobox', { name: 'Solution approach', exact: true });
const seek = page => page.getByRole('slider', { name: 'Seek to step', exact: true });
async function readDraft(page) {
  return { text: await page.getByLabel('Text', { exact: true }).inputValue() };
}
async function ready(page) {
  await picker(page).waitFor(); await page.waitForFunction(element => !element.disabled, await picker(page).elementHandle());
}
async function echo(page, input) {
  const actual = await page.locator('[data-testid="input-summary"]:visible').first().locator('span[title]')
    .evaluateAll(elements => Object.fromEntries(elements.map(element => [element.previousElementSibling.textContent, element.title])));
  assert.deepEqual(actual, { text: input.text });
}
async function main() {
  const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright'), base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
  const out = path.join(__dirname, 'lps'), localPackage = packageSnapshot(); fs.mkdirSync(out, { recursive: true });
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  const pace = require('../evidence/pace-executions.cjs')(), rows = [];
  for (const engine of ['chromium', 'firefox']) {
    const browser = await types[engine].launch();
    try {
      for (const width of [320, 1366]) for (const theme of ['light', 'dark']) {
        const page = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
        page.setDefaultTimeout(30000); await pace(page);
        await page.addInitScript(value => { localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(value)); }, theme);
        const errors = []; let requests = 0;
        const execution = url => new URL(url).pathname === `/api/problems/${id}/execute`;
        page.on('pageerror', error => errors.push(error.message)); page.on('request', request => { if (execution(request.url())) requests++; });
        const initialPending = page.waitForResponse(response => execution(response.url()) && response.request().method() === 'POST'
          && new URL(response.url()).searchParams.get('approach') === 'recursion');
        await page.goto(`${base}/problem/${id}?approach=recursion`);
        const initial = await initialPending; assert.equal(initial.status(), 200);
        const initialRaw = await initial.json(); assert.deepEqual(initialRaw.resolvedInput, cases[0].defaults.recursion);
        assert.equal(initialRaw.approachId, 'recursion'); assert.equal(initialRaw.truncated, false); await ready(page);
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        assert.equal(await page.getByLabel('Text', { exact: true }).getAttribute('maxlength'), '5');
        assert.deepEqual(await readDraft(page), cases[0].defaults.recursion); await echo(page, cases[0].defaults.recursion);
        const before = requests, initialUrl = page.url();
        await page.evaluate(value => { window.__lpsEvidenceRandom = Math.random; Math.random = () => value; }, high);
        try { await page.getByRole('button', { name: 'Randomize', exact: true }).click(); }
        finally { await page.evaluate(() => { Math.random = window.__lpsEvidenceRandom; delete window.__lpsEvidenceRandom; }); }
        const generated = { text: 'zzzzz' }; assert.deepEqual(await readDraft(page), generated);
        assert.equal(requests, before); assert.equal(page.url(), initialUrl); await echo(page, cases[0].defaults.recursion);
        const pending = page.waitForResponse(response => execution(response.url()) && response.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const response = await pending; assert.equal(response.status(), 200); assert.deepEqual(response.request().postDataJSON(), generated);
        const raw = await response.json(), steps = decodeTrace(raw); assert.equal(steps.at(-1).variables.answer, String(oracle(id, generated)));
        assert.equal(raw.truncated, false); await ready(page); await page.waitForFunction(() => document.activeElement?.id === 'stage-title');
        await echo(page, generated); await seek(page).fill('2');
        await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === '3');
        const committed = page.url(), beforePrepare = requests;
        await picker(page).selectOption('memoization'); assert.equal(requests, beforePrepare); assert.equal(page.url(), committed);
        assert.equal(await page.getByLabel('Text', { exact: true }).getAttribute('maxlength'), '8');
        assert.equal(await page.getByRole('status', { name: 'Solution approach', exact: true }).textContent(), 'Selected: Memoization; showing: Recursion.');
        const longDraft = { text: 'abcdefgh' };
        await page.getByLabel('Text', { exact: true }).fill(longDraft.text);
        await picker(page).selectOption('recursion'); assert.equal(requests, beforePrepare);
        assert.equal(await page.getByLabel('Text', { exact: true }).getAttribute('maxlength'), '5');
        await page.getByRole('status', { name: 'Input limits', exact: true }).filter({ hasText: 'Input does not fit Recursion visualizer limits.' }).waitFor();
        assert.deepEqual(await readDraft(page), longDraft); await echo(page, generated); assert.equal(page.url(), committed);
        const rejectedPending = page.waitForResponse(result => execution(result.url()) && result.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const rejected = await rejectedPending; assert.equal(rejected.status(), 400); assert.deepEqual(rejected.request().postDataJSON(), longDraft);
        const failure = await rejected.json(); assert.equal(typeof failure.fieldErrors?.text, 'string'); assert.equal(failure.steps, undefined);
        await ready(page); const summary = page.getByRole('alert').filter({ hasText: 'This input could not run' }); await summary.waitFor();
        assert(await summary.evaluate(element => element === document.activeElement)); assert.deepEqual(await readDraft(page), longDraft);
        assert.equal(page.url(), committed); assert.equal(await seek(page).inputValue(), '2'); await echo(page, generated);
        const beforeViews = requests;
        await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
        const sourceButton = page.getByRole('button', { name: 'Source', exact: true }); if (await sourceButton.isVisible()) await sourceButton.click();
        const source = page.getByRole('region', { name: 'Java source', exact: true }); await source.waitFor();
        assert.equal((await source.locator(':scope > div').evaluateAll(elements => elements.map(element => element.lastElementChild.textContent).join('\n')))
          .replace(/\r\n/g, '\n'), raw.code.replace(/\r\n/g, '\n'));
        await source.focus(); await page.keyboard.press('ArrowRight'); assert.equal(await seek(page).inputValue(), '2');
        await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
        const stack = page.getByRole('region', { name: 'Call stack', exact: true }); await stack.waitFor();
        assert.deepEqual(await stack.locator('ol > li > code').allTextContents(), steps[2].callStack);
        await page.getByRole('tab', { name: 'Playground', exact: true }).click();
        assert.equal(requests, beforeViews); assert.deepEqual(await readDraft(page), longDraft);
        const beforeReplace = requests;
        await page.getByRole('button', { name: 'Replace input with recursion example', exact: true }).click();
        assert.deepEqual(await readDraft(page), cases[0].defaults.recursion); assert.equal(requests, beforeReplace);
        assert.equal(await seek(page).inputValue(), '2'); await echo(page, generated);
        const recoveredPending = page.waitForResponse(result => execution(result.url()) && result.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const recovered = await recoveredPending; assert.equal(recovered.status(), 200);
        const recoveryRaw = await recovered.json(); assert.deepEqual(recoveryRaw.resolvedInput, cases[0].defaults.recursion);
        assert.equal(decodeTrace(recoveryRaw).at(-1).variables.answer, String(oracle(id, cases[0].defaults.recursion))); assert.equal(recoveryRaw.truncated, false);
        await ready(page); await page.waitForFunction(() => document.activeElement?.id === 'stage-title');
        assert.deepEqual(errors, []); assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        rows.push({ problemId: id, engine, width, theme, requests, sourceHash: sourceHash(raw.code), passed: true,
          bareRecursiveLinkUsesOwnDefaults: true, actualRandomizeDraftOnly: true, generatedInputRunsAndShares: true,
          longRetainedStringWarning: true, noAutomaticTruncation: true, rejectedDraftRunStepAndLinkKept: true,
          nativeMaxLengthMatchesSelected: true,
          errorSummaryFocused: true, viewsAndSourceKeysPreserveRun: true, actualCallStackRegion: true,
          explicitOwnExampleDraftOnly: true, successfulRecovery: true, noOverflowOrErrors: true });
        await page.close(); console.log(`PASS LPS recovery ${engine}/${width}/${theme}`);
      }
    } finally { await browser.close(); }
  }
  assert.equal(rows.length, 8); assert.deepEqual(packageSnapshot(), localPackage);
  fs.writeFileSync(path.join(out, 'input-recovery-results.json'), JSON.stringify({ generated: new Date().toISOString(), realBackend: true,
    buildMode: 'production-preview', servedBuild, localPackage, rows,
    limits: 'Eight bounded automated string-input tasks. Not a human learner, real-device, screen-reader or performance acceptance gate.' }, null, 2));
}
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
