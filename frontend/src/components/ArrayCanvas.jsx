import React from 'react';
import { BarChart2 } from 'lucide-react';

export default function ArrayCanvas({ problem, currentStep }) {
  const arrayState = currentStep?.arrayState || problem?.defaultArray || [];

  const getElementColor = (state) => {
    switch (state) {
      case 'pivot':
        return { bg: 'linear-gradient(180deg, #6366f1, #4f46e5)', border: '#818cf8', glow: '0 0 18px rgba(99, 102, 241, 0.8)', label: 'mini / i' };
      case 'comparing':
      case 'active':
        return { bg: 'linear-gradient(180deg, #f59e0b, #d97706)', border: '#fbbf24', glow: '0 0 16px rgba(245, 158, 11, 0.7)', label: 'comparing (j)' };
      case 'swapping':
        return { bg: 'linear-gradient(180deg, #f97316, #ea580c)', border: '#fb923c', glow: 'var(--glow-orange)', label: 'swapping' };
      case 'sorted':
      case 'visited':
        return { bg: 'linear-gradient(180deg, #10b981, #059669)', border: '#34d399', glow: 'var(--glow-emerald)', label: 'sorted' };
      default:
        return { bg: 'linear-gradient(180deg, #334155, #1e293b)', border: '#475569', glow: 'none', label: 'unsorted' };
    }
  };

  const maxVal = Math.max(...arrayState.map(el => Math.abs(el.value)), 1);

  return (
    <div className="glass-panel" style={{ flex: 1, minHeight: '380px', padding: '20px', display: 'flex', flexDirection: 'column', position: 'relative' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
          <BarChart2 size={18} color="var(--accent-indigo)" />
          <span style={{ fontSize: '0.9rem', fontWeight: '700', letterSpacing: '0.4px' }}>
            Array & Bar Visualizer
          </span>
        </div>

        {/* Legend Badges */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '0.75rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#10b981' }}></span>
            <span style={{ color: '#34d399' }}>Sorted</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#6366f1' }}></span>
            <span style={{ color: '#818cf8' }}>Active / Minimum</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#f59e0b' }}></span>
            <span style={{ color: '#fbbf24' }}>Comparing</span>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
            <span style={{ width: '10px', height: '10px', borderRadius: '50%', background: '#f97316' }}></span>
            <span style={{ color: '#fb923c' }}>Swapping</span>
          </div>
        </div>
      </div>

      <div style={{ flex: 1, width: '100%', minHeight: '280px', display: 'flex', alignItems: 'flex-end', justifyContent: 'center', gap: '16px', padding: '20px', background: 'rgba(0, 0, 0, 0.25)', borderRadius: '12px', overflowX: 'auto' }}>
        {arrayState.map((el, idx) => {
          const colorInfo = getElementColor(el.state);
          const heightPercent = Math.max(25, Math.min(100, (Math.abs(el.value) / maxVal) * 100));

          return (
            <div key={idx} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              {/* Value text pill */}
              <span style={{ fontSize: '0.86rem', fontWeight: '800', color: colorInfo.border }}>
                {el.value}
              </span>

              {/* Animated Bar with Spring Overshoot Easing */}
              <div
                style={{
                  width: '44px',
                  height: `${heightPercent * 2.2}px`,
                  borderRadius: '8px 8px 4px 4px',
                  background: colorInfo.bg,
                  border: `2px solid ${colorInfo.border}`,
                  boxShadow: colorInfo.glow,
                  transition: 'all var(--motion-slow) var(--ease-out-back)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              />

              {/* Index label */}
              <span style={{ fontSize: '0.75rem', color: 'var(--text-muted)', fontWeight: '600' }}>
                [{el.index !== undefined ? el.index : idx}]
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
