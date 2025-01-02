package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

public class PSP1Test {
    @Test
    public void testP2() {
        int[][] ps = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {1, 6, 7},
                {2, 5, 7},
                {3, 4, 7},
                {2, 6, 8},
                {3, 5, 8},
                {1, 4, 8},
                {1, 5, 9},
                {2, 4, 9},
                //{3, 6, 9}
        };
        FuzzyLiner base = FuzzyLiner.of(ps, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)}).liner();
        Function<FuzzyLiner, LinerHistory> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1S, ContradictionUtil::processD3));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines().liner();
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, op);
        base.printChars();
    }

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
        FuzzyLiner base = FuzzyLiner.of(ps, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)}).liner();
        Function<FuzzyLiner, LinerHistory> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1,
                ContradictionUtil::processD2S, ContradictionUtil::processD3));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines().liner();
        base.printChars();
        base = op.apply(base).liner();
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
        FuzzyLiner base = FuzzyLiner.of(p1, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)}).liner();
        Function<FuzzyLiner, LinerHistory> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processPS,
                ContradictionUtil::processD2S, ContradictionUtil::processD3));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines().liner();
        base.printChars();
        base = op.apply(base).liner();
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
