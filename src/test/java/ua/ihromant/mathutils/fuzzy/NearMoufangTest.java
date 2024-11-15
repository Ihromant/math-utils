package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Queue;

public class NearMoufangTest {
    @Test
    public void test() {
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
        System.out.println(base.undefinedTriples());
        base = base.intersectLines();
        base.printChars();
        base = enhanceWeakPappus(base);
        base.printChars();
    }

    private FuzzySLiner enhanceWeakPappus(FuzzySLiner liner) {
        while (true) {
            int pc = liner.getPc();
            Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
            for (int a = 0; a < pc; a++) {
                for (int b = a + 1; b < pc; b++) {
                    if (!liner.distinct(a, b)) {
                        continue;
                    }
                    for (int c = b + 1; c < pc; c++) {
                        if (!liner.collinear(a, b, c)) {
                            continue;
                        }
                        for (int a1 = 0; a1 < pc; a1++) {
                            if (!liner.distinct(a1, a) || !liner.distinct(a1, b) || !liner.distinct(a1, c)) {
                                continue;
                            }
                            for (int b1 = 0; b1 < pc; b1++) {
                                if (!liner.distinct(b1, a) || !liner.distinct(b1, b) || !liner.distinct(b1, c) || !liner.distinct(b1, a1)) {
                                    continue;
                                }
                                int o = liner.intersection(a1, b1, a, b);
                                for (int c1 = 0; c1 < pc; c1++) {
                                    if (!liner.collinear(a1, b1, c1) || !liner.distinct(c1, a) || !liner.distinct(c1, b) || !liner.distinct(c1, c)) {
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
                                                    && (liner.collinear(o, bc1b1c, ac1a1c) || liner.collinear(a, a1, bc1b1c) || liner.collinear(b, b1, ac1a1c))) {
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
                                                    && (liner.collinear(o, ab1a1b, ac1a1c) || liner.collinear(c, c1, ab1a1b) || liner.collinear(b, b1, ac1a1c))) {
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
                                                    && (liner.collinear(o, bc1b1c, ab1a1b) || liner.collinear(a, a1, bc1b1c) || liner.collinear(c, c1, ab1a1b))) {
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
            if (queue.isEmpty()) {
                return liner;
            }
            //System.out.println("Enhancing fano " + cnt++ + " iteration, changes " + queue.size());
            liner.update(queue);
            //System.out.println("Before " + liner.getPc());
            liner = liner.quotient();
            //System.out.println("After " + liner.getPc());
        }
    }
}
