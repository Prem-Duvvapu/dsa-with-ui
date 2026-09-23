import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { CATEGORIES } from './Sidebar';

const contract = JSON.parse(readFileSync(
  resolve(process.cwd(), '../contracts/categories.json'),
  'utf8'
));

describe('sidebar category contract', () => {
  it('offers a button for every category the backend serves', () => {
    // Not cosmetic. The grid filters on an exact string match, so a category the backend
    // serves and this list has never heard of is unreachable - its problems answer over
    // the API and cannot be browsed to. The backend called one "BST" while this said
    // "Binary Search Trees", and sixteen problems were invisible here the whole time.
    const declared = contract.map(({ category }) => category).sort();
    const offered = CATEGORIES.map(({ id }) => id).sort();
    expect(offered).toEqual(declared);
  });

  it('gives every category a short and a full label', () => {
    for (const cat of CATEGORIES) {
      expect(cat.label?.length, `${cat.id} has no short label`).toBeGreaterThan(0);
      expect(cat.fullLabel?.length, `${cat.id} has no full label`).toBeGreaterThan(0);
      expect(cat.icon, `${cat.id} has no icon`).toBeTruthy();
    }
  });
});
