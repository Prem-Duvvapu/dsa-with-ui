import React from 'react';
import styles from './RecursionTreeCanvas.module.css';
import { Layers, ArrowDown } from 'lucide-react';

import { buildRecursionTree, layoutRecursionTree } from '../trace/recursionTree';
import derived from './DerivedRecursionTree.module.css';

const NODE_W = 88;
const NODE_H = 26;
const ROW_H = 62;
const LABEL_CHARS = 13;

/**
 * What to write in a node.
 *
 * The ARGUMENTS, not the function name. Every node in a recursion tree calls the same
 * function, so the name is the one part carrying no information - repeated once per node -
 * while the arguments are the only thing telling two nodes apart. Truncating from the front
 * got this exactly backwards: `backtrack(idx=0)` became `backtrack(…` and every node in the
 * permutations tree read identically.
 */
function nodeLabel(frame) {
  const open = frame.indexOf('(');
  const close = frame.lastIndexOf(')');
  const inner = open >= 0 && close > open ? frame.slice(open + 1, close).trim() : frame;
  const text = inner.length > 0 ? inner : frame;
  return text.length > LABEL_CHARS ? `${text.slice(0, LABEL_CHARS - 1)}…` : text;
}

/**
 * The tree a backtracking run explored, rebuilt from the call stacks it emitted.
 *
 * None of the twenty-five Recursion & Backtracking tracers emits `treeNodes`, so this
 * canvas had nothing to draw for them and they were routed to a stack or an array instead.
 * A stack shows how deep you are; it cannot show that you tried a branch, abandoned it, and
 * took the next one - which is the whole of backtracking.
 */
function DerivedRecursionTree({ steps, currentStepIndex }) {
  const { nodes, truncated } = buildRecursionTree(steps, currentStepIndex);
  if (!nodes.length) return null;

  const { positions, width, depth } = layoutRecursionTree(nodes);
  const svgW = Math.max(width * NODE_W + NODE_W, 240);
  const svgH = depth * ROW_H + NODE_H;
  const x = (id) => positions.get(id) * NODE_W + NODE_W / 2;
  const y = (d) => d * ROW_H + NODE_H / 2;

  return (
    <div className={derived.wrap} data-testid="derived-recursion-tree">
      <svg width={svgW} height={svgH} role="img" aria-label={`Recursion tree, ${nodes.length} calls explored`}>
        {nodes.filter((n) => n.parentId !== null).map((n) => {
          const parent = nodes[n.parentId];
          return (
            <line
              key={`e${n.id}`}
              x1={x(parent.id)} y1={y(parent.depth) + NODE_H / 2}
              x2={x(n.id)} y2={y(n.depth) - NODE_H / 2}
              className={`${derived.edge} ${derived[`edge_${n.state}`] || ''}`}
            />
          );
        })}
        {nodes.map((n) => (
          <g key={n.id} data-state={n.state} transform={`translate(${x(n.id) - NODE_W / 2}, ${y(n.depth) - NODE_H / 2})`}>
            <rect
              width={NODE_W} height={NODE_H} rx="6"
              className={`${derived.node} ${derived[`node_${n.state}`] || ''}`}
            />
            <text x={NODE_W / 2} y={NODE_H / 2 + 4} textAnchor="middle" className={derived.label}>
              {nodeLabel(n.label)}
              {/* The full frame, function name included, on hover. */}
              <title>{n.label}</title>
            </text>
          </g>
        ))}
      </svg>
      {truncated && (
        <p className={derived.note}>
          Showing the first {nodes.length} calls. The run explores more than fits here.
        </p>
      )}
    </div>
  );
}

export default function RecursionTreeCanvas({ problem, currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  const treeNodes = (activeStep?.treeNodes && activeStep.treeNodes.length > 0)
    ? activeStep.treeNodes
    : (problem?.defaultTreeNodes || []);
  const nodeStates = activeStep?.nodeStates || {};
  // Tracers that emit their own tree keep it; the rest have theirs rebuilt from callStack.
  const hasOwnTree = Boolean(activeStep?.treeNodes?.length);
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
    <div className={styles.wrap}>
      {/* Main SVG Recursion Tree Canvas */}
      <div className={styles.stage} data-testid="recursion-tree-stage">
        {!hasOwnTree ? (
          <DerivedRecursionTree steps={steps} currentStepIndex={currentStepIndex} />
        ) : treeNodes.length > 0 ? (
          <svg width="100%" height="250" viewBox="0 0 380 250" className={styles.svg}>
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
                <g key={`node-${node.id}`} transform={`translate(${node.x}, ${node.y})`} className={styles.nodeGroup}>
                  <rect
                    x={-boxWidth / 2}
                    y={-boxHeight / 2}
                    width={boxWidth}
                    height={boxHeight}
                    rx="8"
                    fill={colorInfo.fill}
                    stroke={colorInfo.stroke}
                    strokeWidth={isCalling || isMerging ? 2.5 : 1.5}
                    className={styles.nodeShape}
                    style={{ filter: colorInfo.glow !== 'none' ? `drop-shadow(${colorInfo.glow})` : 'none' }}
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
          <div className={styles.emptyNote}>
            Recursion Call Stack Active
          </div>
        )}
      </div>

      {/* Subarray State Bar Visualizer */}
      <div className={styles.strip}>
        <span className={styles.stripLabel}>
          Live Array State:
        </span>
        <div className={styles.stripCells}>
          {arrayState.map((el, idx) => (
            <div key={idx} className={styles.stripCell}>
              <span className={`${styles.stripValue}${el.state === 'sorted' || el.state === 'visited' ? ` ${styles.stripValueDone}` : ''}`}>
                {el.value}
              </span>
              <div
                className={[
                  styles.stripBar,
                  el.state === 'sorted' || el.state === 'visited' ? styles.stripBarDone : '',
                  el.state === 'active' || el.state === 'comparing' ? styles.stripBarActive : ''
                ].filter(Boolean).join(' ')}
              />
              <span className={styles.stripIndex}>[{idx}]</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
