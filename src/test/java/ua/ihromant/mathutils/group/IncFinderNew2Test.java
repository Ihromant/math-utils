package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.LInc;
import ua.ihromant.mathutils.MaxLexCheck2;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongPredicate;

public class IncFinderNew2Test {
    @Test
    public void testBfs() {
        int v = 25;
        int k = 4;
        int r = (v - 1) / (k - 1);
        int b = v * (v - 1) / k / (k - 1);
        LInc zero = new LInc(new long[0], v, k);
        List<LInc> firstStage = new ArrayList<>();
        expand(zero, r, firstStage::add);
        List<LInc> secondStage = new ArrayList<>();
        for (LInc next : firstStage) {
            expand(next, 2 * r - 4, secondStage::add);
        }
        System.out.println(secondStage.size());
        //secondStage.stream().parallel().forEach(next -> expand(next, b - 2 * r + 1, li -> System.out.println(li.toLines())));
    }

    private static void expand(LInc base, int depth, Consumer<LInc> cons) {
        if (depth == 0) {
            cons.accept(base);
            return;
        }
        LongPredicate canAdd = MaxLexCheck2.extender(base);   // base analysed once, reused for every candidate
        for (int[] bl : base.blocks()) {
            long line = MaxLexCheck2.toMask(bl);
            if (!canAdd.test(line)) {
                continue;
            }
            expand(base.addLine(line), depth - 1, cons);
        }
    }
}
