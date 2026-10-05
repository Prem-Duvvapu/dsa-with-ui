import { useEffect } from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]', 'button:not([disabled])', 'input:not([disabled])',
  'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])'
].join(',');

/** True when an element can take focus where the reader can see it. */
function isReachable(element) {
  if (!element || !element.isConnected || typeof element.focus !== 'function') return false;
  if (element === document.body) return false;
  if (element.closest('[hidden], [inert]')) return false;
  // Inside a closed <details>, only its own <summary> is rendered.
  const closed = element.closest('details:not([open])');
  if (closed && element.closest('summary')?.parentElement !== closed) return false;
  return true;
}

/**
 * Marks everything outside `root` inert: each sibling of root and of each of its ancestors up
 * to <body>. Returns the elements it marked, so it can release exactly those - an element
 * that was already inert (another dialog's work, or the page's own) is left as it was.
 */
function inertOutside(root) {
  const marked = [];
  for (let node = root; node && node.parentElement && node !== document.body; node = node.parentElement) {
    for (const sibling of node.parentElement.children) {
      if (sibling === node || sibling.hasAttribute('inert') || ['SCRIPT', 'STYLE', 'LINK'].includes(sibling.tagName)) continue;
      sibling.setAttribute('inert', '');
      marked.push(sibling);
    }
  }
  return marked;
}

/**
 * The modal contract every dialog in the workspace shares (IMPLEMENTATION_HANDOFF.md 4.7):
 * focus moves into the dialog, Tab cannot leave it, the page behind it is inert (so a pointer
 * or a screen reader cannot reach it either), and on close focus returns to whatever opened
 * it - or, when that control has gone (a phone Menu that closed, a page that re-rendered),
 * to a visible fallback instead of being dropped on <body>.
 *
 * `rootRef` is the dialog's outermost element (the backdrop); `initialFocusRef` is optional,
 * otherwise the first focusable element inside the root takes focus. `fallbackFocus` returns
 * the element to focus when the opener cannot be focused.
 *
 * Cleanup is idempotent and runs on close AND unmount, so a route change or StrictMode's
 * double effect never leaves the page inert.
 */
export default function useModalDialog(rootRef, isOpen, { initialFocusRef, fallbackFocus } = {}) {
  useEffect(() => {
    if (!isOpen) return undefined;
    const root = rootRef.current;
    if (!root) return undefined;

    const opener = document.activeElement;
    const marked = inertOutside(root);
    const focusable = () => Array.from(root.querySelectorAll(FOCUSABLE_SELECTOR));

    const first = initialFocusRef?.current ?? focusable()[0];
    first?.focus();

    const onKeyDown = (event) => {
      if (event.key !== 'Tab') return;
      const items = focusable();
      if (!items.length) { event.preventDefault(); return; }
      const [firstItem, lastItem] = [items[0], items[items.length - 1]];
      if (!root.contains(document.activeElement)) {
        event.preventDefault();
        firstItem.focus();
      } else if (event.shiftKey && document.activeElement === firstItem) {
        event.preventDefault();
        lastItem.focus();
      } else if (!event.shiftKey && document.activeElement === lastItem) {
        event.preventDefault();
        firstItem.focus();
      }
    };
    window.addEventListener('keydown', onKeyDown);

    return () => {
      window.removeEventListener('keydown', onKeyDown);
      // Release first: an inert element cannot take focus.
      marked.forEach((element) => element.removeAttribute('inert'));
      // A dialog that navigated away (a problem chosen) leaves focus to the new page.
      if (root.contains(document.activeElement) || document.activeElement === document.body || !document.activeElement) {
        if (isReachable(opener)) {
          opener.focus();
        } else {
          const summary = opener?.isConnected ? opener.closest('details:not([open])')?.querySelector('summary') : null;
          const fallback = (summary && isReachable(summary) && summary) || fallbackFocus?.();
          if (fallback && isReachable(fallback)) fallback.focus();
        }
      }
    };
    // initialFocusRef and fallbackFocus are read when the dialog opens/closes, not tracked.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isOpen, rootRef]);
}
