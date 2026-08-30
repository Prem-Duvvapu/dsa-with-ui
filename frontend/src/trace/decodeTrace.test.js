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
    const decoded = decodeTrace({
      encoding: 'delta',
      steps: [
        {
          stepNumber: 1,
          keyframe: true,
          dsType: 'Array',
          arrayState: arrayState('default'),
          graphNodes,
          graphEdges
        },
        { stepNumber: 2, description: 'moved' }
      ]
    });

    expect(decoded[1].arrayState).toEqual(arrayState('default'));
    expect(decoded[1].dsType).toBe('Array');
    expect(decoded[1].graphNodes).toBe(graphNodes);
    expect(decoded[1].graphEdges).toBe(graphEdges);
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
