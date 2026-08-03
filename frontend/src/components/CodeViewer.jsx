import React from 'react';
import { Code2 } from 'lucide-react';

export default function CodeViewer({ problem, currentStep }) {
  const activeLine = currentStep?.activeLine || 4;
  const javaCode = problem?.javaCode || `// Java sliding window (LeetCode 3)
public int lengthOfLongestSubstring(String s) {
    HashMap<Character, Integer> map = new HashMap<>();
    int left = 0, right = 0, maxLen = 0;
    while (right < s.length()) {
        char ch = s.charAt(right);
        if (map.containsKey(ch)) {
            left = Math.max(map.get(ch) + 1, left);
        }
        map.put(ch, right);
        maxLen = Math.max(maxLen, right - left + 1);
        right++;
    }
    return maxLen;
}`;

  const lines = javaCode.split('\n');

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '12px', display: 'flex', flexDirection: 'column', gap: '8px', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-default)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Code2 size={15} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.82rem', fontWeight: '700', color: 'var(--text-primary)' }}>Java interview solution</span>
        </div>
        <span style={{ fontSize: '0.66rem', padding: '1px 6px', borderRadius: 'var(--radius-sm)', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', border: '1px solid var(--border-accent)', fontWeight: '700', fontFamily: 'var(--font-code)' }}>
          Active line: {activeLine}
        </span>
      </div>

      {/* Code Editor Body */}
      <div style={{
        flex: 1,
        background: '#090d16',
        borderRadius: 'var(--radius-sm)',
        border: '1px solid var(--border-default)',
        overflowY: 'auto',
        fontFamily: 'var(--font-code)',
        fontSize: '0.78rem',
        lineHeight: '1.5',
        padding: '6px 0'
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
                padding: '1px 12px',
                background: isHighlighted ? 'var(--accent-violet-tint)' : 'transparent',
                borderLeft: isHighlighted ? '3px solid var(--accent-violet)' : '3px solid transparent',
                color: isHighlighted ? 'var(--text-primary)' : lineText.trim().startsWith('//') ? 'var(--text-muted)' : '#cbd5e1',
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
