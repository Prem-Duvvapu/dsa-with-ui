import React from 'react';
import styles from './ProblemWorkspace.module.css';

export function hasApproachTeaching(approach) {
  const teaching = approach?.teaching;
  const text = value => typeof value === 'string' && Boolean(value.trim());
  const notes = value => value && typeof value === 'object' && !Array.isArray(value)
    && Object.entries(value).every(([key, note]) => text(key) && text(note));
  return Boolean(teaching && ['state', 'baseCases', 'recurrence', 'evaluationOrder'].every(key => text(teaching[key]))
    && (teaching.cacheKey == null || text(teaching.cacheKey)) && notes(teaching.eventNotes) && notes(teaching.anchorNotes));
}

/** Registry-owned explanations; current-event copy is keyed to emitted fields, never narration. */
export default function ApproachTeaching({ approach, step, anchors }) {
  const teaching = approach?.teaching;
  if (!hasApproachTeaching(approach)) return null;
  const event = step?.variables?.event;
  const authored = (notes, key) => typeof key === 'string' && notes && Object.hasOwn(notes, key)
    && typeof notes[key] === 'string' ? notes[key] : null;
  // Multiple source anchors may alias one physical line. Do not pick a conflicting
  // explanation by object insertion order when the trace identifies only that line.
  const anchorNotes = [...new Set(Object.entries(anchors ?? {}).filter(([, line]) => line === step?.activeLine)
    .map(([key]) => authored(teaching.anchorNotes, key)).filter(note => note != null))];
  const note = event != null ? authored(teaching.eventNotes, event)
    : anchorNotes.length === 1 ? anchorNotes[0] : null;
  return <details className={styles.history}>
    <summary>How the shown approach works</summary>
    <p className={styles.meta}>Showing {approach.label}. This explanation describes the displayed execution, not a prepared selection.</p>
    <dl className={styles.teaching}>
      <dt>State</dt><dd>{teaching.state}</dd>
      <dt>Base cases</dt><dd>{teaching.baseCases}</dd>
      <dt>Recurrence</dt><dd>{teaching.recurrence}</dd>
      <dt>Evaluation order</dt><dd>{teaching.evaluationOrder}</dd>
      {teaching.cacheKey && <><dt>Memo key</dt><dd>{teaching.cacheKey}</dd></>}
    </dl>
    {note && <p className={styles.meta} aria-live="polite">{note}</p>}
  </details>;
}
