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
if ((partition || reverse || hashing) && !core) throw Error('This custom case requires the core family');
const id = partition ? 'book-allocation' : reverse ? 'reverse-every-word' : hashing ? 'rabin-karp-algo'
  : core ? 'balanced-parentheses' : 'longest-common-subsequence';
const submitted = partition ? { pages: [1, 2, 3, 4], m: 2 } : reverse ? { s: allSpaces ? ' '.repeat(60) : " a  BC'  d " }
  : hashing ? longerPattern ? { text: 'a', pattern: 'ab' } : { text: 'caah', pattern: 'ah' }
  : core ? { expression: '[]{}()' } : { first: 'abc', second: 'ac' };
const rejectedInput = partition ? { pages: [1, 2, 3, 4], m: 13 } : reverse ? { s: 'hello!' }
  : hashing ? { text: 'CAAH', pattern: 'ah' } : core ? { expression: 'abc' } : { first: 'ABC', second: 'ac' };
const expectedAnswer = partition ? '6' : reverse ? allSpaces ? ' '.repeat(60) : " a  'CB  d "
  : hashing ? longerPattern ? '[]' : '[2]' : null;
const width = Number(process.env.AUDIT_WIDTH || 1366);
if (![320, 1366].includes(width)) throw Error('Unknown custom-case viewport');
(async () => {
  const servedBuild = await servedBuildIdentity(base);
  const browser = await playwright[engine].launch();
  try {
    const page = await browser.newPage({ viewport: { width, height: width === 320 ? 568 : 768 } });
    let executions = 0;
    page.on('request', request => {
      if (new URL(request.url()).pathname === `/api/problems/${id}/execute`) executions++;
    });
    await pace(page);
    await page.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
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
      } else await page.getByLabel('Bracket string', { exact: true }).fill(submitted.expression);
    } else {
      await page.getByLabel('First string', { exact: true }).fill(submitted.first);
      await page.getByLabel('Second string', { exact: true }).fill(submitted.second);
    }
    const custom = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    const response = await custom;
    if (response.status() !== 200) throw Error(`custom run HTTP ${response.status()}`);
    const run = await response.json();
    if (run.code !== original.code || !isDeepStrictEqual(run.resolvedInput, submitted)) {
      throw Error('custom run source/input mismatch');
    }
    if (expectedAnswer !== null && run.steps.at(-1).variables.answer !== expectedAnswer) {
      throw Error(`custom ${id} answer is not ${expectedAnswer}`);
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
    await page.getByLabel(partition ? 'Students' : reverse ? 'Sentence' : hashing ? 'Text' : core ? 'Bracket string' : 'First string', { exact: true })
      .fill(partition ? String(rejectedInput.m) : reverse ? rejectedInput.s : hashing ? rejectedInput.text : core ? rejectedInput.expression : rejectedInput.first);
    const rejected = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    if ((await rejected).status() !== 400) throw Error('invalid input was not rejected');
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
      browserVersion: browser.version(), width,
      servedBuild, executions,
      input: run.resolvedInput, rejectedInput,
      checks: ['custom source matches default source', 'resolved input matches submitted input',
        'shared input decodes to committed input', '400 focuses error summary', '400 preserves shared input',
        '400 preserves shared step', '400 preserves source and step', 'view switches execute nothing', 'no page-wide overflow',
        ...(expectedAnswer !== null ? [`independent custom answer is ${expectedAnswer}`] : [])]
    }, null, 2));
    console.log('PASS custom input, rejected rerun, source/step/link retention and error focus');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
