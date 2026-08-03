import React from 'react';
import { ChevronRight } from 'lucide-react';

export default function Breadcrumb({ problem }) {
  const category = problem?.category || 'Arrays & math';
  const title = problem?.title || 'Longest substring without repeating characters';
  const difficulty = problem?.difficulty || 'Easy';

  const getDiffStyle = (diff) => {
    switch (diff?.toLowerCase()) {
      case 'easy':
        return { color: 'var(--diff-easy)', bg: 'var(--diff-easy-bg)', border: 'var(--diff-easy-border)' };
      case 'medium':
        return { color: 'var(--diff-medium)', bg: 'var(--diff-medium-bg)', border: 'var(--diff-medium-border)' };
      case 'hard':
        return { color: 'var(--diff-hard)', bg: 'var(--diff-hard-bg)', border: 'var(--diff-hard-border)' };
      default:
        return { color: 'var(--diff-easy)', bg: 'var(--diff-easy-bg)', border: 'var(--diff-easy-border)' };
    }
  };

  const diffStyle = getDiffStyle(difficulty);

  return (
    <div 
      style={{ 
        display: 'flex', 
        alignItems: 'center', 
        gap: '6px', 
        fontSize: '0.76rem', 
        padding: '2px var(--space-md)', 
        height: '24px', 
        color: 'var(--text-muted)',
        flexShrink: 0
      }}
    >
      <span style={{ color: 'var(--text-secondary)', fontWeight: '600' }}>
        {category}
      </span>
      
      <ChevronRight size={13} color="var(--text-muted)" />
      
      <span 
        style={{ 
          color: 'var(--text-primary)', 
          fontWeight: '700', 
          whiteSpace: 'nowrap', 
          overflow: 'hidden', 
          textOverflow: 'ellipsis',
          maxWidth: '380px'
        }}
        title={title}
      >
        {title}
      </span>

      <span 
        style={{ 
          fontSize: '0.64rem', 
          fontWeight: '700', 
          padding: '1px 7px', 
          borderRadius: 'var(--radius-full)', 
          color: diffStyle.color, 
          background: diffStyle.bg, 
          border: `1px solid ${diffStyle.border}`,
          marginLeft: '4px'
        }}
      >
        {difficulty}
      </span>
    </div>
  );
}
