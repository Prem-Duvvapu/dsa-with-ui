import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from './components/Header';
import Breadcrumb from './components/Breadcrumb';
import SectionNav from './components/SectionNav';
import ProblemStatement from './components/ProblemStatement';
import InputSummary from './components/InputSummary';
import ShortcutHelp from './components/ShortcutHelp';
import CommandPalette from './components/CommandPalette';
import WelcomeGuide from './components/WelcomeGuide';
import TourGuide from './components/TourGuide';
import StepStateSummary from './components/StepStateSummary';
import usePersistentState from './hooks/usePersistentState';
import useShareableView from './hooks/useShareableView';
import useProgress from './hooks/useProgress';
import useLastVisited from './hooks/useLastVisited';
import useStreak from './hooks/useStreak';
import useLayoutPreferences from './hooks/useLayoutPreferences';
import useKeyboardShortcuts from './hooks/useKeyboardShortcuts';
import useTheme from './hooks/useTheme';
import useFocusTrap from './hooks/useFocusTrap';
import Sidebar from './components/Sidebar';
import CanvasShell from './components/CanvasShell';
import ErrorBoundary from './components/ErrorBoundary';
import CaptureStrip from './components/CaptureStrip';
import CompareStrip from './components/CompareStrip';
import CodeViewer from './components/CodeViewer';
import MemoryComplexityCard from './components/MemoryComplexityCard';
import InputPanel from './components/InputPanel';
import Controls from './components/Controls';
import LiveTraceTicker from './components/LiveTraceTicker';
import useTrace from './hooks/useTrace';
import { CANVAS_BY_DSTYPE } from './canvas/registry';
import { getCompanions } from './canvas/companions';
import { RefreshCw, ChevronLeft, ChevronRight, ChevronUp, ChevronDown } from 'lucide-react';
import styles from './App.module.css';

const DEFAULT_FALLBACK_PROBLEMS = [
  {
    id: 'two-sum',
    title: 'Two Sum',
    category: 'Arrays & Hashing',
    difficulty: 'Easy',
    dsType: 'Array',
    defaultArray: [
      { value: 2, state: 'default' },
      { value: 7, state: 'current' },
      { value: 11, state: 'target' },
      { value: 15, state: 'visited' }
    ],
    javaCode: `public int[] twoSum(int[] nums, int target) {
    Map<Integer, Integer> map = new HashMap<>();
    for (int i = 0; i < nums.length; i++) {
        int complement = target - nums[i];
        if (map.containsKey(complement)) {
            return new int[] { map.get(complement), i };
        }
        map.put(nums[i], i);
    }
    return new int[0];
}`,
    complexity: {
      timeComplexity: 'O(N)',
      spaceComplexity: 'O(N)',
      timeExplanation: 'Single pass through array using Hash Map lookups.',
      spaceExplanation: 'Hash map stores up to N element complement mappings.'
    },
    executionSteps: [
      {
        stepNumber: 1,
        activeLine: 3,
        description: 'Initialize empty HashMap. Iterate index i = 0, current value = 2.',
        arrayState: [
          { value: 2, state: 'current' },
          { value: 7, state: 'default' },
          { value: 11, state: 'default' },
          { value: 15, state: 'default' }
        ],
        variables: { i: 0, val: 2, target: 9, complement: 7 }
      },
      {
        stepNumber: 2,
        activeLine: 7,
        description: 'Iterate index i = 1, current value = 7. Complement 9 - 7 = 2 exists in map at index 0!',
        arrayState: [
          { value: 2, state: 'done' },
          { value: 7, state: 'target' },
          { value: 11, state: 'default' },
          { value: 15, state: 'default' }
        ],
        variables: { i: 1, val: 7, target: 9, complement: 2, result: '[0, 1]' }
      }
    ]
  },
  {
    id: 'longest-substring-without-repeating',
    title: 'Longest Substring Without Repeating Characters',
    category: 'Sliding Window',
    difficulty: 'Medium',
    dsType: 'Array',
    defaultArray: [
      { value: 97, state: 'visited' },
      { value: 98, state: 'current' },
      { value: 99, state: 'target' },
      { value: 97, state: 'default' }
    ],
    javaCode: `public int lengthOfLongestSubstring(String s) {
    HashMap<Character, Integer> map = new HashMap<>();
    int left = 0, right = 0, maxLen = 0;
    while (right < s.length()) {
        char ch = s.charAt(right);
        if (map.containsKey(ch)) {
            left = Math.max(map.get(ch) + 1, left);
        }
        map.put(ch, right);
        maxLen = Math.max(maxLen, right - left + 1);
        right++;
    }
    return maxLen;
}`,
    complexity: {
      timeComplexity: 'O(N)',
      spaceComplexity: 'O(min(m, n))',
      timeExplanation: 'Single pass sliding window pointers right and left.',
      spaceExplanation: 'Hash map stores unique characters bounded by alphabet size.'
    },
    executionSteps: [
      {
        stepNumber: 1,
        activeLine: 4,
        description: 'Input string s = "abcabcbb". Initialize sliding window pointers left = 0, right = 0, maxLen = 0.',
        variables: { left: 0, right: 0, maxLen: 0, s: '"abcabcbb"' }
      }
    ]
  }
];

