import { describe, it, expect } from 'vitest';
import { decodeTrace } from './decodeTrace';

const arrayState = (state) => [{ index: 0, value: 1, state }];

describe('decodeTrace', () => {
  it('passes a full response through untouched', () => {
    const steps = [{ stepNumber: 1, arrayState: arrayState('default') }];
    expect(decodeTrace({ encoding: 'full', steps })).toBe(steps);
  });

  it('passes a legacy response through, since it has no encoding field', () => {
    // The eighteen per-topic endpoints still send complete steps and know nothing
    // about deltas.
    const steps = [{ stepNumber: 1, description: 'legacy' }];
    expect(decodeTrace({ steps })).toBe(steps);
  });

  it('carries an omitted field forward', () => {
    const graphNodes = [{ id: 0, label: '0', x: 180, y: 40, state: 'unvisited' }];
    const graphEdges = [{ from: 0, to: 1, weight: null, directed: false, highlighted: false }];
    const dpTable = {
      rowLabels: ['length'],
      colLabels: ['0'],
      cells: [[{ value: '1', state: 'known' }]]
    };
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        {
          stepNumber: 1,
          keyframe: true,
          dsType: 'Array',
          arrayState: arrayState('default'),
          graphNodes,
          graphEdges,
          dpTable
        },
        { stepNumber: 2, description: 'moved' }
      ]
    });

    expect(decoded[1].arrayState).toEqual(arrayState('default'));
    expect(decoded[1].dsType).toBe('Array');
    expect(decoded[1].graphNodes).toBe(graphNodes);
    expect(decoded[1].graphEdges).toBe(graphEdges);
    expect(decoded[1].dpTable).toBe(dpTable);
  });

  it('replaces a field that is present', () => {
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        { stepNumber: 1, keyframe: true, arrayState: arrayState('default') },
        { stepNumber: 2, arrayState: arrayState('active') }
      ]
    });

    expect(decoded[1].arrayState[0].state).toBe('active');
  });

  it('treats an explicit empty array as a value, not as an omission', () => {
    // The regression this whole encoding is most likely to introduce. bfs-traversal
    // empties activeEdges on 6 of its 21 steps; if [] were read as "unchanged", the
    // canvas would keep highlighting edges the algorithm has already left.
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        { stepNumber: 1, keyframe: true, activeEdges: ['0-1'] },
        { stepNumber: 2, activeEdges: [] }
      ]
    });

    expect(decoded[1].activeEdges).toEqual([]);
  });

  it('carries queue and call stack independently and clears each only when explicitly empty', () => {
    const queueOrStackState = ['front', 'back'];
    const callStack = ['solve(0)', 'solve(1)'];
    const trieState = [
      { id: 0, character: 'root', endOfWord: false, children: { a: 1 }, state: 'known' },
      { id: 1, character: 'a', endOfWord: true, children: {}, state: 'probe' }
    ];
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        { stepNumber: 1, keyframe: true, queueOrStackState, callStack, trieState },
        { stepNumber: 2, description: 'no memory change' },
        { stepNumber: 3, queueOrStackState: [] },
        { stepNumber: 4, callStack: [], trieState: [] },
        { stepNumber: 5, description: 'empty state carries too' }
      ]
    });

    expect(decoded[1].queueOrStackState).toBe(queueOrStackState);
    expect(decoded[1].callStack).toBe(callStack);
    expect(decoded[1].trieState).toBe(trieState);
    expect(decoded[2].queueOrStackState).toEqual([]);
    expect(decoded[2].callStack).toBe(callStack);
    expect(decoded[2].trieState).toBe(trieState);
    expect(decoded[3].queueOrStackState).toEqual([]);
    expect(decoded[3].callStack).toEqual([]);
    expect(decoded[3].trieState).toEqual([]);
    expect(decoded[4].queueOrStackState).toEqual([]);
    expect(decoded[4].callStack).toEqual([]);
    expect(decoded[4].trieState).toEqual([]);
  });

  it('does not leak call-stack or trie state across a keyframe', () => {
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        {
          stepNumber: 1,
          keyframe: true,
          callStack: ['search(root)'],
          trieState: [{ id: 'root', children: [], state: 'read' }]
        },
        { stepNumber: 2, keyframe: true }
      ]
    });

    expect(decoded[1].callStack).toBeNull();
    expect(decoded[1].trieState).toBeNull();
  });

  it('discards carried state at a keyframe', () => {
    // A keyframe stands alone: a field absent from it is genuinely empty, which is what
    // makes seeking to one render a correct frame.
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        { stepNumber: 1, keyframe: true, arrayState: arrayState('default'), dsType: 'Array' },
        { stepNumber: 2, arrayState: arrayState('active') },
        { stepNumber: 3, keyframe: true, dsType: 'Array' }
      ]
    });

    expect(decoded[2].arrayState).toBeNull();
    expect(decoded[2].dsType).toBe('Array');
  });

  it('returns an empty list for a missing or malformed response', () => {
    expect(decodeTrace(undefined)).toEqual([]);
    expect(decodeTrace({})).toEqual([]);
    expect(decodeTrace({ encoding: 'delta', steps: null })).toEqual([]);
  });
});
