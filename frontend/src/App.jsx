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
  const [activeCategory, setActiveCategory] = useState('Graph BFS/DFS');
  const [activeProblemId, setActiveProblemId] = useState('bfs-traversal');
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
    if (activeProblemId) {
      fetchProblemDetailsAndSteps(activeProblemId);
    }
  }, [activeProblemId]);

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

  const fetchAllProblems = async () => {
    try {
      setLoading(true);
      const [resBfs, resAdv, resTree, resRec, resSort, resArr, resLl, resBs, resDp, resTrie, resGreedy, resStr, resBit, resHeap] = await Promise.allSettled([
        fetch('/api/graphs/bfs-dfs/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/graphs/advanced/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/trees/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/recursion-backtracking/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/sorting/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/arrays/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/linkedlist/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/binarysearch/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/dp/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/tries/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/greedy/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/strings/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/bitmanipulation/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/heaps/problems').then(r => r.ok ? r.json() : [])
      ]);

      const combined = [
        ...(resBfs.status === 'fulfilled' ? resBfs.value : []),
        ...(resAdv.status === 'fulfilled' ? resAdv.value : []),
        ...(resTree.status === 'fulfilled' ? resTree.value : []),
        ...(resRec.status === 'fulfilled' ? resRec.value : []),
        ...(resSort.status === 'fulfilled' ? resSort.value : []),
        ...(resArr.status === 'fulfilled' ? resArr.value : []),
        ...(resLl.status === 'fulfilled' ? resLl.value : []),
        ...(resBs.status === 'fulfilled' ? resBs.value : []),
        ...(resDp.status === 'fulfilled' ? resDp.value : []),
        ...(resTrie.status === 'fulfilled' ? resTrie.value : []),
        ...(resGreedy.status === 'fulfilled' ? resGreedy.value : []),
        ...(resStr.status === 'fulfilled' ? resStr.value : []),
        ...(resBit.status === 'fulfilled' ? resBit.value : []),
        ...(resHeap.status === 'fulfilled' ? resHeap.value : [])
      ];

      if (combined.length > 0) {
        setProblems(combined);
        setActiveProblemId(combined[0].id);
      }
    } catch (err) {
      console.warn('Backend connection failed:', err);
    } finally {
      setLoading(false);
    }
  };

  const fetchProblemDetailsAndSteps = async (id) => {
    try {
      setIsPlaying(false);
      setCurrentStepIndex(0);

      let endpoint = `/api/graphs/bfs-dfs`;
      const prob = problems.find(p => p.id === id);
      if (prob) {
        if (prob.category === 'Advanced Graphs') endpoint = `/api/graphs/advanced`;
        else if (prob.category === 'Binary Trees' || prob.category === 'Binary Search Trees') endpoint = `/api/trees`;
        else if (prob.category === 'Recursion & Backtracking') endpoint = `/api/recursion-backtracking`;
        else if (prob.category === 'Sorting Algorithms') endpoint = `/api/sorting`;
        else if (prob.category === 'Arrays') endpoint = `/api/arrays`;
        else if (prob.category === 'Linked List') endpoint = `/api/linkedlist`;
        else if (prob.category === 'Binary Search') endpoint = `/api/binarysearch`;
        else if (prob.category === 'Dynamic Programming') endpoint = `/api/dp`;
        else if (prob.category === 'Tries & Prefixes') endpoint = `/api/tries`;
        else if (prob.category === 'Greedy Algorithms') endpoint = `/api/greedy`;
        else if (prob.category === 'Strings') endpoint = `/api/strings`;
        else if (prob.category === 'Bit Manipulation') endpoint = `/api/bitmanipulation`;
        else if (prob.category === 'Heaps & PriorityQueue') endpoint = `/api/heaps`;
      }

      const [probRes, stepsRes] = await Promise.allSettled([
        fetch(`${endpoint}/problems/${id}`).then(r => r.ok ? r.json() : null),
        fetch(`${endpoint}/execute/${id}`).then(r => r.ok ? r.json() : [])
      ]);

      if (probRes.status === 'fulfilled' && probRes.value) {
        setActiveProblem(probRes.value);
      }
      if (stepsRes.status === 'fulfilled' && stepsRes.value) {
        setSteps(stepsRes.value);
      }
    } catch (err) {
      console.error('Error fetching details/steps:', err);
    }
  };

  // Global Keyboard Shortcuts (Space: Play/Pause, ArrowLeft: Prev, ArrowRight: Next, R: Reset)
  useEffect(() => {
    const handleKeyDown = (e) => {
      const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
      if (activeTag === 'input' || activeTag === 'textarea' || activeTag === 'select') return;

      if (e.code === 'Space') {
        e.preventDefault();
        setIsPlaying(prev => !prev);
      } else if (e.code === 'ArrowLeft') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(prev => Math.max(0, prev - 1));
      } else if (e.code === 'ArrowRight') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(prev => Math.min((steps.length || 1) - 1, prev + 1));
      } else if (e.code === 'KeyR') {
        e.preventDefault();
        setIsPlaying(false);
        setCurrentStepIndex(0);
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    return () => window.removeEventListener('keydown', handleKeyDown);
  }, [steps.length]);

  const currentStep = steps[currentStepIndex] || null;

  const renderCanvas = () => {
    if (!activeProblem) return <GraphCanvas problem={activeProblem} currentStep={currentStep} />;

    // Matrix / Grid / Chessboard problems render GraphCanvas (4x4 Chessboard, 9x9 Sudoku, 2D Grid)
    if (activeProblem.id === 'n-queens' || activeProblem.id === 'sudoku-solver' || activeProblem.id === 'rat-in-a-maze' || activeProblem.dsType === 'Matrix' || currentStep?.gridState || activeProblem.defaultGrid) {
      return <GraphCanvas problem={activeProblem} currentStep={currentStep} />;
    }

    if (activeProblem.category === 'Recursion & Backtracking' || activeProblem.category === 'Tries & Prefixes' || (activeProblem.defaultTreeNodes && activeProblem.defaultTreeNodes.length > 0 && activeProblem.category === 'Sorting Algorithms')) {
      return <RecursionTreeCanvas problem={activeProblem} currentStep={currentStep} />;
    }

    switch (activeProblem.category) {
      case 'Binary Trees':
      case 'Binary Search Trees':
        return <TreeCanvas problem={activeProblem} currentStep={currentStep} />;
      case 'Sorting Algorithms':
      case 'Arrays':
      case 'Binary Search':
      case 'Greedy Algorithms':
      case 'Strings':
      case 'Bit Manipulation':
      case 'Heaps & PriorityQueue':
        return <ArrayCanvas problem={activeProblem} currentStep={currentStep} />;
      case 'Linked List':
        return <LinkedListCanvas problem={activeProblem} currentStep={currentStep} />;
      default:
        return <GraphCanvas problem={activeProblem} currentStep={currentStep} />;
    }
  };

  return (
    <div style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', paddingBottom: '40px' }}>
      <Header totalProblems={problems.length} />

      <div style={{ flex: 1, display: 'flex', gap: '20px', margin: '20px 24px 0 24px', overflow: 'hidden' }}>
        {/* Sidebar */}
        <Sidebar
          problems={problems}
          activeProblemId={activeProblemId}
          activeCategory={activeCategory}
          onSelectCategory={(cat) => {
            setActiveCategory(cat);
            const firstInCat = problems.find(p => p.category === cat);
            if (firstInCat) setActiveProblemId(firstInCat.id);
          }}
          onSelectProblem={(id) => setActiveProblemId(id)}
        />

        {/* Main Content Workspace */}
        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '20px', minWidth: 0, overflowY: 'auto' }}>
          {/* Header Info Banner */}
          {activeProblem && (
            <div className="glass-panel" style={{ padding: '16px 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                  <h2 style={{ fontSize: '1.25rem', fontWeight: '800' }}>{activeProblem.title}</h2>
                  <span className={`badge badge-${activeProblem.difficulty?.toLowerCase()}`}>
                    {activeProblem.difficulty}
                  </span>
                </div>
                <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                  {activeProblem.description}
                </p>
              </div>

              <button className="btn btn-secondary" onClick={fetchAllProblems} title="Synchronized Engine Status" style={{ fontSize: '0.8rem' }}>
                <RefreshCw size={14} /> Synchronized
              </button>
            </div>
          )}

          {/* Controls Bar */}
          <Controls
            isPlaying={isPlaying}
            currentStepIndex={currentStepIndex}
            totalSteps={steps.length}
            speed={speed}
            onPlayPause={() => setIsPlaying(!isPlaying)}
            onStepNext={() => setCurrentStepIndex((i) => Math.min(steps.length - 1, i + 1))}
            onStepPrev={() => setCurrentStepIndex((i) => Math.max(0, i - 1))}
            onStepSelect={(idx) => setCurrentStepIndex(idx)}
            onReset={() => { setIsPlaying(false); setCurrentStepIndex(0); }}
            onSpeedChange={(val) => setSpeed(val)}
            stepDescription={currentStep?.description}
          />

          {/* Visualizers Grid */}
          <div style={{ display: 'flex', gap: '20px', flexWrap: 'wrap' }}>
            {renderCanvas()}
            <DataStructurePanel currentStep={currentStep} dsType={activeProblem?.dsType || 'Stack'} />
          </div>

          {/* Code Viewer */}
          <CodeViewer problem={activeProblem} currentStep={currentStep} />

          {/* Complexity Explanation Panel */}
          {activeProblem?.complexity && (
            <ComplexityPanel complexity={activeProblem.complexity} />
          )}
        </main>
      </div>
    </div>
  );
}
