import React from 'react';
import { Activity } from 'lucide-react';

export default function LiveTraceTicker({ stepDescription }) {
  return (
    <footer 
      style={{ 
        width: '100%', 
        height: '32px', 
        minHeight: '32px', 
        background: 'rgba(15, 23, 42, 0.95)', 
        borderTop: '1px solid rgba(99, 102, 241, 0.3)', 
        display: 'flex', 
        alignItems: 'center', 
        padding: '0 var(--space-md)', 
        gap: 'var(--space-md)', 
        zIndex: 100, 
        flexShrink: 0,
        boxShadow: '0 -4px 12px rgba(0, 0, 0, 0.3)'
      }}
    >
      <div 
        style={{ 
          display: 'flex', 
          alignItems: 'center', 
          gap: '6px', 
          background: 'linear-gradient(135deg, var(--accent-indigo), var(--accent-purple))', 
          color: '#ffffff', 
          padding: '2px 8px', 
          borderRadius: '4px', 
          fontSize: '0.68rem', 
          fontWeight: '800', 
          letterSpacing: '0.6px', 
          textTransform: 'uppercase',
          flexShrink: 0
        }}
      >
        <Activity size={12} className="spin" color="#ffffff" />
        <span>LIVE TRACE</span>
      </div>

      <div 
        style={{ 
          flex: 1, 
          fontSize: '0.76rem', 
          fontFamily: 'var(--font-code)', 
          color: 'var(--text-primary)', 
          whiteSpace: 'nowrap', 
          overflow: 'hidden', 
          textOverflow: 'ellipsis',
          lineHeight: '1'
        }}
      >
        {stepDescription || 'Input String s = \'abcabcbb\'. Initialize Sliding Window pointers left = 0, right = 0, maxLen = 0.'}
      </div>
    </footer>
  );
}
