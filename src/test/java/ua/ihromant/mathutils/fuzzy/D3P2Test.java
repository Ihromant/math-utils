package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhanceD31(base);
        base.printChars();
        List<FuzzySLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, this::enhanceD31, liners::add);
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
        base.printChars();
        List<FuzzySLiner> liners = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, this::enhanceD31, liners::add);
        List<FuzzySLiner> lnrs = new ArrayList<>();
        for (FuzzySLiner l : liners) {
            try {
                l = l.intersectLines();
                l.printChars();
                l = enhanceD31(l);
                l.printChars();
                List<FuzzySLiner> list = new ArrayList<>();
                ContradictionUtil.multipleByContradiction(l, true, this::enhanceD31, list::add);
                System.out.println("List " + list.size());
                List<FuzzySLiner> after = new ArrayList<>();
                for (FuzzySLiner l1 : list) {
                    l1.printChars();
                    ContradictionUtil.multipleByContradiction(l1, false, this::enhanceD31, after::add);
                    System.out.println(after.size());
                }
                System.out.println("After " + after.size());
                l = ContradictionUtil.singleByContradiction(l, false, this::enhanceD31);
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
        base.printChars();
        List<FuzzySLiner> lnrs = new ArrayList<>();
        ContradictionUtil.multipleByContradiction(base, false, this::enhanceD31, lnrs::add);
        System.out.println(lnrs.size());
        for (FuzzySLiner test : lnrs) {
            try {
                test = test.intersectLines();
                test.printChars();
                test = enhanceD31(test);
                test.printChars();
                test = ContradictionUtil.singleByContradiction(test, true, this::enhanceD31);
                test.printChars();
                ContradictionUtil.multipleByContradiction(test, true, this::enhanceD31, l -> {
                    l.printChars();
                    System.out.println("Found partial");
                    ContradictionUtil.multipleByContradiction(l, false, this::enhanceD31, l1 -> {
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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhanceP2(base);
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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhanceD3(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, this::enhanceD3);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, this::enhanceD3, l -> {
            try {
                l.printChars();
                System.out.println("Found partial");
                l = ContradictionUtil.singleByContradiction(l, false, this::enhanceD3);
                l.printChars();
                ContradictionUtil.multipleByContradiction(l, false, this::enhanceD3, l1 -> {
                    l1.printChars();
                    System.out.println("Found example");
                });
            } catch (IllegalArgumentException e) {
                System.out.println("Exception partial");
                // ok
            }
        });
    }

    private FuzzySLiner enhanceP2(FuzzySLiner liner) {
        while (true) {
            int pc = liner.getPc();
            Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
            for (int o = 0; o < pc; o++) {
                for (int a = 0; a < pc; a++) {
                    if (!liner.distinct(o, a)) {
                        continue;
                    }
                    for (int b = a + 1; b < pc; b++) {
                        if (!liner.collinear(o, a, b)) {
                            continue;
                        }
                        for (int c = b + 1; c < pc; c++) {
                            if (!liner.collinear(o, a, c) || !liner.collinear(a, b, c)) {
                                continue;
                            }
                            for (int a1 = 0; a1 < pc; a1++) {
                                if (!liner.triangle(o, a, a1)) {
                                    continue;
                                }
                                for (int b1 = 0; b1 < pc; b1++) {
                                    if (!liner.collinear(o, a1, b1)) {
                                        continue;
                                    }
                                    for (int c1 = 0; c1 < pc; c1++) {
                                        if (!liner.collinear(o, a1, c1) || !liner.collinear(a1, b1, c1)) {
                                            continue;
                                        }
                                        int ab1a1b = -1;
                                        int ac1a1c = -1;
                                        int bc1b1c = -1;
                                        for (int i = 0; i < liner.getPc(); i++) {
                                            if (liner.collinear(a, b1, i) && liner.collinear(a1, b, i)) {
                                                if (ab1a1b < 0) {
                                                    ab1a1b = i;
                                                } else {
                                                    queue.add(new Same(ab1a1b, i));
                                                }
                                                if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(bc1b1c, ac1a1c, i)
                                                        && liner.collinear(o, bc1b1c, ac1a1c) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(b, b1, ac1a1c))) {
                                                    queue.add(new Col(bc1b1c, ac1a1c, i));
                                                }
                                            }
                                            if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                                if (bc1b1c < 0) {
                                                    bc1b1c = i;
                                                } else {
                                                    queue.add(new Same(bc1b1c, i));
                                                }
                                                if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, i)
                                                        && liner.collinear(o, ab1a1b, ac1a1c) && (liner.collinear(c, c1, ab1a1b) || liner.collinear(b, b1, ac1a1c))) {
                                                    queue.add(new Col(ac1a1c, ab1a1b, i));
                                                }
                                            }
                                            if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                                if (ac1a1c < 0) {
                                                    ac1a1c = i;
                                                } else {
                                                    queue.add(new Same(ac1a1c, i));
                                                }
                                                if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(ab1a1b, bc1b1c, i)
                                                        && liner.collinear(o, bc1b1c, ab1a1b) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(c, c1, ab1a1b))) {
                                                    queue.add(new Col(ab1a1b, bc1b1c, i));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) {
                return liner;
            }
            liner.update(queue);
            liner = liner.quotient();
        }
    }

    private FuzzySLiner enhanceD3(FuzzySLiner liner) {
        while (true) {
            int pc = liner.getPc();
            Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
            for (int o = 0; o < pc; o++) {
                for (int a = 0; a < pc; a++) {
                    if (!liner.distinct(o, a)) {
                        continue;
                    }
                    for (int a1 = 0; a1 < pc; a1++) {
                        if (!liner.collinear(o, a, a1)) {
                            continue;
                        }
                        for (int b = 0; b < pc; b++) {
                            if (!liner.triangle(o, a, b)) {
                                continue;
                            }
                            for (int b1 = 0; b < pc; b++) {
                                if (!liner.collinear(o, b, b1)) {
                                    continue;
                                }
                                for (int c = 0; c < pc; c++) {
                                    if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c) || !liner.triangle(a, b, c)
                                        || !liner.collinear(a, c, b1) || !liner.collinear(b, c, a1)) {
                                        continue;
                                    }
                                    for (int c1 = 0; c1 < pc; c1++) {
                                        if (!liner.collinear(o, c, c1)
                                            || !liner.collinear(a, b, c1)) {
                                            continue;
                                        }
                                        int aba1b1 = -1;
                                        int bcb1c1 = -1;
                                        int aca1c1 = -1;
                                        for (int i = 0; i < liner.getPc(); i++) {
                                            if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                                if (aba1b1 < 0) {
                                                    aba1b1 = i;
                                                } else {
                                                    queue.add(new Same(aba1b1, i));
                                                }
                                                if (bcb1c1 >= 0 && aca1c1 >= 0 && !liner.collinear(bcb1c1, aca1c1, i)) {
                                                    queue.add(new Col(bcb1c1, aca1c1, i));
                                                }
                                            }
                                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                                if (aca1c1 < 0) {
                                                    aca1c1 = i;
                                                } else {
                                                    queue.add(new Same(aca1c1, i));
                                                }
                                                if (aba1b1 >= 0 && bcb1c1 >= 0 && !liner.collinear(aba1b1, bcb1c1, i)) {
                                                    queue.add(new Col(aba1b1, bcb1c1, i));
                                                }
                                            }
                                            if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                                if (bcb1c1 < 0) {
                                                    bcb1c1 = i;
                                                } else {
                                                    queue.add(new Same(bcb1c1, i));
                                                }
                                                if (aba1b1 >= 0 && aca1c1 >= 0 && !liner.collinear(aba1b1, aca1c1, i)) {
                                                    queue.add(new Col(aba1b1, aca1c1, i));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) {
                return liner;
            }
            liner.update(queue);
            liner = liner.quotient();
        }
    }

    private FuzzySLiner enhanceD31(FuzzySLiner liner) {
        while (true) {
            int pc = liner.getPc();
            Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
            for (int o = 0; o < pc; o++) {
                for (int a = 0; a < pc; a++) {
                    if (!liner.distinct(o, a)) {
                        continue;
                    }
                    for (int a1 = 0; a1 < pc; a1++) {
                        if (!liner.collinear(o, a, a1)) {
                            continue;
                        }
                        for (int b = 0; b < pc; b++) {
                            if (!liner.triangle(o, a, b)) {
                                continue;
                            }
                            for (int b1 = 0; b < pc; b++) {
                                if (!liner.collinear(o, b, b1)) {
                                    continue;
                                }
                                for (int c = 0; c < pc; c++) {
                                    if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c) || !liner.triangle(a, b, c)
                                            || !liner.collinear(a, c, b1) || !liner.collinear(b, c, a1)) {
                                        continue;
                                    }
                                    for (int c1 = 0; c1 < pc; c1++) {
                                        if (!liner.collinear(o, c, c1)
                                                || !liner.collinear(a, b, c1)) {
                                            continue;
                                        }
                                        int aba1b1 = -1;
                                        int bcb1c1 = -1;
                                        int aca1c1 = -1;
                                        for (int i = 0; i < liner.getPc(); i++) {
                                            if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                                if (aba1b1 < 0) {
                                                    aba1b1 = i;
                                                } else {
                                                    queue.add(new Same(aba1b1, i));
                                                }
                                                if (bcb1c1 >= 0 && aca1c1 >= 0 && liner.collinear(o, bcb1c1, aca1c1) && !liner.collinear(bcb1c1, aca1c1, i)) {
                                                    queue.add(new Col(bcb1c1, aca1c1, i));
                                                }
                                            }
                                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                                if (aca1c1 < 0) {
                                                    aca1c1 = i;
                                                } else {
                                                    queue.add(new Same(aca1c1, i));
                                                }
                                                if (aba1b1 >= 0 && bcb1c1 >= 0 && liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, bcb1c1, i)) {
                                                    queue.add(new Col(aba1b1, bcb1c1, i));
                                                }
                                            }
                                            if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                                if (bcb1c1 < 0) {
                                                    bcb1c1 = i;
                                                } else {
                                                    queue.add(new Same(bcb1c1, i));
                                                }
                                                if (aba1b1 >= 0 && aca1c1 >= 0 && liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, i)) {
                                                    queue.add(new Col(aba1b1, aca1c1, i));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (queue.isEmpty()) {
                return liner;
            }
            liner.update(queue);
            liner = liner.quotient();
        }
    }
}
