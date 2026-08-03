import React from 'react';
import { Database, Cpu, Layers } from 'lucide-react';

export default function DataStructurePanel({ currentStep, dsType }) {
  const dsElements = currentStep?.queueOrStackState || [];
  const variables = currentStep?.variables || {};
  const isQueue = dsType === 'Queue';
  const isStackOrTree = dsElements.length > 0 || dsType === 'Stack' || dsType === 'RecursionTree';

  const panelTitle = isQueue 
    ? 'Queue (FIFO State)' 
    : isStackOrTree && dsElements.length > 0 
      ? 'Call Stack (LIFO State)' 
      : 'HashMap & Memory Inspector';

  const iconColor = isQueue ? 'var(--accent-amber)' : isStackOrTree ? 'var(--accent-purple)' : 'var(--accent-cyan)';

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '14px', display: 'flex', flexDirection: 'column', gap: '10px', overflow: 'hidden' }}>
      {/* Panel Header */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '6px', borderBottom: '1px solid var(--border-color)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Database size={16} color={iconColor} />
          <span style={{ fontSize: '0.86rem', fontWeight: '800' }}>
            {panelTitle}
          </span>
        </div>
        <span style={{ fontSize: '0.7rem', padding: '2px 8px', borderRadius: '12px', background: 'rgba(99, 102, 241, 0.15)', color: '#818cf8', border: '1px solid var(--border-color)', fontWeight: '700' }}>
          {dsElements.length > 0 ? `Size: ${dsElements.length}` : `Vars: ${Object.keys(variables).length}`}
        </span>
      </div>

      {/* Primary Memory / Storage Visualizer Container */}
      <div style={{ flex: 1, background: 'rgba(0, 0, 0, 0.25)', borderRadius: '8px', padding: '8px', display: 'flex', flexDirection: isQueue ? 'row' : 'column-reverse', gap: '6px', alignItems: isQueue ? 'center' : 'stretch', overflowX: isQueue ? 'auto' : 'hidden', overflowY: isQueue ? 'hidden' : 'auto' }}>
        {dsElements.length > 0 ? (
          dsElements.map((el, idx) => (
            <div
              key={idx}
              style={{
                padding: '5px 10px',
                borderRadius: '6px',
                background: idx === 0 && isQueue ? 'linear-gradient(135deg, var(--accent-amber), #d97706)' : idx === dsElements.length - 1 && !isQueue ? 'linear-gradient(135deg, var(--accent-purple), #7e22ce)' : 'rgba(255, 255, 255, 0.08)',
                color: '#ffffff',
                fontWeight: '700',
                fontSize: '0.8rem',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <span>{el}</span>
            </div>
          ))
        ) : Object.keys(variables).length > 0 ? (
          <div style={{ width: '100%', height: '100%', display: 'flex', flexDirection: 'column', gap: '6px', overflowY: 'auto' }}>
            {Object.entries(variables).map(([key, val]) => (
              <div key={key} style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '5px 8px', borderRadius: '6px', background: 'rgba(255, 255, 255, 0.05)', border: '1px solid rgba(255, 255, 255, 0.08)', fontSize: '0.76rem', fontFamily: 'var(--font-code)' }}>
                <span style={{ color: 'var(--accent-cyan)', fontWeight: '700' }}>{key}:</span>
                <span style={{ color: '#f8fafc', fontWeight: '800', background: 'rgba(99, 102, 241, 0.2)', padding: '1px 6px', borderRadius: '4px' }}>{val}</span>
              </div>
            ))}
          </div>
        ) : (
          <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.78rem', fontStyle: 'italic' }}>
            No active memory allocation
          </div>
        )}
      </div>

      {/* Auxiliary Variables Footer */}
      {dsElements.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '4px', borderTop: '1px solid var(--border-color)', paddingTop: '6px', flexShrink: 0 }}>
          <span style={{ fontSize: '0.68rem', fontWeight: '800', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
            Execution Variables
          </span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '4px', maxHeight: '55px', overflowY: 'auto' }}>
            {Object.entries(variables).map(([key, val]) => (
              <div key={key} style={{ padding: '3px 6px', borderRadius: '4px', background: 'rgba(255, 255, 255, 0.04)', border: '1px solid var(--border-color)', fontSize: '0.72rem', fontFamily: 'var(--font-code)' }}>
                <span style={{ color: 'var(--accent-cyan)' }}>{key}: </span>
                <span style={{ color: '#f8fafc', fontWeight: '600' }}>{val}</span>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}
