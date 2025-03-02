package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

public class Applicator1Test {
    @Test
    public void findPossible() {
        int v = 53;
        int k = 6;
        IntList il = new IntList(v);
        find(v, k, 0, 0, 0, il);
    }

    private static void find(int v, int k, int left, int right, int inter, IntList lst) {
        if (left == v - 1 && right == v - 1 && inter == v) {
            System.out.println(Arrays.toString(lst.toArray()));
            return;
        }
        if (left >= v || right >= v || inter > v) {
            return;
        }
        int prev = lst.isEmpty() ? 0 : lst.get(lst.size() - 1);
        for (int i = prev; i <= k; i++) {
            IntList nextLst = lst.copy();
            nextLst.add(i);
            find(v, k, left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst);
        }
    }
}
