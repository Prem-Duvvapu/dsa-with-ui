import React from 'react';
import { GitBranch, Layers, ArrowDown } from 'lucide-react';

export default function RecursionTreeCanvas({ problem, currentStep }) {
  const treeNodes = (currentStep?.treeNodes && currentStep.treeNodes.length > 0)
    ? currentStep.treeNodes
    : (problem?.defaultTreeNodes || []);
  const nodeStates = currentStep?.nodeStates || {};
  const arrayState = currentStep?.arrayState || problem?.defaultArray || [];

  const getNodeColor = (nodeId, explicitState) => {
    const state = explicitState || nodeStates[nodeId] || 'unvisited';
    switch (state) {
      case 'active':
      case 'calling':
        return { fill: '#3b82f6', stroke: '#60a5fa', glow: '0 0 20px rgba(59, 130, 246, 0.8)', label: 'Calling' };
      case 'merging':
      case 'comparing':
        return { fill: '#f59e0b', stroke: '#fbbf24', glow: '0 0 18px rgba(245, 158, 11, 0.7)', label: 'Merging' };
      case 'memo_hit':
      case 'cache_hit':
        return { fill: '#eab308', stroke: '#fde047', glow: 'var(--glow-gold)', label: 'Cache Hit' };
      case 'pruned':
      case 'backtrack':
        return { fill: '#ef4444', stroke: '#f87171', glow: 'var(--glow-rose)', label: 'Backtracked' };
      case 'visited':
      case 'merged':
      case 'sorted':
        return { fill: '#10b981', stroke: '#34d399', glow: '0 0 14px rgba(16, 185, 129, 0.5)', label: 'Sorted' };
      default:
        return { fill: '#1e293b', stroke: '#475569', glow: 'none', label: 'Pending' };
    }
  };

  return (
    <div style={{ flex: 1, padding: '14px 20px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <GitBranch size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.92rem', fontWeight: '800', letterSpacing: '0.4px' }}>
            Divide & Conquer Recursion Tree Visualizer
          </span>
        </div>

        {/* Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#475569' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Pending</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#3b82f6' }}></span>
            <span style={{ color: '#60a5fa' }}>Splitting / Calling</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#f59e0b' }}></span>
            <span style={{ color: '#fbbf24' }}>Merging</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Sorted</span>
          </div>
        </div>
      </div>

      {/* Main SVG Recursion Tree Canvas */}
      <div style={{ flex: 1, width: '100%', minHeight: '260px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflow: 'auto', padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        {treeNodes.length > 0 ? (
          <svg width="100%" height="250" viewBox="0 0 380 250" style={{ overflow: 'visible' }}>
            {/* Connecting Call Branch Lines */}
            {treeNodes.map((node) => {
              const leftChild = treeNodes.find((n) => n.id === node.leftId);
              const rightChild = treeNodes.find((n) => n.id === node.rightId);

              return (
                <g key={`lines-${node.id}`}>
                  {leftChild && (
                    <line
                      x1={node.x}
                      y1={node.y}
                      x2={leftChild.x}
                      y2={leftChild.y}
                      stroke={nodeStates[leftChild.id] === 'pruned' ? '#ef4444' : '#475569'}
                      strokeWidth="2"
                      strokeDasharray={nodeStates[leftChild.id] ? 'none' : '4 4'}
                    />
                  )}
                  {rightChild && (
                    <line
                      x1={node.x}
                      y1={node.y}
                      x2={rightChild.x}
                      y2={rightChild.y}
                      stroke={nodeStates[rightChild.id] === 'pruned' ? '#ef4444' : '#475569'}
                      strokeWidth="2"
                      strokeDasharray={nodeStates[rightChild.id] ? 'none' : '4 4'}
                    />
                  )}
                </g>
              );
            })}

            {/* Recursion Tree Nodes */}
            {treeNodes.map((node) => {
              const nodeState = node.state || nodeStates[node.id] || 'unvisited';
              const colorInfo = getNodeColor(node.id, nodeState);
              const isCalling = nodeState === 'calling' || nodeState === 'active';
              const isMerging = nodeState === 'merging';
              const textStr = String(node.val || '');
              const boxWidth = Math.max(74, textStr.length * 6.5 + 16);
              const boxHeight = 32;

              return (
                <g key={`node-${node.id}`} transform={`translate(${node.x}, ${node.y})`} style={{ cursor: 'pointer' }}>
                  <rect
                    x={-boxWidth / 2}
                    y={-boxHeight / 2}
                    width={boxWidth}
                    height={boxHeight}
                    rx="8"
                    fill={colorInfo.fill}
                    stroke={colorInfo.stroke}
                    strokeWidth={isCalling || isMerging ? 2.5 : 1.5}
                    style={{
                      transition: 'all var(--motion-normal) var(--ease-standard)',
                      filter: colorInfo.glow !== 'none' ? `drop-shadow(${colorInfo.glow})` : 'none'
                    }}
                  />
                  <text
                    textAnchor="middle"
                    dy=".3em"
                    fill="#ffffff"
                    fontSize="10"
                    fontWeight="800"
                    letterSpacing="0.2px"
                  >
                    {textStr}
                  </text>
                </g>
              );
            })}
          </svg>
        ) : (
          <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>
            Recursion Call Stack Active
          </div>
        )}
      </div>

      {/* Subarray State Bar Visualizer */}
      <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid var(--border-color)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px' }}>
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: '700' }}>
          Live Array State:
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {arrayState.map((el, idx) => (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: '800', color: el.state === 'sorted' || el.state === 'visited' ? '#34d399' : '#ffffff' }}>
                {el.value}
              </span>
              <div
                style={{
                  width: '28px',
                  height: '14px',
                  borderRadius: '4px',
                  background: el.state === 'sorted' || el.state === 'visited' ? '#10b981' : (el.state === 'active' || el.state === 'comparing' ? '#f59e0b' : '#334155'),
                  border: '1px solid rgba(255, 255, 255, 0.2)'
                }}
              />
              <span style={{ fontSize: '0.65rem', color: 'var(--text-muted)' }}>[{idx}]</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
