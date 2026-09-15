import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach, afterEach } from 'vitest';
import useLayoutPreferences from './useLayoutPreferences';

const ORIGINAL_WIDTH = window.innerWidth;
const setWidth = (value) =>
  Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value });

describe('useLayoutPreferences', () => {
  beforeEach(() => window.localStorage.clear());
  afterEach(() => setWidth(ORIGINAL_WIDTH));

  it('remembers the desktop panel preferences across a reload', () => {
    setWidth(1200);
    const first = renderHook(() => useLayoutPreferences());
    act(() => first.result.current.setIsComplexityOpen(true));
    act(() => first.result.current.setIsStatementOpen(false));

    const second = renderHook(() => useLayoutPreferences());
    expect(second.result.current.isComplexityOpen).toBe(true);
    expect(second.result.current.isStatementOpen).toBe(false);
  });

  it('never restores an open sidebar on a narrow viewport', () => {
    // The rule this hook exists to hold. On mobile the sidebar is a modal drawer over the
    // canvas; restoring "open" from a desktop session would greet a phone user with the
    // drawer covering the thing they came to watch.
    setWidth(1200);
    const desktop = renderHook(() => useLayoutPreferences());
    act(() => desktop.result.current.setIsSidebarOpen(true));
    expect(window.localStorage.getItem('dsa-ui:sidebarOpen')).toBe('true');

    setWidth(480);
    const mobile = renderHook(() => useLayoutPreferences());
    expect(mobile.result.current.isMobile).toBe(true);
    expect(mobile.result.current.isSidebarOpen).toBe(false);
  });

  it('does not let the mobile drawer overwrite the desktop preference', () => {
    // Opening the drawer on a phone is not a statement about how the desktop workspace
    // should look. The stored value is the desktop one and the drawer must not touch it.
    setWidth(1200);
    const desktop = renderHook(() => useLayoutPreferences());
    act(() => desktop.result.current.setIsSidebarOpen(false));
    expect(window.localStorage.getItem('dsa-ui:sidebarOpen')).toBe('false');

    setWidth(480);
    const mobile = renderHook(() => useLayoutPreferences());
    act(() => mobile.result.current.setIsSidebarOpen(true));
    expect(mobile.result.current.isSidebarOpen).toBe(true);
    expect(window.localStorage.getItem('dsa-ui:sidebarOpen'))
      .toBe('false');

    // And back on a wide viewport, the collapsed desktop choice is still there.
    setWidth(1200);
    const back = renderHook(() => useLayoutPreferences());
    expect(back.result.current.isSidebarOpen).toBe(false);
  });

  it('reports the viewport it is deciding from', () => {
    setWidth(1024);
    const { result } = renderHook(() => useLayoutPreferences());
    expect(result.current.viewportWidth).toBe(1024);
    expect(result.current.isMobile).toBe(false);
  });
});
