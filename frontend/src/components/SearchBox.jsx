import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Search, X, PlayCircle, ChevronRight } from 'lucide-react';
import { useProblemSearch } from '../search/useProblemSearch';
import { matchRanges } from '../search/scoreProblem';
import { normalizeCategory } from '../search/normalizeCategory';

const IS_APPLE = typeof navigator !== 'undefined' && (
  /Mac|iPod|iPhone|iPad/.test(navigator.platform || '') ||
  /Macintosh|Mac OS X/.test(navigator.userAgent || '')
);
const SHORTCUT_HINT = IS_APPLE ? '⌘K' : 'Ctrl K';

function HighlightedText({ text, query }) {
  if (!query || !text) return <span>{text}</span>;
  const ranges = matchRanges(query, text);
  if (ranges.length === 0) return <span>{text}</span>;

  const elements = [];
  let lastIndex = 0;
  ranges.forEach(([start, end], idx) => {
    if (start > lastIndex) {
      elements.push(text.slice(lastIndex, start));
    }
    elements.push(
      <mark key={idx}>
        {text.slice(start, end)}
      </mark>
    );
    lastIndex = end;
  });
  if (lastIndex < text.length) {
    elements.push(text.slice(lastIndex));
  }
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
  const listRef = useRef(null);
  const [isFocused, setIsFocused] = useState(false);


  const {
    query,
    setQuery,
    runnableOnly,
    setRunnableOnly,
    results,
    visible,
    totalMatches,
    globalMatches,
    activeIndex,
    setActiveIndex,
    recents,
    commitRecent,
    isSearching
  } = useProblemSearch({
    problems,
    activeCategory,
    normalizeCategory
  });

  const hasTracedProblems = useMemo(() => {
    return Array.isArray(problems) && problems.some(p => p && p.traced !== undefined);
  }, [problems]);

  // Global ⌘K / Ctrl+K keyboard shortcut
  useEffect(() => {
    const handleGlobalKeyDown = (e) => {
      if ((e.metaKey || e.ctrlKey) && (e.key === 'k' || e.key === 'K')) {
        e.preventDefault();
        if (inputRef.current) {
          inputRef.current.focus();
          inputRef.current.select();
        }
      }
    };
    window.addEventListener('keydown', handleGlobalKeyDown);
    return () => window.removeEventListener('keydown', handleGlobalKeyDown);
  }, []);

  // Keyboard navigation contract inside search input
  const handleKeyDown = (e) => {
    if (e.key === 'ArrowDown') {
      e.preventDefault();
      if (visible.length === 0) return;
      if (activeIndex === -1) {
        setActiveIndex(0);
      } else {
        setActiveIndex((activeIndex + 1) % visible.length);
      }
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      if (visible.length === 0) return;
      if (activeIndex === -1) {
        setActiveIndex(visible.length - 1);
      } else {
        setActiveIndex((activeIndex - 1 + visible.length) % visible.length);
      }
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (visible.length === 0) return;
      const target = activeIndex >= 0 ? visible[activeIndex] : visible[0];
      if (target) {
        if (query.trim()) {
          commitRecent(query);
        }
        onSelectProblem(target.id);
      }
    } else if (e.key === 'Escape') {
      e.preventDefault();
      if (query.length > 0) {
        setQuery('');
      } else {
        inputRef.current?.blur();
      }
    } else if (e.key === 'Home') {
      if (visible.length > 0) {
        e.preventDefault();
        setActiveIndex(0);
      }
    } else if (e.key === 'End') {
      if (visible.length > 0) {
        e.preventDefault();
        setActiveIndex(visible.length - 1);
      }
    }
  };

  // The zero-result copy must name the actual cause. Blaming a query the user
  // never typed ("No algorithm matches \"\"") is reachable in two clicks: enable
  // the runnable filter, then pick any of the categories with no traced problems.
  const scopeLabel = activeCategory ? `${activeCategory}` : 'the catalogue';
  const emptyStateMessage = isSearching
    ? `No algorithm matches "${query}".`
    : runnableOnly
      ? `Nothing in ${scopeLabel} is runnable yet.`
      : `No problems in ${scopeLabel}.`;

  // Scroll active descendant into view
  useEffect(() => {
    if (activeIndex >= 0 && listRef.current && visible[activeIndex]) {
      // getElementById, not querySelector: a problem id containing '.' or '/'
      // would make the interpolated selector a SyntaxError.
      const activeEl = document.getElementById(`problem-opt-${visible[activeIndex].id}`);
      if (activeEl && typeof activeEl.scrollIntoView === 'function') {
        activeEl.scrollIntoView({ block: 'nearest' });
      }
    }
  }, [activeIndex, visible]);

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {/* Search Input Box */}
      <div style={{ position: 'relative', width: '100%' }}>
        <Search
          size={14}
          color="var(--text-muted)"
          style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', pointerEvents: 'none' }}
        />
        <input
          ref={inputRef}
          role="combobox"
          aria-expanded={isSearching}
          aria-controls="problem-results"
          aria-activedescendant={activeIndex >= 0 && visible[activeIndex] ? `problem-opt-${visible[activeIndex].id}` : undefined}
          aria-label="Search algorithms"
          aria-describedby="search-hint"
          type="text"
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleKeyDown}
          onFocus={() => setIsFocused(true)}
          onBlur={() => setIsFocused(false)}
          placeholder={`Search ${problems.length} algorithms…`}
          style={{
            width: '100%',
            padding: '9px 64px 9px 34px',
            fontSize: '0.78rem',
            borderRadius: '8px',
            border: query ? '1px solid var(--accent-violet)' : '1px solid var(--border-default)',
            background: 'rgba(0, 0, 0, 0.35)',
            color: '#ffffff',
            outline: 'none',
            transition: 'all 0.2s ease',
            boxShadow: query ? '0 0 12px rgba(99, 102, 241, 0.25)' : 'none'
          }}
        />

        {/* Clear Button or Shortcut Hint */}
        {query ? (
          <button
            type="button"
            onClick={() => {
              setQuery('');
              inputRef.current?.focus();
            }}
            aria-label="Clear search"
            style={{
              position: 'absolute',
              right: '10px',
              top: '50%',
              transform: 'translateY(-50%)',
              background: 'none',
              border: 'none',
              color: 'var(--text-muted)',
              cursor: 'pointer',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              padding: '2px'
            }}
          >
            <X size={13} />
          </button>
        ) : !isFocused ? (
          <span
            aria-hidden="true"
            style={{
              position: 'absolute',
              right: '10px',
              top: '50%',
              transform: 'translateY(-50%)',
              fontSize: '0.65rem',
              fontWeight: '700',
              color: 'var(--text-muted)',
              background: 'rgba(255, 255, 255, 0.06)',
              border: '1px solid var(--border-default)',
              borderRadius: '4px',
              padding: '2px 5px',
              pointerEvents: 'none'
            }}
          >
            {SHORTCUT_HINT}
          </span>
        ) : null}

        {/* Always present, so aria-describedby never dangles. */}
        <span id="search-hint" className="sr-only">
          {`Press ${SHORTCUT_HINT} to focus. Use up and down arrows to browse results, Enter to open, Escape to clear.`}
        </span>
      </div>

      {/* Runnable Only Filter Toggle (renders only if traced data exists) */}
      {hasTracedProblems && (
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'flex-end', marginTop: '-4px' }}>
          <label style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '0.72rem', color: 'var(--text-secondary)', cursor: 'pointer', userSelect: 'none' }}>
            <input
              type="checkbox"
              checked={runnableOnly}
              onChange={(e) => setRunnableOnly(e.target.checked)}
              style={{ accentColor: 'var(--accent-violet)', cursor: 'pointer' }}
            />
            <span>⚡ Runnable only</span>
          </label>
        </div>
      )}

      {/* Recent Searches Section (hidden if empty) */}
      {recents.length > 0 && (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
          <span style={{ fontSize: '0.68rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
            Recent Searches
          </span>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
            {recents.map((item) => (
              <button
                key={item}
                type="button"
                onClick={() => {
                  setQuery(item);
                  inputRef.current?.focus();
                }}
                style={{
                  fontSize: '0.72rem',
                  color: 'var(--text-secondary)',
                  background: 'rgba(255, 255, 255, 0.035)',
                  border: '1px solid var(--border-default)',
                  borderRadius: '6px',
                  padding: '4px 9px',
                  cursor: 'pointer',
                  transition: 'all 0.15s ease'
                }}
              >
                {item}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Screen Reader Announcement */}
      <div role="status" aria-live="polite" className="sr-only">
        {isSearching ? `${totalMatches} results for ${query}` : ''}
      </div>

      {/* Optional Children (e.g. Category Grid in Sidebar) */}
      {children}

      {/* Problems Results Area */}
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '8px' }}>
        {problems.length === 0 ? (
          <div style={{ textAlign: 'center', padding: '24px 12px', color: 'var(--text-muted)', fontSize: '0.8rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
            <span>Could not reach the backend.</span>
            <button
              type="button"
              className="btn btn-primary"
              onClick={() => (onRetry ? onRetry() : window.location.reload())}
              style={{ fontSize: '0.75rem', padding: '5px 12px' }}
            >
              Retry
            </button>
          </div>
        ) : (
          <>
            {/* Header / Counts */}
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <span style={{ fontSize: '0.68rem', fontWeight: '800', color: 'var(--text-muted)', textTransform: 'uppercase', letterSpacing: '0.8px' }}>
                {!isSearching
                  ? (!activeCategory ? `All problems · ${totalMatches}` : `${activeCategory} · ${totalMatches}`)
                  : (totalMatches > 50
                      ? `Showing 50 of ${totalMatches} — keep typing to narrow`
                      : `${totalMatches} ${totalMatches === 1 ? 'result' : 'results'}`)}
              </span>
            </div>

            {/* Listbox or 0-matches State */}
            {totalMatches > 0 ? (
              <div
                role="listbox"
                id="problem-results"
                ref={listRef}
                tabIndex={-1}
                style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}
              >
                {visible.map((prob, i) => {
                  const isProblemActive = activeProblemId === prob.id;
                  const isSelected = i === activeIndex;
                  return (
                    <div
                      key={prob.id}
                      role="option"
                      id={`problem-opt-${prob.id}`}
                      aria-selected={isSelected}
                      onClick={() => {
                        if (query.trim()) {
                          commitRecent(query);
                        }
                        onSelectProblem(prob.id);
                      }}
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'space-between',
                        padding: '8px 10px',
                        borderRadius: 'var(--radius-sm)',
                        border: '1px solid var(--border-default)',
                        borderLeft: (isProblemActive || isSelected) ? '3px solid var(--accent-violet)' : '1px solid var(--border-default)',
                        background: (isProblemActive || isSelected) ? 'var(--accent-violet-tint)' : 'rgba(255, 255, 255, 0.02)',
                        cursor: 'pointer',
                        transition: 'all 0.15s ease'
                      }}
                    >
                      <div style={{ display: 'flex', alignItems: 'center', gap: '8px', overflow: 'hidden', minWidth: 0 }}>
                        <PlayCircle
                          size={14}
                          color={isProblemActive ? 'var(--accent-violet)' : 'var(--text-muted)'}
                          style={{ flexShrink: 0 }}
                        />
                        <div style={{ overflow: 'hidden', minWidth: 0 }}>
                          <div
                            style={{
                              fontSize: '0.78rem',
                              fontWeight: isProblemActive ? '700' : '500',
                              color: isProblemActive ? 'var(--text-primary)' : 'var(--text-secondary)',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis'
                            }}
                          >
                            <HighlightedText text={prob.title} query={query} />
                          </div>
                          <div
                            style={{
                              fontSize: '0.66rem',
                              color: 'var(--text-muted)',
                              whiteSpace: 'nowrap',
                              overflow: 'hidden',
                              textOverflow: 'ellipsis'
                            }}
                          >
                            {prob.category} · {prob.difficulty}
                          </div>
                        </div>
                      </div>

                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', flexShrink: 0 }}>
                        {prob.traced === true && (
                          <span
                            title="Runnable — executes on your input"
                            aria-label="Runnable"
                            style={{
                              fontSize: '0.65rem',
                              padding: '2px 5px',
                              borderRadius: '4px',
                              background: 'rgba(124, 109, 242, 0.2)',
                              color: 'var(--accent-violet)',
                              border: '1px solid rgba(124, 109, 242, 0.35)',
                              fontWeight: '700',
                              display: 'inline-flex',
                              alignItems: 'center',
                              gap: '2px'
                            }}
                          >
                            <span aria-hidden="true">⚡</span>
                            <span>Runnable</span>
                          </span>
                        )}
                        <ChevronRight size={13} color="var(--text-muted)" />
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <div style={{ textAlign: 'center', padding: '20px 10px', color: 'var(--text-muted)', fontSize: '0.8rem', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
                <span>{emptyStateMessage}</span>
                {globalMatches > 0 && activeCategory && (
                  <button
                    type="button"
                    onClick={() => onSelectCategory(null)}
                    style={{
                      fontSize: '0.75rem',
                      padding: '4px 10px',
                      background: 'var(--accent-violet)',
                      color: '#ffffff',
                      border: 'none',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontWeight: '600'
                    }}
                  >
                    {isSearching
                      ? `Search all ${globalMatches} matches`
                      : `Show all ${globalMatches} across every category`}
                  </button>
                )}
                {runnableOnly && (
                  <button
                    type="button"
                    onClick={() => setRunnableOnly(false)}
                    style={{
                      fontSize: '0.75rem',
                      padding: '4px 10px',
                      background: 'rgba(255, 255, 255, 0.08)',
                      color: 'var(--text-primary)',
                      border: '1px solid var(--border-default)',
                      borderRadius: '6px',
                      cursor: 'pointer',
                      fontWeight: '600'
                    }}
                  >
                    Include problems that aren't runnable yet
                  </button>
                )}
              </div>
            )}
          </>
        )}
      </div>
    </div>
  );
}
