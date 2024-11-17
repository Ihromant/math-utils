package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhanceP1(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, this::enhanceP1);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, this::enhanceP1, l -> {
            l.printChars();
            System.out.println("Found partial");
            ContradictionUtil.multipleByContradiction(l, false, this::enhanceP1, l1 -> {
                l1.printChars();
                System.out.println("Found example");
            });
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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhancePS(base);
        base.printChars();
        base = ContradictionUtil.singleByContradiction(base, true, this::enhancePS);
        base.printChars();
        ContradictionUtil.multipleByContradiction(base, true, this::enhancePS, l -> {
            l.printChars();
            System.out.println("Found partial");
            ContradictionUtil.multipleByContradiction(l, false, this::enhancePS, l1 -> {
                l1.printChars();
                System.out.println("Found example");
            });
        });
    }

    private FuzzySLiner enhanceP1(FuzzySLiner liner) {
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
                                                        && (liner.collinear(a, a1, bc1b1c) || liner.collinear(b, b1, ac1a1c))) {
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
                                                        && (liner.collinear(c, c1, ab1a1b) || liner.collinear(b, b1, ac1a1c))) {
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
                                                        && (liner.collinear(a, a1, bc1b1c) || liner.collinear(c, c1, ab1a1b))) {
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

    private FuzzySLiner enhancePS(FuzzySLiner liner) {
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
                                                        && liner.collinear(o, bc1b1c, ac1a1c)) {
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
                                                        && liner.collinear(o, ab1a1b, ac1a1c)) {
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
                                                        && liner.collinear(o, bc1b1c, ab1a1b)) {
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
}
