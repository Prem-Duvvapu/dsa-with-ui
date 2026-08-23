import React from 'react';
import { Plus, X } from 'lucide-react';

/**
 * Vertex count plus an edge table for GRAPH. Matches the wire shape exactly:
 * {vertices, edges: [[from,to]]}, or [[from,to,weight]] when the spec is weighted.
 */
export default function GraphField({ field, value, onChange }) {
  const vertices = value?.vertices ?? 1;
  const edges = Array.isArray(value?.edges) ? value.edges : [];
  const c = field.constraints || {};
  const maxVertices = c.maxVertices ?? Infinity;
  const maxEdges = c.maxEdges ?? Infinity;
  const weighted = c.weighted === true;

  const setVertices = (n) => {
    const clamped = Math.max(1, Math.min(maxVertices, n));
    onChange({ vertices: clamped, edges });
  };

  const setEdge = (index, position, n) => {
    const next = edges.map((e) => e.slice());
    next[index][position] = n;
    onChange({ vertices, edges: next });
  };

  const removeEdge = (index) => {
    onChange({ vertices, edges: edges.slice(0, index).concat(edges.slice(index + 1)) });
  };

  const addEdge = () => {
    if (edges.length >= maxEdges) return;
    const fresh = weighted ? [0, 0, 1] : [0, 0];
    onChange({ vertices, edges: edges.concat([fresh]) });
  };

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
      <label style={{ display: 'flex', alignItems: 'center', gap: '6px', fontSize: '0.76rem', color: 'var(--text-secondary)' }}>
        Vertices
        <input
          type="number"
          min={1}
          max={maxVertices === Infinity ? undefined : maxVertices}
          value={vertices}
          onChange={(e) => setVertices(Number(e.target.value) || 1)}
          className="ip-input"
          style={{ width: '64px' }}
        />
      </label>

      <div style={{ display: 'flex', flexDirection: 'column', gap: '4px' }}>
        {edges.map((edge, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span style={{ fontSize: '0.68rem', color: 'var(--text-muted)', width: '14px' }}>{i + 1}</span>
            <input
              type="number" min={0} max={Math.max(0, vertices - 1)}
              value={edge[0]}
              onChange={(ev) => setEdge(i, 0, Number(ev.target.value) || 0)}
              aria-label={`Edge ${i + 1} from vertex`}
              className="ip-input" style={{ width: '48px' }}
            />
            <span style={{ color: 'var(--text-muted)' }}>→</span>
            <input
              type="number" min={0} max={Math.max(0, vertices - 1)}
              value={edge[1]}
              onChange={(ev) => setEdge(i, 1, Number(ev.target.value) || 0)}
              aria-label={`Edge ${i + 1} to vertex`}
              className="ip-input" style={{ width: '48px' }}
            />
            {weighted && (
              <input
                type="number"
                value={edge[2] ?? 1}
                onChange={(ev) => setEdge(i, 2, Number(ev.target.value) || 0)}
                aria-label={`Edge ${i + 1} weight`}
                title="weight"
                className="ip-input" style={{ width: '48px' }}
              />
            )}
            <button
              type="button"
              onClick={() => removeEdge(i)}
              aria-label={`Remove edge ${i + 1}`}
              style={{ border: 'none', background: 'transparent', color: 'var(--text-muted)', cursor: 'pointer', display: 'flex' }}
            >
              <X size={12} />
            </button>
          </div>
        ))}
      </div>

      <button
        type="button"
        onClick={addEdge}
        disabled={edges.length >= maxEdges}
        className="btn btn-outline"
        style={{ padding: '4px 8px', width: 'fit-content', opacity: edges.length >= maxEdges ? 0.4 : 1 }}
      >
        <Plus size={12} /> Add edge
      </button>
    </div>
  );
}
