import React from 'react';
import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { MemoryRouter, useLocation } from 'react-router-dom';
import useProblemSession from './useProblemSession';
import { encodeInput } from './useShareableView';

const ok = (body) => ({ ok: true, status: 200, json: () => Promise.resolve(body) });
const ENTRY = { id: 'alpha', title: 'Alpha', dsType: 'Array', inputSpec: { fields: [{ name: 'n', label: 'Count', type: 'INT', defaultValue: 1 }] } };
const trace = (n) => ({ encoding: 'delta', resolvedInput: { n }, steps: [
  { stepNumber: 1, activeLine: 1, keyframe: true, dsType: 'Array', description: `n=${n} one`, variables: {} },
  { stepNumber: 2, activeLine: 1, dsType: 'Array', description: `n=${n} two` },
  { stepNumber: 3, activeLine: 1, dsType: 'Array', description: `n=${n} three` }
] });

let held;
beforeEach(() => {
  held = [];
  vi.stubGlobal('fetch', vi.fn((url, opts = {}) => {
    if (url === '/api/problems/alpha') return Promise.resolve(ok(ENTRY));
    if (opts.method === 'POST') {
      const { n } = JSON.parse(opts.body);
      return new Promise((resolve) => held.push({ n, release: () => resolve(ok(trace(n))) }));
    }
    return Promise.resolve(ok(trace(1)));
  }));
});
afterEach(() => { vi.unstubAllGlobals(); vi.restoreAllMocks(); });

describe('useProblemSession restoration lifecycle (review #4)', () => {
  it('releases the restoring state when a newer submission supersedes the link', async () => {
    let location;
    function Probe() { location = useLocation(); return null; }
    const wrapper = ({ children }) => (
      <MemoryRouter initialEntries={[`/problem/alpha?input=${encodeInput({ n: 7 })}&step=2`]}>{children}<Probe /></MemoryRouter>
    );
    const { result } = renderHook(
      () => useProblemSession({ problemId: 'alpha', catalogEntry: ENTRY, initialSpeed: 1000 }),
      { wrapper }
    );
    await waitFor(() => expect(held).toHaveLength(1));
    expect(result.current.restoring).toBe(true);

    let newer;
    act(() => { newer = result.current.submit({ n: 9 }); });
    await waitFor(() => expect(held).toHaveLength(2));
    await act(async () => { held[0].release(); held[1].release(); await newer; });

    expect(result.current.steps[0].description).toBe('n=9 one');
    expect(result.current.restoring).toBe(false);
    // Mirroring works again: a step change reaches the URL, not only the local index.
    act(() => result.current.seek(2));
    expect(result.current.currentStepIndex).toBe(2);
    await waitFor(() => expect(new URLSearchParams(location.search).get('step')).toBe('3'));
  });
});
