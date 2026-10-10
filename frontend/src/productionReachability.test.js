// @vitest-environment node
import { describe, expect, it } from 'vitest';
import { inspectProductionReachability, isProductionSource } from '../../docs/ui-revamp/evidence/p9/production-reachability.mjs';

describe('production source reachability', () => {
  it('leaves no JS/JSX/CSS module that is loaded only by tests or by another retired module', async () => {
    // A real production build, not a test-import graph or a hand-maintained legacy list.
    // Vite resolves dynamic imports, CSS and the renderer registry; write:false keeps dist intact.
    const report = await inspectProductionReachability();
    expect(report.productionModules.length).toBeGreaterThan(0);
    expect(report.assets.some(file => file.endsWith('.js'))).toBe(true);
    expect(report.assets.some(file => file.endsWith('.css'))).toBe(true);
    expect(report.unreachableFiles).toEqual([]);
  }, 30000);

  it.each(['components/Example.test.jsx', 'hooks/Example.spec.js', 'setupTests.js',
    'test/api-fixture.jsx', 'C:\\repo\\src\\setupTests.js'])('does not count test-only %s as a production module', file => {
    expect(isProductionSource(file)).toBe(false);
  });

  it.each(['components/Canvas.jsx', 'hooks/session.js', 'components/Canvas.module.css'])('checks production %s rather than ignoring it', file => {
    expect(isProductionSource(file)).toBe(true);
  });
});
