package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.LInc;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class IncFinderNewTest {
    @Test
    public void testBfs() {
        int v = 15;
        int k = 3;
        int r = (v - 1) / (k - 1);
        int b = v * (v - 1) / k / (k - 1);
        LInc zero = new LInc(new long[0], v, k);
        List<LInc> firstStage = new ArrayList<>();
        expand(zero, r, firstStage::add);
        List<LInc> secondStage = new ArrayList<>();
        for (LInc next : firstStage) {
            expand(next, r - 1, secondStage::add);
        }
        System.out.println(secondStage.size());
        secondStage.stream().parallel().forEach(next -> expand(next, b - 2 * r + 1, li -> System.out.println(li.toLines())));
    }

    private static void expand(LInc base, int depth, Consumer<LInc> cons) {
        if (depth == 0) {
            cons.accept(base);
            return;
        }
        for (int[] bl : base.blocks()) {
            LInc next = base.addLine(bl);
            if (!next.isMaxLex()) {
                continue;
            }
            expand(next, depth - 1, cons);
        }
    }
}
