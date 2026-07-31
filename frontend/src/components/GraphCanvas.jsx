import React from 'react';
import { Network, Grid } from 'lucide-react';

export default function GraphCanvas({ problem, currentStep }) {
  const nodeStates = currentStep?.nodeStates || {};
  const activeEdges = currentStep?.activeEdges || [];
  const gridState = currentStep?.gridState || problem?.defaultGrid;

  // Determine state color
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

  const getGridCellColor = (val) => {
    // 0 = water/empty/unvisited, 1 = fresh land/cell, 2 = rotten/target/safe, 3 = visiting, 4 = visited
    switch (val) {
      case 0: return { bg: '#1e293b', text: '0', color: '#64748b' };
      case 1: return { bg: 'rgba(59, 130, 246, 0.25)', text: '1', color: '#60a5fa', border: '1px solid #3b82f6' };
      case 2: return { bg: 'rgba(239, 68, 68, 0.3)', text: '2', color: '#f87171', border: '1px solid #ef4444' };
      case 3: return { bg: 'rgba(245, 158, 11, 0.4)', text: 'V', color: '#fbbf24', border: '1px solid #f59e0b' };
      case 4: return { bg: 'rgba(16, 185, 129, 0.3)', text: '✓', color: '#34d399', border: '1px solid #10b981' };
      default: return { bg: '#0f172a', text: String(val), color: '#94a3b8' };
    }
  };

  return (
    <div className="glass-panel" style={{ flex: 1, minHeight: '380px', padding: '20px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {gridState ? <Grid size={18} color="var(--accent-cyan)" /> : <Network size={18} color="var(--accent-indigo)" />}
          <span style={{ fontSize: '0.9rem', fontWeight: '700', letterSpacing: '0.4px' }}>
            {gridState ? '2D Matrix Grid Visualizer' : 'Graph Network Topology'}
          </span>
        </div>

        {/* Legend */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#475569' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Unvisited</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#f59e0b', boxShadow: 'var(--glow-amber)' }}></span>
            <span style={{ color: '#fbbf24' }}>In Queue/Stack</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#3b82f6' }}></span>
            <span style={{ color: '#60a5fa' }}>Visiting</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Visited</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', height: '100%', minHeight: '300px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 0, 0, 0.2)', borderRadius: '12px', overflow: 'hidden' }}>
        {gridState ? (
          /* Render 2D Grid Matrix */
          <div style={{ display: 'grid', gridTemplateColumns: `repeat(${gridState[0].length}, 60px)`, gap: '8px', padding: '20px' }}>
            {gridState.map((row, rIdx) =>
              row.map((val, cIdx) => {
                const cellStyle = getGridCellColor(val);
                return (
                  <div
                    key={`${rIdx}-${cIdx}`}
                    style={{
                      width: '60px',
                      height: '60px',
                      borderRadius: '10px',
                      background: cellStyle.bg,
                      border: cellStyle.border || '1px solid var(--border-color)',
                      color: cellStyle.color,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: '700',
                      fontSize: '1rem',
                      transition: 'all 0.3s ease',
                      boxShadow: val === 3 || val === 2 ? '0 0 15px rgba(245, 158, 11, 0.4)' : 'none'
                    }}
                  >
                    <span>{cellStyle.text}</span>
                    <span style={{ fontSize: '0.65rem', opacity: 0.6, marginTop: '2px' }}>
                      ({rIdx},{cIdx})
                    </span>
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
