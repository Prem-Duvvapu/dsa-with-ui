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
 *
 * The problem statement and code panel follow the identical rule, for the identical
 * reason, and it is not optional there: the app shell is a fixed 100vh with overflow
 * hidden, and both panels default open on desktop. Stacked above the canvas on a phone
 * with nothing yielding height, they squeeze the canvas's own wrapper to zero pixels -
 * measured, not assumed - so a first-time mobile visitor opening any problem never sees
 * the thing the whole app exists to show. Desktop keeps its persisted default-open
 * preference; mobile gets its own transient state, closed by default, so the canvas gets
 * the room on first load. A returning mobile visitor who opened them last time does not
 * have that remembered - the alternative is a returning phone visitor greeted by an
 * invisible canvas again, which is worse.
 */
export default function useLayoutPreferences() {
  const [viewportWidth, setViewportWidth] = useState(window.innerWidth);
  const isMobile = viewportWidth <= MOBILE_BREAKPOINT;

  const [desktopSidebarOpen, setDesktopSidebarOpen] = usePersistentState('sidebarOpen', true, isBool);
  const [mobileSidebarOpen, setMobileSidebarOpen] = useState(false);
  const isSidebarOpen = isMobile ? mobileSidebarOpen : desktopSidebarOpen;
  const setIsSidebarOpen = isMobile ? setMobileSidebarOpen : setDesktopSidebarOpen;

  // The bottom row, and the panels that live around it. isInputEditorOpen and
  // isComplexityOpen already default closed, so they need no mobile/desktop split.
  const [desktopBottomPanelOpen, setDesktopBottomPanelOpen] = usePersistentState('bottomPanelOpen', true, isBool);
  const [mobileBottomPanelOpen, setMobileBottomPanelOpen] = useState(false);
  const isBottomPanelOpen = isMobile ? mobileBottomPanelOpen : desktopBottomPanelOpen;
  const setIsBottomPanelOpen = isMobile ? setMobileBottomPanelOpen : setDesktopBottomPanelOpen;

  const [isInputEditorOpen, setIsInputEditorOpen] = usePersistentState('inputEditorOpen', false, isBool);
  const [isComplexityOpen, setIsComplexityOpen] = usePersistentState('complexityOpen', false, isBool);

  const [desktopStatementOpen, setDesktopStatementOpen] = usePersistentState('statementOpen', true, isBool);
  const [mobileStatementOpen, setMobileStatementOpen] = useState(false);
  const isStatementOpen = isMobile ? mobileStatementOpen : desktopStatementOpen;
  const setIsStatementOpen = isMobile ? setMobileStatementOpen : setDesktopStatementOpen;

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
