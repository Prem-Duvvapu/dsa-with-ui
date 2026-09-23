import React from 'react';
import styles from './WindowCanvas.module.css';

const CELL = 42;
const GAP = 6;

/**
 * The window itself, derived from the cells rather than from variable names.
 *
 * Every sliding-window tracer marks the cells inside its window with a non-default state -
 * `current` for the edge it just moved, `active`/`target` for what is still in - and leaves
 * everything outside as `default`. That marking is the one signal all twelve agree on. The
 * variables do not: some call the bounds left/right, some start/end, some only i, and
 * several steps carry no bounds at all.
 */
function windowFrom(step) {
  const cells = step?.arrayState;
  if (!Array.isArray(cells) || cells.length === 0) return null;
  const inside = cells
    .map((c, i) => ({ i, in: c.state && c.state !== 'default' }))
    .filter((c) => c.in)
    .map((c) => c.i);
  if (inside.length === 0) return null;
  return { left: Math.min(...inside), right: Math.max(...inside), cells };
}

/**
 * The last step at or before `index` that actually carried cells.
 *
 * Tracers interleave "here is the window" steps with commentary steps that carry only
 * variables. Dropping the frame on those would make it flicker out every other step, which
 * reads as the window disappearing rather than the narration pausing.
 */
function lastWindow(steps, index, fallback) {
  if (!Array.isArray(steps)) return windowFrom(fallback);
  for (let i = Math.min(index, steps.length - 1); i >= 0; i -= 1) {
    const w = windowFrom(steps[i]);
    if (w) return w;
  }
  return windowFrom(fallback);
}

/** Aggregates worth showing beside the window; the bounds are already drawn. */
const BOUND_NAMES = new Set(['left', 'right', 'start', 'end', 'i', 'j', 'windowstart', 'windowend']);

export default function WindowCanvas({ currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  const win = lastWindow(steps, currentStepIndex ?? 0, activeStep);

  if (!win) {
    return (
      <div className={styles.empty} role="status">
        No window data for this step.
      </div>
    );
  }

  const { left, right, cells } = win;
  const width = right - left + 1;
  const vars = Object.entries(activeStep?.variables || {})
    .filter(([k]) => !BOUND_NAMES.has(k.toLowerCase()));

  return (
    <div className={styles.container} data-testid="window-canvas">
      <div className={styles.summary}>
        <span className={styles.windowBadge} data-testid="window-bounds">
          window [{left}, {right}] · {width} wide
        </span>
        {vars.map(([k, v]) => (
          <span key={k} className={styles.varBadge}>{k} {String(v)}</span>
        ))}
      </div>

      <div className={styles.track} style={{ width: cells.length * (CELL + GAP) }}>
        {/* The frame is positioned rather than applied per cell, so it TRANSLATES between
            steps instead of repainting. That movement is the thing being taught: the
            window slides and stretches, it does not blink from one place to another. */}
        <div
          className={styles.frame}
          data-testid="window-frame"
          style={{
            left: left * (CELL + GAP) - 3,
            width: width * CELL + (width - 1) * GAP + 6
          }}
        >
          <span className={`${styles.edge} ${styles.edgeLeft}`}>L</span>
          <span className={`${styles.edge} ${styles.edgeRight}`}>R</span>
        </div>

        <div className={styles.cells} style={{ gap: GAP }}>
          {cells.map((cell, i) => {
            const inside = i >= left && i <= right;
            return (
              <div
                key={i}
                data-inside={inside ? 'true' : undefined}
                className={`${styles.cell} ${inside ? styles.cellInside : styles.cellOutside}`}
                style={{ width: CELL, height: CELL }}
              >
                <span className={styles.cellValue}>{cell.label ?? cell.value}</span>
                <span className={styles.cellIndex}>{i}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}
