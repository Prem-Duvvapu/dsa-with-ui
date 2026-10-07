import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class Solution {
    // Input values are distinct positive integers.
    public List<Integer> solve(int[] nums) {
        Arrays.sort(nums);
        int n = nums.length;
        int[] length = new int[n], parent = new int[n];
        // @a init
        Arrays.fill(length, 1);
        Arrays.fill(parent, -1);
        int end = 0;
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // @a fill
                if (nums[i] % nums[j] == 0 && length[j] + 1 > length[i]) {
                    length[i] = length[j] + 1;
                    parent[i] = j;
                }
            }
            if (length[i] > length[end]) end = i;
        }
        List<Integer> subset = new ArrayList<>();
        // @a reconstruct
        for (int at = end; at >= 0; at = parent[at]) subset.add(nums[at]);
        Collections.reverse(subset);
        // @a done
        return subset;
    }
}
