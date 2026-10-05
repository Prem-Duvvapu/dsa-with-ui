import { act, renderHook, waitFor } from '@testing-library/react';
import { describe, expect, it, vi, beforeEach } from 'vitest';
import useComparisonTrace from './useComparisonTrace';

function execResponse(steps) {
  return { ok: true, json: () => Promise.resolve({ steps, encoding: 'full', resolvedInput: { n: steps.length } }) };
}

describe('useComparisonTrace', () => {
  beforeEach(() => {
    vi.stubGlobal('fetch', vi.fn());
  });

  it('does nothing until isActive is true, so it never fetches for a panel the learner has not opened', () => {
    renderHook(() => useComparisonTrace('two-sum', { nums: [1] }, false));
    expect(fetch).not.toHaveBeenCalled();
  });

  it('fetches the default input and the alternate input once opened, in parallel', async () => {
    fetch
      .mockResolvedValueOnce(execResponse([{ stepNumber: 1, description: 'a' }]))
      .mockResolvedValueOnce(execResponse([{ stepNumber: 1, description: 'b' }, { stepNumber: 2, description: 'c' }]));

    const { result } = renderHook(() => useComparisonTrace('two-sum', { nums: [1, 2, 3] }, true));

    await waitFor(() => expect(result.current.loading).toBe(false));

    expect(result.current.defaultRun.steps).toHaveLength(1);
    expect(result.current.alternateRun.steps).toHaveLength(2);
    expect(fetch).toHaveBeenCalledTimes(2);

    const [defaultCall, alternateCall] = fetch.mock.calls;
    expect(JSON.parse(defaultCall[1].body)).toEqual({});
    expect(JSON.parse(alternateCall[1].body)).toEqual({ nums: [1, 2, 3] });
  });

  it('reports an error if either run fails, without throwing', async () => {
    fetch
      .mockResolvedValueOnce(execResponse([{ stepNumber: 1, description: 'a' }]))
      .mockResolvedValueOnce({ ok: false, status: 500 });

    const { result } = renderHook(() => useComparisonTrace('two-sum', { nums: [1] }, true));

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.error).toBeTruthy();
    expect(result.current.defaultRun).toBeNull();
  });

  it('drops a late answer for a problem it has since moved away from', async () => {
    const slow = [];
    fetch.mockImplementation((url) => {
      if (url.includes('/old/')) return new Promise((resolve) => slow.push(() => resolve(execResponse([{ stepNumber: 1, description: 'old' }]))));
      return Promise.resolve(execResponse([{ stepNumber: 1, description: 'new' }, { stepNumber: 2, description: 'new 2' }]));
    });
    const { result, rerender } = renderHook(({ id }) => useComparisonTrace(id, { n: 1 }, true), { initialProps: { id: 'old' } });
    rerender({ id: 'new' });
    await waitFor(() => expect(result.current.defaultRun?.steps).toHaveLength(2));
    expect(slow).toHaveLength(2);
    await act(async () => {
      slow.forEach((release) => release());
      await new Promise((resolve) => setTimeout(resolve, 20));
    });
    expect(result.current.defaultRun.steps[0].description).toBe('new');
    expect(result.current.loading).toBe(false);
  });
});
