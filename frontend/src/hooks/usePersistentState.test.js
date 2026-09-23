import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import usePersistentState from './usePersistentState';

describe('usePersistentState', () => {
  beforeEach(() => window.localStorage.clear());

  it('starts from the default when nothing is stored', () => {
    const { result } = renderHook(() => usePersistentState('speed', 1000));
    expect(result.current[0]).toBe(1000);
  });

  it('restores a stored value on the next mount', () => {
    const first = renderHook(() => usePersistentState('speed', 1000));
    act(() => first.result.current[1](250));
    const second = renderHook(() => usePersistentState('speed', 1000));
    expect(second.result.current[0]).toBe(250);
  });

  it('falls back to the default when the stored value fails validation', () => {
    // A preference written by an older build can name a preset this version no longer
    // renders. Honouring it would pin the UI into a state the user cannot click out of.
    window.localStorage.setItem('dsa-ui:speed', JSON.stringify(777));
    const { result } = renderHook(() =>
      usePersistentState('speed', 1000, (v) => [2000, 1000, 500, 250].includes(v)));
    expect(result.current[0]).toBe(1000);
  });

  it('falls back to the default when the stored value is corrupt', () => {
    window.localStorage.setItem('dsa-ui:speed', 'not json');
    const { result } = renderHook(() => usePersistentState('speed', 1000));
    expect(result.current[0]).toBe(1000);
  });

  it('namespaces its keys so it cannot collide with another app on the origin', () => {
    const { result } = renderHook(() => usePersistentState('sidebarOpen', true));
    act(() => result.current[1](false));
    expect(window.localStorage.getItem('dsa-ui:sidebarOpen')).toBe('false');
    expect(window.localStorage.getItem('sidebarOpen')).toBeNull();
  });
})
