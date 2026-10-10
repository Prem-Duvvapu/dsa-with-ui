import React from 'react';
import DpTableCanvas from '../components/DpTableCanvas';
import styles from './ProblemWorkspace.module.css';

/** A truthful secondary cache view; closed by default so the recursion tree keeps the room. */
export default function MemoTable({ step, steps = [], dsType }) {
  if (dsType !== 'RecursionTree' || !steps.some(s => s?.dpTable?.cells?.length)) return null;
  return (
    <details className={styles.history}>
      <summary>Memo table</summary>
      <p className={styles.meta}>▫ Unknown · □ Known · ○ Cache read · ▼ Stored this step. Unknown is not zero.</p>
      {typeof step?.variables?.memoSlice === 'string' && <p className={styles.meta}>
        Showing memo slice: {step.variables.memoSlice}. Other slices remain cached.
      </p>}
      <DpTableCanvas step={step} />
    </details>
  );
}
