import React, { useEffect, useRef, useState } from 'react';
import { Search, Sun, Link2, Check, AlertTriangle } from 'lucide-react';
import { useProblemSearch } from '../search/useProblemSearch';
import useFocusTrap from '../hooks/useFocusTrap';
import styles from './CommandPalette.module.css';

/**
 * Opened with Cmd/Ctrl+K from anywhere in the app - unlike `/`, which only focuses the
 * sidebar's own search box, this reaches every problem (and a couple of universal
 * commands) even when the sidebar is closed or off-screen on mobile.
 *
 * Deliberately out of scope here: problem-specific actions like "run the other case" or
 * loading a saved input preset. Those need the active problem's InputPanel state, which
 * this palette has no reason to hold - threading it through would turn "reachable from
 * anywhere" into "coupled to whatever problem happens to be open".
 */
export default function CommandPalette({ isOpen, onClose, problems, onSelectProblem, onCycleTheme }) {
  const panelRef = useRef(null);
  useFocusTrap(panelRef, isOpen);

  const { query, setQuery, visible, isSearching } = useProblemSearch({ problems });
  const [activeIndex, setActiveIndex] = useState(0);
  const [copyState, setCopyState] = useState('idle');
  const resetTimer = useRef(null);

  useEffect(() => {
    if (isOpen) {
      setQuery('');
      setCopyState('idle');
      setActiveIndex(0);
    }
  }, [isOpen, setQuery]);

  useEffect(() => () => clearTimeout(resetTimer.current), []);
  useEffect(() => { setActiveIndex(0); }, [query]);

  if (!isOpen) return null;

  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    }
    clearTimeout(resetTimer.current);
    resetTimer.current = setTimeout(() => setCopyState('idle'), 2000);
  };

  const copyLinkLabel = copyState === 'copied' ? 'Link copied'
    : copyState === 'failed' ? 'Copy failed - try again'
    : 'Copy link to this step';

  const actions = [
    { id: 'toggle-theme', label: 'Toggle theme', icon: Sun, run: () => { onCycleTheme(); onClose(); } },
    {
      id: 'copy-link',
      label: copyLinkLabel,
      icon: copyState === 'copied' ? Check : copyState === 'failed' ? AlertTriangle : Link2,
      run: copyLink
    }
  ];

  const items = isSearching
    ? visible.map((p) => ({ id: p.id, label: p.title, run: () => onSelectProblem(p.id) }))
    : actions;

  const handleKeyDown = (event) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setActiveIndex((i) => (items.length ? (i + 1) % items.length : 0));
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex((i) => (items.length ? (i - 1 + items.length) % items.length : 0));
    } else if (event.key === 'Enter') {
      event.preventDefault();
      items[activeIndex]?.run();
    }
  };

  return (
    <div className={styles.backdrop} onClick={onClose} data-testid="command-palette-backdrop">
      <div
        ref={panelRef}
        role="dialog"
        aria-modal="true"
        aria-label="Command palette"
        className={`glass-panel ${styles.panel}`}
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.inputRow}>
          <Search size={15} className={styles.searchIcon} />
          <input
            type="text"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Jump to a problem, or run a command…"
            aria-label="Jump to a problem or run a command"
            className={styles.input}
          />
        </div>

        <ul className={styles.list}>
          {items.length === 0 && <li className={styles.empty}>No matches</li>}
          {items.map((item, index) => {
            const Icon = item.icon;
            return (
              <li key={item.id}>
                <button
                  type="button"
                  onClick={item.run}
                  onMouseEnter={() => setActiveIndex(index)}
                  className={`${styles.item} ${index === activeIndex ? styles.itemActive : ''}`}
                >
                  {Icon && <Icon size={14} className={styles.itemIcon} />}
                  {item.label}
                </button>
              </li>
            );
          })}
        </ul>

        {!isSearching && (
          <p className={styles.hint}>Type to search all problems. ↑↓ to move, Enter to run, Esc to close.</p>
        )}
      </div>
    </div>
  );
}
