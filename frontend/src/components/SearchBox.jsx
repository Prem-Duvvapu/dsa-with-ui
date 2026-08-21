import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Search, X, CornerDownLeft, ArrowUpDown } from 'lucide-react';
import { useProblemSearch } from '../search/useProblemSearch';
import { matchRanges } from '../search/scoreProblem';
import { normalizeCategory } from '../search/normalizeCategory';

const IS_APPLE = typeof navigator !== 'undefined' && (
  /Mac|iPod|iPhone|iPad/.test(navigator.platform || '') ||
  /Macintosh|Mac OS X/.test(navigator.userAgent || '')
);
const SHORTCUT_HINT = IS_APPLE ? '⌘K' : 'Ctrl K';

/** Difficulty is shown as a single mono letter — see the note in index.css. */
const DIFFICULTY = {
  easy: { letter: 'E', className: 'sb-diff-easy', label: 'Easy' },
  medium: { letter: 'M', className: 'sb-diff-medium', label: 'Medium' },
  hard: { letter: 'H', className: 'sb-diff-hard', label: 'Hard' }
};

function HighlightedText({ text, query }) {
  if (!query || !text) return <span>{text}</span>;
  const ranges = matchRanges(query, text);
  if (ranges.length === 0) return <span>{text}</span>;

  const elements = [];
  let lastIndex = 0;
  ranges.forEach(([start, end], idx) => {
    if (start > lastIndex) elements.push(text.slice(lastIndex, start));
    elements.push(<mark key={idx}>{text.slice(start, end)}</mark>);
    lastIndex = end;
  });
  if (lastIndex < text.length) elements.push(text.slice(lastIndex));
  return <span>{elements}</span>;
}

