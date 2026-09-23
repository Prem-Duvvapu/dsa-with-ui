import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import styles from './TourGuide.module.css';

/**
 * The guided tour, opened from the header.
 *
 * Every step points at a `data-tour` attribute rather than a CSS class or a coordinate.
 * Classes are styling decisions that move with a redesign and positions move with the
 * viewport; the attribute is a contract that a refactor has to break deliberately. This
 * matters here more than usual - the code panel moved from below the canvas to beside it
 * in this same branch, which is exactly the change that silently breaks a tour.
 *
 * A step whose target is not on screen right now - a collapsed panel, a control that only
 * exists on desktop - is dropped before the tour starts rather than pointing at nothing.
 * TourGuide.test.jsx asserts every declared target exists in the rendered app, so a
 * removed anchor fails a test instead of shipping a tour that highlights empty space.
 */
// {{totalProblems}} is substituted at render time from the live catalogue length, not
// hardcoded - the number this replaced (433) was already stale once by the time anyone
// noticed. TOUR_STEPS itself stays a plain exported array, structurally unchanged, because
// TourGuide.test.jsx iterates it to assert every declared target exists in the rendered
// app; only the substitution happens per-render, in the component.
export const TOUR_STEPS = [
  {
    target: 'problem-list',
    title: 'Every problem, one list',
    body: '{{totalProblems}} problems, each with a real execution trace rather than a recording. '
        + 'Press / from anywhere to jump into the search box.'
  },
  {
    target: 'canvas',
    title: 'The algorithm actually runs',
    body: 'This is the real algorithm executing on the input below — arrays, trees, graphs, '
        + 'grids and DP tables each get the picture that suits them.'
  },
  {
    target: 'controls',
    title: 'Drive it like a player',
    body: 'Play, pause, scrub, and change speed. Space plays, the arrow keys step, '
        + 'and [ and ] change the pace without touching the mouse.'
  },
  {
    target: 'capture-strip',
    body: 'Every step of the run at once — one column per step, one row per tracked slot. '
        + 'It shows the shape of the execution before you watch any single frame: how long '
        + 'the run is, and where the interesting part sits. Click any column to jump there. '
        + 'Some problems hide it, because for a graph the diagram already says it better.',
    title: 'The whole run, at a glance'
  },
  {
    target: 'code-panel',
    title: 'The code keeps pace',
    body: 'The highlighted line is the one executing in the step you are looking at. '
        + 'That pairing is the whole point — the picture and the code never drift apart.'
  },
  {
    target: 'input-summary',
    title: 'Always know what it is running on',
    body: 'The input being animated is stated here. Open the editor from the buttons on the '
        + 'right to try your own.'
  },
  {
    target: 'panel-toggles',
    title: 'Clear the desk',
    body: 'The input editor and the complexity analysis stay out of the way until you want '
        + 'them, so the animation gets the room. Your choice is remembered.'
  },
  {
    target: 'theme',
    title: 'Light or dark, your call',
    body: 'Follows your system by default. Click to pin it light or dark.'
  }
];

const PAD = 8;

/** Where the tooltip sits relative to the spotlight, kept inside the viewport. */
function placeTooltip(rect, tipSize) {
  const { innerWidth: vw, innerHeight: vh } = window;
  const gap = 12;
  const below = rect.bottom + gap;
  const above = rect.top - gap - tipSize.height;

  const top = below + tipSize.height <= vh - gap
    ? below
    : above >= gap
      ? above
      : Math.max(gap, Math.min(vh - tipSize.height - gap, rect.top));

  const left = Math.max(gap, Math.min(
    vw - tipSize.width - gap,
    rect.left + rect.width / 2 - tipSize.width / 2
  ));

  return { top, left };
}

