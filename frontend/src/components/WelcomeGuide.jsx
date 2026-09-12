import React, { useEffect, useRef } from 'react';
import { List, Play, Code2, Keyboard } from 'lucide-react';
import styles from './WelcomeGuide.module.css';

/**
 * The one screen a first-time visitor sees, and only the first time.
 *
 * Deliberately not a multi-step tour: those get clicked through reflexively and teach
 * nothing, and they go stale the moment the layout moves. This names the three regions
 * already on screen behind it and gets out of the way. Ongoing reference lives in the
 * shortcut overlay, which this points at.
 *
 * It is shown once, tracked in localStorage, and reachable again from that overlay - a
 * first-run screen nobody can get back to is a first-run screen that punishes a misclick.
 */
const STEPS = [
  {
    icon: List,
    title: 'Pick a problem',
    body: 'The list on the left holds 433 of them, every one with a real execution trace. '
        + 'Press / to jump straight to the search box.'
  },
  {
    icon: Play,
    title: 'Watch it run',
    body: 'The canvas animates the actual algorithm on the input shown beneath it — not a '
        + 'recording. Space plays and pauses; the arrow keys step one at a time.'
  },
  {
    icon: Code2,
    title: 'Follow the code',
    body: 'The panel on the right highlights the exact line being executed, in step with '
        + 'the animation. That pairing is the point of the whole thing.'
  }
];

export default function WelcomeGuide({ open, onDismiss, onShowShortcuts }) {
  const dismissRef = useRef(null);

  useEffect(() => {
    if (open) dismissRef.current?.focus();
  }, [open]);

  if (!open) return null;

  return (
    <div className={styles.backdrop} data-testid="welcome-guide">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="welcome-title"
        className={`glass-panel ${styles.panel}`}
      >
        <h2 id="welcome-title" className={styles.title}>Welcome to the DSA Visualizer</h2>
        <p className={styles.lede}>Three things worth knowing before you start.</p>

        <ol className={styles.steps}>
          {STEPS.map(({ icon: Icon, title, body }, index) => (
            <li key={title} className={styles.step}>
              <span className={styles.stepIcon} aria-hidden="true"><Icon size={15} /></span>
              <div className={styles.stepText}>
                <h3 className={styles.stepTitle}>
                  <span className={styles.stepNumber}>{index + 1}</span> {title}
                </h3>
                <p className={styles.stepBody}>{body}</p>
              </div>
            </li>
          ))}
        </ol>

        <div className={styles.actions}>
          <button
            type="button"
            onClick={onShowShortcuts}
            className={`btn btn-outline ${styles.secondaryBtn}`}
          >
            <Keyboard size={13} /> See all shortcuts
          </button>
          <button
            ref={dismissRef}
            type="button"
            onClick={onDismiss}
            className={`btn btn-primary ${styles.primaryBtn}`}
          >
            Start exploring
          </button>
        </div>

        <p className={styles.note}>You can reopen this from the shortcuts panel at any time.</p>
      </div>
    </div>
  );
}
