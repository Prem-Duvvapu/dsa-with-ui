import React, { useId } from 'react';
import { defaultInput } from '../input/randomizeInput';
import { inputLimitWarnings } from '../input/inputLimitWarnings';
import styles from './ProblemWorkspace.module.css';

/** Preparing is not running. All displayed-run surfaces remain on the previous executable. */
export default function ApproachSelector({ session, onRun, onEditInput }) {
  const controlId = useId();
  const limitsId = useId();
  const { approaches, selectedApproach, shownApproach, selectApproach, run, pending, loading, restoring, draft } = session;
  if (approaches.length <= 1 || !selectedApproach) return null;
  const differs = run.approachId !== selectedApproach.id;
  const shown = run.id > 0 ? run.approachLabel ?? shownApproach?.label : null;
  const warnings = inputLimitWarnings(selectedApproach.inputSpec, draft.values);
  const busy = loading || pending || restoring;
  return (
    <div className={styles.approachSetup}>
      <div className={styles.approachRow}>
        <label htmlFor={controlId}>Solution approach</label>
        <select id={controlId} className={styles.approachSelect} value={selectedApproach.id}
          disabled={busy} aria-describedby={warnings.length ? limitsId : undefined}
          onChange={event => selectApproach(event.target.value)}>
          {approaches.map(a => <option key={a.id} value={a.id}>{a.label}</option>)}
        </select>
        {differs && <button type="button" className={styles.control} disabled={busy}
          aria-describedby={warnings.length ? limitsId : undefined}
          onClick={() => onRun(draft.values ?? {})}>Run {selectedApproach.label.toLowerCase()}</button>}
        <span role="status" aria-label="Solution approach" className={styles.meta}>
          {differs ? `Selected: ${selectedApproach.label}; showing: ${shown ?? 'no run yet'}.`
            : `Showing: ${shown ?? 'no run yet'}.`}
        </span>
      </div>
      {warnings.length > 0 && <div className={`${styles.notice} ${styles.approachLimits}`}>
        <div id={limitsId} role="status" aria-label="Input limits">
          <p>Input does not fit {selectedApproach.label} visualizer limits.</p>
          <ul>{warnings.map((warning, index) => <li key={`${warning.field}:${index}`}>{warning.message}</li>)}</ul>
          <p>Your input is preserved. Adjust it, or explicitly replace it with this approach’s example. Neither action runs it.</p>
        </div>
        <button type="button" className={styles.control} disabled={busy} onClick={onEditInput}>Adjust input</button>
        <button type="button" className={styles.control} disabled={busy} onClick={() => {
          draft.replace(defaultInput(selectedApproach.inputSpec));
          onEditInput();
        }}>Replace input with {selectedApproach.label.toLowerCase()} example</button>
      </div>}
      <details className={styles.approachHelp}>
        <summary>About {selectedApproach.label.toLowerCase()}</summary>
        <p>{selectedApproach.summary}</p>
        <p>Selection pauses playback; only a successful run changes the result and share link.
          Visualizer input limits may differ by approach. Your input is never silently shortened.</p>
      </details>
    </div>
  );
}
