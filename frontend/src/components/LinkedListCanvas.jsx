import styles from './LinkedListCanvas.module.css';
import React, { useLayoutEffect, useRef, useState } from 'react';
import { ArrowRight } from 'lucide-react';
import { lastPayload } from '../trace/lastPayload';

/**
 * A childId/randomId edge can point at any other node in the row, not just the one
 * physically next to it — e.g. flattening-ll's child pointer, or clone-ll-random-pointer's
 * random pointer. That needs a real line between two arbitrary boxes, not the inline arrow
 * glyph used for an adjacent pair. This measures each node's box (ref + layout effect owned
 * by THIS component, per RCA-017 — never split a ref from the effect that reads it) and
 * draws those extra edges as an absolutely-positioned SVG overlay on top of the row, without
 * touching how next/prev render when childId/randomId are absent.
 */
export default function LinkedListCanvas({ problem, currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  // Absence is not "show the default". Tracers restate a structure only on the steps
  // that change it, so falling through to the catalogue default drew the CATALOGUE's
  // data over the caller's own input. Measured app-wide: 1506 steps across 142 of 232
  // problems. The default is honest only before any step has emitted anything.
  const listState = lastPayload(steps, currentStepIndex, 'listState', activeStep)
    || problem?.defaultList || [];

  // Seven problems emit prevId, and for a doubly linked list the backward pointer is half
  // the structure: reversing one means swapping next AND prev on every node, which was
  // invisible while only next was drawn.
  const isDoubly = listState.some((n) => n.prevId !== null && n.prevId !== undefined);

  const containerRef = useRef(null);
  const nodeRefs = useRef(new Map());
  const [edges, setEdges] = useState({ child: [], random: [] });

  const getNodeColor = (state) => {
    switch (state) {
      case 'active':
      case 'curr':
        return { fill: 'var(--role-current)', stroke: 'var(--role-current-edge)', glow: '0 0 18px color-mix(in srgb, var(--role-current) 80%, transparent)' };
      case 'slow':
        return { fill: 'var(--role-secondary)', stroke: 'var(--role-secondary-edge)', glow: '0 0 16px color-mix(in srgb, var(--role-secondary) 70%, transparent)' };
      case 'fast':
        return { fill: 'var(--role-alternate)', stroke: 'var(--role-alternate-edge)', glow: '0 0 18px color-mix(in srgb, var(--role-alternate) 80%, transparent)' };
      case 'visited':
        return { fill: 'var(--role-done)', stroke: 'var(--role-done-edge)', glow: '0 0 14px color-mix(in srgb, var(--role-done) 50%, transparent)' };
      default:
        return { fill: 'var(--canvas-node-fill)', stroke: 'var(--canvas-edge)', glow: 'none' };
    }
  };

  useLayoutEffect(() => {
    const container = containerRef.current;
    if (!container) return;

    const measure = () => {
      const containerRect = container.getBoundingClientRect();
      const rectFor = (id) => {
        const el = nodeRefs.current.get(id);
        if (!el) return null;
        const r = el.getBoundingClientRect();
        return {
          left: r.left - containerRect.left + container.scrollLeft,
          right: r.right - containerRect.left + container.scrollLeft,
          top: r.top - containerRect.top + container.scrollTop,
          bottom: r.bottom - containerRect.top + container.scrollTop,
          centerX: (r.left + r.right) / 2 - containerRect.left + container.scrollLeft,
        };
      };

      const buildEdges = (getTargetId, curveAbove) => {
        const out = [];
        for (const node of listState) {
          const targetId = getTargetId(node);
          if (targetId === null || targetId === undefined) continue;
          if (targetId === node.id) continue; // no self-loop line to draw
          const from = rectFor(node.id);
          const to = rectFor(targetId);
          if (!from || !to) continue;
          const y = curveAbove ? Math.min(from.top, to.top) : Math.max(from.bottom, to.bottom);
          const lift = curveAbove ? -18 : 18;
          out.push({
            key: `${node.id}->${targetId}`,
            x1: from.centerX,
            y1: y,
            x2: to.centerX,
            y2: y,
            midY: y + lift,
          });
        }
        return out;
      };

      setEdges({
        child: buildEdges((n) => n.childId, false),
        random: buildEdges((n) => n.randomId, true),
      });
    };

    measure();
    const onResize = () => measure();
    window.addEventListener('resize', onResize);
    container.addEventListener('scroll', measure);
    return () => {
      window.removeEventListener('resize', onResize);
      container.removeEventListener('scroll', measure);
    };
    // Re-measure whenever the rendered nodes (identity, order, or pointers) change.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(listState.map((n) => [n.id, n.childId, n.randomId]))]);

  return (
    <div className={styles.wrap}>
      {/* No title here: CanvasShell already names the problem. What survives is the dash
          key, which the shell's generic legend cannot express - it explains two link KINDS
          drawn as two dash patterns, not two states. It appears only when the list
          actually has those links. */}
      {(edges.child.length > 0 || edges.random.length > 0) && (
        <div className={styles.linkKey}>
          {edges.child.length > 0 && (
            <span className={styles.linkKeyItem}>
              <svg width="20" height="8" aria-hidden="true"><line x1="0" y1="4" x2="20" y2="4" stroke="var(--role-link-child)" strokeWidth="2" strokeDasharray="4,3" /></svg>
              child
            </span>
          )}
          {edges.random.length > 0 && (
            <span className={styles.linkKeyItem}>
              <svg width="20" height="8" aria-hidden="true"><line x1="0" y1="4" x2="20" y2="4" stroke="var(--role-link-random)" strokeWidth="2" strokeDasharray="1,3" strokeLinecap="round" /></svg>
              random
            </span>
          )}
        </div>
      )}

      <div
        ref={containerRef}
        className={styles.stage}
      >
        <svg
          className={styles.linkLayer}
        >
          <defs>
            <marker id="llc-child-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L6,3 L0,6 Z" fill="var(--role-link-child)" />
            </marker>
            <marker id="llc-random-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L6,3 L0,6 Z" fill="var(--role-link-random)" />
            </marker>
          </defs>
          {edges.child.map((e) => (
            <path
              key={`child-${e.key}`}
              d={`M ${e.x1} ${e.y1} Q ${(e.x1 + e.x2) / 2} ${e.midY} ${e.x2} ${e.y2}`}
              fill="none"
              stroke="var(--role-link-child)"
              strokeWidth="2"
              strokeDasharray="6,4"
              markerEnd="url(#llc-child-arrow)"
            />
          ))}
          {edges.random.map((e) => (
            <path
              key={`random-${e.key}`}
              d={`M ${e.x1} ${e.y1} Q ${(e.x1 + e.x2) / 2} ${e.midY} ${e.x2} ${e.y2}`}
              fill="none"
              stroke="var(--role-link-random)"
              strokeWidth="2"
              strokeDasharray="1,4"
              strokeLinecap="round"
              markerEnd="url(#llc-random-arrow)"
            />
          ))}
        </svg>

        {listState.map((node, idx) => {
          const colorInfo = getNodeColor(node.state);
          const next = listState[idx + 1];
          // Draw the inline "next" arrow only when the array-adjacent box is genuinely
          // this node's next — unchanged for every existing tracer (nextId is always set
          // to the adjacent box's id there), but lets a new tracer render a node whose
          // true next is not its visual neighbor without lying about a connection.
          const adjacentIsNext = next && node.nextId === next.id;

          return (
            <React.Fragment key={node.id}>
              <div
                ref={(el) => {
                  if (el) nodeRefs.current.set(node.id, el);
                  else nodeRefs.current.delete(node.id);
                }}
                className={styles.node}
                style={{
                  background: colorInfo.fill,
                  border: `2px solid ${colorInfo.stroke}`,
                  boxShadow: colorInfo.glow
                }}
              >
                <div className={styles.nodeValue}>
                  {node.val}
                </div>
                <div className={styles.nodeDivider} />
                <div className={styles.nodePointers}>
                  {/* Only doubly linked lists get a prev row. A singly linked list should
                      not grow a column of NULLs it never had. */}
                  {isDoubly && (
                    <div className={styles.pointerMuted}>
                      {node.prevId !== null && node.prevId !== undefined
                        ? `prev -> [${node.prevId}]`
                        : 'prev -> NULL'}
                    </div>
                  )}
                  <div className={styles.pointerStrong}>
                    {node.nextId !== null && node.nextId !== undefined
                      ? `next -> [${node.nextId}]`
                      : 'next -> NULL'}
                  </div>
                </div>
              </div>

              {/* Arrow Connection */}
              {idx < listState.length - 1 && (
                adjacentIsNext
                  ? <ArrowRight size={22} color="var(--text-muted)" className={styles.arrowIcon} />
                  : <div className={styles.arrowSpacer} />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
