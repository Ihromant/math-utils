package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.function.UnaryOperator;

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
        FuzzySLiner base = FuzzySLiner.of(ps, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1S, ContradictionUtil::processD3));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.intersectLines();
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
        FuzzySLiner base = FuzzySLiner.of(ps, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1,
                ContradictionUtil::processD2S, ContradictionUtil::processD3));
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        base = base.addPoints(4);
        ArrayDeque<Rel> q = new ArrayDeque<>();
        q.addAll(List.of(new Col(7, 9, 10), new Col(8, 9, 11), new Col(7, 9, 12), new Col(8, 9, 13)));
        q.addAll(List.of(new Col(0, 1, 10), new Col(0, 3, 11), new Col(0, 4, 12), new Col(0, 6, 13)));
        base.update(q);
        base.printChars();
        base = op.apply(base);
        base = ContradictionUtil.singleByContradiction(base, false, op);
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
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processPS,
                ContradictionUtil::processD2S, ContradictionUtil::processD3));
        base.printChars();
        base = base.addPoints(6);
        Queue<Rel> q = new ArrayDeque<>();
        q.addAll(List.of(new Col(7, 8, 10), new Col(7, 9, 11), new Col(8, 9, 12), new Col(7, 8, 13), new Col(7, 9, 14), new Col(8, 9, 15)));
        q.addAll(List.of(new Col(0, 5, 11), new Col(0, 5, 12), new Col(10, 11, 12), new Col(0, 2, 14), new Col(0, 2, 15), new Col(13, 14, 15)));
        base.update(q);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
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
