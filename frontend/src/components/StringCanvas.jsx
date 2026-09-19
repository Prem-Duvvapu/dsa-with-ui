import React from 'react';
import styles from './StringCanvas.module.css';
import { lastPayload } from '../trace/lastPayload';

const CELL = 40;
const GAP = 4;

/**
 * Hero canvas for DsType.STRING, replacing ArrayCanvas as the placeholder it used to be.
 *
 * Every STRING tracer emits through StepEmitter.chars(), which writes arrayState exactly
 * as StepEmitter.array() does - each element's `value` is the character's Unicode code
 * point and `label` carries the actual character - so ArrayCanvas already "worked" here in
 * the sense that it did not crash. What it drew was wrong for the structure: a bar chart
 * whose height and headline number are the ASCII/code-point value, with the letter itself
 * demoted to a small caption underneath. A learner watching kmp-lps-algo or
 * z-function-algo saw bars of essentially random height swinging around while reading
 * "code point 108" in large type above the "l" that mattered.
 *
 * This draws what the structure actually is: a row of character cells, the letter itself
 * the only thing rendered at size, coloured by the same two-pointer vocabulary
 * chars(value, primary, secondary) already emits - "current" for the position being
 * examined right now, "target" for a second tracked position (KMP's candidate length,
 * Z-function's box boundary, a palindrome's mirror index), "default" for everything else.
 * No bar height, because a character's code point does not mean anything pedagogically;
 * unlike a numeric array, encoding it spatially would teach the wrong lesson.
 */
export default function StringCanvas({ step, currentStep, steps, currentStepIndex, title = 'String' }) {
  const activeStep = currentStep || step;
  // Same carry-forward rule as every other structure canvas: a tracer restates arrayState
  // only on the steps that change it, so absence means "unchanged", not "empty".
  const cells = lastPayload(steps, currentStepIndex, 'arrayState', activeStep) || [];

  if (cells.length === 0) {
    return (
      <div className={styles.empty} role="status">
        Empty string.
      </div>
    );
  }

  return (
    <div className={styles.wrap} data-testid="string-canvas">
      <div className={styles.header}>
        <span className={styles.title}>{title}</span>
        <span className={styles.count}>
          {cells.length} character{cells.length === 1 ? '' : 's'}
        </span>
      </div>

      <div className={styles.track}>
        {cells.map((cell, idx) => {
          const char = typeof cell.label === 'string' && cell.label.length > 0
            ? cell.label
            : String.fromCodePoint(cell.value ?? 32);
          const state = cell.state || 'default';
          // Spelled out, not interpolated: designTokens.test.js reads the JSX statically
          // to catch a className index.css does not define, and a template literal is
          // invisible to it - the same discipline CaptureStrip's CELL_CLASS follows.
          const stateClass = state === 'current' ? styles.cellCurrent
            : state === 'target' ? styles.cellTarget
              : styles.cellDefault;
          return (
            <div
              key={idx}
              data-string-index={idx}
              data-state={state}
              className={`${styles.cell} ${stateClass}`}
              style={{ width: CELL, height: CELL, marginRight: GAP }}
            >
              {/* Whitespace collapses to nothing in a normal text node; a visible glyph
                  matters here because a step highlighting a space (many of these tracers
                  operate on arbitrary strings, not just alphabetic ones) should not read
                  as an empty, unexplained box. */}
              <span className={styles.char}>{char === ' ' ? ' ' : char}</span>
              <span className={styles.index}>{idx}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
