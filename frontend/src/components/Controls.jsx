import React from 'react';
import { Play, Pause, SkipBack, SkipForward, RotateCcw, Sliders } from 'lucide-react';

export default function Controls({
  isPlaying,
  currentStepIndex,
  totalSteps,
  speed,
  onPlayPause,
  onStepNext,
  onStepPrev,
  onStepSelect,
  onReset,
  onSpeedChange
}) {
  const stepCount = Number.isInteger(totalSteps) && totalSteps > 0 ? totalSteps : 0;
  const hasSteps = stepCount > 0;
  const safeIndex = hasSteps
    ? Math.min(Math.max(currentStepIndex || 0, 0), stepCount - 1)
    : 0;
  const maxIndex = Math.max(0, stepCount - 1);

  return (
    <div style={{ padding: '8px 16px', display: 'flex', flexDirection: 'column', gap: '8px', borderTop: '1px solid var(--border-default)', background: 'rgba(15, 23, 42, 0.4)', flexShrink: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        {/* Step Counter & Scrubber Slider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: '1', minWidth: '200px' }}>
          <span aria-label="Playback position" style={{ fontSize: '0.74rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
            Step <strong style={{ color: 'var(--text-primary)', fontWeight: '700' }}>{hasSteps ? safeIndex + 1 : 0}</strong> of {stepCount}
          </span>
          <input
            type="range"
            min="0"
            max={maxIndex}
            value={safeIndex}
            disabled={!hasSteps}
            aria-label="Trace step"
            onChange={(e) => onStepSelect && onStepSelect(Number(e.target.value))}
            className="step-scrubber-slider"
            style={{ flex: 1, accentColor: 'var(--accent-violet)', cursor: hasSteps ? 'pointer' : 'not-allowed', opacity: hasSteps ? 1 : 0.4 }}
          />
        </div>

        {/* Control Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <button className="btn btn-outline" onClick={onReset} disabled={!hasSteps} title="Reset (Shortcut: R)">
            <RotateCcw size={13} /> Reset
          </button>

          <button className="btn btn-outline" onClick={onStepPrev} disabled={!hasSteps || safeIndex <= 0} style={{ opacity: !hasSteps || safeIndex <= 0 ? 0.4 : 1 }}>
            <SkipBack size={13} /> Prev
          </button>

          <button className="btn btn-primary" onClick={onPlayPause} disabled={!hasSteps} style={{ minWidth: '82px', justifyContent: 'center' }}>
            {isPlaying ? <><Pause size={14} /> Pause</> : <><Play size={14} /> Play</>}
          </button>

          <button className="btn btn-outline" onClick={onStepNext} disabled={!hasSteps || safeIndex >= stepCount - 1} style={{ opacity: !hasSteps || safeIndex >= stepCount - 1 ? 0.4 : 1 }}>
            Next <SkipForward size={13} />
          </button>

          {/* Segmented Speed Control (Single Control Container) */}
          <div className="mobile-hide" style={{ display: 'flex', alignItems: 'center', background: 'rgba(255, 255, 255, 0.04)', borderRadius: 'var(--radius-sm)', border: '1px solid var(--border-default)', padding: '2px', marginLeft: '4px' }}>
            {[0.5, 1.0, 2.0, 4.0].map((spdVal) => {
              const ms = Math.round(1000 / spdVal);
              const isActive = Math.abs(speed - ms) < 50;
              return (
                <button
                  key={spdVal}
                  onClick={() => onSpeedChange(ms)}
                  disabled={!hasSteps}
                  style={{
                    padding: '2px 7px',
                    borderRadius: '4px',
                    fontSize: '0.68rem',
                    fontWeight: isActive ? '700' : '500',
                    border: 'none',
                    background: isActive ? 'var(--accent-violet)' : 'transparent',
                    color: isActive ? 'var(--text-on-accent)' : 'var(--text-muted)',
                    cursor: hasSteps ? 'pointer' : 'not-allowed',
                    opacity: hasSteps ? 1 : 0.4,
                    transition: 'all 0.15s ease'
                  }}
                >
                  {spdVal}x
                </button>
              );
            })}
          </div>
        </div>

        {/* Quiet Trailing Keyboard Shortcut Text */}
        <span className="mobile-hide" style={{ fontSize: '0.68rem', color: 'var(--text-muted)', fontFamily: 'var(--font-code)' }}>
          space · ← · → · r
        </span>
      </div>
    </div>
  );
}
