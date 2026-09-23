import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { DEFAULT_FALLBACK_PROBLEMS } from './offlineSamples';

const CatalogContext = createContext(null);

export function uniqueProblems(problems) {
  const ids = new Set();
  return problems.filter(problem => {
    if (!problem || typeof problem.id !== 'string' || !problem.id.trim() || ids.has(problem.id)) return false;
    ids.add(problem.id);
    return true;
  });
}

function useCatalogResource(enabled) {
  const [state, setState] = useState({ problems: DEFAULT_FALLBACK_PROBLEMS, loading: true, error: null });
  const [attempt, setAttempt] = useState(0);
  const retry = useCallback(() => setAttempt(value => value + 1), []);
  useEffect(() => {
    if (!enabled) return;
    const controller = new AbortController();
    let active = true;
    setState(previous => ({ ...previous, loading: true, error: null }));
    (async () => {
      try {
        const response = await fetch('/api/problems', { signal: controller.signal });
        if (!response.ok) throw new Error('Catalogue unavailable');
        const body = await response.json();
        if (!Array.isArray(body)) throw new Error('Invalid catalogue');
        if (active) setState({ problems: uniqueProblems(body), loading: false, error: null });
      } catch (error) {
        if (active && error.name !== 'AbortError') {
          setState(previous => ({ ...previous, loading: false, error: 'Could not reach the backend. Showing the last available catalogue or a small offline sample.' }));
        }
      }
    })();
    return () => { active = false; controller.abort(); };
  }, [attempt, enabled]);
  return { ...state, retry };
}

export function CatalogProvider({ children }) {
  const catalog = useCatalogResource(true);
  return <CatalogContext.Provider value={catalog}>{children}</CatalogContext.Provider>;
}

export function useCatalog() {
  const shared = useContext(CatalogContext);
  // Standalone component consumers (including existing integration fixtures) remain usable.
  const local = useCatalogResource(!shared);
  return shared || local;
}
