import React, { useState, useEffect, useCallback } from 'react';
import Header from './components/Header';
import Breadcrumb from './components/Breadcrumb';
import Sidebar from './components/Sidebar';
import GraphCanvas from './components/GraphCanvas';
import GridCanvas from './components/GridCanvas';
import DsuCanvas from './components/DsuCanvas';
import TreeCanvas from './components/TreeCanvas';
import ArrayCanvas from './components/ArrayCanvas';
import LinkedListCanvas from './components/LinkedListCanvas';
import RecursionTreeCanvas from './components/RecursionTreeCanvas';
import TrieCanvas from './components/TrieCanvas';
import CanvasShell from './components/CanvasShell';
import CaptureStrip from './components/CaptureStrip';
import CodeViewer from './components/CodeViewer';
import MemoryComplexityCard from './components/MemoryComplexityCard';
import Controls from './components/Controls';
import LiveTraceTicker from './components/LiveTraceTicker';
import useTrace from './hooks/useTrace';
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

export default function App() {
  const [problems, setProblems] = useState(DEFAULT_FALLBACK_PROBLEMS);
  const [activeCategory, setActiveCategory] = useState(null);
  const [activeProblemId, setActiveProblemId] = useState('two-sum');
  const [catalogLoading, setCatalogLoading] = useState(false);

  // The catalogue entry — summary fields only (id, title, category, dsType, traced).
  const catalogEntry = problems.find(p => p.id === activeProblemId) || problems[0] || null;

  // All playback state lives in useTrace.
  const {
    steps, currentStep, currentStepIndex,
    isPlaying, speed,
    loading: traceLoading,
    error: traceError,
    detail,
    togglePlay, stepNext, stepPrev, reset, seek, setSpeed
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
        setProblems(data);
        const initialId = data.find(p => p.id === 'two-sum')?.id || data[0].id;
        setActiveProblemId(initialId);
      }
    } catch (err) {
      console.warn('Backend connection failed:', err);
      // Fall through to DEFAULT_FALLBACK_PROBLEMS — the app is usable offline.
    } finally {
      setCatalogLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchAllProblems();
  }, [fetchAllProblems]);

  // ── Global keyboard shortcuts ────────────────────────────────────────────
  useEffect(() => {
    const handleKeyDown = (e) => {
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
  }, [togglePlay, stepNext, stepPrev, reset]);

  // ── Layout state ─────────────────────────────────────────────────────────
  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 768);
  const [activeTab, setActiveTab] = useState('code');
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);

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

  const handleSelectCategory = (cat) => {
    setActiveCategory(cat);
  };

  const handleSelectProblem = (id) => {
    setActiveProblemId(id);
    if (viewportWidth <= 768) {
      setIsSidebarOpen(false);
    }
  };

  const isMobile = viewportWidth <= 768;
  const loading = catalogLoading;

  // ── Canvas selection by dsType ───────────────────────────────────────────
  const renderCanvas = () => {
    if (!activeProblem) return null;

    const dsType = currentStep?.dsType || activeProblem.dsType || '';
    const props = { currentStep, step: currentStep, problem: activeProblem };

    // DSU is a special case within Graph problems — check the problem id/title.
    const isDsu = activeProblem.id === 'disjoint-set-dsu'
      || activeProblem.title?.toLowerCase().includes('disjoint set')
      || activeProblem.title?.toLowerCase().includes('dsu');
    if (isDsu) return <DsuCanvas {...props} />;

    // Grid/Matrix: if current step carries gridState, prefer the grid renderer.
    const hasGrid = currentStep?.gridState || activeProblem.defaultGrid;
    if (hasGrid && (dsType === 'Matrix' || dsType === 'Grid')) {
      return <GridCanvas {...props} />;
    }

    switch (dsType) {
      case 'Tree':
        return <TreeCanvas {...props} />;
      case 'LinkedList':
        return <LinkedListCanvas {...props} />;
      case 'RecursionTree':
        return <RecursionTreeCanvas {...props} />;
      case 'Trie':
        return <TrieCanvas {...props} />;
      case 'Graph':
      case 'Queue':
        // Graph-type problems with nodeStates or graph nodes
        if (hasGrid) return <GridCanvas {...props} />;
        return <GraphCanvas {...props} />;
      case 'Stack':
      case 'PriorityQueue':
      case 'Array':
      case 'Matrix':
      default:
        // For anything else that carries gridState, use GridCanvas
        if (hasGrid) return <GridCanvas {...props} />;
        return <ArrayCanvas {...props} />;
    }
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

      {/* Main Workspace Container */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', padding: '0 12px 8px 12px', gap: '12px', position: 'relative' }}>
        {/* Sidebar (Search & Explore Panel) */}
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
                  ) : (
                    renderCanvas()
                  )}
                </CanvasShell>
              ) : (
                renderCanvas()
              )}
            </div>

            {/* The whole run at once, under the single frame it belongs to. */}
            <CaptureStrip
              steps={steps}
              current={currentStepIndex}
              onSeek={seek}
            />

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

          {/* Desktop Bottom Section: Wide Java Code + Right Tabbed Memory/Complexity Card */}
          {!isMobile ? (
            <div style={{ height: '210px', minHeight: '210px', display: 'grid', gridTemplateColumns: '1.6fr 1fr', gap: '12px', overflow: 'hidden', flexShrink: 0 }}>
              <CodeViewer problem={activeProblem} currentStep={currentStep} />
              <MemoryComplexityCard currentStep={currentStep} problem={activeProblem} />
            </div>
          ) : (
            /* Mobile 3-Tab Bottom Card Section (Code / Memory / Complexity) */
            <div className="glass-panel" style={{ height: '180px', minHeight: '180px', display: 'flex', flexDirection: 'column', overflow: 'hidden', flexShrink: 0 }}>
              <div style={{ display: 'flex', padding: '4px', gap: '4px', borderBottom: '1px solid var(--border-default)', background: 'rgba(0,0,0,0.2)' }}>
                <button 
                  onClick={() => setActiveTab('code')}
                  className={`btn ${activeTab === 'code' ? 'btn-primary' : 'btn-outline'}`} 
                  style={{ flex: 1, padding: '4px 8px', fontSize: '0.74rem', justifyContent: 'center' }}
                >
                  Code
                </button>
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
