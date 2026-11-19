// Actual editor controls and production-preview assets against the packaged backend.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { root, owners, forms, fields, high, work, assertSpec, assertInput, assertRun, sourceHash, packageSnapshot } = require('./api.cjs');
const seek = page => page.getByRole('slider', { name: 'Seek to step', exact: true });

async function draft(page, spec) {
  const values = {};
  for (const field of spec.fields) {
    if (field.type === 'INT_ARRAY') values[field.name] = await page.getByRole('group', { name: field.label, exact: true })
      .getByRole('spinbutton', { name: /^Position \d+ value$/ }).evaluateAll(elements => elements.map(element => Number(element.value)));
    else values[field.name] = Number(await page.getByLabel(field.label, { exact: true }).inputValue());
  }
  return values;
}

async function echo(page, input, scalarName) {
  const summary = page.locator('[data-testid="input-summary"]:visible').first(); await summary.waitFor();
  const entries = summary.locator('span[title]');
  const expected = Object.fromEntries(Object.entries(input).map(([name, value]) => [name, Array.isArray(value) ? `[${value.join(', ')}]` : String(value)]));
  assert.deepEqual(await entries.evaluateAll(elements => Object.fromEntries(elements.map(element =>
    [element.previousElementSibling.textContent, element.getAttribute('title')]))), expected);
  const scalar = await entries.evaluateAll((elements, name) => {
    const element = elements.find(entry => entry.previousElementSibling.textContent === name);
    if (!element) return null;
    const rect = element.getBoundingClientRect(), range = document.createRange(); range.selectNodeContents(element);
    const text = range.getBoundingClientRect(); let unclipped = true;
    for (let parent = element.parentElement; parent; parent = parent.parentElement) {
      if (!/^(hidden|clip|auto|scroll)$/.test(getComputedStyle(parent).overflowX)) continue;
      const bounds = parent.getBoundingClientRect(), left = bounds.left + parent.clientLeft;
      if (text.left < left - 1 || text.right > left + parent.clientWidth + 1) unclipped = false;
    }
    return { text: element.textContent, fits: element.clientWidth > 0 && rect.height > 0 && element.scrollWidth <= element.clientWidth + 1
      && text.left >= rect.left - 1 && text.right <= rect.right + 1 && unclipped };
  }, scalarName);
  assert.deepEqual(scalar, { text: String(input[scalarName]), fits: true }, 'The short amount/target echo must be readable');
}

async function noOverflow(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide horizontal overflow');
}

async function screenshot(page, target, file) {
  await target.waitFor(); await target.evaluate(element => element.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'instant' }));
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  const metadata = await target.evaluate(element => {
    const rect = element.getBoundingClientRect();
    return { scope: 'viewport', scroll: { x: scrollX, y: scrollY }, viewport: { width: innerWidth, height: innerHeight },
      targetRect: { top: rect.top, right: rect.right, bottom: rect.bottom, left: rect.left } };
  });
  await page.screenshot({ path: file, fullPage: false }); return metadata;
}

async function randomize(page) {
  await page.evaluate(value => { window.__coinSubsetEvidenceRandom = Math.random; Math.random = () => value; }, high);
  try { await page.getByRole('button', { name: 'Randomize', exact: true }).click(); }
  finally { await page.evaluate(() => { Math.random = window.__coinSubsetEvidenceRandom; delete window.__coinSubsetEvidenceRandom; }); }
}

