import React from 'react';
import styles from './LiveTraceTicker.module.css';

export default function LiveTraceTicker({ stepDescription }) {
  const hasStep = typeof stepDescription === 'string' && stepDescription.trim().length > 0;

  return (
    <div
      role="status"
      aria-live="polite"
      className={styles.ticker}
    >
      {hasStep && <div className={`pulse-dot ${styles.pulseDot}`} />}

      <span className={styles.label}>
        {hasStep ? 'Live trace' : 'Trace status'}
      </span>

      <span className={styles.description}>
        {hasStep ? stepDescription : 'No trace steps available.'}
      </span>
    </div>
  );
}
