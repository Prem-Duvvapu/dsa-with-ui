package com.dsa.ui.tracer.impl;

import com.dsa.ui.tracer.*;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * String to Integer (atoi) (LeetCode 8), traced on the owner's own accepted submission: skip spaces,
 * read an optional sign, skip leading zeros, then read digits into a long - stopping as soon as it
 * passes Integer.MAX_VALUE - apply the sign and clamp. O(n).
 *
 * <p>Each early {@code return 0} is highlighted on the check that decides it, so every highlight is
 * reachable from the contract inputs. The code's Character.isDigit also accepts non-ASCII digits, so
 * the input is LeetCode's: English letters, digits, ' ', '+', '-' and '.'.
 */
@Component
class StringToIntegerAtoiTracer extends StringTracerSupport {
    public String id() { return "string-to-integer-atoi"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "Input text", "   -42", 1, 40, "[A-Za-z0-9 +.\\-]+",
                "English letters, digits, spaces, '+', '-' and '.' only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "4193 with words"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public int myAtoi(String s) {
                       int n=s.length();
                       long res=0;
                       int i=0;
                       boolean isPositive=true;

                       //skip whitespaces
                       // @a spaces
                       while (i<n && s.charAt(i)==' ')
                           i++;

                       if (i==n)
                           return 0;

                       //check sign
                       // @a sign
                       if (s.charAt(i)=='-') {
                           isPositive=false;
                           i++;
                       } else if (s.charAt(i)=='+') {
                           isPositive=true;
                           i++;
                       } else if (!Character.isDigit(s.charAt(i)))
                           return 0;

                       //check zeroes
                       // @a zeros
                       while (i<n && s.charAt(i)=='0')
                           i++;

                       if (i==n || !Character.isDigit(s.charAt(i)))
                           return 0;

                       while (i<n && Character.isDigit(s.charAt(i))) {
                           // @a digit
                           res=(res*10+(s.charAt(i)-'0'));
                           i++;

                           if (res>(long)Integer.MAX_VALUE)
                               break;
                       }

                       // @a clamp
                       if (!isPositive)
                           res*=-1;

                       if (res>(long)Integer.MAX_VALUE)
                           return Integer.MAX_VALUE;
                       else if (res<(long)Integer.MIN_VALUE)
                           return Integer.MIN_VALUE;

                       // @a done
                       return (int)res;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        long res = 0;
        int i = 0;
        boolean isPositive = true;
        while (i < n && s.charAt(i) == ' ') i++;
        if (i == n) {
            emit.at("spaces").say("The string is only spaces: nothing to read. Return 0.")
                    .var("answer", 0).chars(s).step();
            return;
        }
        emit.at("spaces").say("Skip %d leading space%s; reading starts at index %d.", i, Narration.s(i), i)
                .var("i", i).chars(s, i).step();
        char c = s.charAt(i);
        if (c == '-') {
            isPositive = false;
            i++;
            emit.at("sign").say("'-': the number is negative.").var("i", i).chars(s, i - 1).step();
        } else if (c == '+') {
            i++;
            emit.at("sign").say("'+': the number is positive.").var("i", i).chars(s, i - 1).step();
        } else if (!Character.isDigit(c)) {
            emit.at("sign").say("'%c' is neither a sign nor a digit, so there is no number. Return 0.", c)
                    .var("answer", 0).chars(s, i).step();
            return;
        } else {
            emit.at("sign").say("No sign: the number is positive.").var("i", i).chars(s, i).step();
        }
        int zerosFrom = i;
        while (i < n && s.charAt(i) == '0') i++;
        if (i == n || !Character.isDigit(s.charAt(i))) {
            emit.at("zeros").say(i > zerosFrom
                            ? "Only zeros follow, then no more digits: the value is 0. Return 0."
                            : "No digit follows. Return 0.")
                    .var("answer", 0).chars(s, Math.min(i, n - 1)).step();
            return;
        }
        emit.at("zeros").say(i > zerosFrom
                        ? String.format("Skip %d leading zero%s.", i - zerosFrom, Narration.s(i - zerosFrom))
                        : "No leading zeros to skip.")
                .var("i", i).chars(s, i).step();
        while (i < n && Character.isDigit(s.charAt(i))) {
            res = res * 10 + (s.charAt(i) - '0');
            i++;
            boolean over = res > (long) Integer.MAX_VALUE;
            emit.at("digit").say(over
                            ? String.format("Digit '%c': res = %d, already past Integer.MAX_VALUE - stop reading.", s.charAt(i - 1), res)
                            : String.format("Digit '%c': res = %d.", s.charAt(i - 1), res))
                    .var("i", i).var("res", res).chars(s, i - 1).step();
            if (over) break;
        }
        if (!isPositive) res *= -1;
        int answer;
        if (res > (long) Integer.MAX_VALUE) {
            answer = Integer.MAX_VALUE;
            emit.at("clamp").say("%d is above Integer.MAX_VALUE: clamp to %d.", res, answer)
                    .var("res", res).var("answer", answer).chars(s).step();
            return;
        } else if (res < (long) Integer.MIN_VALUE) {
            answer = Integer.MIN_VALUE;
            emit.at("clamp").say("%d is below Integer.MIN_VALUE: clamp to %d.", res, answer)
                    .var("res", res).var("answer", answer).chars(s).step();
            return;
        }
        emit.at("clamp").say(isPositive ? "Positive, and within int range." : "Apply the minus sign: res = %d, within int range.",
                        res)
                .var("res", res).chars(s).step();
        emit.at("done").say("Return %d.", res).var("res", res).var("answer", (int) res).chars(s).step();
    }
}

