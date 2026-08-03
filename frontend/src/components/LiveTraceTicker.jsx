import React from 'react';

export default function LiveTraceTicker({ stepDescription }) {
  return (
    <div 
      style={{ 
        width: '100%', 
        padding: '7px 14px', 
        borderRadius: 'var(--radius-sm)',
        background: 'rgba(15, 23, 42, 0.6)', 
        border: '1px solid var(--border-default)', 
        borderLeft: '3px solid var(--accent-violet)',
        display: 'flex', 
        alignItems: 'center', 
        gap: '8px', 
        fontSize: '0.74rem', 
        fontFamily: 'var(--font-code)',
        flexShrink: 0
      }}
    >
      <div className="pulse-dot" style={{ flexShrink: 0 }} />

      <span style={{ color: 'var(--text-primary)', fontWeight: '700', flexShrink: 0 }}>
        Live trace
      </span>

      <span style={{ color: 'var(--text-secondary)', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
        {stepDescription || 'Input string s = "abcabcbb". Initialize sliding window pointers left = 0, right = 0, maxLen = 0.'}
      </span>
    </div>
  );
}
