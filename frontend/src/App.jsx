import React, { useState, useEffect, useRef } from 'react';
import Header from './components/Header';
import Sidebar from './components/Sidebar';
import GraphCanvas from './components/GraphCanvas';
import TreeCanvas from './components/TreeCanvas';
import ArrayCanvas from './components/ArrayCanvas';
import LinkedListCanvas from './components/LinkedListCanvas';
import RecursionTreeCanvas from './components/RecursionTreeCanvas';
import DataStructurePanel from './components/DataStructurePanel';
import CodeViewer from './components/CodeViewer';
import ComplexityPanel from './components/ComplexityPanel';
import Controls from './components/Controls';
import { RefreshCw } from 'lucide-react';

export default function App() {
  const [problems, setProblems] = useState([]);
  const [activeCategory, setActiveCategory] = useState(null);
  const [activeProblemId, setActiveProblemId] = useState('two-sum');
  const [activeProblem, setActiveProblem] = useState(null);
  const [steps, setSteps] = useState([]);
  const [currentStepIndex, setCurrentStepIndex] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [speed, setSpeed] = useState(800);
  const [loading, setLoading] = useState(true);

  const timerRef = useRef(null);

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
        { url: '/api/math/basic/problems', base: '/api/math/basic' },
        { url: '/api/recursion/basic/problems', base: '/api/recursion/basic' }
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
        setActiveProblemId(initialId);
        fetchProblemDetailsAndSteps(initialId, combined);
      }
    } catch (err) {
      console.warn('Backend connection failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchProblemDetailsAndSteps = async (id, probList = problems) => {
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

      const fetchedProblem = (probRes.status === 'fulfilled' && probRes.value) ? probRes.value : prob;
      setActiveProblem(fetchedProblem);

      if (stepsRes.status === 'fulfilled' && stepsRes.value && stepsRes.value.length > 0) {
        setSteps(stepsRes.value);
      } else if (fetchedProblem && fetchedProblem.executionSteps && fetchedProblem.executionSteps.length > 0) {
        setSteps(fetchedProblem.executionSteps);
      } else {
        // Safe fallback step
        setSteps([{
          stepIndex: 1,
          lineNumber: 1,
          description: `Interactive Execution Visualizer for ${fetchedProblem?.title || id}.`,
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

  const [isSidebarOpen, setIsSidebarOpen] = useState(window.innerWidth > 1024);
  const [activeTab, setActiveTab] = useState('canvas'); // 'canvas' | 'code' | 'diagnostics'
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);

  useEffect(() => {
    const handleResize = () => {
      setViewportWidth(window.innerWidth);
      if (window.innerWidth > 1024) {
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
    fetchProblemDetailsAndSteps(id);
    if (viewportWidth <= 1024) {
      setIsSidebarOpen(false);
    }
  };

  const currentStep = steps[currentStepIndex] || null;
  const isMobileOrTablet = viewportWidth <= 1024;

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
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', overflow: 'hidden', background: 'var(--bg-primary)' }}>
      <Header 
        problem={activeProblem} 
        totalProblems={problems.length} 
        isSidebarOpen={isSidebarOpen}
        onToggleSidebar={() => setIsSidebarOpen(prev => !prev)}
      />

      {/* Main Container */}
      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', padding: 'var(--space-sm) var(--space-md)', gap: 'var(--space-md)', position: 'relative' }}>
        {/* Sidebar (Responsive Desktop / Mobile Drawer) */}
        {isSidebarOpen && (
          <div style={{
            position: isMobileOrTablet ? 'absolute' : 'relative',
            top: isMobileOrTablet ? 0 : 'auto',
            left: isMobileOrTablet ? 0 : 'auto',
            bottom: isMobileOrTablet ? 0 : 'auto',
            zIndex: isMobileOrTablet ? 100 : 1,
            height: isMobileOrTablet ? '100%' : 'auto',
            boxShadow: isMobileOrTablet ? '0 0 40px rgba(0,0,0,0.8)' : 'none'
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

        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 'var(--space-sm)', overflow: 'hidden' }}>
          {/* Mobile / Tablet Tab Selector Bar */}
          {isMobileOrTablet && (
            <div className="glass-panel" style={{ display: 'flex', padding: '4px', gap: '4px', flexShrink: 0 }}>
              <button 
                onClick={() => setActiveTab('canvas')}
                className={`btn ${activeTab === 'canvas' ? 'btn-primary' : 'btn-secondary'}`} 
                style={{ flex: 1, padding: '6px 10px', fontSize: '0.78rem', justifyContent: 'center' }}
              >
                🎨 Canvas Stage
              </button>
              <button 
                onClick={() => setActiveTab('code')}
                className={`btn ${activeTab === 'code' ? 'btn-primary' : 'btn-secondary'}`} 
                style={{ flex: 1, padding: '6px 10px', fontSize: '0.78rem', justifyContent: 'center' }}
              >
                💻 Solution & Code
              </button>
              <button 
                onClick={() => setActiveTab('diagnostics')}
                className={`btn ${activeTab === 'diagnostics' ? 'btn-primary' : 'btn-secondary'}`} 
                style={{ flex: 1, padding: '6px 10px', fontSize: '0.78rem', justifyContent: 'center' }}
              >
                📊 Memory & Proof
              </button>
            </div>
          )}

          {/* Desktop Layout OR Active Tab View */}
          {(!isMobileOrTablet || activeTab === 'canvas') && (
            <div className="glass-panel" style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0 }}>
              {/* Visualizer Canvas Area */}
              <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden', minHeight: 0 }}>
                {loading ? (
                  <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-indigo)', gap: '10px' }}>
                    <RefreshCw size={24} className="spin" />
                    <span style={{ fontWeight: '700' }}>Loading Algorithm Engine & Catalog...</span>
                  </div>
                ) : (
                  renderCanvas()
                )}
              </div>

              {/* Pinned Execution Controls Toolbar */}
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
                stepDescription={currentStep?.description}
              />
            </div>
          )}

          {/* Code Viewer View for Mobile/Tablet */}
          {isMobileOrTablet && activeTab === 'code' && (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', overflow: 'hidden' }}>
              <CodeViewer problem={activeProblem} currentStep={currentStep} />
            </div>
          )}

          {/* Diagnostics View for Mobile/Tablet */}
          {isMobileOrTablet && activeTab === 'diagnostics' && (
            <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '10px', overflowY: 'auto' }}>
              <DataStructurePanel currentStep={currentStep} dsType={activeProblem?.dsType || 'Array'} />
              <ComplexityPanel complexity={activeProblem?.complexity} problem={activeProblem} />
            </div>
          )}

          {/* Desktop 3-Column Bottom Diagnostics Dashboard */}
          {!isMobileOrTablet && (
            <div style={{ height: '220px', minHeight: '220px', display: 'grid', gridTemplateColumns: '1fr 1.2fr 1fr', gap: 'var(--space-md)', overflow: 'hidden', flexShrink: 0 }}>
              <DataStructurePanel 
                currentStep={currentStep} 
                dsType={activeProblem?.dsType || (activeProblem?.category?.includes('Graph') ? 'Queue' : 'Stack')} 
              />
              <CodeViewer 
                problem={activeProblem} 
                currentStep={currentStep} 
              />
              <ComplexityPanel 
                complexity={activeProblem?.complexity} 
                problem={activeProblem}
              />
            </div>
          )}
        </main>
      </div>
    </div>
  );
}
