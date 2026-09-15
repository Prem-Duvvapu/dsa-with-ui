import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import useStreak from './useStreak';

describe('useStreak', () => {
  beforeEach(() => window.localStorage.clear());

  it('starts at 0 with nothing recorded', () => {
    const { result } = renderHook(() => useStreak('2026-09-15'));
    expect(result.current.current).toBe(0);
  });

  it('becomes 1 after the first visit is recorded', () => {
    const { result } = renderHook(() => useStreak('2026-09-15'));
    act(() => result.current.recordVisit());
    expect(result.current.current).toBe(1);
  });

  it('recording twice on the same day does not double-count', () => {
    const { result } = renderHook(() => useStreak('2026-09-15'));
    act(() => result.current.recordVisit());
    act(() => result.current.recordVisit());
    expect(result.current.current).toBe(1);
  });

  it('extends the streak on the very next calendar day', () => {
    const { result, rerender } = renderHook(({ today }) => useStreak(today), {
      initialProps: { today: '2026-09-15' }
    });
    act(() => result.current.recordVisit());

    rerender({ today: '2026-09-16' });
    act(() => result.current.recordVisit());

    expect(result.current.current).toBe(2);
  });

  it('resets to 1 after a skipped day', () => {
    const { result, rerender } = renderHook(({ today }) => useStreak(today), {
      initialProps: { today: '2026-09-15' }
    });
    act(() => result.current.recordVisit());

    rerender({ today: '2026-09-20' });
    act(() => result.current.recordVisit());

    expect(result.current.current).toBe(1);
  });

  it('tracks the longest streak even after a reset', () => {
    const { result, rerender } = renderHook(({ today }) => useStreak(today), {
      initialProps: { today: '2026-09-15' }
    });
    act(() => result.current.recordVisit());
    rerender({ today: '2026-09-16' });
    act(() => result.current.recordVisit());
    rerender({ today: '2026-09-17' });
    act(() => result.current.recordVisit());

    rerender({ today: '2026-09-25' });
    act(() => result.current.recordVisit());

    expect(result.current.current).toBe(1);
    expect(result.current.longest).toBe(3);
  });
});
