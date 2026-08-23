import React from 'react';
import { AlertTriangle, RotateCcw } from 'lucide-react';

/**
 * A throw in any canvas used to blank the entire app — five renderers reading
 * per-step payloads with no shape guarantee, and no boundary above them. This
 * catches at the canvas boundary specifically, not the app root, so a bad step
 * loses one visualization rather than the sidebar, controls, and code panel too.
 */
export default class ErrorBoundary extends React.Component {
  constructor(props) {
    super(props);
    this.state = { error: null };
  }

  static getDerivedStateFromError(error) {
    return { error };
  }

  componentDidCatch(error, info) {
    console.error('Canvas crashed:', error, info.componentStack);
  }

  componentDidUpdate(prevProps) {
    // A new problem or step after a crash gets a fresh try, not a permanently dead canvas.
    if (this.state.error && prevProps.resetKey !== this.props.resetKey) {
      this.setState({ error: null });
    }
  }

  render() {
    if (this.state.error) {
      return (
        <div style={{
          flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center',
          justifyContent: 'center', gap: '10px', padding: '24px', textAlign: 'center',
          color: 'var(--bench-ink-dim)', fontFamily: 'var(--font-code)'
        }}>
          <AlertTriangle size={22} color="var(--probe)" />
          <span style={{ fontSize: '0.85rem', color: 'var(--bench-ink)', fontWeight: 700 }}>
            This visualization hit an error.
          </span>
          <span style={{ fontSize: '0.76rem', maxWidth: '380px' }}>
            {this.state.error?.message || 'Something went wrong rendering this step.'}
          </span>
          <button
            type="button"
            className="btn btn-outline"
            onClick={() => this.setState({ error: null })}
          >
            <RotateCcw size={12} /> Try again
          </button>
        </div>
      );
    }
    return this.props.children;
  }
}
