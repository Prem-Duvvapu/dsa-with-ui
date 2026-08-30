/**
 * The frame every visualization sits in.
 *
 * Five canvases each drew their own chrome, their own legend, and their own idea of what
 * "current" looks like: getNodeColor exists four times with four different palettes, and
 * the four-badge legend is copy-pasted verbatim three times. Those disagreements are why
 * the same state reads differently depending on which problem you opened.
 *
 * The shell owns the chrome. A canvas supplies only its stage.
 */

// Class names are written out rather than interpolated. designTokens.test.js reads the
// JSX statically to catch a className index.css does not define, and `shell-key-${kind}`
// is invisible to it — the guard is worth more than the brevity.
const LEGEND = [
  { kind: 'probe', className: 'shell-key-probe', glyph: '▼', label: 'happening now' },
  { kind: 'read', className: 'shell-key-read', glyph: '○', label: 'read this step' },
  { kind: 'known', className: 'shell-key-known', glyph: '□', label: 'has a value' },
  { kind: 'resolved', className: 'shell-key-settled', glyph: '✓', label: 'resolved / final' },
  { kind: 'void', className: 'shell-key-void', glyph: '▫', label: 'untouched' }
];

export default function CanvasShell({ title, meta, legend = true, children, footer }) {
  return (
    <section className="shell" aria-label={title ?? 'Visualization'}>
      <header className="shell-head">
        <span className="shell-title">{title}</span>
        {meta && <span className="shell-meta">{meta}</span>}
        {legend && (
          <ul className="shell-legend">
            {LEGEND.map(({ kind, className, glyph, label }) => (
              <li key={kind} className={`shell-key ${className}`}>
                {/* Glyph plus word: state never rides on colour alone. */}
                <i aria-hidden="true">{glyph}</i>
                {label}
              </li>
            ))}
          </ul>
        )}
      </header>

      <div className="shell-stage">{children}</div>

      {footer && <div className="shell-foot">{footer}</div>}
    </section>
  );
}
