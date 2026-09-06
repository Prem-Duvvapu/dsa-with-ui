import React from 'react';

/**
 * A step's queue contents, drawn as an array-shaped row.
 *
 * Two call sites, one component:
 *  - companion pane, beside a Graph/Matrix hero (see `canvas/companions.js`). BFS narrates
 *    "Seed the queue" / "Dequeue 0" while its hero is the graph; without this pane those
 *    sentences pointed at nothing on screen.
 *  - hero canvas for DsType.QUEUE (see `canvas/registry.js`), where the queue IS the
 *    picture. `variant="hero"` widens the pane, which is otherwise pinned to the 200px
 *    companion column.
 *
 * Laid out LEFT-TO-RIGHT, front first: a queue is served from the front and grown at the
 * back, so an array-style row reads the way enqueue/dequeue actually behave. It used to
 * render as a vertical column, which is a stack's shape, not a queue's.
 *
 * `queueOrStackState` carries plain string labels only, no per-element Bench state, since
 * the backend field carries no richer payload. Front-of-queue is always index 0, per
 * StepEmitter.queue()'s contract.
 */
export default function QueueCanvas({ step, currentStep, title = 'Queue', variant = 'companion' }) {
  // App.jsx passes both props (same object); companions.js passes only `step`.
  const activeStep = currentStep || step;
  const items = activeStep?.queueOrStackState || [];
  const paneClass = variant === 'hero' ? 'companion-pane queue-pane-hero' : 'companion-pane';

  return (
    <div className={paneClass} aria-label={title}>
      <div className="companion-title">{title}</div>
      {items.length === 0 ? (
        <div className="companion-empty">empty</div>
      ) : (
        <ol className="queue-row">
          {items.map((value, idx) => (
            <li
              key={`${idx}-${value}`}
              data-queue-index={idx}
              className={idx === 0 ? 'queue-cell queue-cell-front' : 'queue-cell'}
            >
              {idx === 0 && <span className="queue-cell-tag">front</span>}
              <span className="queue-cell-value">{value}</span>
            </li>
          ))}
        </ol>
      )}
    </div>
  );
}

/**
 * The DsType.QUEUE hero mapping. A distinct component so `canvas/registry.js` stays a plain
 * dsType→component table and `canvas/companions.js` keeps handing back the default export
 * unchanged.
 */
export function QueueHeroCanvas(props) {
  return <QueueCanvas {...props} variant="hero" />;
}
