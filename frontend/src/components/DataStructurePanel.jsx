import React from 'react';
import { Database } from 'lucide-react';

export default function DataStructurePanel({ currentStep, dsType }) {
  const dsElements = currentStep?.queueOrStackState || [];
  const variables = currentStep?.variables || {};
  const isQueue = dsType === 'Queue';

  return (
    <div className="glass-panel" style={{ width: '100%', height: '100%', padding: '16px', display: 'flex', flexDirection: 'column', gap: '12px', overflow: 'hidden' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', paddingBottom: '8px', borderBottom: '1px solid var(--border-color)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <Database size={18} color={isQueue ? 'var(--accent-amber)' : 'var(--accent-purple)'} />
          <span style={{ fontSize: '0.9rem', fontWeight: '700' }}>
            {isQueue ? 'Queue (FIFO State)' : 'Call Stack (LIFO State)'}
          </span>
        </div>
        <span style={{ fontSize: '0.72rem', padding: '2px 8px', borderRadius: '12px', background: isQueue ? 'rgba(245, 158, 11, 0.15)' : 'rgba(168, 85, 247, 0.15)', color: isQueue ? '#fbbf24' : '#c084fc', border: '1px solid var(--border-color)', fontWeight: '700' }}>
          Size: {dsElements.length}
        </span>
      </div>

      {/* Visual Data Structure Storage */}
      <div style={{ flex: 1, minHeight: '100px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '10px', padding: '10px', display: 'flex', flexDirection: isQueue ? 'row' : 'column-reverse', gap: '6px', alignItems: isQueue ? 'center' : 'stretch', overflowX: isQueue ? 'auto' : 'hidden', overflowY: isQueue ? 'hidden' : 'auto' }}>
        {dsElements.length === 0 ? (
          <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--text-muted)', fontSize: '0.82rem', fontStyle: 'italic' }}>
            Data structure is currently empty
          </div>
        ) : (
          dsElements.map((el, idx) => (
            <div
              key={idx}
              style={{
                padding: '6px 12px',
                borderRadius: '6px',
                background: idx === 0 && isQueue ? 'linear-gradient(135deg, var(--accent-amber), #d97706)' : idx === dsElements.length - 1 && !isQueue ? 'linear-gradient(135deg, var(--accent-purple), #7e22ce)' : 'rgba(255, 255, 255, 0.08)',
                color: '#ffffff',
                fontWeight: '700',
                fontSize: '0.82rem',
                border: '1px solid rgba(255, 255, 255, 0.15)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <span>{el}</span>
            </div>
          ))
        )}
      </div>

      {/* Variables Inspector */}
      <div style={{ display: 'flex', flexDirection: 'column', gap: '6px', borderTop: '1px solid var(--border-color)', paddingTop: '8px' }}>
        <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.5px' }}>
          Execution Variables
        </span>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', maxHeight: '75px', overflowY: 'auto' }}>
          {Object.keys(variables).length === 0 ? (
            <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontStyle: 'italic' }}>No active variables</span>
          ) : (
            Object.entries(variables).map(([key, val]) => (
              <div key={key} style={{ padding: '4px 8px', borderRadius: '6px', background: 'rgba(255, 255, 255, 0.04)', border: '1px solid var(--border-color)', fontSize: '0.75rem', fontFamily: 'var(--font-code)' }}>
                <span style={{ color: 'var(--accent-cyan)' }}>{key}: </span>
                <span style={{ color: '#f8fafc', fontWeight: '600' }}>{val}</span>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}
