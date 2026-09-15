import { useCallback, useEffect, useState } from 'react';

/**
 * Inputs a learner built by hand and wants back.
 *
 * A field editor already lets someone construct a tricky 9-node graph or a carefully
 * chosen 20-element array - and until now, navigating away discarded it. Watching a trace
 * once and never returning to the exact case that was interesting is a retention leak this
 * hook exists to close: name the input, and it survives the reload useProgress's own
 * "watched" flag already implies is coming back.
 *
 * Scoped per problem, all under one localStorage key rather than one key per problem, for
 * the same reason useProgress does it that way: 431 possible keys would be 431 synchronous
 * reads if every problem were touched, and one JSON blob is one.
 */

const KEY = 'dsa-ui:presets';
const MAX_PER_PROBLEM = 8;

/**
 * A stored shape from a future or corrupted version must not take the app down on mount.
 * Anything that is not { problemId: [ {id, name, values, savedAt}, ... ] } is discarded -
 * per malformed problem entry, not the whole store, so one bad key does not cost every
 * other problem's presets.
 */
function read() {
  try {
    const raw = window.localStorage.getItem(KEY);
    if (!raw) return {};
    const parsed = JSON.parse(raw);
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) return {};
    const clean = {};
    for (const [problemId, list] of Object.entries(parsed)) {
      if (!Array.isArray(list)) continue;
      const entries = list.filter((p) => p
        && typeof p.id === 'string'
        && typeof p.name === 'string' && p.name.trim().length > 0
        && p.values && typeof p.values === 'object' && !Array.isArray(p.values)
        && typeof p.savedAt === 'number');
      if (entries.length > 0) clean[problemId] = entries;
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
    // Private mode, quota, a hostile extension. The saved input simply does not survive
    // this session; nothing here is worth failing a render over.
  }
}

export default function useInputPresets(problemId) {
  const [all, setAll] = useState(read);

  useEffect(() => { write(all); }, [all]);

  const savePreset = useCallback((name, values) => {
    const trimmed = typeof name === 'string' ? name.trim() : '';
    if (!problemId || !trimmed) return;
    setAll((prev) => {
      const existing = prev[problemId] ?? [];
      const entry = { id: `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`, name: trimmed, values, savedAt: Date.now() };
      // Oldest first, so the cap evicts the least recently saved rather than the least
      // recently used - simpler to reason about, and the learner just proved this new one
      // is the one they want kept.
      const kept = [...existing, entry].slice(-MAX_PER_PROBLEM);
      return { ...prev, [problemId]: kept };
    });
  }, [problemId]);

  const removePreset = useCallback((id) => {
    if (!problemId) return;
    setAll((prev) => {
      const remaining = (prev[problemId] ?? []).filter((p) => p.id !== id);
      const next = { ...prev };
      if (remaining.length > 0) next[problemId] = remaining;
      else delete next[problemId];
      return next;
    });
  }, [problemId]);

  return { presets: all[problemId] ?? [], savePreset, removePreset };
}
