import { describe, expect, it } from 'vitest';
import { curriculumNeighbours } from './curriculum';

const section = 'Authored section';
const problems = [
  { id: 'first', title: 'Zulu', striverSheetSection: section },
  { id: 'unrelated', title: 'Other', striverSheetSection: 'Other section' },
  { id: 'middle', title: 'Middle', striverSheetSection: section },
  { id: 'last', title: 'Alpha', striverSheetSection: section }
];

describe('live curriculum neighbours (retired SectionNav guards)', () => {
  it('uses authored catalogue order, not title order or adjacency across sections', () => {
    expect(curriculumNeighbours(problems, 'middle')).toEqual({
      section, position: 2, total: 3, previous: problems[0], next: problems[3]
    });
  });
  it('does not invent a previous problem at the section start', () => {
    expect(curriculumNeighbours(problems, 'first')).toMatchObject({ previous: null, next: problems[2], position: 1 });
  });
  it('does not invent a next problem at the section end', () => {
    expect(curriculumNeighbours(problems, 'last')).toMatchObject({ next: null, previous: problems[2], position: 3 });
  });
  it('offers no neighbours for a single-problem section', () => {
    expect(curriculumNeighbours(problems, 'unrelated')).toBeNull();
  });
  it('offers no invented section for unknown or sectionless problems or an empty catalogue', () => {
    expect(curriculumNeighbours(problems, 'unknown')).toBeNull();
    expect(curriculumNeighbours([{ id: 'no-section' }], 'no-section')).toBeNull();
    expect(curriculumNeighbours([], 'first')).toBeNull();
  });
});
