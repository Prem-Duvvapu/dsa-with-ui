import java.util.ArrayList;
import java.util.List;

public class Solution {
    private static final int BASE = 256;
    private static final int MOD = 101;

    public List<Integer> solve(String text, String pattern) {
        List<Integer> matches = new ArrayList<>();
        long high = 1;
        for (int index = 1; index < pattern.length(); index++) high = high * BASE % MOD;
        long patternHash = hash(pattern);
        long windowHash = 0;
        // Check the bound before forming the first substring: a longer pattern visits no windows.
        // @a window
        for (int start = 0; start + pattern.length() <= text.length(); start++) {
            if (start == 0) windowHash = hash(text.substring(0, pattern.length()));
            if (patternHash == windowHash && text.startsWith(pattern, start)) matches.add(start);
            // There is no incoming character after the final full window.
            if (start + pattern.length() < text.length()) {
                windowHash = (windowHash - text.charAt(start) * high) % MOD;
                if (windowHash < 0) windowHash += MOD;
                windowHash = (windowHash * BASE + text.charAt(start + pattern.length())) % MOD;
            }
        }
        // @a done
        return matches;
    }

    private static long hash(String value) {
        long hash = 0;
        for (int index = 0; index < value.length(); index++) hash = (hash * BASE + value.charAt(index)) % MOD;
        return hash;
    }
}
