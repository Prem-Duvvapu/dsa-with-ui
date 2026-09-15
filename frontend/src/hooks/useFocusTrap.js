import { useEffect } from 'react';

const FOCUSABLE_SELECTOR = [
  'a[href]', 'button:not([disabled])', 'input:not([disabled])',
  'select:not([disabled])', 'textarea:not([disabled])', '[tabindex]:not([tabindex="-1"])'
].join(',');

/**
 * Extracted from the mobile drawer, which had this Tab-wrapping and
 * restore-on-close logic inline. The command palette needs the identical
 * behaviour, and a second hand-rolled copy is how the two quietly drift apart.
 */
export default function useFocusTrap(containerRef, isActive) {
  useEffect(() => {
    if (!isActive) return undefined;

    const container = containerRef.current;
    if (!container) return undefined;

    const previouslyFocused = document.activeElement;
    const focusable = () => Array.from(container.querySelectorAll(FOCUSABLE_SELECTOR));

    const first = focusable()[0];
    if (first) first.focus();

    const onKeyDown = (event) => {
      if (event.key !== 'Tab') return;
      const items = focusable();
      if (!items.length) return;
      const [firstItem, lastItem] = [items[0], items[items.length - 1]];

      if (event.shiftKey && document.activeElement === firstItem) {
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
      if (previouslyFocused && previouslyFocused.focus) previouslyFocused.focus();
    };
  }, [isActive, containerRef]);
}
