import React from 'react';
import { Network, Grid, Crown, GitBranch, Layers } from 'lucide-react';

export default function GraphCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const nodeStates = activeStep?.nodeStates || {};
  const activeEdges = activeStep?.activeEdges || [];
  const gridState = activeStep?.gridState || problem?.defaultGrid;
  const variables = activeStep?.variables || {};

  // Determine state color for graph nodes using 4 semantic state tokens
  const getNodeColor = (nodeId, stateOverride) => {
    const st = stateOverride || nodeStates[nodeId] || 'unvisited';
    switch (st) {
      case 'queued':
      case 'visiting':
      case 'current':
      case 'active':
        return { fill: 'var(--state-current)', stroke: '#fbbf24', glow: 'var(--state-current-glow)' };
      case 'target':
      case 'root':
      case 'cycle':
        return { fill: 'var(--state-target)', stroke: '#a78bfa', glow: 'var(--state-target-glow)' };
      case 'visited':
      case 'processed':
        return { fill: 'var(--state-visited-bg)', stroke: 'var(--state-visited)', glow: 'none' };
      case 'done':
      case 'placed':
      case 'safe':
      case 'sorted':
        return { fill: 'var(--state-done)', stroke: '#2dd4bf', glow: 'var(--state-done-glow)' };
      default:
        return { fill: '#1e293b', stroke: 'var(--border-default)', glow: 'none' };
    }
  };

  const isSudoku = gridState && gridState.length === 9 && gridState[0].length === 9;
  const isChessboard = gridState && gridState.length === 4 && gridState[0].length === 4 && (problem?.id === 'n-queens' || problem?.title?.toLowerCase().includes('queen'));
  const isDsu = problem?.id === 'disjoint-set-dsu' || 
                problem?.title?.toLowerCase().includes('disjoint set') || 
                problem?.title?.toLowerCase().includes('dsu');

  // Parse DSU array strings if present
  const parentStr = variables['parent[]'] || '[0, 1, 2, 3, 4, 5, 6, 7]';
  const rankStr = variables['rank[]'] || '[0, 0, 0, 0, 0, 0, 0, 0]';
  const dsuSetsStr = variables['Disjoint Sets'] || '{1}, {2}, {3}, {4}, {5}, {6}, {7}';
  const dsuOpStr = variables['Operation'] || 'Initialize DSU(7)';

  const parentArr = parentStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());
  const rankArr = rankStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());

  return (
    <div style={{ flex: 1, padding: '12px 16px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          {isDsu ? <GitBranch size={16} color="var(--accent-violet)" /> : gridState ? <Grid size={16} color="var(--accent-violet)" /> : <Network size={16} color="var(--accent-violet)" />}
          <span style={{ fontSize: '0.86rem', fontWeight: '800', letterSpacing: '0.3px', color: 'var(--text-primary)' }}>
            {isDsu ? 'Disjoint Set Union (DSU) Visualizer' : isChessboard ? '4x4 Chessboard Visualizer (N-Queens)' : isSudoku ? '9x9 Sudoku Board Visualizer' : gridState ? '2D Matrix Grid Visualizer' : 'Graph Network Topology'}
          </span>
        </div>

        {/* 4 Semantic Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.72rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-current)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Current</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-target)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Target</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-visited)' }}></span>
            <span style={{ color: 'var(--text-muted)' }}>Visited</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-done)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Done</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', height: '100%', minHeight: '320px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflow: 'auto', padding: '16px' }}>
        {isDsu ? (
          /* Specialized DSU Component & Array Visualizer */
          <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '20px', alignItems: 'center' }}>
            {/* Current DSU Operation Banner */}
            <div style={{ display: 'flex', alignItems: 'center', gap: '10px', padding: '8px 16px', background: 'rgba(168, 85, 247, 0.15)', border: '1px solid rgba(168, 85, 247, 0.4)', borderRadius: '10px', boxShadow: '0 0 16px rgba(168, 85, 247, 0.3)' }}>
              <Layers size={18} color="#a855f7" />
              <span style={{ fontSize: '0.86rem', fontWeight: '800', color: '#ffffff' }}>
                {dsuOpStr}
              </span>
            </div>

            {/* Disjoint Sets Component Cards */}
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', justifyContent: 'center', maxWidth: '700px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', padding: '12px 18px', background: 'rgba(30, 41, 59, 0.6)', border: '1px solid rgba(255, 255, 255, 0.1)', borderRadius: '12px' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: '800', color: '#a855f7', textTransform: 'uppercase', letterSpacing: '0.6px' }}>
                  Connected Components / Disjoint Sets
                </span>
                <span style={{ fontSize: '0.95rem', fontWeight: '800', color: '#38bdf8' }}>
                  {dsuSetsStr}
                </span>
              </div>
            </div>

            {/* Parent & Rank Interactive Tables */}
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px', width: '100%', maxWidth: '650px', background: 'rgba(15, 23, 42, 0.6)', padding: '16px', borderRadius: '12px', border: '1px solid rgba(255, 255, 255, 0.08)' }}>
              {/* Element Index Header */}
              <div style={{ display: 'grid', gridTemplateColumns: '80px repeat(7, 1fr)', gap: '8px', alignItems: 'center', textAlign: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-muted)' }}>Element i</span>
                {[1, 2, 3, 4, 5, 6, 7].map(idx => (
                  <div key={idx} style={{ padding: '4px', background: 'rgba(255,255,255,0.04)', borderRadius: '6px', fontSize: '0.8rem', fontWeight: '800', color: '#ffffff' }}>
                    {idx}
                  </div>
                ))}
              </div>

              {/* parent[i] Row */}
              <div style={{ display: 'grid', gridTemplateColumns: '80px repeat(7, 1fr)', gap: '8px', alignItems: 'center', textAlign: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: '800', color: '#38bdf8' }}>parent[i]</span>
                {[1, 2, 3, 4, 5, 6, 7].map(idx => {
                  const val = parentArr[idx] || idx;
                  const isRoot = String(val) === String(idx);
                  return (
                    <div 
                      key={idx} 
                      style={{ 
                        padding: '8px 4px', 
                        background: isRoot ? 'rgba(56, 189, 248, 0.25)' : 'rgba(255,255,255,0.06)', 
                        border: isRoot ? '1px solid #38bdf8' : '1px solid rgba(255,255,255,0.1)',
                        boxShadow: isRoot ? '0 0 12px rgba(56, 189, 248, 0.4)' : 'none',
                        borderRadius: '8px', 
                        fontSize: '0.88rem', 
                        fontWeight: '800', 
                        color: isRoot ? '#38bdf8' : '#ffffff',
                        transition: 'all 0.3s ease'
                      }}
                    >
                      {val}
                    </div>
                  );
                })}
              </div>

              {/* rank[i] Row */}
              <div style={{ display: 'grid', gridTemplateColumns: '80px repeat(7, 1fr)', gap: '8px', alignItems: 'center', textAlign: 'center' }}>
                <span style={{ fontSize: '0.72rem', fontWeight: '800', color: '#a855f7' }}>rank[i]</span>
                {[1, 2, 3, 4, 5, 6, 7].map(idx => {
                  const rVal = rankArr[idx] || 0;
                  return (
                    <div key={idx} style={{ padding: '6px 4px', background: 'rgba(168, 85, 247, 0.15)', border: '1px solid rgba(168, 85, 247, 0.3)', borderRadius: '8px', fontSize: '0.82rem', fontWeight: '800', color: '#e9d5ff' }}>
                      {rVal}
                    </div>
                  );
                })}
              </div>
            </div>
          </div>
        ) : gridState ? (
          /* Render 2D Grid / Chessboard / Sudoku */
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: `repeat(${gridState[0].length}, minmax(${isSudoku ? '24px' : isChessboard ? '44px' : '36px'}, 1fr))`,
              gap: isSudoku ? '2px' : 'var(--space-xs)',
              padding: 'var(--space-md)',
              background: isSudoku ? '#0f172a' : 'transparent',
              borderRadius: 'var(--radius-md)',
              border: isSudoku ? '2px solid #334155' : 'none',
              maxWidth: '100%',
              overflow: 'auto'
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
                        width: '100%',
                        aspectRatio: '1',
                        minWidth: '44px',
                        minHeight: '44px',
                        borderRadius: 'var(--radius-xs)',
                        background: val === 1 ? 'rgba(16, 185, 129, 0.35)' : isLightSquare ? '#334155' : '#1e293b',
                        border: val === 1 ? '2px solid #34d399' : '1px solid var(--border-default)',
                        display: 'flex',
                        flexDirection: 'column',
                        alignItems: 'center',
                        justifyContent: 'center',
                        position: 'relative',
                        transition: 'all var(--motion-normal) var(--ease-standard)',
                        boxShadow: val === 1 ? 'var(--glow-emerald)' : 'none'
                      }}
                    >
                      {val === 1 ? (
                        <Crown size={24} color="#fbbf24" style={{ filter: 'drop-shadow(0 0 8px #f59e0b)' }} />
                      ) : (
                        <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)' }}>({rIdx},{cIdx})</span>
                      )}
                    </div>
                  );
                }

                if (isSudoku) {
                  const borderRight = (cIdx + 1) % 3 === 0 && cIdx !== 8 ? '2px solid var(--accent-violet)' : '1px solid #334155';
                  const borderBottom = (rIdx + 1) % 3 === 0 && rIdx !== 8 ? '2px solid var(--accent-violet)' : '1px solid #334155';

                  return (
                    <div
                      key={`sudoku-${rIdx}-${cIdx}`}
                      style={{
                        width: '100%',
                        aspectRatio: '1',
                        minWidth: '24px',
                        minHeight: '24px',
                        background: val !== 0 ? 'rgba(99, 102, 241, 0.25)' : '#1e293b',
                        borderRight,
                        borderBottom,
                        color: val !== 0 ? '#ffffff' : 'var(--text-muted)',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        fontWeight: '800',
                        fontSize: 'var(--text-sm)'
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
                    className={val !== 0 ? 'animate-ripple' : ''}
                    style={{
                      width: '55px',
                      height: '55px',
                      borderRadius: '8px',
                      background: val === 2 ? 'rgba(16, 185, 129, 0.35)' : val === 1 || val === 99 ? 'rgba(59, 130, 246, 0.25)' : '#1e293b',
                      border: val === 2 ? '1px solid #34d399' : val === 1 || val === 99 ? '1px solid #3b82f6' : '1px solid #334155',
                      boxShadow: val === 2 ? '0 0 14px rgba(16,185,129,0.5)' : val === 1 || val === 99 ? '0 0 14px rgba(59,130,246,0.5)' : 'none',
                      color: val !== 0 ? '#ffffff' : '#64748b',
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                      justifyContent: 'center',
                      fontWeight: '700',
                      fontSize: '0.9rem',
                      transition: 'all var(--motion-normal) var(--ease-standard)'
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
