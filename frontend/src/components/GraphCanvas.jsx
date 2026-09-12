import React from 'react';

/**
 * SVG node-link graph visualizer.
 *
 * This used to be 350 lines holding five visualizers behind ad-hoc predicates.
 * DSU, sudoku, chessboard, and 2D matrix are now in DsuCanvas and GridCanvas.
 * The header, icon row, and 4-badge legend are removed — CanvasShell owns those.
 *
 * What remains: edges (lines with optional weights and arrowheads) and nodes
 * (circles with state-driven fill), for BFS/DFS, Dijkstra, and general graph
 * problems.
 */

/** The most recent step that stated a graph, returned whole so nodes and edges agree. */
function lastTopology(steps, index, current) {
  const take = (s) => ({
    nodes: Array.isArray(s?.graphNodes) ? s.graphNodes : [],
    edges: Array.isArray(s?.graphEdges) ? s.graphEdges : []
  });
  const here = take(current);
  if (here.nodes.length) return here;
  if (Array.isArray(steps)) {
    for (let i = Math.min(index ?? steps.length - 1, steps.length - 1); i >= 0; i -= 1) {
      const found = take(steps[i]);
      if (found.nodes.length) return found;
    }
  }
  return { nodes: [], edges: [] };
}

/** Maps node state strings to Bench-token colours. */
function nodeStyle(state) {
  switch (state) {
    case 'queued':
    case 'visiting':
    case 'current':
    case 'active':
      return { fill: 'var(--probe)', stroke: 'var(--probe)', glow: true };
    case 'target':
    case 'root':
    case 'cycle':
      return { fill: 'var(--bench-fill)', stroke: 'var(--probe)', glow: false };
    case 'visited':
    case 'processed':
      return { fill: 'var(--bench-fill)', stroke: 'var(--bench-rule-strong)', glow: false };
    case 'done':
    case 'placed':
    case 'safe':
    case 'sorted':
      return { fill: 'var(--settled)', stroke: 'var(--settled)', glow: true };
    default:
      return { fill: 'var(--bench-fill)', stroke: 'var(--bench-rule)', glow: false };
  }
}

export default function GraphCanvas({ problem, currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  const nodeStates = activeStep?.nodeStates && typeof activeStep.nodeStates === 'object'
    ? activeStep.nodeStates
    : {};
  const activeEdges = Array.isArray(activeStep?.activeEdges) ? activeStep.activeEdges : [];

  // A step's nodes make its whole topology authoritative. In particular, an explicitly
  // edgeless trace graph must not be joined with stale catalogue edges whose endpoints
  // happen to share ids. Defaults exist only for traces that carry no topology yet.
  // Absence is not "show the default". Tracers restate a structure only on the steps
  // that change it, so falling through to the catalogue default drew the CATALOGUE's
  // data over the caller's own input. Measured app-wide: 1506 steps across 142 of 232
  // problems. The default is honest only before any step has emitted anything.
  // Nodes and edges must come from the SAME step. Carrying the nodes forward while reading
  // edges off the current one would draw a carried topology with no edges at all - a graph
  // as a field of disconnected dots.
  const carried = lastTopology(steps, currentStepIndex, activeStep);
  const hasStepTopology = carried.nodes.length > 0;
  const nodes = hasStepTopology
    ? carried.nodes
    : Array.isArray(problem?.defaultGraphNodes) ? problem.defaultGraphNodes : [];
  const edges = hasStepTopology
    ? carried.edges
    : Array.isArray(problem?.defaultGraphEdges) ? problem.defaultGraphEdges : [];

  if (!nodes.length) {
    return (
      <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)' }}>
        No graph data available
      </div>
    );
  }

  return (
    <div style={{ flex: 1, width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'auto', padding: '8px' }}>
      <svg width="100%" height="320" viewBox="0 0 360 330" style={{ overflow: 'visible' }}>
        <defs>
          <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="var(--bench-ink-dim)" />
          </marker>
          <marker id="arrowhead-active" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
            <polygon points="0 0, 10 3.5, 0 7" fill="var(--probe)" />
          </marker>
        </defs>

        {/* Edges */}
        {edges.map((edge, idx) => {
          const u = nodes.find(n => n.id === edge.from);
          const v = nodes.find(n => n.id === edge.to);
          if (!u || !v) return null;

          const isActive = activeEdges.includes(`${edge.from}-${edge.to}`)
            || activeEdges.includes(`${edge.to}-${edge.from}`);

          return (
            <g key={idx}>
              <line
                x1={u.x} y1={u.y}
                x2={v.x} y2={v.y}
                stroke={isActive ? 'var(--probe)' : 'var(--bench-rule)'}
                strokeWidth={isActive ? 3 : 2}
                strokeDasharray={isActive ? '5,5' : 'none'}
                markerEnd={edge.directed ? (isActive ? 'url(#arrowhead-active)' : 'url(#arrowhead)') : ''}
                style={{ transition: 'all 0.3s ease' }}
              />
              {edge.weight !== null && edge.weight !== undefined && (
                <text
                  x={(u.x + v.x) / 2}
                  y={(u.y + v.y) / 2 - 6}
                  fill="var(--bench-ink-secondary)"
                  fontSize="12"
                  fontWeight="600"
                  fontFamily="var(--font-code)"
                  textAnchor="middle"
                >
                  {edge.weight}
                </text>
              )}
            </g>
          );
        })}

        {/* Nodes */}
        {nodes.map(node => {
          const state = nodeStates[node.id] || 'unvisited';
          const s = nodeStyle(state);
          const isVisiting = state === 'visiting' || state === 'current' || state === 'active';

          return (
            <g key={node.id} transform={`translate(${node.x}, ${node.y})`}>
              <circle
                r={isVisiting ? 22 : 19}
                fill={s.fill}
                stroke={s.stroke}
                strokeWidth={isVisiting ? 3 : 2}
                style={{
                  transition: 'all 0.3s ease',
                  filter: s.glow ? 'drop-shadow(0 0 8px currentColor)' : 'none'
                }}
              />
              <text
                textAnchor="middle"
                dy=".3em"
                fill={state === 'done' || state === 'placed' ? 'var(--settled-on)' : state === 'current' || state === 'visiting' || state === 'active' ? 'var(--probe-on)' : 'var(--bench-ink)'}
                fontSize="13"
                fontWeight="700"
                fontFamily="var(--font-code)"
              >
                {node.label}
              </text>
            </g>
          );
        })}
      </svg>
    </div>
  );
}
