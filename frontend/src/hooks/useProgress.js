import { useCallback, useEffect, useState } from 'react';

/**
 * What the learner has actually watched, and what they want to come back to.
 *
 * 431 problems with no sense of position is a wall, and the app had no memory of anything
 * except playback speed and which panels were open. This is the smallest thing that fixes
 * that: two facts per problem, both cheap, neither needing a server or an account.
 *
 * "Watched" means reaching the LAST step, not opening the page. Opening a problem is an
 * accident of clicking; sitting through the trace to the end is the thing worth recording,
 * and it is the only signal available here that means anything.
 *
 * Stored under one key rather than one per problem: 431 keys would be 431 reads on mount,
 * and localStorage is synchronous.
 */

const KEY = 'dsa-ui:progress';

/**
 * A stored shape from a future or corrupted version must not take the app down on mount,
 * and must not be half-applied either. Anything that is not a plain object of plain
 * objects is discarded whole.
 */
function read() {
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
    const clean = {};
    for (const [id, entry] of Object.entries(parsed)) {
      if (!entry || typeof entry !== 'object' || Array.isArray(entry)) continue;
      const watched = entry.watched === true;
      const starred = entry.starred === true;
      if (watched || starred) clean[id] = { watched, starred };
    }
    return clean;
  } catch {
    return {};
  }
}

function write(state) {
  try {
    window.localStorage.setItem(KEY, JSON.stringify(state));
  } catch {
    // Private mode, quota, a hostile extension. The session keeps its in-memory copy and
    // simply does not survive a reload; nothing here is worth failing a render over.
  }
}

export default function useProgress() {
  const [progress, setProgress] = useState(read);

  useEffect(() => { write(progress); }, [progress]);

  /** Idempotent: called on every step change once the end is reached. */
  const markWatched = useCallback((id) => {
    if (!id) return;
    setProgress((prev) => (prev[id]?.watched
      ? prev
      : { ...prev, [id]: { ...prev[id], watched: true, starred: prev[id]?.starred === true } }));
  }, []);

  const toggleStar = useCallback((id) => {
    if (!id) return;
    setProgress((prev) => {
      const next = { ...prev };
      const starred = !(prev[id]?.starred === true);
      const watched = prev[id]?.watched === true;
      if (!starred && !watched) delete next[id];
      else next[id] = { watched, starred };
      return next;
    });
  }, []);

  const clearProgress = useCallback(() => setProgress({}), []);

  const watchedCount = Object.values(progress).filter((e) => e.watched).length;
  const starredCount = Object.values(progress).filter((e) => e.starred).length;

  return { progress, markWatched, toggleStar, clearProgress, watchedCount, starredCount };
}
