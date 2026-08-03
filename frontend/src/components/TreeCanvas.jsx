import React from 'react';
import { GitCommit, Sparkles } from 'lucide-react';

export default function TreeCanvas({ problem, currentStep }) {
  const treeNodes = problem?.defaultTreeNodes || [];
  const nodeStates = currentStep?.nodeStates || {};

  const getNodeColor = (nodeId) => {
    const state = nodeStates[nodeId] || 'unvisited';
    switch (state) {
      case 'active':
      case 'queued':
        return { fill: '#f59e0b', stroke: '#fbbf24', glow: '0 0 16px rgba(245,158,11,0.7)' };
      case 'visiting':
        return { fill: '#3b82f6', stroke: '#60a5fa', glow: '0 0 20px rgba(59,130,246,0.8)' };
      case 'visited':
        return { fill: '#10b981', stroke: '#34d399', glow: '0 0 14px rgba(16,185,129,0.5)' };
      case 'burned':
      case 'cycle':
        return { fill: '#ef4444', stroke: '#f87171', glow: '0 0 22px rgba(239,68,68,0.8)' };
      default:
        return { fill: '#1e293b', stroke: '#475569', glow: 'none' };
    }
  };

  return (
    <div style={{ flex: 1, padding: '14px 20px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <GitCommit size={18} color="var(--accent-purple)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700', letterSpacing: '0.4px' }}>
            Binary Tree & BST Topology Visualizer
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
            <span style={{ color: '#60a5fa' }}>Visiting</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Visited</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', height: '100%', minHeight: '260px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 0, 0, 0.25)', borderRadius: 'var(--radius-md)', overflow: 'hidden', padding: 'var(--space-md)' }}>
        <svg width="100%" height="100%" viewBox="0 0 360 300" preserveAspectRatio="xMidYMid meet" style={{ overflow: 'visible', maxHeight: '100%' }}>
          {/* Render Parent-Child Connecting Lines */}
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
                    stroke="#475569"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                  />
                )}
                {rightChild && (
                  <line
                    x1={node.x}
                    y1={node.y}
                    x2={rightChild.x}
                    y2={rightChild.y}
                    stroke="#475569"
                    strokeWidth="2.5"
                    strokeLinecap="round"
                  />
                )}
              </g>
            );
          })}

          {/* Render Tree Nodes */}
          {treeNodes.map((node) => {
            const colorInfo = getNodeColor(node.id);
            const isVisiting = nodeStates[node.id] === 'visiting';

            return (
              <g key={`node-${node.id}`} transform={`translate(${node.x}, ${node.y})`} style={{ cursor: 'pointer' }}>
                <circle
                  r={isVisiting ? 21 : 18}
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
                  {node.val}
                </text>
              </g>
            );
          })}
        </svg>
      </div>
    </div>
  );
}
