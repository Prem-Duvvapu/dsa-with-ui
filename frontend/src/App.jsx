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
      const [
        resBfs, resAdv, resTree, resRec, resSort, resArr, resLl, resBs, 
        resDp, resTrie, resGreedy, resStr, resBit, resHeap, resSq, resSw, resMath, resBasicRec
      ] = await Promise.allSettled([
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
        fetch('/api/heaps/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/stackqueue/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/slidingwindow/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/math/basic/problems').then(r => r.ok ? r.json() : []),
        fetch('/api/recursion/basic/problems').then(r => r.ok ? r.json() : [])
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
        ...(resHeap.status === 'fulfilled' ? resHeap.value : []),
        ...(resSq.status === 'fulfilled' ? resSq.value : []),
        ...(resSw.status === 'fulfilled' ? resSw.value : []),
        ...(resMath.status === 'fulfilled' ? resMath.value : []),
        ...(resBasicRec.status === 'fulfilled' ? resBasicRec.value : [])
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
      }

      const [probRes, stepsRes] = await Promise.allSettled([
        fetch(`${endpoint}/problems/${id}`).then(r => r.ok ? r.json() : null),
        fetch(`${endpoint}/execute/${id}`).then(r => r.ok ? r.json() : [])
      ]);

      if (probRes.status === 'fulfilled' && probRes.value) {
        setActiveProblem(probRes.value);
      } else if (prob) {
        setActiveProblem(prob);
      }

      if (stepsRes.status === 'fulfilled' && stepsRes.value && stepsRes.value.length > 0) {
        setSteps(stepsRes.value);
      } else if (prob && prob.executionSteps && prob.executionSteps.length > 0) {
        setSteps(prob.executionSteps);
      }
    } catch (err) {
      console.error('Error fetching details/steps:', err);
    }
  };

  const handleSelectCategory = (cat) => {
    setActiveCategory(cat);
  };

  const handleSelectProblem = (id) => {
    setActiveProblemId(id);
  };

  const currentStep = steps[currentStepIndex] || null;

  const renderCanvas = () => {
    if (!activeProblem) return null;
    const cat = activeProblem.category || '';
    const dsType = activeProblem.dsType || '';

    if (cat.includes('Trees') || cat.includes('BST') || dsType === 'Tree') {
      return <TreeCanvas step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('Recursion') || cat.includes('Backtracking') || dsType === 'RecursionTree') {
      return <RecursionTreeCanvas step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('List') || cat.includes('Linked') || dsType === 'LinkedList') {
      return <LinkedListCanvas step={currentStep} problem={activeProblem} />;
    } else if (cat.includes('Array') || cat.includes('Sort') || cat.includes('Binary Search') || cat.includes('DP') || cat.includes('Heap') || cat.includes('Greedy') || cat.includes('Bit') || cat.includes('Stack') || cat.includes('Queue') || cat.includes('Window') || dsType === 'Array' || dsType === 'Matrix') {
      return <ArrayCanvas step={currentStep} problem={activeProblem} />;
    } else {
      return <GraphCanvas step={currentStep} problem={activeProblem} />;
    }
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '100vh', width: '100vw', overflow: 'hidden', background: 'var(--bg-dark)' }}>
      <Header problem={activeProblem} totalProblems={problems.length} />

      <div style={{ display: 'flex', flex: 1, overflow: 'hidden', padding: '12px', gap: '12px' }}>
        <Sidebar
          problems={problems}
          activeProblemId={activeProblemId}
          activeCategory={activeCategory}
          onSelectCategory={handleSelectCategory}
          onSelectProblem={handleSelectProblem}
        />

        <main style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: '12px', overflow: 'hidden' }}>
          {/* Main Visualizer Stage */}
          <div className="glass-panel" style={{ flex: 1, position: 'relative', overflow: 'hidden', display: 'flex', flexDirection: 'column' }}>
            {loading ? (
              <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', color: 'var(--accent-indigo)', gap: '10px' }}>
                <RefreshCw size={24} className="spin" />
                <span style={{ fontWeight: '700' }}>Loading Algorithm Engine & Catalog...</span>
              </div>
            ) : (
              renderCanvas()
            )}

            <Controls
              currentStepIndex={currentStepIndex}
              totalSteps={steps.length}
              isPlaying={isPlaying}
              speed={speed}
              onPlay={() => setIsPlaying(true)}
              onPause={() => setIsPlaying(false)}
              onStepForward={() => setCurrentStepIndex(p => Math.min(p + 1, steps.length - 1))}
              onStepBack={() => setCurrentStepIndex(p => Math.max(p - 1, 0))}
              onReset={() => { setIsPlaying(false); setCurrentStepIndex(0); }}
              onSpeedChange={setSpeed}
              stepDescription={currentStep?.description}
            />
          </div>

          {/* Bottom Diagnostics & Code Inspection Dashboard */}
          <div style={{ height: '320px', display: 'grid', gridTemplateColumns: '1.2fr 1fr 0.8fr', gap: '12px' }}>
            <DataStructurePanel step={currentStep} problem={activeProblem} />
            <CodeViewer code={activeProblem?.javaCode} currentStep={currentStep} />
            <ComplexityPanel problem={activeProblem} />
          </div>
        </main>
      </div>
    </div>
  );
}
