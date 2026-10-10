// Live custom-input and rejected-rerun check; no mocked API responses.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const { isDeepStrictEqual } = require('util');
const pace = require('../pace-executions.cjs')();
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const out = process.argv[2] || __dirname;
const core = process.env.SOURCE_RESOURCE_FAMILY === 'core';
const partition = process.env.SOURCE_CUSTOM_CASE === 'partition';
if (partition && !core) throw Error('Partition custom case requires the core family');
const id = partition ? 'book-allocation' : core ? 'balanced-parentheses' : 'longest-common-subsequence';
const submitted = partition ? { pages: [1, 2, 3, 4], m: 2 } : core ? { expression: '[]{}()' } : { first: 'abc', second: 'ac' };
const rejectedInput = partition ? { pages: [1, 2, 3, 4], m: 13 } : core ? { expression: 'abc' } : { first: 'ABC', second: 'ac' };
(async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 768 } });
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
      await page.getByLabel('Bracket string', { exact: true }).fill(submitted.expression);
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
    if (partition && run.steps.at(-1).variables.answer !== '6') throw Error('custom partition answer is not 6');
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    const source = page.getByRole('region', { name: 'Java source', exact: true });
    await source.waitFor();
    const seek = page.getByRole('slider', { name: 'Seek to step' });
    await seek.fill('3');
    await page.waitForTimeout(100);
    const before = await source.textContent();
    const sharedInput = new URL(page.url()).searchParams.get('input');
    const sharedStep = new URL(page.url()).searchParams.get('step');
    if (!sharedInput) throw Error('successful run did not share input');
    if (sharedStep !== '4') throw Error('seek did not mirror step4 to the shared URL');
    if (!isDeepStrictEqual(JSON.parse(Buffer.from(sharedInput, 'base64url').toString('utf8')), submitted)) {
      throw Error('shared input differs from the committed run');
    }
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    await page.getByLabel(partition ? 'Students' : core ? 'Bracket string' : 'First string', { exact: true })
      .fill(partition ? String(rejectedInput.m) : core ? rejectedInput.expression : rejectedInput.first);
    const rejected = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    if ((await rejected).status() !== 400) throw Error('invalid input was not rejected');
    const alert = page.getByRole('alert').filter({ hasText: 'This input could not run' });
    await alert.waitFor();
    if (!await alert.evaluate(el => el === document.activeElement)) throw Error('error summary did not receive focus');
    if (new URL(page.url()).searchParams.get('input') !== sharedInput) throw Error('rejected input changed shared input');
    if (new URL(page.url()).searchParams.get('step') !== sharedStep) throw Error('rejected input changed shared step');
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    if (await source.textContent() !== before || await seek.inputValue() !== '3') throw Error('failed rerun changed prior source/step');
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, 'custom-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, passed: true, id,
      input: run.resolvedInput, rejectedInput,
      checks: ['custom source matches default source', 'resolved input matches submitted input',
        'shared input decodes to committed input', '400 focuses error summary', '400 preserves shared input',
        '400 preserves shared step', '400 preserves source and step',
        ...(partition ? ['independent custom answer is 6'] : [])]
    }, null, 2));
    console.log('PASS custom input, rejected rerun, source/step/link retention and error focus');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
