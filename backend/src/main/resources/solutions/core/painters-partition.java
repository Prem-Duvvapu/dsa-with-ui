public class Solution {
    public int solve(int[] boards, int painters) {
        int low = max(boards), high = sum(boards), ans = high;
        // @a init
        while (low <= high) {
            // @a mid
            int mid = low + (high - low) / 2;
            if (paintersNeeded(boards, mid) <= painters) {
                // @a feasible
                ans = mid;
                high = mid - 1;
            } else {
                // @a infeasible
                low = mid + 1;
            }
        }
        // @a done
        return ans;
    }

    private int paintersNeeded(int[] boards, int cap) {
        int painters = 1, time = 0;
        for (int b : boards) {
            if (time + b > cap) {
                // @a newPainter
                painters++;
                time = b;
            } else {
                // @a addToCurrent
                time += b;
            }
        }
        return painters;
    }

    private int max(int[] boards) {
        int largest = boards[0];
        for (int b : boards) largest = Math.max(largest, b);
        return largest;
    }

    private int sum(int[] boards) {
        int total = 0;
        for (int b : boards) total += b;
        return total;
    }
}
