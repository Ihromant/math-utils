package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
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
        base.printChars();
        base = base.intersectLines();
        base.printChars();
        base = enhanceP1(base);
        base.printChars();
        base = singleByContradiction(base, true, this::enhanceP1);
        base.printChars();
        multipleByContradiction(base, true, this::enhanceP1, l -> {
            l.printChars();
            System.out.println("Found partial");
            multipleByContradiction(l, false, this::enhanceP1, l1 -> {
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
        base = singleByContradiction(base, true, this::enhancePS);
        base.printChars();
        multipleByContradiction(base, true, this::enhancePS, l -> {
            l.printChars();
            System.out.println("Found partial");
            multipleByContradiction(l, false, this::enhancePS, l1 -> {
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

    private void multipleByContradiction(FuzzySLiner base, boolean onlyDist, UnaryOperator<FuzzySLiner> op, Consumer<FuzzySLiner> sink) {
        recur(base, onlyDist, l -> {
            try {
                FuzzySLiner next = op.apply(l);
                sink.accept(next);
            } catch (IllegalArgumentException e) {
                // ok
            }
        });
    }

    private void recur(FuzzySLiner base, boolean onlyDist, Consumer<FuzzySLiner> sink) {
        Pair p = base.undefinedPair();
        if (p != null) {
            try {
                Queue<Rel> rels = new ArrayDeque<>();
                rels.add(new Same(p.f(), p.s()));
                FuzzySLiner copy = base.copy();
                copy.update(rels);
                recur(copy, onlyDist, sink);
            } catch (IllegalArgumentException e) {
                // ok
            }
            try {
                Queue<Rel> rels = new ArrayDeque<>();
                rels.add(new Dist(p.f(), p.s()));
                FuzzySLiner copy = base.copy();
                copy.update(rels);
                recur(copy, onlyDist, sink);
            } catch (IllegalArgumentException e) {
                // ok
            }
            return;
        }
        if (onlyDist) {
            sink.accept(base);
            return;
        }
        Triple tr = base.undefinedTriple();
        if (tr == null) {
            sink.accept(base);
            return;
        }
        try {
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Col(tr.f(), tr.s(), tr.t()));
            FuzzySLiner copy = base.copy();
            copy.update(rels);
            recur(copy, onlyDist, sink);
        } catch (IllegalArgumentException e) {
            // ok
        }
        try {
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Trg(tr.f(), tr.s(), tr.t()));
            FuzzySLiner copy = base.copy();
            copy.update(rels);
            recur(copy, onlyDist, sink);
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private FuzzySLiner singleByContradiction(FuzzySLiner ln, boolean onlyDist, UnaryOperator<FuzzySLiner> op) {
        List<Pair> pairs = ln.undefinedPairs();
        Queue<Rel> q = new ConcurrentLinkedDeque<>();
        pairs.stream().parallel().forEach(p -> {
            Boolean dist = identifyDistinction(ln, p, op);
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
        FuzzySLiner afterDist = op.apply(ln);
        if (onlyDist) {
            return afterDist;
        }
        List<Triple> triples = afterDist.undefinedTriples();
        Queue<Rel> q1 = new ConcurrentLinkedDeque<>();
        triples.stream().parallel().forEach(tr -> {
            Boolean coll = identifyCollinearity(afterDist, tr, op);
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
        return op.apply(afterDist);
    }

    private Boolean identifyDistinction(FuzzySLiner l, Pair p, UnaryOperator<FuzzySLiner> op) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Same(p.f(), p.s()));
            copy.update(rels);
            op.apply(copy);
        } catch (IllegalArgumentException e) {
            result = true;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Dist(p.f(), p.s()));
            copy.update(rels);
            op.apply(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = false;
        }
        return result;
    }

    private Boolean identifyCollinearity(FuzzySLiner l, Triple t, UnaryOperator<FuzzySLiner> op) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Col(t.f(), t.s(), t.t()));
            copy.update(rels);
            op.apply(copy);
        } catch (IllegalArgumentException e) {
            result = false;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Trg(t.f(), t.s(), t.t()));
            copy.update(rels);
            op.apply(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = true;
        }
        return result;
    }
}
