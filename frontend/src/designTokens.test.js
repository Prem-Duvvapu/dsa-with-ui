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

function getJsxFiles(dir) {
  const entries = readdirSync(dir, { withFileTypes: true });
  const files = [];
  for (const entry of entries) {
    const fullPath = join(dir, entry.name);
    if (entry.isDirectory()) {
      files.push(...getJsxFiles(fullPath));
    } else if (entry.isFile() && entry.name.endsWith('.jsx')) {
      files.push(fullPath);
    }
  }
  return files;
}

function componentSources() {
  const files = getJsxFiles(SRC);
  return files.map((fullPath) => ({
    name: fullPath.slice(SRC.length + 1).replace(/\\/g, '/'),
    text: readFileSync(fullPath, 'utf8')
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


/** The 14 tokens the Bench design defines in every theme. */
const BENCH_TOKENS = [
  '--bench-ground', '--bench-recessed', '--bench-panel', '--bench-rule',
  '--bench-rule-strong', '--bench-fill', '--bench-ink-dim', '--bench-ink-secondary',
  '--bench-ink', '--probe', '--probe-on', '--probe-wash', '--settled', '--settled-on'
];

/** Declarations inside one CSS block, as { token: rawValue }. */
function rawTokens(block) {
  const out = {};
  for (const m of block.matchAll(/(--[a-zA-Z0-9-]+)\s*:\s*([^;]+);/g)) {
    out[m[1]] = m[2].trim();
  }
  return out;
}

/** The first `:root {` block — the base every theme starts from. */
function baseBlock() {
  const start = CSS.indexOf(':root {');
  return rawTokens(CSS.slice(start, CSS.indexOf('\n}', start)));
}

function lightMediaBlock() {
  const start = CSS.indexOf('@media (prefers-color-scheme: light)');
  return CSS.slice(start, CSS.indexOf('\n  }', start));
}

function lightStampedBlock() {
  const start = CSS.indexOf(':root[data-theme="light"]');
  return CSS.slice(start, CSS.indexOf('\n}', start));
}

/** Follows var() chains so a token defined as var(--other) resolves to a real colour. */
function resolveTheme(tokens) {
  const resolved = {};
  const lookup = (name, depth = 0) => {
    if (depth > 10) return tokens[name];
    const value = tokens[name];
    if (!value) return undefined;
    const ref = value.match(/^var\((--[a-zA-Z0-9-]+)\)$/);
    return ref ? lookup(ref[1], depth + 1) : value;
  };
  for (const name of Object.keys(tokens)) resolved[name] = lookup(name);
  return resolved;
}

function luminance(hex) {
  const h = hex.replace('#', '');
  const [r, g, b] = [0, 2, 4].map((i) => {
    const c = parseInt(h.slice(i, i + 2), 16) / 255;
    return c <= 0.03928 ? c / 12.92 : ((c + 0.055) / 1.055) ** 2.4;
  });
  return 0.2126 * r + 0.7152 * g + 0.0722 * b;
}

function contrast(a, b) {
  const [hi, lo] = [luminance(a), luminance(b)].sort((x, y) => y - x);
  return (hi + 0.05) / (lo + 0.05);
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
    // This test used to assert only that the tokens EXISTED, while its name promised
    // they were distinct. They were not: --state-current and --diff-medium were both
    // #f59e0b, one meaning "the algorithm is here right now" and the other "medium".
    const base = resolveTheme(baseBlock());

    for (const t of ['--state-current', '--state-target', '--state-visited', '--state-done']) {
      expect(base[t], `${t} must exist`).toBeTruthy();
    }
    for (const t of ['--diff-easy', '--diff-medium', '--diff-hard']) {
      expect(base[t], `${t} must exist`).toBeTruthy();
    }

    const collisions = [];
    for (const state of ['--state-current', '--state-done']) {
      for (const diff of ['--diff-easy', '--diff-medium', '--diff-hard']) {
        if (base[state] === base[diff]) {
          collisions.push(`${state} and ${diff} are both ${base[state]}`);
        }
      }
    }
    expect(collisions, 'a difficulty pill must never borrow a semantic state colour').toEqual([]);
  });

  it('resolves every var() used inside index.css itself', () => {
    // The className and component-var guards read the JSX. They do not read the CSS, so a
    // rule inside this file could reference a token that does not exist and nothing would
    // notice — CSS drops an unresolvable declaration silently, exactly as it did when a
    // rewrite deleted 15 tokens. Caught while writing CaptureStrip: 18 var(--bench-*) calls
    // were live against a branch where the token layer had not landed yet, and every guard
    // stayed green.
    const defined = definedTokens();
    const missing = new Set();

    for (const match of CSS.matchAll(/var\(\s*(--[a-zA-Z0-9-]+)/g)) {
      if (!defined.has(match[1])) missing.add(match[1]);
    }

    expect([...missing], 'index.css references tokens it does not define').toEqual([]);
  });

  it('defines every Bench token in the base :root, not only behind a media query', () => {
    // A colour whose only definition sits inside @media or [data-theme] never applies
    // in the unstamped "system" state, which renders one theme's text on the other's
    // ground. The base block has to be complete on its own.
    const base = resolveTheme(baseBlock());
    const missing = BENCH_TOKENS.filter((t) => !base[t]);
    expect(missing, 'these resolve to nothing when the viewer has made no theme choice').toEqual([]);
  });

  it('redefines the same Bench tokens in both light declarations', () => {
    // Light is declared twice on purpose — once for a light OS with no explicit
    // choice, once for an explicit choice. If they drift, the toggle and the OS
    // setting disagree.
    const media = rawTokens(lightMediaBlock());
    const stamped = rawTokens(lightStampedBlock());
    expect(Object.keys(media).sort()).toEqual(Object.keys(stamped).sort());
    for (const key of Object.keys(media)) {
      expect(media[key], `${key} differs between the two light declarations`).toBe(stamped[key]);
    }
  });

  it('clears 4.5:1 contrast for every ink and state role, in both themes', () => {
    const themes = {
      dark: resolveTheme(baseBlock()),
      light: resolveTheme({ ...baseBlock(), ...rawTokens(lightStampedBlock()) })
    };

    const failures = [];
    for (const [name, tokens] of Object.entries(themes)) {
      const ground = tokens['--bench-recessed'];
      for (const role of ['--bench-ink', '--bench-ink-secondary', '--bench-ink-dim',
                          '--probe', '--settled']) {
        const ratio = contrast(tokens[role], ground);
        if (ratio < 4.5) {
          failures.push(`${name}: ${role} ${tokens[role]} on ${ground} is ${ratio.toFixed(2)}:1`);
        }
      }
      // Text sitting ON a filled probe or settled block.
      for (const [ink, fill] of [['--probe-on', '--probe'], ['--settled-on', '--settled']]) {
        const ratio = contrast(tokens[ink], tokens[fill]);
        if (ratio < 4.5) {
          failures.push(`${name}: ${ink} on ${fill} is ${ratio.toFixed(2)}:1`);
        }
      }
    }
    expect(failures, 'measured, not eyeballed — recalculate before changing a value').toEqual([]);
  });

  it('keeps compact text readable on the brand accent in both themes', () => {
    const themes = {
      dark: resolveTheme(baseBlock()),
      light: resolveTheme({ ...baseBlock(), ...rawTokens(lightStampedBlock()) })
    };

    for (const [name, tokens] of Object.entries(themes)) {
      expect(tokens['--text-on-accent'], `${name} must define --text-on-accent`).toBeTruthy();
      expect(
        contrast(tokens['--text-on-accent'], tokens['--accent-violet']),
        `${name} compact text on --accent-violet must meet WCAG AA`
      ).toBeGreaterThanOrEqual(4.5);
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
