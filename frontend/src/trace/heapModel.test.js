import { describe, expect, it } from 'vitest';
import { heapSlots, heapLayout, parentOf, leftOf, rightOf } from './heapModel';

describe('heap index arithmetic', () => {
  it('is the whole relationship between the two views', () => {
    expect(parentOf(0)).toBeNull();
    expect(parentOf(1)).toBe(0);
    expect(parentOf(2)).toBe(0);
    expect(parentOf(4)).toBe(1);
    expect(leftOf(0)).toBe(1);
    expect(rightOf(0)).toBe(2);
    expect(leftOf(2)).toBe(5);
  });
});

describe('heapSlots', () => {
  it('reads an explicit tree in level order, so indices line up', () => {
    const step = {
      treeNodes: [
        { id: 0, val: '1', leftId: 1, rightId: 2, state: 'current' },
        { id: 1, val: '3', leftId: 3, rightId: null, state: 'default' },
        { id: 2, val: '8', leftId: null, rightId: null, state: 'default' },
        { id: 3, val: '5', leftId: null, rightId: null, state: 'target' }
      ]
    };
    const { slots, source } = heapSlots(step);
    expect(source).toBe('tree');
    expect(slots.map((s) => s.value)).toEqual(['1', '3', '8', '5']);
    expect(slots[3].state).toBe('target');
  });

  it('reads an array when that is what the tracer emitted', () => {
    const step = { arrayState: [{ value: 4, state: 'current' }, { value: 9, state: 'default' }] };
    const { slots, source } = heapSlots(step);
    expect(source).toBe('array');
    expect(slots.map((s) => s.value)).toEqual(['4', '9']);
  });

  it('prefers the explicit tree when a step somehow carries both', () => {
    const step = {
      treeNodes: [{ id: 0, val: '7', leftId: null, rightId: null, state: 'default' }],
      arrayState: [{ value: 99, state: 'default' }]
    };
    expect(heapSlots(step).slots[0].value).toBe('7');
  });

  it('reports nothing rather than guessing when a step carries neither', () => {
    expect(heapSlots({ variables: {} })).toEqual({ slots: [], source: null });
  });
});

describe('heapLayout', () => {
  it('groups indices into the rows the tree draws', () => {
    // 1, 2, 4, 8 - a complete binary tree's levels, which is why the arithmetic works.
    expect(heapLayout(7)).toEqual([[0], [1, 2], [3, 4, 5, 6]]);
    expect(heapLayout(4)).toEqual([[0], [1, 2], [3]]);
  });
});
