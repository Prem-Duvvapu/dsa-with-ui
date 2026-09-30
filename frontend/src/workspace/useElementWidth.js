import { useLayoutEffect, useState } from 'react';

/**
 * The measured width of an element, kept current as it resizes (ResizeObserver) or, where
 * that is unavailable, as the window does. Layout decisions use the CONTAINER, not the
 * viewport, because the same viewport holds different room depending on what else is shown.
 */
export default function useElementWidth(ref) {
  const [width, setWidth] = useState(0);
  useLayoutEffect(() => {
    const node = ref.current;
    if (!node) return undefined;
    const read = () => setWidth(node.getBoundingClientRect().width || 0);
    read();
    if (typeof ResizeObserver !== 'undefined') {
      const observer = new ResizeObserver(read);
      observer.observe(node);
      return () => observer.disconnect();
    }
    window.addEventListener('resize', read);
    return () => window.removeEventListener('resize', read);
  }, [ref]);
  return width;
}