/**
 * Count number of substrings with exactly k distinct characters (GfG), traced on the owner's own
 * accepted submission: exactly(k) = atMost(k) - atMost(k - 1), each counted with a sliding window that
 * keeps 26 counts and the number of distinct letters in it. O(n).
 */
@Component
class CountSubstringsKDistinctTracer extends StringTracerSupport {
    public String id() { return "count-substrings-k-distinct"; }

    public InputSpec inputSpec() {
        return InputSpec.of(
                patternedText("s", "String", "pqpqs", 1, 24, "[a-z]+", "Use lowercase letters a-z."),
                InputField.of("k", FieldType.INT).label("Distinct characters")
                        .range(1, 10).defaultValue(2).build());
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "abcbaa", "k", 3); }

    public String annotatedCode() {
        return """
               class Solution
               {
                   long substrCount (String s, int k)//returns no. of substrings of size=k
                   {
                       // @a init
                       return solve(s,k)-solve(s,k-1);
                   }

                   private long solve(String s,int k)//returns no. of substrings of size<=k
                   {
                       int n=s.length();
                       long ans=0;
                       int left=0;
                       int[] cnt=new int[26];
                       int distinctChar=0;

                       for (int right=0;right<n;right++)
                       {
                           // @a add
                           char ch=s.charAt(right);
                           cnt[ch-'a']++;

                           if (cnt[ch-'a']==1)
                               distinctChar++;

                           while (distinctChar>k)
                           {
                               // @a shrink
                               char temp=s.charAt(left);
                               cnt[temp-'a']--;

                               if (cnt[temp-'a']==0)
                                   distinctChar--;

                               left++;
                           }

                           // @a count
                           ans+=(right-left+1);
                       }

                       // @a total
                       return ans;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int k = in.getInt("k");
        emit.at("init").say("Substrings with exactly %d distinct letters = atMost(%d) - atMost(%d).", k, k, k - 1)
                .var("k", k).chars(s).step();
        long a = solve(s, k, emit);
        long b = solve(s, k - 1, emit);
        emit.at("init").say("%d - %d = %d substrings have exactly %d distinct letters.", a, b, a - b, k)
                .var("answer", a - b).chars(s).step();
    }

    private static long solve(String s, int k, StepEmitter emit) {
        emit.push("solve(s, " + k + ")");
        int n = s.length();
        long ans = 0;
        int left = 0;
        int[] cnt = new int[26];
        int distinctChar = 0;
        for (int right = 0; right < n; right++) {
            char ch = s.charAt(right);
            cnt[ch - 'a']++;
            if (cnt[ch - 'a'] == 1) distinctChar++;
            emit.at("add").say("solve(%d): take in '%c'. %d distinct letter%s in s[%d..%d].",
                            k, ch, distinctChar, Narration.s(distinctChar), left, right)
                    .var("k", k).var("left", left).var("right", right).var("distinctChar", distinctChar).var("ans", ans)
                    .chars(s, right, left).step();
            while (distinctChar > k) {
                char temp = s.charAt(left);
                cnt[temp - 'a']--;
                if (cnt[temp - 'a'] == 0) distinctChar--;
                left++;
                emit.at("shrink").say("More than %d distinct: drop '%c'. %d distinct left.", k, temp, distinctChar)
                        .var("k", k).var("left", left).var("right", right).var("distinctChar", distinctChar).var("ans", ans)
                        .chars(s, right, left).step();
            }
            ans += right - left + 1;
            emit.at("count").say("%d substring%s %s at %d with at most %d distinct. ans = %d.",
                            right - left + 1, Narration.s(right - left + 1),
                            Narration.plural(right - left + 1, "ends", "end"), right, k, ans)
                    .var("k", k).var("left", left).var("right", right).var("distinctChar", distinctChar).var("ans", ans)
                    .chars(s, right, left).step();
        }
        emit.at("total").say("solve(s, %d) = %d.", k, ans).var("k", k).var("ans", ans).chars(s).step();
        emit.pop();
        return ans;
    }
}

/**
 * Longest Palindromic Substring (LeetCode 5), traced on the owner's own accepted submission: expand
 * around every centre. Each centre first absorbs the run of identical letters around it - which is
 * what makes even-length palindromes ("bb") work without a second pass - then grows outward while the
 * two ends match. O(n^2), the expected interview answer.
 */
@Component
class LongestPalindromicSubstringTracer extends StringTracerSupport {
    public String id() { return "longest-palindromic-substring"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "babad", 1, 30,
                "[A-Za-z0-9]+", "Use letters and digits only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "cbbd"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public String longestPalindrome(String s) {
                       // @a init
                       int n=s.length();
                       int maxLen=0;
                       int start=-1;
                       int currLen=0;

                       for (int i=0;i<n;i++) {
                           // @a center
                           int left=i-1;
                           int right=i+1;

                           while (left>=0 && s.charAt(left)==s.charAt(i))
                               left--;

                           while (right<n && s.charAt(right)==s.charAt(i))
                               right++;

                           while (left>=0 && right<n) {
                               if (s.charAt(left)==s.charAt(right)) {
                                   // @a expand
                                   left--;
                                   right++;
                               } else {
                                   break;
                               }
                           }

                           currLen=right-left-1;
                           if (currLen>maxLen) {
                               // @a best
                               maxLen=currLen;
                               start=left+1;
                           }
                       }

                       // @a done
                       return s.substring(start,start+maxLen);
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int maxLen = 0;
        int start = -1;
        emit.at("init").say("Try every index as the centre of a palindrome and grow it outward.").chars(s).step();
        for (int i = 0; i < n; i++) {
            int left = i - 1;
            int right = i + 1;
            while (left >= 0 && s.charAt(left) == s.charAt(i)) left--;
            while (right < n && s.charAt(right) == s.charAt(i)) right++;
            emit.at("center").say("Centre %d ('%c'). The run of '%c' around it is s[%d..%d] = \"%s\".",
                            i, s.charAt(i), s.charAt(i), left + 1, right - 1, s.substring(left + 1, right))
                    .var("i", i).var("maxLen", maxLen).chars(s, left + 1, right - 1).step();
            while (left >= 0 && right < n) {
                if (s.charAt(left) == s.charAt(right)) {
                    left--;
                    right++;
                    emit.at("expand").say("s[%d] = s[%d] = '%c': grow to \"%s\".",
                                    left + 1, right - 1, s.charAt(left + 1), s.substring(left + 1, right))
                            .var("i", i).var("maxLen", maxLen).chars(s, left + 1, right - 1).step();
                } else {
                    break;
                }
            }
            int currLen = right - left - 1;
            if (currLen > maxLen) {
                maxLen = currLen;
                start = left + 1;
                emit.at("best").say("\"%s\" (length %d) is the longest so far.", s.substring(start, start + maxLen), maxLen)
                        .var("i", i).var("maxLen", maxLen).var("best", s.substring(start, start + maxLen))
                        .chars(s, start, start + maxLen - 1).step();
            }
        }
        String answer = s.substring(start, start + maxLen);
        emit.at("done").say("Return \"%s\".", answer).var("answer", answer).chars(s, start, start + maxLen - 1).step();
    }
}

/**
 * Sum of Beauty of All Substrings (LeetCode 1781), traced on the owner's own accepted submission: for
 * every start i, extend the substring one letter at a time, keeping 26 counts, and add (most frequent
 * - least frequent among letters present) for each. O(26 * n^2).
 */
@Component
class SumBeautyAllSubstringsTracer extends StringTracerSupport {
    public String id() { return "sum-beauty-all-substrings"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "String", "aabcb", 1, 12,
                "[a-z]+", "Use lowercase letters a-z."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "aabcbaa"); }

    public String annotatedCode() {
        return """
               class Solution {
                   public int beautySum(String s) {
                       // @a init
                       int n=s.length();
                       int totalSum=0;
                       int[] freq=new int[26];
                       int minFreq=Integer.MAX_VALUE;
                       int maxFreq=Integer.MIN_VALUE;

                       for (int i=0;i<n;i++) {
                           // @a start
                           Arrays.fill(freq,0);

                           for (int j=i;j<n;j++) {
                               int pos=s.charAt(j)-'a';
                               freq[pos]++;

                               minFreq=Integer.MAX_VALUE;
                               maxFreq=Integer.MIN_VALUE;

                               for (int k=0;k<26;k++) {
                                   if (freq[k]!=0 && freq[k]<minFreq)
                                       minFreq=Math.min(minFreq,freq[k]);

                                   maxFreq=Math.max(maxFreq,freq[k]);
                               }

                               // @a add
                               totalSum+=(maxFreq-minFreq);
                           }
                       }

                       // @a done
                       return totalSum;
                   }
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        int n = s.length();
        int totalSum = 0;
        int[] freq = new int[26];
        emit.at("init").say("Beauty = (most frequent count) - (least frequent count, among letters present). Sum it over "
                        + "every substring.")
                .var("totalSum", 0).chars(s).step();
        for (int i = 0; i < n; i++) {
            java.util.Arrays.fill(freq, 0);
            emit.at("start").say("Substrings starting at %d: reset the counts.", i)
                    .var("i", i).var("totalSum", totalSum).chars(s, i).step();
            for (int j = i; j < n; j++) {
                freq[s.charAt(j) - 'a']++;
                int minFreq = Integer.MAX_VALUE;
                int maxFreq = Integer.MIN_VALUE;
                for (int k = 0; k < 26; k++) {
                    if (freq[k] != 0 && freq[k] < minFreq) minFreq = Math.min(minFreq, freq[k]);
                    maxFreq = Math.max(maxFreq, freq[k]);
                }
                totalSum += maxFreq - minFreq;
                emit.at("add").say("\"%s\": most frequent %d, least %d, beauty %d. totalSum = %d.",
                                s.substring(i, j + 1), maxFreq, minFreq, maxFreq - minFreq, totalSum)
                        .var("i", i).var("j", j).var("totalSum", totalSum).chars(s, i, j).step();
            }
        }
        emit.at("done").say("Return %d.", totalSum).var("totalSum", totalSum).var("answer", totalSum).chars(s).step();
    }
}

