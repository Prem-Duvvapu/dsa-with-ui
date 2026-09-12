import { useEffect } from 'react';

/**
 * Every global key binding in the app, in one place.
 *
 * This lived inline in App among seventeen other hooks, which is how it ended up ignoring
 * every key while a BUTTON had focus - one click on Play killed the keyboard until you
 * clicked elsewhere. It is easier to see that rule is wrong when the rule is the only
 * thing in the file.
 *
 * The bindings themselves are documented in ShortcutHelp, which is what the `?` key opens.
 * Keep the two in step: a binding that is not in that list is a binding nobody finds.
 */
export default function useKeyboardShortcuts({
  togglePlay, stepNext, stepPrev, reset, seek, stepCount, nudgeSpeed,
  isMobile, isSidebarOpen, setIsSidebarOpen,
  isHelpOpen, setIsHelpOpen,
  hasSeenWelcome, setHasSeenWelcome
}) {
// ── Global keyboard shortcuts ────────────────────────────────────────────
// This is a media player, so it uses a media player's keys: J/K/L and ,/. alongside the
// arrows. The list lives in ShortcutHelp, opened with `?` - the shortcuts worked before
// but were written down only in two button tooltips, which is not discoverable.
useEffect(() => {
  const handleKeyDown = (e) => {
    if (e.metaKey || e.ctrlKey || e.altKey) return;

    const active = document.activeElement;
    const tag = active?.tagName;
    const isTyping = ['INPUT', 'TEXTAREA', 'SELECT'].includes(tag) || active?.isContentEditable;

    // Escape works even while typing - someone in the search field is exactly who needs
    // it - and closes the topmost thing first.
    if (e.code === 'Escape') {
      if (!hasSeenWelcome) {
        e.preventDefault();
        setHasSeenWelcome(true);
        return;
      }
      if (isHelpOpen) {
        e.preventDefault();
        setIsHelpOpen(false);
        return;
      }
      if (isMobile && isSidebarOpen) {
        e.preventDefault();
        setIsSidebarOpen(false);
        return;
      }
      if (isTyping) active.blur();
      return;
    }

    if (isTyping) return;

    // `/` focuses search, matching every other search-first UI.
    if (e.key === '/') {
      e.preventDefault();
      setIsSidebarOpen(true);
      document.querySelector('[data-search-input]')?.focus();
      return;
    }

    if (e.key === '?') {
      e.preventDefault();
      setIsHelpOpen(prev => !prev);
      return;
    }

    // A focused button activates on Space/Enter natively. Letting Space through here too
    // would toggle playback twice; blocking every key while a button has focus - which is
    // what this used to do - meant one click on Play killed the keyboard for good.
    const onButton = tag === 'BUTTON';

    switch (e.code) {
      case 'Space':
        if (onButton) return;
        e.preventDefault();
        togglePlay();
        return;
      case 'KeyK':
        e.preventDefault();
        togglePlay();
        return;
      case 'ArrowRight':
      case 'KeyL':
      case 'Period':
        e.preventDefault();
        stepNext();
        return;
      case 'ArrowLeft':
      case 'KeyJ':
      case 'Comma':
        e.preventDefault();
        stepPrev();
        return;
      case 'Home':
        e.preventDefault();
        seek(0);
        return;
      case 'End':
        e.preventDefault();
        if (stepCount > 0) seek(stepCount - 1);
        return;
      case 'KeyR':
        e.preventDefault();
        reset();
        return;
      case 'BracketLeft':
        e.preventDefault();
        nudgeSpeed(-1);
        return;
      case 'BracketRight':
        e.preventDefault();
        nudgeSpeed(1);
        return;
      default:
    }
  };

  window.addEventListener('keydown', handleKeyDown);
  return () => window.removeEventListener('keydown', handleKeyDown);
}, [togglePlay, stepNext, stepPrev, reset, seek, stepCount, nudgeSpeed,
    isMobile, isSidebarOpen, isHelpOpen, setIsSidebarOpen, setIsHelpOpen,
    hasSeenWelcome, setHasSeenWelcome]);
}
