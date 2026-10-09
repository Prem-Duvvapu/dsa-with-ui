import { act, renderHook } from '@testing-library/react';
import { createElement, startTransition, StrictMode, Suspense } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useApproachComparison from './useApproachComparison';

const response = body => ({ ok: true, status: 200, json: async () => body });
const trace = (id, input = { n: 5 }, extra = {}) => ({ problemId: 'stairs', approachId: id,
  truncated: false, resolvedInput: input, encoding: 'full',
  steps: [{ stepNumber: 1, description: `${id} answer`, variables: { answer: '8', calls: '15' } }], ...extra });
const deferred = () => { let resolve; const promise = new Promise(r => { resolve = r; }); return { promise, resolve }; };
beforeEach(() => vi.stubGlobal('fetch', vi.fn((url, options) => Promise.resolve(response(
  trace(new URL(url, 'https://test.invalid').searchParams.get('approach'), JSON.parse(options.body)))))));
afterEach(() => vi.unstubAllGlobals());
describe('isolated explicit approach comparison', () => {
  it('measures actual UTF-8 response JSON including Unicode, not JS string length', async () => {
    fetch.mockImplementation(url => {
      const raw = JSON.stringify(trace(new URL(url, 'https://test.invalid').searchParams.get('approach'), { n: 5 }, { note: 'λ🙂' }));
      return Promise.resolve({ ok: true, status: 200, text: async () => raw });
    });
    const { result } = renderHook(() => useApproachComparison('stairs'));
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 5 }));
    const raw = JSON.stringify(trace('tabulation', { n: 5 }, { note: 'λ🙂' }));
    expect(result.current.results[0].run.responseJsonBytes).toBe(new TextEncoder().encode(raw).byteLength);
    expect(result.current.results[0].run.responseJsonBytes).toBeGreaterThan(raw.length);
  });
  it('does not request on mount or StrictMode replay; snapshots the same input for both', async () => {
    const { result } = renderHook(() => useApproachComparison('stairs'), { wrapper: StrictMode });
    expect(fetch).not.toHaveBeenCalled();
    const input = { n: 5 };
    await act(async () => { const run = result.current.compare(['tabulation', 'recursion'], input); input.n = 20; await run; });
    expect(fetch.mock.calls.map(([, options]) => JSON.parse(options.body))).toEqual([{ n: 5 }, { n: 5 }]);
    expect(result.current.results.map(r => r.run.resolvedInput)).toEqual([{ n: 5 }, { n: 5 }]);
  });
  it('retains the successful side when the other is rejected, without inventing an answer', async () => {
    fetch.mockImplementation((url) => Promise.resolve(url.includes('recursion') ? { ok: false, status: 400,
      json: async () => ({ fieldErrors: { n: 'Must be at most 10.' } }) } : response(trace('tabulation'))));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 5 }));
    expect(result.current.results[0].run.steps[0].variables.answer).toBe('8');
    expect(result.current.results[1]).toMatchObject({ run: null, error: 'n: Must be at most 10.' });
  });
  it.each([
    { problemId: 'other' }, { approachId: 'other' }, { resolvedInput: { n: 6 } },
    { truncated: undefined }, { steps: [] }, { steps: [{ stepNumber: 1 }] },
    { encoding: 'unknown' }, { steps: [{ stepNumber: 2, description: 'Wrong ordinal' }] },
    { steps: [{ stepNumber: 1, description: ' ' }] },
    ...[[], 7, { answer: { value: '8' } }, { calls: -1 }].map(variables => ({
      steps: [{ stepNumber: 1, description: 'Malformed debug values', variables }]
    }))
  ])('refuses substituted or malformed comparison metadata %j', async extra => {
    fetch.mockImplementation(url => Promise.resolve(response(trace(new URL(url, 'https://test.invalid').searchParams.get('approach'), { n: 5 }, extra))));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 5 }));
    expect(result.current.results.every(r => r.run === null && r.error)).toBe(true);
  });
  it('preserves truncation instead of treating its last frame as a complete run', async () => {
    fetch.mockImplementation(url => Promise.resolve(response(trace(new URL(url, 'https://test.invalid').searchParams.get('approach'), { n: 5 }, { truncated: true }))));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 5 }));
    expect(result.current.results.every(r => r.run.truncated)).toBe(true);
  });
  it('retires old decoded bodies after a newer comparison starts', async () => {
    const old = deferred();
    fetch.mockImplementationOnce(() => Promise.resolve({ ok: true, json: () => old.promise }));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    let stale;
    act(() => { stale = result.current.compare(['tabulation', 'recursion'], { n: 5 }); });
    await act(() => result.current.compare(['memoization', 'recursion'], { n: 6 }));
    await act(async () => { old.resolve(trace('tabulation')); await stale; });
    expect(result.current.results.map(r => r.id)).toEqual(['memoization', 'recursion']);
    expect(result.current.results[0].run.resolvedInput).toEqual({ n: 6 });
    expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
  });
  it('clear aborts pending bodies and prevents them from reopening results', async () => {
    const body = deferred();
    fetch.mockImplementationOnce(() => Promise.resolve({ ok: true, json: () => body.promise }));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    let pending;
    act(() => { pending = result.current.compare(['tabulation', 'recursion'], { n: 5 }); });
    act(() => result.current.clear());
    await act(async () => { body.resolve(trace('tabulation')); await pending; });
    expect(result.current.results).toBeNull();
    expect(result.current.pending).toBe(false);
  });
  it('gates results on problem identity and aborts on problem switch/unmount', async () => {
    const { result, rerender, unmount } = renderHook(({ id }) => useApproachComparison(id), { initialProps: { id: 'stairs' } });
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 5 }));
    rerender({ id: 'other' });
    expect(result.current.results).toBeNull();
    expect(fetch.mock.calls[0][1].signal.aborted).toBe(true);
    unmount();
  });
  it('never launches identical sides', async () => {
    const { result } = renderHook(() => useApproachComparison('stairs'));
    await act(() => result.current.compare(['recursion', 'recursion'], { n: 5 }));
    expect(fetch).not.toHaveBeenCalled();
  });

  it('does not launch requests through a retained callback after unmount', async () => {
    const { result, unmount } = renderHook(() => useApproachComparison('stairs'));
    const compare = result.current.compare;
    unmount();
    await act(() => compare(['tabulation', 'recursion'], { n: 5 }));
    expect(fetch).not.toHaveBeenCalled();
  });

  it('cannot let a retired problem callback cancel or replace the current comparison', async () => {
    const { result, rerender } = renderHook(({ id }) => useApproachComparison(id), { initialProps: { id: 'stairs' } });
    const oldCompare = result.current.compare;
    const oldClear = result.current.clear;
    rerender({ id: 'other' });
    fetch.mockImplementation((url, options) => Promise.resolve(response({
      ...trace(new URL(url, 'https://test.invalid').searchParams.get('approach'), JSON.parse(options.body)), problemId: 'other'
    })));
    await act(() => result.current.compare(['tabulation', 'recursion'], { n: 6 }));
    await act(() => oldCompare(['tabulation', 'recursion'], { n: 5 }));
    act(() => oldClear());
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(result.current.results[0].run.resolvedInput).toEqual({ n: 6 });
  });

  it('does not adopt an uncommitted problem from a suspended transition', async () => {
    const blocked = new Promise(() => {});
    const { result, rerender } = renderHook(({ id, suspend }) => {
      const hook = useApproachComparison(id);
      if (suspend) throw blocked;
      return hook;
    }, { initialProps: { id: 'stairs', suspend: false },
      wrapper: ({ children }) => createElement(Suspense, { fallback: 'Loading' }, children) });
    const compare = result.current.compare;
    act(() => startTransition(() => rerender({ id: 'other', suspend: true })));
    await act(() => compare(['tabulation', 'recursion'], { n: 5 }));
    expect(fetch).toHaveBeenCalledTimes(2);
    expect(result.current.results[0].run.problemId).toBe('stairs');
  });

  it('rejecting an invalid pair retires pending work without leaving pending stuck', async () => {
    const body = deferred();
    fetch.mockImplementationOnce(() => Promise.resolve({ ok: true, json: () => body.promise }));
    const { result } = renderHook(() => useApproachComparison('stairs'));
    let pending;
    act(() => { pending = result.current.compare(['tabulation', 'recursion'], { n: 5 }); });
    await act(() => result.current.compare(['recursion', 'recursion'], { n: 5 }));
    expect(result.current.pending).toBe(false);
    expect(result.current.results).toBeNull();
    expect(fetch).toHaveBeenCalledTimes(2);
    await act(async () => { body.resolve(trace('tabulation')); await pending; });
    expect(result.current.pending).toBe(false);
    expect(result.current.results).toBeNull();
  });
});
