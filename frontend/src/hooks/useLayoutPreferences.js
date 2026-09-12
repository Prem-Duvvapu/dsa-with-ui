import { useEffect, useState } from 'react';
import usePersistentState from './usePersistentState';

const MOBILE_BREAKPOINT = 768;

const isBool = (v) => typeof v === 'boolean';

/**
 * Viewport tracking plus every panel the user can open, close, or have remembered.
 *
 * Extracted from App, which held eighteen hooks and had twice grown a temporal-dead-zone
 * bug from something being declared below the thing that used it. These belong together:
 * they are all "what shape is the workspace in", they are all read by the same layout, and
 * the sidebar rule below cannot be stated correctly without the viewport beside it.
 *
 * The sidebar rule is the only subtle part. Its preference is persisted for DESKTOP only,
 * because on a narrow viewport the sidebar is a modal drawer over the canvas - restoring
 * "open" from a desktop session would greet a phone user with the drawer covering the
 * thing they came to watch. The mobile drawer keeps its own state and always starts shut.
 */
export default function useLayoutPreferences() {
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);
  const isMobile = viewportWidth <= MOBILE_BREAKPOINT;

  const [desktopSidebarOpen, setDesktopSidebarOpen] = usePersistentState('sidebarOpen', true, isBool);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const isSidebarOpen = isMobile ? mobileSidebarOpen : desktopSidebarOpen;
  const setIsSidebarOpen = isMobile ? setMobileSidebarOpen : setDesktopSidebarOpen;

  // The bottom row, and the panels that live around it. All persisted: someone who prefers
  // a collapsed workspace should not rebuild it every session.
  const [isBottomPanelOpen, setIsBottomPanelOpen] = usePersistentState('bottomPanelOpen', true, isBool);
  const [isInputEditorOpen, setIsInputEditorOpen] = usePersistentState('inputEditorOpen', false, isBool);
  const [isComplexityOpen, setIsComplexityOpen] = usePersistentState('complexityOpen', false, isBool);
  const [isStatementOpen, setIsStatementOpen] = usePersistentState('statementOpen', true, isBool);

  useEffect(() => {
    const handleResize = () => {
      setViewportWidth(window.innerWidth);
      if (window.innerWidth > MOBILE_BREAKPOINT) {
        setDesktopSidebarOpen(true);
      }
    };
    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, [setDesktopSidebarOpen]);

  return {
    viewportWidth,
    isMobile,
    isSidebarOpen,
    setIsSidebarOpen,
    isBottomPanelOpen,
    setIsBottomPanelOpen,
    isInputEditorOpen,
    setIsInputEditorOpen,
    isComplexityOpen,
    setIsComplexityOpen,
    isStatementOpen,
    setIsStatementOpen
  };
}
