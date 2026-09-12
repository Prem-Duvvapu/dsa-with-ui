import React, { useState, useEffect, useCallback, useMemo, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import Header from './components/Header';
import Breadcrumb from './components/Breadcrumb';
import ProblemStatement from './components/ProblemStatement';
import InputSummary from './components/InputSummary';
import ShortcutHelp from './components/ShortcutHelp';
import WelcomeGuide from './components/WelcomeGuide';
import TourGuide from './components/TourGuide';
import usePersistentState from './hooks/usePersistentState';
import useTheme from './hooks/useTheme';
import Sidebar from './components/Sidebar';
import CanvasShell from './components/CanvasShell';
import ErrorBoundary from './components/ErrorBoundary';
import CaptureStrip from './components/CaptureStrip';
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
    togglePlay, stepNext, stepPrev, reset, seek, setSpeed, runInput, resolvedInput
  } = useTrace(activeProblemId, catalogEntry, { initialSpeed: persistedSpeed });

  // Merge in the per-problem detail (javaCode, complexity, defaultGraphNodes, ...) —
  // it isn't in the catalogue summary, so CodeViewer/MemoryComplexityCard/canvases
  // would otherwise silently fall back to placeholder data for every problem.
  const activeProblem = detail ? { ...catalogEntry, ...detail } : catalogEntry;

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
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);
  const isMobile = viewportWidth <= 768;

  // Only the DESKTOP sidebar preference is persisted. On a narrow viewport the sidebar is
  // a modal drawer over the canvas, and it must always start closed there - restoring
  // "open" from a desktop session would greet a phone user with the drawer covering the
  // thing they came to watch.
  const [desktopSidebarOpen, setDesktopSidebarOpen] = usePersistentState('sidebarOpen', true, isBool);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const isSidebarOpen = isMobile ? mobileSidebarOpen : desktopSidebarOpen;
  const setIsSidebarOpen = isMobile ? setMobileSidebarOpen : setDesktopSidebarOpen;

  const [isHelpOpen, setIsHelpOpen] = useState(false);
  const { theme, cycleTheme } = useTheme();
  // Shown once, on a genuine first visit. Tracked rather than inferred from other
  // preferences: someone who only ever changed the theme has still never been introduced.
  const [hasSeenWelcome, setHasSeenWelcome] = usePersistentState('seenWelcome', false, isBool);
  const [isTourOpen, setIsTourOpen] = useState(false);
  // Collapsing this row frees up vertical space for the canvas while a trace is playing.
  const [isBottomPanelOpen, setIsBottomPanelOpen] = usePersistentState('bottomPanelOpen', true, isBool);
  // The input editor and the complexity card are setup furniture: useful before a run,
  // noise during one. They are opened on demand instead of holding a fixed share of the
  // 340px bottom row, and what you actually want from the editor while watching - the
  // input being animated - is stated by InputSummary in a single line.
  const [isInputEditorOpen, setIsInputEditorOpen] = usePersistentState('inputEditorOpen', false, isBool);
  const [isComplexityOpen, setIsComplexityOpen] = usePersistentState('complexityOpen', false, isBool);
  const [isStatementOpen, setIsStatementOpen] = usePersistentState('statementOpen', true, isBool);
  const [activeTab, setActiveTab] = useState('code');

  useEffect(() => {
    const handleResize = () => {
      setViewportWidth(window.innerWidth);
      if (window.innerWidth > 768) {
        setIsSidebarOpen(true);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const drawerRef = useRef(null);

  // ── Mobile drawer focus trap ─────────────────────────────────────────────
  useEffect(() => {
    if (!isMobile || !isSidebarOpen) return;

    const drawer = drawerRef.current;
    if (!drawer) return;

    const previouslyFocused = document.activeElement;
    const focusables = drawer.querySelectorAll(
      'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
    );
    if (focusables.length > 0) {
      focusables[0].focus();
    }

    const handleTabKey = (e) => {
      if (e.key !== 'Tab') return;
      const currentFocusables = Array.from(drawer.querySelectorAll(
        'button, [href], input, select, textarea, [tabindex]:not([tabindex="-1"])'
      )).filter(el => !el.disabled && el.offsetParent !== null);
      if (currentFocusables.length === 0) return;

      const firstEl = currentFocusables[0];
      const lastEl = currentFocusables[currentFocusables.length - 1];

      if (e.shiftKey) {
        if (document.activeElement === firstEl) {
          e.preventDefault();
          lastEl.focus();
        }
      } else {
        if (document.activeElement === lastEl) {
          e.preventDefault();
          firstEl.focus();
        }
      }
    };

    window.addEventListener('keydown', handleTabKey);
    return () => {
      window.removeEventListener('keydown', handleTabKey);
      if (previouslyFocused && previouslyFocused.focus) {
        previouslyFocused.focus();
      }
    };
  }, [isMobile, isSidebarOpen]);

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

  // ── Global keyboard shortcuts ────────────────────────────────────────────
  // This is a media player, so it uses a media player's keys: J/K/L and ,/. alongside the
  // arrows. The list lives in ShortcutHelp, opened with `?` - the shortcuts worked before
  // but were written down only in two button tooltips, which is not discoverable.
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return;

      const active = document.activeElement;
      const tag = active?.tagName;
      const isTyping = ['INPUT', 'TEXTAREA', 'SELECT'].includes(tag) || active?.isContentEditable;

      // Escape works even while typing - someone in the search field is exactly who needs
      // it - and closes the topmost thing first.
      if (e.code === 'Escape') {
        if (!hasSeenWelcome) {
          e.preventDefault();
          setHasSeenWelcome(true);
          return;
        }
        if (isHelpOpen) {
          e.preventDefault();
          setIsHelpOpen(false);
          return;
        }
        if (isMobile && isSidebarOpen) {
          e.preventDefault();
          setIsSidebarOpen(false);
          return;
        }
        if (isTyping) active.blur();
        return;
      }

      if (isTyping) return;

      // `/` focuses search, matching every other search-first UI.
      if (e.key === '/') {
        e.preventDefault();
        setIsSidebarOpen(true);
        document.querySelector('[data-search-input]')?.focus();
        return;
      }

      if (e.key === '?') {
        e.preventDefault();
        setIsHelpOpen(prev => !prev);
        return;
      }

      // A focused button activates on Space/Enter natively. Letting Space through here too
      // would toggle playback twice; blocking every key while a button has focus - which is
      // what this used to do - meant one click on Play killed the keyboard for good.
      const onButton = tag === 'BUTTON';

      switch (e.code) {
        case 'Space':
          if (onButton) return;
          e.preventDefault();
          togglePlay();
          return;
        case 'KeyK':
          e.preventDefault();
          togglePlay();
          return;
        case 'ArrowRight':
        case 'KeyL':
        case 'Period':
          e.preventDefault();
          stepNext();
          return;
        case 'ArrowLeft':
        case 'KeyJ':
        case 'Comma':
          e.preventDefault();
          stepPrev();
          return;
        case 'Home':
          e.preventDefault();
          seek(0);
          return;
        case 'End':
          e.preventDefault();
          if (steps.length > 0) seek(steps.length - 1);
          return;
        case 'KeyR':
          e.preventDefault();
          reset();
          return;
        case 'BracketLeft':
          e.preventDefault();
          nudgeSpeed(-1);
          return;
        case 'BracketRight':
          e.preventDefault();
          nudgeSpeed(1);
          return;
        default:
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [togglePlay, stepNext, stepPrev, reset, seek, steps.length, nudgeSpeed,
      isMobile, isSidebarOpen, isHelpOpen, setIsSidebarOpen,
      hasSeenWelcome, setHasSeenWelcome]);

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

    const props = { currentStep, step: currentStep, problem: activeProblem };

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

      <Breadcrumb problem={activeProblem} />

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
                <CodeViewer problem={activeProblem} currentStep={currentStep} />
              </div>
            )}

          {/* Main Visualizer Stage + Controls + Live Trace Banner */}
          <div className={styles.stageColumn}>
          <div data-tour="canvas" className={`glass-panel ${styles.stagePanel}`}>
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
              <CaptureStrip
                steps={steps}
                current={currentStepIndex}
                dsType={activeDsType}
                onSeek={seek}
              />
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
                    fieldErrors={fieldErrors}
                    running={traceLoading}
                    onRun={runInput}
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
                  <CodeViewer problem={activeProblem} currentStep={currentStep} />
                ) : activeTab === 'input' ? (
                  <div className={styles.mobileInputContainer}>
                    <InputPanel
                      problemId={activeProblemId}
                      inputSpec={activeProblem.inputSpec}
                      fieldErrors={fieldErrors}
                      running={traceLoading}
                      onRun={runInput}
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
