import React, { useEffect, useId, useState } from 'react';
import { Database, Zap, Cpu, HardDrive } from 'lucide-react';

const SECTION_HEADING_STYLE = {
  margin: 0,
  color: 'var(--text-muted)',
  fontFamily: 'var(--font-code)',
  fontSize: '0.66rem',
  fontWeight: 700,
  letterSpacing: '0.06em',
  textTransform: 'uppercase'
};

const MEMORY_ROW_STYLE = {
  display: 'flex',
  alignItems: 'center',
  justifyContent: 'space-between',
  gap: '8px',
  padding: '6px 10px',
  borderRadius: 'var(--radius-sm)',
  background: 'var(--bench-fill)',
  border: '1px solid var(--border-default)',
  fontSize: '0.74rem',
  fontFamily: 'var(--font-code)'
};

function displayValue(value) {
  if (typeof value === 'string') return value;
  if (value === null) return 'null';
  if (value === undefined) return 'undefined';
  if (typeof value !== 'object') return String(value);

  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function SectionHeading({ children }) {
  return <h3 style={SECTION_HEADING_STYLE}>{children}</h3>;
}

function MemorySequence({ title, values, markerAt }) {
  return (
    <section aria-label={title} style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
      <SectionHeading>{title}</SectionHeading>
      <ol style={{ display: 'flex', flexDirection: 'column', gap: '4px', margin: 0, padding: 0, listStyle: 'none' }}>
        {values.map((value, index) => {
          const text = displayValue(value);
          const marker = markerAt(index, values.length);
          return (
            <li key={`${index}-${text}`} style={MEMORY_ROW_STYLE}>
              <span
                aria-label={marker ? `${marker}: ${text}` : undefined}
                style={{ color: 'var(--text-primary)', minWidth: 0, overflowWrap: 'anywhere' }}
              >
                {text}
              </span>
              {marker && (
                <span aria-hidden="true" style={{ color: 'var(--accent-violet)', fontSize: '0.66rem', fontWeight: 700, textTransform: 'uppercase' }}>
                  {marker}
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
}

export default function MemoryComplexityCard({ currentStep, problem, initialTab }) {
  const [activeTab, setActiveTab] = useState(initialTab || 'memory'); // 'memory' | 'complexity'
  const tabGroupId = useId();
  const memoryTabId = `${tabGroupId}-memory-tab`;
  const complexityTabId = `${tabGroupId}-complexity-tab`;
  const panelId = `${tabGroupId}-panel`;

  // On mobile, App's own tab bar drives `initialTab`; the card must follow it on every
  // change, not just its first mount. Desktop never passes this prop, so the card keeps
  // its own internal toggle there — this effect simply never fires.
  useEffect(() => {
    if (initialTab === 'memory' || initialTab === 'complexity') {
      setActiveTab(initialTab);
    }
  }, [initialTab]);

  const handleTabKeyDown = (event) => {
    let nextTab;
    if (event.key === 'ArrowLeft' || event.key === 'Home') nextTab = 'memory';
    if (event.key === 'ArrowRight' || event.key === 'End') nextTab = 'complexity';
    if (!nextTab) return;

    event.preventDefault();
    setActiveTab(nextTab);
    document.getElementById(nextTab === 'memory' ? memoryTabId : complexityTabId)?.focus();
  };

  const variables = currentStep?.variables && typeof currentStep.variables === 'object'
    && !Array.isArray(currentStep.variables) ? currentStep.variables : {};
  const callStack = Array.isArray(currentStep?.callStack) ? currentStep.callStack : [];
  const dsElements = Array.isArray(currentStep?.queueOrStackState)
    ? currentStep.queueOrStackState : [];
  const variableEntries = Object.entries(variables);
  const dsType = currentStep?.dsType || problem?.dsType;
  const dataStructureTitle = {
    Queue: 'Queue contents',
    Stack: 'Stack contents',
    PriorityQueue: 'Priority queue contents'
  }[dsType] || 'Data structure contents';
  const memoryItemCount = variableEntries.length + callStack.length + dsElements.length;
  const complexity = problem?.complexity;

  const timeBadge = complexity?.timeComplexity || '—';
  const spaceBadge = complexity?.spaceComplexity || '—';
  const timeExplanation = complexity?.timeExplanation || 'Time explanation unavailable.';
  const spaceExplanation = complexity?.spaceExplanation || 'Space explanation unavailable.';

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
        <div
          role="tablist"
          aria-label="Memory and complexity"
          style={{ display: 'flex', alignItems: 'center', gap: '4px', background: 'var(--bench-fill)', padding: '2px', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-default)' }}
        >
          <button
            type="button"
            id={memoryTabId}
            role="tab"
            aria-selected={activeTab === 'memory'}
            aria-controls={panelId}
            tabIndex={activeTab === 'memory' ? 0 : -1}
            onClick={() => setActiveTab('memory')}
            onKeyDown={handleTabKeyDown}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '0.74rem',
              fontWeight: activeTab === 'memory' ? '700' : '500',
              border: 'none',
              background: activeTab === 'memory' ? 'var(--accent-violet)' : 'transparent',
              color: activeTab === 'memory' ? 'var(--text-on-accent)' : 'var(--text-muted)',
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
            type="button"
            id={complexityTabId}
            role="tab"
            aria-selected={activeTab === 'complexity'}
            aria-controls={panelId}
            tabIndex={activeTab === 'complexity' ? 0 : -1}
            onClick={() => setActiveTab('complexity')}
            onKeyDown={handleTabKeyDown}
            style={{
              padding: '3px 10px',
              borderRadius: '4px',
              fontSize: '0.74rem',
              fontWeight: activeTab === 'complexity' ? '700' : '500',
              border: 'none',
              background: activeTab === 'complexity' ? 'var(--accent-violet)' : 'transparent',
              color: activeTab === 'complexity' ? 'var(--text-on-accent)' : 'var(--text-muted)',
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
          {activeTab === 'memory' ? `${memoryItemCount} items` : timeBadge}
        </span>
      </div>

      {/* Internal Scrollable Content Area (Never clips overall layout) */}
      <div
        id={panelId}
        role="tabpanel"
        aria-labelledby={activeTab === 'memory' ? memoryTabId : complexityTabId}
        style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column', gap: '8px' }}
      >
        {activeTab === 'memory' ? (
          /* Memory Inspector Tab Content */
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {variableEntries.length > 0 && (
              <section aria-label="Variables" style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
                <SectionHeading>Variables</SectionHeading>
                {variableEntries.map(([key, val]) => (
                  <div
                    key={key}
                    style={MEMORY_ROW_STYLE}
                  >
                    <span style={{ color: 'var(--text-secondary)' }}>{key}</span>
                    <span style={{ color: 'var(--text-primary)', fontWeight: '700', background: 'var(--accent-violet-tint)', padding: '1px 6px', borderRadius: '4px', border: '1px solid var(--border-accent)' }}>
                      {displayValue(val)}
                    </span>
                  </div>
                ))}
              </section>
            )}

            {callStack.length > 0 && (
              <MemorySequence
                title="Call stack"
                values={callStack}
                markerAt={(index, length) => index === length - 1 ? 'Current frame' : null}
              />
            )}

            {dsElements.length > 0 && (
              <MemorySequence
                title={dataStructureTitle}
                values={dsElements}
                markerAt={(index, length) => {
                  if (dsType === 'Queue') {
                    if (length === 1) return 'Front / back';
                    if (index === 0) return 'Front';
                    if (index === length - 1) return 'Back';
                    return null;
                  }
                  if (dsType === 'Stack') return index === length - 1 ? 'Top' : null;
                  return null;
                }}
              />
            )}

            {memoryItemCount === 0 && (
              <div style={{ padding: '16px', textOverflow: 'ellipsis', textAlign: 'center', color: 'var(--text-muted)', fontSize: '0.74rem', fontStyle: 'italic' }}>
                No active memory state
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
                <strong>Proof: </strong>{timeExplanation}
              </p>
            </div>

            {/* Space Complexity */}
            <div style={{ background: 'var(--bench-fill)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--bench-rule-strong)', padding: '8px 10px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
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
                <strong>Auxiliary Space: </strong>{spaceExplanation}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
