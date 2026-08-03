import React, { useState } from 'react';
import { Database, Zap, Cpu, HardDrive } from 'lucide-react';

export default function MemoryComplexityCard({ currentStep, problem, initialTab }) {
  const [activeTab, setActiveTab] = useState(initialTab || 'memory'); // 'memory' | 'complexity'

  const variables = currentStep?.variables || {};
  const dsElements = currentStep?.queueOrStackState || [];
  const complexity = problem?.complexity;

  const timeBadge = complexity?.timeComplexity || 'O(N)';
  const spaceBadge = problem?.id === 'longest-substring-without-repeating' 
    ? 'O(min(m, n))' 
    : (complexity?.spaceComplexity || 'O(1)');

  return (
    <div 
      className="glass-panel" 
      style={{ 
        width: '100%', 
        height: '100%', 
        padding: '12px', 
        display: 'flex', 
        flexDirection: 'column', 
        gap: '10px', 
        overflow: 'hidden' 
      }}
    >
      {/* Tab Switcher Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-default)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'rgba(255, 255, 255, 0.03)', padding: '2px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-default)' }}>
          <button
            onClick={() => setActiveTab('memory')}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '0.74rem',
              fontWeight: activeTab === 'memory' ? '700' : '500',
              border: 'none',
              background: activeTab === 'memory' ? 'var(--accent-violet)' : 'transparent',
              color: activeTab === 'memory' ? '#ffffff' : 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              transition: 'all 0.15s ease'
            }}
          >
            <Database size={13} />
            <span>Memory</span>
          </button>

          <button
            onClick={() => setActiveTab('complexity')}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '0.74rem',
              fontWeight: activeTab === 'complexity' ? '700' : '500',
              border: 'none',
              background: activeTab === 'complexity' ? 'var(--accent-violet)' : 'transparent',
              color: activeTab === 'complexity' ? '#ffffff' : 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              gap: '4px',
              transition: 'all 0.15s ease'
            }}
          >
            <Zap size={13} />
            <span>Complexity</span>
          </button>
        </div>

        <span style={{ fontSize: '0.66rem', color: 'var(--text-muted)', fontFamily: 'var(--font-code)' }}>
          {activeTab === 'memory' ? `Vars: ${Object.keys(variables).length}` : timeBadge}
        </span>
      </div>

      {/* Internal Scrollable Content Area (Never clips overall layout) */}
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {activeTab === 'memory' ? (
          /* Memory Inspector Tab Content */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
            {Object.keys(variables).length > 0 ? (
              Object.entries(variables).map(([key, val]) => (
                <div 
                  key={key} 
                  style={{ 
                    display: 'flex', 
                    alignItems: 'center', 
                    justifyContent: 'space-between', 
                    padding: '6px 10px', 
                    borderRadius: 'var(--radius-sm)', 
                    background: 'rgba(255, 255, 255, 0.03)', 
                    border: '1px solid var(--border-default)', 
                    fontSize: '0.74rem', 
                    fontFamily: 'var(--font-code)' 
                  }}
                >
                  <span style={{ color: 'var(--text-secondary)' }}>{key}</span>
                  <span style={{ color: 'var(--text-primary)', fontWeight: '700', background: 'var(--accent-violet-tint)', padding: '1px 6px', borderRadius: '4px', border: '1px solid var(--border-accent)' }}>
                    {val}
                  </span>
                </div>
              ))
            ) : (
              <div style={{ padding: '16px', textOverflow: 'ellipsis', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.74rem', fontStyle: 'italic' }}>
                No active variable allocations
              </div>
            )}
          </div>
        ) : (
          /* Complexity Proof Tab Content */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {/* Time Complexity */}
            <div style={{ background: 'var(--accent-violet-tint)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-accent)', padding: '8px 10px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px', color: 'var(--accent-violet)' }}>
                  <Cpu size={13} />
                  <span style={{ fontWeight: '700', fontSize: '0.76rem' }}>Time Complexity</span>
                </div>
                <span style={{ fontSize: '0.8rem', fontWeight: '800', fontFamily: 'var(--font-code)', color: 'var(--accent-violet)' }}>
                  {timeBadge}
                </span>
              </div>
              <p style={{ fontSize: '0.72rem', color: 'var(--text-primary)', lineHeight: '1.35' }}>
                <strong>Proof: </strong>{complexity?.timeExplanation || 'Single pass iteration through string/array with sliding window pointers.'}
              </p>
            </div>

            {/* Space Complexity */}
            <div style={{ background: 'rgba(20, 184, 166, 0.08)', borderRadius: 'var(--radius-sm)', border: '1px solid rgba(20, 184, 166, 0.3)', padding: '8px 10px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '5px', color: 'var(--state-done)' }}>
                  <HardDrive size={13} />
                  <span style={{ fontWeight: '700', fontSize: '0.76rem' }}>Space Complexity</span>
                </div>
                <span style={{ fontSize: '0.8rem', fontWeight: '800', fontFamily: 'var(--font-code)', color: 'var(--state-done)' }}>
                  {spaceBadge}
                </span>
              </div>
              <p style={{ fontSize: '0.72rem', color: 'var(--text-primary)', lineHeight: '1.35' }}>
                <strong>Auxiliary Space: </strong>{complexity?.spaceExplanation || 'Map stores unique characters bounded by charset m or length n.'}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