async function main() {
  const engine = process.env.AUDIT_ENGINE || 'chromium'; assert(['chromium', 'firefox'].includes(engine));
  const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
  const out = path.resolve(process.env.COIN_SUBSET_EVIDENCE_DIR || __dirname);
  const localPackage = packageSnapshot(), servedBuild = await require('../served-build-identity.cjs')(base);
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  const samplerFile = path.join(root, 'frontend/src/input/randomizeInput.js');
  const { randomizeInput } = await import(pathToFileURL(samplerFile).href);
  const samplerHash = sourceHash(fs.readFileSync(samplerFile)), details = new Map();
  for (const id of owners) for (const form of forms(id)) {
    const response = await fetch(`${base}/api/problems/${id}?approach=${form}`); assert.equal(response.status, 200);
    const detail = await response.json(); assertSpec(id, form, detail.inputSpec); details.set(`${id}/${form}`, detail);
  }
  const pace = require('../pace-executions.cjs')(), browser = await types[engine].launch();
  const rows = [], screenshots = []; fs.mkdirSync(out, { recursive: true });
  try {
    for (const id of owners) for (const form of forms(id)) for (const [width, theme] of [[320, 'dark'], [1366, 'light']]) {
      const detail = details.get(`${id}/${form}`), spec = detail.inputSpec, rule = spec.workLimits[0];
      const [arrayName, scalarName] = fields(id), expected = randomizeInput(spec, () => high);
      const defaults = Object.fromEntries(spec.fields.map(field => [field.name, field.defaultValue]));
      const page = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
      page.setDefaultTimeout(30000); await pace(page);
      await page.addInitScript(value => { localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(value)); }, theme);
      const errors = []; let executions = 0;
      const route = url => new URL(url).pathname === `/api/problems/${id}/execute`;
      page.on('pageerror', error => errors.push(error.message)); page.on('request', request => { if (route(request.url())) executions++; });
      const initialPending = page.waitForResponse(response => route(response.url()) && response.request().method() === 'POST'
        && new URL(response.url()).searchParams.get('approach') === form);
      await page.goto(`${base}/problem/${id}?approach=${form}`);
      const initial = await initialPending; assert.equal(initial.status(), 200);
      const initialRaw = await initial.json(); assertRun(initialRaw, decodeTrace(initialRaw), detail, defaults);
      await seek(page).waitFor();
      const picker = page.getByRole('combobox', { name: 'Solution approach', exact: true });
      await picker.waitFor(); await page.waitForFunction(element => !element.disabled, await picker.elementHandle());
      assert.equal(await picker.inputValue(), form);
      const initialLabel = detail.approaches.find(approach => approach.id === form).label;
      assert.equal(await page.getByRole('status', { name: 'Solution approach', exact: true }).textContent(), `Showing: ${initialLabel}.`);
      await echo(page, defaults, scalarName);
      await page.getByRole('button', { name: 'Edit input', exact: true }).click(); assert.deepEqual(await draft(page, spec), defaults);
      const before = executions, beforeUrl = page.url(), beforeStep = await seek(page).inputValue();
      await randomize(page); const generated = await draft(page, spec); assert.deepEqual(generated, expected); assertInput(spec, generated);
      assert.notDeepEqual(generated, defaults); assert.equal(executions, before); assert.equal(page.url(), beforeUrl);
      assert.equal(await seek(page).inputValue(), beforeStep); await echo(page, defaults, scalarName); await noOverflow(page);
      const selected = id === 'coin-change-2' && form === 'canonical', rowScreenshots = [];
      if (selected) {
        const file = `${engine}-${width}-${theme}-coin-draft.png`;
        const metadata = await screenshot(page, page.getByRole('region', { name: 'Try your own input', exact: true }), path.join(out, file));
        rowScreenshots.push(file); screenshots.push({ file, id, form, width, theme, phase: 'draft', ...metadata,
          expected: 'Eight distinct actual denominations, amount24; no run or sharing until explicit submission.' });
      }
      const pending = page.waitForResponse(response => route(response.url()) && response.request().method() === 'POST');
      await page.getByRole('button', { name: 'Run input', exact: true }).click(); const response = await pending; assert.equal(response.status(), 200);
      assert.deepEqual(response.request().postDataJSON(), generated);
      const raw = await response.json(), steps = decodeTrace(raw), answer = assertRun(raw, steps, detail, generated);
      await page.waitForFunction(() => document.activeElement?.id === 'stage-title'); assert.equal(await seek(page).inputValue(), '0');
      await echo(page, generated, scalarName); assert.deepEqual(await draft(page, spec), generated);
      await page.waitForFunction(() => new URL(location.href).searchParams.get('input') !== null);
      const shared = new URL(page.url());
      assert.deepEqual(JSON.parse(Buffer.from(shared.searchParams.get('input'), 'base64url').toString('utf8')), generated);
      assert.equal(shared.searchParams.get('approach'), form === 'canonical' ? null : form); assert.equal(executions, before + 1);
      await seek(page).fill(String(steps.length - 1));
      await page.waitForFunction(last => new URL(location.href).searchParams.get('step') === String(last), steps.length);
      if (selected) {
        const file = `${engine}-${width}-${theme}-coin-run.png`;
        const metadata = await screenshot(page, page.locator('[data-testid="input-summary"]:visible').first(), path.join(out, file));
        rowScreenshots.push(file); screenshots.push({ file, id, form, width, theme, phase: 'complete-run', ...metadata,
          expected: 'Successful original distinct coin input; readable amount24; actual complete answer.' });
      }
      const retainedUrl = page.url(), candidate = form === 'canonical' ? 'memoization' : 'canonical';
      await page.getByRole('combobox', { name: 'Solution approach', exact: true }).selectOption(candidate);
      await page.getByRole('status', { name: 'Solution approach', exact: true }).filter({ hasText: 'Selected:' }).waitFor();
      assert.equal(executions, before + 1); assert.equal(page.url(), retainedUrl); assert.deepEqual(await draft(page, spec), generated);
      await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
      const sourceButton = page.getByRole('button', { name: 'Source', exact: true }); if (await sourceButton.isVisible()) await sourceButton.click();
      const source = page.getByRole('region', { name: 'Java source', exact: true }); await source.waitFor();
      assert.equal((await source.locator(':scope > div').evaluateAll(elements => elements.map(element => element.lastElementChild.textContent).join('\n')))
        .replace(/\r\n/g, '\n'), raw.code.replace(/\r\n/g, '\n'));
      const active = source.locator('[data-active-line="true"]'); await active.waitFor(); assert.equal(await active.count(), 1);
      assert.equal(await active.evaluate(element => element.children[1].textContent), String(steps.at(-1).activeLine)); await noOverflow(page);
      await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
      const variables = page.getByRole('region', { name: 'Variables', exact: true }); await variables.waitFor();
      const answerRow = variables.locator('dl > div').filter({ has: page.getByText('answer', { exact: true }) });
      assert.equal(await answerRow.locator('dd > code').textContent(), String(answer.expected)); await noOverflow(page);
      await page.getByRole('tab', { name: 'Playground', exact: true }).click();
      await page.getByRole('combobox', { name: 'Solution approach', exact: true }).selectOption(form);
      assert.equal(await seek(page).inputValue(), String(steps.length - 1)); await echo(page, generated, scalarName);
      assert.equal(executions, before + 1);
      let errorRecovery = null;
      if (selected && width === 320) {
        const scalar = spec.fields.find(field => field.name === scalarName);
        const bad = { ...generated, [scalarName]: 25 };
        await page.getByLabel(scalar.label, { exact: true }).fill('25');
        const previousUrl = page.url(), rejectedPending = page.waitForResponse(result => route(result.url()) && result.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const rejected = await rejectedPending; assert.equal(rejected.status(), 400); assert.deepEqual(rejected.request().postDataJSON(), bad);
        assert.equal(typeof (await rejected.json()).fieldErrors?.[scalarName], 'string');
        const summary = page.getByRole('alert').filter({ hasText: 'This input could not run' }); await summary.waitFor();
        await page.waitForFunction(() => document.activeElement?.getAttribute('role') === 'alert'
          && document.activeElement.textContent.includes('This input could not run'));
        assert.equal(page.url(), previousUrl); assert.equal(await seek(page).inputValue(), String(steps.length - 1));
        await echo(page, generated, scalarName); assert.deepEqual(await draft(page, spec), bad);
        const refusedCount = executions; await randomize(page); assert.equal(executions, refusedCount);
        assert.deepEqual(await draft(page, spec), generated); await echo(page, generated, scalarName);
        const recoveryPending = page.waitForResponse(result => route(result.url()) && result.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const recovery = await recoveryPending; assert.equal(recovery.status(), 200);
        const recovered = await recovery.json(); assertRun(recovered, decodeTrace(recovered), detail, generated);
        await page.waitForFunction(() => document.activeElement?.id === 'stage-title'); assert.equal(await seek(page).inputValue(), '0');
        errorRecovery = { overBudget400: true, rejectedDraftKept: true, priorRunStepEchoAndUrlKept: true, errorSummaryFocused: true,
          randomizedDraftOnly: true, successfulRunRecovery: true };
      }
      assert.deepEqual(errors, []); await noOverflow(page);
      rows.push({ engine, id, form, width, height: width === 320 ? 568 : 768, theme, input: generated, arrayField: arrayName,
        work: work(rule, generated), limit: rule.limit, steps: steps.length, ...answer, executions, sourceHash: sourceHash(raw.code),
        screenshots: rowScreenshots, actualApproachUrlAndRandomize: true, draftOnlyUntilRun: true, priorStepInputAndUrlPreserved: true,
        completeUntruncatedPost: true, stageHeadingFocused: true, actualSourceAndHighlight: true, updatedEchoAndShareInput: true,
        shortScalarActuallyReadable: true, approachSelectionPreservesShownRun: true, viewChangesPreserveStepWithoutExecution: true,
        errorRecovery, noPageOverflowOrErrors: true, passed: true });
      await page.close(); console.log(`PASS coin/subset browser ${engine}/${id}/${form}/${width}/${theme}`);
    }
    assert.equal(rows.length, 20); assert.equal(screenshots.length, 4); assert.deepEqual(packageSnapshot(), localPackage);
    assert.equal(sourceHash(fs.readFileSync(samplerFile)), samplerHash);
    fs.writeFileSync(path.join(out, `${engine}-results.json`), JSON.stringify({ generated: new Date().toISOString(), realBackend: true,
      complete: true, buildMode: 'production-preview', engine, browserVersion: browser.version(), servedBuild, localPackage, samplerHash, rows, screenshots,
      limits: 'Ten coin/direct-subset specifications with actual high-RNG editor controls on320dark/1366light; one manual400/recovery per engine. Four viewport captures require visual inspection. Not derived-target/parity budgets, all-input, source compilation, real devices, screen readers, native zoom, performance, public rollout or whole-project acceptance.' }, null, 2));
  } finally { await browser.close(); }
}
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
