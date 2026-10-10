import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Solution {
    public int[] solve(int[] nums) {
        // @a init
        int n = nums.length;
        int[] dp = new int[n], parent = new int[n];
        Arrays.fill(dp, 1);
        Arrays.fill(parent, -1);
        int bestEnd = 0;
        for (int i = 1; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // @a compare
                if (nums[j] < nums[i] && dp[j] + 1 > dp[i]) {
                    // @a take
                    dp[i] = dp[j] + 1;
                    parent[i] = j;
                }
            }
            // Strict improvement keeps the first predecessor and endpoint on a tie.
            // @a best
            if (dp[i] > dp[bestEnd]) bestEnd = i;
        }
        List<Integer> reversed = new ArrayList<>();
        int cursor = bestEnd;
        while (cursor != -1) {
            // @a backlink
            reversed.add(nums[cursor]);
            cursor = parent[cursor];
        }
        int[] answer = new int[reversed.size()];
        for (int i = 0; i < answer.length; i++) {
            answer[i] = reversed.get(answer.length - 1 - i);
        }
        // @a done
        return answer;
    }
}
