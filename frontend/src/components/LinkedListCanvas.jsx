import React from 'react';
import { Link2, ArrowRight } from 'lucide-react';

export default function LinkedListCanvas({ problem, currentStep }) {
  const listState = currentStep?.listState || problem?.defaultList || [];

  const getNodeColor = (state) => {
    switch (state) {
      case 'active':
      case 'curr':
        return { fill: '#3b82f6', stroke: '#60a5fa', glow: '0 0 18px rgba(59,130,246,0.8)' };
      case 'slow':
        return { fill: '#f59e0b', stroke: '#fbbf24', glow: '0 0 16px rgba(245,158,11,0.7)' };
      case 'fast':
        return { fill: '#ec4899', stroke: '#f472b6', glow: '0 0 18px rgba(236,72,153,0.8)' };
      case 'visited':
        return { fill: '#10b981', stroke: '#34d399', glow: '0 0 14px rgba(16,185,129,0.5)' };
      default:
        return { fill: '#1e293b', stroke: '#475569', glow: 'none' };
    }
  };

  return (
    <div style={{ flex: 1, padding: '14px 20px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Link2 size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700', letterSpacing: '0.4px' }}>
            Linked List Topology Visualizer
          </span>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', minHeight: '280px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '16px', padding: '20px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflowX: 'auto' }}>
        {listState.map((node, idx) => {
          const colorInfo = getNodeColor(node.state);

          return (
            <React.Fragment key={node.id}>
              {/* Linked List Node Box */}
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  borderRadius: '12px',
                  background: colorInfo.fill,
                  border: `2px solid ${colorInfo.stroke}`,
                  boxShadow: colorInfo.glow,
                  padding: '12px 18px',
                  transition: 'all 0.3s ease',
                  gap: '12px'
                }}
              >
                <div style={{ fontSize: '1rem', fontWeight: '800', color: '#ffffff' }}>
                  {node.val}
                </div>
                <div style={{ width: '1px', height: '24px', background: 'rgba(255, 255, 255, 0.2)' }} />
                <div style={{ fontSize: '0.72rem', color: '#cbd5e1', fontWeight: '600' }}>
                  {node.nextId !== null ? `next -> [${node.nextId}]` : 'next -> NULL'}
                </div>
              </div>

              {/* Arrow Connection */}
              {idx < listState.length - 1 && (
                <ArrowRight size={22} color="#64748b" style={{ flexShrink: 0 }} />
              )}
            </React.Fragment>
          );
        })}
      </div>
    </div>
  );
}