const TRACE_ERROR_COPY = Object.freeze({
  fetch: 'Could not load this trace from the backend.',
  empty: 'The backend returned an empty trace.',
  malformed: 'The backend returned a malformed trace.'
});

// dsTypes whose hero canvas already IS the full-run view, so the capture strip beneath
// it would either duplicate what's on screen (DpTable) or convey the run's shape less
// directly than watching the diagram animate (Graph, Tree) — see RCA-002 for the
// original DpTable case and PROMPT-F-visual-fidelity.md for Graph/Tree.
const CAPTURE_STRIP_REDUNDANT_FOR = new Set(['DpTable', 'Graph', 'Tree']);

function uniqueProblemsById(problems) {
  const seen = new Set();
  return problems.filter((problem) => {
    const id = typeof problem?.id === 'string' ? problem.id : '';
    if (!id || seen.has(id)) return false;
    seen.add(id);
    return true;
  });
}

export default function App() {
  const { id: urlProblemId } = useParams();
  const navigate = useNavigate();

  const [problems, setProblems] = useState(DEFAULT_FALLBACK_PROBLEMS);
  const [activeCategory, setActiveCategory] = useState(null);
  const activeProblemId = urlProblemId || 'two-sum';
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [catalogError, setCatalogError] = useState(null);

  // The catalogue entry — summary fields only (id, title, category, dsType, traced).
  const catalogEntry = problems.find(p => p.id === activeProblemId) || problems[0] || null;

  // Only the four presets Controls can render. A speed persisted by an older build would
  // otherwise highlight no button and could not be changed back by clicking one.
  const [persistedSpeed, setPersistedSpeed] = usePersistentState(
    'speed', 1000, (v) => [2000, 1000, 500, 250].includes(v));

  // All playback state lives in useTrace.
  const {
    steps, currentStep, currentStepIndex,
    isPlaying, speed,
    loading: traceLoading,
    error: traceError,
    truncated: traceTruncated,
    fieldErrors,
    detail,
    togglePlay, stepNext, stepPrev, reset, seek, setSpeed, runInput, resolvedInput, anchors
  } = useTrace(activeProblemId, catalogEntry, { initialSpeed: persistedSpeed });

  // Merge in the per-problem detail (javaCode, complexity, defaultGraphNodes, ...) —
  // it isn't in the catalogue summary, so CodeViewer/MemoryComplexityCard/canvases
  // would otherwise silently fall back to placeholder data for every problem.
  const activeProblem = detail ? { ...catalogEntry, ...detail } : catalogEntry;

  // ── What has actually been watched ───────────────────────────────────────
  const { progress, markWatched, toggleStar } = useProgress();
  useLastVisited(activeProblemId);

  // A visit only counts once real navigation to a problem has happened, not merely the
  // app mounting - Dashboard reads this streak but never writes it, for the same reason.
  const today = useMemo(() => new Date().toISOString().slice(0, 10), []);
  const { recordVisit } = useStreak(today);
  useEffect(() => { recordVisit(); }, [recordVisit]);
  const activeProgress = progress[activeProblemId];

  // Reaching the last step, not opening the page: clicking into a problem is an accident
  // of browsing, sitting through the trace to the end is not.
  useEffect(() => {
    if (steps.length > 0 && currentStepIndex === steps.length - 1) {
      markWatched(activeProblemId);
    }
  }, [activeProblemId, currentStepIndex, steps.length, markWatched]);

  // ── The rest of "what I am looking at", carried in the URL ───────────────
  // /problem/:id already made the problem linkable; the step and the input were not, so a
  // refresh landed you back on step 1 of the defaults.
  const pendingView = useRef(null);
  const { shareInput } = useShareableView({
    problemId: activeProblemId,
    stepIndex: currentStepIndex,
    totalSteps: steps.length,
    // Held, not applied: at restore time the trace for this problem has not loaded yet, so
    // there is nothing to seek into and no inputSpec to validate against.
    onRestore: (view) => { pendingView.current = view; }
  });

  // A custom input replaces the trace entirely, so it has to run before the step is
  // restored — seeking into the default trace and then replacing it would land on step 1.
  useEffect(() => {
    const view = pendingView.current;
    if (!view || traceLoading) return;
    if (view.input) {
      const input = view.input;
      pendingView.current = { ...view, input: null };
      runInput(input);
      return;
    }
    if (view.step === null || steps.length === 0) return;
    pendingView.current = null;
    if (view.step < steps.length) seek(view.step);
  }, [traceLoading, steps.length, runInput, seek]);

  // Runs from the input editor are the shareable ones; the defaults are already implied by
  // the problem id, so a ?input for them would be noise in every link.
  const runAndShare = useCallback((values) => {
    shareInput(values);
    return runInput(values);
    // shareInput closes over the live search params and is re-created each render.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [runInput]);

  // ── Single-endpoint catalogue fetch ──────────────────────────────────────
  const fetchAllProblems = useCallback(async () => {
    try {
      setCatalogLoading(true);
      const response = await fetch('/api/problems');
      if (!response.ok) throw new Error(`Catalogue fetch failed: ${response.status}`);
      const data = await response.json();

      if (Array.isArray(data) && data.length > 0) {
        const uniqueProblems = uniqueProblemsById(data);
        if (uniqueProblems.length > 0) {
          setProblems(uniqueProblems);
          setCatalogError(null);

          // If the URL points to a problem that doesn't exist in the catalogue,
          // navigate to a sensible default instead of showing a blank canvas.
          const urlIdExists = uniqueProblems.some(p => p.id === activeProblemId);
          if (!urlIdExists) {
            const fallbackId = uniqueProblems.find(p => p.id === 'two-sum')?.id
              || uniqueProblems[0].id;
            navigate(`/problem/${fallbackId}`, { replace: true });
          }
        }
      }
    } catch (err) {
      console.warn('Backend connection failed:', err);
      // Fall through to DEFAULT_FALLBACK_PROBLEMS — the app is usable offline, but the
      // learner should be told why the library says "2 algorithms" instead of guessing.
      setCatalogError('Could not reach the backend. Showing a small offline sample.');
    } finally {
      setCatalogLoading(false);
    }
  }, [activeProblemId, navigate]);

  useEffect(() => {
    fetchAllProblems();
  }, [fetchAllProblems]);

  // ── Layout state ─────────────────────────────────────────────────────────
  // View preferences survive a reload. The selected problem deliberately does not - the
  // URL owns that, and persisting it would fight deep links.
  const isBool = (v) => typeof v === 'boolean';
  const {
    viewportWidth, isMobile,
    isSidebarOpen, setIsSidebarOpen,
    isBottomPanelOpen, setIsBottomPanelOpen,
    isInputEditorOpen, setIsInputEditorOpen,
    isComplexityOpen, setIsComplexityOpen,
    isStatementOpen, setIsStatementOpen
  } = useLayoutPreferences();

  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const [isPaletteOpen, setIsPaletteOpen] = useState(false);
  const [isCompareOpen, setIsCompareOpen] = useState(false);
  const { theme, cycleTheme } = useTheme();
  // Shown once, on a genuine first visit. Tracked rather than inferred from other
  // preferences: someone who only ever changed the theme has still never been introduced.
  const [hasSeenWelcome, setHasSeenWelcome] = usePersistentState('seenWelcome', false, isBool);
  const [isTourOpen, setIsTourOpen] = useState(false);
  const [activeTab, setActiveTab] = useState('code');

  const drawerRef = useRef(null);
  useFocusTrap(drawerRef, isMobile && isSidebarOpen);

  // A speed change is both playback state and a saved preference, so it goes through one
  // handler rather than leaving the two to drift.
  const changeSpeed = useCallback((ms) => {
    setSpeed(ms);
    setPersistedSpeed(ms);
  }, [setSpeed, setPersistedSpeed]);

  const SPEED_PRESETS = useMemo(() => [2000, 1000, 500, 250], []);

  const nudgeSpeed = useCallback((direction) => {
    // Presets run slow -> fast, so "faster" moves right. Clamped rather than wrapping:
    // holding the key should settle at 4x, not jump back to 0.5x.
    const at = SPEED_PRESETS.indexOf(speed);
    const from = at === -1 ? 1 : at;
    const next = Math.min(SPEED_PRESETS.length - 1, Math.max(0, from + direction));
    changeSpeed(SPEED_PRESETS[next]);
  }, [SPEED_PRESETS, speed, changeSpeed]);

  useKeyboardShortcuts({
    togglePlay, stepNext, stepPrev, reset, seek, stepCount: steps.length, nudgeSpeed,
    isMobile, isSidebarOpen, setIsSidebarOpen,
    isHelpOpen, setIsHelpOpen,
    isPaletteOpen, setIsPaletteOpen,
    hasSeenWelcome, setHasSeenWelcome
  });

  const handleSelectCategory = (cat) => {
    setActiveCategory(cat);
  };

  const handleSelectProblem = (id) => {
    navigate(`/problem/${id}`);
    if (viewportWidth <= 768) {
      setIsSidebarOpen(false);
    }
  };

  const loading = catalogLoading;
  const hasInputSpec = Boolean(activeProblem?.inputSpec?.fields?.length);

  // With the code beside the canvas, the bottom row exists only for the on-demand panels
  // and disappears entirely when neither is open - no empty reserved strip.
  const showInputEditor = hasInputSpec && isInputEditorOpen;
  const showBottomRow = showInputEditor || isComplexityOpen;
  const bottomGridColumns = showInputEditor && isComplexityOpen ? '1fr 1fr' : '1fr';
  const activeDsType = currentStep?.dsType || activeProblem?.dsType || '';
  const traceErrorCopy = TRACE_ERROR_COPY[traceError];
  const showingOfflineTrace = traceError === 'fetch' && steps.length > 0;

  // ── Canvas selection by dsType ───────────────────────────────────────────
  const renderCanvas = () => {
    if (!activeProblem) {
      return (
        <div
          role="status"
          className={styles.canvasEmpty}
        >
          Select a problem to begin visualization
        </div>
      );
    }

    // resolvedInput is the trace's, not a step's. IntervalCanvas needs it to draw the
    // intervals the run actually used rather than the inputSpec defaults.
    // steps + index let a canvas look back for the last step that carried structure.
    // WindowCanvas needs it: tracers interleave commentary steps with no arrayState, and
    // dropping the frame on those would make the window flicker out every other step.
    const props = {
      currentStep, step: currentStep, problem: activeProblem, resolvedInput,
      steps, currentStepIndex
    };

    const Canvas = CANVAS_BY_DSTYPE[activeDsType];
    if (!Canvas) {
      return (
        <div
          role="status"
          className={styles.canvasEmpty}
        >
          No visualization for {activeDsType || 'unknown'}
        </div>
      );
    }

    const companions = getCompanions(activeDsType, currentStep, steps);
    if (companions.length === 0) {
      return <Canvas {...props} />;
    }

    return (
      <div className="stage-with-companions">
        <div className="canvas-hero">
          <Canvas {...props} />
        </div>
        {companions.map(({ key, Component, props: companionProps }) => (
          <Component key={key} {...companionProps} />
        ))}
      </div>
    );
  };

  return (
    <div className={styles.rootLayout}>
      <Header 
        problem={activeProblem} 
        totalProblems={problems.length}
        runnableProblems={problems.filter((p) => p.traced === true).length} 
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen(prev => !prev)}
        theme={theme}
        onCycleTheme={cycleTheme}
        onStartTour={isMobile ? null : () => {
          // Dismiss the first-run screen first: the tour highlights the UI behind it.
          setHasSeenWelcome(true);
          setIsHelpOpen(false);
          setIsTourOpen(true);
        }}
      />

      <Breadcrumb
        problem={activeProblem}
        watched={activeProgress?.watched === true}
        starred={activeProgress?.starred === true}
        onToggleStar={() => toggleStar(activeProblemId)}
      />

      {/* Where this problem sits in its curriculum section, and the one either side of it -
          every trace used to end in silence, with no next action and no reason to come
          back. striverSheetSection is on every catalogue entry already; this is the first
          thing in the app that reads it. */}
      <SectionNav
        problems={problems}
        activeProblemId={activeProblemId}
        progress={progress}
        onSelectProblem={handleSelectProblem}
      />

      <ProblemStatement
        problem={activeProblem}
        open={isStatementOpen}
        onToggle={() => setIsStatementOpen(prev => !prev)}
      />

      <ShortcutHelp
        open={isHelpOpen}
        onClose={() => setIsHelpOpen(false)}
        onReplayWelcome={() => {
          setIsHelpOpen(false);
          setHasSeenWelcome(false);
        }}
      />

      <CommandPalette
        isOpen={isPaletteOpen}
        onClose={() => setIsPaletteOpen(false)}
        problems={problems}
        onSelectProblem={(id) => {
          handleSelectProblem(id);
          setIsPaletteOpen(false);
        }}
        onCycleTheme={cycleTheme}
      />

      <WelcomeGuide
        open={!hasSeenWelcome && !loading && !catalogError && !isTourOpen}
        onDismiss={() => setHasSeenWelcome(true)}
        onShowShortcuts={() => {
          setHasSeenWelcome(true);
          setIsHelpOpen(true);
        }}
        onStartTour={isMobile ? null : () => {
          setHasSeenWelcome(true);
          setIsTourOpen(true);
        }}
      />

      <TourGuide open={isTourOpen} onClose={() => setIsTourOpen(false)} />

      {catalogError && (
        <div
          role="alert"
          className={styles.catalogAlert}
        >
          <span>{catalogError}</span>
          <button type="button" className={`btn btn-outline ${styles.catalogAlertRetry}`} onClick={fetchAllProblems}>
            Retry
          </button>
        </div>
      )}

      {/* Main Workspace Container */}
      <div className={styles.workspace}>
        {/* Sidebar (Search & Explore Panel) */}
        {isSidebarOpen && isMobile && (
          <div
            onClick={() => setIsSidebarOpen(false)}
            aria-hidden="true"
            data-testid="mobile-backdrop"
            className={styles.mobileBackdrop}
          />
        )}
        {isSidebarOpen && (
          <div
            ref={drawerRef}
            data-tour="problem-list"
            className={isMobile ? styles.sidebarMobile : styles.sidebarDesktop}
          >
            <Sidebar
              problems={problems}
              activeProblemId={activeProblemId}
              activeCategory={activeCategory}
              progress={progress}
              onSelectCategory={handleSelectCategory}
              onSelectProblem={handleSelectProblem}
              onRetry={fetchAllProblems}
            />
            {/* Collapse handle: sits on the sidebar's own edge, right where a viewer
                watching the canvas is already looking, instead of only in the header. */}
            <button
              type="button"
              onClick={() => setIsSidebarOpen(false)}
              aria-label="Collapse the problem list"
              title="Collapse the problem list"
              className={styles.sidebarCollapseBtn}
            >
              <ChevronLeft size={14} />
            </button>
          </div>
        )}
        {!isSidebarOpen && !isMobile && (
          <button
            type="button"
            onClick={() => setIsSidebarOpen(true)}
            aria-label="Open the problem list"
            title="Open the problem list"
            className={styles.sidebarExpandBtn}
          >
            <ChevronRight size={14} />
          </button>
        )}

        <main className={styles.mainStage}>
          {/* Desktop puts the code beside the canvas rather than beneath it. Vertical space
              is the scarcer axis on a laptop, and a stacked layout spends it on the one
              panel that reads fine in a tall narrow column while squeezing the graphs and
              trees that need width. Mobile stays stacked, where that is the right shape. */}
          <div className={isMobile ? styles.stageStack : styles.stageSplit}>
            {!isMobile && isBottomPanelOpen && (
              <div data-tour="code-panel" className={styles.codeColumn}>
                <CodeViewer problem={activeProblem} currentStep={currentStep} anchors={anchors} steps={steps} />
              </div>
            )}

          {/* Main Visualizer Stage + Controls + Live Trace Banner */}
          <div className={styles.stageColumn}>
          <div data-tour="canvas" className={`glass-panel ${styles.stagePanel}`}>
            {/* The canvas draws the state; this says it. Inside the canvas region so it
                reads as part of the visualization rather than as stray page text. */}
            <StepStateSummary step={currentStep} dsType={activeDsType} />
            <div className={styles.stageInner}>
              {loading ? (
                <div className={styles.loadingCatalog}>
                  <RefreshCw size={24} className="spin" />
                  <span className={styles.loadingCatalogText}>Loading Algorithm Engine & Catalog...</span>
                </div>
              ) : activeProblem ? (
                <CanvasShell
                  title={activeProblem.title}
                  meta={steps.length ? `Step ${currentStepIndex + 1} of ${steps.length}` : null}
                >
                  {traceLoading ? (
                    <div className={styles.loadingTrace}>
                      <RefreshCw size={18} className="spin" />
                      <span className={styles.loadingTraceText}>Loading trace…</span>
                    </div>
                  ) : traceError === 'untraced' ? (
                    <div className={styles.untracedNotice}>
                      This problem is catalogued but not yet traced.
                    </div>
                  ) : traceErrorCopy && !showingOfflineTrace ? (
                    <div role="alert" className={styles.traceAlert}>
                      {traceErrorCopy}
                    </div>
                  ) : (
                    <ErrorBoundary resetKey={activeProblemId}>
                      {renderCanvas()}
                    </ErrorBoundary>
                  )}
                </CanvasShell>
              ) : (
                <ErrorBoundary resetKey={activeProblemId}>
                  {renderCanvas()}
                </ErrorBoundary>
              )}
            </div>

            {showingOfflineTrace && (
              <div role="status" className={styles.offlineStatus}>
                Live trace unavailable. Showing the checked-in offline sample.
              </div>
            )}

            {traceTruncated && (
              <div className={styles.truncatedStatus}>
                This trace hit the step budget and was cut short — try a smaller input for the full run.
              </div>
            )}

            {/* Redundant with the hero for these types, and it costs real vertical space:
                DP's cell states already show the recurrence across the whole table; a
                graph or tree traversal is already fully legible from watching the nodes
                change state in motion, and the strip's row-per-vertex grid conveys that
                traversal order less directly than the diagram already does. */}
            {!CAPTURE_STRIP_REDUNDANT_FOR.has(activeDsType) && (
              <div data-tour="capture-strip">
                <CaptureStrip
                  steps={steps}
                  current={currentStepIndex}
                  dsType={activeDsType}
                  onSeek={seek}
                  resolvedInput={resolvedInput}
                />
              </div>
            )}

            {!isMobile && isCompareOpen && activeProblem?.alternateInput
              && !CAPTURE_STRIP_REDUNDANT_FOR.has(activeDsType) && (
              <div className={styles.compareCard}>
                <CompareStrip
                  key={activeProblemId}
                  problemId={activeProblemId}
                  dsType={activeDsType}
                  alternateInput={activeProblem.alternateInput}
                />
              </div>
            )}

            {/* Integrated Playback Controls */}
            <div data-tour="controls">
            <Controls
              isPlaying={isPlaying}
              currentStepIndex={currentStepIndex}
              totalSteps={steps.length}
              speed={speed}
              onPlayPause={togglePlay}
              onStepNext={stepNext}
              onStepPrev={stepPrev}
              onStepSelect={seek}
              onReset={reset}
              onSpeedChange={changeSpeed}
            />
            </div>

            {/* Quiet Live Trace Banner */}
            <div className={styles.tickerWrapper}>
              <LiveTraceTicker stepDescription={currentStep?.description} />
            </div>
          </div>
          </div>
          </div>

          {/* Collapse handle for the whole bottom section, so the canvas can take the
              full height while a trace is playing. Always visible so it can be reopened. */}
          <div className={styles.bottomBar}>
            <button
              type="button"
              onClick={() => setIsBottomPanelOpen(prev => !prev)}
              aria-expanded={isBottomPanelOpen}
              aria-label={isBottomPanelOpen ? 'Collapse the code panel' : 'Expand the code panel'}
              title={isBottomPanelOpen ? 'Collapse the code panel' : 'Expand the code panel'}
              className={`btn btn-outline ${styles.bottomToggleBtn}`}
            >
              {isBottomPanelOpen ? <ChevronDown size={13} /> : <ChevronUp size={13} />}
              {isBottomPanelOpen ? 'Hide code' : 'Show code'}
            </button>

            {/* What the animation is running on, without the editor that sets it. */}
            {!isMobile && !isInputEditorOpen && (
              <div data-tour="input-summary">
                <InputSummary resolvedInput={resolvedInput} />
              </div>
            )}

            {!isMobile && (
              <div data-tour="panel-toggles" className={styles.bottomBarActions}>
                {hasInputSpec && (
                  <button
                    type="button"
                    onClick={() => {
                      setIsInputEditorOpen(prev => !prev);
                      setIsBottomPanelOpen(true);
                    }}
                    aria-expanded={isInputEditorOpen}
                    className={`btn btn-outline ${styles.bottomToggleBtn}`}
                    title={isInputEditorOpen ? 'Close the input editor' : 'Change the input'}
                  >
                    {isInputEditorOpen ? 'Done editing' : 'Edit input'}
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => {
                    setIsComplexityOpen(prev => !prev);
                    setIsBottomPanelOpen(true);
                  }}
                  aria-expanded={isComplexityOpen}
                  className={`btn btn-outline ${styles.bottomToggleBtn}`}
                  title={isComplexityOpen ? 'Hide memory and complexity' : 'Show memory and complexity'}
                >
                  {isComplexityOpen ? 'Hide' : 'Show'} memory &amp; complexity
                </button>
                {activeProblem?.alternateInput && !CAPTURE_STRIP_REDUNDANT_FOR.has(activeDsType) && (
                  <button
                    type="button"
                    onClick={() => setIsCompareOpen(prev => !prev)}
                    aria-expanded={isCompareOpen}
                    className={`btn btn-outline ${styles.bottomToggleBtn}`}
                    title={isCompareOpen ? 'Hide the comparison' : 'Compare against the other case'}
                  >
                    {isCompareOpen ? 'Hide' : 'Compare'} other case
                  </button>
                )}
                <button
                  type="button"
                  onClick={() => setIsHelpOpen(true)}
                  className={`btn btn-outline ${styles.bottomToggleBtn}`}
                  title="Keyboard shortcuts (?)"
                  aria-label="Show keyboard shortcuts"
                >
                  ?
                </button>
              </div>
            )}
          </div>

          {/* Bottom section. On desktop the code now lives beside the canvas, so this row
              carries only the on-demand panels and disappears when neither is open. Mobile
              keeps its stacked tab card, which is the right shape on a narrow screen. */}
          {!isMobile ? (showBottomRow ? (
            <div
              className={styles.bottomDesktopGrid}
              style={{ gridTemplateColumns: bottomGridColumns }}
            >
              {hasInputSpec && isInputEditorOpen && (
                <div className={`glass-panel ${styles.inputCard}`}>
                  <InputPanel
                    problemId={activeProblemId}
                    inputSpec={activeProblem.inputSpec}
                    alternateInput={activeProblem.alternateInput}
                    fieldErrors={fieldErrors}
                    running={traceLoading}
                    onRun={runAndShare}
                  />
                </div>
              )}
              {isComplexityOpen && (
                <MemoryComplexityCard currentStep={currentStep} problem={activeProblem} />
              )}
            </div>
          ) : null) : (isBottomPanelOpen ? (
            /* Mobile Tab Bottom Card Section (Code / Input / Memory / Complexity) */
            <div className={`glass-panel ${styles.bottomMobileCard}`}>
              <div className={styles.mobileTabNav}>
                <button
                  onClick={() => setActiveTab('code')}
                  className={`btn ${activeTab === 'code' ? 'btn-primary' : 'btn-outline'} ${styles.mobileTabBtn}`}
                >
                  Code
                </button>
                {hasInputSpec && (
                  <button
                    onClick={() => setActiveTab('input')}
                    className={`btn ${activeTab === 'input' ? 'btn-primary' : 'btn-outline'} ${styles.mobileTabBtn}`}
                  >
                    Input
                  </button>
                )}
                <button
                  onClick={() => setActiveTab('memory')}
                  className={`btn ${activeTab === 'memory' ? 'btn-primary' : 'btn-outline'} ${styles.mobileTabBtn}`}
                >
                  Memory
                </button>
                <button
                  onClick={() => setActiveTab('complexity')}
                  className={`btn ${activeTab === 'complexity' ? 'btn-primary' : 'btn-outline'} ${styles.mobileTabBtn}`}
                >
                  Complexity
                </button>
              </div>

              <div className={styles.mobileTabBody}>
                {activeTab === 'code' ? (
                  <CodeViewer problem={activeProblem} currentStep={currentStep} anchors={anchors} steps={steps} />
                ) : activeTab === 'input' ? (
                  <div className={styles.mobileInputContainer}>
                    <InputPanel
                      problemId={activeProblemId}
                      inputSpec={activeProblem.inputSpec}
                      alternateInput={activeProblem.alternateInput}
                      fieldErrors={fieldErrors}
                      running={traceLoading}
                      onRun={runAndShare}
                    />
                  </div>
                ) : (
                  <MemoryComplexityCard currentStep={currentStep} problem={activeProblem} initialTab={activeTab} />
                )}
              </div>
            </div>
          ) : null)}
        </main>
      </div>
    </div>
  );
}
