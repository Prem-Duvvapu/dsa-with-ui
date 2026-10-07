// Browser evidence must respect the backend's normal 60/minute execution limit.
// Share one pacer across all contexts in a journey. Aborted StrictMode requests are
// paced too; do not raise/disable the server's limit just to make a sweep pass.
module.exports = function createExecutionPacer(intervalMs = 1100) {
  let next = 0;
  return async function install(page) {
    await page.route('**/api/problems/*/execute', async route => {
      const now = Date.now();
      const due = Math.max(now, next);
      next = due + intervalMs;
      if (due > now) await new Promise(resolve => setTimeout(resolve, due - now));
      try {
        await route.continue();
      } catch (error) {
        // A retired execution can be cancelled while waiting for its turn.
        if (!page.isClosed() && !route.request().failure()) throw error;
      }
    });
  };
};
