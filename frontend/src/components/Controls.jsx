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
  onSpeedChange,
  stepDescription
}) {
  const maxIndex = Math.max(0, (totalSteps || 1) - 1);

  return (
    <div style={{ padding: '8px 16px', display: 'flex', flexDirection: 'column', gap: '8px', borderTop: '1px solid rgba(255, 255, 255, 0.08)', background: 'rgba(15, 23, 42, 0.5)', flexShrink: 0 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '10px' }}>
        {/* Step Counter & Scrubber Slider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', flex: '1', minWidth: '220px' }}>
          <span style={{ fontSize: '0.8rem', fontWeight: '700', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
            Step <strong style={{ color: 'var(--accent-indigo)', fontSize: '0.92rem' }}>{currentStepIndex + 1}</strong> / {totalSteps || 1}
          </span>
          <input
            type="range"
            min="0"
            max={maxIndex}
            value={currentStepIndex}
            onChange={(e) => onStepSelect && onStepSelect(Number(e.target.value))}
            className="step-scrubber-slider"
            title="Drag to jump to any step in the complete execution trace"
            aria-label="Execution step timeline scrubber"
            style={{ flex: 1 }}
          />
        </div>

        {/* Control Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 'var(--space-xs)' }}>
          <button className="btn btn-secondary" onClick={onReset} title="Reset to Start (Shortcut: R)" aria-label="Reset execution to start" style={{ padding: '5px 10px', fontSize: 'var(--text-xs)' }}>
            <RotateCcw size={14} /> Reset
          </button>

          <button className="btn btn-secondary" onClick={onStepPrev} disabled={currentStepIndex <= 0} title="Previous Step (Shortcut: Left Arrow)" aria-label="Step backward" style={{ padding: '5px 10px', fontSize: 'var(--text-xs)', opacity: currentStepIndex <= 0 ? 0.4 : 1 }}>
            <SkipBack size={15} /> Prev
          </button>

          <button className={`btn ${isPlaying ? 'btn-danger' : 'btn-primary'}`} onClick={onPlayPause} title="Play / Pause (Shortcut: Spacebar)" aria-label={isPlaying ? "Pause execution playback" : "Start execution playback"} style={{ padding: '5px 16px', minWidth: '90px', justifyContent: 'center', fontSize: 'var(--text-sm)' }}>
            {isPlaying ? <><Pause size={16} /> Pause</> : <><Play size={16} /> Play</>}
          </button>

          <button className="btn btn-secondary" onClick={onStepNext} disabled={currentStepIndex >= (totalSteps - 1)} title="Next Step (Shortcut: Right Arrow)" aria-label="Step forward" style={{ padding: '5px 10px', fontSize: 'var(--text-xs)', opacity: currentStepIndex >= (totalSteps - 1) ? 0.4 : 1 }}>
            Next <SkipForward size={15} />
          </button>
        </div>

        {/* Speed Selector Pills */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
          <Sliders size={14} color="var(--text-muted)" />
          <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>Speed:</span>
          {[0.5, 1.0, 2.0, 4.0].map((spdVal) => {
            const ms = Math.round(1000 / spdVal);
            const isActive = Math.abs(speed - ms) < 50;
            return (
              <button
                key={spdVal}
                onClick={() => onSpeedChange(ms)}
                style={{
                  padding: '2px 6px',
                  borderRadius: '5px',
                  fontSize: '0.72rem',
                  fontWeight: '700',
                  border: isActive ? '1px solid var(--accent-indigo)' : '1px solid rgba(255, 255, 255, 0.1)',
                  background: isActive ? 'rgba(99, 102, 241, 0.25)' : 'rgba(255, 255, 255, 0.05)',
                  color: isActive ? '#ffffff' : 'var(--text-secondary)',
                  cursor: 'pointer'
                }}
              >
                {spdVal}x
              </button>
            );
          })}
        </div>

        {/* Keyboard Hotkeys Badge */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '4px', fontSize: '0.68rem', color: 'var(--text-muted)', background: 'rgba(255,255,255,0.04)', padding: '3px 8px', borderRadius: '5px', border: '1px solid var(--border-color)', fontWeight: '600' }}>
          <span>[Space] [←] [→] [R]</span>
        </div>
      </div>

      {/* Live Step Explanation Banner */}
      {stepDescription && (
        <div style={{
          background: 'rgba(99, 102, 241, 0.12)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          borderRadius: '8px',
          padding: '6px 12px',
          fontSize: '0.82rem',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          gap: '8px'
        }}>
          <span style={{ fontWeight: '800', color: 'var(--accent-indigo)', background: 'rgba(99, 102, 241, 0.25)', padding: '2px 6px', borderRadius: '4px', fontSize: '0.7rem', letterSpacing: '0.4px' }}>
            LIVE TRACE
          </span>
          <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{stepDescription}</span>
        </div>
      )}
    </div>
  );
}
