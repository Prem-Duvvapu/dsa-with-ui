import React, { useRef, useState } from 'react';
import usePersistentState from '../hooks/usePersistentState';
import useElementWidth from './useElementWidth';
import styles from './ProblemWorkspace.module.css';

/**
 * Diagram and source together when both fit, one at a time when they do not.
 *
 * The split is offered only when the MEASURED container fits the pane minima
 * (REFERENCE_DESIGN.md D10): 480px of diagram, 360px of source and the separator. Below
 * that the view becomes Diagram / Source subviews sharing the same narration and controls,
 * rather than two panes too cramped to read. The ratio is a small versioned preference,
 * clamped on read so an old or hand-edited value cannot pin an unusable layout.
 */

export const SPLIT = Object.freeze({ min: 0.45, max: 0.7, initial: 0.6, step: 0.05 });
const PANE_MIN = { diagram: 480, source: 360, gap: 24 };
export const SPLIT_MIN_WIDTH = PANE_MIN.diagram + PANE_MIN.source + PANE_MIN.gap;

const clamp = (ratio) => Math.min(SPLIT.max, Math.max(SPLIT.min, ratio));
const isSplitPref = (v) => v && v.v === 1 && typeof v.ratio === 'number' && Number.isFinite(v.ratio);

export default function CodeWalkthrough({ diagram, source, subview, onSubview }) {
  const containerRef = useRef(null);
  const width = useElementWidth(containerRef);
  const [pref, setPref] = usePersistentState('codeSplit', { v: 1, ratio: SPLIT.initial }, isSplitPref);
  const ratio = clamp(pref.ratio);
  const [dragging, setDragging] = useState(false);
  const split = width >= SPLIT_MIN_WIDTH;

  const setRatio = (next) => setPref({ v: 1, ratio: clamp(Math.round(next * 100) / 100) });

  const onKeyDown = (event) => {
    const moves = {
      ArrowLeft: ratio - SPLIT.step,
      ArrowRight: ratio + SPLIT.step,
      Home: SPLIT.min,
      End: SPLIT.max,
      Enter: SPLIT.initial
    };
    if (!(event.key in moves)) return;
    event.preventDefault();
    setRatio(moves[event.key]);
  };

  const onPointerDown = (event) => {
    event.preventDefault();
    const box = event.currentTarget.parentElement.getBoundingClientRect();
    setDragging(true);
    const move = (e) => setRatio((e.clientX - box.left) / box.width);
    const up = () => {
      setDragging(false);
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  };

  // One measured element for both layouts, so the observer never watches a removed node.
  if (!split) {
    return (
      <div ref={containerRef} className={styles.codeArea}><div className={styles.codeStack}>
        <div className={styles.subviews} role="group" aria-label="Show in Code walkthrough">
          {['diagram', 'source'].map((name) => (
            <button key={name} type="button" className={styles.subview} aria-pressed={subview === name} onClick={() => onSubview(name)}>
              {name === 'diagram' ? 'Diagram' : 'Source'}
            </button>
          ))}
        </div>
        {subview === 'diagram' ? diagram : source}
      </div></div>
    );
  }

  const percent = Math.round(ratio * 100);
  return (
    <div ref={containerRef} className={styles.codeArea}><div
      className={`${styles.split}${dragging ? ` ${styles.splitDragging}` : ''}`}
      style={{ gridTemplateColumns: `minmax(${PANE_MIN.diagram}px, ${percent}fr) ${PANE_MIN.gap}px minmax(${PANE_MIN.source}px, ${100 - percent}fr)` }}
    >
      <div className={styles.splitPane}>{diagram}</div>
      <div
        role="separator"
        tabIndex={0}
        aria-orientation="vertical"
        aria-label="Resize diagram and source"
        aria-valuemin={Math.round(SPLIT.min * 100)}
        aria-valuemax={Math.round(SPLIT.max * 100)}
        aria-valuenow={percent}
        aria-valuetext={`Diagram ${percent}%, source ${100 - percent}%`}
        className={styles.separator}
        onKeyDown={onKeyDown}
        onPointerDown={onPointerDown}
        onDoubleClick={() => setRatio(SPLIT.initial)}
        title="Drag, or use the arrow keys. Enter or double-click resets."
      />
      <div className={styles.splitPane}>{source}</div>
    </div></div>
  );
}
