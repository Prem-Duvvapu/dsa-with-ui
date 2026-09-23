import React, { useEffect, useRef, useState } from 'react';
import { Play, Pause, SkipBack, SkipForward, RotateCcw, Link2, Check, AlertTriangle } from 'lucide-react';
import styles from './Controls.module.css';

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

  // 'idle' | 'copied' | 'failed'. useShareableView already mirrors the current step (and
  // any custom input) into the URL with `replace`, for every run - so the address bar at
  // any moment already IS the shareable link. This never reconstructs one; it only copies
  // what is already there, which is also why it needs no props from the caller.
  const [copyState, setCopyState] = useState('idle');
  const resetTimer = useRef(null);
  useEffect(() => () => clearTimeout(resetTimer.current), []);

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopyState('copied');
    } catch {
      // Never fail silently: an insecure context, an older browser, or a denied
      // permission are all real, and a click that does nothing looks like a broken button
      // rather than an environment limitation.
      setCopyState('failed');
    }
    clearTimeout(resetTimer.current);
    resetTimer.current = setTimeout(() => setCopyState('idle'), 2000);
  };

  return (
    <div className={styles.container}>
      <div className={styles.row}>
        {/* Step Counter & Scrubber Slider */}
        <div className={styles.scrubberWrapper}>
          <span aria-label="Playback position" className={styles.stepCounter}>
            Step <strong>{hasSteps ? safeIndex + 1 : 0}</strong> of {stepCount}
          </span>
          <input
            type="range"
            min="0"
            max={maxIndex}
            value={safeIndex}
            disabled={!hasSteps}
            aria-label="Trace step"
            onChange={(e) => onStepSelect && onStepSelect(Number(e.target.value))}
            className={`step-scrubber-slider ${styles.slider}`}
            style={{
              cursor: hasSteps ? 'pointer' : 'not-allowed',
              opacity: hasSteps ? 1 : 0.4
            }}
          />
        </div>

        {/* Control Buttons */}
        <div className={styles.buttonGroup}>
          <button className="btn btn-outline" onClick={onReset} disabled={!hasSteps} title="Reset (Shortcut: R)">
            <RotateCcw size={13} /> Reset
          </button>

          <button
            type="button"
            className="btn btn-outline"
            onClick={copyLink}
            title="Copy a link to this exact step"
            aria-label="Copy link to this step"
          >
            {copyState === 'copied' ? <Check size={13} /> : copyState === 'failed' ? <AlertTriangle size={13} /> : <Link2 size={13} />}
            {' '}{copyState === 'copied' ? 'Copied' : copyState === 'failed' ? 'Copy failed' : 'Copy link'}
          </button>

          {copyState !== 'idle' && (
            <span role="status" className="sr-only">
              {copyState === 'copied'
                ? 'Link to this step copied to clipboard.'
                : 'Could not copy the link. Copy the address bar instead.'}
            </span>
          )}

          <button
            className="btn btn-outline"
            onClick={onStepPrev}
            disabled={!hasSteps || safeIndex <= 0}
            style={{ opacity: !hasSteps || safeIndex <= 0 ? 0.4 : 1 }}
          >
            <SkipBack size={13} /> Prev
          </button>

          <button
            className={`btn btn-primary ${styles.playBtn}`}
            onClick={onPlayPause}
            disabled={!hasSteps}
          >
            {isPlaying ? <><Pause size={14} /> Pause</> : <><Play size={14} /> Play</>}
          </button>

          <button
            className="btn btn-outline"
            onClick={onStepNext}
            disabled={!hasSteps || safeIndex >= stepCount - 1}
            style={{ opacity: !hasSteps || safeIndex >= stepCount - 1 ? 0.4 : 1 }}
          >
            Next <SkipForward size={13} />
          </button>

          {/* Segmented Speed Control (Single Control Container) */}
          <div className={`mobile-hide ${styles.speedContainer}`}>
            {[0.5, 1.0, 2.0, 4.0].map((spdVal) => {
              const ms = Math.round(1000 / spdVal);
              const isActive = Math.abs(speed - ms) < 50;
              return (
                <button
                  key={spdVal}
                  onClick={() => onSpeedChange(ms)}
                  disabled={!hasSteps}
                  className={`${styles.speedBtn} ${isActive ? styles.speedBtnActive : ''}`}
                  style={{
                    cursor: hasSteps ? 'pointer' : 'not-allowed',
                    opacity: hasSteps ? 1 : 0.4
                  }}
                >
                  {spdVal}x
                </button>
              );
            })}
          </div>
        </div>

        {/* Quiet Trailing Keyboard Shortcut Text */}
        <span className={`mobile-hide ${styles.shortcutHint}`}>
          space · ← · → · r
        </span>
      </div>
    </div>
  );
}
