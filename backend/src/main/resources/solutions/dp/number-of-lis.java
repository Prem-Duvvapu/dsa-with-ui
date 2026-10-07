import java.util.Arrays;

public class Solution {
    public long solve(int[] nums) {
        int n = nums.length;
        long[] length = new long[n], count = new long[n];
        // @a init
        Arrays.fill(length, 1);
        Arrays.fill(count, 1);
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < i; j++) {
                // @a fill
                if (nums[j] < nums[i]) {
                    if (length[j] + 1 > length[i]) {
                        length[i] = length[j] + 1;
                        count[i] = count[j];
                    } else if (length[j] + 1 == length[i]) {
                        count[i] += count[j];
                    }
                }
            }
        }
        long longest = Arrays.stream(length).max().orElse(1), answer = 0;
        for (int i = 0; i < n; i++) if (length[i] == longest) answer += count[i];
        // @a done
        return answer;
    }
}
