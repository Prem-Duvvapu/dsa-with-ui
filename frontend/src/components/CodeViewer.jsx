import React from 'react';
import { Code2, Terminal } from 'lucide-react';

export default function CodeViewer({ problem, currentStep }) {
  const activeLine = currentStep?.activeLine || 0;
  const lines = (problem?.javaCode || '').split('\n');

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-color)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Code2 size={16} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.86rem', fontWeight: '800', color: '#ffffff' }}>Java Interview Solution</span>
        </div>
        <span style={{ fontSize: '0.72rem', fontFamily: 'var(--font-code)', color: 'var(--text-muted)' }}>
          Active Line: <strong style={{ color: 'var(--accent-cyan)' }}>{activeLine || 'Idle'}</strong>
        </span>
      </div>

      {/* Code Editor Container */}
      <div style={{
        flex: 1,
        background: '#090d14',
        borderRadius: '8px',
        border: '1px solid rgba(255, 255, 255, 0.08)',
        overflowY: 'auto',
        fontFamily: 'var(--font-code)',
        fontSize: '0.8rem',
        lineHeight: '1.5',
        padding: '8px 0'
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