export default function TourGuide({ open, onClose, steps = TOUR_STEPS, totalProblems }) {
  const [index, setIndex] = useState(0);
  const [visibleSteps, setVisibleSteps] = useState([]);
  const [rect, setRect] = useState(null);
  const [tipPos, setTipPos] = useState({ top: 0, left: 0 });
  const tipRef = useRef(null);
  const nextRef = useRef(null);

  // Resolve targets once per opening. A step whose anchor is not rendered right now is
  // dropped rather than shown pointing at nothing.
  useEffect(() => {
    if (!open) return;
    const present = steps.filter(
      (s) => document.querySelector(`[data-tour="${s.target}"]`) !== null
    );
    setVisibleSteps(present);
    setIndex(0);
  }, [open, steps]);

  const step = visibleSteps[index];

  const measure = useCallback(() => {
    if (!step) return;
    const el = document.querySelector(`[data-tour="${step.target}"]`);
    if (!el) return;
    const r = el.getBoundingClientRect();
    setRect({ top: r.top, left: r.left, width: r.width, height: r.height,
              bottom: r.bottom, right: r.right });
  }, [step]);

  useLayoutEffect(() => { measure(); }, [measure]);

  useEffect(() => {
    if (!open) return undefined;
    window.addEventListener('resize', measure);
    window.addEventListener('scroll', measure, true);
    return () => {
      window.removeEventListener('resize', measure);
      window.removeEventListener('scroll', measure, true);
    };
  }, [open, measure]);

  useLayoutEffect(() => {
    if (!rect || !tipRef.current) return;
    const t = tipRef.current.getBoundingClientRect();
    setTipPos(placeTooltip(rect, { width: t.width, height: t.height }));
  }, [rect]);

  useEffect(() => { if (open) nextRef.current?.focus(); }, [open, index]);

  const total = visibleSteps.length;
  const isLast = index >= total - 1;

  const next = useCallback(() => {
    if (isLast) onClose();
    else setIndex((i) => i + 1);
  }, [isLast, onClose]);

  const back = useCallback(() => setIndex((i) => Math.max(0, i - 1)), []);

  // The tour owns the keyboard while it is up, so the app's own shortcuts cannot step the
  // trace out from under the thing being described.
  useEffect(() => {
    if (!open) return undefined;
    const onKey = (e) => {
      if (e.code === 'Escape') { e.preventDefault(); onClose(); return; }
      if (e.code === 'ArrowRight' || e.code === 'Enter') { e.preventDefault(); next(); return; }
      if (e.code === 'ArrowLeft') { e.preventDefault(); back(); }
    };
    window.addEventListener('keydown', onKey, true);
    return () => window.removeEventListener('keydown', onKey, true);
  }, [open, next, back, onClose]);

  if (!open || !step || !rect) return null;

  return (
    <div className={styles.overlay} data-testid="tour-guide">
      {/* Four panels around the target rather than one box-shadow: the cutout stays crisp
          at any size and the dimmed areas remain clickable-through-free. */}
      <div className={styles.shade} style={{ top: 0, left: 0, right: 0, height: Math.max(0, rect.top - PAD) }} />
      <div className={styles.shade} style={{ top: rect.bottom + PAD, left: 0, right: 0, bottom: 0 }} />
      <div className={styles.shade} style={{ top: rect.top - PAD, left: 0, width: Math.max(0, rect.left - PAD), height: rect.height + PAD * 2 }} />
      <div className={styles.shade} style={{ top: rect.top - PAD, left: rect.right + PAD, right: 0, height: rect.height + PAD * 2 }} />

      <div
        className={styles.spotlight}
        style={{ top: rect.top - PAD, left: rect.left - PAD, width: rect.width + PAD * 2, height: rect.height + PAD * 2 }}
      />

      <div
        ref={tipRef}
        role="dialog"
        aria-modal="true"
        aria-labelledby="tour-step-title"
        className={`glass-panel ${styles.tip}`}
        style={{ top: tipPos.top, left: tipPos.left }}
      >
        <div className={styles.tipHeader}>
          <span className={styles.progress}>{index + 1} of {total}</span>
          <button type="button" onClick={onClose} aria-label="End the tour" className={styles.closeBtn}>
            <X size={13} />
          </button>
        </div>

        <h3 id="tour-step-title" className={styles.tipTitle}>{step.title}</h3>
        <p className={styles.tipBody}>
          {step.body.replace('{{totalProblems}}', totalProblems ? String(totalProblems) : 'All the')}
        </p>

        <div className={styles.tipActions}>
          <button type="button" onClick={onClose} className={styles.skipBtn}>Skip</button>
          <div className={styles.tipNav}>
            <button
              type="button"
              onClick={back}
              disabled={index === 0}
              className={`btn btn-outline ${styles.navBtn}`}
            >
              Back
            </button>
            <button ref={nextRef} type="button" onClick={next} className={`btn btn-primary ${styles.navBtn}`}>
              {isLast ? 'Done' : 'Next'}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
