package com.dsa.ui.tracer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Anchors exist because {@code activeLine} used to be an unchecked integer that drifted
 * away from the code it claimed to highlight. These tests pin the resolution rules.
 */
class AnnotatedCodeTest {

    @Test
    @DisplayName("An anchor resolves to the line below it, after markers are stripped")
    void anchorPointsAtTheFollowingStatement() {
        AnnotatedCode code = AnnotatedCode.parse("""
                int solve() {
                    // @a init
                    int x = 0;
                    // @a ret
                    return x;
                }""");

        // init marks `int x = 0;`, which is line 2 once the marker is removed
        assertEquals(2, code.resolve("init"));
        assertEquals(3, code.resolve("ret"));
        assertEquals(4, code.lineCount());
    }

    @Test
    @DisplayName("Markers never appear in the displayed source")
    void markersAreStripped() {
        AnnotatedCode code = AnnotatedCode.parse("""
                // @a only
                int x = 0;""");
        assertEquals("int x = 0;", code.getDisplayCode());
        assertFalse(code.getDisplayCode().contains("@a"));
    }

    @Test
    @DisplayName("Several anchors may share one statement")
    void stackedAnchorsShareALine() {
        AnnotatedCode code = AnnotatedCode.parse("""
                // @a first
                // @a second
                doWork();""");
        assertEquals(1, code.resolve("first"));
        assertEquals(1, code.resolve("second"));
    }

    @Test
    @DisplayName("Indentation does not affect marker detection")
    void indentedMarkersStillParse() {
        AnnotatedCode code = AnnotatedCode.parse("""
                while (true) {
                            // @a deep
                            break;
                }""");
        assertEquals(2, code.resolve("deep"));
    }

    @Test
    @DisplayName("An unknown anchor throws and names what is available")
    void unknownAnchorThrows() {
        AnnotatedCode code = AnnotatedCode.parse("// @a real\nint x = 0;");
        IllegalArgumentException e =
                assertThrows(IllegalArgumentException.class, () -> code.resolve("typo"));
        assertTrue(e.getMessage().contains("typo"));
        assertTrue(e.getMessage().contains("real"), "should list the declared anchors");
    }

    @Test
    @DisplayName("A duplicate anchor name is rejected at parse time")
    void duplicateAnchorRejected() {
        assertThrows(IllegalArgumentException.class, () -> AnnotatedCode.parse("""
                // @a dup
                int a = 0;
                // @a dup
                int b = 0;"""));
    }

    @Test
    @DisplayName("A trailing anchor with no statement is rejected")
    void danglingAnchorRejected() {
        assertThrows(IllegalArgumentException.class, () -> AnnotatedCode.parse("""
                int x = 0;
                // @a nothingFollows"""));
    }

    @Test
    @DisplayName("An unnamed marker is rejected")
    void unnamedAnchorRejected() {
        assertThrows(IllegalArgumentException.class, () -> AnnotatedCode.parse("// @a\nint x = 0;"));
    }

    @Test
    @DisplayName("Source with no anchors is left untouched")
    void plainSourceIsUnchanged() {
        String src = "int x = 0;\nreturn x;";
        AnnotatedCode code = AnnotatedCode.parse(src);
        assertEquals(src, code.getDisplayCode());
        assertTrue(code.getAnchors().isEmpty());
    }
}
