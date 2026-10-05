import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { ChevronLeft, ChevronRight, Code2, Link2, Maximize2, Minimize2, Pencil, RefreshCw, Search, Star } from 'lucide-react';
import LearningHeader from '../components/LearningHeader';
import SiteFooter from '../components/SiteFooter';
import layout from '../components/LearningLayout.module.css';
import CanvasShell from '../components/CanvasShell';
import ErrorBoundary from '../components/ErrorBoundary';
import StepStateSummary from '../components/StepStateSummary';
import InputPanel from '../components/InputPanel';
import InputSummary from '../components/InputSummary';
import CaptureStrip from '../components/CaptureStrip';
import CompareStrip from '../components/CompareStrip';
import ShortcutHelp from '../components/ShortcutHelp';
import CommandPalette from '../components/CommandPalette';
import WelcomeGuide from '../components/WelcomeGuide';
import TourGuide from '../components/TourGuide';
import { useCatalog } from '../catalog/CatalogProvider';
import { CANVAS_BY_DSTYPE } from '../canvas/registry';
import { getCompanions } from '../canvas/companions';
import useProblemSession from '../hooks/useProblemSession';
import usePersistentState from '../hooks/usePersistentState';
import useProgress from '../hooks/useProgress';
import useLastVisited from '../hooks/useLastVisited';
import useStreak from '../hooks/useStreak';
import useTheme from '../hooks/useTheme';
import useKeyboardShortcuts from '../hooks/useKeyboardShortcuts';
import PlaybackBar, { SPEEDS } from './PlaybackBar';
import ViewRail, { VIEWS, panelId, tabId } from './ViewRail';
import { stageFor } from './stageFamily';
import CodeWalkthrough from './CodeWalkthrough';
import SourcePane from './SourcePane';
import StateInspector from './StateInspector';
import { curriculumNeighbours } from './curriculum';
import { TRACE_ERROR_COPY, linkNoticeText, runFailureText } from './sessionCopy';
import styles from './ProblemWorkspace.module.css';

/**
 * The problem page, built to docs/ui-revamp/playground-concept.png.
 *
 * One session (useProblemSession) sits above all three views, so changing view, opening
 * the editor or closing a panel can never lose the run, the step or the draft - the views
 * are presentations of one execution, not three components that each fetch their own.
 * The browser owns vertical scrolling; only the view rail is sticky.
 *
 * Nothing here is invented: narration, the input echo, complexity and constraints are the
 * backend's; an unknown id is "not found"; an unknown dsType renders an explicit state.
 */

// Graph, Tree and DP heroes already show the whole run's shape; see RCA-002 and
// PROMPT-F-visual-fidelity.md. The capture strip (and its comparison) stays off for them.
const CAPTURE_STRIP_REDUNDANT_FOR = new Set(['DpTable', 'Graph', 'Tree']);
// The shell's five-state key (happening now / read / has a value / resolved / untouched)
// explains marks that only the DP table draws on its cells. On every other canvas it was a
// key to symbols that never appear, so it is shown only where it describes the stage.
const SHELL_LEGEND_DRAWN_BY = new Set(['DpTable']);
const DIFFICULTIES = new Set(['Easy', 'Medium', 'Hard']);

function firstSentence(text) {
  const trimmed = text?.trim();
  if (!trimmed) return null;
  const match = trimmed.match(/^.+?[.!?](?=\s|$)/);
  return match ? match[0] : trimmed;
}

/** "isConnected = [[1,1,0],[1,1,0],[0,0,1]]" - an example's input as the problem names it. */
function formatExampleInput(input) {
  return Object.entries(input ?? {}).map(([name, value]) => `${name} = ${JSON.stringify(value)}`).join(', ');
}

function prefersReducedMotion() {
  try {
    return window.matchMedia?.('(prefers-reduced-motion: reduce)').matches === true;
  } catch {
    return false;
  }
}

