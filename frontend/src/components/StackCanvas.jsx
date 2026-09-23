import styles from './StackCanvas.module.css';
import React from 'react';
import { Layers } from 'lucide-react';
import { lastPayload } from '../trace/lastPayload';

/**
 * Hero canvas for a step's own stack (`queueOrStackState`, via StepEmitter.stack()) —
 * not `arrayState`. min-stack, next-greater-element-2, trapping-rainwater and every other
 * DsType.STACK tracer already emit this field every step; before this canvas existed,
 * `Stack` routed to ArrayCanvas, so the actual stack was computed but never drawn — only
 * the input array was, which is a different structure than the one the narration
 * describes ("push 4", "pop the top").
 *
 * Index 0 is the top, matching StepEmitter.stack()'s push-to-front convention (an
 * ArrayDeque used as a stack iterates head-first).
 *
 * FILLS BOTTOM-UP, which is a property of the container, not of the iteration order. The
 * items are drawn in array order — index 0 first, so the top of the stack is highest on
 * screen — and the well is anchored to its FLOOR (`justify-content: flex-end`). The last
 * index, the earliest push, therefore rests on the bottom edge and stays there; every
 * later push adds a block ABOVE the pile, growing it upward the way a physical stack does.
 *
 * Anchored to the ceiling (`flex-start`, as it was) the same DOM produced the opposite
 * reading: a single element hung from the top of the panel and the pile grew DOWNWARD as
 * values were pushed, which is a list, not a stack. Reversing the iteration instead of the
 * anchor is the tempting fix and it is wrong — it puts the bottom of the stack on top and
 * the "top" badge on the floor.
 */
export default function StackCanvas({ step, currentStep, title = 'Stack', steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  // Absence is not emptiness. Tracers restate the stack only on the steps that change it
  // and narrate in between, so `|| []` made it blink empty between every push - measured
  // at 60 steps across 21 of 24 Stack & Queue problems. An explicit [] still renders empty,
  // because that is a real value.
  const items = lastPayload(steps, currentStepIndex, 'queueOrStackState', activeStep) || [];

  return (
    <div className={styles.wrap}>
      <div className={styles.header}>
        <div className={styles.headerGroup}>
          <Layers size={16} color="var(--bench-ink-secondary)" />
          <span className={styles.title}>
            {title}
          </span>
          <span className={styles.count}>
            {items.length} item{items.length === 1 ? '' : 's'}
          </span>
        </div>
      </div>

      <div
        className={`${styles.stage}${items.length ? '' : ` ${styles.stageEmpty}`}`}
      >
        {items.length === 0 ? (
          <span className={styles.empty}>
            empty
          </span>
        ) : (
          items.map((value, idx) => (
            <div
              key={`${idx}-${value}`}
              data-stack-index={idx}
              className={`${styles.item}${idx === 0 ? ` ${styles.itemTop}` : ''}`}
            >
              {idx === 0 && (
                <span className={styles.topTag}>
                  top
                </span>
              )}
              <span className={idx === 0 ? styles.valueTop : styles.value}>{value}</span>
            </div>
          ))
        )}
      </div>
    </div>
  );
}
