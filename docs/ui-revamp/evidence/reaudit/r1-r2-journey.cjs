// Run with PLAYWRIGHT_MODULE pointing to a cached Playwright package (no project dependency).
// Uses explicit API fixtures to reproduce failure combinations without changing a backend.
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const base = process.env.AUDIT_BASE_URL || 'http://localhost:5181';
const output = process.env.AUDIT_OUTPUT_DIR || __dirname;
const enc = values => Buffer.from(JSON.stringify(values)).toString('base64url');
const entry = {
  id: 'audit-example', title: 'Audit example', category: 'Test', difficulty: 'Easy', dsType: 'Array', traced: true,
  javaCode: Array.from({ length: 30 }, (_, i) => `int value${i} = ${Array.from({ length: 60 }, () => '1').join(' + ')};`).join('\n'),
  inputSpec: { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1, constraints: { min: 0, max: 50 } }] }
};
const trace = n => ({
  encoding: 'delta', resolvedInput: { n }, anchors: {},
  steps: Array.from({ length: 5 }, (_, i) => ({ stepNumber: i + 1, activeLine: i + 1, keyframe: true,
    dsType: 'Array', description: `n=${n} step ${i + 1}`, variables: { n },
    arrayState: [{ index: 0, value: n, state: 'default' }] }))
});
const sleep = ms => new Promise(resolve => setTimeout(resolve, ms));

(async () => {
  fs.mkdirSync(output, { recursive: true });
  const browser = await chromium.launch();
  const results = [];
  const errors = [];
  try {
    for (const theme of ['light', 'dark']) {
      for (const width of [1366, 390]) {
        const context = await browser.newContext({ viewport: { width, height: 844 }, reducedMotion: 'reduce' });
        await context.addInitScript(t => {
          localStorage.setItem('dsa-ui:seenWelcome', 'true');
          localStorage.setItem('dsa-ui:theme', JSON.stringify(t));
        }, theme);
        let failDefault = false;
        let sample = false;
        let posts = 0;
        let gets = 0;
        await context.route('**/api/**', async route => {
          const pathname = new URL(route.request().url()).pathname;
          const problem = sample ? { ...entry, executionSteps: trace(1).steps } : entry;
          const reply = (body, status = 200) => route.fulfill({ status, contentType: 'application/json', body: JSON.stringify(body) });
          if (pathname === '/api/problems') return reply([problem]);
          if (pathname === '/api/problems/audit-example') return reply(problem);
          if (pathname.endsWith('/execute')) {
            if (route.request().method() === 'POST') {
              posts++;
              const { n } = route.request().postDataJSON();
              return n > 50 ? reply({ fieldErrors: { n: 'Must be at most 50.' } }, 400) : reply(trace(n));
            }
            gets++;
            // Ensure the catalogue has arrived before the failed default's sample fallback.
            if (failDefault) { await sleep(100); return reply(null, 500); }
            return reply(trace(1));
          }
          return reply(null, 404);
        });
        const page = await context.newPage();
        page.setDefaultTimeout(10000);
        page.on('pageerror', error => errors.push(error.message));
        const narration = page.locator('p[aria-live="polite"]');
        await page.goto(`${base}/problem/audit-example?view=code&input=${enc({ n: 7 })}&step=3`, { waitUntil: 'networkidle' });
        await page.getByText('n=7 step 3', { exact: true }).waitFor();
        const source = page.getByRole('region', { name: 'Java source' });
        await source.focus();
        await source.evaluate(node => { node.scrollLeft = 200; });
        const beforeUrl = page.url();
        const beforePosts = posts;
        const beforeGets = gets;
        await source.press('ArrowRight');
        await page.waitForFunction(() => document.querySelector('[aria-label="Java source"]').scrollLeft > 200);
        const right = await source.evaluate(node => node.scrollLeft);
        await source.press('ArrowLeft');
        await page.waitForFunction(x => document.querySelector('[aria-label="Java source"]').scrollLeft < x, right);
        const left = await source.evaluate(node => node.scrollLeft);
        assert.equal(await narration.innerText(), 'n=7 step 3');
        assert.equal(page.url(), beforeUrl);
        assert.equal(posts, beforePosts);
        assert.equal(gets, beforeGets);
        await page.getByText('Not following — you scrolled the source', { exact: true }).waitFor();
        assert.equal(await source.evaluate(node => node === document.activeElement), true);
        assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false);
        results.push({ theme, width, check: 'source arrows', right, left, step: 3, urlUnchanged: true, extraExecutions: 0 });
        await page.screenshot({ path: path.join(output, `source-${theme}-${width}.png`), fullPage: true });

        // The same keys still own playback when focus is outside the source region.
        await page.getByRole('heading', { name: 'Audit example', exact: true }).click();
        await page.keyboard.press('ArrowRight');
        await page.getByText('n=7 step 4', { exact: true }).waitFor();
        await page.keyboard.press('ArrowLeft');
        await page.getByText('n=7 step 3', { exact: true }).waitFor();

        for (const available of ['none', 'offline']) {
          failDefault = true;
          sample = available === 'offline';
          for (const input of [enc({ n: 99 }), 'not-valid-json']) {
            for (const step of [null, 3]) {
              await page.goto(`${base}/problem/audit-example?input=${input}${step ? `&step=${step}` : ''}`, { waitUntil: 'networkidle' });
              const notice = page.getByRole('status', { name: 'Shared link' });
              await notice.waitFor();
              const text = await notice.innerText();
              assert.doesNotMatch(text, /showing the default input/i);
              if (available === 'none') {
                assert.match(text, /There is no run to show/);
                assert.doesNotMatch(text, /playback starts at step 1/i);
                if (step) assert.match(text, /no run to show it in/);
              } else {
                assert.match(text, /Showing the offline sample; its input is unknown/);
                assert.equal(await narration.innerText(), 'n=1 step 1');
              }
              const search = new URL(page.url()).searchParams;
              assert.equal(search.get('input'), null);
              assert.equal(search.get('step'), null);
              results.push({ theme, width, check: 'failed link', available, input: input === 'not-valid-json' ? 'unreadable' : 'rejected', step, text });
            }
          }
        }
        // No phantom previous-run claim in the alternate view's validation alert.
        sample = false;
        await page.goto(`${base}/problem/audit-example?view=analysis&input=${enc({ n: 99 })}&step=3`, { waitUntil: 'networkidle' });
        const alert = page.getByRole('alert', { name: 'Your input could not run' });
        assert.match(await alert.innerText(), /There is no run to show/);
        await alert.getByRole('button', { name: 'Fix it in the editor' }).click();
        // StrictMode replays the InputPanel effect, which focuses its validation summary.
        // Either the editor heading or that summary is an actionable editor focus target.
        await page.waitForFunction(() => document.activeElement?.closest('#try-input'));
        const focused = await page.evaluate(() => ({ id: document.activeElement.id, role: document.activeElement.getAttribute('role') }));
        assert.ok(focused.id === 'try-input-title' || focused.role === 'alert');
        await page.getByText('Count: Must be at most 50.', { exact: true }).waitFor();
        results.push({ theme, width, check: 'validation recovery', focused });
        console.log(`${theme} ${width}px passed source, failure-link and recovery checks.`);
        await context.close();
      }
    }
    assert.deepEqual(errors, []);
    fs.writeFileSync(path.join(output, 'results.json'), JSON.stringify({ backend: 'API fixtures', results, errors }, null, 2) + '\n');
    console.log(`Passed ${results.length} browser checks; ${errors.length} page errors.`);
  } finally {
    await browser.close();
  }
})().catch(error => { console.error(error); process.exitCode = 1; });
