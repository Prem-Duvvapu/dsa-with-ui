import React from 'react';

/**
 * Disjoint Set Union (DSU) visualizer.
 *
 * Previously part of GraphCanvas.jsx (lines 84–159), which held five visualizers
 * behind ad-hoc predicates. The DSU renderer is structurally different from all
 * other canvases — parent[] and rank[] tables, component cards — so it belongs
 * in its own file.
 *
 * Header and legend are removed: CanvasShell owns those now.
 */
export default function DsuCanvas({ problem, currentStep, step }) {
  const activeStep = currentStep || step;
  const variables = activeStep?.variables || {};

  const parentStr = variables['parent[]'] || '[0, 1, 2, 3, 4, 5, 6, 7]';
  const rankStr = variables['rank[]'] || '[0, 0, 0, 0, 0, 0, 0, 0]';
  const dsuSetsStr = variables['Disjoint Sets'] || '{1}, {2}, {3}, {4}, {5}, {6}, {7}';
  const dsuOpStr = variables['Operation'] || 'Initialize DSU(7)';

  const parentArr = parentStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());
  const rankArr = rankStr.replace(/[\[\]]/g, '').split(',').map(s => s.trim());

  // How many elements (skip index 0 when it's just padding)
  const count = Math.max(parentArr.length - 1, 7);
  const indices = Array.from({ length: count }, (_, i) => i + 1);

  return (
    <div style={{ flex: 1, padding: '12px 16px', display: 'flex', flexDirection: 'column', width: '100%', height: '100%', overflow: 'auto' }}>
      <div style={{ width: '100%', display: 'flex', flexDirection: 'column', gap: '20px', alignItems: 'center' }}>
        {/* Operation banner */}
        <div style={{
          display: 'flex', alignItems: 'center', gap: '10px',
          padding: '8px 16px',
          background: 'rgba(255, 176, 0, 0.1)',
          border: '1px solid rgba(255, 176, 0, 0.3)',
          borderRadius: '10px',
          fontFamily: 'var(--font-code)'
        }}>
          <span style={{ fontSize: '0.86rem', fontWeight: '800', color: 'var(--bench-ink)' }}>
            {dsuOpStr}
          </span>
        </div>

        {/* Connected components */}
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '14px', justifyContent: 'center', maxWidth: '700px' }}>
          <div style={{
            display: 'flex', flexDirection: 'column', gap: '6px',
            padding: '12px 18px',
            background: 'var(--bench-fill)',
            border: '1px solid var(--bench-rule)',
            borderRadius: '12px'
          }}>
            <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--probe)', textTransform: 'uppercase', letterSpacing: '0.6px', fontFamily: 'var(--font-code)' }}>
              Connected Components
            </span>
            <span style={{ fontSize: '0.95rem', fontWeight: '800', color: 'var(--bench-ink)', fontFamily: 'var(--font-code)' }}>
              {dsuSetsStr}
            </span>
          </div>
        </div>

        {/* Parent & Rank tables */}
        <div style={{
          display: 'flex', flexDirection: 'column', gap: '12px',
          width: '100%', maxWidth: '650px',
          background: 'var(--bench-fill)',
          padding: '16px', borderRadius: '12px',
          border: '1px solid var(--bench-rule)'
        }}>
          {/* Element index header */}
          <div style={{ display: 'grid', gridTemplateColumns: `80px repeat(${count}, 1fr)`, gap: '8px', alignItems: 'center', textAlign: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)' }}>Element i</span>
            {indices.map(idx => (
              <div key={idx} style={{
                padding: '4px',
                background: 'var(--bench-ground)',
                borderRadius: '6px',
                fontSize: '0.8rem', fontWeight: '800', color: 'var(--bench-ink)',
                fontFamily: 'var(--font-code)'
              }}>
                {idx}
              </div>
            ))}
          </div>

          {/* parent[i] row */}
          <div style={{ display: 'grid', gridTemplateColumns: `80px repeat(${count}, 1fr)`, gap: '8px', alignItems: 'center', textAlign: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--probe)', fontFamily: 'var(--font-code)' }}>parent[i]</span>
            {indices.map(idx => {
              const val = parentArr[idx] || idx;
              const isRoot = String(val) === String(idx);
              return (
                <div
                  key={idx}
                  style={{
                    padding: '8px 4px',
                    background: isRoot ? 'rgba(255, 176, 0, 0.15)' : 'var(--bench-ground)',
                    border: isRoot ? '1px solid var(--probe)' : '1px solid var(--bench-rule)',
                    boxShadow: isRoot ? '0 0 12px rgba(255, 176, 0, 0.3)' : 'none',
                    borderRadius: '8px',
                    fontSize: '0.88rem', fontWeight: '800',
                    color: isRoot ? 'var(--probe)' : 'var(--bench-ink)',
                    fontFamily: 'var(--font-code)',
                    transition: 'all 0.3s ease'
                  }}
                >
                  {val}
                </div>
              );
            })}
          </div>

          {/* rank[i] row */}
          <div style={{ display: 'grid', gridTemplateColumns: `80px repeat(${count}, 1fr)`, gap: '8px', alignItems: 'center', textAlign: 'center' }}>
            <span style={{ fontSize: '0.72rem', fontWeight: '800', color: 'var(--settled)', fontFamily: 'var(--font-code)' }}>rank[i]</span>
            {indices.map(idx => {
              const rVal = rankArr[idx] || 0;
              return (
                <div key={idx} style={{
                  padding: '6px 4px',
                  background: 'rgba(61, 220, 151, 0.1)',
                  border: '1px solid rgba(61, 220, 151, 0.3)',
                  borderRadius: '8px',
                  fontSize: '0.82rem', fontWeight: '800',
                  color: 'var(--settled)',
                  fontFamily: 'var(--font-code)'
                }}>
                  {rVal}
                </div>
              );
            })}
          </div>
        </div>
      </div>
    </div>
  );
}
