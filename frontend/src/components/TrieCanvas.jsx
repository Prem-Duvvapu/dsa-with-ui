import React, { useMemo } from 'react';

/**
 * Trie (prefix tree) visualizer.
 *
 * Canonical wire shape (RCA-012): the backend's `TrieNodeModel` is the single source of
 * truth for both the node fields AND the child-linking shape. A step's `trieState` is a
 * flat list of:
 *
 *   { id, character, endOfWord, x, y, children: { [char]: childId }, state }
 *
 * - `character` is the incoming edge label for this node; the root's is `null`.
 * - `children` maps each outgoing character to the child node's `id` (NOT an array of
 *   ids) — this mirrors `Map<String, Integer>` on the Java side exactly.
 * - `x`/`y` are laid out server-side by the tracer (the same convention `TreeCanvas`
 *   uses for `treeNodes`), so this canvas only draws — it does not compute its own tree
 *   layout the way the pre-RCA-012 code did.
 *
 * Do not reintroduce a second accepted shape (e.g. `char`/`isEnd`/id-array children) —
 * that mismatch against the real backend serializer is exactly what RCA-012 tracked.
 */

/** Maps node states to Bench-token CSS. */
function stateStyle(state) {
  switch (state) {
    case 'current':
    case 'visiting':
    case 'active':
      return { fill: 'var(--probe)', stroke: 'var(--probe)', text: 'var(--probe-on)' };
    case 'found':
    case 'done':
    case 'placed':
      return { fill: 'var(--settled)', stroke: 'var(--settled)', text: 'var(--settled-on)' };
    case 'visited':
    case 'processed':
      return { fill: 'var(--bench-fill)', stroke: 'var(--bench-rule-strong)', text: 'var(--bench-ink)' };
    default:
      return { fill: 'var(--bench-fill)', stroke: 'var(--bench-rule)', text: 'var(--bench-ink)' };
  }
}

/** Derives the edge list from each node's `children` map (char -> child id). */
function edgesFromChildren(nodes) {
  const edges = [];
  for (const node of nodes) {
    const children = node.children;
    if (!children) continue;
    for (const childId of Object.values(children)) {
      edges.push({ from: node.id, to: childId });
    }
  }
  return edges;
}

export default function TrieCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const rawNodes = activeStep?.trieState;

  const { positioned, edges } = useMemo(() => {
    if (!rawNodes || rawNodes.length === 0) return { positioned: [], edges: [] };
    return {
      positioned: rawNodes.map((n) => ({
        id: n.id,
        character: n.character,
        endOfWord: Boolean(n.endOfWord),
        state: n.state || 'default',
        x: n.x,
        y: n.y
      })),
      edges: edgesFromChildren(rawNodes)
    };
  }, [rawNodes]);

  if (!positioned.length) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)' }}>
        No trie data available
      </div>
    );
  }

  // Compute viewBox from positioned nodes.
  const xs = positioned.map(n => n.x);
  const ys = positioned.map(n => n.y);
  const pad = 40;
  const minX = Math.min(...xs) - pad;
  const maxX = Math.max(...xs) + pad;
  const minY = Math.min(...ys) - pad;
  const maxY = Math.max(...ys) + pad;
  const width = maxX - minX;
  const height = maxY - minY;

  const nodeMap = new Map(positioned.map(n => [n.id, n]));

  return (
    <div style={{ flex: 1, width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'auto', padding: '8px' }}>
      <svg width="100%" height="100%" viewBox={`${minX} ${minY} ${width} ${height}`} style={{ overflow: 'visible', maxHeight: '100%' }}>
        {/* Edges */}
        {edges.map((edge, idx) => {
          const from = nodeMap.get(edge.from);
          const to = nodeMap.get(edge.to);
          if (!from || !to) return null;
          return (
            <line
              key={idx}
              x1={from.x} y1={from.y}
              x2={to.x} y2={to.y}
              stroke="var(--bench-rule-strong)"
              strokeWidth={2}
              style={{ transition: 'all 0.3s ease' }}
            />
          );
        })}

        {/* Nodes */}
        {positioned.map(node => {
          const s = stateStyle(node.state);
          const isActive = node.state === 'current' || node.state === 'visiting' || node.state === 'active';
          const radius = isActive ? 20 : 17;
          const label = node.character == null ? '•' : node.character;

          return (
            <g key={node.id} transform={`translate(${node.x}, ${node.y})`}>
              <circle
                r={radius}
                fill={s.fill}
                stroke={s.stroke}
                strokeWidth={isActive ? 3 : 2}
                style={{ transition: 'all 0.3s ease' }}
              />
              <text
                textAnchor="middle"
                dy=".35em"
                fill={s.text}
                fontSize="14"
                fontWeight="700"
                fontFamily="var(--font-code)"
              >
                {label}
              </text>
              {/* End-of-word marker */}
              {node.endOfWord && (
                <circle
                  cx={radius * 0.7}
                  cy={-radius * 0.7}
                  r={4}
                  fill="var(--settled)"
                  stroke="var(--settled-on)"
                  strokeWidth={1}
                />
              )}
            </g>
          );
        })}
      </svg>
    </div>
  );
}
