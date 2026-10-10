// Live custom-input and rejected-rerun check; no mocked API responses.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
const out = process.argv[2] || __dirname;
const core = process.env.SOURCE_RESOURCE_FAMILY === 'core';
const id = core ? 'balanced-parentheses' : 'longest-common-subsequence';
const submitted = core ? { expression: '[]{}()' } : { first: 'abc', second: 'ac' };
const rejectedInput = core ? { expression: 'abc' } : { first: 'ABC', second: 'ac' };
(async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 768 } });
    await page.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
    const endpoint = response => new URL(response.url()).pathname === `/api/problems/${id}/execute`;
    const initial = page.waitForResponse(endpoint);
    await page.goto(`${base}/problem/${id}?view=code`);
    const original = await (await initial).json();
    await page.getByRole('slider', { name: 'Seek to step' }).waitFor();
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    if (core) {
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
    if (run.code !== original.code || Object.entries(submitted).some(([key, value]) => run.resolvedInput[key] !== value)) {
      throw Error('custom run source/input mismatch');
    }
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    const source = page.getByRole('region', { name: 'Java source', exact: true });
    await source.waitFor();
    const seek = page.getByRole('slider', { name: 'Seek to step' });
    await seek.fill('3');
    await page.waitForTimeout(100);
    const before = await source.textContent();
    const sharedInput = new URL(page.url()).searchParams.get('input');
    if (!sharedInput) throw Error('successful run did not share input');
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    await page.getByLabel(core ? 'Bracket string' : 'First string', { exact: true })
      .fill(core ? rejectedInput.expression : rejectedInput.first);
    const rejected = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    if ((await rejected).status() !== 400) throw Error('invalid input was not rejected');
    const alert = page.getByRole('alert').filter({ hasText: 'This input could not run' });
    await alert.waitFor();
    if (!await alert.evaluate(el => el === document.activeElement)) throw Error('error summary did not receive focus');
    if (new URL(page.url()).searchParams.get('input') !== sharedInput) throw Error('rejected input changed shared input');
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    if (await source.textContent() !== before || await seek.inputValue() !== '3') throw Error('failed rerun changed prior source/step');
    fs.mkdirSync(out, { recursive: true });
    fs.writeFileSync(path.join(out, 'custom-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, passed: true, id,
      input: run.resolvedInput, rejectedInput,
      checks: ['custom source matches default source', 'resolved input matches submitted input',
        'successful input shared', '400 focuses error summary', '400 preserves shared input', '400 preserves source and step']
    }, null, 2));
    console.log('PASS custom input, rejected rerun, source/step/link retention and error focus');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