@Component
class ReverseEveryWordTracer extends StringTracerSupport {
    public String id() { return "reverse-every-word"; }

    public InputSpec inputSpec() {
        return InputSpec.of(patternedText("s", "Sentence", "Let's take LeetCode contest", 1, 60,
                "[A-Za-z' ]+", "Use letters, apostrophes, and spaces only."));
    }

    public Map<String, Object> alternateInput() { return Map.of("s", "God Ding"); }

    public String annotatedCode() {
        return """
               public String reverseEveryWord(String s) {
                   char[] chars = s.toCharArray();
                   int start = 0;
                   while (start < chars.length) {
                       int end = start;
                       while (end < chars.length && chars[end] != ' ') end++;
                       // @a reverse
                       reverse(chars, start, end - 1);
                       start = end + 1;
                   }
                   // @a done
                   return new String(chars);
               }""";
    }

    public void run(Inputs in, StepEmitter emit) {
        String s = in.getString("s");
        char[] chars = s.toCharArray();
        int start = 0;
        while (start < chars.length) {
            if (chars[start] == ' ') {
                start++;
                continue;
            }
            int end = start;
            while (end < chars.length && chars[end] != ' ') end++;
            int left = start;
            int right = end - 1;
            while (left < right) {
                char temporary = chars[left];
                chars[left++] = chars[right];
                chars[right--] = temporary;
            }
            String current = new String(chars);
            emit.at("reverse").say("Reverse word [%d,%d]; sentence becomes \"%s\".", start, end - 1, current)
                    .var("wordStart", start).var("wordEnd", end - 1).var("current", current)
                    .chars(current, start, end - 1).step();
            start = end + 1;
        }
        String answer = new String(chars);
        emit.at("done").say("Every word reversed in place while spaces stay fixed: \"%s\".", answer)
                .var("answer", answer).chars(answer).step();
    }
}
