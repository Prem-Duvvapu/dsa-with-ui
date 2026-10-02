import { describe, expect, it } from 'vitest';
import { highlightJavaLines, tokenizeJava } from './highlightJava';

const SAMPLE = `class Solution {
    public static int[] dRow = {-1,0,1,0};
    /* a block comment
       that spans lines */
    public int findCircleNum(int[][] isConnected) {
        int n=isConnected.length; // trailing comment
        Queue<Integer> q = new LinkedList<>();
        String s = "a \\"quoted\\" // not a comment";
        char c = '\\'';
        long big = 1_000L + 0x1F;
        String block = """
            text block
            """;
        @Override
        boolean done = visited[0] && true || null == q;
        return MAX_N;
    }
}`;

const typeOf = (text) => tokenizeJava(SAMPLE).find((t) => t.text === text)?.type;

describe('highlightJava', () => {
  it('partitions the source exactly - colouring never changes the code', () => {
    const lines = highlightJavaLines(SAMPLE);
    expect(lines.map((line) => line.map((t) => t.text).join(''))).toEqual(SAMPLE.split('\n'));
    expect(tokenizeJava(SAMPLE).map((t) => t.text).join('')).toBe(SAMPLE);
  });

  it('keeps a block comment and a text block whole across the lines they span', () => {
    const lines = highlightJavaLines(SAMPLE);
    expect(lines[3]).toEqual([{ type: 'comment', text: '       that spans lines */' }]);
    expect(lines[11]).toEqual([{ type: 'string', text: '            text block' }]);
    expect(lines[12]).toEqual([{ type: 'string', text: '            """' }, { type: 'plain', text: ';' }]);
  });

  it('does not read a // inside a string as a comment', () => {
    expect(typeOf('"a \\"quoted\\" // not a comment"')).toBe('string');
    expect(typeOf("'\\''")).toBe('string');
    expect(typeOf('// trailing comment')).toBe('comment');
  });

  it('classifies keywords, types, numbers and literals', () => {
    expect(typeOf('class')).toBe('keyword');
    expect(typeOf('return')).toBe('keyword');
    expect(typeOf('int')).toBe('type');
    expect(typeOf('Queue')).toBe('type');
    expect(typeOf('Integer')).toBe('type');
    expect(typeOf('1_000L')).toBe('number');
    expect(typeOf('0x1F')).toBe('number');
    expect(typeOf('true')).toBe('number');
    expect(typeOf('@Override')).toBe('annotation');
    // An ALL_CAPS constant is not a type, and an identifier with digits is not a number.
    expect(typeOf('MAX_N')).toBeUndefined();
    expect(tokenizeJava('dRow2 = x1;')).toEqual([{ type: 'plain', text: 'dRow2 = x1;' }]);
  });

  it('terminates on unterminated strings and comments', () => {
    for (const src of ['"open', "'x", '/* never closed', '"""\nopen']) {
      expect(tokenizeJava(src).map((t) => t.text).join('')).toBe(src);
    }
  });
});
