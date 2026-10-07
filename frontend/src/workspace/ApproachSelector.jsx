import React, { useId } from 'react';
import styles from './ProblemWorkspace.module.css';

/** Preparing is not running. All displayed-run surfaces remain on the previous executable. */
export default function ApproachSelector({ session, onRun }) {
  const controlId = useId();
  const { approaches, selectedApproach, shownApproach, selectApproach, run, pending, loading, restoring, draft } = session;
  if (approaches.length <= 1 || !selectedApproach) return null;
  const differs = run.approachId !== selectedApproach.id;
  const shown = run.id > 0 ? run.approachLabel ?? shownApproach?.label : null;
  return (
    <div className={styles.approachSetup}>
      <div className={styles.approachRow}>
        <label htmlFor={controlId}>Solution approach</label>
        <select id={controlId} className={styles.approachSelect} value={selectedApproach.id}
          disabled={loading || pending || restoring} onChange={event => selectApproach(event.target.value)}>
          {approaches.map(a => <option key={a.id} value={a.id}>{a.label}</option>)}
        </select>
        {differs && <button type="button" className={styles.control} disabled={loading || pending || restoring}
          onClick={() => onRun(draft.values ?? {})}>Run {selectedApproach.label.toLowerCase()}</button>}
        <span role="status" aria-label="Solution approach" className={styles.meta}>
          {differs ? `Selected: ${selectedApproach.label}; showing: ${shown ?? 'no run yet'}.`
            : `Showing: ${shown ?? 'no run yet'}.`}
        </span>
      </div>
      <details className={styles.approachHelp}>
        <summary>About {selectedApproach.label.toLowerCase()}</summary>
        <p>{selectedApproach.summary}</p>
        <p>Selection pauses playback; only a successful run changes the result and share link.
          Visualizer input limits may differ by approach. Your input is never silently shortened.</p>
      </details>
    </div>
  );
}