export default function ProblemWorkspace() {
  const { id: problemId } = useParams();
  const navigate = useNavigate();
  const { problems, loading: catalogLoading, error: catalogError, retry: retryCatalog } = useCatalog();
  const catalogEntry = problems.find((p) => p.id === problemId) || null;

  const [persistedSpeed, setPersistedSpeed] = usePersistentState(
    'speed', 1000, (v) => SPEEDS.some((s) => s.ms === v));
  const session = useProblemSession({ problemId, catalogEntry, initialSpeed: persistedSpeed });
  const {
    problem, steps, currentStep, currentStepIndex, run, loading: traceLoading, pending,
    error: traceError, truncated, rerunFailure, retry, dismissRerunFailure, linkNotices,
    dismissLinkNotices, shareNote, draft, draftChanged, submit, fieldErrors, resolvedInput,
    anchors, setSpeed, speed, pause
  } = session;

  const view = VIEWS.some((v) => v.id === session.view) ? session.view : 'playground';
  const viewLabel = VIEWS.find((v) => v.id === view).label;

  // ── Focus: the Playground with everything optional out of the way ───────
  // Presentation only: the run, step, draft and URL are the session's and do not change.
  // The hidden panels stay MOUNTED (the `hidden` attribute), so an editor mid-edit keeps its
  // state. Entering or leaving pauses; a new problem or another view leaves Focus.
  const [isFocus, setIsFocus] = useState(false);
  const focusTriggerRef = useRef(null);
  const exitFocusRef = useRef(null);
  const focusMove = useRef(null);
  const enterFocus = useCallback(() => { pause(); focusMove.current = 'exit'; setIsFocus(true); }, [pause]);
  const exitFocus = useCallback(() => { pause(); focusMove.current = 'trigger'; setIsFocus(false); }, [pause]);
  useEffect(() => {
    const target = focusMove.current === 'exit' ? exitFocusRef.current : focusMove.current === 'trigger' ? focusTriggerRef.current : null;
    focusMove.current = null;
    target?.focus();
  }, [isFocus]);
  useEffect(() => { setIsFocus(false); }, [problemId]);
  useEffect(() => { if (view !== 'playground') setIsFocus(false); }, [view]);

  const selectView = useCallback((next) => {
    pause();
    setIsFocus(false);
    session.setView(next, 'playground');
  }, [pause, session]);

  // ── Progress: watched means a complete run reached its end ───────────────
  const { progress, markWatched, toggleStar } = useProgress();
  useLastVisited(catalogEntry ? problemId : null);
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const { recordVisit } = useStreak(today);
  useEffect(() => { recordVisit(); }, [recordVisit]);
  const completable = !truncated && !run.offline && run.problemId === problemId;
  useEffect(() => {
    if (completable && steps.length > 0 && currentStepIndex === steps.length - 1) markWatched(problemId);
  }, [completable, currentStepIndex, steps.length, markWatched, problemId]);

  // ── Chrome state ─────────────────────────────────────────────────────────
  const { cycleTheme } = useTheme();
  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);
  const [isTourOpen, setIsTourOpen] = useState(false);
  const [isCompareOpen, setIsCompareOpen] = useState(false);
  const [hasSeenWelcome, setHasSeenWelcome] = usePersistentState('seenWelcome', false, (v) => typeof v === 'boolean');
  // Presentation state that belongs to this problem's session: where the reader left the
  // source, and which Code subview they chose on a narrow screen. A new problem starts fresh.
  const sourceScroll = useRef(null);
  const [codeSubview, setCodeSubview] = useState('source');
  useEffect(() => {
    setIsCompareOpen(false);
  }, [problemId]);

  // The tour runs on every width: it scrolls each target into view and drops the ones a
  // narrow screen does not show (the header links live in the closed phone Menu). It starts
  // from the Playground, out of Focus, so every step has something real to point at.
  const startTour = useCallback(() => {
    setHasSeenWelcome(true);
    setIsHelpOpen(false);
    pause();
    setIsFocus(false);
    session.setView('playground', 'playground');
    setIsTourOpen(true);
  }, [setHasSeenWelcome, session, pause]);

  const changeSpeed = useCallback((ms) => { setSpeed(ms); setPersistedSpeed(ms); }, [setSpeed, setPersistedSpeed]);
  const nudgeSpeed = useCallback((direction) => {
    const order = SPEEDS.map((s) => s.ms);
    const at = order.indexOf(speed);
    const next = Math.min(order.length - 1, Math.max(0, (at === -1 ? 1 : at) + direction));
    changeSpeed(order[next]);
  }, [speed, changeSpeed]);

  useKeyboardShortcuts({
    togglePlay: session.togglePlay, stepNext: session.stepNext, stepPrev: session.stepPrev,
    reset: session.reset, seek: session.seek, stepCount: steps.length, nudgeSpeed,
    isMobile: false, isSidebarOpen: false,
    // "/" searches problems: with no sidebar, that is the switcher.
    setIsSidebarOpen: () => setIsPaletteOpen(true),
    isHelpOpen, setIsHelpOpen, isPaletteOpen, setIsPaletteOpen, hasSeenWelcome, setHasSeenWelcome,
    isFocus, exitFocus
  });

  // Dialogs and a hidden tab pause; nothing resumes by itself.
  useEffect(() => { if (isHelpOpen || isPaletteOpen || isTourOpen) pause(); }, [isHelpOpen, isPaletteOpen, isTourOpen, pause]);

  const notFound = traceError === 'notfound'
    || (!catalogLoading && !catalogError && problems.length > 0 && !catalogEntry);
  const title = problem?.title ?? (catalogLoading ? 'Loading…' : problemId);

  useEffect(() => {
    document.title = notFound
      ? 'Algorithm not found · DSA Visualizer'
      : `${title} · ${viewLabel} · DSA Visualizer`;
  }, [notFound, title, viewLabel]);

  // ── Focus targets ────────────────────────────────────────────────────────
  const stageHeadingRef = useRef(null);
  const inputHeadingRef = useRef(null);
  /** Set by "Fix it in the editor": focus the editor once the Playground has rendered it. */
  const editorFocusPending = useRef(false);

  const openEditor = () => {
    const heading = inputHeadingRef.current;
    if (!heading) return;
    heading.scrollIntoView?.({ block: 'start', behavior: prefersReducedMotion() ? 'auto' : 'smooth' });
    heading.focus({ preventScroll: true });
  };

  // A successful run returns attention to the result, paused at step 1; a rejected one
  // leaves it in the editor, whose error summary takes focus itself.
  const runFromEditor = useCallback(async (values) => {
    const outcome = await submit(values);
    if (outcome.ok) stageHeadingRef.current?.focus();
    return outcome;
  }, [submit]);

  useEffect(() => {
    if (view !== 'playground' || !editorFocusPending.current) return;
    editorFocusPending.current = false;
    openEditor();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [view]);

  const [copyState, setCopyState] = useState('idle');
  const copyTimer = useRef(null);
  useEffect(() => () => clearTimeout(copyTimer.current), []);
  const copyLink = async () => {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopyState('copied');
    } catch {
      setCopyState('failed');
    }
    clearTimeout(copyTimer.current);
    copyTimer.current = setTimeout(() => setCopyState('idle'), 2500);
  };

  // The full statement when one has been written (own words, examples proven against the
  // tracer by StatementContractTest); otherwise the catalogue's short description.
  const paragraphs = Array.isArray(problem?.statement) && problem.statement.length > 0
    ? problem.statement
    : (problem?.description ? [problem.description] : []);
  const examples = Array.isArray(problem?.examples) ? problem.examples : [];
  const sources = Array.isArray(problem?.sources)
    ? problem.sources.filter((source) => typeof source?.url === 'string' && source.url.startsWith('https://'))
    : [];

  const dsType = currentStep?.dsType || problem?.dsType || '';
  const stage = stageFor(dsType);
  const neighbours = curriculumNeighbours(problems, problemId);
  const starred = progress[problemId]?.starred === true;
  const watched = progress[problemId]?.watched === true;
  const hasInputSpec = Boolean(session.inputSpec?.fields?.length);
  const showingOffline = run.offline && steps.length > 0;
  const showCapture = !CAPTURE_STRIP_REDUNDANT_FOR.has(dsType) && steps.length > 0;
  const canCompare = Boolean(problem?.alternateInput) && !CAPTURE_STRIP_REDUNDANT_FOR.has(dsType);

  const navLinks = (
    <>
      <Link to="/" className={styles.headerLink}>All algorithms</Link>
      <button type="button" className={styles.headerLink} onClick={() => setIsPaletteOpen(true)} data-tour="switcher">
        <Search size={16} aria-hidden="true" /> Switch problem <kbd className={styles.kbd}>Ctrl K</kbd>
      </button>
      <button type="button" className={styles.headerLink} onClick={() => setIsHelpOpen(true)}>Help</button>
    </>
  );
  // Phones get the same three actions behind one labelled Menu, never Play/Next/Run.
  const header = (
    <LearningHeader>
      <nav className={styles.headerNav} aria-label="Workspace">{navLinks}</nav>
      <details className={styles.headerMenu}>
        <summary className={styles.headerLink}>Menu</summary>
        <nav className={styles.headerMenuList} aria-label="Workspace">{navLinks}</nav>
      </details>
    </LearningHeader>
  );

  // Dialogs every state of the page shares - the not-found page's Help and Switch problem
  // controls open these too (INDEPENDENT_REVIEW_DB8683B.md S8).
  const overlays = (
    <>
      <ShortcutHelp open={isHelpOpen} onClose={() => setIsHelpOpen(false)}
        onStartTour={startTour}
        onReplayWelcome={() => { setIsHelpOpen(false); setHasSeenWelcome(false); }} />
      <CommandPalette isOpen={isPaletteOpen} onClose={() => setIsPaletteOpen(false)} problems={problems}
        currentProblemId={problemId} catalogLoading={catalogLoading} catalogError={catalogError}
        onSelectProblem={(id) => { setIsPaletteOpen(false); if (id !== problemId) navigate(`/problem/${id}`); }}
        onViewAll={(query) => { setIsPaletteOpen(false); navigate(`/?q=${encodeURIComponent(query)}`); }}
        onCycleTheme={cycleTheme} />
      <WelcomeGuide
        open={!hasSeenWelcome && !catalogLoading && !catalogError && !isTourOpen}
        onDismiss={() => setHasSeenWelcome(true)}
        onShowShortcuts={() => { setHasSeenWelcome(true); setIsHelpOpen(true); }}
        onStartTour={startTour}
        totalProblems={problems.length}
      />
      <TourGuide open={isTourOpen} onClose={() => setIsTourOpen(false)} totalProblems={problems.length} />
    </>
  );

  if (notFound) {
    return (
      <div className={layout.page}>
        {header}
        <main className={styles.notFound} aria-labelledby="not-found-title">
          <h1 id="not-found-title">No algorithm called “{problemId}”</h1>
          <p>This link does not match any problem in the catalogue, so nothing has been run in its place.</p>
          <Link to="/" className={`${layout.button} ${layout.primary}`}>Browse all algorithms</Link>
        </main>
        <SiteFooter />
        {overlays}
      </div>
    );
  }

  // ── The diagram ──────────────────────────────────────────────────────────
  const renderCanvas = () => {
    const Canvas = CANVAS_BY_DSTYPE[dsType];
    if (!Canvas) {
      return <div role="status" className={styles.stageState}>No visualization for {dsType || 'unknown'}</div>;
    }
    const props = { currentStep, step: currentStep, problem, resolvedInput, steps, currentStepIndex };
    const companions = getCompanions(dsType, currentStep, steps);
    if (companions.length === 0) return <Canvas {...props} />;
    return (
      <div className="stage-with-companions">
        <div className="canvas-hero"><Canvas {...props} /></div>
        {companions.map(({ key, Component, props: companionProps }) => <Component key={key} {...companionProps} />)}
      </div>
    );
  };

  const stageBody = traceLoading ? (
    <div role="status" className={styles.stageState}><RefreshCw size={18} className="spin" aria-hidden="true" /> Loading the trace…</div>
  ) : traceError === 'untraced' ? (
    <div className={styles.stageState}>Nothing to draw: this problem is not yet traced.</div>
  ) : TRACE_ERROR_COPY[traceError] && !showingOffline ? (
    <div className={styles.stageState}>Nothing to draw: the trace did not load.</div>
  ) : (
    <ErrorBoundary resetKey={problemId}>{renderCanvas()}</ErrorBoundary>
  );

  const stageNode = (
    <div className={styles.stage} data-family={stage.family} data-tour="canvas" data-audit="stage">
      <StepStateSummary step={currentStep} dsType={dsType} />
      <CanvasShell title={problem?.title} meta={steps.length ? `Step ${currentStepIndex + 1} of ${steps.length}` : null} legend={SHELL_LEGEND_DRAWN_BY.has(dsType)}>
        {stageBody}
      </CanvasShell>
    </div>
  );

  const narration = (
    <div className={styles.narration}>
      <p className={styles.narrationLabel}>
        Current step{steps.length > 0 && <span> · Step {currentStepIndex + 1} of {steps.length}</span>}
      </p>
      <p className={styles.narrationText} role="status" aria-live="polite">
        {currentStep?.description || (traceLoading ? 'Loading…' : 'No trace steps available.')}
      </p>
    </div>
  );

  // Why the run is unavailable belongs to the SESSION, not the diagram: these show in every
  // view, so opening Code or Analysis never hides the reason (INDEPENDENT_REVIEW_DB8683B.md S7).
  const loadFailure = TRACE_ERROR_COPY[traceError] && !showingOffline ? TRACE_ERROR_COPY[traceError] : null;
  const hasFieldErrors = Object.keys(fieldErrors ?? {}).length > 0;
  const notices = (
    <>
      {loadFailure && (
        <div role="alert" className={styles.notice}><span>{loadFailure}</span></div>
      )}
      {traceError === 'untraced' && (
        <div role="status" className={styles.notice}><span>This problem is catalogued but not yet traced.</span></div>
      )}
      {hasFieldErrors && view !== 'playground' && (
        <div role="alert" aria-label="Your input could not run" className={styles.notice}>
          <span>Your input could not run: the server rejected some fields. {steps.length > 0
            ? 'The run shown is still the previous one.' : 'There is no run to show.'}</span>
          <button type="button" className={styles.control} onClick={() => { editorFocusPending.current = true; selectView('playground'); }}>
            Fix it in the editor
          </button>
        </div>
      )}
      {linkNotices.length > 0 && (
        <div role="status" aria-label="Shared link" className={styles.notice}>
          <span>{linkNotices.map(linkNoticeText).join(' ')}</span>
          <button type="button" className={styles.control} onClick={dismissLinkNotices}>Dismiss</button>
        </div>
      )}
      {rerunFailure && (
        <div role="alert" aria-label={steps.length > 0 ? 'New run failed' : 'Run failed'} className={styles.notice}>
          <span>{runFailureText(rerunFailure, steps.length > 0)}</span>
          <button type="button" className={styles.control} onClick={retry} aria-label="Retry this input">Retry</button>
          <button type="button" className={styles.control} onClick={dismissRerunFailure}>Dismiss</button>
        </div>
      )}
      {pending && (
        <div role="status" className={styles.pending}>
          <RefreshCw size={14} className="spin" aria-hidden="true" /> Running your input… the result shown is still the previous run.
        </div>
      )}
      {showingOffline && <div role="status" className={styles.notice}>Live trace unavailable. Showing the checked-in offline sample.</div>}
      {truncated && (
        <div role="status" className={styles.notice}>
          This trace hit the step budget and was cut short — it is not the complete run. Try a smaller input for the full run.
        </div>
      )}
    </>
  );

  const inputUsed = (
    <div className={styles.inputUsed} data-tour="input-summary">
      <InputSummary resolvedInput={resolvedInput} label="Input used" comfortable />
      {draftChanged && <span className={styles.badge}>Changes not run</span>}
      <span className={styles.spacer} />
      <button type="button" className={styles.quiet} onClick={copyLink}>
        <Link2 size={16} aria-hidden="true" /> {copyState === 'copied' ? 'Link copied' : copyState === 'failed' ? 'Copy failed' : 'Copy link'}
      </button>
      {copyState !== 'idle' && (
        <span role="status" className="sr-only">
          {copyState === 'copied' ? 'Link to this run and step copied.' : 'Could not copy the link. Copy the address bar instead.'}
        </span>
      )}
      {shareNote === 'too-long' && <span className={styles.meta}>This input is too large for a link; the link opens the default input.</span>}
    </div>
  );

  return (
    <div className={layout.page}>
      <a className={layout.skip} href="#workspace-view">Skip to the visualization</a>
      <div hidden={isFocus}>{header}</div>

      <div className={styles.width} hidden={isFocus}>
        {catalogError && (
          <div role="alert" className={`${styles.notice} ${styles.catalogNotice}`}>
            <span>{catalogError} Switching problems and curriculum navigation are limited until it loads.</span>
            <button type="button" className={styles.control} onClick={retryCatalog}>Retry loading the catalogue</button>
          </div>
        )}
        <header className={styles.context}>
          <p className={styles.breadcrumb}>
            <Link to={`/?category=${encodeURIComponent(problem?.category ?? '')}`}>{problem?.category ?? '…'}</Link>
            <span aria-hidden="true"> / </span>{stage.label}
          </p>
          <div className={styles.titleRow}>
            <h1 className={styles.title}>{title}</h1>
            <div className={styles.titleMeta}>
              {DIFFICULTIES.has(problem?.difficulty) && <span className={styles.difficulty}>{problem.difficulty}</span>}
              {watched && <span className={styles.meta}>Watched</span>}
              <button
                type="button"
                className={styles.star}
                aria-pressed={starred}
                aria-label={starred ? 'Remove from starred' : 'Star this problem'}
                onClick={() => toggleStar(problemId)}
              >
                <Star size={20} fill={starred ? 'currentColor' : 'none'} aria-hidden="true" />
              </button>
            </div>
          </div>
          {firstSentence(paragraphs[0]) && <p className={styles.summary}>{firstSentence(paragraphs[0])}</p>}

          <div className={styles.contextRow}>
            {(paragraphs.length > 0 || problem?.constraints?.length > 0) && (
              <details className={styles.statement}>
                <summary>Problem &amp; examples</summary>
                <div className={styles.statementBody}>
                  {paragraphs.map((text) => <p key={text}>{text}</p>)}
                  {examples.map((example, index) => (
                    <figure key={index} className={styles.example}>
                      <figcaption>Example {index + 1}</figcaption>
                      <p><strong>Input:</strong> <code>{formatExampleInput(example.input)}</code></p>
                      <p><strong>Output:</strong> <code>{example.output}</code></p>
                      {example.explanation && <p><strong>Explanation:</strong> {example.explanation}</p>}
                    </figure>
                  ))}
                  {problem?.constraints?.length > 0 && (
                    <>
                      <p className={styles.constraintsLabel}>Problem constraints <span>(the original problem's, not this visualizer's input limits)</span></p>
                      <ul className={styles.constraints}>{problem.constraints.map((c) => <li key={c}>{c}</li>)}</ul>
                    </>
                  )}
                  {sources.length > 0 && (
                    <p className={styles.sources}>
                      Original problem:{' '}
                      {sources.map((source, index) => (
                        <React.Fragment key={source.url}>
                          {index > 0 && ' · '}
                          <a href={source.url} target="_blank" rel="noopener noreferrer">{source.label}</a>
                        </React.Fragment>
                      ))}
                    </p>
                  )}
                </div>
              </details>
            )}
            {neighbours && (
              <nav className={styles.curriculum} aria-label="Position in curriculum section">
                <span className={styles.meta}>{neighbours.section} · {neighbours.position} of {neighbours.total}</span>
                <button type="button" className={styles.control} disabled={!neighbours.previous}
                  onClick={() => navigate(`/problem/${neighbours.previous.id}`)}
                  aria-label={neighbours.previous ? `Previous: ${neighbours.previous.title}` : 'No previous problem'}>
                  <ChevronLeft size={16} aria-hidden="true" /> Previous
                </button>
                <button type="button" className={styles.control} disabled={!neighbours.next}
                  onClick={() => navigate(`/problem/${neighbours.next.id}`)}
                  aria-label={neighbours.next ? `Next: ${neighbours.next.title}` : 'No next problem'}>
                  Next <ChevronRight size={16} aria-hidden="true" />
                </button>
              </nav>
            )}
          </div>
        </header>
      </div>

      <div data-tour="view-rail" className={styles.railWrap} hidden={isFocus}><ViewRail view={view} onSelect={selectView} /></div>

      {/* <main> keeps its landmark role; the tab panel is the element the tabs point at. */}
      <main id="workspace-view" className={`${styles.width}${isFocus ? ` ${styles.focusMain}` : ''}`} tabIndex={-1} data-focus={isFocus || undefined}>
        <div id={panelId} className={styles.panel} role="tabpanel" aria-labelledby={tabId(view)} tabIndex={-1}>
          {view === 'playground' && (
            <>
              <section className={styles.card} aria-labelledby="stage-title">
                <div className={styles.cardHead}>
                  {isFocus ? (
                    <h2 id="stage-title" ref={stageHeadingRef} tabIndex={-1} className={styles.cardTitle}>
                      {title} <span className={styles.focusFamily}>· {stage.label}</span>
                    </h2>
                  ) : (
                    <h2 id="stage-title" ref={stageHeadingRef} tabIndex={-1} className={styles.cardTitle}>{stage.label}</h2>
                  )}
                  <div className={styles.cardActions}>
                    {hasInputSpec && !isFocus && (
                      <button type="button" className={styles.control} onClick={openEditor}>
                        <Pencil size={16} aria-hidden="true" /> Edit input
                      </button>
                    )}
                    {!isFocus && (
                      <button type="button" className={styles.control} onClick={() => selectView('code')}>
                        <Code2 size={16} aria-hidden="true" /> Show code
                      </button>
                    )}
                    {isFocus ? (
                      <button type="button" ref={exitFocusRef} className={styles.control} onClick={exitFocus}>
                        <Minimize2 size={16} aria-hidden="true" /> Exit focus
                      </button>
                    ) : (
                      <button type="button" ref={focusTriggerRef} className={styles.control} onClick={enterFocus}
                        aria-describedby="focus-hint" data-tour="focus">
                        <Maximize2 size={16} aria-hidden="true" /> Focus
                      </button>
                    )}
                    <span id="focus-hint" className="sr-only">Hides everything but the diagram, its narration and the controls. Esc returns.</span>
                  </div>
                </div>
                {notices}
                {stageNode}
                {narration}
                <PlaybackBar session={session} onSpeedChange={changeSpeed} />
              </section>

              <div hidden={isFocus}>{inputUsed}</div>

              {hasInputSpec && (
                <section className={styles.card} aria-labelledby="try-input-title" id="try-input" data-tour="input-editor" hidden={isFocus}>
                  <h2 id="try-input-title" ref={inputHeadingRef} tabIndex={-1} className={styles.cardTitle}>Try your own input</h2>
                  <p className={styles.hint}>Change the input, then run it. The result above stays until your run succeeds.</p>
                  <InputPanel
                    variant="section"
                    problemId={problemId}
                    inputSpec={session.inputSpec}
                    alternateInput={problem?.alternateInput}
                    fieldErrors={fieldErrors}
                    running={traceLoading || pending}
                    values={draft.values}
                    onChange={draft.replace}
                    onRun={runFromEditor}
                  />
                </section>
              )}

              {(showCapture || canCompare) && (
                <section className={styles.historyRow} aria-label="Execution history and comparison" hidden={isFocus}>
                  {showCapture && (
                    <details className={styles.history} data-tour="capture-strip">
                      <summary>Execution history <span>step {currentStepIndex + 1} of {steps.length}</span></summary>
                      <CaptureStrip steps={steps} current={currentStepIndex} dsType={dsType} onSeek={session.seek} resolvedInput={resolvedInput} />
                    </details>
                  )}
                  {canCompare && (
                    <button type="button" className={styles.control} aria-expanded={isCompareOpen} onClick={() => setIsCompareOpen((v) => !v)}>
                      {isCompareOpen ? 'Hide comparison' : 'Compare other case'}
                    </button>
                  )}
                </section>
              )}
              {canCompare && isCompareOpen && (
                <section className={styles.card} aria-label="Comparison with the other case" hidden={isFocus}>
                  <CompareStrip key={problemId} problemId={problemId} dsType={dsType} alternateInput={problem.alternateInput} />
                </section>
              )}
            </>
          )}

          {view === 'code' && (
            <section className={styles.card} aria-labelledby="code-title">
              <div className={styles.cardHead}>
                <h2 id="code-title" className={styles.cardTitle}>Code walkthrough</h2>
                <button type="button" className={styles.control} onClick={() => selectView('playground')}>Back to visualization</button>
              </div>
              {notices}
              <CodeWalkthrough
                diagram={stageNode}
                source={<SourcePane key={problemId} problemId={problemId} problem={problem} currentStep={currentStep} anchors={anchors} steps={steps} scrollMemory={sourceScroll} />}
                subview={codeSubview}
                onSubview={(next) => { pause(); setCodeSubview(next); }}
              />
              {narration}
              <PlaybackBar session={session} onSpeedChange={changeSpeed} />
            </section>
          )}

          {view === 'analysis' && (
            <section className={styles.card} aria-labelledby="analysis-title">
              <div className={styles.cardHead}>
                <h2 id="analysis-title" className={styles.cardTitle}>Analysis</h2>
                <button type="button" className={styles.control} onClick={() => selectView('playground')}>Back to visualization</button>
              </div>
              {notices}
              {narration}
              <PlaybackBar session={session} onSpeedChange={changeSpeed} compact />
              <StateInspector step={currentStep} steps={steps} problem={problem} dsType={dsType} />
            </section>
          )}
        </div>
      </main>

      <div hidden={isFocus}><SiteFooter /></div>

      {overlays}
    </div>
  );
}
