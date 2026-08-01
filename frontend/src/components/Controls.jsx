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
    <div className="glass-panel" style={{ padding: '16px 24px', display: 'flex', flexDirection: 'column', gap: '12px' }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap', gap: '16px' }}>
        {/* Step Counter & Scrubber */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flex: '1', minWidth: '240px' }}>
          <span style={{ fontSize: '0.85rem', fontWeight: '700', color: 'var(--text-secondary)', whiteSpace: 'nowrap' }}>
            Step <strong style={{ color: 'var(--accent-indigo)', fontSize: '1rem' }}>{currentStepIndex + 1}</strong> / {totalSteps || 1}
          </span>
          {/* Interactive Step Scrubber Slider */}
          <input
            type="range"
            min="0"
            max={maxIndex}
            value={currentStepIndex}
            onChange={(e) => onStepSelect && onStepSelect(Number(e.target.value))}
            style={{
              flex: '1',
              accentColor: 'var(--accent-indigo)',
              cursor: 'pointer',
              height: '6px',
              borderRadius: '3px'
            }}
            title="Drag to jump to any step in the complete execution trace"
          />
        </div>

        {/* Control Buttons */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <button className="btn btn-secondary" onClick={onReset} title="Reset to Start" style={{ padding: '8px 12px' }}>
            <RotateCcw size={16} /> Reset
          </button>

          <button className="btn btn-secondary" onClick={onStepPrev} disabled={currentStepIndex <= 0} style={{ padding: '8px 14px', opacity: currentStepIndex <= 0 ? 0.4 : 1 }}>
            <SkipBack size={18} /> Prev
          </button>

          <button className={`btn ${isPlaying ? 'btn-danger' : 'btn-primary'}`} onClick={onPlayPause} style={{ padding: '8px 20px', minWidth: '100px', justifyContent: 'center' }}>
            {isPlaying ? <><Pause size={18} /> Pause</> : <><Play size={18} /> Play</>}
          </button>

          <button className="btn btn-secondary" onClick={onStepNext} disabled={currentStepIndex >= (totalSteps - 1)} style={{ padding: '8px 14px', opacity: currentStepIndex >= (totalSteps - 1) ? 0.4 : 1 }}>
            Next <SkipForward size={18} />
          </button>
        </div>

        {/* Speed Slider */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <Sliders size={16} color="var(--text-muted)" />
          <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>Speed:</span>
          <input
            type="range"
            min="200"
            max="2000"
            step="100"
            value={speed}
            onChange={(e) => onSpeedChange(Number(e.target.value))}
            style={{ accentColor: 'var(--accent-indigo)', cursor: 'pointer', width: '90px' }}
          />
          <span style={{ fontSize: '0.78rem', fontFamily: 'var(--font-code)', color: 'var(--accent-cyan)', width: '38px' }}>
            {(2000 / speed).toFixed(1)}x
          </span>
        </div>
      </div>

      {/* Live Step Explanation Banner */}
      {stepDescription && (
        <div style={{
          background: 'rgba(99, 102, 241, 0.12)',
          border: '1px solid rgba(99, 102, 241, 0.3)',
          borderRadius: '10px',
          padding: '10px 16px',
          fontSize: '0.88rem',
          color: '#ffffff',
          display: 'flex',
          alignItems: 'center',
          gap: '10px'
        }}>
          <span style={{ fontWeight: '700', color: 'var(--accent-indigo)', background: 'rgba(99, 102, 241, 0.25)', padding: '2px 8px', borderRadius: '6px', fontSize: '0.75rem' }}>
            LIVE TRACE
          </span>
          <span>{stepDescription}</span>
        </div>
      )}
    </div>
  );
}
