// @vitest-environment node
import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

const config = JSON.parse(readFileSync(new URL('../vercel.json', import.meta.url), 'utf8'));
const fallback = config.rewrites.find(rule => rule.destination === '/index.html');
const matchesFallback = path => new RegExp(`^${fallback.source}$`).test(path);

describe('Vercel SPA fallback boundaries', () => {
  it.each([
    '/_vercel',
    '/_vercel/insights/script.js',
    '/_vercel/insights/view',
    '/_vercel/insights/event',
    '/_vercel/speed-insights/script.js',
    '/_vercel/speed-insights/vitals',
  ])('keeps platform endpoint %s out of the HTML fallback', path => {
    expect(matchesFallback(path)).toBe(false);
  });

  it.each(['/', '/problem/two-sum', '/problem/longest-increasing-subsequence',
    '/_vercel-guide', '/api-guide'])('preserves SPA deep link %s', path => {
    expect(matchesFallback(path)).toBe(true);
  });

  it.each(['/api/problems', '/api/problems/two-sum/execute'])('keeps API route %s out of the HTML fallback', path => {
    expect(matchesFallback(path)).toBe(false);
  });
});
