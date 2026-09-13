import React from 'react';
import styles from './ArrayCanvas.module.css';
import { lastPayload } from '../trace/lastPayload';

export default function ArrayCanvas({ problem, currentStep, step, steps, currentStepIndex }) {
  const activeStep = currentStep || step;
  // Absence is not "show the default". Tracers restate a structure only on the steps
  // that change it, so falling through to the catalogue default drew the CATALOGUE's
  // data over the caller's own input. Measured app-wide: 1506 steps across 142 of 232
  // problems. The default is honest only before any step has emitted anything.
  const carried = lastPayload(steps, currentStepIndex, 'arrayState', activeStep);
  const rawArray = (carried && carried.length > 0)
    ? carried 
    : (problem?.defaultArray && problem.defaultArray.length > 0) 
      ? problem.defaultArray 
      : [{ value: 2, state: 'default' }, { value: 7, state: 'comparing' }, { value: 11, state: 'active' }, { value: 15, state: 'sorted' }];

  const normalizedArray = rawArray.map((el, idx) => {
    if (typeof el === 'object' && el !== null) {
      const value = el.value !== undefined ? el.value : (el.val !== undefined ? el.val : idx);
      const state = el.state || 'default';
      const index = el.index !== undefined ? el.index : idx;
      const label = typeof el.label === 'string' && el.label.length > 0 ? el.label : null;
      return { value, state, index, label };
    }
    return { value: Number(el) || 0, state: 'default', index: idx };
  });

  const getElementColor = (state) => {
    switch (state) {
      case 'pivot':
      case 'max':
      case 'target':
        return { bg: 'linear-gradient(180deg, var(--state-target), var(--state-target-deep))', border: 'var(--state-target)', glow: 'var(--state-target-glow)' };
      case 'comparing':
      case 'active':
      case 'current':
        return { bg: 'linear-gradient(180deg, var(--state-current), var(--state-current-deep))', border: 'var(--state-current)', glow: 'var(--state-current-glow)' };
      case 'swapping':
        return { bg: 'linear-gradient(180deg, var(--role-pruned), var(--role-pruned-edge))', border: 'var(--role-pruned)', glow: '0 0 14px color-mix(in srgb, var(--role-pruned) 50%, transparent)' };
      case 'sorted':
      case 'done':
        return { bg: 'linear-gradient(180deg, var(--state-done), var(--state-done-deep))', border: 'var(--state-done)', glow: 'var(--state-done-glow)' };
      case 'visited':
      case 'eliminated':
      default:
        return { bg: 'linear-gradient(180deg, var(--canvas-node-fill-2), var(--canvas-node-fill))', border: 'var(--border-default)', glow: 'none' };
    }
  };

  const values = normalizedArray.map(el => Math.abs(el.value));
  const maxVal = Math.max(...values, 1);

  return (
    <div className={styles.wrap}>
      {/* Array Stage with Faint Horizontal Gridlines */}
      <div className={styles.stage} data-testid="array-stage">
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

              {/* Whatever the tracer chose to call this cell, or its index when it said
                  nothing. The VALUE stays on top because that is what the bar height
                  encodes; the label is the extra dimension - minimum-platforms' A/D event
                  kind, candy's rating→candies, job-sequencing's deadline and profit - and
                  before this it was set by three tracers and drawn by none. */}
              <span
                style={{
                  fontSize: el.label ? '0.68rem' : '0.72rem',
                  color: 'var(--text-muted)',
                  fontFamily: 'var(--font-code)',
                  fontWeight: '600',
                  maxWidth: '64px',
                  textAlign: 'center',
                  overflowWrap: 'anywhere'
                }}
              >
                {el.label ?? `[${idx}]`}
              </span>
            </div>
          );
        })}
      </div>
    </div>
  );
}
