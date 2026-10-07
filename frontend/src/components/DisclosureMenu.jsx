import React, { useEffect, useRef } from 'react';

/** A native disclosure: Tab stays native; Escape, blur or an outside click dismisses it. */
export default function DisclosureMenu({ summary, summaryLabel, className, summaryClassName, children }) {
  const ref = useRef(null);
  useEffect(() => {
    const outside = (event) => {
      if (ref.current?.open && !ref.current.contains(event.target)) ref.current.open = false;
    };
    document.addEventListener('pointerdown', outside);
    return () => document.removeEventListener('pointerdown', outside);
  }, []);
  return (
    <details ref={ref} className={className}
      onBlur={(event) => { if (!event.currentTarget.contains(event.relatedTarget)) event.currentTarget.open = false; }}
      onKeyDown={(event) => {
        // Native navigation inside an open disclosure belongs to that disclosure,
        // not to the document-level player. Do not prevent the browser's action.
        if (event.currentTarget.open && ['ArrowLeft', 'ArrowRight', 'ArrowUp', 'ArrowDown', 'Home', 'End', 'Space', 'Enter'].includes(event.code)) {
          event.stopPropagation();
          return;
        }
        if (event.key !== 'Escape' || !event.currentTarget.open) return;
        event.preventDefault();
        event.currentTarget.open = false;
        event.currentTarget.querySelector('summary')?.focus();
      }}>
      <summary className={summaryClassName} aria-label={summaryLabel}>{summary}</summary>
      {children}
    </details>
  );
}
