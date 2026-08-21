import { describe, it, expect } from 'vitest';
import { scoreProblem, searchProblems, matchRanges } from './scoreProblem';

describe('scoreProblem', () => {
  // Test 1: Exact title beats prefix beats word-prefix beats contains beats subsequence
  it('ranks exact title > prefix > word-prefix > contains > subsequence (asserts score ordering)', () => {
    const q = 'target';
    const exact = { title: 'target' };
    const prefix = { title: 'target word' };
    const wordPrefix = { title: 'some targetword' };
    const contains = { title: 'notargethere' };
    const subsequence = { title: 'xtxaargxext' };

    const scoreExact = scoreProblem(q, exact);
    const scorePrefix = scoreProblem(q, prefix);
    const scoreWordPrefix = scoreProblem(q, wordPrefix);
    const scoreContains = scoreProblem(q, contains);
    const scoreSubsequence = scoreProblem(q, subsequence);

    expect(scoreExact).toBeGreaterThan(scorePrefix);
    expect(scorePrefix).toBeGreaterThan(scoreWordPrefix);
    expect(scoreWordPrefix).toBeGreaterThan(scoreContains);
    expect(scoreContains).toBeGreaterThan(scoreSubsequence);
    expect(scoreSubsequence).toBeGreaterThan(0);
  });

  // Test 2: 'two sum' ranks 'Two Sum' first
  it('ranks Two Sum first out of Two Sum, Two Sum II, 3Sum, Two Pointers', () => {
    const fixture = [
      { id: 'two-pointers', title: 'Two Pointers' },
      { id: '3sum', title: '3Sum' },
      { id: 'two-sum-ii', title: 'Two Sum II' },
      { id: 'two-sum', title: 'Two Sum' }
    ];

    const results = searchProblems('two sum', fixture);
    expect(results[0].id).toBe('two-sum');
    expect(results[0].title).toBe('Two Sum');
  });

  // Test 3: 'bfs graph' matches 'BFS Traversal of Graph' (out-of-order tokens)
  it('matches BFS Traversal of Graph with out-of-order query "bfs graph"', () => {
    const prob = {
      id: 'bfs-graph',
      title: 'BFS Traversal of Graph',
      category: 'Graph BFS/DFS'
    };

    const score = scoreProblem('bfs graph', prob);
    expect(score).toBeGreaterThan(0);

    const results = searchProblems('bfs graph', [prob, { id: 'other', title: 'Binary Search' }]);
    expect(results).toHaveLength(1);
    expect(results[0].id).toBe('bfs-graph');
  });

  // Test 4: 'bfs' matches 'Breadth First Search' via acronym
  it('matches Breadth First Search when searching acronym "bfs"', () => {
    const prob = {
      id: 'breadth-first-search',
      title: 'Breadth First Search',
      category: 'Graph BFS/DFS'
    };

    const score = scoreProblem('bfs', prob);
    expect(score).toBeGreaterThan(0);
  });

  // Test 5: AND semantics: 'kadane zzzz' returns 0
  it('enforces AND semantics: returns 0 when any token fails to match', () => {
    const prob = {
      id: 'kadane-algorithm',
      title: "Kadane's Algorithm",
      category: 'Arrays'
    };

    expect(scoreProblem('kadane zzzz', prob)).toBe(0);
  });

  // Test 6: Case and surrounding whitespace are irrelevant
  it('treats case and surrounding whitespace as irrelevant', () => {
    const prob = {
      id: 'kadane-algorithm',
      title: "Kadane's Algorithm",
      category: 'Arrays'
    };

    const s1 = scoreProblem('  KADANE ', prob);
    const s2 = scoreProblem('kadane', prob);
    expect(s1).toBe(s2);
    expect(s1).toBeGreaterThan(0);
  });

  // Test 7: Empty query returns array unchanged and in original order
  it('returns input array unchanged and in original order on empty query', () => {
    const fixture = [
      { id: 'z-prob', title: 'Zebra' },
      { id: 'a-prob', title: 'Apple' },
      { id: 'm-prob', title: 'Mango' }
    ];

    expect(scoreProblem('', fixture[0])).toBe(0);
    expect(scoreProblem('   ', fixture[0])).toBe(0);

    const resEmpty = searchProblems('', fixture);
    expect(resEmpty).toBe(fixture); // Same reference / original order
    expect(resEmpty.map(p => p.id)).toEqual(['z-prob', 'a-prob', 'm-prob']);

    const resWhitespace = searchProblems('   ', fixture);
    expect(resWhitespace).toBe(fixture);
  });

  // Test 8: Unknown / missing fields do not throw
  it('handles unknown or missing fields without throwing', () => {
    const incompleteProb = {
      title: 'Partial Problem',
      dsType: undefined,
      category: null,
      difficulty: undefined
    };

    expect(() => scoreProblem('partial', incompleteProb)).not.toThrow();
    expect(scoreProblem('partial', incompleteProb)).toBeGreaterThan(0);
    expect(() => scoreProblem('test', null)).not.toThrow();
    expect(() => scoreProblem('test', undefined)).not.toThrow();
    expect(() => searchProblems('test', [incompleteProb, null, undefined])).not.toThrow();
  });

  // Test 9: Ties break by traced then title; searchProblems is pure and deterministic
  it('breaks ties by traced DESC then title ASC deterministically', () => {
    const fixture = [
      { id: 'p3', title: 'Beta Problem', category: 'General', traced: false },
      { id: 'p1', title: 'Alpha Problem', category: 'General', traced: false },
      { id: 'p2', title: 'Beta Problem', category: 'General', traced: true },
      { id: 'p4', title: 'Alpha Problem', category: 'General', traced: true }
    ];

    // All match category 'General' with identical score
    const res1 = searchProblems('general', fixture);
    const res2 = searchProblems('general', fixture);

    expect(res1.map(p => p.id)).toEqual(['p4', 'p2', 'p1', 'p3']);
    expect(res2.map(p => p.id)).toEqual(res1.map(p => p.id));
  });

  // Test 10: matchRanges merges overlapping ranges and returns [] for empty query
  it('merges overlapping ranges and returns [] for an empty query in matchRanges', () => {
    expect(matchRanges('', 'Two Sum')).toEqual([]);
    expect(matchRanges('   ', 'Two Sum')).toEqual([]);
    expect(matchRanges('two', '')).toEqual([]);

    // Single token
    expect(matchRanges('two', 'Two Sum')).toEqual([[0, 3]]);

    // Overlapping tokens
    expect(matchRanges('two wo', 'Two Sum')).toEqual([[0, 3]]);

    // Multi-token non-overlapping
    expect(matchRanges('two sum', 'Two Sum')).toEqual([[0, 3], [4, 7]]);

    // Overlapping in 'banana' with 'an' and 'na'
    expect(matchRanges('an na', 'banana')).toEqual([[1, 6]]);
  });
});
