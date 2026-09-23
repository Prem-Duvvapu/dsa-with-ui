import React, { useState } from 'react';
import { RefreshCw } from 'lucide-react';
import useComparisonTrace from '../hooks/useComparisonTrace';
import CaptureStrip from './CaptureStrip';
import styles from './CompareStrip.module.css';

/**
 * The default run and the tracer's own alternate run, stacked, each with its own
 * independent scrubber. Every one of the 431 problems already guarantees a materially
 * different alternateInput (TracerContractTest.alternateInputDiffersFromDefaults), so
 * this needs no per-problem configuration - it reuses that contract rather than trying
 * to guess a "bigger" input for an arbitrary problem, which would need per-field-type
 * scaling logic this first pass deliberately skips.
 */
export default function CompareStrip({ problemId, dsType, alternateInput }) {
  const { loading, error, defaultRun, alternateRun, retry } = useComparisonTrace(problemId, alternateInput, true);
  const [currentDefault, setCurrentDefault] = useState(0);
  const [currentAlternate, setCurrentAlternate] = useState(0);

  if (loading) {
    return <p className={styles.status}>Loading both runs to compare…</p>;
  }

  if (error) {
    return (
      <div className={styles.errorBox} role="alert">
        <p>{error}</p>
        <button type="button" onClick={retry} className="btn btn-outline">
          <RefreshCw size={13} /> Retry
        </button>
      </div>
    );
  }

  if (!defaultRun || !alternateRun) return null;

  return (
    <section className={styles.wrap} aria-label="Compare the default input against the other case">
      <div className={styles.row}>
        <p className={styles.rowLabel}>
          Default input &mdash; {defaultRun.steps.length} step{defaultRun.steps.length === 1 ? '' : 's'}
        </p>
        <CaptureStrip
          steps={defaultRun.steps}
          current={currentDefault}
          dsType={dsType}
          onSeek={setCurrentDefault}
          resolvedInput={defaultRun.resolvedInput}
        />
      </div>
      <div className={styles.row}>
        <p className={styles.rowLabel}>
          Other case &mdash; {alternateRun.steps.length} step{alternateRun.steps.length === 1 ? '' : 's'}
        </p>
        <CaptureStrip
          steps={alternateRun.steps}
          current={currentAlternate}
          dsType={dsType}
          onSeek={setCurrentAlternate}
          resolvedInput={alternateRun.resolvedInput}
        />
      </div>
    </section>
  );
}
