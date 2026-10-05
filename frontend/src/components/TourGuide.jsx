import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from 'react';
import { X } from 'lucide-react';
import useModalDialog from '../hooks/useModalDialog';
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
 * A step whose target is not on screen right now - hidden (Focus mode, the phone Menu's
 * collapsed contents), or not rendered at all - is dropped before the tour starts rather
 * than pointing at nothing. Each step scrolls its target into view before spotlighting it,
 * so a tour on a short or narrow screen does not describe something below the fold.
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
    target: 'switcher',
    title: 'Every problem, one search away',
    body: '{{totalProblems}} problems, each with a real execution trace rather than a recording. '
        + 'Switch problem (or Ctrl/⌘ K, or /) jumps to any of them; All algorithms returns to the library.'
  },
  {
    target: 'view-rail',
    title: 'Three ways to look at one run',
    body: 'Playground shows the picture, Code walkthrough puts the Java beside it with the '
        + 'executing line highlighted, and Analysis lists the variables, stack and complexity. '
        + 'Switching never restarts the run or loses your place.'
  },
  {
    target: 'canvas',
    title: 'The algorithm actually runs',
    body: 'This is the real algorithm executing on the input below — arrays, trees, graphs, '
        + 'grids and DP tables each get the picture that suits them.'
  },
  {
    target: 'focus',
    title: 'Clear everything else away',
    body: 'Focus hides the statement, the editor and the rest of the page, and keeps the '
        + 'diagram, its narration and the controls. Exit focus, or Esc, brings them back '
        + 'exactly as you left them.'
  },
  {
    target: 'controls',
    title: 'Drive it like a player',
    body: 'Play, pause, scrub, and change speed. Space plays, the arrow keys step, '
        + 'and [ and ] change the pace without touching the mouse.'
  },
  {
    target: 'input-summary',
    title: 'Always know what it is running on',
    body: 'The input the run on screen actually used. If you have edited the input without '
        + 'running it, this says so; Copy link shares exactly this run and step.'
  },
  {
    target: 'input-editor',
    title: 'Try your own input',
    body: 'Change the input and run it, or try the other case the problem declares. '
        + 'The result above stays until your run succeeds, and a saved input comes back later.'
  },
  {
    target: 'capture-strip',
    body: 'Every step of the run at once — one column per step, one row per tracked slot. '
        + 'Open it to see how long the run is and where the interesting part sits, then click '
        + 'any column to jump there. Graphs, trees and DP tables skip it: their diagram already '
        + 'shows the whole run.',
    title: 'The whole run, at a glance'
  },
  {
    target: 'theme',
    title: 'Light or dark, your call',
    body: 'Follows your system by default. Click to pin it light or dark.'
  }
];

const PAD = 8;

/** A target the reader can actually see: rendered, not hidden, not in a closed disclosure. */
export function isShown(element) {
  if (!element || element.closest('[hidden], [inert]')) return false;
  // CSS can hide it too: the desktop header links are display:none on a phone. Browsers
  // answer that directly; jsdom has no layout and no checkVisibility, so it skips this.
  if (typeof element.checkVisibility === 'function' && !element.checkVisibility()) return false;
  const closed = element.closest('details:not([open])');
  if (closed && element !== closed && element.closest('summary')?.parentElement !== closed) return false;
  return true;
}

function prefersReducedMotion() {
  try {
    return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true;
  } catch {
    return false;
  }
}

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
  const overlayRef = useRef(null);

  // Resolve targets once per opening. A step whose anchor is not rendered right now is
  // dropped rather than shown pointing at nothing.
  useEffect(() => {
    if (!open) return;
    const present = steps.filter((s) => isShown(document.querySelector(`[data-tour="${s.target}"]`)));
    setVisibleSteps(present);
    setIndex(0);
  }, [open, steps]);

  const step = visibleSteps[index];

  // Bring each target on screen before measuring it.
  useLayoutEffect(() => {
    if (!open || !step) return;
    document.querySelector(`[data-tour="${step.target}"]`)
      ?.scrollIntoView?.({ block: 'center', behavior: prefersReducedMotion() ? 'auto' : 'instant' });
  }, [open, step]);

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

  // The page behind is inert while the tour is up; when it ends, focus goes back to whatever
  // started it, or to the page when that was a dialog that has since closed.
  useModalDialog(overlayRef, Boolean(open && step && rect), {
    initialFocusRef: nextRef,
    fallbackFocus: () => document.getElementById('workspace-view')
  });
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
    <div ref={overlayRef} className={styles.overlay} data-testid="tour-guide">
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
