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
  const maxIndex = Math.max(0, (totalSteps || 1) - 1);

  return (
    <div style={{ padding: '8px 16px', display: 'flex', flexDirection: 'column', gap: '8px', borderTop: '1px solid var(--border-default)', background: 'rgba(15, 23, 42, 0.4)', flexShrink: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '12px' }}>
        {/* Step Counter & Scrubber Slider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: '1', minWidth: '200px' }}>
          <span style={{ fontSize: '0.74rem', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
            Step <strong style={{ color: 'var(--text-primary)', fontWeight: '700' }}>{currentStepIndex + 1}</strong> of {totalSteps || 1}
          </span>
          <input
            type="range"
            min="0"
            max={maxIndex}
            value={currentStepIndex}
            onChange={(e) => onStepSelect && onStepSelect(Number(e.target.value))}
            className="step-scrubber-slider"
            style={{ flex: 1, accentColor: 'var(--accent-violet)', cursor: 'pointer' }}
          />
        </div>

        {/* Control Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <button className="btn btn-outline" onClick={onReset} title="Reset (Shortcut: R)">
            <RotateCcw size={13} /> Reset
          </button>

          <button className="btn btn-outline" onClick={onStepPrev} disabled={currentStepIndex <= 0} style={{ opacity: currentStepIndex <= 0 ? 0.4 : 1 }}>
            <SkipBack size={13} /> Prev
          </button>

          <button className="btn btn-primary" onClick={onPlayPause} style={{ minWidth: '82px', justifyContent: 'center' }}>
            {isPlaying ? <><Pause size={14} /> Pause</> : <><Play size={14} /> Play</>}
          </button>

          <button className="btn btn-outline" onClick={onStepNext} disabled={currentStepIndex >= (totalSteps - 1)} style={{ opacity: currentStepIndex >= (totalSteps - 1) ? 0.4 : 1 }}>
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
                  style={{
                    padding: '2px 7px',
                    borderRadius: '4px',
                    fontSize: '0.68rem',
                    fontWeight: isActive ? '700' : '500',
                    border: 'none',
                    background: isActive ? 'var(--accent-violet)' : 'transparent',
                    color: isActive ? '#ffffff' : 'var(--text-muted)',
                    cursor: 'pointer',
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
