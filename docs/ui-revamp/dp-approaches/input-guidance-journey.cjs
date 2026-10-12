// Real-backend regression for preserved input + approach-specific recovery, not a mock API.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const { chromium, firefox } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const output = path.join(__dirname, 'input-guidance');
const pace = require('../evidence/pace-executions.cjs')();
const original = [10, 9, 2, 5, 3, 7, 101, 18];
const widths = [[320, 568], [390, 844], [768, 1024], [1366, 768], [1440, 900]];
const identity = page => {
  const url = new URL(page.url());
  return ['input', 'approach', 'step'].map(key => url.searchParams.get(key));
};
const executing = (page, method) => page.waitForResponse(response =>
  new URL(response.url()).pathname.endsWith('/execute') && response.request().method() === method);
async function focused(page, locator) {
  await locator.waitFor();
  assert(await locator.evaluate(element => document.activeElement === element));
  await page.waitForFunction(() => {
    const heading = document.getElementById('try-input-title');
    const rail = document.querySelector('[data-tour="view-rail"]');
    return heading && rail && heading.getBoundingClientRect().top >= Math.max(0, rail.getBoundingClientRect().bottom);
  });
  const box = await locator.boundingBox();
  const rail = await page.locator('[data-tour="view-rail"]').boundingBox();
  assert(box.y >= Math.max(0, rail.y + rail.height), 'Focused editor heading is obscured by the sticky rail');
}
async function geometry(page) {
  assert(await page.evaluate(() => document.documentElement.scrollWidth <= innerWidth), 'Page-wide overflow');
  for (const name of ['Adjust input', 'Replace input with recursion example', 'Replace input with memoization example']) {
    const button = page.getByRole('button', { name, exact: true });
    if (!await button.isVisible()) continue;
    const box = await button.boundingBox();
    assert(box.height >= 44 && box.width >= 44, `${name}: target smaller than44px`);
    assert(box.x >= 0 && box.x + box.width <= page.viewportSize().width, `${name}: clipped action`);
  }
}

