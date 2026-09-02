import React from 'react';

/**
 * Companion pane for a step's queue contents.
 *
 * Not a hero canvas — it renders inside `.companion-pane` beside whatever the step's
 * dsType chose as the primary visualization (see `canvas/companions.js`). BFS narrates
 * "Seed the queue" / "Dequeue 0" while its hero is the graph; without this pane those
 * sentences pointed at nothing on screen.
 *
 * `queueOrStackState` is shared with the (currently unbuilt) stack companion — plain
 * string labels only, no per-element Bench state, since the backend field carries no
 * richer payload. Front-of-queue is always index 0, per StepEmitter.queue()'s contract.
 */
export default function QueueCanvas({ step, title = 'Queue' }) {
  const items = step?.queueOrStackState || [];

  return (
    <div className="companion-pane" aria-label={title}>
      <div className="companion-title">{title}</div>
      {items.length === 0 ? (
        <div className="companion-empty">empty</div>
      ) : (
        <ol className="queue-row">
          {items.map((value, idx) => (
            <li
              key={`${idx}-${value}`}
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
