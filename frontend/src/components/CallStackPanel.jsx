import React, { useState } from 'react';
import { Layers, ChevronDown, ChevronRight, Variable, Database } from 'lucide-react';

export default function CallStackPanel({ currentStep, problem }) {
  const [isParamsOpen, setIsParamsOpen] = useState(true);
  const [isVarsOpen, setIsVarsOpen] = useState(true);
  const [isMapOpen, setIsMapOpen] = useState(true);

  const variables = currentStep?.variables || {};
  const problemTitle = problem?.title || 'lengthOfLongestSubstring';
  
  // Format function signature name (e.g., lengthOfLongestSubstring(String s))
  const getFnName = () => {
    if (problem?.id === 'longest-substring-without-repeating') {
      return 'lengthOfLongestSubstring(String s)';
    }
    const cleanTitle = problemTitle.replace(/[^a-zA-Z0-9]/g, '');
    const fnName = cleanTitle ? cleanTitle.charAt(0).toLowerCase() + cleanTitle.slice(1) : 'solve';
    return `${fnName}(...)`;
  };

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px', overflow: 'hidden' }}>
      {/* Panel Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-color)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Layers size={16} color="var(--accent-purple)" />
          <span style={{ fontSize: '0.86rem', fontWeight: '800', color: '#ffffff' }}>
            Call Stack Panel
          </span>
        </div>
        <span style={{ fontSize: '0.68rem', padding: '2px 8px', borderRadius: '12px', background: 'rgba(168, 85, 247, 0.15)', color: '#c084fc', border: '1px solid rgba(168, 85, 247, 0.3)', fontWeight: '700' }}>
          Frame: Active
        </span>
      </div>

      {/* Stack Frame Box */}
      <div style={{ flex: 1, background: 'rgba(0, 0, 0, 0.3)', borderRadius: '8px', border: '1px solid rgba(168, 85, 247, 0.25)', padding: '10px', display: 'flex', flexDirection: 'column', gap: '8px', overflowY: 'auto' }}>
        {/* Active Function Signature */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: '#a855f7', fontWeight: '700', fontSize: '0.82rem', fontFamily: 'var(--font-code)' }}>
          <ChevronRight size={14} color="#a855f7" />
          <span>{getFnName()}</span>
        </div>

        {/* Parameters Section */}
        <div style={{ paddingLeft: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <button 
            onClick={() => setIsParamsOpen(!isParamsOpen)}
            style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', color: 'var(--text-secondary)', fontSize: '0.74rem', fontWeight: '700', cursor: 'pointer', padding: 0 }}
          >
            {isParamsOpen ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
            <span>Parameters:</span>
          </button>

          {isParamsOpen && (
            <div style={{ paddingLeft: '14px', display: 'flex', flexDirection: 'column', gap: '4px', fontSize: '0.73rem', fontFamily: 'var(--font-code)' }}>
              {problem?.id === 'longest-substring-without-repeating' ? (
                <div style={{ color: 'var(--text-muted)' }}>s = "abcabcbb"</div>
              ) : (
                <div style={{ color: 'var(--text-muted)' }}>arr = [N elements]</div>
              )}
            </div>
          )}
        </div>

        {/* Variables Section */}
        <div style={{ paddingLeft: '12px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
          <button 
            onClick={() => setIsVarsOpen(!isVarsOpen)}
            style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', color: 'var(--text-secondary)', fontSize: '0.74rem', fontWeight: '700', cursor: 'pointer', padding: 0 }}
          >
            {isVarsOpen ? <ChevronDown size={13} /> : <ChevronRight size={13} />}
            <span>Variables</span>
          </button>

          {isVarsOpen && (
            <div style={{ paddingLeft: '14px', display: 'flex', flexDirection: 'column', gap: '5px', fontSize: '0.75rem', fontFamily: 'var(--font-code)' }}>
              {Object.keys(variables).length > 0 ? (
                Object.entries(variables).map(([key, val]) => {
                  if (key.toLowerCase().includes('map') || key.toLowerCase().includes('hash')) return null;
                  return (
                    <div key={key} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '2px 6px', background: 'rgba(255, 255, 255, 0.03)', borderRadius: '4px' }}>
                      <span style={{ color: '#94a3b8' }}>{key}:</span>
                      <span style={{ color: '#38bdf8', fontWeight: '700' }}>{val}</span>
                    </div>
                  );
                })
              ) : (
                <div style={{ color: 'var(--text-muted)' }}>maxLen: 0</div>
              )}

              {/* Simplified HashMap representation if present */}
              <div style={{ display: 'flex', flexDirection: 'column', gap: '2px', marginTop: '4px' }}>
                <button 
                  onClick={() => setIsMapOpen(!isMapOpen)}
                  style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'none', border: 'none', color: 'var(--accent-purple)', fontSize: '0.72rem', fontWeight: '700', cursor: 'pointer', padding: 0 }}
                >
                  {isMapOpen ? <ChevronDown size={12} /> : <ChevronRight size={12} />}
                  <span>HashMap: {variables.map || variables.HashMap || '{a: 0, b: 1, c: 2}'}</span>
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
