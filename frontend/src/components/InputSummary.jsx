import React from 'react';
import styles from './InputSummary.module.css';

/** A compact, readable rendering of one input value. */
function formatValue(value) {
  if (value === null || value === undefined) return '—';
  if (Array.isArray(value)) {
    // A grid renders as rows so the shape survives; a flat array stays on one line.
    if (value.length > 0 && Array.isArray(value[0])) {
      return value.map((row) => `[${row.join(', ')}]`).join(' ');
    }
    return `[${value.join(', ')}]`;
  }
  if (typeof value === 'object') return JSON.stringify(value);
  return String(value);
}

/**
 * States what the trace is running on, without keeping the input editor on screen.
 *
 * The editor is the right shape for setting up a run and the wrong shape for watching one:
 * it holds a third of a fixed 340px row to offer editing you are not doing, while the one
 * thing you do want from it — what the animation is actually running on — takes a line.
 *
 * `resolvedInput` is the server's own echo of what it ran, so this cannot drift from the
 * trace the way re-rendering the editor's local form state could.
 *
 * Display only, deliberately. The control that opens the editor lives in the bottom bar and
 * is keyed off the problem having an inputSpec, not off this echo arriving - otherwise an
 * offline or legacy trace with no resolvedInput would hide the summary AND the only way
 * back to the editor.
 */
export default function InputSummary({ resolvedInput }) {
  const entries = Object.entries(resolvedInput ?? {});
  if (entries.length === 0) return null;

  return (
    <div className={styles.container} data-testid="input-summary">
      <span className={styles.label}>Running on</span>

      <div className={styles.values}>
        {entries.map(([name, value]) => (
          <span key={name} className={styles.entry}>
            <span className={styles.name}>{name}</span>
            <span className={styles.value} title={formatValue(value)}>{formatValue(value)}</span>
          </span>
        ))}
      </div>
    </div>
  );
}
