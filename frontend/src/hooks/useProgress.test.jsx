import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import useProgress from './useProgress';

const KEY = 'dsa-ui:progress';

describe('useProgress', () => {
  beforeEach(() => window.localStorage.clear());

  it('remembers what was watched across a reload', () => {
    const first = renderHook(() => useProgress());
    act(() => first.result.current.markWatched('kadane-algo'));
    expect(first.result.current.watchedCount).toBe(1);

    const second = renderHook(() => useProgress());
    expect(second.result.current.progress['kadane-algo'].watched).toBe(true);
  });

  it('toggles a star and forgets the problem entirely when nothing is left to remember', () => {
    // Otherwise every problem ever starred-then-unstarred accumulates a dead entry, and the
    // stored object grows without bound for someone browsing the catalogue.
    const { result } = renderHook(() => useProgress());
    act(() => result.current.toggleStar('two-sum'));
    expect(result.current.starredCount).toBe(1);

    act(() => result.current.toggleStar('two-sum'));
    expect(result.current.progress['two-sum']).toBeUndefined();
  });

  it('keeps the watched flag when a star is removed', () => {
    const { result } = renderHook(() => useProgress());
    act(() => result.current.markWatched('two-sum'));
    act(() => result.current.toggleStar('two-sum'));
    act(() => result.current.toggleStar('two-sum'));
    expect(result.current.progress['two-sum']).toEqual({ watched: true, starred: false });
  });

  it('discards a corrupted or foreign stored shape instead of half-applying it', () => {
    window.localStorage.setItem(KEY, '{"a": 5, "b": [1,2], "c": {"watched": true}}');
    const { result } = renderHook(() => useProgress());
    expect(result.current.progress).toEqual({ c: { watched: true, starred: false } });

    window.localStorage.setItem(KEY, 'not json');
    expect(renderHook(() => useProgress()).result.current.progress).toEqual({});
  });

  it('marking watched twice does not rewrite state', () => {
    const { result } = renderHook(() => useProgress());
    act(() => result.current.markWatched('x'));
    const before = result.current.progress;
    act(() => result.current.markWatched('x'));
    expect(result.current.progress).toBe(before);
  });
});
