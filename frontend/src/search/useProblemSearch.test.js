import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useProblemSearch } from './useProblemSearch';

describe('useProblemSearch hook', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.restoreAllMocks();
  });

  // Test 11: runnableOnly filters to traced === true
  it('filters results to traced === true when runnableOnly is enabled', () => {
    const problems = [
      { id: 'two-sum', title: 'Two Sum', category: 'Arrays', traced: true },
      { id: 'three-sum', title: '3Sum', category: 'Arrays', traced: false },
      { id: 'kadane', title: "Kadane's Algorithm", category: 'Arrays' } // undefined traced
    ];

    const { result } = renderHook(() => useProblemSearch({ problems }));

    expect(result.current.results).toHaveLength(3);

    act(() => {
      result.current.setRunnableOnly(true);
    });

    expect(result.current.results).toHaveLength(1);
    expect(result.current.results[0].id).toBe('two-sum');
    expect(result.current.results[0].traced).toBe(true);
  });

  // Test 12: activeIndex resets to -1 when query changes
  it('resets activeIndex to -1 when the query changes', () => {
    const problems = [
      { id: 'two-sum', title: 'Two Sum', category: 'Arrays' },
      { id: 'three-sum', title: '3Sum', category: 'Arrays' }
    ];

    const { result } = renderHook(() => useProblemSearch({ problems }));

    act(() => {
      result.current.setActiveIndex(1);
    });
    expect(result.current.activeIndex).toBe(1);

    act(() => {
      result.current.setQuery('sum');
    });
    expect(result.current.activeIndex).toBe(-1);
  });

  // Test 13: commitRecent writes to localStorage, caps at 5, and de-duplicates case-insensitively
  it('writes to localStorage, caps at 5, and de-duplicates case-insensitively on commitRecent', () => {
    const { result } = renderHook(() => useProblemSearch({ problems: [] }));

    act(() => {
      result.current.commitRecent('Two Sum');
      result.current.commitRecent('Binary Search');
      result.current.commitRecent('two sum'); // duplicate case-insensitive
      result.current.commitRecent('BFS');
      result.current.commitRecent('DFS');
      result.current.commitRecent('DP');
      result.current.commitRecent('Graph');
    });

    expect(result.current.recents).toHaveLength(5);
    // Most recent first: Graph, DP, DFS, BFS, two sum
    expect(result.current.recents).toEqual(['Graph', 'DP', 'DFS', 'BFS', 'two sum']);

    const stored = JSON.parse(localStorage.getItem('dsa:recentSearches'));
    expect(stored).toEqual(['Graph', 'DP', 'DFS', 'BFS', 'two sum']);
  });

  // Test 14: Recents survive a hook remount
  it('preserves recents across hook remounts', () => {
    const { result: r1, unmount } = renderHook(() => useProblemSearch({ problems: [] }));

    act(() => {
      r1.current.commitRecent('Kadane');
      r1.current.commitRecent('Dijkstra');
    });

    expect(r1.current.recents).toEqual(['Dijkstra', 'Kadane']);
    unmount();

    const { result: r2 } = renderHook(() => useProblemSearch({ problems: [] }));
    expect(r2.current.recents).toEqual(['Dijkstra', 'Kadane']);
  });

  // Test 15: Throwing localStorage degrades to [] without crashing
  it('degrades to [] without crashing when localStorage throws', () => {
    vi.spyOn(Storage.prototype, 'getItem').mockImplementation(() => {
      throw new Error('SecurityError: The operation is insecure.');
    });

    let hookResult;
    expect(() => {
      hookResult = renderHook(() => useProblemSearch({ problems: [] }));
    }).not.toThrow();

    expect(hookResult.result.current.recents).toEqual([]);

    // Also verify commitRecent doesn't crash when setItem throws
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('QuotaExceededError');
    });

    expect(() => {
      act(() => {
        hookResult.result.current.commitRecent('Test');
      });
    }).not.toThrow();
  });

});
