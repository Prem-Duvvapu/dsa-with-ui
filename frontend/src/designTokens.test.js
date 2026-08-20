import { describe, it, expect } from 'vitest';
import { readFileSync, readdirSync } from 'node:fs';
import { join, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';

/**
 * Static guard for the design-token system.
 *
 * Commit b553e56 rewrote index.css from 343 lines to 170 and updated only 8 of 13
 * components. The other 5 kept referencing 15 custom properties that no longer
 * existed. CSS drops a whole declaration when a var() cannot resolve, so the header
 * silently lost its margin, the breadcrumb its padding, and several canvases their
 * transitions — with no error anywhere and every test still green.
 *
 * These tests fail the build instead.
 */

const SRC = dirname(fileURLToPath(import.meta.url));
const CSS = readFileSync(join(SRC, 'index.css'), 'utf8');

function componentSources() {
  const dir = join(SRC, 'components');
  const files = readdirSync(dir).filter((f) => f.endsWith('.jsx'));
  files.push('../App.jsx');
  return files.map((f) => ({
    name: f,
    text: readFileSync(f.startsWith('..') ? join(SRC, 'App.jsx') : join(dir, f), 'utf8')
  }));
}

/** Custom properties declared anywhere in index.css. */
function definedTokens() {
  return new Set([...CSS.matchAll(/^\s*(--[a-zA-Z0-9-]+)\s*:/gm)].map((m) => m[1]));
}

/** Class selectors declared anywhere in index.css, including inside @media blocks. */
function definedClasses() {
  return new Set([...CSS.matchAll(/^\s*\.([a-zA-Z0-9_-]+)/gm)].map((m) => m[1]));
}

describe('design tokens', () => {
  it('defines every custom property the components reference', () => {
    const defined = definedTokens();
    const missing = [];

    for (const { name, text } of componentSources()) {
      for (const match of text.matchAll(/var\((--[a-zA-Z0-9-]+)/g)) {
        if (!defined.has(match[1])) missing.push(`${name}: ${match[1]}`);
      }
    }

    expect(missing).toEqual([]);
  });

  it('defines every static className the components reference', () => {
    const defined = definedClasses();
    const missing = [];

    for (const { name, text } of componentSources()) {
      // Static string classNames only; template literals with ${} are checked below.
      for (const match of text.matchAll(/className=["`]([^"`${}]+)["`]/g)) {
        for (const cls of match[1].trim().split(/\s+/)) {
          if (cls && !defined.has(cls)) missing.push(`${name}: .${cls}`);
        }
      }
      // Literal class names appearing inside template-literal classNames.
      for (const match of text.matchAll(/className=\{`([^`]*)`\}/g)) {
        for (const cls of match[1].replace(/\$\{[^}]*\}/g, ' ').trim().split(/\s+/)) {
          if (cls && !defined.has(cls)) missing.push(`${name}: .${cls}`);
        }
      }
    }

    expect(missing).toEqual([]);
  });

  it('keeps the four semantic execution-state tokens distinct from difficulty tokens', () => {
    const defined = definedTokens();
    for (const t of ['--state-current', '--state-target', '--state-visited', '--state-done']) {
      expect(defined.has(t), `${t} must exist`).toBe(true);
    }
    for (const t of ['--diff-easy', '--diff-medium', '--diff-hard']) {
      expect(defined.has(t), `${t} must exist`).toBe(true);
    }
  });

  it('animates the loading spinner', () => {
    // App renders <RefreshCw className="spin" /> as the catalogue loading indicator.
    expect(CSS).toMatch(/@keyframes\s+spin\b/);
    expect(CSS).toMatch(/\.spin\s*\{[^}]*animation:/);
  });

  it('respects prefers-reduced-motion', () => {
    expect(CSS).toMatch(/@media\s*\(prefers-reduced-motion:\s*reduce\)/);
  });
});
