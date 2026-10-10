import { useCallback, useMemo, useState } from 'react';
import { defaultInput } from '../input/randomizeInput';

/**
 * The editable input for the current problem, owned by the problem session rather than by
 * the editor component.
 *
 * InputPanel used to hold this in its own useState, seeded once on mount or problem change.
 * Three things followed from that: closing the editor (unmounting it) threw away an edited
 * draft; a spec that arrived after mount was never applied; and the render that switched
 * problems seeded the new editor from the previous problem's still-merged detail.
 *
 * Until someone edits, the draft IS the spec's defaults - derived, not copied - so a late
 * spec simply appears, and nothing a later render does can overwrite an edit. An edit is
 * stored with the problem id it belongs to; a different problem never sees it.
 *
 * Drafts do not survive a refresh or a problem change. Saved presets and the shared-input
 * link are the deliberate ways to keep an input.
 */
export default function useInputDraft(problemId, inputSpec) {
  const [edited, setEdited] = useState({ problemId: null, values: null });
  const hasSpec = Boolean(inputSpec?.fields?.length);
  const defaults = useMemo(() => (hasSpec ? defaultInput(inputSpec) : null), [hasSpec, inputSpec]);

  const own = edited.problemId === problemId && problemId ? edited.values : null;
  const values = own ?? defaults;

  /** Replace the whole draft: a field edit, Randomize, Restore defaults, a loaded preset. */
  const replace = useCallback((next) => {
    setEdited({ problemId, values: next });
  }, [problemId]);

  /** Keep a derived draft when changing specs, without overwriting a same-tick edit. */
  const preserve = useCallback(() => {
    setEdited((current) => current.problemId === problemId && current.values !== null
      ? current : { problemId, values: defaults });
  }, [problemId, defaults]);

  return { values, defaults, isEdited: own !== null, replace, preserve };
}

/** Structural equality for input maps, ignoring key order. */
export function sameInput(a, b) {
  return stableStringify(a) === stableStringify(b);
}

function stableStringify(value) {
  if (Array.isArray(value)) return `[${value.map(stableStringify).join(',')}]`;
  if (value && typeof value === 'object') {
    return `{${Object.keys(value).sort().map((k) => `${JSON.stringify(k)}:${stableStringify(value[k])}`).join(',')}}`;
  }
  return JSON.stringify(value ?? null);
}
