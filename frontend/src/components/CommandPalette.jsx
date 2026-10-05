import React, { useEffect, useId, useRef, useState } from 'react';
import { Search, Sun, Link2, Check, AlertTriangle, History, ArrowRight } from 'lucide-react';
import { useProblemSearch } from '../search/useProblemSearch';
import { matchRanges } from '../search/scoreProblem';
import useModalDialog from '../hooks/useModalDialog';
import styles from './CommandPalette.module.css';

/** Results shown in the dialog; the rest are one "View all" away, in the library. */
export const PALETTE_RESULT_LIMIT = 8;

function Highlighted({ query, text }) {
  const parts = [];
  let cursor = 0;
  matchRanges(query, text).forEach(([start, end]) => {
    parts.push(text.slice(cursor, start), <mark key={start}>{text.slice(start, end)}</mark>);
    cursor = end;
  });
  parts.push(text.slice(cursor));
  return <>{parts}</>;
}

/**
 * The single switcher, opened with Cmd/Ctrl+K (or `/`) from the workspace. It is a modal
 * combobox: focus stays in the query, arrows move the active option (announced through
 * aria-activedescendant and scrolled into view), Enter chooses it, Escape cancels. The page
 * behind it is inert, and closing returns focus to whatever opened it (useModalDialog).
 *
 * Results are the library's own ranking and highlighting, bounded to a handful; "View all"
 * carries the query to the library, where filters and the full list live. An empty query
 * offers recent searches - shared with the library - and the universal commands.
 *
 * Deliberately out of scope: problem-specific actions such as "run the other case". Those
 * need the active problem's editor state, which this dialog has no reason to hold.
 */
export default function CommandPalette({
  isOpen, onClose, problems, onSelectProblem, onCycleTheme, onViewAll,
  currentProblemId = null, catalogLoading = false, catalogError = null
}) {
  const rootRef = useRef(null);
  const inputRef = useRef(null);
  const listRef = useRef(null);
  const listId = useId();
  useModalDialog(rootRef, isOpen, {
    initialFocusRef: inputRef,
    fallbackFocus: () => document.getElementById('workspace-view')
  });

  const { query, setQuery, results, isSearching, recents, commitRecent } = useProblemSearch({ problems });
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

  // The active option is the one Enter would choose, so it must be on screen.
  useEffect(() => {
    if (!isOpen) return;
    listRef.current?.querySelector('[aria-selected="true"]')?.scrollIntoView?.({ block: 'nearest' });
  }, [activeIndex, isOpen, query]);

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

  const trimmed = query.trim();
  let items;
  if (isSearching) {
    items = results.slice(0, PALETTE_RESULT_LIMIT).map((p) => ({
      id: `problem-${p.id}`,
      label: <Highlighted query={trimmed} text={p.title} />,
      detail: p.category,
      current: p.id === currentProblemId,
      run: () => { commitRecent(trimmed); onSelectProblem(p.id); }
    }));
    if (results.length > 0 && onViewAll) {
      items.push({
        id: 'view-all',
        icon: ArrowRight,
        label: results.length > PALETTE_RESULT_LIMIT
          ? `View all ${results.length} results in the library`
          : 'Open these results in the library',
        run: () => { commitRecent(trimmed); onViewAll(trimmed); }
      });
    }
  } else {
    items = [
      ...recents.map((recent) => ({
        id: `recent-${recent}`, icon: History, label: recent, detail: 'Recent search',
        run: () => setQuery(recent)
      })),
      { id: 'toggle-theme', label: 'Toggle theme', icon: Sun, run: () => { onCycleTheme(); onClose(); } },
      {
        id: 'copy-link',
        label: copyLinkLabel,
        icon: copyState === 'copied' ? Check : copyState === 'failed' ? AlertTriangle : Link2,
        run: copyLink
      }
    ];
  }

  const active = Math.min(activeIndex, Math.max(0, items.length - 1));
  const optionId = (index) => `${listId}-option-${index}`;

  const handleKeyDown = (event) => {
    if (event.key === 'ArrowDown') {
      event.preventDefault();
      setActiveIndex(items.length ? (active + 1) % items.length : 0);
    } else if (event.key === 'ArrowUp') {
      event.preventDefault();
      setActiveIndex(items.length ? (active - 1 + items.length) % items.length : 0);
    } else if (event.key === 'Enter') {
      event.preventDefault();
      items[active]?.run();
    } else if (event.key === 'Escape') {
      // The dialog owns its own Escape; preventDefault tells the page's shortcuts it is handled.
      event.preventDefault();
      onClose();
    }
  };

  let status = null;
  if (isSearching && items.length === 0) {
    status = catalogLoading ? 'Loading the catalogue…'
      : catalogError ? 'The catalogue did not load, so there is nothing to search yet.'
      : `No problem matches “${trimmed}”.`;
  }

  return (
    <div ref={rootRef} className={styles.backdrop} onClick={onClose} data-testid="command-palette-backdrop">
      <div
        role="dialog"
        aria-modal="true"
        aria-label="Command palette"
        className={`glass-panel ${styles.panel}`}
        onClick={(event) => event.stopPropagation()}
      >
        <div className={styles.inputRow}>
          <Search size={15} className={styles.searchIcon} aria-hidden="true" />
          <input
            ref={inputRef}
            type="text"
            role="combobox"
            aria-expanded="true"
            aria-controls={listId}
            aria-autocomplete="list"
            aria-activedescendant={items.length ? optionId(active) : undefined}
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Jump to a problem, or run a command…"
            aria-label="Jump to a problem or run a command"
            className={styles.input}
          />
        </div>

        <ul ref={listRef} id={listId} role="listbox" aria-label={isSearching ? 'Matching problems' : 'Recent searches and commands'} className={styles.list}>
          {items.map((item, index) => {
            const Icon = item.icon;
            return (
              <li
                key={item.id}
                id={optionId(index)}
                role="option"
                aria-selected={index === active}
                onClick={item.run}
                onMouseMove={() => { if (index !== active) setActiveIndex(index); }}
                className={`${styles.item} ${index === active ? styles.itemActive : ''}`}
              >
                {Icon && <Icon size={14} className={styles.itemIcon} aria-hidden="true" />}
                <span className={styles.itemLabel}>{item.label}</span>
                {item.current && <span className={styles.itemTag}>Current</span>}
                {item.detail && <span className={styles.itemDetail}>{item.detail}</span>}
              </li>
            );
          })}
        </ul>
        {status && <p role="status" className={styles.empty}>{status}</p>}

        <p className={styles.hint}>
          {isSearching
            ? `${results.length} match${results.length === 1 ? '' : 'es'}. ↑↓ to move, Enter to open, Esc to close.`
            : 'Type to search all problems. ↑↓ to move, Enter to run, Esc to close.'}
        </p>
      </div>
    </div>
  );
}
