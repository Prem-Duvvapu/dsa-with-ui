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
  const [dragging, setDragging] = useState(false);
  const drag = useRef(null);
  const split = width >= SPLIT_MIN_WIDTH;

  // What the panes can ACTUALLY be at this width. The preference may ask for 70%, but at a
  // 1000px container the 360px source minimum caps the diagram near 63%; the layout and the
  // announced value both use this effective ratio, so a screen reader is told the geometry
  // that is on screen (INDEPENDENT_REVIEW_DB8683B.md S5).
  const available = Math.max(1, width - PANE_MIN.gap);
  const lo = Math.max(SPLIT.min, PANE_MIN.diagram / available);
  const hi = Math.max(lo, Math.min(SPLIT.max, 1 - PANE_MIN.source / available));
  const effective = Math.min(hi, Math.max(lo, clamp(pref.ratio)));
  const percent = Math.round(effective * 100);

  const setRatio = (next) => setPref({ v: 1, ratio: Math.round(Math.min(hi, Math.max(lo, next)) * 100) / 100 });

  const onKeyDown = (event) => {
    const moves = {
      ArrowLeft: effective - SPLIT.step,
      ArrowRight: effective + SPLIT.step,
      Home: lo,
      End: hi,
      Enter: SPLIT.initial,
      ' ': null
    };
    if (!(event.key in moves)) return;
    event.preventDefault();
    if (moves[event.key] !== null) setRatio(moves[event.key]);
  };

  // Pointer capture keeps the drag on the separator itself: no window listeners exist to
  // outlive the view, and release, cancel and lost capture all end it.
  const onPointerDown = (event) => {
    event.preventDefault();
    event.currentTarget.setPointerCapture?.(event.pointerId);
    drag.current = { pointerId: event.pointerId, box: containerRef.current.getBoundingClientRect() };
    setDragging(true);
  };
  const onPointerMove = (event) => {
    const active = drag.current;
    if (!active || event.pointerId !== active.pointerId || !active.box.width) return;
    setRatio((event.clientX - active.box.left) / active.box.width);
  };
  const endDrag = () => {
    drag.current = null;
    setDragging(false);
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

  return (
    <div ref={containerRef} className={styles.codeArea}><div
      className={`${styles.split}${dragging ? ` ${styles.splitDragging}` : ''}`}
      style={{ gridTemplateColumns: `minmax(0, ${percent}fr) ${PANE_MIN.gap}px minmax(0, ${100 - percent}fr)` }}
    >
      <div className={styles.splitPane}>{diagram}</div>
      <div
        role="separator"
        tabIndex={0}
        aria-orientation="vertical"
        aria-label="Resize diagram and source"
        aria-valuemin={Math.round(lo * 100)}
        aria-valuemax={Math.round(hi * 100)}
        aria-valuenow={percent}
        aria-valuetext={`Diagram ${percent}%, source ${100 - percent}%`}
        className={styles.separator}
        onKeyDown={onKeyDown}
        onPointerDown={onPointerDown}
        onPointerMove={onPointerMove}
        onPointerUp={endDrag}
        onPointerCancel={endDrag}
        onLostPointerCapture={endDrag}
        onDoubleClick={() => setRatio(SPLIT.initial)}
        title="Drag, or use the arrow keys. Enter or double-click resets."
      />
      <div className={styles.splitPane}>{source}</div>
    </div></div>
  );
}
