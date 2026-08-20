import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import Breadcrumb from './components/Breadcrumb';
import Sidebar from './components/Sidebar';
import GraphCanvas from './components/GraphCanvas';
import TreeCanvas from './components/TreeCanvas';
import ArrayCanvas from './components/ArrayCanvas';
import LinkedListCanvas from './components/LinkedListCanvas';
import RecursionTreeCanvas from './components/RecursionTreeCanvas';
import CodeViewer from './components/CodeViewer';
import MemoryComplexityCard from './components/MemoryComplexityCard';
import Controls from './components/Controls';
import LiveTraceTicker from './components/LiveTraceTicker';
import { RefreshCw } from 'lucide-react';

const DEFAULT_FALLBACK_PROBLEMS = [
  {
    id: 'two-sum',
    title: 'Two Sum',
    category: 'Arrays & Hashing',
    subcategory: 'Array',
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
    subcategory: 'Window',
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
  const [activeProblem, setActiveProblem] = useState(DEFAULT_FALLBACK_PROBLEMS[0]);
  const [steps, setSteps] = useState(DEFAULT_FALLBACK_PROBLEMS[0].executionSteps);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(800);
  const [loading, setLoading] = useState(false);

  const timerRef = useRef(null);
  // Monotonic request counter: a slower earlier response must never overwrite a newer one.
  const requestIdRef = useRef(0);

  useEffect(() => {
    fetchAllProblems();
  }, []);

  useEffect(() => {
    if (activeProblemId && problems.length > 0) {
      fetchProblemDetailsAndSteps(activeProblemId);
    }
  }, [activeProblemId, problems.length]);

  useEffect(() => {
    if (isPlaying) {
      timerRef.current = setInterval(() => {
        setCurrentStepIndex((prevIdx) => {
          if (prevIdx >= steps.length - 1) {
            setIsPlaying(false);
            return prevIdx;
          }
          return prevIdx + 1;
        });
      }, speed);
    } else {
      clearInterval(timerRef.current);
    }
    return () => clearInterval(timerRef.current);
  }, [isPlaying, speed, steps.length]);

  // Global Keyboard Shortcuts (Space: Play/Pause, Right: Next, Left: Prev, R: Reset)
  useEffect(() => {
    const handleKeyDown = (e) => {
      if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;

      if (e.code === 'Space') {
        e.preventDefault();
        setIsPlaying(prev => !prev);
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(p => Math.min(p + 1, steps.length - 1));
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(p => Math.max(p - 1, 0));
      } else if (e.code === 'KeyR') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(0);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [steps.length]);

  const fetchAllProblems = async () => {
    try {
      setLoading(true);
      const endpoints = [
        { url: '/api/graphs/bfs-dfs/problems', base: '/api/graphs/bfs-dfs' },
        { url: '/api/graphs/advanced/problems', base: '/api/graphs/advanced' },
        { url: '/api/trees/problems', base: '/api/trees' },
        { url: '/api/recursion-backtracking/problems', base: '/api/recursion-backtracking' },
        { url: '/api/sorting/problems', base: '/api/sorting' },
        { url: '/api/arrays/problems', base: '/api/arrays' },
        { url: '/api/linkedlist/problems', base: '/api/linkedlist' },
        { url: '/api/binarysearch/problems', base: '/api/binarysearch' },
        { url: '/api/dp/problems', base: '/api/dp' },
        { url: '/api/tries/problems', base: '/api/tries' },
        { url: '/api/greedy/problems', base: '/api/greedy' },
        { url: '/api/strings/problems', base: '/api/strings' },
        { url: '/api/bitmanipulation/problems', base: '/api/bitmanipulation' },
        { url: '/api/heaps/problems', base: '/api/heaps' },
        { url: '/api/stackqueue/problems', base: '/api/stackqueue' },
        { url: '/api/slidingwindow/problems', base: '/api/slidingwindow' },
        { url: '/api/maths/problems', base: '/api/maths' },
        { url: '/api/basic-recursion/problems', base: '/api/basic-recursion' }
      ];

      const results = await Promise.allSettled(
        endpoints.map(ep => fetch(ep.url).then(r => r.ok ? r.json() : []))
      );

      const combined = [];
      results.forEach((res, idx) => {
        if (res.status === 'fulfilled' && Array.isArray(res.value)) {
          res.value.forEach(item => {
            combined.push({
              ...item,
              _endpoint: endpoints[idx].base
            });
          });
        }
      });

      if (combined.length > 0) {
        setProblems(combined);
        const initialId = combined.find(p => p.id === 'two-sum')?.id || combined[0].id;
        // Setting these is enough: the [activeProblemId, problems.length] effect
        // owns fetching details/steps. Calling it here too would double-request.
        setActiveProblemId(initialId);
      }
    } catch (err) {
      console.warn('Backend connection failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchProblemDetailsAndSteps = async (id, probList = problems) => {
    const requestId = ++requestIdRef.current;
    try {
      setIsPlaying(false);
      setCurrentStepIndex(0);

      const prob = probList.find(p => p.id === id);
      let endpoint = prob?._endpoint;

      if (!endpoint && prob) {
        const cat = prob.category || '';
        if (cat.includes('Advanced Graphs')) endpoint = `/api/graphs/advanced`;
        else if (cat.includes('Binary Trees') || cat.includes('BST') || cat.includes('Tree')) endpoint = `/api/trees`;
        else if (cat.includes('Recursion') || cat.includes('Backtracking')) endpoint = `/api/recursion-backtracking`;
        else if (cat.includes('Sort')) endpoint = `/api/sorting`;
        else if (cat.includes('Array')) endpoint = `/api/arrays`;
        else if (cat.includes('List') || cat.includes('Linked')) endpoint = `/api/linkedlist`;
        else if (cat.includes('Binary Search')) endpoint = `/api/binarysearch`;
        else if (cat.includes('DP') || cat.includes('Dynamic')) endpoint = `/api/dp`;
        else if (cat.includes('Trie')) endpoint = `/api/tries`;
        else if (cat.includes('Greedy')) endpoint = `/api/greedy`;
        else if (cat.includes('String')) endpoint = `/api/strings`;
        else if (cat.includes('Bit') || cat.includes('Math')) endpoint = `/api/bitmanipulation`;
        else if (cat.includes('Heap') || cat.includes('Priority')) endpoint = `/api/heaps`;
        else if (cat.includes('Stack') || cat.includes('Queue')) endpoint = `/api/stackqueue`;
        else if (cat.includes('Sliding Window') || cat.includes('Window')) endpoint = `/api/slidingwindow`;
        else endpoint = `/api/graphs/bfs-dfs`;
      }

      if (!endpoint) endpoint = `/api/graphs/bfs-dfs`;

      const [probRes, stepsRes] = await Promise.allSettled([
        fetch(`${endpoint}/problems/${id}`).then(r => r.ok ? r.json() : null),
        fetch(`${endpoint}/execute/${id}`).then(r => r.ok ? r.json() : [])
      ]);

      // A newer selection landed while these were in flight — discard this response.
      if (requestId !== requestIdRef.current) return;

      const fetchedProblem = (probRes.status === 'fulfilled' && probRes.value) ? probRes.value : prob;
      setActiveProblem(fetchedProblem);

      if (stepsRes.status === 'fulfilled' && stepsRes.value && stepsRes.value.length > 0) {
        setSteps(stepsRes.value);
      } else if (fetchedProblem && fetchedProblem.executionSteps && fetchedProblem.executionSteps.length > 0) {
        setSteps(fetchedProblem.executionSteps);
      } else {
        setSteps([{
          stepNumber: 1,
          activeLine: 1,
          description: `Interactive execution visualizer for ${fetchedProblem?.title || id}.`,
          queueOrStackState: [],
          nodeStates: {},
          activeEdges: [],
          variables: { Status: "Loaded", Algorithm: fetchedProblem?.title || id },
          dsType: fetchedProblem?.dsType || 'Array'
        }]);
      }
    } catch (err) {
      console.error('Error fetching details/steps:', err);
    }
  };

  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 768);
  const [activeTab, setActiveTab] = useState('canvas'); // 'canvas' | 'code' | 'memory' | 'complexity'
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

  const currentStep = steps[currentStepIndex] || null;
  const isMobile = viewportWidth <= 768;

  const renderCanvas = () => {
    if (!activeProblem) return null;
    const cat = activeProblem.category || '';
    const dsType = activeProblem.dsType || '';

    if (cat.includes('Trees') || cat.includes('BST') || dsType === 'Tree') {
      return <TreeCanvas currentStep={currentStep} step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('Recursion') || cat.includes('Backtracking') || dsType === 'RecursionTree') {
      return <RecursionTreeCanvas currentStep={currentStep} step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('List') || cat.includes('Linked') || dsType === 'LinkedList') {
      return <LinkedListCanvas currentStep={currentStep} step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('Array') || cat.includes('Sort') || cat.includes('Binary Search') || cat.includes('DP') || cat.includes('Heap') || cat.includes('Greedy') || cat.includes('Bit') || cat.includes('Stack') || cat.includes('Queue') || cat.includes('Window') || dsType === 'Array' || dsType === 'Matrix') {
      return <ArrayCanvas currentStep={currentStep} step={currentStep} problem={activeProblem} />;
    } else {
      return <GraphCanvas currentStep={currentStep} step={currentStep} problem={activeProblem} />;
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
              ) : (
                renderCanvas()
              )}
            </div>

            {/* Integrated Playback Controls */}
            <Controls
              isPlaying={isPlaying}
              currentStepIndex={currentStepIndex}
              totalSteps={steps.length}
              speed={speed}
              onPlayPause={() => setIsPlaying(prev => !prev)}
              onStepNext={() => setCurrentStepIndex(p => Math.min(p + 1, steps.length - 1))}
              onStepPrev={() => setCurrentStepIndex(p => Math.max(p - 1, 0))}
              onStepSelect={(idx) => { setIsPlaying(false); setCurrentStepIndex(idx); }}
              onReset={() => { setIsPlaying(false); setCurrentStepIndex(0); }}
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
