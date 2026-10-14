// Real-backend malformed shared links -> editor recovery -> Randomize -> source/reload continuity.
const playwright = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const { createHash } = require('crypto');
const { isDeepStrictEqual } = require('util');
const servedBuildIdentity = require('../served-build-identity.cjs');
const pace = require('../pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const engine = process.env.AUDIT_ENGINE || 'chromium';
if (!['chromium', 'firefox'].includes(engine)) throw Error('Unknown browser engine');
const out = process.argv[2];
if (!out) throw Error('Provide a separate evidence output directory');

// Independent recursive-descent syntax tree, not stack conversion or reversal.
function answer(text, prefix) {
  let cursor = 0;
  const combine = (op, left, right) => prefix ? op + left + right : left + right + op;
  function atom() {
    const token = text[cursor++];
    if (/^[A-Z]$/.test(token ?? '')) return token;
    if (token !== '(') throw Error('Randomize produced an invalid operand');
    const result = sum();
    if (text[cursor++] !== ')') throw Error('Randomize produced an unbalanced group');
    return result;
  }
  function power() {
    const left = atom();
    if (text[cursor] !== '^') return left;
    cursor++;
    return combine('^', left, power());
  }
  function product() {
    let result = power();
    while ('*/'.includes(text[cursor] ?? '\0')) {
      const op = text[cursor++];
      result = combine(op, result, power());
    }
    return result;
  }
  function sum() {
    let result = product();
    while ('+-'.includes(text[cursor] ?? '\0')) {
      const op = text[cursor++];
      result = combine(op, result, product());
    }
    return result;
  }
  const result = sum();
  if (cursor !== text.length) throw Error('Randomize left trailing invalid input');
  return result;
}

(async () => {
  const servedBuild = await servedBuildIdentity(base);
  const browser = await playwright[engine].launch();
  const rows = [];
  const save = complete => {
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, 'session-results.json'), JSON.stringify({
      generated: new Date().toISOString(), engine, browserVersion: browser.version(),
      realBackend: true, servedBuild, complete, rows
    }, null, 2));
  };
  try {
    for (const theme of ['light', 'dark']) for (const width of [320, 1366]) {
      for (const id of ['infix-to-postfix', 'infix-to-prefix']) {
        const context = await browser.newContext({ viewport: { width, height: width === 320 ? 568 : 768 },
          colorScheme: theme, reducedMotion: 'reduce' });
        await context.addInitScript(theme => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
        }, theme);
        const page = await context.newPage();
        await pace(page);
        const errors = [];
        let executions = 0;
        const endpoint = response => new URL(response.url()).pathname === `/api/problems/${id}/execute`;
        page.on('pageerror', error => errors.push(error.message));
        page.on('request', request => { if (endpoint(request)) executions++; });
        const originalResponse = page.waitForResponse(response => endpoint(response) && response.request().method() === 'GET');
        const rejectedResponse = page.waitForResponse(response => endpoint(response) && response.request().method() === 'POST');
        const requested = Buffer.from(JSON.stringify({ expression: 'AB' })).toString('base64url');
        await page.goto(`${base}/problem/${id}?view=analysis&input=${requested}&step=3`);
        const original = await (await originalResponse).json();
        const rejection = await rejectedResponse;
        if (rejection.status() !== 400 || !(await rejection.json()).fieldErrors?.expression) {
          throw Error('Malformed shared expression was not rejected with a field error');
        }
        const notice = page.getByRole('status', { name: 'Shared link', exact: true });
        await notice.waitFor();
        const copy = await notice.textContent();
        if (!copy.includes('server rejected it') || !copy.includes('Showing the default input')
            || !copy.includes('playback starts at step 1')) throw Error('Rejected link identity was not explained');
        const seek = page.getByRole('slider', { name: 'Seek to step' });
        if (await seek.inputValue() !== '0') throw Error('Rejected link kept the requested run step');
        if (new URL(page.url()).searchParams.has('input') || new URL(page.url()).searchParams.has('step')) {
          throw Error('Rejected link input/step were not removed');
        }
        await seek.fill('1');
        await page.waitForURL(url => url.searchParams.get('step') === '2');
        await page.getByRole('button', { name: 'Fix it in the editor', exact: true }).click();
        const field = page.getByLabel('Infix expression', { exact: true });
        if (await field.inputValue() !== 'AB') throw Error('Rejected shared draft was lost');
        const urlBeforeRandomize = page.url();
        await page.getByRole('button', { name: 'Randomize', exact: true }).click();
        const expression = await field.inputValue();
        if (expression.length > 40) throw Error('Randomize exceeded the actual input cap');
        const expected = answer(expression, id === 'infix-to-prefix');
        if (executions !== 2 || page.url() !== urlBeforeRandomize) throw Error('Randomize ran or shared an unsubmitted draft');
        const customResponse = page.waitForResponse(response => endpoint(response) && response.request().method() === 'POST');
        await page.getByRole('button', { name: 'Run input', exact: true }).click();
        const response = await customResponse;
        if (response.status() !== 200) throw Error(`Randomize input rejected: ${expression}`);
        const run = await response.json();
        if (run.truncated || run.code !== original.code || run.steps.at(-1).variables.answer !== expected
            || !isDeepStrictEqual(run.resolvedInput, { expression })) throw Error('Randomized answer/source/input mismatch');
        await page.waitForURL(url => url.searchParams.has('input'));
        const shared = new URL(page.url()).searchParams.get('input');
        if (!isDeepStrictEqual(JSON.parse(Buffer.from(shared, 'base64url').toString()), { expression })) {
          throw Error('Randomized successful run did not become shared identity');
        }
        await seek.fill('1');
        await page.waitForURL(url => url.searchParams.get('step') === '2');
        await page.getByRole('button', { name: 'Focus', exact: true }).click();
        await page.getByRole('button', { name: 'Exit focus', exact: true }).click();
        await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
        const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
        if (await sourceButton.isVisible()) await sourceButton.click();
        const source = page.getByRole('region', { name: 'Java source', exact: true });
        await source.waitFor();
        if (await seek.inputValue() !== '1' || executions !== 3) throw Error('Focus/Code changed the run or step');
        const restoredResponse = page.waitForResponse(response => endpoint(response) && response.request().method() === 'POST');
        await page.reload();
        const restored = await (await restoredResponse).json();
        await page.waitForFunction(() => document.querySelector('input[aria-label="Seek to step"]')?.value === '1');
        if (restored.code !== run.code || restored.steps.at(-1).variables.answer !== expected
            || !isDeepStrictEqual(restored.resolvedInput, { expression })) throw Error('Successful shared reload restored a different run');
        if (executions !== 5) throw Error(`Unexpected execution count: ${executions}`);
        if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('Page overflow');
        if (errors.length) throw Error(errors.join('; '));
        rows.push({ id, width, theme, passed: true, rejectedInput: { expression: 'AB' },
          randomizedInput: { expression }, expectedAnswer: expected, executions,
          sourceSha256: createHash('sha256').update(run.code).digest('hex'),
          checks: ['rejected shared link uses default identity', 'rejected step dropped', 'step mirroring resumes',
            'rejected draft retained in editor', 'Randomize edits only draft', 'Randomize accepted by real backend',
            'answer matches independent recursive tree', 'successful input shared exactly', 'Focus/Code preserve step without requests',
            'successful shared input/step reload', 'no page errors or page-wide overflow'] });
        save(false);
        console.log(`PASS ${id} shared-link/Randomize/reload ${width} ${theme}`);
        await context.close();
      }
    }
    save(true);
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
