import React from 'react';
import { BarChart2 } from 'lucide-react';

export default function ArrayCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const rawArray = (activeStep?.arrayState && activeStep.arrayState.length > 0) 
    ? activeStep.arrayState 
    : (problem?.defaultArray && problem.defaultArray.length > 0) 
      ? problem.defaultArray 
      : [{ value: 2, state: 'default' }, { value: 7, state: 'comparing' }, { value: 11, state: 'active' }, { value: 15, state: 'sorted' }];

  const normalizedArray = rawArray.map((el, idx) => {
    if (typeof el === 'object' && el !== null) {
      const value = el.value !== undefined ? el.value : (el.val !== undefined ? el.val : idx);
      const state = el.state || 'default';
      const index = el.index !== undefined ? el.index : idx;
      return { value, state, index };
    }
    return { value: Number(el) || 0, state: 'default', index: idx };
  });

  const getElementColor = (state) => {
    switch (state) {
      case 'pivot':
      case 'max':
        return { bg: 'linear-gradient(180deg, #a855f7, #7e22ce)', border: '#c084fc', glow: '0 0 16px rgba(168,85,247,0.7)' };
      case 'comparing':
      case 'active':
        return { bg: 'linear-gradient(180deg, var(--state-comparing), #d97706)', border: '#fbbf24', glow: 'var(--glow-amber)' };
      case 'swapping':
        return { bg: 'linear-gradient(180deg, var(--state-swapping), #dc2626)', border: '#f87171', glow: 'var(--glow-rose)' };
      case 'sorted':
        return { bg: 'linear-gradient(180deg, var(--state-sorted), #059669)', border: '#34d399', glow: 'var(--glow-emerald)' };
      case 'visited':
        return { bg: 'linear-gradient(180deg, #475569, #334155)', border: '#64748b', glow: 'none' };
      default:
        return { bg: 'linear-gradient(180deg, #334155, #1e293b)', border: 'var(--border-color)', glow: 'none' };
    }
  };

  const values = normalizedArray.map(el => Math.abs(el.value));
  const maxVal = Math.max(...values, 1);

  return (
    <div style={{ flex: 1, padding: 'var(--space-md) var(--space-xl)', display: 'flex', flexDirection: 'column', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* Visualizer Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 'var(--space-sm)', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-sm)' }}>
          <BarChart2 size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: 'var(--text-base)', fontWeight: '800', letterSpacing: '0.4px', color: 'var(--text-primary)' }}>
            Array & Bar Visualizer
          </span>
          <span style={{ fontSize: 'var(--text-xs)', padding: '2px 8px', background: 'rgba(99,102,241,0.15)', color: 'var(--accent-sky)', borderRadius: 'var(--radius-full)', border: '1px solid rgba(99,102,241,0.3)', fontWeight: '700' }}>
            Size: {normalizedArray.length} Elements
          </span>
        </div>

        {/* Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-md)', fontSize: 'var(--text-xs)' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#a855f7' }}></span>
            <span style={{ color: '#c084fc' }}>Max Element</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--state-comparing)' }}></span>
            <span style={{ color: '#fbbf24' }}>Current (i)</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#64748b' }}></span>
            <span style={{ color: 'var(--text-muted)' }}>Visited</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: 'var(--state-sorted)' }}></span>
            <span style={{ color: '#34d399' }}>Sorted / Done</span>
          </div>
        </div>
      </div>

      {/* Array Bars Ground Floor Baseline Stage */}
      <div style={{ flex: 1, width: '100%', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: 'var(--space-xl)', padding: 'var(--space-lg)', background: 'rgba(0, 0, 0, 0.25)', borderRadius: 'var(--radius-md)', borderBottom: '2px solid var(--border-color)', overflowX: 'auto', overflowY: 'hidden' }}>
        {normalizedArray.map((el, idx) => {
          const colorInfo = getElementColor(el.state);
          const ratio = Math.abs(el.value) / maxVal;
          const barPx = Math.max(18, Math.min(100, Math.round(ratio * 90)));

          return (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', gap: 'var(--space-xs)' }}>
              {/* Value pill on top of bar */}
              <span style={{ fontSize: 'var(--text-sm)', fontWeight: '800', color: colorInfo.border, lineHeight: '1' }}>
                {el.value}
              </span>

              {/* Bar sitting on baseline floor */}
              <div
                style={{
                  width: '42px',
                  height: `${barPx}px`,
                  borderRadius: 'var(--radius-xs) var(--radius-xs) 2px 2px',
                  background: colorInfo.bg,
                  border: `2px solid ${colorInfo.border}`,
                  boxShadow: colorInfo.glow,
                  transition: 'all var(--motion-normal) var(--ease-standard)',
                  flexShrink: 0
                }}
              />

              {/* Index label below bar */}
              <span style={{ fontSize: 'var(--text-xs)', color: 'var(--text-muted)', fontWeight: '600', lineHeight: '1' }}>
                [{el.index}]
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
