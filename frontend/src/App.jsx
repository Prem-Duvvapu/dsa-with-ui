import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import Breadcrumb from './components/Breadcrumb';
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
import { RefreshCw } from 'lucide-react';

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
  const [problems, setProblems] = useState(DEFAULT_FALLBACK_PROBLEMS);
  const [activeCategory, setActiveCategory] = useState(null);
  const [activeProblemId, setActiveProblemId] = useState('two-sum');
  const [catalogLoading, setCatalogLoading] = useState(false);
  const [catalogError, setCatalogError] = useState(null);

  // The catalogue entry — summary fields only (id, title, category, dsType, traced).
  const catalogEntry = problems.find(p => p.id === activeProblemId) || problems[0] || null;

  // All playback state lives in useTrace.
  const {
    steps, currentStep, currentStepIndex,
    isPlaying, speed,
    loading: traceLoading,
    error: traceError,
    truncated: traceTruncated,
    fieldErrors,
    detail,
    togglePlay, stepNext, stepPrev, reset, seek, setSpeed, runInput
  } = useTrace(activeProblemId, catalogEntry);

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
          const initialId = uniqueProblems.find(p => p.id === 'two-sum')?.id
            || uniqueProblems[0].id;
          setActiveProblemId(initialId);
          setCatalogError(null);
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
  }, []);

  useEffect(() => {
    fetchAllProblems();
  }, [fetchAllProblems]);

  // ── Layout state ─────────────────────────────────────────────────────────
  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 768);
  const [activeTab, setActiveTab] = useState('code');
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);
  const isMobile = viewportWidth <= 768;

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

  // ── Global keyboard shortcuts ────────────────────────────────────────────
  useEffect(() => {
    const handleKeyDown = (e) => {
      // Escape closes the mobile drawer regardless of what's focused — a learner typing
      // in the search field is exactly who needs Escape to work.
      if (e.code === 'Escape' && isMobile && isSidebarOpen) {
        e.preventDefault();
        setIsSidebarOpen(false);
        return;
      }

      const tag = document.activeElement?.tagName;
      if (['INPUT', 'TEXTAREA', 'SELECT', 'BUTTON'].includes(tag)
          || document.activeElement?.isContentEditable) return;

      if (e.code === 'Space') {
        e.preventDefault();
        togglePlay();
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        stepNext();
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        stepPrev();
      } else if (e.code === 'KeyR') {
        e.preventDefault();
        reset();
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [togglePlay, stepNext, stepPrev, reset, isMobile, isSidebarOpen]);

  const handleSelectCategory = (cat) => {
    setActiveCategory(cat);
  };

  const handleSelectProblem = (id) => {
    setActiveProblemId(id);
    if (viewportWidth <= 768) {
      setIsSidebarOpen(false);
    }
  };

  const loading = catalogLoading;
  const hasInputSpec = Boolean(activeProblem?.inputSpec?.fields?.length);
  const activeDsType = currentStep?.dsType || activeProblem?.dsType || '';
  const traceErrorCopy = TRACE_ERROR_COPY[traceError];
  const showingOfflineTrace = traceError === 'fetch' && steps.length > 0;

  // ── Canvas selection by dsType ───────────────────────────────────────────
  const renderCanvas = () => {
    if (!activeProblem) return null;

    const props = { currentStep, step: currentStep, problem: activeProblem };

    const Canvas = CANVAS_BY_DSTYPE[activeDsType];
    if (!Canvas) {
      return (
        <div
          role="status"
          style={{
            flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center',
            color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)',
            fontSize: '0.9rem', padding: '24px', textAlign: 'center'
          }}
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
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', overflow: 'hidden', background: 'var(--bg-page)' }}>
      <Header 
        problem={activeProblem} 
        totalProblems={problems.length} 
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen(prev => !prev)}
      />

      <Breadcrumb problem={activeProblem} />

      {catalogError && (
        <div
          role="alert"
          style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '10px',
            margin: '0 12px', padding: '6px 12px', fontSize: '0.74rem',
            color: 'var(--probe)', background: 'rgba(255, 176, 0, 0.08)',
            border: '1px solid rgba(255, 176, 0, 0.3)', borderRadius: 'var(--radius-sm)'
          }}
        >
          <span>{catalogError}</span>
          <button type="button" className="btn btn-outline" style={{ padding: '2px 8px', fontSize: '0.7rem' }} onClick={fetchAllProblems}>
            Retry
          </button>
        </div>
      )}

      {/* Main Workspace Container */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', padding: '0 12px 8px 12px', gap: '12px', position: 'relative' }}>
        {/* Sidebar (Search & Explore Panel) */}
        {isSidebarOpen && isMobile && (
          <div
            onClick={() => setIsSidebarOpen(false)}
            aria-hidden="true"
            style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', zIndex: 99 }}
          />
        )}
        {isSidebarOpen && (
          <div style={{
            position: isMobile ? 'absolute' : 'relative',
            top: isMobile ? 0 : 'auto',
            left: isMobile ? 0 : 'auto',
            bottom: isMobile ? 0 : 'auto',
            zIndex: isMobile ? 100 : 1,
            height: isMobile ? '100%' : 'auto',
            boxShadow: isMobile ? '0 0 40px rgba(0,0,0,0.8)' : 'none'
          }}>
            <Sidebar
              problems={problems}
              activeProblemId={activeProblemId}
              activeCategory={activeCategory}
              onSelectCategory={handleSelectCategory}
              onSelectProblem={handleSelectProblem}
              onRetry={fetchAllProblems}
            />
          </div>
        )}

        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '10px', overflow: 'hidden' }}>
          {/* Main Visualizer Stage + Controls + Live Trace Banner */}
          <div className="glass-panel" style={{ flex: isMobile ? '1' : '1', display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0 }}>
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0 }}>
              {loading ? (
                <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-violet)', gap: '10px' }}>
                  <RefreshCw size={24} className="spin" />
                  <span style={{ fontWeight: '700' }}>Loading Algorithm Engine & Catalog...</span>
                </div>
              ) : activeProblem ? (
                <CanvasShell
                  title={activeProblem.title}
                  meta={steps.length ? `Step ${currentStepIndex + 1} of ${steps.length}` : null}
                >
                  {traceLoading ? (
                    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bench-ink-dim)', gap: '8px' }}>
                      <RefreshCw size={18} className="spin" />
                      <span style={{ fontFamily: 'var(--font-code)', fontSize: '0.85rem' }}>Loading trace…</span>
                    </div>
                  ) : traceError === 'untraced' ? (
                    <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)', fontSize: '0.9rem', padding: '24px', textAlign: 'center' }}>
                      This problem is catalogued but not yet traced.
                    </div>
                  ) : traceErrorCopy && !showingOfflineTrace ? (
                    <div role="alert" style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--probe)', fontFamily: 'var(--font-code)', fontSize: '0.9rem', padding: '24px', textAlign: 'center' }}>
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
              <div role="status" style={{ padding: '4px 12px', fontSize: '0.72rem', fontFamily: 'var(--font-code)', color: 'var(--probe)' }}>
                Live trace unavailable. Showing the checked-in offline sample.
              </div>
            )}

            {traceTruncated && (
              <div style={{ padding: '4px 12px', fontSize: '0.72rem', fontFamily: 'var(--font-code)', color: 'var(--probe)' }}>
                This trace hit the step budget and was cut short — try a smaller input for the full run.
              </div>
            )}

            {/* DP tables need the full stage; their cell states already show the recurrence. */}
            {activeDsType !== 'DpTable' && (
              <CaptureStrip
                steps={steps}
                current={currentStepIndex}
                onSeek={seek}
              />
            )}

            {/* Integrated Playback Controls */}
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
              onSpeedChange={setSpeed}
            />

            {/* Quiet Live Trace Banner */}
            <div style={{ padding: '0 12px 10px 12px' }}>
              <LiveTraceTicker stepDescription={currentStep?.description} />
            </div>
          </div>

          {/* Desktop Bottom Section: Wide Java Code + Input Panel + Right Tabbed Memory/Complexity Card */}
          {!isMobile ? (
            <div style={{
              height: '210px', minHeight: '210px', display: 'grid',
              gridTemplateColumns: hasInputSpec ? '1.2fr 1fr 1fr' : '1.6fr 1fr',
              gap: '12px', overflow: 'hidden', flexShrink: 0
            }}>
              <CodeViewer problem={activeProblem} currentStep={currentStep} />
              {hasInputSpec && (
                <div className="glass-panel" style={{ padding: '10px 12px', overflow: 'hidden' }}>
                  <InputPanel
                    problemId={activeProblemId}
                    inputSpec={activeProblem.inputSpec}
                    fieldErrors={fieldErrors}
                    running={traceLoading}
                    onRun={runInput}
                  />
                </div>
              )}
              <MemoryComplexityCard currentStep={currentStep} problem={activeProblem} />
            </div>
          ) : (
            /* Mobile Tab Bottom Card Section (Code / Input / Memory / Complexity) */
            <div className="glass-panel" style={{ height: '180px', minHeight: '180px', display: 'flex', flexDirection: 'column', overflow: 'hidden', flexShrink: 0 }}>
              <div style={{ display: 'flex', padding: '4px', gap: '4px', borderBottom: '1px solid var(--border-default)', background: 'rgba(0,0,0,0.2)' }}>
                <button
                  onClick={() => setActiveTab('code')}
                  className={`btn ${activeTab === 'code' ? 'btn-primary' : 'btn-outline'}`}
                  style={{ flex: 1, padding: '4px 8px', fontSize: '0.74rem', justifyContent: 'center' }}
                >
                  Code
                </button>
                {hasInputSpec && (
                  <button
                    onClick={() => setActiveTab('input')}
                    className={`btn ${activeTab === 'input' ? 'btn-primary' : 'btn-outline'}`}
                    style={{ flex: 1, padding: '4px 8px', fontSize: '0.74rem', justifyContent: 'center' }}
                  >
                    Input
                  </button>
                )}
                <button
                  onClick={() => setActiveTab('memory')}
                  className={`btn ${activeTab === 'memory' ? 'btn-primary' : 'btn-outline'}`}
                  style={{ flex: 1, padding: '4px 8px', fontSize: '0.74rem', justifyContent: 'center' }}
                >
                  Memory
                </button>
                <button
                  onClick={() => setActiveTab('complexity')}
                  className={`btn ${activeTab === 'complexity' ? 'btn-primary' : 'btn-outline'}`}
                  style={{ flex: 1, padding: '4px 8px', fontSize: '0.74rem', justifyContent: 'center' }}
                >
                  Complexity
                </button>
              </div>

              <div style={{ flex: 1, overflow: 'hidden' }}>
                {activeTab === 'code' ? (
                  <CodeViewer problem={activeProblem} currentStep={currentStep} />
                ) : activeTab === 'input' ? (
                  <div style={{ padding: '10px 12px', height: '100%', overflow: 'hidden' }}>
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
          )}
        </main>
      </div>
    </div>
  );
}
