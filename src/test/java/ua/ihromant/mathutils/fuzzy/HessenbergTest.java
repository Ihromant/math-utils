package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

public class HessenbergTest {
    @Test
    public void test() {
        int[][] desargues = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner base = FuzzySLiner.of(desargues, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(7, 8, 9)});
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhancePappus(base);
        base.printChars();
    }

    private FuzzySLiner enhancePappus(FuzzySLiner liner) {
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
                                                if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(bc1b1c, ac1a1c, i)) {
                                                    queue.add(new Col(bc1b1c, ac1a1c, i));
                                                }
                                            }
                                            if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                                if (bc1b1c < 0) {
                                                    bc1b1c = i;
                                                } else {
                                                    queue.add(new Same(bc1b1c, i));
                                                }
                                                if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, i)) {
                                                    queue.add(new Col(ac1a1c, ab1a1b, i));
                                                }
                                            }
                                            if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                                if (ac1a1c < 0) {
                                                    ac1a1c = i;
                                                } else {
                                                    queue.add(new Same(ac1a1c, i));
                                                }
                                                if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(ab1a1b, bc1b1c, i)) {
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
            //System.out.println("Enhancing fano " + cnt++ + " iteration, changes " + queue.size());
            liner.update(queue);
            //System.out.println("Before " + liner.getPc());
            liner = liner.quotient();
            //System.out.println("After " + liner.getPc());
        }
    }

    private FuzzySLiner intersect6(FuzzySLiner liner) {
        for (int d = 0; d < liner.getPc(); d++) {
            for (int c = 0; c < d; c++) {
                if (!liner.distinct(d, c)) {
                    continue;
                }
                for (int b = 0; b < c; b++) {
                    if (!liner.triangle(b, c, d)) {
                        continue;
                    }
                    for (int a = 0; a < b; a++) {
                        if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(a, b, c)) {
                            continue;
                        }
                        int abcd = -1;
                        int acbd = -1;
                        int adbc = -1;
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                abcd = i;
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                acbd = i;
                            }
                            if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                adbc = i;
                            }
                        }
                        Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
                        if (abcd >= 0 && acbd >= 0 && adbc < 0) {
                            queue.add(new Col(a, d, liner.getPc()));
                            queue.add(new Col(b, c, liner.getPc()));
                        }
                        if (abcd >= 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, liner.getPc()));
                            queue.add(new Col(b, d, liner.getPc()));
                        }
                        if (abcd < 0 && acbd >= 0 && adbc >= 0) {
                            queue.add(new Col(a, b, liner.getPc()));
                            queue.add(new Col(c, d, liner.getPc()));
                        }
                        if (!queue.isEmpty()) {
                            //System.out.println(a + " " + b + " " + c + " " + d);
                            FuzzySLiner res = liner.addPoints(1);
                            res.update(queue);
                            return res;
                        }
                    }
                }
            }
        }
        return null;
    }

    private FuzzySLiner singleByContradiction(FuzzySLiner ln, boolean onlyDist) {
        List<Pair> pairs = ln.undefinedPairs();
        Queue<Rel> q = new ConcurrentLinkedDeque<>();
        pairs.stream().parallel().forEach(p -> {
            Boolean dist = identifyDistinction(ln, p);
            if (dist == null) {
                return;
            }
            if (dist) {
                q.add(new Dist(p.f(), p.s()));
            } else {
                q.add(new Same(p.f(), p.s()));
            }
        });
        ln.update(q);
        FuzzySLiner afterDist = enhancePappus(ln);
        if (onlyDist) {
            return afterDist;
        }
        List<Triple> triples = afterDist.undefinedTriples();
        Queue<Rel> q1 = new ConcurrentLinkedDeque<>();
        triples.stream().parallel().forEach(tr -> {
            Boolean coll = identifyCollinearity(afterDist, tr);
            if (coll == null) {
                return;
            }
            if (coll) {
                q1.add(new Col(tr.f(), tr.s(), tr.t()));
            } else {
                q1.add(new Trg(tr.f(), tr.s(), tr.t()));
            }
        });
        afterDist.update(q1);
        return enhancePappus(afterDist);
    }

    private Boolean identifyDistinction(FuzzySLiner l, Pair p) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Same(p.f(), p.s()));
            copy.update(rels);
            enhancePappus(copy);
        } catch (IllegalArgumentException e) {
            result = true;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Dist(p.f(), p.s()));
            copy.update(rels);
            enhancePappus(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = false;
        }
        return result;
    }

    private Boolean identifyCollinearity(FuzzySLiner l, Triple t) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Col(t.f(), t.s(), t.t()));
            copy.update(rels);
            enhancePappus(copy);
        } catch (IllegalArgumentException e) {
            result = false;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Trg(t.f(), t.s(), t.t()));
            copy.update(rels);
            enhancePappus(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = true;
        }
        return result;
    }

    private List<FuzzySLiner> multipleByContradiction(FuzzySLiner base) {
        return recur(base).stream().<FuzzySLiner>mapMulti((l, sink) -> {
            try {
                sink.accept(enhancePappus(l));
            } catch (IllegalArgumentException e) {
                // ok
            }
        }).collect(Collectors.toList());
    }

    private List<FuzzySLiner> recur(FuzzySLiner base) {
        List<FuzzySLiner> result = new ArrayList<>();
        Pair p = base.undefinedPair();
        if (p != null) {
            try {
                Queue<Rel> rels = new ArrayDeque<>();
                rels.add(new Same(p.f(), p.s()));
                FuzzySLiner copy = base.copy();
                copy.update(rels);
                result.addAll(recur(copy));
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                Queue<Rel> rels = new ArrayDeque<>();
                rels.add(new Dist(p.f(), p.s()));
                FuzzySLiner copy = base.copy();
                copy.update(rels);
                result.addAll(recur(copy));
            } catch (IllegalArgumentException e) {
                // ok
            }
            return result;
        }
        Triple tr = base.undefinedTriple();
        if (tr == null) {
            return List.of(base);
        }
        try {
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Col(tr.f(), tr.s(), tr.t()));
            FuzzySLiner copy = base.copy();
            copy.update(rels);
            result.addAll(recur(copy));
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Trg(tr.f(), tr.s(), tr.t()));
            FuzzySLiner copy = base.copy();
            copy.update(rels);
            result.addAll(recur(copy));
        } catch (IllegalArgumentException e) {
            // ok
        }
        return result;
    }

    private FuzzySLiner intersect56(FuzzySLiner liner) {
        int pt = liner.getPc();
        Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
        for (int a = 0; a < liner.getPc(); a++) {
            for (int b = a + 1; b < liner.getPc(); b++) {
                if (!liner.distinct(a, b)) {
                    continue;
                }
                for (int c = b + 1; c < liner.getPc(); c++) {
                    if (!liner.triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = c + 1; d < liner.getPc(); d++) {
                        if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(b, c, d)) {
                            continue;
                        }
                        int abcd = -1;
                        int acbd = -1;
                        int adbc = -1;
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                abcd = i;
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                acbd = i;
                            }
                            if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                adbc = i;
                            }
                        }
                        if (abcd >= 0 && acbd >= 0 && adbc < 0) {
                            queue.add(new Col(a, d, pt));
                            queue.add(new Col(b, c, pt));
                            pt++;
                        }
                        if (abcd >= 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd >= 0 && adbc >= 0) {
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            pt++;
                        }
                        if (abcd >= 0 && acbd < 0 && adbc < 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            pt++;
                            queue.add(new Col(a, d, pt));
                            queue.add(new Col(b, c, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd >= 0 && adbc < 0) {
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            pt++;
                            queue.add(new Col(a, d, pt));
                            queue.add(new Col(b, c, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            pt++;
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            pt++;
                        }
                    }
                }
            }
        }
        System.out.println("Expanding 5-6 from " + liner.getPc() + " to " + pt);
        liner = liner.addPoints(pt - liner.getPc());
        liner.update(queue);
        return enhancePappus(liner);
    }
}
