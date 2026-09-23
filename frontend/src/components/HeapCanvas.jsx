import React from 'react';
import { heapSlots, heapLayout, parentOf, leftOf, rightOf } from '../trace/heapModel';
import styles from './HeapCanvas.module.css';

const NODE_R = 17;
const COL_W = 54;
const ROW_H = 62;
const SLOT_W = 40;

const LIVE = new Set(['current', 'probe', 'active', 'target', 'visiting']);

/**
 * A heap, drawn as the two things it is at once.
 *
 * The defining idea is that the tree and the array are the SAME structure - the node at
 * index i has children at 2i+1 and 2i+2 and there is no pointer anywhere - and that is
 * exactly what a canvas showing only one of them hides. These problems were split across
 * two tags, Array and PriorityQueue, that both routed to a bar chart, plus five more on a
 * plain tree; none of the three showed the correspondence.
 *
 * Indices are printed under every array slot and inside every node, because the index is
 * the thing the two views share.
 */
export default function HeapCanvas({ currentStep, step }) {
  const activeStep = currentStep || step;
  const { slots, source } = heapSlots(activeStep);

  if (slots.length === 0) {
    // "Empty" and "not mentioned" look identical if you only count slots, and they mean
    // opposite things: one is the algorithm's own state, the other is missing payload.
    return (
      <div className={styles.empty} role="status">
        {source ? 'The heap is empty.' : 'No heap contents for this step.'}
      </div>
    );
  }

  const rows = heapLayout(slots.length);
  const widest = Math.max(...rows.map((r) => r.length));
  const svgW = Math.max(widest * COL_W + COL_W, 220);
  const svgH = rows.length * ROW_H + NODE_R;

  // A row is centred on the widest row, so the tree reads as a tree rather than a ragged
  // left-aligned list.
  const pos = new Map();
  rows.forEach((row, depth) => {
    const offset = (svgW - row.length * COL_W) / 2;
    row.forEach((idx, k) => {
      pos.set(idx, { x: offset + k * COL_W + COL_W / 2, y: depth * ROW_H + NODE_R + 6 });
    });
  });

  const cls = (state) => (LIVE.has(state) ? styles.live : styles.settledSlot);

  return (
    <div className={styles.container} data-testid="heap-canvas">
      <svg width={svgW} height={svgH} role="img" aria-label={`Heap of ${slots.length} elements`}>
        {slots.map((_, i) => [leftOf(i), rightOf(i)]
          .filter((c) => c < slots.length)
          .map((c) => (
            <line
              key={`e${i}-${c}`}
              x1={pos.get(i).x} y1={pos.get(i).y}
              x2={pos.get(c).x} y2={pos.get(c).y}
              className={styles.edge}
            />
          )))}
        {slots.map((slot, i) => (
          <g key={i} data-index={i} data-state={slot.state}>
            <circle
              cx={pos.get(i).x} cy={pos.get(i).y} r={NODE_R}
              className={`${styles.node} ${cls(slot.state)}`}
            />
            <text x={pos.get(i).x} y={pos.get(i).y + 4} textAnchor="middle" className={styles.nodeValue}>
              {slot.value}
            </text>
            <text x={pos.get(i).x} y={pos.get(i).y - NODE_R - 4} textAnchor="middle" className={styles.nodeIndex}>
              {i}
            </text>
          </g>
        ))}
      </svg>

      {/* The same heap, as the array it actually is. */}
      <div className={styles.arrayBlock}>
        <span className={styles.arrayLabel}>
          the same heap, stored as an array{source === 'array' ? '' : ' (level order)'}
        </span>
        <div className={styles.slots} data-testid="heap-array">
          {slots.map((slot, i) => (
            <div
              key={i}
              data-index={i}
              className={`${styles.slot} ${cls(slot.state)}`}
              style={{ width: SLOT_W }}
              title={`index ${i}${parentOf(i) === null ? ' (root)' : `, parent ${parentOf(i)}`}`}
            >
              <span className={styles.slotValue}>{slot.value}</span>
              <span className={styles.slotIndex}>{i}</span>
            </div>
          ))}
        </div>
        <span className={styles.rule}>
          children of <code>i</code> live at <code>2i+1</code> and <code>2i+2</code>; its parent at <code>(i-1)/2</code>
        </span>
      </div>
    </div>
  );
}
