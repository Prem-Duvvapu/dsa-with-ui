import { renderHook } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import useLastVisited from './useLastVisited';

describe('useLastVisited', () => {
  beforeEach(() => window.localStorage.clear());

  it('is null when nothing has ever been visited', () => {
    const { result } = renderHook(() => useLastVisited());
    expect(result.current).toBeNull();
  });

  it('records a problem id passed to it, visible to a later reader', () => {
    renderHook(() => useLastVisited('two-sum'));

    const { result } = renderHook(() => useLastVisited());
    expect(result.current).toBe('two-sum');
  });

  it('updates when called again with a different id', () => {
    const { rerender } = renderHook(({ id }) => useLastVisited(id), { initialProps: { id: 'two-sum' } });
    rerender({ id: 'kadane-algo' });

    const { result } = renderHook(() => useLastVisited());
    expect(result.current).toBe('kadane-algo');
  });

  it('a read-only call (no id) never overwrites what was already recorded', () => {
    renderHook(() => useLastVisited('two-sum'));
    renderHook(() => useLastVisited());

    const { result } = renderHook(() => useLastVisited());
    expect(result.current).toBe('two-sum');
  });
});
