/**
 * Java syntax colouring for the source pane, without a highlighting library.
 *
 * Every traced solution is Java, so one small tokenizer covers the catalogue. It reads the
 * WHOLE source before splitting into lines, because a block comment or a text block can
 * span lines and a per-line pass would colour the rest of it as code.
 *
 * The output is the input, partitioned: joining every token's text, line by line, gives
 * back exactly the original source. Colouring may never change what the code says.
 */

const KEYWORDS = new Set([
  'abstract', 'assert', 'break', 'case', 'catch', 'class', 'const', 'continue', 'default',
  'do', 'else', 'enum', 'extends', 'final', 'finally', 'for', 'goto', 'if', 'implements',
  'import', 'instanceof', 'interface', 'native', 'new', 'package', 'private', 'protected',
  'public', 'record', 'return', 'static', 'strictfp', 'super', 'switch', 'synchronized',
  'this', 'throw', 'throws', 'transient', 'try', 'var', 'volatile', 'while', 'yield'
]);

const PRIMITIVES = new Set(['boolean', 'byte', 'char', 'double', 'float', 'int', 'long', 'short', 'void']);
const LITERALS = new Set(['true', 'false', 'null']);

// Order matters: comments and text blocks before strings, strings before everything else.
const RULES = [
  ['comment', /\/\/[^\n]*/y],
  ['comment', /\/\*[\s\S]*?(?:\*\/|$)/y],
  ['string', /"""[\s\S]*?(?:"""|$)/y],
  ['string', /"(?:[^"\\\n]|\\.)*"?/y],
  ['string', /'(?:[^'\\\n]|\\.)*'?/y],
  ['annotation', /@[A-Za-z_$][\w$]*/y],
  ['number', /(?:0[xX][0-9a-fA-F_]+|0[bB][01_]+|(?:\d[\d_]*)?\.?\d[\d_]*(?:[eE][+-]?\d+)?)[lLfFdD]?/y],
  ['word', /[A-Za-z_$][\w$]*/y],
  ['space', /\s+/y],
  ['plain', /[^\sA-Za-z0-9_$"'/@]+|./y]
];

function classify(word) {
  if (KEYWORDS.has(word)) return 'keyword';
  if (PRIMITIVES.has(word)) return 'type';
  if (LITERALS.has(word)) return 'number';
  // Java convention: a capitalised identifier names a type (Queue, List, Integer, Solution).
  // An ALL_CAPS constant is not a type.
  if (/^[A-Z]/.test(word) && /[a-z]/.test(word)) return 'type';
  return 'plain';
}

/** The whole source as a flat list of {type, text} tokens. */
export function tokenizeJava(source) {
  const tokens = [];
  let i = 0;
  while (i < source.length) {
    let matched = false;
    for (const [type, re] of RULES) {
      re.lastIndex = i;
      const m = re.exec(source);
      if (!m || m[0].length === 0) continue;
      const text = m[0];
      const kind = type === 'word' ? classify(text) : type === 'space' ? 'plain' : type;
      const last = tokens[tokens.length - 1];
      // Merge adjacent plain runs so a line is a handful of spans, not one per character.
      if (kind === 'plain' && last?.type === 'plain') last.text += text;
      else tokens.push({ type: kind, text });
      i += text.length;
      matched = true;
      break;
    }
    if (!matched) {
      // Unreachable given the catch-all rule, but never loop forever on input.
      tokens.push({ type: 'plain', text: source[i] });
      i += 1;
    }
  }
  return tokens;
}

/** Tokens grouped per source line; a token that spans a newline is split across lines. */
export function highlightJavaLines(source) {
  const lines = [[]];
  for (const token of tokenizeJava(source)) {
    const parts = token.text.split('\n');
    parts.forEach((part, idx) => {
      if (idx > 0) lines.push([]);
      if (part) lines[lines.length - 1].push({ type: token.type, text: part });
    });
  }
  return lines;
}
