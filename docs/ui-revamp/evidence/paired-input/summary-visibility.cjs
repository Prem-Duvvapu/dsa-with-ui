const fs = require('node:fs');
const assert = require('node:assert/strict');
const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');

(async () => {
  const base = process.env.FRONTEND_URL || 'http://127.0.0.1:5180';
  const ready = await fetch(`${base}/api/problems/knapsack-01`);
  assert.equal(ready.status, 200, 'Start this probe only after the packaged API is ready');
  const browser = await chromium.launch();
  const rows = [];
  try {
    for (const width of [320, 1366]) for (const theme of ['light', 'dark']) {
      const page = await browser.newPage({ viewport: { width, height: 768 }, colorScheme: theme });
      await page.addInitScript(theme => {
        localStorage.setItem('dsa-ui:seenWelcome', 'true');
        localStorage.setItem('dsa-ui:theme', JSON.stringify(theme));
      }, theme);
      const input = Buffer.from(JSON.stringify({ weights: [1], values: [1], capacity: 1 })).toString('base64url');
      await page.goto(`${base}/problem/knapsack-01?input=${input}`);
      await page.waitForFunction(() => {
        const summary = document.querySelector('[data-testid="input-summary"]');
        return summary && [...summary.querySelectorAll('[title]')].map(el => el.title).sort().join('|') === '1|[1]|[1]';
      });
      const summary = page.getByTestId('input-summary').first();
      await summary.scrollIntoViewIfNeeded();
      rows.push(await summary.evaluate((summary, context) => {
        const box = el => {
          const r = el.getBoundingClientRect();
          return { left: r.left, right: r.right, top: r.top, bottom: r.bottom, width: r.width, height: r.height };
        };
        const inspect = element => {
          const range = document.createRange();
          range.selectNodeContents(element);
          const r = range.getBoundingClientRect();
          const textRect = { left: r.left, right: r.right, top: r.top, bottom: r.bottom, width: r.width };
          const clippedBy = [];
          for (let ancestor = element; ancestor; ancestor = ancestor.parentElement) {
            const style = getComputedStyle(ancestor), rect = box(ancestor);
            if (/hidden|clip|scroll|auto/.test(style.overflowX) &&
                (textRect.left < rect.left - 1 || textRect.right > rect.right + 1)) {
              clippedBy.push({ className: ancestor.className, rect, overflow: style.overflowX });
            }
          }
          return { text: element.textContent, box: box(element), textRect,
            clientWidth: element.clientWidth, scrollWidth: element.scrollWidth, clippedBy, fullyVisible: !clippedBy.length };
        };
        const entries = [...summary.querySelectorAll('[title]')].map(value => ({
          name: inspect(value.previousElementSibling), value: inspect(value)
        }));
        return { ...context, summary: box(summary), pageWidth: document.documentElement.scrollWidth,
          entries, allShortValuesVisible: entries.every(entry => entry.name.fullyVisible && entry.value.fullyVisible) };
      }, { width, theme }));
      await page.close();
    }
  } finally { await browser.close(); }
  const output = process.argv[2] || require('node:path').join(__dirname, 'summary-visibility.json');
  fs.writeFileSync(output, JSON.stringify(rows, null, 2) + '\n');
  console.log(JSON.stringify(rows.map(row => ({ width: row.width, theme: row.theme, summaryWidth: row.summary.width,
    fullyVisible: row.allShortValuesVisible, entries: row.entries.map(entry => ({ name: entry.name.text,
      text: entry.value.text, boxWidth: entry.value.box.width, textWidth: entry.value.textRect.width,
      clipped: !entry.value.fullyVisible })) })), null, 2));
  assert(rows.every(row => row.pageWidth <= row.width), 'Page overflow');
  assert(rows.every(row => row.allShortValuesVisible), 'Short input values are clipped');
})().catch(error => { console.error(error); process.exitCode = 1; });
