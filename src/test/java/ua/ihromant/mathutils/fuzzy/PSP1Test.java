package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.UnaryOperator;

public class PSP1Test {
    @Test
    public void testPS() {
        int[][] ps = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {1, 5, 7},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 8},
                {1, 6, 9},
                {3, 4, 9},
                {0, 7, 8}
        };
        FuzzySLiner base = FuzzySLiner.of(ps, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, op);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, op, l -> {
            try {
                l.printChars();
                System.out.println("Found partial");
                l = ContradictionUtil.singleByContradiction(l, false, op);
                l.printChars();
                ContradictionUtil.multipleByContradiction(l, false, op, l1 -> {
                    l1.printChars();
                    System.out.println("Found example");
                });
            } catch (IllegalArgumentException e) {
                System.out.println("Exception partial");
                // ok
            }
        });
    }

    @Test
    public void testP1() {
        int[][] p1 = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {1, 5, 7},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 8},
                {1, 6, 9},
                {3, 4, 9},
                {2, 5, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(p1, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processPS));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, op);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, op, l -> {
            try {
                l.printChars();
                System.out.println("Found partial");
                l = ContradictionUtil.singleByContradiction(l, false, op);
                l.printChars();
                ContradictionUtil.multipleByContradiction(l, false, op, l1 -> {
                    l1.printChars();
                    System.out.println("Found example");
                });
            } catch (IllegalArgumentException e) {
                System.out.println("Exception partial");
                // ok
            }
        });
    }
}
