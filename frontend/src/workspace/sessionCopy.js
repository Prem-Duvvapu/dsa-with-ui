/**
 * The words the workspace uses for run and link status. Shared by the legacy shell and the
 * new workspace so the two cannot describe the same state differently.
 */

export const TRACE_ERROR_COPY = Object.freeze({
  fetch: 'Could not load this trace from the backend.',
  empty: 'The backend returned an empty trace.',
  malformed: 'The backend returned a malformed trace.'
});

/** Why a submitted run failed, for the label beside the result it did not replace. */
export const RUN_FAILURE_COPY = Object.freeze({
  fetch: 'the backend could not be reached',
  malformed: 'the backend returned a malformed trace',
  empty: 'the backend returned an empty trace',
  untraced: 'this problem has no execution trace',
  'rate-limited': 'too many runs in a short time; wait a moment and retry'
});

/** What a shared link asked for that could not be honoured, in the learner's terms. */
export function linkNoticeText(notice) {
  // Never infer a default execution merely from the absence of a custom submission.
  const outcome = {
    custom: notice.shared
      ? 'The previous run is still shown, and the link now points to it.'
      : 'The previous run is still shown, but its input cannot fit in a share link.',
    default: 'Showing the default input.',
    offline: 'Showing the offline sample; its input is unknown.',
    none: 'There is no run to show.'
  }[notice.available] ?? 'There is no run to show.';
  if (notice.kind === 'input' && notice.reason === 'unreadable') {
    return `The input in this link could not be read, so it was not run. ${outcome}`;
  }
  if (notice.kind === 'step-no-run') {
    return `This link pointed to step ${notice.requested}, but there is no run to show it in.`;
  }
  if (notice.kind === 'input') {
    return notice.reason === 'invalid'
      ? `The input in this link could not be run: the server rejected it. ${outcome} The rejected values are in the editor.`
      : `The input in this link could not be run: ${RUN_FAILURE_COPY[notice.reason] ?? 'the run failed'}. ${outcome}`;
  }
  if (notice.kind === 'step-dropped') {
    return `Its step ${notice.requested} belonged to that run, so playback starts at step 1.`;
  }
  return `This link pointed to step ${notice.requested}, but this run has ${notice.total} steps. Showing step 1.`;
}

export function runFailureText(failure, hasPreviousRun) {
  const reason = RUN_FAILURE_COPY[failure.kind] ?? 'the run did not complete';
  return hasPreviousRun
    ? `Previous result shown. The new run failed: ${reason}.`
    : `The run failed: ${reason}.`;
}
