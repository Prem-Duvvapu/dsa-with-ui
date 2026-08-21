import { useState, useMemo, useEffect, useCallback } from 'react';
import { searchProblems } from './scoreProblem';

const STORAGE_KEY = 'dsa:recentSearches';

function loadRecents() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return [];
    const parsed = JSON.parse(raw);
    if (Array.isArray(parsed)) {
      return parsed
        .filter(item => typeof item === 'string' && item.trim().length > 0)
        .slice(0, 5);
    }
    return [];
  } catch {
    return [];
  }
}

function saveRecents(items) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
  } catch {
    // Graceful degradation when localStorage is unavailable
  }
}

export function useProblemSearch({
  problems = [],
  activeCategory = null,
  normalizeCategory = (c) => c
} = {}) {
  const [query, setQuery] = useState('');
  const [runnableOnly, setRunnableOnly] = useState(false);
  const [activeIndex, setActiveIndex] = useState(-1);
  const [recents, setRecents] = useState(loadRecents);

  const isSearching = Boolean(query && query.trim().length > 0);

  // Reset activeIndex on query, runnableOnly, or activeCategory change
  useEffect(() => {
    setActiveIndex(-1);
  }, [query, runnableOnly, activeCategory]);

  // Compute ranked & filtered results
  // Filter order: score -> filter by runnableOnly -> filter by activeCategory -> sort
  // searchProblems already performs scoring and deterministic sorting
  const { results, globalMatches } = useMemo(() => {
    let list = isSearching ? searchProblems(query, problems) : problems;

    if (runnableOnly) {
      list = list.filter(p => Boolean(p && p.traced));
    }

    const globalCount = list.length;

    if (activeCategory) {
      list = list.filter(p => normalizeCategory(p.category) === activeCategory);
    }

    return {
      results: list,
      globalMatches: globalCount
    };
  }, [problems, query, isSearching, runnableOnly, activeCategory, normalizeCategory]);

  const visible = useMemo(() => results.slice(0, 50), [results]);
  const totalMatches = results.length;

  const commitRecent = useCallback((q) => {
    if (!q || typeof q !== 'string') return;
    const trimmed = q.trim();
    if (!trimmed) return;

    setRecents((prev) => {
      const filtered = prev.filter(item => item.toLowerCase() !== trimmed.toLowerCase());
      const next = [trimmed, ...filtered].slice(0, 5);
      saveRecents(next);
      return next;
    });
  }, []);

  const removeRecent = useCallback((q) => {
    if (!q || typeof q !== 'string') return;
    const trimmed = q.trim();
    setRecents((prev) => {
      const next = prev.filter(item => item.toLowerCase() !== trimmed.toLowerCase());
      saveRecents(next);
      return next;
    });
  }, []);

  return {
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
    removeRecent,
    isSearching
  };
}
