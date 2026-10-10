import java.util.ArrayDeque;
import java.util.Deque;

public class Solution {
    public int[] solve(int[] asteroids) {
        // @a init
        Deque<Integer> stack = new ArrayDeque<>();
        for (int a : asteroids) {
            boolean alive = true;
            while (alive && a < 0 && !stack.isEmpty() && stack.peek() > 0) {
                if (stack.peek() < -a) {
                    // @a topExplodes
                    stack.pop();
                } else if (stack.peek() == -a) {
                    // @a bothExplode
                    stack.pop();
                    alive = false;
                } else {
                    // @a currentExplodes
                    alive = false;
                }
            }
            if (alive) {
                // @a push
                stack.push(a);
            }
        }
        // @a done
        return toArray(stack);
    }

    private int[] toArray(Deque<Integer> stack) {
        int[] answer = new int[stack.size()];
        int index = answer.length - 1;
        // Stack iteration visits the rightmost survivor first; output is left-to-right.
        for (int asteroid : stack) answer[index--] = asteroid;
        return answer;
    }
}
