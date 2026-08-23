import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import useTrace from './useTrace';

function ok(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}

function notImplemented() {
  return { ok: false, status: 501, json: () => Promise.resolve(null) };
}

const STEPS = [
  { stepNumber: 1, activeLine: 1, description: 'step one', dsType: 'Array',
    arrayState: [{ index: 0, value: 2, state: 'default' }], variables: {} },
  { stepNumber: 2, activeLine: 2, description: 'step two', dsType: 'Array',
    arrayState: [{ index: 0, value: 2, state: 'comparing' }], variables: {} }
];

const DELTA_RESPONSE = {
  encoding: 'delta',
  steps: [
    { stepNumber: 1, activeLine: 1, description: 'step one', keyframe: true,
      dsType: 'Array', arrayState: [{ index: 0, value: 2, state: 'default' }], variables: {} },
    { stepNumber: 2, activeLine: 2, description: 'step two',
      arrayState: [{ index: 0, value: 2, state: 'comparing' }] }
  ]
};

let fetchCalls;
let resolvers;

beforeEach(() => {
  fetchCalls = [];
  resolvers = new Map();

  vi.stubGlobal('fetch', vi.fn((url, opts) => {
    fetchCalls.push({ url, signal: opts?.signal });

    if (resolvers.has(url)) {
      return new Promise((resolve) => resolvers.set(url, { resolve, signal: opts?.signal }));
    }

    // Default: detail returns a problem, execute returns steps
    if (url.endsWith('/execute')) return Promise.resolve(ok(STEPS));
    if (url.match(/\/api\/problems\/[^/]+$/)) return Promise.resolve(ok({ id: 'two-sum', title: 'Two Sum', dsType: 'Array' }));
    return Promise.resolve(ok(null));
  }));
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe('useTrace', () => {
  it('fetches detail and execute on mount when given a problemId', async () => {
    const { result } = renderHook(() => useTrace('two-sum', null));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(fetchCalls.map(c => c.url)).toContain('/api/problems/two-sum');
    expect(fetchCalls.map(c => c.url)).toContain('/api/problems/two-sum/execute');
    expect(result.current.steps).toHaveLength(2);
    expect(result.current.steps[0].description).toBe('step one');
    expect(result.current.error).toBeNull();
  });

  it('does nothing with a null problemId', () => {
    const { result } = renderHook(() => useTrace(null, null));
    expect(result.current.steps).toHaveLength(0);
    expect(result.current.loading).toBe(false);
  });

  it('decodes delta-encoded responses', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url.endsWith('/execute')) return Promise.resolve(ok(DELTA_RESPONSE));
      return Promise.resolve(ok({ id: 'two-sum', title: 'Two Sum', dsType: 'Array' }));
    }));

    const { result } = renderHook(() => useTrace('two-sum', null));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.steps).toHaveLength(2);
    // Delta decoding should carry forward dsType and variables from keyframe
    expect(result.current.steps[1].dsType).toBe('Array');
    expect(result.current.steps[1].variables).toEqual({});
  });

  it('marks untraced problems with error="untraced"', async () => {
    vi.stubGlobal('fetch', vi.fn((url) => {
      if (url.endsWith('/execute')) return Promise.resolve(notImplemented());
      return Promise.resolve(ok({ id: 'unknown', title: 'Unknown' }));
    }));

    const { result } = renderHook(() => useTrace('unknown', null));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBe('untraced');
    expect(result.current.steps).toHaveLength(0);
  });

  it('discards a stale response when the problemId changes', async () => {
    // Hold the first problem's execute response open
    resolvers.set('/api/problems/slow/execute', null);

    const { result, rerender } = renderHook(
      ({ id }) => useTrace(id, null),
      { initialProps: { id: 'slow' } }
    );

    // Wait for the resolver to be set (promise created)
    await waitFor(() =>
      expect(resolvers.get('/api/problems/slow/execute')).toBeTruthy()
    );

    // Switch to a different problem that resolves immediately
    vi.stubGlobal('fetch', vi.fn((url) => {
      fetchCalls.push({ url });
      if (url.endsWith('/execute')) return Promise.resolve(ok(STEPS));
      return Promise.resolve(ok({ id: 'fast', title: 'Fast' }));
    }));

    rerender({ id: 'fast' });

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.steps).toHaveLength(2);

    // Now resolve the stale request — it must not overwrite
    const staleResolver = resolvers.get('/api/problems/slow/execute');
    if (staleResolver?.resolve) {
      staleResolver.resolve(ok([
        { stepNumber: 1, activeLine: 99, description: 'STALE', variables: {} }
      ]));
    }

    // Wait a tick, then confirm nothing changed
    await new Promise(r => setTimeout(r, 50));
    expect(result.current.steps[0].description).toBe('step one');
  });

  it('aborts in-flight requests when the problemId changes', async () => {
    resolvers.set('/api/problems/first/execute', null);

    const { rerender } = renderHook(
      ({ id }) => useTrace(id, null),
      { initialProps: { id: 'first' } }
    );

    await waitFor(() =>
      expect(resolvers.get('/api/problems/first/execute')).toBeTruthy()
    );

    // The signal attached to the first fetch should be aborted on rerender
    const firstCall = fetchCalls.find(c => c.url === '/api/problems/first/execute');
    expect(firstCall?.signal).toBeDefined();

    rerender({ id: 'second' });

    expect(firstCall.signal.aborted).toBe(true);
  });

  it('exposes playback controls', async () => {
    const { result } = renderHook(() => useTrace('two-sum', null));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.currentStepIndex).toBe(0);
    expect(result.current.isPlaying).toBe(false);

    act(() => result.current.stepNext());
    expect(result.current.currentStepIndex).toBe(1);

    act(() => result.current.stepPrev());
    expect(result.current.currentStepIndex).toBe(0);

    act(() => result.current.seek(1));
    expect(result.current.currentStepIndex).toBe(1);

    act(() => result.current.reset());
    expect(result.current.currentStepIndex).toBe(0);
  });
});
