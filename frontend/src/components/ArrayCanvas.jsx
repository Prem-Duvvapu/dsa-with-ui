import React from 'react';
import { BarChart2 } from 'lucide-react';

export default function ArrayCanvas({ problem, currentStep }) {
  const rawArray = (currentStep?.arrayState && currentStep.arrayState.length > 0) 
    ? currentStep.arrayState 
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
        return { bg: 'linear-gradient(180deg, #6366f1, #4f46e5)', border: '#818cf8', glow: '0 0 16px rgba(99, 102, 241, 0.8)' };
      case 'comparing':
      case 'active':
        return { bg: 'linear-gradient(180deg, #f59e0b, #d97706)', border: '#fbbf24', glow: '0 0 16px rgba(245, 158, 11, 0.7)' };
      case 'swapping':
        return { bg: 'linear-gradient(180deg, #f97316, #ea580c)', border: '#fb923c', glow: 'var(--glow-orange)' };
      case 'sorted':
      case 'visited':
        return { bg: 'linear-gradient(180deg, #10b981, #059669)', border: '#34d399', glow: 'var(--glow-emerald)' };
      default:
        return { bg: 'linear-gradient(180deg, #334155, #1e293b)', border: '#475569', glow: 'none' };
    }
  };

  const values = normalizedArray.map(el => Math.abs(el.value));
  const maxVal = Math.max(...values, 1);

  return (
    <div style={{ flex: 1, padding: '16px', display: 'flex', flexDirection: 'column', position: 'relative', width: '100%', height: '100%', overflow: 'hidden' }}>
      {/* Header Bar */}
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '12px', flexWrap: 'wrap', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChart2 size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.88rem', fontWeight: '800', letterSpacing: '0.4px' }}>
            Array & Bar Visualizer
          </span>
          <span style={{ fontSize: '0.7rem', padding: '2px 8px', background: 'rgba(99,102,241,0.15)', color: '#818cf8', borderRadius: '12px', border: '1px solid rgba(99,102,241,0.3)', fontWeight: '700' }}>
            Size: {normalizedArray.length} Elements
          </span>
        </div>

        {/* Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', fontSize: '0.73rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Sorted</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#6366f1' }}></span>
            <span style={{ color: '#818cf8' }}>Active / Target</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#f59e0b' }}></span>
            <span style={{ color: '#fbbf24' }}>Comparing</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '4px' }}>
            <span style={{ width: '8px', height: '8px', borderRadius: '50%', background: '#f97316' }}></span>
            <span style={{ color: '#fb923c' }}>Swapping</span>
          </div>
        </div>
      </div>

      {/* Array Bar Stage */}
      <div style={{ flex: 1, width: '100%', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: '16px', padding: '16px 20px 24px 20px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflowX: 'auto' }}>
        {normalizedArray.map((el, idx) => {
          const colorInfo = getElementColor(el.state);
          const ratio = Math.abs(el.value) / maxVal;
          const barPx = Math.max(24, Math.round(ratio * 120));

          return (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
              {/* Value pill */}
              <span style={{ fontSize: '0.84rem', fontWeight: '800', color: colorInfo.border }}>
                {el.value}
              </span>

              {/* Bar */}
              <div
                style={{
                  width: '42px',
                  height: `${barPx}px`,
                  borderRadius: '8px 8px 4px 4px',
                  background: colorInfo.bg,
                  border: `2px solid ${colorInfo.border}`,
                  boxShadow: colorInfo.glow,
                  transition: 'all 0.3s ease',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              />

              {/* Index label */}
              <span style={{ fontSize: '0.72rem', color: 'var(--text-muted)', fontWeight: '600' }}>
                [{el.index}]
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
