import React from 'react';
import { Pause, Play, RotateCcw, SkipBack, SkipForward } from 'lucide-react';
import styles from './ProblemWorkspace.module.css';

export const SPEEDS = [
  { ms: 2000, label: '0.5×' },
  { ms: 1000, label: '1×' },
  { ms: 500, label: '2×' },
  { ms: 250, label: '4×' }
];

/**
 * Restart / Previous / Play / Next, the seek slider and speed, shared by every view so
 * there is exactly one way to move through a run. Every control is named by its visible
 * text; "Restart" restarts PLAYBACK and never touches the input (that is "Restore
 * defaults", in the editor).
 */
export default function PlaybackBar({ session, onSpeedChange, compact = false }) {
  const { steps, currentStepIndex, isPlaying, speed, togglePlay, stepNext, stepPrev, reset, seek } = session;
  const total = steps.length;
  const has = total > 0;
  const index = has ? Math.min(currentStepIndex, total - 1) : 0;

  return (
    <div className={`${styles.playback}${compact ? ` ${styles.playbackCompact}` : ''}`} data-tour="controls">
      <div className={styles.transport}>
        {!compact && (
          <button type="button" className={styles.control} onClick={reset} disabled={!has}>
            <RotateCcw size={16} aria-hidden="true" /> Restart
          </button>
        )}
        <button type="button" className={styles.control} onClick={stepPrev} disabled={!has || index === 0}>
          <SkipBack size={16} aria-hidden="true" /> Previous
        </button>
        <button type="button" className={`${styles.control} ${styles.primary}`} onClick={togglePlay} disabled={!has}>
          {isPlaying ? <Pause size={16} aria-hidden="true" /> : <Play size={16} aria-hidden="true" />}
          {isPlaying ? 'Pause' : 'Play'}
        </button>
        <button type="button" className={styles.control} onClick={stepNext} disabled={!has || index >= total - 1}>
          Next <SkipForward size={16} aria-hidden="true" />
        </button>
      </div>
      <input
        type="range"
        className={styles.seek}
        min="0"
        max={Math.max(0, total - 1)}
        value={index}
        disabled={!has}
        aria-label="Seek to step"
        aria-valuetext={has ? `Step ${index + 1} of ${total}` : 'No steps'}
        onChange={(event) => seek(Number(event.target.value))}
      />
      {!compact && (
        <label className={styles.speed}>
          Speed
          <select value={speed} onChange={(event) => onSpeedChange(Number(event.target.value))} disabled={!has}>
            {SPEEDS.map((option) => <option key={option.ms} value={option.ms}>{option.label}</option>)}
          </select>
        </label>
      )}
    </div>
  );
}
