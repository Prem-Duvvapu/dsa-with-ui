// Live custom-input and rejected-rerun check; no mocked API responses.
const playwright = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const engine = process.env.AUDIT_ENGINE || 'chromium';
if (!['chromium', 'firefox'].includes(engine)) throw Error('Unknown browser engine');
const fs = require('fs');
const path = require('path');
const { isDeepStrictEqual } = require('util');
const servedBuildIdentity = require('../served-build-identity.cjs');
const pace = require('../pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const out = process.argv[2] || __dirname;
const core = process.env.SOURCE_RESOURCE_FAMILY === 'core';
const partition = process.env.SOURCE_CUSTOM_CASE === 'partition';
const allSpaces = process.env.SOURCE_CUSTOM_CASE === 'reverse-spaces';
const longerPattern = process.env.SOURCE_CUSTOM_CASE === 'rabin-karp-longer';
const reverse = process.env.SOURCE_CUSTOM_CASE === 'reverse-words' || allSpaces;
const hashing = process.env.SOURCE_CUSTOM_CASE === 'rabin-karp' || longerPattern;
const infix = ['infix-postfix', 'infix-prefix', 'infix-postfix-cap', 'infix-prefix-cap'].includes(process.env.SOURCE_CUSTOM_CASE);
const prefix = ['infix-prefix', 'infix-prefix-cap'].includes(process.env.SOURCE_CUSTOM_CASE);
const infixCap = infix && process.env.SOURCE_CUSTOM_CASE.endsWith('-cap');
if ((partition || reverse || hashing || infix) && !core) throw Error('This custom case requires the core family');
const id = partition ? 'book-allocation' : reverse ? 'reverse-every-word' : hashing ? 'rabin-karp-algo'
  : infix ? prefix ? 'infix-to-prefix' : 'infix-to-postfix' : core ? 'balanced-parentheses' : 'longest-common-subsequence';
const submitted = partition ? { pages: [1, 2, 3, 4], m: 2 } : reverse ? { s: allSpaces ? ' '.repeat(60) : " a  BC'  d " }
  : hashing ? longerPattern ? { text: 'a', pattern: 'ab' } : { text: 'caah', pattern: 'ah' }
  : infix ? { expression: infixCap ? 'A' + (prefix ? '-B' : '^B').repeat(19) : '(A+B)*(C-D)^E^F' }
    : core ? { expression: '[]{}()' } : { first: 'abc', second: 'ac' };
const rejectedInput = partition ? { pages: [1, 2, 3, 4], m: 13 } : reverse ? { s: 'hello!' }
  : hashing ? { text: 'CAAH', pattern: 'ah' } : infix ? { expression: process.env.SOURCE_REJECTED_EXPRESSION || 'A++B' }
    : core ? { expression: 'abc' } : { first: 'ABC', second: 'ac' };
const expectedAnswer = partition ? '6' : reverse ? allSpaces ? ' '.repeat(60) : " a  'CB  d "
  : hashing ? longerPattern ? '[]' : '[2]' : infix ? infixCap
    ? prefix ? '-'.repeat(19) + 'A' + 'B'.repeat(19) : 'A' + 'B'.repeat(19) + '^'.repeat(19)
    : prefix ? '*+AB^-CD^EF' : 'AB+CD-EF^^*' : null;
const width = Number(process.env.AUDIT_WIDTH || 1366);
if (![320, 1366].includes(width)) throw Error('Unknown custom-case viewport');
const theme = process.env.AUDIT_THEME || 'light';
if (!['light', 'dark'].includes(theme)) throw Error('Unknown custom-case theme');
(async () => {
  const servedBuild = await servedBuildIdentity(base);
  const browser = await playwright[engine].launch();
  try {
    const page = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 }, colorScheme: theme, reducedMotion: 'reduce' });
    let executions = 0;
    page.on('request', request => {
      if (new URL(request.url()).pathname === `/api/problems/${id}/execute`) executions++;
    });
    await pace(page);
    await page.addInitScript(theme => {
      localStorage.setItem('dsa-ui:seenWelcome', 'true');
      localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
    }, theme);
    const endpoint = response => new URL(response.url()).pathname === `/api/problems/${id}/execute`;
    const initial = page.waitForResponse(endpoint);
    await page.goto(`${base}/problem/${id}?view=code`);
    const original = await (await initial).json();
    await page.getByRole('slider', { name: 'Seek to step' }).waitFor();
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    if (partition) {
      for (const [index, value] of submitted.pages.entries()) {
        await page.getByRole('spinbutton', { name: `Position ${index + 1} value`, exact: true }).fill(String(value));
      }
      await page.getByLabel('Students', { exact: true }).fill(String(submitted.m));
    } else if (core) {
      if (reverse) await page.getByLabel('Sentence', { exact: true }).fill(submitted.s);
      else if (hashing) {
        await page.getByLabel('Text', { exact: true }).fill(submitted.text);
        await page.getByLabel('Pattern', { exact: true }).fill(submitted.pattern);
      } else await page.getByLabel(infix ? 'Infix expression' : 'Bracket string', { exact: true }).fill(submitted.expression);
    } else {
      await page.getByLabel('First string', { exact: true }).fill(submitted.first);
      await page.getByLabel('Second string', { exact: true }).fill(submitted.second);
    }
    const custom = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    const response = await custom;
    if (response.status() !== 200) throw Error(`custom run HTTP ${response.status()}`);
    const run = await response.json();
    await page.waitForFunction(expected => Number(document.querySelector('input[aria-label="Seek to step"]')?.max) === expected,
      run.steps.length - 1);
    if (run.code !== original.code || !isDeepStrictEqual(run.resolvedInput, submitted)) {
      throw Error('custom run source/input mismatch');
    }
    if (expectedAnswer !== null && run.steps.at(-1).variables.answer !== expectedAnswer) {
      throw Error(`custom ${id} answer is not ${expectedAnswer}`);
    }
    if (infix && prefix && !infixCap) {
      let chars = [];
      const pops = run.steps.filter(step => {
        if (Array.isArray(step.arrayState)) chars = step.arrayState;
        return step.activeLine === run.anchors.higherPrec && step.variables?.popped === '^'
          && chars.some(cell => cell.state === 'current' && cell.label === '^');
      });
      if (!pops.length || pops.some(step => !step.description.includes("Equal '^' precedence")
          || !step.description.includes('right-associative'))) throw Error('Prefix equal-exponent pop narration is false or absent');
    }
    let peakStack = null;
    if (infixCap) {
      const peak = run.steps.reduce((best, step, index) => (step.queueOrStackState?.length ?? 0) > best.depth
        ? { depth: step.queueOrStackState.length, index } : best, { depth: 0, index: 0 });
      if (peak.depth !== 19) throw Error('The cap case must exercise a genuine 19-operator stack');
      await page.getByRole('slider', { name: 'Seek to step' }).fill(String(peak.index));
      await page.locator('[data-stack-index="18"]').waitFor();
      if (await page.locator('[data-stack-index]').count() !== 19) throw Error('The peak operator stack is not actually rendered');
      if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('Peak stack overflows page');
      peakStack = { depth: peak.depth, step: peak.index + 1 };
      if ((!prefix && engine === 'chromium' && width === 320 && theme === 'dark')
          || (prefix && engine === 'firefox' && width === 1366 && theme === 'light')) {
        await page.locator('[data-stack-index="0"]').scrollIntoViewIfNeeded();
        fs.mkdirSync(out, { recursive: true });
        await page.screenshot({ path: path.join(out, 'peak-stage.png') });
      }
    }
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    const sourceButton = page.getByRole('button', { name: 'Source', exact: true });
    if (await sourceButton.isVisible()) await sourceButton.click();
    const source = page.getByRole('region', { name: 'Java source', exact: true });
    await source.waitFor();
    const seek = page.getByRole('slider', { name: 'Seek to step' });
    const retainedIndex = Math.min(3, Number(await seek.getAttribute('max')));
    if (retainedIndex > 0) await seek.fill(String(retainedIndex));
    await page.waitForTimeout(100);
    const before = await source.textContent();
    const sharedInput = new URL(page.url()).searchParams.get('input');
    const sharedStep = new URL(page.url()).searchParams.get('step');
    if (!sharedInput) throw Error('successful run did not share input');
    if (sharedStep !== (retainedIndex === 0 ? null : String(retainedIndex + 1))) {
      throw Error('seek did not mirror the selected step to the shared URL');
    }
    if (!isDeepStrictEqual(JSON.parse(Buffer.from(sharedInput, 'base64url').toString('utf8')), submitted)) {
      throw Error('shared input differs from the committed run');
    }
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    await page.getByLabel(partition ? 'Students' : reverse ? 'Sentence' : hashing ? 'Text' : infix ? 'Infix expression' : core ? 'Bracket string' : 'First string', { exact: true })
      .fill(partition ? String(rejectedInput.m) : reverse ? rejectedInput.s : hashing ? rejectedInput.text : core ? rejectedInput.expression : rejectedInput.first);
    const rejected = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    const rejection = await rejected;
    if (rejection.status() !== 400) throw Error('invalid input was not rejected');
    const failure = await rejection.json();
    if (infix && (typeof failure.fieldErrors?.expression !== 'string' || failure.steps)) {
      throw Error('infix grammar rejection must have a field error and no trace');
    }
    const alert = page.getByRole('alert').filter({ hasText: 'This input could not run' });
    await alert.waitFor();
    if (!await alert.evaluate(el => el === document.activeElement)) throw Error('error summary did not receive focus');
    if (new URL(page.url()).searchParams.get('input') !== sharedInput) throw Error('rejected input changed shared input');
    if (new URL(page.url()).searchParams.get('step') !== sharedStep) throw Error('rejected input changed shared step');
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    if (await sourceButton.isVisible()) await sourceButton.click();
    if (await source.textContent() !== before || await seek.inputValue() !== String(retainedIndex)) throw Error('failed rerun changed prior source/step');
    if (executions !== 3) throw Error(`view switches re-executed: expected3 requests, saw${executions}`);
    if (await page.evaluate(() => document.documentElement.scrollWidth > innerWidth)) throw Error('page-wide overflow');
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, 'custom-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, passed: true, id, engine,
      browserVersion: browser.version(), width, theme,
      servedBuild, executions,
      input: run.resolvedInput, rejectedInput, ...(infix ? { fieldError: failure.fieldErrors.expression } : {}),
      ...(peakStack ? { peakStack } : {}),
      checks: ['custom source matches default source', 'resolved input matches submitted input',
        'shared input decodes to committed input', '400 focuses error summary', '400 preserves shared input',
        '400 preserves shared step', '400 preserves source and step', 'view switches execute nothing', 'no page-wide overflow',
        ...(infix && prefix && !infixCap ? ['equal-exponent pop explains reversed right associativity'] : []),
        ...(peakStack ? ['39-character input and peak 19-operator stack fit the page'] : []),
        ...(expectedAnswer !== null ? [`independent custom answer is ${expectedAnswer}`] : [])]
    }, null, 2));
    console.log('PASS custom input, rejected rerun, source/step/link retention and error focus');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
