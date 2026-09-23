import React, { useEffect, useRef } from 'react';
import { X } from 'lucide-react';
import styles from './ShortcutHelp.module.css';

/**
 * The shortcut list, opened with `?`.
 *
 * The app had working shortcuts and no way to discover them: Space/arrows/R were only
 * written down in a `title` on two buttons. A keyboard contract nobody can find is a
 * keyboard contract nobody uses.
 */
export const SHORTCUTS = [
  { keys: ['Space', 'K'], label: 'Play / pause' },
  { keys: ['→', 'L', '.'], label: 'Next step' },
  { keys: ['←', 'J', ','], label: 'Previous step' },
  { keys: ['Home'], label: 'Jump to the first step' },
  { keys: ['End'], label: 'Jump to the last step' },
  { keys: ['R'], label: 'Reset the trace' },
  { keys: ['['], label: 'Slower' },
  { keys: [']'], label: 'Faster' },
  { keys: ['/'], label: 'Search problems' },
  { keys: ['⌘K', 'Ctrl+K'], label: 'Open command palette' },
  { keys: ['?'], label: 'Show this list' },
  { keys: ['Esc'], label: 'Close this list or the sidebar' }
];

export default function ShortcutHelp({ open, onClose, onReplayWelcome }) {
  const closeRef = useRef(null);

  // Focus the dialog's own control on open, so Escape and Tab act on the dialog rather
  // than on whatever was focused behind it.
  useEffect(() => {
    if (open) closeRef.current?.focus();
  }, [open]);

  if (!open) return null;

  return (
    <div
      className={styles.backdrop}
      onClick={onClose}
      data-testid="shortcut-help-backdrop"
    >
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Keyboard shortcuts"
        className={`glass-panel ${styles.panel}`}
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.header}>
          <h2 className={styles.title}>Keyboard shortcuts</h2>
          <button
            ref={closeRef}
            type="button"
            onClick={onClose}
            aria-label="Close keyboard shortcuts"
            className={styles.closeBtn}
          >
            <X size={14} />
          </button>
        </div>

        <ul className={styles.list}>
          {SHORTCUTS.map(({ keys, label }) => (
            <li key={label} className={styles.row}>
              <span className={styles.keys}>
                {keys.map((k) => <kbd key={k} className={styles.key}>{k}</kbd>)}
              </span>
              <span className={styles.label}>{label}</span>
            </li>
          ))}
        </ul>

        <p className={styles.note}>
          Shortcuts pause while you are typing in an input.
        </p>

        {onReplayWelcome && (
          <button type="button" onClick={onReplayWelcome} className={styles.replayBtn}>
            Show the introduction again
          </button>
        )}
      </div>
    </div>
  );
}
