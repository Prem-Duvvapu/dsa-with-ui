import { renderHook, waitFor } from '@testing-library/react';
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
});
