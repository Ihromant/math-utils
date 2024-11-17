package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class D3P2Test {
    @Test
    public void test() {
        int[][] d31 = new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 6, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {2, 3, 5, 9},
                {4, 6, 9},
                {0, 7, 8, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(d31, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        List<FuzzySLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, liners::add);
        if (liners.size() == 1) {
            base = liners.getFirst();
        } else {
            throw new IllegalStateException();
        }
        base.printChars();
    }

    @Test
    public void testNearMoufang() {
        int[][] nearMoufang = new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(nearMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        List<FuzzySLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, liners::add);
        List<FuzzySLiner> lnrs = new ArrayList<>();
        for (FuzzySLiner l : liners) {
            try {
                l = l.intersectLines();
                l.printChars();
                l = op.apply(l);
                l.printChars();
                List<FuzzySLiner> list = new ArrayList<>();
                ContradictionUtil.multipleByContradiction(l, true, op, list::add);
                System.out.println("List " + list.size());
                List<FuzzySLiner> after = new ArrayList<>();
                for (FuzzySLiner l1 : list) {
                    l1.printChars();
                    ContradictionUtil.multipleByContradiction(l1, false, op, after::add);
                    System.out.println(after.size());
                }
                System.out.println("After " + after.size());
                l = ContradictionUtil.singleByContradiction(l, false, op);
                l.printChars();
                System.out.println("Added");
                lnrs.add(l);
            } catch (IllegalArgumentException e) {
                System.out.println("Exception");
                // ok
            }
        }
        System.out.println(lnrs.size());
    }

    @Test
    public void testD3D4() {
        int[][] d3 = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 6, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {2, 3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(d3, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        List<FuzzySLiner> lnrs = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, lnrs::add);
        System.out.println(lnrs.size());
        for (FuzzySLiner test : lnrs) {
            try {
                test = test.intersectLines();
                test.printChars();
                test = op.apply(test);
                test.printChars();
                test = ContradictionUtil.singleByContradiction(test, true, op);
                test.printChars();
                ContradictionUtil.multipleByContradiction(test, true, op, l -> {
                    l.printChars();
                    System.out.println("Found partial");
                    ContradictionUtil.multipleByContradiction(l, false, op, l1 -> {
                        l1.printChars();
                        System.out.println("Found example");
                    });
                });
            } catch (Exception e) {
                System.out.println("Exception");
                // ok
            }
        }
    }

    @Test
    public void testD3() {
        int[][] d3 = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 6, 7},
                {1, 4, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {2, 3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(d3, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
    }

    @Test
    public void testP2() {
        int[][] p2 = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {1, 5, 7},
                {2, 4, 7},
                {1, 6, 8},
                {2, 5, 8},
                {3, 4, 8},
                {2, 6, 9},
                {3, 5, 9},
                {0, 7, 8}
        };
        FuzzySLiner base = FuzzySLiner.of(p2, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzySLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3));
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
