import { useCallback, useEffect, useRef, useState } from 'react';

const PREFIX = 'dsa-ui:';

/**
 * Reads a stored value, or returns the fallback when there is nothing usable there.
 *
 * Storage is wrapped in try/catch everywhere in this file on purpose: Safari's private
 * mode throws on write, and a browser extension or a corrupted value should degrade to
 * the default rather than take the whole app down on mount.
 */
function read(key, fallback) {
  try {
    const raw = window.localStorage.getItem(PREFIX + key);
    if (raw === null) return fallback;
    return JSON.parse(raw);
  } catch {
    return fallback;
  }
}

/**
 * useState that survives a reload.
 *
 * Only view preferences belong here - playback speed, which panels are open. Not the
 * selected problem: the URL already owns that, and persisting it would fight deep links.
 *
 * `validate` rejects a stored value that no longer makes sense (an old speed preset, a
 * boolean that became an enum). Without it, a value written by a previous version of the
 * app can pin the UI into a state it can no longer produce, and the user has no way to
 * clear it short of devtools.
 */
export default function usePersistentState(key, defaultValue, validate) {
  const validateRef = useRef(validate);
  validateRef.current = validate;

  const [value, setValue] = useState(() => {
    const stored = read(key, undefined);
    if (stored === undefined) return defaultValue;
    if (validateRef.current && !validateRef.current(stored)) return defaultValue;
    return stored;
  });

  useEffect(() => {
    try {
      window.localStorage.setItem(PREFIX + key, JSON.stringify(value));
    } catch {
      // Storage unavailable or full. The preference simply does not persist; the app
      // keeps working with the in-memory value.
    }
  }, [key, value]);

  return [value, setValue];
}

/** Clears everything this app has stored. Exposed for the shortcut help panel's reset. */
export function clearPersistedState() {
  try {
    const keys = [];
    for (let i = 0; i < window.localStorage.length; i += 1) {
      const k = window.localStorage.key(i);
      if (k && k.startsWith(PREFIX)) keys.push(k);
    }
    keys.forEach((k) => window.localStorage.removeItem(k));
  } catch {
    // Nothing to do - the caller only wants a best-effort reset.
  }
}
