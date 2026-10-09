import { act, renderHook, waitFor } from '@testing-library/react';
import { createElement, startTransition, StrictMode, Suspense } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import useComparisonTrace from './useComparisonTrace';

const trace = (overrides = {}) => ({ problemId: 'stairs', approachId: 'canonical',
  encoding: 'full', truncated: false, resolvedInput: { n: 5 },
  steps: [{ stepNumber: 1, description: 'Actual event' }], ...overrides });
const ok = data => ({ ok: true, json: async () => data });

describe('input comparison response integrity', () => {
  beforeEach(() => vi.stubGlobal('fetch', vi.fn().mockResolvedValue(ok(trace()))));
  afterEach(() => vi.unstubAllGlobals());

  it.each([
    ['wrong problem', { problemId: 'another-problem' }],
    ['wrong canonical approach', { approachId: 'recursion' }],
    ['missing truncation status', { truncated: undefined }],
    ['empty trace', { steps: [] }],
    ['malformed event', { steps: [{ stepNumber: 1 }] }],
    ['unknown encoding', { encoding: 'unknown' }],
    ['wrong event ordinal', { steps: [{ stepNumber: 2, description: 'Wrong ordinal' }] }],
    ['blank narration', { steps: [{ stepNumber: 1, description: ' ' }] }],
    ['missing input echo', { resolvedInput: null }],
    ['different submitted input', { resolvedInput: { n: 9 } }],
    ...[[], 7, { answer: { value: '8' } }, { calls: -1 }].map(variables => [
      `malformed debug values ${JSON.stringify(variables)}`, {
        steps: [{ stepNumber: 1, description: 'Malformed debug values', variables }]
      }
    ])
  ])('refuses %s instead of showing it as a successful comparison', async (_, invalid) => {
    fetch.mockResolvedValueOnce(ok(trace())).mockResolvedValueOnce(ok(trace(invalid)));
    const { result } = renderHook(() => useComparisonTrace('stairs', { n: 5 }, true));
    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBeTruthy();
    expect(result.current.defaultRun).toBeNull();
    expect(result.current.alternateRun).toBeNull();
  });

  it('retains truncation status and accepts defaults added by the server', async () => {
    fetch.mockResolvedValue(ok(trace({ truncated: true, resolvedInput: { n: 5, mode: 'default' } })));
    const { result } = renderHook(() => useComparisonTrace('stairs', { n: 5 }, true));
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    expect(result.current.defaultRun.truncated).toBe(true);
    expect(result.current.alternateRun.truncated).toBe(true);
    expect(result.current.alternateRun.resolvedInput).toEqual({ n: 5, mode: 'default' });
  });

  it('retires decoded bodies on retry, even when abort does not stop JSON decoding', async () => {
    const decode = [];
    fetch.mockImplementationOnce(async () => ({ ok: true, json: () => new Promise(resolve => decode.push(resolve)) }))
      .mockImplementationOnce(async () => ({ ok: true, json: () => new Promise(resolve => decode.push(resolve)) }));
    const { result } = renderHook(() => useComparisonTrace('stairs', { n: 5 }, true));
    await waitFor(() => expect(decode).toHaveLength(2));
    await act(async () => result.current.retry());
    await waitFor(() => expect(result.current.defaultRun?.steps[0].description).toBe('Actual event'));
    await act(async () => decode.forEach(resolve => resolve(trace({ steps: [{ stepNumber: 1, description: 'Retired event' }] }))));
    expect(result.current.defaultRun.steps[0].description).toBe('Actual event');
  });

  it('exposes no retained runs or pending state when disabled', async () => {
    const { result, rerender } = renderHook(({ active }) => useComparisonTrace('stairs', { n: 5 }, active), {
      initialProps: { active: true }
    });
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    rerender({ active: false });
    expect(result.current.loading).toBe(false);
    expect(result.current.defaultRun).toBeNull();
    expect(result.current.alternateRun).toBeNull();
  });

  it('retires both requests during StrictMode replay and keeps only the live pair', async () => {
    const { result } = renderHook(() => useComparisonTrace('stairs', { n: 5 }, true), { wrapper: StrictMode });
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    expect(fetch).toHaveBeenCalledTimes(4);
    expect(fetch.mock.calls.slice(0, 2).every(([, options]) => options.signal.aborted)).toBe(true);
    expect(fetch.mock.calls.slice(2).every(([, options]) => !options.signal.aborted)).toBe(true);
    expect(result.current.error).toBeNull();
  });

  it('cannot restart requests through a retained retry callback after unmount', async () => {
    const { result, unmount } = renderHook(() => useComparisonTrace('stairs', { n: 5 }, true));
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    const retry = result.current.retry;
    unmount();
    await act(async () => retry());
    expect(fetch).toHaveBeenCalledTimes(2);
  });

  it.each([
    ['problem change', { problem: 'another', approach: 'recursion', active: true }],
    ['approach change', { problem: 'stairs', approach: 'memoization', active: true }],
    ['panel close', { problem: 'stairs', approach: 'recursion', active: false }]
  ])('does not let a retained retry cross a %s', async (_, next) => {
    fetch.mockImplementation(url => {
      const parsed = new URL(url, 'https://test.invalid');
      return Promise.resolve(ok(trace({ problemId: parsed.pathname.split('/')[3],
        approachId: parsed.searchParams.get('approach') })));
    });
    const { result, rerender } = renderHook(({ problem, approach, active }) =>
      useComparisonTrace(problem, { n: 5 }, active, approach), {
      initialProps: { problem: 'stairs', approach: 'recursion', active: true }
    });
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    const retiredRetry = result.current.retry;
    rerender(next);
    if (next.active) await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    const calls = fetch.mock.calls.length;
    await act(() => retiredRetry());
    expect(fetch).toHaveBeenCalledTimes(calls);
    expect(result.current.loading).toBe(false);
    if (next.active) expect(result.current.defaultRun).not.toBeNull();
    else expect(result.current.defaultRun).toBeNull();
  });

  it.each(['problem', 'input'])('does not adopt an uncommitted %s from a suspended transition', async change => {
    const blocked = new Promise(() => {});
    const { result, rerender } = renderHook(({ problem, input, suspend }) => {
      const hook = useComparisonTrace(problem, input, true);
      if (suspend) throw blocked;
      return hook;
    }, { initialProps: { problem: 'stairs', input: { n: 5 }, suspend: false },
      wrapper: ({ children }) => createElement(Suspense, { fallback: 'Loading' }, children) });
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    const retry = result.current.retry;
    act(() => startTransition(() => rerender({ problem: change === 'problem' ? 'other' : 'stairs',
      input: { n: 6 }, suspend: true })));
    await act(() => retry());
    expect(fetch).toHaveBeenCalledTimes(4);
    expect(JSON.parse(fetch.mock.calls[3][1].body)).toEqual({ n: 5 });
    expect(result.current.alternateRun.resolvedInput).toEqual({ n: 5 });
  });

  it('clears old approach results immediately and rejects its late decoded pair', async () => {
    const decode = [];
    fetch.mockImplementation((url) => {
      const approachId = new URL(url, 'https://test.invalid').searchParams.get('approach');
      return Promise.resolve(approachId === 'recursion'
        ? { ok: true, json: () => new Promise(resolve => decode.push(resolve)) }
        : ok(trace({ approachId })));
    });
    const { result, rerender } = renderHook(({ approach }) => useComparisonTrace('stairs', { n: 5 }, true, approach), {
      initialProps: { approach: 'recursion' }
    });
    await waitFor(() => expect(decode).toHaveLength(2));
    rerender({ approach: 'memoization' });
    expect(result.current.defaultRun).toBeNull();
    await waitFor(() => expect(result.current.defaultRun).not.toBeNull());
    await act(async () => decode.forEach(resolve => resolve(trace({ approachId: 'recursion',
      steps: [{ stepNumber: 1, description: 'Retired recursion' }] }))));
    expect(result.current.defaultRun.steps[0].description).toBe('Actual event');
    expect(result.current.error).toBeNull();
  });
});
