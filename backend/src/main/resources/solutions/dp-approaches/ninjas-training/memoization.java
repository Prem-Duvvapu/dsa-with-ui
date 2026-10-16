public class Solution {
    public int solve(int[][] points) {
        // @a start
        Integer[][] memo = new Integer[points.length][3];
        int answer = 0;
        for (int task = 0; task < 3; task++) {
            // @a reduce
            answer = Math.max(answer, score(points.length - 1, task, points, memo));
        }
        // @a done
        return answer;
    }
    private int score(int day, int task, int[][] points, Integer[][] memo) {
        // @a enter
        if (memo[day][task] != null) {
            // @a hit
            return memo[day][task];
        }
        if (day == 0) {
            // @a base
            return memo[day][task] = points[0][task];
        }
        // @a branch
        int previous = 0;
        for (int other = 0; other < 3; other++) {
            if (other != task) previous = Math.max(previous, score(day - 1, other, points, memo));
        }
        // @a combine
        return memo[day][task] = points[day][task] + previous;
    }
}
