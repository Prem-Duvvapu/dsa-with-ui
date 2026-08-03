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
      case 'target':
        return { bg: 'linear-gradient(180deg, var(--state-target), #5b46e0)', border: 'var(--state-target)', glow: 'var(--state-target-glow)' };
      case 'comparing':
      case 'active':
      case 'current':
        return { bg: 'linear-gradient(180deg, var(--state-current), #d97706)', border: 'var(--state-current)', glow: 'var(--state-current-glow)' };
      case 'swapping':
        return { bg: 'linear-gradient(180deg, #f43f5e, #dc2626)', border: '#f43f5e', glow: '0 0 14px rgba(244, 63, 94, 0.5)' };
      case 'sorted':
      case 'done':
        return { bg: 'linear-gradient(180deg, var(--state-done), #0d9488)', border: 'var(--state-done)', glow: 'var(--state-done-glow)' };
      case 'visited':
      case 'eliminated':
      default:
        return { bg: 'linear-gradient(180deg, #334155, #1e293b)', border: 'var(--border-default)', glow: 'none' };
    }
  };

  const values = normalizedArray.map(el => Math.abs(el.value));
  const maxVal = Math.max(...values, 1);

  return (
    <div style={{ flex: 1, padding: '12px 16px', display: 'flex', flexDirection: 'column', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* Visualizer Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '8px', flexShrink: 0 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChart2 size={16} color="var(--accent-violet)" />
          <span style={{ fontSize: '0.86rem', fontWeight: '800', letterSpacing: '0.3px', color: 'var(--text-primary)' }}>
            Array & bar visualizer
          </span>
          <span style={{ fontSize: '0.66rem', padding: '2px 7px', background: 'var(--accent-violet-tint)', color: 'var(--accent-violet)', borderRadius: 'var(--radius-full)', border: '1px solid var(--border-accent)', fontWeight: '700' }}>
            Size: {normalizedArray.length} elements
          </span>
        </div>

        {/* 4 Semantic State Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '14px', fontSize: '0.72rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-current)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Current</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-target)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Target</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-visited)' }}></span>
            <span style={{ color: 'var(--text-muted)' }}>Visited</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--state-done)' }}></span>
            <span style={{ color: 'var(--text-secondary)' }}>Done</span>
          </div>
        </div>
      </div>

      {/* Array Stage with Faint Horizontal Gridlines */}
      <div 
        style={{ 
          flex: 1, 
          width: '100%', 
          display: 'flex', 
          alignItems: 'flex-end', 
          justifyContent: 'center', 
          gap: '24px', 
          padding: '20px', 
          background: 'radial-gradient(ellipse at center, rgba(15, 23, 42, 0.6), rgba(9, 13, 22, 0.9)), repeating-linear-gradient(0deg, transparent, transparent 35px, rgba(255, 255, 255, 0.035) 35px, rgba(255, 255, 255, 0.035) 36px)', 
          borderRadius: 'var(--radius-md)', 
          border: '1px solid var(--border-default)', 
          borderBottom: '2px solid var(--border-strong)', 
          overflowX: 'auto', 
          overflowY: 'hidden' 
        }}
      >
        {normalizedArray.map((el, idx) => {
          const colorInfo = getElementColor(el.state);
          const ratio = Math.abs(el.value) / maxVal;
          const barHeightPercent = Math.max(15, Math.min(85, Math.round(ratio * 75)));

          return (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'flex-end', gap: '6px', height: '100%' }}>
              {/* Value label on top of bar */}
              <span style={{ fontSize: '0.78rem', fontWeight: '800', color: colorInfo.border, lineHeight: '1' }}>
                {el.value}
              </span>

              {/* Bar proportional height */}
              <div
                style={{
                  width: '38px',
                  height: `${barHeightPercent}%`,
                  minHeight: '24px',
                  borderRadius: 'var(--radius-sm) var(--radius-sm) 2px 2px',
                  background: colorInfo.bg,
                  border: `1.5px solid ${colorInfo.border}`,
                  boxShadow: colorInfo.glow,
                  transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)'
                }}
              />

              {/* Index label underneath bar */}
              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontFamily: 'var(--font-code)', fontWeight: '600' }}>
                [{idx}]
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