export default function SearchBox({
  problems = [],
  activeProblemId = null,
  activeCategory = null,
  onSelectCategory = () => {},
  onSelectProblem = () => {},
  onRetry = null,
  children = null
}) {
  const inputRef = useRef(null);
  const [isFocused, setIsFocused] = useState(false);

  const {
    query, setQuery,
    runnableOnly, setRunnableOnly,
    visible, totalMatches, globalMatches,
    activeIndex, setActiveIndex,
    recents, commitRecent,
    isSearching
  } = useProblemSearch({ problems, activeCategory, normalizeCategory });

  const hasTracedProblems = useMemo(
    () => Array.isArray(problems) && problems.some(p => p && p.traced !== undefined),
    [problems]
  );

  const runnableCount = useMemo(
    () => (Array.isArray(problems) ? problems.filter(p => p && p.traced === true).length : 0),
    [problems]
  );

  // ⌘K / Ctrl+K from anywhere focuses the field.
  useEffect(() => {
    const onGlobalKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        inputRef.current?.focus();
        inputRef.current?.select();
      }
    };
    window.addEventListener('keydown', onGlobalKeyDown);
    return () => window.removeEventListener('keydown', onGlobalKeyDown);
  }, []);

  const openProblem = (id) => {
    if (query.trim()) commitRecent(query);
    onSelectProblem(id);
  };

  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (visible.length === 0) return;
      setActiveIndex(activeIndex === -1 ? 0 : (activeIndex + 1) % visible.length);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (visible.length === 0) return;
      setActiveIndex(activeIndex === -1
        ? visible.length - 1
        : (activeIndex - 1 + visible.length) % visible.length);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (visible.length === 0) return;
      const target = activeIndex >= 0 ? visible[activeIndex] : visible[0];
      if (target) openProblem(target.id);
    } else if (e.key === 'Escape') {
      e.preventDefault();
      if (query.length > 0) setQuery('');
      else inputRef.current?.blur();
    } else if (e.key === 'Home' && visible.length > 0) {
      e.preventDefault();
      setActiveIndex(0);
    } else if (e.key === 'End' && visible.length > 0) {
      e.preventDefault();
      setActiveIndex(visible.length - 1);
    }
  };

  // Keep the highlighted row on screen.
  useEffect(() => {
    if (activeIndex >= 0 && visible[activeIndex]) {
      // getElementById, not querySelector: a problem id containing '.' or '/'
      // would make the interpolated selector a SyntaxError.
      const el = document.getElementById(`problem-opt-${visible[activeIndex].id}`);
      if (el && typeof el.scrollIntoView === 'function') el.scrollIntoView({ block: 'nearest' });
    }
  }, [activeIndex, visible]);

  const scopeLabel = activeCategory || 'the catalogue';
  const emptyStateMessage = isSearching
    ? `No algorithm matches “${query}”.`
    : runnableOnly
      ? `Nothing in ${scopeLabel} is runnable yet.`
      : `No problems in ${scopeLabel}.`;

  const countLabel = isSearching
    ? (totalMatches > 50
        ? <>showing <strong>50</strong> of <strong>{totalMatches}</strong></>
        : <><strong>{totalMatches}</strong> {totalMatches === 1 ? 'result' : 'results'}</>)
    : (<><strong>{totalMatches}</strong> {activeCategory ? 'in scope' : 'algorithms'}</>);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '14px', flex: 1, minHeight: 0 }}>
      {/* Sticky head: the field must survive scrolling a 433-row list. */}
      <div className="sb-sticky">
        <div className="sb-field">
          <span className="sb-adorn sb-adorn-left">
            <Search size={14} color="var(--text-muted)" />
          </span>

          <input
            ref={inputRef}
            className="sb-input"
            role="combobox"
            aria-expanded={isSearching}
            aria-controls="problem-results"
            aria-activedescendant={
              activeIndex >= 0 && visible[activeIndex]
                ? `problem-opt-${visible[activeIndex].id}`
                : undefined
            }
            aria-label="Search algorithms"
            aria-describedby="search-hint"
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            onKeyDown={handleKeyDown}
            onFocus={() => setIsFocused(true)}
            onBlur={() => setIsFocused(false)}
            placeholder={problems.length ? `Search ${problems.length} algorithms…` : 'Search algorithms…'}
          />

          {query ? (
            <button
              type="button"
              className="sb-clear"
              onClick={() => { setQuery(''); inputRef.current?.focus(); }}
              aria-label="Clear search"
            >
              <X size={13} />
            </button>
          ) : !isFocused ? (
            <span className="sb-adorn sb-adorn-right">
              <span className="sb-kbd" aria-hidden="true">{SHORTCUT_HINT}</span>
            </span>
          ) : null}

          {/* Always rendered, so aria-describedby never dangles. */}
          <span id="search-hint" className="sr-only">
            {`Press ${SHORTCUT_HINT} to focus. Up and down arrows browse results, Enter opens, Escape clears.`}
          </span>
        </div>

        <div className="sb-meta">
          <span className="sb-count">{countLabel}</span>

          {hasTracedProblems && (
            <button
              type="button"
              className={runnableOnly ? 'sb-toggle sb-toggle-on' : 'sb-toggle'}
              aria-pressed={runnableOnly}
              onClick={() => setRunnableOnly(!runnableOnly)}
              title={`${runnableCount} of ${problems.length} problems execute on your input`}
            >
              <span className="sb-toggle-dot" />
              runnable {runnableCount}
            </button>
          )}
        </div>
      </div>

      {recents.length > 0 && !isSearching && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          <span className="sb-eyebrow">Recent</span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '5px' }}>
            {recents.map((item) => (
              <button
                key={item}
                type="button"
                className="sb-empty-action"
                style={{ fontSize: '0.7rem', padding: '3px 9px' }}
                onClick={() => { setQuery(item); inputRef.current?.focus(); }}
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      )}

      <div role="status" aria-live="polite" className="sr-only">
        {isSearching ? `${totalMatches} results for ${query}` : ''}
      </div>

      {children}

      <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', minHeight: 0 }}>
        {problems.length === 0 ? (
          <div className="sb-empty">
            <span className="sb-empty-title">Can’t reach the backend.</span>
            <span className="sb-empty-sub">expected http://localhost:8923</span>
            <button
              type="button"
              className="sb-empty-action sb-empty-primary"
              onClick={() => (onRetry ? onRetry() : window.location.reload())}
            >
              Try again
            </button>
          </div>
        ) : totalMatches > 0 ? (
          <div className="sb-list" role="listbox" id="problem-results" tabIndex={-1}>
            {visible.map((prob, i) => {
              const isCurrent = activeProblemId === prob.id;
              const isActive = i === activeIndex;
              const diff = DIFFICULTY[(prob.difficulty || '').toLowerCase()];
              const rowClass = [
                'sb-row',
                prob.traced === true ? 'sb-row-runnable' : '',
                isActive ? 'sb-row-active' : '',
                isCurrent ? 'sb-row-current' : ''
              ].filter(Boolean).join(' ');

              return (
                <div
                  key={prob.id}
                  role="option"
                  id={`problem-opt-${prob.id}`}
                  aria-selected={isActive}
                  className={rowClass}
                  onClick={() => openProblem(prob.id)}
                  onMouseEnter={() => setActiveIndex(i)}
                >
                  <span className="sb-row-text">
                    <span className="sb-title">
                      <HighlightedText text={prob.title} query={query} />
                    </span>
                    <span className="sb-sub">
                      {normalizeCategory(prob.category)}
                      {prob.traced === true ? ' · runnable' : ''}
                    </span>
                  </span>

                  <span className="sb-row-meta">
                    {diff && (
                      <span className={`sb-diff ${diff.className}`} title={diff.label}>
                        <span aria-hidden="true">{diff.letter}</span>
                        <span className="sr-only">{diff.label}</span>
                      </span>
                    )}
                  </span>
                </div>
              );
            })}
          </div>
        ) : (
          <div className="sb-empty">
            <span className="sb-empty-title">{emptyStateMessage}</span>
            {globalMatches > 0 && activeCategory && (
              <button
                type="button"
                className="sb-empty-action sb-empty-primary"
                onClick={() => onSelectCategory(null)}
              >
                {isSearching
                  ? `Search all ${globalMatches} matches`
                  : `Show all ${globalMatches} across every category`}
              </button>
            )}
            {runnableOnly && (
              <button
                type="button"
                className="sb-empty-action"
                onClick={() => setRunnableOnly(false)}
              >
                Include problems that aren’t runnable yet
              </button>
            )}
          </div>
        )}
      </div>

      {/* The keyboard contract is the best thing about this panel and was invisible. */}
      {problems.length > 0 && (
        <div className="sb-legend">
          <span className="sb-legend-item">
            <ArrowUpDown size={11} /> <span className="sb-legend-key">browse</span>
          </span>
          <span className="sb-legend-item">
            <CornerDownLeft size={11} /> <span className="sb-legend-key">open</span>
          </span>
          <span className="sb-legend-item">
            <span className="sb-legend-key">esc</span> clear
          </span>
        </div>
      )}
    </div>
  );
}
