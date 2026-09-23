import React from 'react';
import styles from './SearchSpaceCanvas.module.css';

const CELL = 40;
const GAP = 5;
const LIVE = new Set(['target', 'active', 'current', 'probe']);

const num = (v) => {
  const n = Number(v);
  return Number.isFinite(n) ? n : null;
};

/** low / high / mid, under whichever of the usual names a tracer happened to pick. */
function bounds(vars = {}) {
  const pick = (...names) => {
    for (const n of names) {
      if (vars[n] !== undefined) {
        const v = num(vars[n]);
        if (v !== null) return v;
      }
    }
    return null;
  };
  return {
    low: pick('low', 'lo', 'left', 'l', 'start'),
    high: pick('high', 'hi', 'right', 'r', 'end'),
    mid: pick('mid', 'm')
  };
}

/**
 * Binary search comes in two shapes and they need different pictures.
 *
 * On a sorted ARRAY, low and high are indices: the search space is the array itself, and
 * each step throws away half of it. On an ANSWER space - koko's eating speed, the smallest
 * feasible capacity - low and high are candidate VALUES over a numeric range, and the array
 * is just the input the feasibility check reads. Drawing the second like the first would
 * frame four piles as if they were a range of eleven speeds.
 *
 * The discriminator is not "is high small": that coincides. It is whether the cells behave
 * like the search space, meaning high indexes into them AND everything outside [low, high]
 * has already been eliminated.
 */
function isIndexSpace(cells, low, high) {
  if (!Array.isArray(cells) || cells.length === 0) return false;
  if (low === null || high === null) return false;
  if (high >= cells.length || low < 0) return false;
  return !cells.some((c, i) => (i < low || i > high) && LIVE.has(c.state));
}

/**
 * The first step that stated a range: the widest the search ever considered, so the shrink
 * is visible against it, and the step that decides which KIND of space this is.
 *
 * Deciding the kind per step let it change mid-animation. aggressive-cows searches
 * distances 1..8 over five cow positions and renders as an answer space until high shrinks
 * below five, at which point the heuristic flips and the badge starts calling a distance an
 * index. A tracer does not change what it is searching halfway through, so neither should
 * the picture.
 */
function originalRange(steps, fallback) {
  if (Array.isArray(steps)) {
    for (const s of steps) {
      const b = bounds(s?.variables);
      if (b.low !== null && b.high !== null) return { ...b, cells: s?.arrayState };
    }
  }
  return fallback;
}

/** The most recent step that stated a range; commentary steps in between carry none. */
function lastBounds(steps, index, fallback) {
  if (Array.isArray(steps)) {
    for (let i = Math.min(index, steps.length - 1); i >= 0; i -= 1) {
      const b = bounds(steps[i]?.variables);
      if (b.low !== null && b.high !== null) {
        return { ...b, cells: steps[i]?.arrayState ?? fallback?.arrayState };
      }
    }
  }
  const b = bounds(fallback?.variables);
  return { ...b, cells: fallback?.arrayState };
}

export default function SearchSpaceCanvas({ currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  const here = lastBounds(steps, currentStepIndex ?? 0, activeStep);
  const { low, high, mid } = here;
  const cells = here.cells ?? activeStep?.arrayState ?? [];

  if (low === null || high === null) {
    return <div className={styles.empty} role="status">No search range for this step.</div>;
  }

  const remaining = high - low + 1;
  const full = originalRange(steps, { low, high, cells });
  const indexed = isIndexSpace(full.cells ?? cells, full.low, full.high);
  const span = Math.max(1, full.high - full.low + 1);
  const shareLeft = ((low - full.low) / span) * 100;
  const shareWidth = (remaining / span) * 100;

  return (
    <div className={styles.container} data-testid="search-space-canvas">
      <div className={styles.summary}>
        <span className={styles.rangeBadge} data-testid="search-range">
          {indexed ? 'indices' : 'answers'} [{low}, {high}] · {remaining} left of {span}
        </span>
        {mid !== null && (
          <span className={styles.midBadge} data-testid="search-mid">probing {mid}</span>
        )}
      </div>

      {/* The whole original range, with what survives drawn over it. Halving reads as
          halving only when the part already discarded stays on screen beside it. */}
      <div className={styles.ruler} data-testid="search-ruler">
        <span className={styles.rulerEnd}>{full.low}</span>
        <div className={styles.rulerTrack}>
          <div
            className={styles.rulerLive}
            data-testid="search-live"
            style={{ left: `${shareLeft}%`, width: `${shareWidth}%` }}
          />
          {mid !== null && span > 0 && (
            <div
              className={styles.rulerMid}
              style={{ left: `${((mid - full.low) / span) * 100}%` }}
            />
          )}
        </div>
        <span className={styles.rulerEnd}>{full.high}</span>
      </div>

      {indexed ? (
        <div className={styles.cells} data-testid="search-cells" style={{ gap: GAP }}>
          {cells.map((cell, i) => {
            const alive = i >= low && i <= high;
            const isMid = i === mid;
            return (
              <div
                key={i}
                data-alive={alive ? 'true' : undefined}
                data-mid={isMid ? 'true' : undefined}
                className={`${styles.cell} ${alive ? styles.cellAlive : styles.cellGone} ${isMid ? styles.cellMid : ''}`}
                style={{ width: CELL, height: CELL }}
              >
                <span className={styles.cellValue}>{cell.label ?? cell.value}</span>
                <span className={styles.cellIndex}>{i}</span>
              </div>
            );
          })}
        </div>
      ) : (
        // Answer space: the array is the INPUT the feasibility check reads, not the space
        // being searched. Labelling it as such is the difference between understanding
        // this family and mistaking it for an array search.
        cells.length > 0 && (
          <div className={styles.inputRow} data-testid="search-input-row">
            <span className={styles.inputLabel}>checked against</span>
            <div className={styles.cells} style={{ gap: GAP }}>
              {cells.map((cell, i) => (
                <div
                  key={i}
                  className={`${styles.cell} ${LIVE.has(cell.state) ? styles.cellMid : styles.cellAlive}`}
                  style={{ width: CELL, height: CELL }}
                >
                  <span className={styles.cellValue}>{cell.label ?? cell.value}</span>
                  <span className={styles.cellIndex}>{i}</span>
                </div>
              ))}
            </div>
          </div>
        )
      )}
    </div>
  );
}
