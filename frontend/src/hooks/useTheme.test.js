import { renderHook, act } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import useTheme from './useTheme';

describe('useTheme', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
  });

  it('starts on system and stamps nothing', () => {
    // "system" must leave the attribute off entirely. Stamping light or dark here would
    // defeat index.css's prefers-color-scheme block and freeze a choice nobody made.
    const { result } = renderHook(() => useTheme());
    expect(result.current.theme).toBe('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('stamps an explicit choice onto the root element', () => {
    const { result } = renderHook(() => useTheme());
    act(() => result.current.setTheme('light'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('light');
    act(() => result.current.setTheme('dark'));
    expect(document.documentElement.getAttribute('data-theme')).toBe('dark');
  });

  it('cycles system -> light -> dark -> system so one control reaches all three', () => {
    const { result } = renderHook(() => useTheme());
    act(() => result.current.cycleTheme());
    expect(result.current.theme).toBe('light');
    act(() => result.current.cycleTheme());
    expect(result.current.theme).toBe('dark');
    act(() => result.current.cycleTheme());
    expect(result.current.theme).toBe('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('remembers the choice across a reload', () => {
    const first = renderHook(() => useTheme());
    act(() => first.result.current.setTheme('light'));
    const second = renderHook(() => useTheme());
    expect(second.result.current.theme).toBe('light');
  });

  it('ignores a stored value that is not a real theme', () => {
    window.localStorage.setItem('dsa-ui:theme', JSON.stringify('solarized'));
    const { result } = renderHook(() => useTheme());
    expect(result.current.theme).toBe('system');
  });
});
