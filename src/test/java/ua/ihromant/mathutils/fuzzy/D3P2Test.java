package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

public class D3P2Test {
    @Test
    public void testD3S() {
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
        FuzzyLiner base = FuzzyLiner.of(d31, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
    }

    @Test
    public void testP3S() {
        int[][] p31 = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {0, 7, 8, 9},
                {1, 6, 9},
                {2, 5, 9},
                {3, 4, 9},
                {1, 5, 7},
                {2, 4, 7},
                {3, 6, 7},
                {2, 6, 8},
                {3, 5, 8},
                {1, 4, 8}
        };
        FuzzyLiner base = FuzzyLiner.of(p31, new Triple[]{new Triple(0, 1, 4)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of());// TODO ContradictionUtil::processP3S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        List<FuzzyLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, liners::add);
        if (liners.size() == 1) {
            base = liners.getFirst();
        } else {
            throw new IllegalStateException();
        }
        base.printChars();
    }

    @Test
    public void testP2S() {
        int[][] p21 = new int[][]{
                {0, 1, 2, 3},
                {0, 4, 5, 6},
                {0, 7, 8, 9},
                {1, 6, 9},
                {2, 5, 9},
                {3, 4, 9},
                {1, 5, 7},
                {2, 4, 7},
                {3, 6, 7},
                {2, 6, 8},
                {3, 5, 8}
        };
        FuzzyLiner base = FuzzyLiner.of(p21, new Triple[]{new Triple(0, 1, 4)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of());// TODO ContradictionUtil::processP2S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, false, op);
        base.printChars();
        List<FuzzyLiner> liners = new ArrayList<>();
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
        FuzzyLiner base = FuzzyLiner.of(nearMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        List<FuzzyLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, liners::add);
        List<FuzzyLiner> lnrs = new ArrayList<>();
        for (FuzzyLiner l : liners) {
            try {
                l = l.intersectLines();
                l.printChars();
                l = op.apply(l);
                l.printChars();
                List<FuzzyLiner> list = new ArrayList<>();
                ContradictionUtil.multipleByContradiction(l, true, op, list::add);
                System.out.println("List " + list.size());
                List<FuzzyLiner> after = new ArrayList<>();
                for (FuzzyLiner l1 : list) {
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
        FuzzyLiner base = FuzzyLiner.of(d3, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3S));
        base.printChars();
        List<FuzzyLiner> lnrs = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, op, lnrs::add);
        System.out.println(lnrs.size());
        for (FuzzyLiner test : lnrs) {
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
    public void testD3D2S() {
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
        FuzzyLiner base = FuzzyLiner.of(d3, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD2S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
    }

    @Test
    public void testD2SD3() {
        int[][] d2s = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 6, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {2, 3, 5, 9},
                {4, 6, 9},
                {0, 7, 8}
        };
        FuzzyLiner base = FuzzyLiner.of(d2s, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = op.apply(base);
        base.printChars();
    }

    @Test
    public void testInverseD2S() {
        int[][] d2s = {
                {0, 1, 2},
                //{0, 3, 4},
                {0, 5, 6},
                {1, 3, 6, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {2, 3, 5, 9},
                {4, 6, 9},
                {0, 7, 8, 9}
        };
        FuzzyLiner base = FuzzyLiner.of(d2s, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 3, 4)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD2S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }

    @Test
    public void testD3P1S() {
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
        FuzzyLiner base = FuzzyLiner.of(d3, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1S));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }

    @Test
    public void testP1SD3() {
        int[][] p1s = new int[][]{
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
        FuzzyLiner base = FuzzyLiner.of(p1s, new Triple[]{new Triple(0, 1, 4), new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processD3));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }

    @Test
    public void testD3D11S() {
        int[][] d2s = new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 5, 7},
                {2, 6, 7},
                {3, 5, 8},
                {4, 6, 8},
                {1, 3, 6, 9},
                {2, 4, 5, 9},
                {0, 7, 8}
        };
        FuzzyLiner base = FuzzyLiner.of(d2s, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        UnaryOperator<FuzzyLiner> op = lnr -> ContradictionUtil.process(lnr, List.of(ContradictionUtil::processP1,
                ContradictionUtil::processPS, ContradictionUtil::processP1S, ContradictionUtil::processD2S, ContradictionUtil::processD3));
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        ContradictionUtil.printContradiction(base, op);
    }
}
