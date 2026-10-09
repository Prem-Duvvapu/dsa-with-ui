const path = require('path');
const fs = require('fs');

/** Native tab zoom, not CSS zoom, pinch scaling, or deviceScaleFactor emulation.
 * https://playwright.dev/docs/chrome-extensions
 * https://developer.chrome.com/docs/extensions/reference/api/tabs#method-setZoom
 * Each page gets its own disposable persistent context; no user profile is opened.
 */
module.exports = async function nativeBrowserZoom(chromium) {
  const extension = path.join(__dirname, 'browser-zoom-extension');
  const contexts = new Set();
  const pages = new WeakMap();
  return {
    async newPage(options) {
      const context = await chromium.launchPersistentContext('', {
        ...options, deviceScaleFactor: 1, channel: 'chromium', headless: true,
        args: [`--disable-extensions-except=${extension}`, `--load-extension=${extension}`]
      });
      contexts.add(context);
      const worker = context.serviceWorkers()[0] || await context.waitForEvent('serviceworker');
      const page = context.pages()[0] || await context.newPage();
      pages.set(page, { worker, context });
      return page;
    },
    async setZoom(page, factor) {
      const worker = pages.get(page)?.worker;
      if (!worker) throw Error('Page does not belong to the native zoom audit');
      const actual = await worker.evaluate(async ({ url, factor }) => {
        const tabs = await chrome.tabs.query({});
        const tab = tabs.find(tab => tab.url === url);
        if (!tab) throw Error('Audit tab not found');
        await chrome.tabs.setZoom(tab.id, factor);
        return chrome.tabs.getZoom(tab.id);
      }, { url: page.url(), factor });
      if (actual !== factor) throw Error(`Native zoom ${actual}, expected ${factor}`);
      await page.waitForFunction(expected => window.devicePixelRatio === expected, factor);
      return actual;
    },
    async screenshot(page, target) {
      // Playwright's fullPage clip uses CSS dimensions, which crops native-zoom captures.
      // Capture the zoomed surface directly without changing viewport metrics or CSS.
      const session = await page.context().newCDPSession(page);
      try {
        await page.evaluate(() => document.fonts.ready);
        const before = await page.evaluate(() => ({ width: innerWidth, height: innerHeight, devicePixelRatio }));
        const metrics = await session.send('Page.getLayoutMetrics');
        const zoom = metrics.cssVisualViewport.zoom;
        const clip = { x: 0, y: 0, width: Math.ceil(metrics.cssContentSize.width * zoom),
          height: Math.ceil(metrics.cssContentSize.height * zoom), scale: 1 };
        const result = await session.send('Page.captureScreenshot', { format: 'png', clip, captureBeyondViewport: true });
        const png = Buffer.from(result.data, 'base64');
        const size = { width: png.readUInt32BE(16), height: png.readUInt32BE(20) };
        if (size.width !== clip.width || size.height !== clip.height) throw Error('Native zoom screenshot dimensions disagree');
        const after = await page.evaluate(() => ({ width: innerWidth, height: innerHeight, devicePixelRatio }));
        if (JSON.stringify(before) !== JSON.stringify(after)) throw Error('Native screenshot changed viewport metrics');
        fs.writeFileSync(target, png);
        return { ...size, nativeZoom: zoom, viewportUnchanged: true, method: 'CDP zoomed surface, no metric override' };
      } finally { await session.detach(); }
    },
    async closePage(page) {
      const context = pages.get(page)?.context;
      if (!context) throw Error('Page does not belong to the native zoom audit');
      await context.close();
      contexts.delete(context);
      pages.delete(page);
    },
    async close() {
      await Promise.all([...contexts].map(context => context.close()));
      contexts.clear();
    }
  };
};
