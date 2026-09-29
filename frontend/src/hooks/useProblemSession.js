import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import useTrace from './useTrace';
import useInputDraft, { sameInput } from './useInputDraft';
import useShareableView from './useShareableView';

/**
 * One problem's learning session: the trace and its playback, the editable draft, what the
 * link shares, and restoring a link someone opened. It lives above every panel, so the
 * editor closing, a view changing or a panel remounting cannot lose any of it.
 *
 * The rules this exists to hold (IMPLEMENTATION_HANDOFF.md §6):
 *
 * - Editing changes the draft only. Only a SUCCESSFUL run changes the run on screen, the
 *   "Running on" echo and the shared link - together.
 * - A rejected or failed run keeps the previous result and says so; it never becomes the
 *   link's identity.
 * - A link's input runs before its step is restored, and nothing mirrors into the URL
 *   until that has finished. A link that cannot be honoured says why.
 */

/** The spec's own fields only: a hand-edited link may carry keys no editor can show. */
function specFieldsOf(values, inputSpec) {
  const names = new Set((inputSpec?.fields ?? []).map((f) => f.name));
  return Object.fromEntries(Object.entries(values ?? {}).filter(([k]) => names.has(k)));
}

export default function useProblemSession({ problemId, catalogEntry, initialSpeed }) {
  const trace = useTrace(problemId, catalogEntry, { initialSpeed });
  const {
    run, steps, settled, runInput, seek, rerunFailure, currentStepIndex
  } = trace;

  // The detail is keyed to its problem inside useTrace, so this merge cannot pair a new
  // problem's summary with the previous problem's code, spec or complexity.
  const problem = useMemo(
    () => (trace.detail ? { ...catalogEntry, ...trace.detail } : catalogEntry),
    [catalogEntry, trace.detail]
  );
  const inputSpec = problem?.id === problemId ? problem?.inputSpec : null;
  const draft = useInputDraft(problemId, inputSpec);
  const { replace: replaceDraft } = draft;

  const [restoring, setRestoring] = useState(false);
  const [linkNotices, setLinkNotices] = useState([]);
  const [shareNote, setShareNote] = useState(null);
  const pendingRestore = useRef(null);
  const problemIdRef = useRef(problemId);
  problemIdRef.current = problemId;
  const inputSpecRef = useRef(inputSpec);
  inputSpecRef.current = inputSpec;

  const ownRun = run.problemId === problemId;
  const route = useShareableView({
    problemId,
    stepIndex: currentStepIndex,
    totalSteps: ownRun ? steps.length : 0,
    mirror: !restoring,
    onRestore: (view) => {
      const needed = view.step !== null || Boolean(view.input);
      pendingRestore.current = needed ? { problemId, ...view, started: false } : null;
      setRestoring(needed);
      setLinkNotices([]);
      setShareNote(null);
    }
  });
  const { shareInput } = route;

  // ── Restore a shared link, in order: its input, then its step ──────────────
  // Waits for this problem's first load to SETTLE - its own default run, or an error - so
  // a rejected link input still has the default run to fall back on, and a step has a run
  // to be restored into.
  useEffect(() => {
    const pending = pendingRestore.current;
    if (!pending || pending.problemId !== problemId || pending.started || !settled) return;
    pending.started = true;

    (async () => {
      const notices = [];
      let total = ownRun ? steps.length : 0;

      if (pending.input) {
        const outcome = await runInput(pending.input);
        if (problemIdRef.current !== pending.problemId || outcome.kind === 'superseded') return;
        if (outcome.ok) {
          total = outcome.run.steps.length;
          replaceDraft(outcome.run.submittedInput);
        } else {
          // Show the learner exactly what was rejected, beside the server's field errors,
          // and stop the link from claiming an input the screen is not running.
          replaceDraft({ ...draft.defaults, ...specFieldsOf(pending.input, inputSpecRef.current) });
          shareInput(null);
          notices.push({ kind: 'input', reason: outcome.kind });
        }
      }

      // A step belongs to the run the link described. When that run could not happen, the
      // default run's step N is a different moment of a different execution, so playback
      // starts at the beginning instead and the notice above says why.
      const inputHonoured = !pending.input || notices.length === 0;
      if (pending.step !== null && total > 0 && inputHonoured) {
        if (pending.step < total) seek(pending.step);
        else notices.push({ kind: 'step', requested: pending.step + 1, total });
      } else if (pending.step !== null && !inputHonoured) {
        notices.push({ kind: 'step-dropped', requested: pending.step + 1 });
      }

      if (pendingRestore.current === pending) pendingRestore.current = null;
      setLinkNotices(notices);
      setRestoring(false);
    })();
    // Re-running this effect is harmless: `started` makes it a no-op once claimed.
  }, [problemId, settled, ownRun, steps.length, runInput, seek, replaceDraft, shareInput, draft.defaults]);

  // ── Submitting ─────────────────────────────────────────────────────────────
  /** Runs the given values; on success, and only then, they become the link's input. */
  const submit = useCallback(async (values) => {
    const outcome = await runInput(values);
    if (outcome.ok) {
      setShareNote(shareInput(outcome.run.submittedInput) ? null : 'too-long');
      setLinkNotices([]);
    }
    return outcome;
  }, [runInput, shareInput]);

  /** Retries exactly the submission that failed, not whatever the draft holds now. */
  const retry = useCallback(
    () => (rerunFailure ? submit(rerunFailure.input) : Promise.resolve({ ok: false, kind: 'superseded' })),
    [rerunFailure, submit]
  );

  const dismissLinkNotices = useCallback(() => setLinkNotices([]), []);

  // What the run on screen was given. The default GET runs the spec's defaults; an offline
  // sample's input is unknown, so no "changed" claim can be made against it.
  const committedInput = run.id > 0 && ownRun
    ? (run.submittedInput ?? (run.offline ? null : draft.defaults))
    : null;
  const draftChanged = Boolean(draft.values && committedInput && !sameInput(draft.values, committedInput));

  return {
    ...trace,
    problem,
    inputSpec,
    draft,
    draftChanged,
    submit,
    retry,
    restoring,
    linkNotices,
    dismissLinkNotices,
    shareNote,
    view: route.view,
    setView: route.setView
  };
}
