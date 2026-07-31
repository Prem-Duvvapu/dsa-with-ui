import React from 'react';
import { Code2, Terminal } from 'lucide-react';

export default function CodeViewer({ problem, currentStep }) {
  const activeLine = currentStep?.activeLine || 0;
  const lines = (problem?.javaCode || '').split('\n');

  return (
    <div className="glass-panel" style={{ flex: 1, minHeight: '340px', padding: '20px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Code2 size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700' }}>Java Interview Solution</span>
        </div>
        <span style={{ fontSize: '0.75rem', fontFamily: 'var(--font-code)', color: 'var(--text-muted)' }}>
          Active Line: <strong style={{ color: 'var(--accent-cyan)' }}>{activeLine || 'Idle'}</strong>
        </span>
      </div>

      {/* Code Editor Container */}
      <div style={{
        flex: 1,
        background: '#090d14',
        borderRadius: '12px',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        overflowY: 'auto',
        maxHeight: '320px',
        fontFamily: 'var(--font-code)',
        fontSize: '0.83rem',
        lineHeight: '1.6',
        padding: '12px 0'
      }}>
        {lines.map((lineText, idx) => {
          const lineNumber = idx + 1;
          const isHighlighted = lineNumber === activeLine;

          return (
            <div
              key={lineNumber}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '2px 16px',
                background: isHighlighted ? 'rgba(99, 102, 241, 0.25)' : 'transparent',
                borderLeft: isHighlighted ? '3px solid var(--accent-indigo)' : '3px solid transparent',
                color: isHighlighted ? '#ffffff' : lineText.trim().startsWith('//') ? '#64748b' : '#cbd5e1',
                fontWeight: isHighlighted ? '600' : '400',
                transition: 'all 0.15s ease'
              }}
            >
              <span style={{ width: '36px', minWidth: '36px', color: isHighlighted ? 'var(--accent-indigo)' : '#475569', fontSize: '0.75rem', userSelect: 'none' }}>
                {lineNumber}
              </span>
              <span style={{ whiteSpace: 'pre' }}>{lineText}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
