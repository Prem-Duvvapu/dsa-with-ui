import React from 'react';
import { GitBranch, Layers, ArrowDown } from 'lucide-react';

export default function RecursionTreeCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const treeNodes = (activeStep?.treeNodes && activeStep.treeNodes.length > 0)
    ? activeStep.treeNodes
    : (problem?.defaultTreeNodes || []);
  const nodeStates = activeStep?.nodeStates || {};
  const arrayState = activeStep?.arrayState || problem?.defaultArray || [];

  const getNodeColor = (nodeId, explicitState) => {
    const state = explicitState || nodeStates[nodeId] || 'unvisited';
    switch (state) {
      case 'active':
      case 'calling':
        return { fill: 'var(--role-current)', stroke: 'var(--role-current-edge)', glow: '0 0 20px color-mix(in srgb, var(--role-current) 80%, transparent)', label: 'Calling' };
      case 'merging':
      case 'comparing':
        return { fill: 'var(--role-secondary)', stroke: 'var(--role-secondary-edge)', glow: '0 0 18px color-mix(in srgb, var(--role-secondary) 70%, transparent)', label: 'Merging' };
      case 'memo_hit':
      case 'cache_hit':
        return { fill: 'var(--role-cached)', stroke: 'var(--role-cached-edge)', glow: 'var(--glow-gold)', label: 'Cache Hit' };
      case 'pruned':
      case 'backtrack':
        return { fill: 'var(--role-pruned)', stroke: 'var(--role-pruned-edge)', glow: 'var(--glow-rose)', label: 'Backtracked' };
      case 'visited':
      case 'merged':
      case 'sorted':
        return { fill: 'var(--role-done)', stroke: 'var(--role-done-edge)', glow: '0 0 14px color-mix(in srgb, var(--role-done) 50%, transparent)', label: 'Sorted' };
      default:
        return { fill: 'var(--canvas-node-fill)', stroke: 'var(--canvas-edge)', glow: 'none', label: 'Pending' };
    }
  };

  return (
    <div style={{ flex: 1, padding: '14px 20px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <GitBranch size={18} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.92rem', fontWeight: '800', letterSpacing: '0.4px' }}>
            Divide & Conquer Recursion Tree Visualizer
          </span>
        </div>

        {/* Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: 'var(--canvas-edge)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Pending</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: 'var(--role-current)' }}></span>
            <span style={{ color: 'var(--role-current-edge)' }}>Splitting / Calling</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: 'var(--role-secondary)' }}></span>
            <span style={{ color: 'var(--role-secondary-edge)' }}>Merging</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: 'var(--role-done)' }}></span>
            <span style={{ color: 'var(--role-done-edge)' }}>Sorted</span>
          </div>
        </div>
      </div>

      {/* Main SVG Recursion Tree Canvas */}
      <div style={{ flex: 1, width: '100%', minHeight: '260px', background: 'var(--canvas-well)', borderRadius: '12px', overflow: 'auto', padding: '16px', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
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
                      stroke={nodeStates[leftChild.id] === 'pruned' ? 'var(--role-pruned)' : 'var(--canvas-edge)'}
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
                      stroke={nodeStates[rightChild.id] === 'pruned' ? 'var(--role-pruned)' : 'var(--canvas-edge)'}
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
                    fill="var(--role-ink)"
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
      <div style={{ marginTop: '14px', paddingTop: '12px', borderTop: '1px solid var(--border-default)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '12px' }}>
        <span style={{ fontSize: '0.78rem', color: 'var(--text-muted)', fontWeight: '700' }}>
          Live Array State:
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          {arrayState.map((el, idx) => (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '4px' }}>
              <span style={{ fontSize: '0.75rem', fontWeight: '800', color: el.state === 'sorted' || el.state === 'visited' ? 'var(--role-done-edge)' : 'var(--role-ink)' }}>
                {el.value}
              </span>
              <div
                style={{
                  width: '28px',
                  height: '14px',
                  borderRadius: '4px',
                  background: el.state === 'sorted' || el.state === 'visited' ? 'var(--role-done)' : (el.state === 'active' || el.state === 'comparing' ? 'var(--role-secondary)' : 'var(--canvas-node-fill-2)'),
                  border: '1px solid var(--border-strong)'
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
