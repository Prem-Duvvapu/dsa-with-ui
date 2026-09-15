import { useEffect } from 'react';
import usePersistentState from './usePersistentState';

const isValidId = (v) => typeof v === 'string' && v.length > 0;

/**
 * Records the most recently viewed problem id, purely for the dashboard's "continue
 * where you left off" link. usePersistentState's own docstring warns that the SELECTED
 * problem must never persist, because the URL already owns that and reading this back
 * into App would fight deep links - this hook only writes here, and only the dashboard
 * (a route with no problem of its own to defer to) ever reads it back.
 *
 * Call with a problemId to record it; call with no argument to read the last one
 * recorded without touching it.
 */
export default function useLastVisited(problemId) {
  const [lastVisitedId, setLastVisitedId] = usePersistentState('lastVisitedProblem', null, isValidId);

  useEffect(() => {
    if (problemId) setLastVisitedId(problemId);
  }, [problemId, setLastVisitedId]);

  return lastVisitedId;
}
