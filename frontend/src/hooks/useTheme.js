import { useCallback, useEffect } from 'react';
import usePersistentState from './usePersistentState';

export const THEMES = ['system', 'light', 'dark'];

const isTheme = (v) => THEMES.includes(v);

/**
 * Owns the `data-theme` stamp on <html>.
 *
 * index.css already defines a complete, contrast-tested light theme in two places: a
 * `@media (prefers-color-scheme: light)` block for a light OS with no explicit choice, and
 * a `:root[data-theme="light"]` block for an explicit one. Nothing ever wrote that
 * attribute, so the light theme could only be reached by changing the OS setting.
 *
 * Three states, not two, matching what the CSS expects: "system" stamps NOTHING and lets
 * the media query decide. Stamping "light" or "dark" on system would defeat the media
 * query and freeze the choice a viewer never made.
 */
export default function useTheme() {
  const [theme, setTheme] = usePersistentState('theme', 'system', isTheme);

  useEffect(() => {
    const root = document.documentElement;
    if (theme === 'system') {
      root.removeAttribute('data-theme');
    } else {
      root.setAttribute('data-theme', theme);
    }
  }, [theme]);

  /** Cycles system -> light -> dark -> system, so one control reaches all three. */
  const cycleTheme = useCallback(() => {
    setTheme((current) => THEMES[(THEMES.indexOf(current) + 1) % THEMES.length]);
  }, [setTheme]);

  return { theme, setTheme, cycleTheme };
}
