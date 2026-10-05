import React, { StrictMode, useRef } from 'react';
import { render, renderHook, fireEvent } from '@testing-library/react';
import { describe, expect, it, beforeEach } from 'vitest';
import '@testing-library/jest-dom';
import useModalDialog from './useModalDialog';

/**
 * The one modal contract (focus in, Tab contained, page inert, focus back out) shared by the
 * switcher and the shortcut list. The trap half replaces useFocusTrap, whose guards are kept
 * here; the inert and fallback halves are what the switcher lacked (P5a): with only
 * aria-modal, a pointer or a screen reader's virtual cursor still reached the page behind.
 */
function setup(active, html) {
  document.body.innerHTML = html ?? `
    <button id="outside">outside</button>
    <main id="workspace-view" tabindex="-1">page</main>
    <div id="container">
      <button id="first">first</button>
      <input id="middle" />
      <button id="last">last</button>
    </div>
  `;
  const outside = document.getElementById('outside');
  outside.focus();
  const { rerender, unmount } = renderHook(
    ({ isActive }) => {
      const ref = useRef(document.getElementById('container'));
      useModalDialog(ref, isActive, { fallbackFocus: () => document.getElementById('workspace-view') });
      return ref;
    },
    { initialProps: { isActive: active } }
  );
  return { rerender, unmount, outside };
}

describe('useModalDialog', () => {
  beforeEach(() => { document.body.innerHTML = ''; });

  it('moves focus into the dialog as soon as it opens', () => {
    setup(true);
    expect(document.activeElement).toBe(document.getElementById('first'));
  });

  it('wraps Tab from the last focusable back to the first, and Shift+Tab the other way', () => {
    setup(true);
    document.getElementById('last').focus();
    fireEvent.keyDown(window, { key: 'Tab' });
    expect(document.activeElement).toBe(document.getElementById('first'));
    fireEvent.keyDown(window, { key: 'Tab', shiftKey: true });
    expect(document.activeElement).toBe(document.getElementById('last'));
  });

  it('restores focus to whatever opened it', () => {
    const { rerender, outside } = setup(true);
    rerender({ isActive: false });
    expect(document.activeElement).toBe(outside);
  });

  it('does nothing when never opened', () => {
    const { outside } = setup(false);
    expect(document.activeElement).toBe(outside);
    expect(document.querySelector('[inert]')).toBeNull();
  });

  it('makes the page behind it inert, and releases it on close', () => {
    const { rerender } = setup(true);
    expect(document.getElementById('outside')).toHaveAttribute('inert');
    expect(document.getElementById('workspace-view')).toHaveAttribute('inert');
    expect(document.getElementById('container')).not.toHaveAttribute('inert');
    rerender({ isActive: false });
    expect(document.querySelector('[inert]')).toBeNull();
  });

  it('releases the page when the dialog unmounts without closing (a route change)', () => {
    const { unmount } = setup(true);
    unmount();
    expect(document.querySelector('[inert]')).toBeNull();
  });

  it('leaves alone an element that was already inert', () => {
    const { rerender } = setup(true, `
      <button id="outside">outside</button>
      <aside id="mine" inert>someone else's</aside>
      <div id="container"><button id="first">first</button></div>
    `);
    rerender({ isActive: false });
    expect(document.getElementById('mine')).toHaveAttribute('inert');
    expect(document.getElementById('outside')).not.toHaveAttribute('inert');
  });

  it("returns focus to a closed menu's summary when the opener was inside it", () => {
    const { rerender } = setup(true, `
      <details id="menu" open><summary id="menu-summary">Menu</summary><button id="outside">Switch</button></details>
      <div id="container"><button id="first">first</button></div>
    `);
    document.getElementById('menu').open = false;
    rerender({ isActive: false });
    expect(document.activeElement).toBe(document.getElementById('menu-summary'));
  });

  it('falls back to the page when the opener is gone', () => {
    const { rerender } = setup(true);
    document.getElementById('outside').remove();
    rerender({ isActive: false });
    expect(document.activeElement).toBe(document.getElementById('workspace-view'));
  });

  it('leaves nothing inert after StrictMode runs its effects twice', () => {
    document.body.innerHTML = '<button id="outside">outside</button><div id="host"></div>';
    function Dialog({ open }) {
      const ref = useRef(null);
      useModalDialog(ref, open);
      return open ? <div ref={ref}><button>inside</button></div> : null;
    }
    const host = document.getElementById('host');
    const { rerender } = render(<StrictMode><Dialog open /></StrictMode>, { container: host });
    expect(document.getElementById('outside')).toHaveAttribute('inert');
    rerender(<StrictMode><Dialog open={false} /></StrictMode>);
    expect(document.querySelector('[inert]')).toBeNull();
  });
});
