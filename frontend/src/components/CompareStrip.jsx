import React, { useState } from 'react';
import { ChevronLeft, ChevronRight, RefreshCw } from 'lucide-react';
import useComparisonTrace from '../hooks/useComparisonTrace';
import CaptureStrip from './CaptureStrip';
import InputSummary from './InputSummary';
import styles from './CompareStrip.module.css';

/** One side of the comparison: its own input, its own position and that step's narration. */
function CompareSide({ label, run, dsType, showCapture }) {
  const [current, setCurrent] = useState(0);
  const total = run.steps.length;
  const index = Math.min(current, Math.max(0, total - 1));
  const seek = (next) => setCurrent(Math.max(0, Math.min(total - 1, next)));

  return (
    <section className={styles.row} aria-label={label}>
      <h3 className={styles.rowLabel}>
        {label} &mdash; {total} step{total === 1 ? '' : 's'}
      </h3>
      <InputSummary resolvedInput={run.resolvedInput} label="Input" />
      {showCapture && (
        <CaptureStrip steps={run.steps} current={index} dsType={dsType} onSeek={seek} resolvedInput={run.resolvedInput} />
      )}
      {total > 0 ? (
        <>
          <div className={styles.position}>
            <button type="button" className={styles.control} onClick={() => seek(index - 1)} disabled={index === 0}
              aria-label={`${label}: previous step`}>
              <ChevronLeft size={16} aria-hidden="true" />
            </button>
            <input
              type="range" min={1} max={total} value={index + 1}
              onChange={(event) => seek(Number(event.target.value) - 1)}
              aria-label={`${label}: step`} aria-valuetext={`Step ${index + 1} of ${total}`}
              className={styles.slider}
            />
            <button type="button" className={styles.control} onClick={() => seek(index + 1)} disabled={index >= total - 1}
              aria-label={`${label}: next step`}>
              <ChevronRight size={16} aria-hidden="true" />
            </button>
            <span className={styles.counter}>Step {index + 1} of {total}</span>
          </div>
          <p className={styles.narration}>{run.steps[index]?.description || 'No narration for this step.'}</p>
        </>
      ) : (
        <p className={styles.status}>This run has no steps.</p>
      )}
    </section>
  );
}

/**
 * The problem's default run and its tracer-declared other case, fetched independently when
 * the panel opens and each read at its own position. It never reuses or replaces the main run:
 * the session above keeps its own input, step and draft.
 *
 * Every tracer guarantees a materially different alternateInput
 * (TracerContractTest.alternateInputDiffersFromDefaults), so this needs no per-problem setup.
 * Families with a capture strip get it on each side; for the others (graph, tree, DP, whose
 * diagram is the whole run) each side is read as text - its narration at a chosen step - rather
 * than the comparison being withheld.
 *
 * Step numbers are positions in each run. Step 5 here and step 5 there need not be the same
 * moment of the algorithm, and the panel says so.
 */
export default function CompareStrip({ problemId, dsType, alternateInput, showCapture = true, mainIsCustom = false }) {
  const { loading, error, defaultRun, alternateRun, retry } = useComparisonTrace(problemId, alternateInput, true);

  const scope = (
    <p className={styles.scope}>
      Compares this problem&apos;s default input with its other case, each run separately.
      {mainIsCustom && ' The run above uses your own input and is not part of this comparison.'}
      {' '}Step numbers are positions within each run, not matching moments of the algorithm.
    </p>
  );

  if (loading) {
    return <div className={styles.wrap}>{scope}<p className={styles.status} role="status">Loading both runs to compare…</p></div>;
  }

  if (error) {
    return (
      <div className={styles.wrap}>
        {scope}
        <div className={styles.errorBox} role="alert">
          <p>{error}</p>
          <button type="button" onClick={retry} className="btn btn-outline">
            <RefreshCw size={13} /> Retry
          </button>
        </div>
      </div>
    );
  }

  if (!defaultRun || !alternateRun) return null;

  return (
    <div className={styles.wrap} aria-label="Compare the default input against the other case" role="group">
      {scope}
      <CompareSide label="Default input" run={defaultRun} dsType={dsType} showCapture={showCapture} />
      <CompareSide label="Other case" run={alternateRun} dsType={dsType} showCapture={showCapture} />
    </div>
  );
}
