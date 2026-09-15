import { renderHook } from '@testing-library/react';
import { fireEvent } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import { useRef } from 'react';
import useFocusTrap from './useFocusTrap';

/**
 * Extracted from the mobile drawer, which had this logic inline and correct - Tab
 * wrapping, initial focus, and restoring focus to whatever had it before the trap opened.
 * A second modal (the command palette) needing the exact same behaviour is what makes it
 * worth pulling out, not a hypothetical: duplicating a focus trap is how one of the two
 * copies quietly stops matching the other.
 */
function setup(active) {
  document.body.innerHTML = `
    <button id="outside">outside</button>
    <div id="container">
      <button id="first">first</button>
      <input id="middle" />
      <button id="last">last</button>
    </div>
  `;
  const outside = document.getElementById('outside');
  outside.focus();

  const { result, rerender } = renderHook(
    ({ isActive }) => {
      const ref = useRef(document.getElementById('container'));
      useFocusTrap(ref, isActive);
      return ref;
    },
    { initialProps: { isActive: active } }
  );
  return { rerender, outside };
}

describe('useFocusTrap', () => {
  beforeEach(() => {
    document.body.innerHTML = '';
  });

  it('moves focus into the container as soon as it activates', () => {
    setup(true);
    expect(document.activeElement).toBe(document.getElementById('first'));
  });

  it('wraps Tab from the last focusable back to the first', () => {
    setup(true);
    document.getElementById('last').focus();
    fireEvent.keyDown(window, { key: 'Tab' });
    expect(document.activeElement).toBe(document.getElementById('first'));
  });

  it('wraps Shift+Tab from the first focusable back to the last', () => {
    setup(true);
    document.getElementById('first').focus();
    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(document.getElementById('last'));
  });

  it('restores focus to whatever had it before the trap activated', () => {
    const { rerender, outside } = setup(true);
    rerender({ isActive: false });
    expect(document.activeElement).toBe(outside);
  });

  it('does nothing when never activated', () => {
    const { outside } = setup(false);
    expect(document.activeElement).toBe(outside);
  });
});
