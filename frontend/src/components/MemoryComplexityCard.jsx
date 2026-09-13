import React, { useEffect, useId, useState } from 'react';
import { Database, Zap, Cpu, HardDrive } from 'lucide-react';
import styles from './MemoryComplexityCard.module.css';

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
  return <h3 className={styles.sectionHeading}>{children}</h3>;
}

function MemorySequence({ title, values, markerAt }) {
  return (
    <section aria-label={title} className={styles.sequenceContainer}>
      <SectionHeading>{title}</SectionHeading>
      <ol className={styles.sequenceList}>
        {values.map((value, index) => {
          const text = displayValue(value);
          const marker = markerAt(index, values.length);
          return (
            <li key={`${index}-${text}`} className={styles.memoryRow}>
              <span
                aria-label={marker ? `${marker}: ${text}` : undefined}
                className={styles.memoryText}
              >
                {text}
              </span>
              {marker && (
                <span aria-hidden="true" className={styles.marker}>
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
    <div className={`glass-panel ${styles.card}`}>
      {/* Tab Switcher Header */}
      <div className={styles.header}>
        <div
          role="tablist"
          aria-label="Memory and complexity"
          className={styles.tablist}
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
              // Selected reads as a raised cell, not as a hue: Bench spends its two
              // colours on algorithm state and leaves the chrome neutral. Ink-on-ink was
              // the accidental result of dropping the violet - white on --bench-ink-secondary
              // is under 3:1 in the dark theme.
              background: activeTab === 'memory' ? 'var(--bench-fill)' : 'transparent',
              border: activeTab === 'memory' ? '1px solid var(--bench-rule-strong)' : '1px solid transparent',
              color: activeTab === 'memory' ? 'var(--bench-ink)' : 'var(--text-muted)',
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
              // Selected reads as a raised cell, not as a hue: Bench spends its two
              // colours on algorithm state and leaves the chrome neutral. Ink-on-ink was
              // the accidental result of dropping the violet - white on --bench-ink-secondary
              // is under 3:1 in the dark theme.
              background: activeTab === 'complexity' ? 'var(--bench-fill)' : 'transparent',
              border: activeTab === 'complexity' ? '1px solid var(--bench-rule-strong)' : '1px solid transparent',
              color: activeTab === 'complexity' ? 'var(--bench-ink)' : 'var(--text-muted)',
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

        <span className={styles.headerMeta}>
          {activeTab === 'memory' ? `${memoryItemCount} items` : timeBadge}
        </span>
      </div>

      {/* Internal Scrollable Content Area (Never clips overall layout) */}
      <div
        id={panelId}
        role="tabpanel"
        aria-labelledby={activeTab === 'memory' ? memoryTabId : complexityTabId}
        className={styles.panel}
      >
        {activeTab === 'memory' ? (
          /* Memory Inspector Tab Content */
          <div className={styles.stack}>
            {variableEntries.length > 0 && (
              <section aria-label="Variables" className={styles.section}>
                <SectionHeading>Variables</SectionHeading>
                {variableEntries.map(([key, val]) => (
                  <div
                    key={key}
                    className={styles.memoryRow}
                  >
                    <span className={styles.rowKey}>{key}</span>
                    <span className={styles.rowValue}>
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
              <div className={styles.emptyNote}>
                No active memory state
              </div>
            )}
          </div>
        ) : (
          /* Complexity Proof Tab Content */
          <div className={styles.stack}>
            {/* Time Complexity */}
            <div className={styles.complexityCard}>
              <div className={styles.complexityHead}>
                <div className={styles.complexityLabel}>
                  <Cpu size={13} />
                  <span className={styles.complexityLabelText}>Time Complexity</span>
                </div>
                <span className={styles.complexityValue}>
                  {timeBadge}
                </span>
              </div>
              <p className={styles.complexityText}>
                <strong>Proof: </strong>{timeExplanation}
              </p>
            </div>

            {/* Space Complexity */}
            <div className={styles.complexityCard}>
              <div className={styles.complexityHead}>
                <div className={styles.complexityLabel}>
                  <HardDrive size={13} />
                  <span className={styles.complexityLabelText}>Space Complexity</span>
                </div>
                <span className={styles.complexityValue}>
                  {spaceBadge}
                </span>
              </div>
              <p className={styles.complexityText}>
                <strong>Auxiliary Space: </strong>{spaceExplanation}
              </p>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
