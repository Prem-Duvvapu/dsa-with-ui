import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Solution {
    public int solve(String words) {
        String[] sorted = words.split(",");
        Arrays.sort(sorted, Comparator.comparingInt(String::length).thenComparing(s -> s));
        int[] length = new int[sorted.length], parent = new int[sorted.length];
        // @a init
        Arrays.fill(length, 1);
        Arrays.fill(parent, -1);
        int best = 0, end = 0;
        for (int i = 0; i < sorted.length; i++) {
            for (int j = 0; j < i; j++) {
                // @a fill
                if (isPredecessor(sorted[j], sorted[i]) && length[j] + 1 > length[i]) {
                    length[i] = length[j] + 1;
                    parent[i] = j;
                }
            }
            if (length[i] > best) {
                best = length[i];
                end = i;
            }
        }
        List<String> chain = new ArrayList<>();
        // @a reconstruct
        for (int at = end; at >= 0; at = parent[at]) chain.add(sorted[at]);
        Collections.reverse(chain); // One chain attaining the returned length.
        // @a done
        return best;
    }

    private boolean isPredecessor(String small, String large) {
        if (large.length() != small.length() + 1) return false;
        int i = 0, j = 0;
        while (i < small.length() && j < large.length()) {
            if (small.charAt(i) == large.charAt(j)) i++;
            j++;
        }
        return i == small.length();
    }
}
