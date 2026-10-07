// Live custom-input and rejected-rerun check; no mocked API responses.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const fs = require('fs');
const path = require('path');
const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
(async () => {
  const browser = await chromium.launch();
  try {
    const page = await browser.newPage({ viewport: { width: 1366, height: 768 } });
    await page.addInitScript(() => localStorage.setItem('dsa-ui:seenWelcome', 'true'));
    const endpoint = response => new URL(response.url()).pathname === '/api/problems/longest-common-subsequence/execute';
    const initial = page.waitForResponse(endpoint);
    await page.goto(`${base}/problem/longest-common-subsequence?view=code`);
    const original = await (await initial).json();
    await page.getByRole('slider', { name: 'Seek to step' }).waitFor();
    await page.getByRole('tab', { name: 'Playground', exact: true }).click();
    await page.getByRole('button', { name: 'Edit input', exact: true }).click();
    await page.getByLabel('First string', { exact: true }).fill('abc');
    await page.getByLabel('Second string', { exact: true }).fill('ac');
    const custom = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    const response = await custom;
    if (response.status() !== 200) throw Error(`custom run HTTP ${response.status()}`);
    const run = await response.json();
    if (run.code !== original.code || run.resolvedInput.first !== 'abc' || run.resolvedInput.second !== 'ac') {
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
    await page.getByLabel('First string', { exact: true }).fill('ABC');
    const rejected = page.waitForResponse(endpoint);
    await page.getByRole('button', { name: 'Run input', exact: true }).click();
    if ((await rejected).status() !== 400) throw Error('invalid input was not rejected');
    const alert = page.getByRole('alert').filter({ hasText: 'This input could not run' });
    await alert.waitFor();
    if (!await alert.evaluate(el => el === document.activeElement)) throw Error('error summary did not receive focus');
    if (new URL(page.url()).searchParams.get('input') !== sharedInput) throw Error('rejected input changed shared input');
    await page.getByRole('tab', { name: 'Code walkthrough', exact: true }).click();
    if (await source.textContent() !== before || await seek.inputValue() !== '3') throw Error('failed rerun changed prior source/step');
    fs.writeFileSync(path.join(__dirname, 'custom-results.json'), JSON.stringify({
      generated: new Date().toISOString(), realBackend: true, passed: true,
      input: run.resolvedInput, rejectedInput: { first: 'ABC', second: 'ac' },
      checks: ['custom source matches default source', 'resolved input matches submitted input',
        'successful input shared', '400 focuses error summary', '400 preserves shared input', '400 preserves source and step']
    }, null, 2));
    console.log('PASS custom input, rejected rerun, source/step/link retention and error focus');
  } finally { await browser.close(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