(async () => {
  fs.mkdirSync(output, { recursive: true });
  const servedBuild = await require('../evidence/served-build-identity.cjs')(base);
  const rows = [];
  const versions = {};
  for (const [engine, launcher] of Object.entries({ chromium, firefox })) {
    const browser = await launcher.launch();
    versions[engine] = browser.version();
    try {
      for (const [width, height] of widths) for (const theme of ['light', 'dark']) {
        const context = await browser.newContext({ viewport: { width, height }, colorScheme: theme });
        await context.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        const page = await context.newPage();
        await pace(page);
        const errors = [];
        let posts = 0;
        page.on('pageerror', error => errors.push(error.message));
        page.on('request', request => { if (new URL(request.url()).pathname.endsWith('/execute') && request.method() === 'POST') posts++; });
        try {
          const first = executing(page, 'GET');
          await page.goto(`${base}/problem/longest-increasing-subsequence`);
          const initial = await (await first).json();
          assert.deepEqual(initial.resolvedInput.nums, original);
          const select = page.getByRole('combobox', { name: 'Solution approach', exact: true });
          await select.waitFor();
          await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
          await select.selectOption('recursion');
          const notice = page.getByRole('status', { name: 'Input limits', exact: true });
          await notice.waitFor();
          assert((await notice.textContent()).includes('Array has 8 values; maximum 6.'));
          const description = await page.getByRole('button', { name: 'Run recursion', exact: true }).getAttribute('aria-describedby');
          assert.equal(description, await notice.getAttribute('id'));
          await geometry(page);
          await select.focus();
          await page.keyboard.press('Tab');
          assert(await page.getByRole('button', { name: 'Run recursion', exact: true }).evaluate(e => document.activeElement === e));
          await page.keyboard.press('Tab');
          const adjust = page.getByRole('button', { name: 'Adjust input', exact: true });
          assert(await adjust.evaluate(e => document.activeElement === e));
          assert(await adjust.evaluate(e => parseFloat(getComputedStyle(e).outlineWidth) >= 2), 'No visible keyboard focus ring');
          await page.keyboard.press('Enter');
          await focused(page, page.getByRole('heading', { name: 'Try your own input', exact: true }));
          assert.equal(await page.getByLabel('Position 8 value', { exact: true }).inputValue(), '18');
          assert.equal(posts, 0);
          await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
          const old = identity(page);
          const rejection = executing(page, 'POST');
          await page.getByRole('button', { name: 'Run recursion', exact: true }).click();
          const refused = await rejection;
          assert.equal(refused.status(), 400);
          const body = await refused.json();
          const failure = page.getByRole('alert', { name: 'Your input could not run', exact: true });
          await failure.waitFor();
          assert((await failure.textContent()).includes(`Array: ${body.fieldErrors.nums}`));
          assert.deepEqual(identity(page), old);
          assert((await page.getByRole('status', { name: 'Solution approach', exact: true }).textContent()).includes('showing: Tabulation'));
          await geometry(page);
          if (engine === 'chromium' && width === 320 && theme === 'light') {
            await notice.scrollIntoViewIfNeeded();
            await page.screenshot({ path: path.join(output, 'lis-input-guidance-320-light.png') });
            await failure.scrollIntoViewIfNeeded();
            await page.screenshot({ path: path.join(output, 'lis-code-rejection-320-light.png') });
          }
          await page.getByRole('tab', { name: 'Analysis', exact: true }).click();
          assert((await failure.textContent()).includes(`Array: ${body.fieldErrors.nums}`));
          const beforeExample = identity(page);
          await page.getByRole('button', { name: 'Replace input with recursion example', exact: true }).focus();
          await page.keyboard.press('Enter');
          await focused(page, page.getByRole('heading', { name: 'Try your own input', exact: true }));
          assert.equal(await page.getByLabel(/Position \d+ value/).count(), 6);
          assert.equal(posts, 1);
          assert.deepEqual(identity(page), beforeExample);
          assert.equal(await page.getByRole('alert', { name: 'This input could not run', exact: true }).count(), 0,
            'The rejected eight-value error was attached to the valid six-value example');
          const earlier = page.getByRole('status', { name: 'Earlier submission rejected', exact: true });
          await earlier.waitFor();
          assert((await earlier.textContent()).includes('The run shown is still the previous one.'));
          await page.getByText('Rejected submission details', { exact: true }).click();
          assert.equal(await page.getByLabel('Rejected input', { exact: true }).textContent(), JSON.stringify({ nums: original }, null, 2));
          await geometry(page);
          await page.getByText('Rejected submission details', { exact: true }).click();
          const accepted = executing(page, 'POST');
          await page.getByRole('button', { name: 'Run input', exact: true }).click();
          const response = await accepted;
          assert.equal(response.status(), 200);
          const run = await response.json();
          assert.equal(run.approachId, 'recursion');
          assert.deepEqual(run.resolvedInput.nums, original.slice(0, 6));
          assert.equal(run.truncated, false);
          // Independent LIS for this concrete example: 2,3,7 has length3.
          assert.equal(String(run.steps.at(-1).variables.answer), '3');
          await page.waitForFunction(() => new URL(location.href).searchParams.get('approach') === 'recursion');
          assert.equal(posts, 2);
          assert.equal(await notice.count(), 0);
          assert(await page.locator('#stage-title').evaluate(e => document.activeElement === e));
          await select.selectOption('memoization');
          assert.equal(await notice.count(), 0, 'A compatible memoized draft retains a stale recursive limit warning');
          assert.equal(posts, 2);
          let lateRejectionPreservesEditedDraftAndFocus = false;
          if ([320, 1366].includes(width) && theme === 'light') {
            await select.selectOption('canonical');
            await page.getByRole('button', { name: 'Restore defaults', exact: true }).click();
            await select.selectOption('recursion');
            const beforeLate = identity(page);
            let release, ready;
            const bodyReady = new Promise(resolve => { ready = resolve; });
            const match = url => url.pathname.endsWith('/execute') && url.searchParams.get('approach') === 'recursion';
            const handler = async route => {
              // Delay delivery of a genuine server400; do not synthesize its status/body.
              await new Promise(resolve => setTimeout(resolve, 1100));
              const actual = await route.fetch();
              assert.equal(actual.status(), 400);
              const wait = new Promise(resolve => { release = resolve; });
              ready();
              await wait;
              await route.fulfill({ response: actual });
            };
            await page.route(match, handler);
            const delayed = executing(page, 'POST');
            await page.getByRole('button', { name: 'Run input', exact: true }).click();
            await bodyReady;
            await page.getByRole('button', { name: 'Remove position 8', exact: true }).click();
            await page.getByRole('button', { name: 'Remove position 7', exact: true }).click();
            const input = page.getByLabel('Position 1 value', { exact: true });
            await input.fill('42');
            release();
            assert.equal((await delayed).status(), 400);
            await earlier.waitFor();
            assert.equal(await input.inputValue(), '42');
            assert(await input.evaluate(element => document.activeElement === element), 'Late rejected input stole focus from the new draft');
            assert.equal(await page.getByRole('alert', { name: 'This input could not run', exact: true }).count(), 0);
            assert.deepEqual(identity(page), beforeLate);
            await page.unroute(match, handler);
            lateRejectionPreservesEditedDraftAndFocus = true;
          }
          await geometry(page);
          assert.deepEqual(errors, []);
          rows.push({ problem: 'longest-increasing-subsequence', engine, width, height, theme,
            preservedEightValueDraft: true, preflightDeclaredLimit: true, exactServerReasonInCodeAndAnalysis: true,
            failedRunKeepsIdentity: true, explicitExampleOnlyEdits: true, keyboardTraversalRingAndRecoveryFocus: true,
            oldFieldsNotAttachedToExample: true, capturedRejectionStillExplained: true,
            lateRejectionPreservesEditedDraftAndFocus,
            realRecursiveAnswer: 3, targetsAndNoOverflow: true, executions: posts });
          console.log(`PASS LIS input guidance ${engine} ${width} ${theme}`);
        } finally { await context.close(); }
      }
      for (const width of [320, 1366]) {
        const context = await browser.newContext({ viewport: { width, height: 844 }, colorScheme: 'light' });
        await context.addInitScript(() => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify('light'));
        });
        const page = await context.newPage();
        await pace(page);
        try {
          const first = executing(page, 'GET');
          await page.goto(`${base}/problem/climbing-stairs`);
          await first;
          const detailResponse = await page.request.get(`${base}/api/problems/climbing-stairs`);
          const detail = await detailResponse.json();
          const recursive = detail.approaches.find(a => a.id === 'recursion');
          const field = recursive.inputSpec.fields[0];
          const excessive = field.constraints.max + 1;
          const select = page.getByRole('combobox', { name: 'Solution approach', exact: true });
          await select.waitFor();
          await select.selectOption('recursion');
          const input = page.getByLabel(field.label, { exact: true });
          await input.fill(String(excessive));
          const before = identity(page);
          const first400 = executing(page, 'POST');
          await page.getByRole('button', { name: 'Run recursion', exact: true }).click();
          assert.equal((await first400).status(), 400);
          const summary = page.getByRole('alert', { name: 'This input could not run', exact: true });
          await page.waitForFunction(() => document.activeElement?.getAttribute('role') === 'alert');
          assert(await summary.evaluate(e => document.activeElement === e));
          await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
          const playground = page.getByRole('tab', { name: 'Playground', exact: true });
          await playground.click();
          assert(await playground.evaluate(e => document.activeElement === e), 'Remounting the old error stole tab focus');
          await input.fill(String(field.defaultValue));
          await page.getByRole('status', { name: 'Earlier submission rejected', exact: true }).waitFor();
          await input.fill(String(excessive));
          await page.waitForFunction(() => document.querySelector('input[aria-invalid="true"]'));
          assert(await input.evaluate(e => document.activeElement === e), 'An old rejection stole focus when edited values reappeared');
          assert.deepEqual(identity(page), before);
          const second400 = executing(page, 'POST');
          await page.getByRole('button', { name: 'Run input', exact: true }).click();
          assert.equal((await second400).status(), 400);
          await page.waitForFunction(() => document.activeElement?.getAttribute('role') === 'alert');
          assert(await summary.evaluate(e => document.activeElement === e));
          assert.deepEqual(identity(page), before);
          let release, ready;
          const bodyReady = new Promise(resolve => { ready = resolve; });
          const match = url => url.pathname.endsWith('/climbing-stairs/execute') && url.searchParams.get('approach') === 'recursion';
          const handler = async route => {
            await new Promise(resolve => setTimeout(resolve, 1100));
            const response = await route.fetch();
            assert.equal(response.status(), 400);
            const wait = new Promise(resolve => { release = resolve; });
            ready();
            await wait;
            await route.fulfill({ response });
          };
          await page.route(match, handler);
          const focus400 = executing(page, 'POST');
          await page.getByRole('button', { name: 'Run input', exact: true }).click();
          await bodyReady;
          await page.getByRole('button', { name: 'Focus', exact: true }).click();
          release();
          assert.equal((await focus400).status(), 400);
          await page.getByRole('alert', { name: 'Your input could not run', exact: true }).waitFor();
          await page.getByRole('button', { name: 'Fix it in the editor', exact: true }).click();
          await focused(page, page.getByRole('heading', { name: 'Try your own input', exact: true }));
          assert.equal(await input.inputValue(), String(excessive));
          assert.deepEqual(identity(page), before);
          await page.unroute(match, handler);
          await geometry(page);
          rows.push({ problem: 'climbing-stairs', engine, width, theme: 'light',
            oldErrorReappearanceKeepsInputFocus: true, newRejectionFocusesSummary: true,
            editorRemountKeepsTabFocus: true, rejectionVisibleInFocusAndRecoveryRevealsEditor: true,
            noShareChange: true, targetsAndNoOverflow: true });
          console.log(`PASS validation focus identity ${engine} ${width}`);
        } finally { await context.close(); }
      }
      for (const tail of [null, 3]) {
        const context = await browser.newContext({ viewport: { width: 320, height: 844 }, colorScheme: 'light' });
        await context.addInitScript(() => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify('light'));
        });
        const page = await context.newPage();
        await pace(page);
        const errors = [];
        page.on('pageerror', error => errors.push(error.message));
        try {
          const input = { grid: [[1, 2], tail] };
          const encoded = Buffer.from(JSON.stringify(input), 'utf8').toString('base64url');
          const initial = executing(page, 'GET');
          const refused = executing(page, 'POST');
          await page.goto(`${base}/problem/ninja-and-his-friends?view=code&approach=recursion&input=${encoded}`);
          const previous = await (await initial).json();
          assert.equal((await refused).status(), 400);
          await page.getByRole('status', { name: 'Earlier submission rejected', exact: true }).waitFor();
          const captured = page.getByLabel('Rejected input', { exact: true });
          assert.equal(await captured.textContent(), JSON.stringify(input, null, 2));
          assert.equal(await captured.isVisible(), false, 'Rejected details did not start closed');
          await page.getByText('Rejected submission details', { exact: true }).click();
          assert.equal(await captured.isVisible(), true);
          assert.equal(await captured.textContent(), JSON.stringify(input, null, 2));
          assert((await page.getByRole('status', { name: 'Solution approach', exact: true }).textContent()).includes(previous.approachLabel));
          await page.getByRole('tab', { name: 'Playground', exact: true }).click();
          assert.equal(await page.getByLabel('Current grid input', { exact: true }).textContent(), JSON.stringify(input.grid, null, 2));
          const beforeRepair = identity(page);
          await page.getByRole('button', { name: 'Restore defaults', exact: true }).click();
          assert.equal(await page.getByLabel('Current grid input', { exact: true }).count(), 0);
          assert.equal(await page.getByRole('button', { name: /^Cell row/ }).count(), previous.resolvedInput.grid.length * previous.resolvedInput.grid[0].length);
          assert.deepEqual(identity(page), beforeRepair);
          await geometry(page);
          assert.deepEqual(errors, []);
          rows.push({ problem: 'ninja-and-his-friends', engine, width: 320, theme: 'light',
            malformedRejectedRow: tail, capturedJsonKeepsNullAndType: true,
            closedDetailsDoNotCrash: true, malformedEditorDoesNotCrashOrInventCells: true,
            explicitDefaultRepairOnlyEdits: true, previousApproachStillShown: true, noOverflowOrPageErrors: true });
          console.log(`PASS malformed rejected JSON ${engine} ${tail === null ? 'null' : 'number'}`);
        } finally { await context.close(); }
      }
      for (const width of [320, 1366]) for (const theme of ['light', 'dark']) {
        const context = await browser.newContext({ viewport: { width, height: 844 }, colorScheme: theme });
        await context.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true'); localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        const page = await context.newPage();
        await pace(page);
        try {
          const first = executing(page, 'GET');
          await page.goto(`${base}/problem/ninja-and-his-friends`);
          await first;
          const select = page.getByRole('combobox', { name: 'Solution approach', exact: true });
          await select.waitFor();
          await page.getByRole('button', { name: 'Edit input', exact: true }).click();
          for (let i = 0; i < 2; i++) await page.getByRole('button', { name: 'Add a column', exact: true }).click();
          const before = identity(page);
          await select.selectOption('memoization');
          const notice = page.getByRole('status', { name: 'Input limits', exact: true });
          await notice.waitFor();
          assert((await notice.textContent()).includes('Chocolate Grid has a row with 5 columns; maximum 4.'));
          await geometry(page);
          await page.getByRole('button', { name: 'Replace input with memoization example', exact: true }).click();
          await focused(page, page.getByRole('heading', { name: 'Try your own input', exact: true }));
          assert.equal(await notice.count(), 0);
          assert.deepEqual(identity(page), before);
          rows.push({ problem: 'ninja-and-his-friends', engine, width, theme,
            actualIntGridLimits: true, explicitExampleOnlyEdits: true, targetsAndNoOverflow: true });
          console.log(`PASS Ninja input guidance ${engine} ${width} ${theme}`);
        } finally { await context.close(); }
      }
    } finally { await browser.close(); }
  }
  fs.writeFileSync(path.join(output, 'browser-results.json'), JSON.stringify({ generated: new Date().toISOString(),
    realBackend: true, buildMode: 'production-preview', servedBuild, versions, rows }, null, 2) + '\n');
})().catch(error => { console.error(error); process.exitCode = 1; });
