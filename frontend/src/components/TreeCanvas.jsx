import React from 'react';
import { GitCommit, Sparkles } from 'lucide-react';

export default function TreeCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const treeNodes = activeStep?.treeNodes?.length
    ? activeStep.treeNodes
    : (problem?.defaultTreeNodes || []);
  const nodeStates = activeStep?.nodeStates || {};
  const nodeXs = treeNodes.map((node) => node.x);
  const nodeYs = treeNodes.map((node) => node.y);
  const viewBoxX = Math.min(0, ...nodeXs.map((x) => x - 24));
  const viewBoxY = Math.min(0, ...nodeYs.map((y) => y - 24));
  const viewBoxWidth = Math.max(360, Math.max(0, ...nodeXs) + 24 - viewBoxX);
  const viewBoxHeight = Math.max(300, Math.max(0, ...nodeYs) + 24 - viewBoxY);

  const getNodeColor = (nodeId, explicitState) => {
    const state = explicitState || nodeStates[nodeId] || 'unvisited';
    switch (state) {
      case 'active':
      case 'visiting':
      case 'current':
      case 'queued':
        return { fill: 'var(--state-current)', stroke: '#fbbf24', glow: 'var(--state-current-glow)' };
      case 'target':
      case 'root':
      case 'found':
        return { fill: 'var(--state-target)', stroke: '#a78bfa', glow: 'var(--state-target-glow)' };
      case 'visited':
      case 'processed':
        return { fill: 'var(--state-visited-bg)', stroke: 'var(--state-visited)', glow: 'none' };
      case 'done':
      case 'completed':
      case 'sorted':
        return { fill: 'var(--state-done)', stroke: '#2dd4bf', glow: 'var(--state-done-glow)' };
      default:
        return { fill: '#1e293b', stroke: 'var(--border-default)', glow: 'none' };
    }
  };

  return (
    <div style={{ flex: 1, padding: '12px 16px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <GitCommit size={16} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.86rem', fontWeight: '800', letterSpacing: '0.3px', color: 'var(--text-primary)' }}>
            Binary tree & BST topology visualizer
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

      <div style={{ flex: 1, width: '100%', height: '100%', minHeight: '260px', display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'rgba(0, 0, 0, 0.25)', borderRadius: 'var(--radius-md)', overflow: 'hidden', padding: 'var(--space-md)' }}>
        <svg width="100%" height="100%" viewBox={`${viewBoxX} ${viewBoxY} ${viewBoxWidth} ${viewBoxHeight}`} preserveAspectRatio="xMidYMid meet" style={{ overflow: 'visible', maxHeight: '100%' }}>
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
            const nodeState = node.state || nodeStates[node.id] || 'unvisited';
            const colorInfo = getNodeColor(node.id, nodeState);
            const isVisiting = nodeState === 'visiting';

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
