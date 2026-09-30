import React from 'react';
import styles from './ProblemWorkspace.module.css';

/**
 * The Analysis view's reading columns: what the algorithm holds at this step, and what it
 * costs. Everything is read from the step and the problem detail as served:
 *
 * - A variable that is null, undefined or empty says so rather than disappearing; a long or
 *   structured value is shown whole behind a disclosure, never silently truncated.
 * - The call stack is labelled with its order and marks the frame executing now; a stack or
 *   queue marks its Top or Front/Back. Those words come from the trace's own dsType, never
 *   from the problem's title.
 * - Complexity is the backend's metadata. Missing values read "unavailable"; nothing is
 *   inferred from the number of steps.
 */

const LONG = 60;

function text(value) {
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value === 'string') return value === '' ? '"" (empty string)' : value;
  try {
    const json = JSON.stringify(value);
    if (Array.isArray(value) && value.length === 0) return '[] (empty)';
    return json;
  } catch {
    return String(value);
  }
}

function Value({ value }) {
  const shown = text(value);
  if (shown.length <= LONG) return <code className={styles.value}>{shown}</code>;
  return (
    <details className={styles.longValue}>
      <summary><code className={styles.value}>{shown.slice(0, LONG)}…</code> <span className={styles.meta}>show all</span></summary>
      <pre className={styles.valueFull}>{typeof value === 'object' ? JSON.stringify(value, null, 2) : shown}</pre>
    </details>
  );
}

function Sequence({ title, items, marker, order }) {
  return (
    <section className={styles.inspectorSection} aria-label={title}>
      <h3 className={styles.inspectorTitle}>{title}</h3>
      {order && <p className={styles.meta}>{order}</p>}
      {items.length === 0 ? <p className={styles.meta}>Empty at this step.</p> : (
        <ol className={styles.sequence}>
          {items.map((item, index) => {
            const mark = marker(index, items.length);
            return (
              <li key={`${index}-${text(item)}`} className={mark ? styles.marked : undefined}>
                <Value value={item} />
                {mark && <span className={styles.badge}>{mark}</span>}
              </li>
            );
          })}
        </ol>
      )}
    </section>
  );
}

/**
 * What `queueOrStackState` IS, from the trace's hero dsType. A Stack or Queue hero says so
 * itself; a Graph, Grid or Array hero that carries one is a BFS-style queue, the same reading
 * canvas/companions.js gives its Queue pane. Anything else is labelled neutrally rather than
 * guessed at.
 */
function containerKind(dsType) {
  if (dsType === 'Stack') return 'stack';
  if (dsType === 'PriorityQueue') return 'heap';
  if (['Queue', 'Graph', 'Matrix', 'Array'].includes(dsType)) return 'queue';
  return 'other';
}

export default function StateInspector({ step, steps = [], problem, dsType }) {
  const variables = step?.variables && typeof step.variables === 'object' && !Array.isArray(step.variables)
    ? Object.entries(step.variables) : [];
  // Sections appear when the RUN uses the structure at all, like the companion panes, so a
  // recursion's stack does not vanish at the base case - and a BFS, which never recurses,
  // is not shown an empty "call stack" it never had.
  const runHasStack = steps.some((s) => s?.callStack?.length > 0);
  const runHasContainer = steps.some((s) => s?.queueOrStackState?.length > 0);
  const callStack = runHasStack ? (Array.isArray(step?.callStack) ? step.callStack : []) : null;
  const container = runHasContainer ? (Array.isArray(step?.queueOrStackState) ? step.queueOrStackState : []) : null;
  const complexity = problem?.complexity;

  const kind = containerKind(dsType);
  const containerTitle = { queue: 'Queue contents', stack: 'Stack contents', heap: 'Priority queue contents' }[kind]
    || 'Container contents';
  const containerOrder = { queue: 'Front first; the next to leave is at the front.', stack: 'Bottom first; the top is the most recent.' }[kind];
  const containerMark = (index, length) => {
    if (kind === 'queue') return length === 1 ? 'Front · Back' : index === 0 ? 'Front' : index === length - 1 ? 'Back' : null;
    if (kind === 'stack') return index === length - 1 ? 'Top' : null;
    return null;
  };

  return (
    <div className={styles.inspector}>
      <div className={styles.inspectorColumn}>
        <section className={styles.inspectorSection} aria-label="Variables">
          <h3 className={styles.inspectorTitle}>Variables</h3>
          {variables.length === 0 ? <p className={styles.meta}>This step reports no variables.</p> : (
            <dl className={styles.variables}>
              {variables.map(([name, value]) => (
                <div key={name}><dt><code>{name}</code></dt><dd><Value value={value} /></dd></div>
              ))}
            </dl>
          )}
        </section>
        {callStack && (
          <Sequence title="Call stack" items={callStack} order="Outermost call first; the last frame is executing now."
            marker={(index, length) => (index === length - 1 ? 'Current frame' : null)} />
        )}
        {container && <Sequence title={containerTitle} items={container} marker={containerMark} order={containerOrder} />}
      </div>
      <section className={styles.inspectorColumn} aria-label="Algorithm complexity">
        <h3 className={styles.inspectorTitle}>Algorithm complexity</h3>
        <p className={styles.meta}>Asymptotic cost of the algorithm itself, not of this animation.</p>
        {[['Time', complexity?.timeComplexity, complexity?.timeExplanation], ['Space', complexity?.spaceComplexity, complexity?.spaceExplanation]].map(([label, big, why]) => (
          <div key={label} className={styles.complexity}>
            <p><strong>{label}</strong> <code className={styles.value}>{big || 'unavailable'}</code></p>
            <p className={styles.reading}>{why || `No ${label.toLowerCase()} explanation is recorded for this problem.`}</p>
          </div>
        ))}
      </section>
    </div>
  );
}
