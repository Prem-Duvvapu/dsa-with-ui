// Independent real-response navigation/default ownership probe. Never invent a trace.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { pathToFileURL } = require('node:url');
const { root, sourceHash, packageSnapshot } = require('../evidence/paired-input/api.cjs');
const { oracle } = require('./lps-cases.cjs');
const id = 'longest-palindromic-subsequence', executePath = `/api/problems/${id}/execute`;
const picker = page => page.getByRole('combobox', { name: 'Solution approach', exact: true });
const seek = page => page.getByRole('slider', { name: 'Seek to step', exact: true });
const encode = input => Buffer.from(JSON.stringify(input)).toString('base64url');
const decode = value => value === null ? null : JSON.parse(Buffer.from(value, 'base64url').toString('utf8'));
const defaults = detail => Object.fromEntries(detail.inputSpec.fields.map(field => [field.name, field.defaultValue]));
function address(approach, input) {
  return `/problem/${id}?${new URLSearchParams({ approach, step: '3', ...(input === undefined ? {} : { input: encode(input) }) })}`;
}
async function until(check, label) {
  const end = Date.now() + 30000;
  while (!check()) { assert(Date.now() < end, `Timed out: ${label}`); await new Promise(resolve => setTimeout(resolve, 25)); }
}
async function ready(page) {
  await picker(page).waitFor();
  await page.waitForFunction(element => !element.disabled, await picker(page).elementHandle());
}
async function draft(page, expected) {
  await page.waitForFunction(input => {
    const label = [...document.querySelectorAll('label')].find(node => node.textContent === 'Text');
    return label && document.getElementById(label.htmlFor)?.value === input.text;
  }, expected);
}
async function echo(page, expected) {
  await page.waitForFunction(input => {
    const summary = [...document.querySelectorAll('[data-testid="input-summary"]')].find(node => node.checkVisibility());
    const values = Object.fromEntries([...(summary?.querySelectorAll('span[title]') ?? [])]
      .map(node => [node.previousElementSibling.textContent, node.title]));
    return values.text === input.text;
  }, expected);
}
async function push(page, target) {
  await page.evaluate(url => {
    const state = { idx: (history.state?.idx ?? 0) + 1, key: crypto.randomUUID(), usr: null };
    history.pushState(state, '', url);
    dispatchEvent(new PopStateEvent('popstate', { state }));
  }, target);
}
async function viewportShot(page, target, filename) {
  await target.waitFor();
  await target.evaluate(node => {
    node.scrollIntoView({ block: 'start', inline: 'nearest', behavior: 'instant' });
    const rail = document.querySelector('[data-tour="view-rail"]');
    const top = node.getBoundingClientRect().top, railBottom = rail?.getBoundingClientRect().bottom ?? 0;
    scrollBy({ top: top - Math.max(12, railBottom + 12), behavior: 'instant' });
  });
  await page.evaluate(() => new Promise(resolve => requestAnimationFrame(() => requestAnimationFrame(resolve))));
  const rectangles = await target.evaluate(node => {
    const rect = element => { if (!element) return null; const r = element.getBoundingClientRect();
      return { top: r.top, right: r.right, bottom: r.bottom, left: r.left, width: r.width, height: r.height }; };
    return { scope: 'viewport', viewport: { width: innerWidth, height: innerHeight }, scroll: { x: scrollX, y: scrollY },
      targetRect: rect(node), railRect: rect(document.querySelector('[data-tour="view-rail"]')),
      inputRects: [...document.querySelectorAll('#try-input input[type="text"]')].map(input => ({ id: input.id, value: input.value, rect: rect(input) })) };
  });
  assert(rectangles.targetRect.top >= (rectangles.railRect?.bottom ?? 0) - 1, 'Capture target obscured by sticky rail');
  await page.screenshot({ path: filename, fullPage: false }); return rectangles;
}
async function shownSource(page, raw, index, decodeTrace, onSource = null) {
  await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
  const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
  if (await sourceButton.isVisible()) await sourceButton.click();
  const source = page.getByRole('region', { name: 'Java source', exact: true }); await source.waitFor();
  const text = await source.locator(':scope > div').evaluateAll(nodes => nodes.map(node => node.lastElementChild.textContent).join('\n'));
  assert.equal(text.replace(/\r\n/g, '\n'), raw.code.replace(/\r\n/g, '\n'));
  const active = source.locator('[data-active-line="true"]'); await active.waitFor();
  assert.equal(await active.count(), 1);
  assert.equal(await active.evaluate(node => node.children[1].textContent), String(decodeTrace(raw)[index].activeLine));
  if (onSource) await onSource(source);
  await page.getByRole('tab', { name: 'Playground', exact: true }).click();
}
async function main() {
  const types = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
  const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180', backend = process.env.BACKEND_URL || 'http://127.0.0.1:8923';
  const out = process.env.LPS_NAVIGATION_OUTPUT_DIR || path.join(__dirname, 'lps'); fs.mkdirSync(out, { recursive: true });
  const localPackage = packageSnapshot(), servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const { decodeTrace } = await import(pathToFileURL(path.join(root, 'frontend/src/trace/decodeTrace.js')).href);
  const details = {};
  for (const approach of ['canonical', 'recursion']) {
    const response = await fetch(`${backend}/api/problems/${id}?approach=${approach}`); assert.equal(response.status, 200);
    details[approach] = await response.json(); assert.equal(details[approach].approachId, approach);
  }
  assert.deepEqual(defaults(details.canonical), { text: 'bbbab' });
  assert.deepEqual(defaults(details.recursion), { text: 'bbbab' });
  const catalogue = await (await fetch(`${backend}/api/problems`)).json();
  const summary = catalogue.find(problem => problem.id === id); assert(summary?.inputSpec); assert.equal(summary.approaches, undefined);
  let nextExecution = 0;
  function validate(raw, approach, input) {
    assert.equal(raw.problemId, id); assert.equal(raw.approachId, approach); assert.equal(raw.truncated, false);
    assert.equal(raw.code, details[approach].javaCode); assert.deepEqual(raw.resolvedInput, input);
    const steps = decodeTrace(raw); assert(steps.length >= 3); assert.equal(steps.at(-1).variables.answer, String(oracle(id, input)));
  }
  async function observe(page) {
    const state = { requests: [], holdNext: false, held: null, release: null };
    await page.route(url => url.pathname === executePath, async route => {
      const due = Math.max(Date.now(), nextExecution); nextExecution = due + 1100;
      if (due > Date.now()) await new Promise(resolve => setTimeout(resolve, due - Date.now()));
      const dispatchedAt = new Date().toISOString();
      const request = route.request(), response = await route.fetch(), raw = await response.json();
      state.requests.push({ dispatchedAt, method: request.method(), approach: new URL(request.url()).searchParams.get('approach') ?? 'canonical',
        input: request.method() === 'POST' ? request.postDataJSON() : null, status: response.status(),
        resolvedInput: raw.resolvedInput ?? null, sourceHash: raw.code ? sourceHash(raw.code) : null, stepCount: raw.stepCount ?? null });
      if (state.holdNext && request.method() === 'POST') {
        state.holdNext = false; state.held = raw;
        await new Promise(resolve => { state.release = resolve; });
      }
      await route.fulfill({ response }); // Exact real status, headers and body; no substituted data.
    });
    return state;
  }
  async function capture(page, action, approach, input, status = 200) {
    const pending = page.waitForResponse(response => new URL(response.url()).pathname === executePath && response.request().method() === 'POST');
    await action(); const response = await pending; assert.equal(response.status(), status);
    assert.equal(new URL(response.url()).searchParams.get('approach') ?? 'canonical', approach);
    assert.deepEqual(response.request().postDataJSON(), input);
    const raw = await response.json(); if (status === 200) validate(raw, approach, { ...defaults(details[approach]), ...input });
    else { assert.equal(typeof raw.fieldErrors?.text, 'string'); assert.equal(raw.steps, undefined); }
    await ready(page); return raw;
  }
  const rows = [], screenshots = [];
  for (const engine of ['chromium', 'firefox']) {
    const browser = await types[engine].launch();
    try {
      for (const [width, theme] of [[320, 'dark'], [1366, 'light']]) {
        const errors = [], page = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
        page.setDefaultTimeout(30000); page.on('pageerror', error => errors.push(error.message));
        await page.addInitScript(theme => { localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(theme)); }, theme);
        const observed = await observe(page);
        await page.goto(`${base}/problem/${id}`); await ready(page);
        await page.getByRole('button', { name: 'Edit input', exact: true }).click();
        const custom = { text: 'abc' };
        await page.getByLabel('Text', { exact: true }).fill(custom.text);
        await capture(page, () => page.getByRole('button', { name: 'Run input', exact: true }).click(), 'canonical', custom);
        await capture(page, () => push(page, address('recursion')), 'recursion', {});
        await draft(page, defaults(details.recursion)); await echo(page, defaults(details.recursion)); assert.equal(await seek(page).inputValue(), '2');
        const partial = await capture(page, () => push(page, address('recursion', {})), 'recursion', {});
        await draft(page, { text: 'bbbab' }); await echo(page, partial.resolvedInput);
        // Both forms share this example. Dirty the recursive draft first so a
        // canonical empty-map link must visibly retire its ownership and restore
        // the requested defaults, rather than passing because values already match.
        await page.getByLabel('Text', { exact: true }).fill('abcab');
        await draft(page, { text: 'abcab' }); await echo(page, partial.resolvedInput);
        const reverseDefaults = await capture(page, () => push(page, address('canonical', {})), 'canonical', {});
        await draft(page, defaults(details.canonical)); await echo(page, reverseDefaults.resolvedInput);
        assert.equal(await seek(page).inputValue(), '2');
        assert.deepEqual(decode(new URL(page.url()).searchParams.get('input')), {});
        const reverseViewRequests = observed.requests.length;
        await shownSource(page, reverseDefaults, 2, decodeTrace);
        assert.equal(observed.requests.length, reverseViewRequests, 'Inspecting canonical defaults re-executed the run');
        await capture(page, () => push(page, address('recursion', {})), 'recursion', {});
        await draft(page, defaults(details.recursion)); await echo(page, defaults(details.recursion));
        await capture(page, () => push(page, address('canonical', { text: 'cbd' })), 'canonical', { text: 'cbd' });
        await draft(page, { text: 'cbd' });
        await capture(page, () => page.goBack(), 'recursion', {}); await draft(page, { text: 'bbbab' });
        await capture(page, () => page.goForward(), 'canonical', { text: 'cbd' }); await draft(page, { text: 'cbd' });
        observed.holdNext = true;
        const heldResponse = page.waitForResponse(response => new URL(response.url()).pathname === executePath && response.request().method() === 'POST');
        await push(page, address('recursion', { text: 'abc' })); await until(() => observed.held !== null, 'actual upstream POST body held');
        validate(observed.held, 'recursion', { text: 'abc' });
        await draft(page, { text: 'abc' }); await echo(page, { text: 'cbd' });
        await page.getByLabel('Text', { exact: true }).fill('xyxy');
        observed.release(); const heldRaw = await (await heldResponse).json(); await ready(page);
        await draft(page, { text: 'xyxy' }); await echo(page, heldRaw.resolvedInput);
        assert.equal(await seek(page).inputValue(), '2');
        const editorFile = `${engine}-${width}-${theme}-navigation-editor.png`, sourceFile = `${engine}-${width}-${theme}-navigation-source.png`;
        screenshots.push({ file: editorFile, engine, width, theme, phase: 'editor-and-input-echo',
          ...await viewportShot(page, page.locator('[data-tour="input-summary"]:visible').first(), path.join(out, editorFile)),
          expectedDraft: { text: 'xyxy' }, expectedCommittedInput: heldRaw.resolvedInput, inspected: false });
        const beforeViews = observed.requests.length;
        await shownSource(page, heldRaw, 2, decodeTrace, async source => screenshots.push({ file: sourceFile, engine, width, theme,
          phase: 'committed-code-source', ...await viewportShot(page, source, path.join(out, sourceFile)),
          expectedSourceHash: sourceHash(heldRaw.code), inspected: false }));
        assert.equal(observed.requests.length, beforeViews);
        await capture(page, () => push(page, address('recursion', { text: 'abcdef' })), 'recursion', { text: 'abcdef' }, 400);
        await draft(page, { text: 'abcdef' }); await echo(page, heldRaw.resolvedInput);
        await page.waitForFunction(() => new URL(location.href).searchParams.get('step') === null);
        assert.deepEqual(decode(new URL(page.url()).searchParams.get('input')), { text: 'abc' });
        assert.equal(new URL(page.url()).searchParams.get('approach'), 'recursion');
        await shownSource(page, heldRaw, 0, decodeTrace); assert.equal(await seek(page).inputValue(), '0');
        assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        const warmRequests = observed.requests; assert.equal(warmRequests.length, 11); await page.close();

        const cold = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
        cold.setDefaultTimeout(30000); cold.on('pageerror', error => errors.push(error.message));
        await cold.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
        const coldObserved = await observe(cold); let detailHeld = false, releaseDetail;
        await cold.route(url => url.pathname === `/api/problems/${id}`, async route => {
          const response = await route.fetch(); assert.equal(response.status(), 200);
          await new Promise(resolve => { releaseDetail = resolve; detailHeld = true; });
          await route.fulfill({ response });
        });
        await cold.goto(base + address('recursion', {}));
        await until(() => detailHeld, 'actual detail response held');
        await echo(cold, { text: 'bbbab' });
        assert.equal(await cold.getByLabel('Text', { exact: true }).count(), 0, 'Unknown recursive limits borrowed canonical editor');
        releaseDetail(); await ready(cold); await draft(cold, { text: 'bbbab' });
        assert.equal(await cold.getByLabel('Text', { exact: true }).getAttribute('maxlength'), '5');
        await cold.getByLabel('Text', { exact: true }).fill('xyz');
        await draft(cold, { text: 'xyz' }); await echo(cold, { text: 'bbbab' });
        assert.equal(new URL(cold.url()).searchParams.get('approach'), 'recursion');
        assert.deepEqual(decode(new URL(cold.url()).searchParams.get('input')), {});
        assert.deepEqual(errors, []); assert(await cold.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        const coldPartialRequests = coldObserved.requests; assert.equal(coldPartialRequests.length, 2); await cold.close();

        // A real canonical form is editable from the summary before detail arrives.
        // Resetting that owned custom draft to an unresolved approach must clear the
        // old ownership, then allow only the newly known defaults to fill omissions.
        const reset = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
        reset.setDefaultTimeout(30000); reset.on('pageerror', error => errors.push(error.message));
        await reset.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
        const resetObserved = await observe(reset); let resetDetailHeld = false, releaseResetDetail;
        await reset.route(url => url.pathname === `/api/problems/${id}`, async route => {
          const response = await route.fetch(); assert.equal(response.status(), 200);
          await new Promise(resolve => { releaseResetDetail = resolve; resetDetailHeld = true; });
          await route.fulfill({ response });
        });
        await reset.goto(`${base}/problem/${id}`); await until(() => resetDetailHeld, 'canonical detail held');
        await draft(reset, defaults(details.canonical)); await echo(reset, defaults(details.canonical));
        await reset.getByLabel('Text', { exact: true }).fill(custom.text);
        const customPending = reset.waitForResponse(response => new URL(response.url()).pathname === executePath && response.request().method() === 'POST');
        await reset.getByRole('button', { name: 'Run input', exact: true }).click();
        const customResponse = await customPending; assert.equal(customResponse.status(), 200);
        assert.deepEqual(customResponse.request().postDataJSON(), custom); validate(await customResponse.json(), 'canonical', custom);
        await echo(reset, custom); resetObserved.holdNext = true;
        const resetPending = reset.waitForResponse(response => new URL(response.url()).pathname === executePath && response.request().method() === 'POST');
        await push(reset, address('recursion')); await until(() => resetObserved.held !== null, 'default recursion body held');
        validate(resetObserved.held, 'recursion', defaults(details.recursion));
        assert.equal(await reset.getByLabel('Text', { exact: true }).count(), 0);
        await echo(reset, custom); releaseResetDetail();
        await draft(reset, defaults(details.recursion));
        assert.equal(await reset.getByLabel('Text', { exact: true }).getAttribute('maxlength'), '5');
        await reset.getByLabel('Text', { exact: true }).fill('xyz');
        resetObserved.release(); const resetResponse = await resetPending; assert.equal(resetResponse.status(), 200);
        await ready(reset); await draft(reset, { text: 'xyz' }); await echo(reset, defaults(details.recursion));
        assert.equal(new URL(reset.url()).searchParams.get('input'), null); assert.equal(await seek(reset).inputValue(), '2');
        assert.equal(resetObserved.requests.length, 3);
        assert.deepEqual(errors, []); assert(await reset.evaluate(() => document.documentElement.scrollWidth <= innerWidth));
        rows.push({ problemId: id, engine, width, theme, passed: true, screenshots: [editorFile, sourceFile],
          warmRequests, coldPartialRequests, coldNoInputRequests: resetObserved.requests,
          bareRequestedDefaults: true, partialDefaultsBothDirections: true, reverseCanonicalEmptyMapResetsDirtyDraft: true, backForward: true,
          realPostBodyDelayPreservesLaterTextEdit: true, rejectedLinkKeepsCommittedIdentity: true,
          currentSourceAndStep: true, coldCanonicalSummaryCannotSupplyUnknownRecursiveEditor: true,
          coldDetailReleaseFillsOnlyRequestedDefaults: true, coldNoInputResetRetiresOldDraftBeforeLateEdits: true, noOverflowOrPageErrors: true });
        await reset.close(); console.log(`PASS independent LPS navigation ${engine}/${width}/${theme}`);
      }
    } finally { await browser.close(); }
  }
  assert.equal(rows.length, 4); assert.equal(screenshots.length, 8); assert.deepEqual(packageSnapshot(), localPackage);
  assert.deepEqual(await require('../evidence/served-build-identity.cjs')(base), servedBuild);
  fs.writeFileSync(path.join(out, 'navigation-results.json'), JSON.stringify({ generated: new Date().toISOString(), realBackend: true,
    buildMode: 'production-preview', servedBuild, localPackage, rows, screenshots,
    delayMechanism: 'Only real upstream response bodies are delayed; exact actual responses are fulfilled unchanged. All executions paced1100ms globally within this script.',
    limits: 'Four automated engine/viewport rows; not real-device, human-screen-reader, learner, performance or all-input acceptance.' }, null, 2));
}
if (require.main === module) main().catch(error => { console.error(error); process.exitCode = 1; });
