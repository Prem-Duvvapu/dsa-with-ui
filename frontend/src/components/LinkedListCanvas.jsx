import React, { useLayoutEffect, useRef, useState } from 'react';
import { Link2, ArrowRight } from 'lucide-react';

/**
 * A childId/randomId edge can point at any other node in the row, not just the one
 * physically next to it — e.g. flattening-ll's child pointer, or clone-ll-random-pointer's
 * random pointer. That needs a real line between two arbitrary boxes, not the inline arrow
 * glyph used for an adjacent pair. This measures each node's box (ref + layout effect owned
 * by THIS component, per RCA-017 — never split a ref from the effect that reads it) and
 * draws those extra edges as an absolutely-positioned SVG overlay on top of the row, without
 * touching how next/prev render when childId/randomId are absent.
 */
export default function LinkedListCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const listState = activeStep?.listState || problem?.defaultList || [];

  const containerRef = useRef(null);
  const nodeRefs = useRef(new Map());
  const [edges, setEdges] = useState({ child: [], random: [] });

  const getNodeColor = (state) => {
    switch (state) {
      case 'active':
      case 'curr':
        return { fill: '#3b82f6', stroke: '#60a5fa', glow: '0 0 18px rgba(59,130,246,0.8)' };
      case 'slow':
        return { fill: '#f59e0b', stroke: '#fbbf24', glow: '0 0 16px rgba(245,158,11,0.7)' };
      case 'fast':
        return { fill: '#ec4899', stroke: '#f472b6', glow: '0 0 18px rgba(236,72,153,0.8)' };
      case 'visited':
        return { fill: '#10b981', stroke: '#34d399', glow: '0 0 14px rgba(16,185,129,0.5)' };
      default:
        return { fill: '#1e293b', stroke: '#475569', glow: 'none' };
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
    <div style={{ flex: 1, padding: '14px 20px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Link2 size={18} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700', letterSpacing: '0.4px' }}>
            Linked List Topology Visualizer
          </span>
        </div>
        {(edges.child.length > 0 || edges.random.length > 0) && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.68rem', color: '#94a3b8' }}>
            {edges.child.length > 0 && (
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <svg width="20" height="8"><line x1="0" y1="4" x2="20" y2="4" stroke="#a855f7" strokeWidth="2" strokeDasharray="4,3" /></svg>
                child
              </span>
            )}
            {edges.random.length > 0 && (
              <span style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                <svg width="20" height="8"><line x1="0" y1="4" x2="20" y2="4" stroke="#f97316" strokeWidth="2" strokeDasharray="1,3" strokeLinecap="round" /></svg>
                random
              </span>
            )}
          </div>
        )}
      </div>

      <div
        ref={containerRef}
        style={{ flex: 1, width: '100%', minHeight: '280px', position: 'relative', display: 'flex', alignItems: 'center', gap: '16px', padding: '20px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflow: 'auto' }}
      >
        <svg
          style={{ position: 'absolute', inset: 0, width: '100%', height: '100%', overflow: 'visible', pointerEvents: 'none' }}
        >
          <defs>
            <marker id="llc-child-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L6,3 L0,6 Z" fill="#a855f7" />
            </marker>
            <marker id="llc-random-arrow" markerWidth="8" markerHeight="8" refX="6" refY="3" orient="auto">
              <path d="M0,0 L6,3 L0,6 Z" fill="#f97316" />
            </marker>
          </defs>
          {edges.child.map((e) => (
            <path
              key={`child-${e.key}`}
              d={`M ${e.x1} ${e.y1} Q ${(e.x1 + e.x2) / 2} ${e.midY} ${e.x2} ${e.y2}`}
              fill="none"
              stroke="#a855f7"
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
              stroke="#f97316"
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
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  borderRadius: '12px',
                  background: colorInfo.fill,
                  border: `2px solid ${colorInfo.stroke}`,
                  boxShadow: colorInfo.glow,
                  padding: '12px 18px',
                  transition: 'all 0.3s ease',
                  gap: '12px',
                  position: 'relative',
                  zIndex: 1,
                }}
              >
                <div style={{ fontSize: '1rem', fontWeight: '800', color: '#ffffff' }}>
                  {node.val}
                </div>
                <div style={{ width: '1px', height: '24px', background: 'rgba(255, 255, 255, 0.2)' }} />
                <div style={{ fontSize: '0.72rem', color: '#cbd5e1', fontWeight: '600' }}>
                  {node.nextId !== null ? `next -> [${node.nextId}]` : 'next -> NULL'}
                </div>
              </div>

              {/* Arrow Connection */}
              {idx < listState.length - 1 && (
                adjacentIsNext
                  ? <ArrowRight size={22} color="#64748b" style={{ flexShrink: 0, position: 'relative', zIndex: 1 }} />
                  : <div style={{ width: '22px', flexShrink: 0 }} />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
