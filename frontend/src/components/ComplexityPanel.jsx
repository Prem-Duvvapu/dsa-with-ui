import React from 'react';
import { Cpu, HardDrive, HelpCircle, Zap } from 'lucide-react';

export default function ComplexityPanel({ complexity: complexityProp, problem }) {
  const complexity = complexityProp || problem?.complexity;

  if (!complexity) {
    return (
      <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px', overflowY: 'auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
          <Zap size={18} color="var(--accent-amber)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700' }}>Time & Space Complexity Proof</span>
        </div>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.82rem', fontStyle: 'italic' }}>
          Complexity analysis proof loading...
        </div>
      </div>
    );
  }

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px', overflowY: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
        <Zap size={18} color="var(--accent-amber)" />
        <span style={{ fontSize: '0.9rem', fontWeight: '700' }}>Complexity Analysis (How & Why)</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
        {/* Time Complexity Card */}
        <div style={{
          background: 'rgba(99, 102, 241, 0.08)',
          borderRadius: '10px',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          padding: '10px 14px',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--accent-indigo)' }}>
              <Cpu size={16} />
              <span style={{ fontWeight: '700', fontSize: '0.85rem' }}>Time Complexity</span>
            </div>
            <span style={{
              fontSize: '0.95rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#818cf8',
              background: 'rgba(99, 102, 241, 0.25)',
              padding: '2px 8px',
              borderRadius: '6px'
            }}>
              {complexity.timeComplexity || 'O(N)'}
            </span>
          </div>

          {complexity.timeExplanation && (
            <div style={{ fontSize: '0.78rem', color: 'var(--text-primary)', lineHeight: '1.4' }}>
              <strong>Proof: </strong>{complexity.timeExplanation}
            </div>
          )}
        </div>

        {/* Space Complexity Card */}
        <div style={{
          background: 'rgba(16, 185, 129, 0.08)',
          borderRadius: '10px',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          padding: '10px 14px',
          display: 'flex',
          flexDirection: 'column',
          gap: '6px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#34d399' }}>
              <HardDrive size={16} />
              <span style={{ fontWeight: '700', fontSize: '0.85rem' }}>Space Complexity</span>
            </div>
            <span style={{
              fontSize: '0.95rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#34d399',
              background: 'rgba(16, 185, 129, 0.25)',
              padding: '2px 8px',
              borderRadius: '6px'
            }}>
              {complexity.spaceComplexity || 'O(1)'}
            </span>
          </div>

          {complexity.spaceExplanation && (
            <div style={{ fontSize: '0.78rem', color: 'var(--text-primary)', lineHeight: '1.4' }}>
              <strong>Auxiliary Space: </strong>{complexity.spaceExplanation}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
