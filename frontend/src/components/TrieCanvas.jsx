import React, { useMemo } from 'react';

/**
 * Trie (prefix tree) visualizer.
 *
 * Trie problems currently fall through to GraphCanvas and render a blank SVG.
 * The backend serves trie data in `trieState` (a flat list of trie nodes) or in
 * `treeNodes` (if the tracer emits tree-shaped data). This canvas renders the
 * trie as a top-down tree where each node shows its character and whether it
 * marks the end of a word.
 *
 * Layout: simple recursive top-down. Each level gets equal vertical space; each
 * node's horizontal position is the midpoint of its children's span. This is the
 * same layout strategy as TreeCanvas but with variable branching factor.
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

/**
 * Builds a laid-out tree from the backend's flat node list.
 *
 * The trie data can come in two shapes:
 *   1. trieState: [{ id, char, isEnd, children: [...ids], state }]
 *   2. treeNodes: [{ id, val, x, y, leftId, rightId, state }] — binary tree shape
 *
 * For shape 1 we do our own layout. For shape 2 we use the provided x/y.
 */
function layoutTrie(nodes) {
  if (!nodes || nodes.length === 0) return { positioned: [], edges: [] };

  // If nodes already have x/y (treeNodes shape), use them directly.
  if (nodes[0]?.x !== undefined && nodes[0]?.y !== undefined) {
    const edges = [];
    for (const node of nodes) {
      if (node.leftId != null) edges.push({ from: node.id, to: node.leftId });
      if (node.rightId != null) edges.push({ from: node.id, to: node.rightId });
    }
    return {
      positioned: nodes.map(n => ({
        id: n.id, char: n.val ?? n.char ?? '', isEnd: n.isEnd ?? false,
        state: n.state || 'default', x: n.x, y: n.y
      })),
      edges
    };
  }

  // Shape 1: build adjacency and lay out from root.
  const byId = new Map(nodes.map(n => [n.id ?? n.char, n]));
  const root = nodes[0];
  const positioned = [];
  const edges = [];

  const NODE_W = 50;
  const NODE_H = 60;

  // First pass: count leaf descendants to determine widths.
  function leafCount(node) {
    const children = (node.children || []).map(cid => byId.get(cid)).filter(Boolean);
    if (children.length === 0) return 1;
    return children.reduce((sum, c) => sum + leafCount(c), 0);
  }

  // Second pass: assign positions.
  function layout(node, depth, leftEdge) {
    const children = (node.children || []).map(cid => byId.get(cid)).filter(Boolean);
    const totalLeaves = leafCount(node);
    const width = totalLeaves * NODE_W;
    const x = leftEdge + width / 2;
    const y = depth * NODE_H + 30;

    positioned.push({
      id: node.id ?? node.char,
      char: node.char ?? node.val ?? '',
      isEnd: node.isEnd ?? false,
      state: node.state || 'default',
      x, y
    });

    let childLeft = leftEdge;
    for (const child of children) {
      const childLeaves = leafCount(child);
      const childWidth = childLeaves * NODE_W;
      edges.push({ from: node.id ?? node.char, to: child.id ?? child.char });
      layout(child, depth + 1, childLeft);
      childLeft += childWidth;
    }
  }

  layout(root, 0, 0);

  return { positioned, edges };
}

export default function TrieCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;

  // Accept data from either trieState or treeNodes
  const rawNodes = activeStep?.trieState || activeStep?.treeNodes;

  const { positioned, edges } = useMemo(() => layoutTrie(rawNodes), [rawNodes]);

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
                {node.char}
              </text>
              {/* End-of-word marker */}
              {node.isEnd && (
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
