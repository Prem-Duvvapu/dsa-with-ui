import React from 'react';
import { Network, Grid, Crown } from 'lucide-react';

export default function GraphCanvas({ problem, currentStep }) {
  const nodeStates = currentStep?.nodeStates || {};
  const activeEdges = currentStep?.activeEdges || [];
  const gridState = currentStep?.gridState || problem?.defaultGrid;

  // Determine state color for graph nodes
  const getNodeColor = (nodeId, stateOverride) => {
    const st = stateOverride || nodeStates[nodeId] || 'unvisited';
    switch (st) {
      case 'queued': return { fill: '#f59e0b', stroke: '#fbbf24', glow: '0 0 15px rgba(245,158,11,0.6)' };
      case 'visiting': return { fill: '#3b82f6', stroke: '#60a5fa', glow: '0 0 20px rgba(59,130,246,0.8)' };
      case 'visited': return { fill: '#10b981', stroke: '#34d399', glow: '0 0 12px rgba(16,185,129,0.5)' };
      case 'cycle': return { fill: '#ef4444', stroke: '#f87171', glow: '0 0 22px rgba(239,68,68,0.8)' };
      default: return { fill: '#1e293b', stroke: '#475569', glow: 'none' };
    }
  };

  const isSudoku = gridState && gridState.length === 9 && gridState[0].length === 9;
  const isChessboard = gridState && gridState.length === 4 && gridState[0].length === 4 && (problem?.id === 'n-queens' || problem?.title?.toLowerCase().includes('queen'));

  return (
    <div className="glass-panel" style={{ flex: 1, minHeight: '440px', padding: '20px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {gridState ? <Grid size={18} color="var(--accent-cyan)" /> : <Network size={18} color="var(--accent-indigo)" />}
          <span style={{ fontSize: '0.9rem', fontWeight: '800', letterSpacing: '0.4px' }}>
            {isChessboard ? '4x4 Chessboard Visualizer (N-Queens)' : isSudoku ? '9x9 Sudoku Board Visualizer' : gridState ? '2D Matrix Grid Visualizer' : 'Graph Network Topology'}
          </span>
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#475569' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Unvisited</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#3b82f6' }}></span>
            <span style={{ color: '#60a5fa' }}>Visiting / Active</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Placed / Safe</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', height: '100%', minHeight: '320px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflow: 'auto', padding: '16px' }}>
        {gridState ? (
          /* Render 2D Grid / Chessboard / Sudoku */
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${gridState[0].length}, ${isSudoku ? '36px' : isChessboard ? '70px' : '55px'})`,
              gap: isSudoku ? '2px' : '6px',
              padding: '16px',
              background: isSudoku ? '#0f172a' : 'transparent',
              borderRadius: '12px',
              border: isSudoku ? '2px solid #334155' : 'none'
            }}
          >
            {gridState.map((row, rIdx) =>
              row.map((val, cIdx) => {
                const isLightSquare = (rIdx + cIdx) % 2 === 0;

                if (isChessboard) {
                  return (
                    <div
                      key={`queen-${rIdx}-${cIdx}`}
                      style={{
                        width: '70px',
                        height: '70px',
                        borderRadius: '8px',
                        background: val === 1 ? 'rgba(16, 185, 129, 0.35)' : isLightSquare ? '#334155' : '#1e293b',
                        border: val === 1 ? '2px solid #34d399' : '1px solid rgba(255, 255, 255, 0.08)',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justify: 'center',
                        position: 'relative',
                        transition: 'all 0.3s ease',
                        boxShadow: val === 1 ? '0 0 18px rgba(16, 185, 129, 0.5)' : 'none'
                      }}
                    >
                      {val === 1 ? (
                        <Crown size={28} color="#fbbf24" style={{ filter: 'drop-shadow(0 0 8px #f59e0b)' }} />
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: '#64748b' }}>({rIdx},{cIdx})</span>
                      )}
                    </div>
                  );
                }

                if (isSudoku) {
                  const borderRight = (cIdx + 1) % 3 === 0 && cIdx !== 8 ? '2px solid #6366f1' : '1px solid #334155';
                  const borderBottom = (rIdx + 1) % 3 === 0 && rIdx !== 8 ? '2px solid #6366f1' : '1px solid #334155';

                  return (
                    <div
                      key={`sudoku-${rIdx}-${cIdx}`}
                      style={{
                        width: '36px',
                        height: '36px',
                        background: val !== 0 ? 'rgba(99, 102, 241, 0.25)' : '#1e293b',
                        borderRight,
                        borderBottom,
                        color: val !== 0 ? '#ffffff' : '#475569',
                        display: 'flex',
                        alignItems: 'center',
                        justify: 'center',
                        fontWeight: '800',
                        fontSize: '0.95rem'
                      }}
                    >
                      {val !== 0 ? val : '.'}
                    </div>
                  );
                }

                // Default 2D Matrix
                return (
                  <div
                    key={`grid-${rIdx}-${cIdx}`}
                    style={{
                      width: '55px',
                      height: '55px',
                      borderRadius: '8px',
                      background: val === 1 || val === 2 ? 'rgba(59, 130, 246, 0.25)' : '#1e293b',
                      border: val === 1 || val === 2 ? '1px solid #3b82f6' : '1px solid #334155',
                      color: val !== 0 ? '#ffffff' : '#64748b',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justify: 'center',
                      fontWeight: '700',
                      fontSize: '0.9rem'
                    }}
                  >
                    <span>{val}</span>
                    <span style={{ fontSize: '0.6rem', opacity: 0.6 }}>({rIdx},{cIdx})</span>
                  </div>
                );
              })
            )}
          </div>
        ) : (
          /* Render SVG Graph */
          <svg width="100%" height="320" viewBox="0 0 360 330" style={{ overflow: 'visible' }}>
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#64748b" />
              </marker>
              <marker id="arrowhead-active" markerWidth="10" markerHeight="7" refX="28" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#3b82f6" />
              </marker>
            </defs>

            {/* Render Edges */}
            {(problem?.defaultGraphEdges || []).map((edge, idx) => {
              const u = (problem?.defaultGraphNodes || []).find((n) => n.id === edge.from);
              const v = (problem?.defaultGraphNodes || []).find((n) => n.id === edge.to);
              if (!u || !v) return null;

              const isEdgeActive = activeEdges.includes(`${edge.from}-${edge.to}`) || activeEdges.includes(`${edge.to}-${edge.from}`);

              return (
                <g key={idx}>
                  <line
                    x1={u.x}
                    y1={u.y}
                    x2={v.x}
                    y2={v.y}
                    stroke={isEdgeActive ? '#3b82f6' : '#334155'}
                    strokeWidth={isEdgeActive ? 3 : 2}
                    strokeDasharray={isEdgeActive ? '5,5' : 'none'}
                    markerEnd={edge.directed ? (isEdgeActive ? 'url(#arrowhead-active)' : 'url(#arrowhead)') : ''}
                    style={{ transition: 'all 0.3s ease' }}
                  />
                  {edge.weight && (
                    <text
                      x={(u.x + v.x) / 2}
                      y={(u.y + v.y) / 2 - 6}
                      fill="#94a3b8"
                      fontSize="12"
                      fontWeight="600"
                      textAnchor="middle"
                    >
                      {edge.weight}
                    </text>
                  )}
                </g>
              );
            })}

            {/* Render Nodes */}
            {(problem?.defaultGraphNodes || []).map((node) => {
              const colorInfo = getNodeColor(node.id);
              const isVisiting = nodeStates[node.id] === 'visiting';

              return (
                <g key={node.id} transform={`translate(${node.x}, ${node.y})`} style={{ cursor: 'pointer' }}>
                  <circle
                    r={isVisiting ? 22 : 19}
                    fill={colorInfo.fill}
                    stroke={colorInfo.stroke}
                    strokeWidth={isVisiting ? 3 : 2}
                    style={{
                      transition: 'all 0.3s ease',
                      filter: colorInfo.glow !== 'none' ? `drop-shadow(${colorInfo.glow})` : 'none'
                    }}
                  />
                  <text
                    textAnchor="middle"
                    dy=".3em"
                    fill="#ffffff"
                    fontSize="13"
                    fontWeight="700"
                  >
                    {node.label}
                  </text>
                </g>
              );
            })}
          </svg>
        )}
      </div>
    </div>
  );
}
