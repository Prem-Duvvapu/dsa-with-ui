import React from 'react';
import { Cpu, HardDrive, HelpCircle, Zap } from 'lucide-react';

export default function ComplexityPanel({ complexity: complexityProp, problem }) {
  const complexity = complexityProp || problem?.complexity;

  // Custom space complexity for sliding window or string problems if applicable
  const timeBadge = complexity?.timeComplexity || 'O(N)';
  const spaceBadge = problem?.id === 'longest-substring-without-repeating' 
    ? 'O(min(m, n))' 
    : (complexity?.spaceComplexity || 'O(1)');

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px', overflowY: 'auto' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', paddingBottom: '6px', borderBottom: '1px solid var(--border-color)', flexShrink: 0 }}>
        <Zap size={16} color="var(--accent-amber)" />
        <span style={{ fontSize: '0.86rem', fontWeight: '800', color: '#ffffff' }}>Complexity Analysis (How & Why)</span>
      </div>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', flex: 1 }}>
        {/* Time Complexity Card */}
        <div style={{
          background: 'rgba(99, 102, 241, 0.08)',
          borderRadius: '8px',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          padding: '8px 10px',
          display: 'flex',
          flexDirection: 'column',
          gap: '4px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: 'var(--accent-indigo)' }}>
              <Cpu size={14} />
              <span style={{ fontWeight: '700', fontSize: '0.8rem' }}>Time Complexity</span>
            </div>
            <span style={{
              fontSize: '0.85rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#818cf8',
              background: 'rgba(99, 102, 241, 0.25)',
              padding: '2px 8px',
              borderRadius: '6px'
            }}>
              {timeBadge}
            </span>
          </div>

          <div style={{ fontSize: '0.74rem', color: 'var(--text-primary)', lineHeight: '1.35' }}>
            <strong>Proof: </strong>{complexity?.timeExplanation || 'Single pass iteration through string/array with sliding window pointers.'}
          </div>
        </div>

        {/* Space Complexity Card */}
        <div style={{
          background: 'rgba(16, 185, 129, 0.08)',
          borderRadius: '8px',
          border: '1px solid rgba(16, 185, 129, 0.3)',
          padding: '8px 10px',
          display: 'flex',
          flexDirection: 'column',
          gap: '4px'
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#34d399' }}>
              <HardDrive size={14} />
              <span style={{ fontWeight: '700', fontSize: '0.8rem' }}>Space Complexity</span>
            </div>
            <span style={{
              fontSize: '0.85rem',
              fontWeight: '800',
              fontFamily: 'var(--font-code)',
              color: '#34d399',
              background: 'rgba(16, 185, 129, 0.25)',
              padding: '2px 8px',
              borderRadius: '6px'
            }}>
              {spaceBadge}
            </span>
          </div>

          <div style={{ fontSize: '0.74rem', color: 'var(--text-primary)', lineHeight: '1.35' }}>
            <strong>Auxiliary Space: </strong>{complexity?.spaceExplanation || 'Map stores characters bounded by size of charset m or string length n.'}
          </div>
        </div>
      </div>
    </div>
  );
}
