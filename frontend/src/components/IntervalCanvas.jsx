import React from 'react';
import styles from './IntervalCanvas.module.css';
import CanvasShell from './CanvasShell';

/**
 * IntervalCanvas — renders interval and scheduling problems on a calibrated continuous timeline.
 *
 * Each interval occupies a horizontal bar spanning [start, end].
 * Used by DsType.INTERVAL (e.g. n-meetings-in-one-room, merge-intervals, insert-interval).
 *
 * State tokens:
 * - Current/evaluating interval: var(--probe)
 * - Selected/scheduled/merged interval: var(--settled)
 * - Rejected/non-overlapping: var(--bench-rule-strong)
 * - Default/pending: var(--bench-fill) with var(--bench-rule-strong) border
 */

function parseMeetingsVar(str) {
  if (!str || typeof str !== 'string') return [];
  // Parse format like: "[#1(1-2), #2(3-4), ...]"
  const matches = [...str.matchAll(/#?(\d+)\s*\(\s*(\d+)\s*[-→]\s*(\d+)\s*\)/g)];
  return matches.map((m) => ({
    id: Number(m[1]),
    label: `#${m[1]}`,
    start: Number(m[2]),
    end: Number(m[3])
  }));
}

export default function IntervalCanvas({ problem, currentStep, step, resolvedInput }) {
  const activeStep = currentStep || step;
  // What the server actually ran, echoed back on the trace. This used to be read off the
  // STEP - `activeStep.resolvedInput` - which nothing ever sets, so those branches had
  // never executed and the chain fell through to the inputSpec defaults instead. For
  // n-meetings-in-one-room that is 15 of its 16 steps, so a custom run drew the default
  // meetings while the narration described the caller's.
  const ranOn = resolvedInput ?? activeStep?.resolvedInput ?? null;

  // Extract intervals from available step metadata
  let intervals = [];

  if (activeStep?.intervals?.length) {
    intervals = activeStep.intervals.map((inv, idx) => ({
      id: idx + 1,
      label: `#${idx + 1}`,
      start: Array.isArray(inv) ? inv[0] : (inv.start ?? 0),
      end: Array.isArray(inv) ? inv[1] : (inv.end ?? 0),
      state: inv.state || 'default'
    }));
  } else if (activeStep?.arrayState?.length
      && activeStep.arrayState.every((cell) => /^\[\s*-?\d+\s*,\s*-?\d+\s*\]$/.test(cell.label || ''))) {
    // Interval tracers sort or merge their working set, so the live array labels are
    // more authoritative than resolvedInput's original order.
    intervals = activeStep.arrayState.map((cell, idx) => {
      const [, start, end] = cell.label.match(/^\[\s*(-?\d+)\s*,\s*(-?\d+)\s*\]$/);
      return {
        id: idx + 1,
        label: `#${idx + 1}`,
        start: Number(start),
        end: Number(end),
        state: cell.state || 'default'
      };
    });
  } else if (activeStep?.variables?.meetings) {
    const parsed = parseMeetingsVar(activeStep.variables.meetings);
    intervals = parsed.map((m, idx) => {
      const state = activeStep?.arrayState?.[idx]?.state || 'default';
      return { ...m, state };
    });
  } else if (Array.isArray(ranOn?.intervals)) {
    intervals = ranOn.intervals
      .filter((interval) => Array.isArray(interval) && interval.length >= 2)
      .map((interval, idx) => ({
        id: idx + 1,
        label: `#${idx + 1}`,
        start: interval[0],
        end: interval[1],
        state: activeStep?.arrayState?.[idx]?.state || 'default'
      }));
  } else if (ranOn?.start && ranOn?.end) {
    const starts = ranOn.start;
    const ends = ranOn.end;
    intervals = starts.map((s, idx) => ({
      id: idx + 1,
      label: `#${idx + 1}`,
      start: s,
      end: ends[idx] ?? s,
      state: activeStep?.arrayState?.[idx]?.state || 'default'
    }));
  } else if (problem?.inputSpec) {
    // Last resort, and only honest before a run has happened: the defaults are what a run
    // WOULD use. Once one has, `ranOn` above is the truth and this must not be reached.
    const startField = problem.inputSpec.fields?.find((f) => f.name === 'start');
    const endField = problem.inputSpec.fields?.find((f) => f.name === 'end');
    if (startField?.defaultValue && endField?.defaultValue) {
      intervals = startField.defaultValue.map((s, idx) => ({
        id: idx + 1,
        label: `#${idx + 1}`,
        start: s,
        end: endField.defaultValue[idx] ?? s,
        state: 'default'
      }));
    }
  }

  // No fallback. This used to fabricate [1,2] [3,4] [0,6] [5,7] - n-meetings-in-one-room's
  // own default meetings, hardcoded, with invented 'settled' and 'probe' states - so a run
  // that fell off the end of the chain above showed another problem's data as if it were
  // its own. That is RCA-031 (DsuCanvas) in a second canvas, and the repo rule is explicit:
  // a canvas that cannot tell what to draw says so.
  if (!intervals.length) {
    return (
      <CanvasShell title={problem?.title || 'Interval Timeline'} meta="no intervals">
        <p role="status" className="canvas-empty">
          No intervals in this step.
        </p>
      </CanvasShell>
    );
  }

  // Calculate timeline bounds
  const minTime = intervals.length ? Math.min(...intervals.map((i) => i.start), 0) : 0;
  const maxTime = intervals.length ? Math.max(...intervals.map((i) => i.end)) : 10;
  const timeRange = Math.max(maxTime - minTime, 1);

  // Parse sweep position (e.g. room availability lastEnd)
  const lastEndVar = activeStep?.variables?.lastEnd;
  const sweepPos = (lastEndVar !== undefined && lastEndVar !== '-1') ? Number(lastEndVar) : null;

  // Generate integer time tick marks
  const ticks = [];
  const tickStep = timeRange > 30 ? 5 : (timeRange > 15 ? 2 : 1);
  for (let t = minTime; t <= maxTime; t += tickStep) {
    ticks.push(t);
  }

  const getIntervalStyle = (state) => {
    switch (state) {
      case 'probe':
      case 'active':
      case 'current':
      case 'comparing':
        return {
          background: 'var(--probe)',
          color: 'var(--probe-on)',
          border: '1px solid var(--probe)',
          fontWeight: '700',
          boxShadow: 'var(--state-current-glow)'
        };
      case 'settled':
      case 'sorted':
      case 'done':
      case 'selected':
      case 'visited':
        return {
          background: 'var(--settled)',
          color: 'var(--settled-on)',
          border: '1px solid var(--settled)',
          fontWeight: '700',
          boxShadow: 'var(--state-done-glow)'
        };
      case 'target':
        // Bench's `read`: a hollow probe ring rather than a sixth colour. That is what
        // lets "being looked at" and "being written" share one hue and still read apart.
        return {
          background: 'transparent',
          color: 'var(--probe)',
          border: '1px solid var(--probe)',
          fontWeight: '600'
        };
      case 'rejected':
      case 'eliminated':
        return {
          background: 'var(--bench-recessed)',
          color: 'var(--bench-ink-dim)',
          border: '1px dashed var(--bench-rule-strong)',
          opacity: 0.6
        };
      default:
        return {
          background: 'var(--bench-fill)',
          color: 'var(--bench-ink-secondary)',
          border: '1px solid var(--bench-rule-strong)',
          fontWeight: '500'
        };
    }
  };

  return (
    <CanvasShell
      title={problem?.title || 'Interval Timeline'}
      meta={`${intervals.length} intervals (span ${minTime} → ${maxTime})`}
      legend={true}
    >
      <div
        data-testid="interval-canvas-stage"
        className={styles.stage}
      >
        {/* Interval Spans Stack */}
        <div
          className={styles.spans}
          style={{ '--interval-rows-height': `${intervals.length * 42}px` }}
        >
          {intervals.map((inv) => {
            const leftPct = ((inv.start - minTime) / timeRange) * 100;
            const widthPct = Math.max(((inv.end - inv.start) / timeRange) * 100, 2);
            const style = getIntervalStyle(inv.state);

            return (
              <div
                key={inv.id}
                className={styles.lane}
              >
                {/* Visual Span Bar */}
                <div
                  data-testid={`interval-span-${inv.id}`}
                  className={styles.span}
                  style={{
                    left: `${leftPct}%`,
                    width: `${widthPct}%`,
                    zIndex: inv.state === 'probe' ? 3 : (inv.state === 'settled' ? 2 : 1),
                    ...style
                  }}
                  title={`${inv.label}: [${inv.start} → ${inv.end}] (${inv.state})`}
                >
                  <span className={styles.spanLabel}>
                    {inv.label}
                  </span>
                  <span className={styles.spanRange}>
                    [{inv.start}, {inv.end}]
                  </span>
                </div>
              </div>
            );
          })}

          {/* Sweep line for current room / availability boundary if present */}
          {sweepPos !== null && sweepPos >= minTime && sweepPos <= maxTime && (
            <div
              className={styles.sweep}
              style={{ left: `${((sweepPos - minTime) / timeRange) * 100}%` }}
            >
              <div
                className={styles.sweepLabel}
              >
                t = {sweepPos}
              </div>
            </div>
          )}
        </div>

        {/* Timeline Axis along bottom */}
        <div
          className={styles.axis}
        >
          {ticks.map((t) => {
            const leftPct = ((t - minTime) / timeRange) * 100;
            return (
              <div
                key={`tick-${t}`}
                className={styles.tick}
                style={{ left: `${leftPct}%` }}
              >
                <div className={styles.tickMark} />
                <span
                  className={styles.tickLabel}
                >
                  {t}
                </span>
              </div>
            );
          })}
        </div>
      </div>
    </CanvasShell>
  );
}
