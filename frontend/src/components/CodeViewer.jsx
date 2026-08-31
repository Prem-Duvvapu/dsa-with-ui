import React from 'react';
import { Code2 } from 'lucide-react';

export default function CodeViewer({ problem, currentStep }) {
  const javaCode = typeof problem?.javaCode === 'string' && problem.javaCode.trim()
    ? problem.javaCode
    : null;
  const activeLine = Number.isInteger(currentStep?.activeLine) && currentStep.activeLine > 0
    ? currentStep.activeLine
    : null;
  const lines = javaCode?.split('\n') || [];

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-default)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Code2 size={15} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.82rem', fontWeight: '700', color: 'var(--text-primary)' }}>Java interview solution</span>
        </div>
        {activeLine !== null && javaCode && (
          <span style={{ fontSize: '0.66rem', padding: '1px 6px', borderRadius: 'var(--radius-sm)', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', border: '1px solid var(--border-accent)', fontWeight: '700', fontFamily: 'var(--font-code)' }}>
            Active line: {activeLine}
          </span>
        )}
      </div>

      {/* Code Editor Body */}
      <div style={{
        flex: 1,
        background: 'var(--bg-page)',
        borderRadius: 'var(--radius-sm)',
        border: '1px solid var(--border-default)',
        overflowY: 'auto',
        fontFamily: 'var(--font-code)',
        fontSize: '0.78rem',
        lineHeight: '1.5',
        padding: '6px 0'
      }}>
        {!javaCode ? (
          <div
            role="status"
            style={{ height: '100%', minHeight: '96px', display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '16px', color: 'var(--text-muted)', textAlign: 'center', fontStyle: 'italic' }}
          >
            Code unavailable for this problem.
          </div>
        ) : lines.map((lineText, idx) => {
          const lineNumber = idx + 1;
          const isHighlighted = lineNumber === activeLine;

          return (
            <div
              key={lineNumber}
              style={{
                display: 'flex',
                alignItems: 'center',
                padding: '1px 12px',
                background: isHighlighted ? 'var(--accent-violet-tint)' : 'transparent',
                borderLeft: isHighlighted ? '3px solid var(--accent-violet)' : '3px solid transparent',
                color: isHighlighted ? 'var(--text-primary)' : lineText.trim().startsWith('//') ? 'var(--text-muted)' : 'var(--text-secondary)',
                fontWeight: isHighlighted ? '600' : '400',
                transition: 'all 0.15s ease'
              }}
            >
              <span style={{ width: '28px', minWidth: '28px', color: isHighlighted ? 'var(--accent-violet)' : 'var(--text-muted)', fontSize: '0.7rem', userSelect: 'none' }}>
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
