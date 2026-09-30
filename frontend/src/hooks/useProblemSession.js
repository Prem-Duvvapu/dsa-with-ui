import { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import useTrace from './useTrace';
import useInputDraft, { sameInput } from './useInputDraft';
import useShareableView from './useShareableView';
import { defaultInput } from '../input/randomizeInput';

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
    run, steps, settled, runInput, seek, rerunFailure, currentStepIndex, retireSubmission
  } = trace;

  // The detail is keyed to its problem inside useTrace, so this merge cannot pair a new
  // problem's summary with the previous problem's code, spec or complexity.
  const problem = useMemo(
    () => (trace.detail ? { ...catalogEntry, ...trace.detail } : catalogEntry),
    [catalogEntry, trace.detail]
  );
  const inputSpec = problem?.id === problemId ? problem?.inputSpec : null;
  const draft = useInputDraft(problemId, inputSpec);
  const { replace: replaceDraft, defaults: draftDefaults } = draft;

  const [restoring, setRestoring] = useState(false);
  const [linkNotices, setLinkNotices] = useState([]);
  const [shareNote, setShareNote] = useState(null);
  /** Bumped per restoration request, so a same-page link (already settled) still restores. */
  const [restoreRequest, setRestoreRequest] = useState(0);
  /** The one restoration in progress; replaced (not mutated) so each has an identity. */
  const pendingRestore = useRef(null);
  const restoreSeq = useRef(0);
  /** Bumped on every external navigation; a submission only shares for the one it began in. */
  const navigationSeq = useRef(0);
  const mounted = useRef(true);
  useEffect(() => {
    mounted.current = true;
    return () => { mounted.current = false; };
  }, []);
  const problemIdRef = useRef(problemId);
  problemIdRef.current = problemId;
  const inputSpecRef = useRef(inputSpec);
  inputSpecRef.current = inputSpec;
  const runRef = useRef(run);
  runRef.current = run;
  const defaultsRef = useRef(draftDefaults);
  defaultsRef.current = draftDefaults;

  const ownRun = run.problemId === problemId;
  const route = useShareableView({
    problemId,
    stepIndex: currentStepIndex,
    totalSteps: ownRun ? steps.length : 0,
    mirror: !restoring,
    onRestore: (view) => {
      navigationSeq.current += 1;
      // A same-page navigation is a new identity for the session: whatever the old URL
      // started must not land on it (INDEPENDENT_REVIEW_DB8683B.md B1).
      if (view.samePage) retireSubmission();
      // A same-page link without input, while a custom run is on screen, describes the
      // default run - so the default has to run again, not the custom one be kept.
      const resetToDefault = view.samePage && view.input.status === 'absent'
        && runRef.current.problemId === problemId && runRef.current.submittedInput !== null;
      // Every same-page navigation is restored, even one with nothing but a step (or no
      // step at all): its step is applied explicitly rather than left from the old URL.
      const needed = view.samePage || view.step !== null || view.input.status !== 'absent' || resetToDefault;
      pendingRestore.current = needed
        ? { id: ++restoreSeq.current, problemId, ...view, resetToDefault, started: false }
        : null;
      setRestoring(needed);
      setRestoreRequest((n) => n + 1);
      setLinkNotices([]);
      setShareNote(null);
      // Seed the editor with what the link asks for BEFORE it runs, so the fields say what
      // is running and anything the learner types meanwhile is theirs - nothing the
      // response does later writes over the draft.
      const spec = inputSpecRef.current;
      if (view.input.status === 'valid') {
        replaceDraft(spec?.fields?.length
          ? { ...defaultInput(spec), ...specFieldsOf(view.input.value, spec) }
          : view.input.value);
      } else if (resetToDefault && spec?.fields?.length) {
        replaceDraft(defaultInput(spec));
      }
    }
  });
  const { shareInput, dropStep } = route;

  // ── Restore a link, in order: its input, then its step ─────────────────────
  // Waits for this problem's first load to SETTLE - its own default run, or an error - so
  // a rejected link input still has the default run to fall back on, and a step has a run
  // to be restored into. A newer submission or navigation cancels it by replacing
  // `pendingRestore`; every ending, including cancellation, releases only its own claim.
  useEffect(() => {
    const pending = pendingRestore.current;
    if (!pending || pending.problemId !== problemId || pending.started || !settled) return;
    pending.started = true;
    const alive = () => mounted.current && pendingRestore.current === pending
      && problemIdRef.current === pending.problemId;

    (async () => {
      const notices = [];
      let total = runRef.current.problemId === pending.problemId ? runRef.current.steps.length : 0;
      let honoured = true;
      // When a link cannot be honoured, whatever was on screen stays on screen. If that is a
      // custom run, saying "showing the default input" - and dropping `input` from the link -
      // would contradict it (B2): the link is pointed back at the run actually displayed.
      const refuse = (reason) => {
        honoured = false;
        const shown = runRef.current;
        const keptCustom = shown.problemId === pending.problemId && shown.submittedInput !== null;
        shareInput(keptCustom ? shown.submittedInput : null);
        notices.push({ kind: 'input', reason, kept: keptCustom });
      };

      if (pending.input.status === 'invalid') {
        refuse('unreadable');
      } else if (pending.input.status === 'valid' || pending.resetToDefault) {
        const values = pending.input.status === 'valid' ? pending.input.value : defaultsRef.current;
        const outcome = await runInput(values ?? {});
        if (!alive()) return;
        if (outcome.ok) total = outcome.run.steps.length;
        else refuse(outcome.kind);
      }

      // The step, decided for every restoration: the requested one when it exists in the run
      // on screen, otherwise step 1 - applied explicitly, never inherited from the old URL.
      if (pending.step !== null && !honoured) {
        dropStep();
        seek(0);
        notices.push({ kind: 'step-dropped', requested: pending.step + 1 });
      } else if (pending.step !== null && total === 0) {
        notices.push({ kind: 'step-no-run', requested: pending.step + 1 });
      } else if (pending.step !== null && pending.step < total) {
        seek(pending.step);
      } else {
        seek(0);
        if (pending.step !== null) notices.push({ kind: 'step', requested: pending.step + 1, total });
      }

      pendingRestore.current = null;
      setLinkNotices(notices);
      setRestoring(false);
    })();
    // Re-running this effect is harmless: `started` makes it a no-op once claimed.
  }, [problemId, settled, restoreRequest, runInput, seek, shareInput, dropStep]);

  // ── Submitting ─────────────────────────────────────────────────────────────
  /**
   * Runs the given values; on success, and only then, they become the link's input. A
   * submission supersedes any link still being restored, and a page that has been left
   * (or switched to another problem) never shares or navigates on a late answer.
   */
  const submit = useCallback(async (values) => {
    if (pendingRestore.current) {
      pendingRestore.current = null;
      setRestoring(false);
    }
    const forProblem = problemIdRef.current;
    const forNavigation = navigationSeq.current;
    const outcome = await runInput(values);
    if (!mounted.current || problemIdRef.current !== forProblem || navigationSeq.current !== forNavigation) return outcome;
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

  // What the run on screen was given, with the spec's defaults filling fields a submission
  // left out (the server does the same). The default GET runs the defaults; an offline
  // sample's input is unknown, so no "changed" claim can be made against it.
  const committedInput = run.id > 0 && ownRun
    ? (run.submittedInput ? { ...(draftDefaults ?? {}), ...run.submittedInput } : (run.offline ? null : draftDefaults))
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
