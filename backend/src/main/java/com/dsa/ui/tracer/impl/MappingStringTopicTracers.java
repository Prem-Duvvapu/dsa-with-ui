package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Isomorphic Strings (LeetCode 205), traced on the owner's own accepted submission: two arrays record
 * which character each s-character maps to and which each t-character maps from. A clash in either
 * direction - one character mapping to two, or two mapping to one - means not isomorphic. O(n).
 *
 * <p>The two checks are highlighted on their {@code if} lines, which decide pass or fail; that keeps
 * both reachable from the contract inputs.
 */
@Component
class IsomorphicStringsTracer extends StringTracerSupport {
    public String id() { return "isomorphic-strings"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "Source", "egg", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("t", "Target", "add", 1, 30, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "foo", "t", "bar"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public boolean isIsomorphic(String s, String t) {
                       // @a init
                       int n=s.length();

                       int[] sTot=new int[256];
                       int[] tTos=new int[256];

                       Arrays.fill(sTot,-1);
                       Arrays.fill(tTos,-1);

                       for (int i=0;i<n;i++) {
                           char sChar=s.charAt(i);
                           char tChar=t.charAt(i);

                           //s to t checking
                           // @a sCheck
                           if (sTot[sChar]!=-1 && sTot[sChar]!=tChar)
                               return false;
                           else
                               sTot[sChar]=tChar;

                           //t to s checking
                           // @a tCheck
                           if (tTos[tChar]!=-1 && tTos[tChar]!=sChar)
                               return false;
                           else
                               tTos[tChar]=sChar;
                       }

                       // @a done
                       return true;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String t = in.getString("t");
        if (s.length() != t.length()) {
            throw new InputValidationException(Map.of("t", "must be as long as s"));
        }
        int n = s.length();
        int[] sTot = new int[256];
        int[] tTos = new int[256];
        java.util.Arrays.fill(sTot, -1);
        java.util.Arrays.fill(tTos, -1);
        emit.at("init").say("sTot records what each s-letter maps to, tTos what each t-letter maps from.")
                .chars(s).step();
        for (int i = 0; i < n; i++) {
            char sChar = s.charAt(i);
            char tChar = t.charAt(i);
            if (sTot[sChar] != -1 && sTot[sChar] != tChar) {
                emit.at("sCheck").say("'%c' already maps to '%c', but here it would map to '%c'. Return false.",
                                sChar, (char) sTot[sChar], tChar)
                        .var("i", i).var("sTot", map(sTot)).var("tTos", map(tTos)).var("answer", false)
                        .chars(s, i).step();
                return;
            }
            boolean newS = sTot[sChar] == -1;
            sTot[sChar] = tChar;
            emit.at("sCheck").say(newS
                            ? String.format("s[%d] = '%c' has no mapping yet: record '%c' -> '%c'.", i, sChar, sChar, tChar)
                            : String.format("s[%d] = '%c' already maps to '%c', and t[%d] is '%c' - consistent.", i, sChar, tChar, i, tChar))
                    .var("i", i).var("sTot", map(sTot)).var("tTos", map(tTos)).chars(s, i).step();
            if (tTos[tChar] != -1 && tTos[tChar] != sChar) {
                emit.at("tCheck").say("'%c' is already the image of '%c', so '%c' cannot map to it too. Return false.",
                                tChar, (char) tTos[tChar], sChar)
                        .var("i", i).var("sTot", map(sTot)).var("tTos", map(tTos)).var("answer", false)
                        .chars(t, i).step();
                return;
            }
            boolean newT = tTos[tChar] == -1;
            tTos[tChar] = sChar;
            emit.at("tCheck").say(newT
                            ? String.format("No other s-letter maps to '%c' yet: record '%c' <- '%c'.", tChar, tChar, sChar)
                            : String.format("'%c' is already the image of '%c' and nothing else - still one-to-one.", tChar, sChar))
                    .var("i", i).var("sTot", map(sTot)).var("tTos", map(tTos)).chars(t, i).step();
        }
        emit.at("done").say("Every position agrees in both directions: the strings are isomorphic.")
                .var("answer", true).chars(s).step();
    }

    /** The mapping as "e->a, g->d". */
    private static String map(int[] m) {
        List<String> out = new ArrayList<>();
        for (int c = 0; c < m.length; c++) {
            if (m[c] != -1) out.add((char) c + "->" + (char) m[c]);
        }
        return String.join(", ", out);
    }
}

/**
 * Rotate String (LeetCode 796), traced on the owner's own accepted submission: goal is a rotation of
 * s exactly when the lengths match and goal appears inside s + s, which they search with KMP - first
 * the longest-prefix-suffix table of goal, then one pass over s + s. O(n).
 *
 * <p>The table and search loops each branch three ways; the highlight sits on each loop's decision
 * and the narration names the branch, which keeps every highlight reachable from the contract inputs.
 */
@Component
class RotateStringTracer extends StringTracerSupport {
    public String id() { return "rotate-string"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "Source", "abcde", 1, 30, "[a-z]+", "Use lowercase letters a-z."),
                patternedText("goal", "Goal", "cdeab", 1, 30, "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "abcde", "goal", "abced"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public boolean rotateString(String s, String goal) {
                       // @a lengths
                       if (s.length()!=goal.length())
                           return false;

                       return kmp(s+s,goal);
                   }

                   public boolean kmp(String text,String pattern) {
                       // @a init
                       int n=text.length();
                       int m=pattern.length();

                       int[] lps=new int[m];
                       lps[0]=0;
                       int i=0;
                       int j=1;
                       int currLength=0;

                       while (j<m) {
                           // @a lps
                           if (pattern.charAt(j)==pattern.charAt(currLength)) {
                               currLength++;
                               lps[j]=currLength;
                               j++;
                           } else {
                               if (currLength==0) {
                                   lps[j]=currLength;
                                   j++;
                               } else {
                                   currLength=lps[currLength-1];
                               }
                           }
                       }

                       j=0;
                       while (i<n && j<m) {
                           // @a search
                           if (text.charAt(i)==pattern.charAt(j)) {
                               i++;
                               j++;
                           } else {
                               if (j==0)
                                   i++;
                               else
                                   j=lps[j-1];
                           }

                           if (j==m)
                               // @a found
                               return true;
                       }

                       // @a none
                       return false;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        String goal = in.getString("goal");
        if (s.length() != goal.length()) {
            emit.at("lengths").say("s and goal have different lengths, so goal cannot be a rotation. Return false.")
                    .var("answer", false).chars(s).step();
            return;
        }
        emit.at("lengths").say("Same length. Every rotation of s appears inside s + s = \"%s\", so search for goal there.",
                        s + s)
                .chars(s + s).step();
        String text = s + s;
        String pattern = goal;
        int n = text.length();
        int m = pattern.length();
        int[] lps = new int[m];
        int i = 0;
        int j = 1;
        int currLength = 0;
        emit.at("init").say("First build lps for \"%s\": lps[j] is the length of the longest proper prefix of "
                        + "pattern[0..j] that is also a suffix of it.", pattern)
                .var("lps", java.util.Arrays.toString(lps)).chars(pattern).step();
        while (j < m) {
            String what;
            if (pattern.charAt(j) == pattern.charAt(currLength)) {
                currLength++;
                lps[j] = currLength;
                what = String.format("pattern[%d] = '%c' extends the matched prefix: lps[%d] = %d.", j, pattern.charAt(j), j, currLength);
                j++;
            } else if (currLength == 0) {
                lps[j] = 0;
                what = String.format("pattern[%d] = '%c' matches no prefix: lps[%d] = 0.", j, pattern.charAt(j), j);
                j++;
            } else {
                currLength = lps[currLength - 1];
                what = String.format("Mismatch: fall back to the next shorter prefix, length %d.", currLength);
            }
            emit.at("lps").say(what).var("lps", java.util.Arrays.toString(lps)).chars(pattern, Math.min(j, m - 1)).step();
        }
        j = 0;
        while (i < n && j < m) {
            String what;
            if (text.charAt(i) == pattern.charAt(j)) {
                what = String.format("text[%d] = pattern[%d] = '%c': %d letter%s of goal matched.",
                        i, j, text.charAt(i), j + 1, Narration.s(j + 1));
                i++;
                j++;
            } else if (j == 0) {
                what = String.format("text[%d] = '%c' cannot start goal: move on.", i, text.charAt(i));
                i++;
            } else {
                int was = j;
                j = lps[j - 1];
                what = String.format("Mismatch after %d matched: lps says %d of them can be kept, so continue from there.", was, j);
            }
            emit.at("search").say(what).var("i", i).var("j", j).chars(text, Math.min(i, n - 1)).step();
            if (j == m) {
                emit.at("found").say("All %d letters of goal matched inside s + s: goal is a rotation of s. Return true.", m)
                        .var("answer", true).chars(text).step();
                return;
            }
        }
        emit.at("none").say("goal never appears inside s + s, so it is not a rotation of s. Return false.")
                .var("answer", false).chars(text).step();
    }
}

/**
 * Sort Characters By Frequency (LeetCode 451), traced on the owner's own accepted submission: count
 * each character in a HashMap, put the counts in a max-heap by frequency, and append each character
 * as many times as it appears, most frequent first. O(n log k) for k distinct characters.
 *
 * <p>Characters with equal counts come out in whatever order Java's HashMap and PriorityQueue give;
 * the problem accepts any order, and the trace uses the real classes so the output is the code's.
 */
@Component
class SortCharactersFrequencyTracer extends StringTracerSupport {
    public String id() { return "sort-characters-frequency"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "tree", 1, 40,
                "[A-Za-z0-9]+", "Use letters and digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "cccaaa"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public String frequencySort(String s) {
                       // @a init
                       char[] arr=s.toCharArray();
                       Map<Character,Integer> freq=new HashMap<>();
                       StringBuilder res=new StringBuilder();

                       for (char ch: arr)
                           // @a count
                           freq.put(ch,freq.getOrDefault(ch,0)+1);

                       // @a heap
                       PriorityQueue<Map.Entry<Character,Integer>> pq=new PriorityQueue<>((x,y)->(y.getValue()-x.getValue()));
                       pq.addAll(freq.entrySet());

                       while (!pq.isEmpty()) {
                           // @a poll
                           Map.Entry<Character,Integer> entry=pq.poll();
                           res.append(String.valueOf(entry.getKey()).repeat(entry.getValue()));
                       }

                       // @a done
                       return res.toString();
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        char[] arr = s.toCharArray();
        Map<Character, Integer> freq = new java.util.HashMap<>();
        StringBuilder res = new StringBuilder();
        emit.at("init").say("Count every character, then emit them most frequent first.").chars(s).step();
        for (int i = 0; i < arr.length; i++) {
            char ch = arr[i];
            freq.put(ch, freq.getOrDefault(ch, 0) + 1);
            emit.at("count").say("'%c': count %d.", ch, freq.get(ch))
                    .var("freq", freq.toString()).chars(s, i).step();
        }
        java.util.PriorityQueue<Map.Entry<Character, Integer>> pq =
                new java.util.PriorityQueue<>((x, y) -> (y.getValue() - x.getValue()));
        pq.addAll(freq.entrySet());
        emit.at("heap").say("Put the %d distinct character%s in a max-heap by count.", freq.size(), Narration.s(freq.size()))
                .var("freq", freq.toString()).chars(s).step();
        while (!pq.isEmpty()) {
            Map.Entry<Character, Integer> entry = pq.poll();
            res.append(String.valueOf(entry.getKey()).repeat(entry.getValue()));
            emit.at("poll").say("The most frequent left is '%c' (%d): append it %d time%s. res = \"%s\".",
                            entry.getKey(), entry.getValue(), entry.getValue(), Narration.s(entry.getValue()), res)
                    .var("res", res.toString()).chars(res.toString()).step();
        }
        emit.at("done").say("Return \"%s\".", res)
                .var("res", res.toString()).var("answer", res.toString()).chars(res.toString()).step();
    }
}

@Component
class MaxNestingDepthParenthesesTracer extends StringTracerSupport {
    public String id() { return "max-nesting-depth-parentheses"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("expression", "Expression", "(1+(2*3)+((8)/4))+1", 1, 50,
                "[A-Za-z0-9()+*/+ -]+", "Use letters, digits, spaces, arithmetic operators, and parentheses."));
    }

    public Map<String, Object> alternateInput() { return Map.of("expression", "(a+(b+c))"); }

    public String annotatedCode() {
        return """
               public int maxDepth(String expression) {
                   int depth = 0, maximum = 0;
                   for (char ch : expression.toCharArray()) {
                       // @a scan
                       if (ch == '(') maximum = Math.max(maximum, ++depth);
                       else if (ch == ')') depth--;
                   }
                   // @a done
                   return maximum;
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String expression = in.getString("expression");
        int depth = 0;
        int maximum = 0;
        boolean valid = true;
        for (int i = 0; i < expression.length(); i++) {
            char ch = expression.charAt(i);
            if (ch == '(') maximum = Math.max(maximum, ++depth);
            else if (ch == ')') {
                depth--;
                if (depth < 0) valid = false;
            }
            emit.at("scan").say("Index %d '%c': current depth=%d, maximum=%d.", i, ch, depth, maximum)
                    .var("index", i).var("depth", depth).var("maximum", maximum)
                    .chars(expression, i).step();
        }
        valid &= depth == 0;
        emit.at("done").say(valid ? "Maximum parenthesis nesting depth is %d."
                        : "Parentheses are unbalanced; measured maximum before the mismatch was %d.", maximum)
                .var("valid", valid).var("answer", maximum).chars(expression).step();
    }
}

/**
 * Roman to Integer (LeetCode 13), traced on the owner's own accepted submission: read right to left,
 * remembering the largest symbol seen so far ({@code high}). A symbol at least that large is added;
 * a smaller one stands before a larger one (IV, XC), so it is subtracted. O(n).
 */
@Component
class RomanToIntegerTracer extends StringTracerSupport {
    public String id() { return "roman-to-integer"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("roman", "Roman numeral", "MCMXCIV", 1, 15,
                "[IVXLCDM]+", "Use uppercase Roman symbols I, V, X, L, C, D, and M."));
    }

    public Map<String, Object> alternateInput() { return Map.of("roman", "LVIII"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public int romanToInt(String s) {
                       // @a init
                       int n=s.length();
                       Map<Character,Integer> map=new HashMap<>();
                       map.put('I',1);
                       map.put('V',5);
                       map.put('X',10);
                       map.put('L',50);
                       map.put('C',100);
                       map.put('D',500);
                       map.put('M',1000);

                       int res=0;
                       char high='I';

                       for (int i=n-1;i>=0;i--) {
                           char curr=s.charAt(i);

                           if (map.get(curr)>=map.get(high)) {
                               // @a add
                               high=curr;
                               res+=map.get(curr);
                           } else {
                               // @a subtract
                               res-=map.get(curr);
                           }
                       }

                       // @a done
                       return res;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("roman");
        Map<Character, Integer> map = Map.of('I', 1, 'V', 5, 'X', 10, 'L', 50, 'C', 100, 'D', 500, 'M', 1000);
        int n = s.length();
        int res = 0;
        char high = 'I';
        emit.at("init").say("Read from the right, keeping high = the largest symbol seen so far (start at I).")
                .var("res", 0).var("high", "I").chars(s).step();
        for (int i = n - 1; i >= 0; i--) {
            char curr = s.charAt(i);
            if (map.get(curr) >= map.get(high)) {
                high = curr;
                res += map.get(curr);
                emit.at("add").say("'%c' = %d is at least as large as everything to its right: add it. res = %d.",
                                curr, map.get(curr), res)
                        .var("i", i).var("high", String.valueOf(high)).var("res", res).chars(s, i).step();
            } else {
                res -= map.get(curr);
                emit.at("subtract").say("'%c' = %d is smaller than '%c' to its right, so it is subtracted (like the I in IV). "
                                + "res = %d.", curr, map.get(curr), high, res)
                        .var("i", i).var("high", String.valueOf(high)).var("res", res).chars(s, i).step();
            }
        }
        emit.at("done").say("Return %d.", res).var("res", res).var("answer", res).chars(s).step();
    }
}
