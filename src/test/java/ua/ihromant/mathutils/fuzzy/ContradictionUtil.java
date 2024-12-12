package ua.ihromant.mathutils.fuzzy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ContradictionUtil {
    public static void multipleByContradiction(FuzzySLiner base, boolean onlyDist, UnaryOperator<FuzzySLiner> op, Consumer<FuzzySLiner> sink) {
        recur(base, onlyDist, l -> {
            try {
                FuzzySLiner next = op.apply(l);
                sink.accept(next);
            } catch (IllegalArgumentException e) {
                // ok
            }
        });
    }

    public static void recur(FuzzySLiner base, boolean onlyDist, Consumer<FuzzySLiner> sink) {
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
            rels.add(new Trg(tr.f(), tr.s(), tr.t()));
            FuzzySLiner copy = base.copy();
            copy.update(rels);
            recur(copy, onlyDist, sink);
        } catch (IllegalArgumentException e) {
            // ok
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
    }

    public static FuzzySLiner singleByContradiction(FuzzySLiner ln, boolean onlyDist, UnaryOperator<FuzzySLiner> op) {
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

    public static Boolean identifyDistinction(FuzzySLiner l, Pair p, UnaryOperator<FuzzySLiner> op) {
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

    public static Boolean identifyCollinearity(FuzzySLiner l, Triple t, UnaryOperator<FuzzySLiner> op) {
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

    public static FuzzySLiner process(FuzzySLiner liner, List<Function<FuzzySLiner, List<Rel>>> processors) {
        while (true) {
            FuzzySLiner lnr = liner;
            Queue<Rel> queue = processors.stream().parallel().flatMap(p -> p.apply(lnr).stream())
                    .collect(Collectors.toCollection(ArrayDeque::new));
            if (queue.isEmpty()) {
                return liner;
            }
            liner.update(queue);
            liner = liner.quotient();
        }
    }

    public static List<Rel> processP(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                            }
                                            if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(bc1b1c, ac1a1c, i)) {
                                                res.add(new Col(bc1b1c, ac1a1c, i));
                                            }
                                        }
                                        if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                            if (bc1b1c < 0) {
                                                bc1b1c = i;
                                            }
                                            if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, i)) {
                                                res.add(new Col(ac1a1c, ab1a1b, i));
                                            }
                                        }
                                        if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                            if (ac1a1c < 0) {
                                                ac1a1c = i;
                                            }
                                            if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(ab1a1b, bc1b1c, i)) {
                                                res.add(new Col(ab1a1b, bc1b1c, i));
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
        return res;
    }

    public static List<Rel> processP1(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int a = 0; a < pc; a++) {
            for (int b = 0; b < pc; b++) {
                if (!liner.distinct(a, b)) {
                    continue;
                }
                for (int c = 0; c < pc; c++) {
                    if (!liner.collinear(a, b, c)) {
                        continue;
                    }
                    for (int a1 = 0; a1 < pc; a1++) {
                        if (!liner.triangle(a, b, a1) || !liner.triangle(a, c, a1) || !liner.triangle(b, c, a1)) {
                            continue;
                        }
                        for (int b1 = 0; b1 < pc; b1++) {
                            if (!liner.triangle(a, a1, b1) || !liner.triangle(b, a1, b1) || !liner.triangle(c, a1, b1)) {
                                continue;
                            }
                            int c1 = -1;
                            int ab1a1b = -1;
                            int ac1a1c = -1;
                            int bc1b1c = -1;
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a1, b, i) && liner.collinear(a, b1, i)) {
                                    ab1a1b = i;
                                    break;
                                }
                            }
                            if (ab1a1b < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a1, b1, i) && liner.collinear(c, ab1a1b, i)) {
                                    c1 = i;
                                    break;
                                }
                            }
                            if (c1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                    bc1b1c = i;
                                }
                                if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                    ac1a1c = i;
                                }
                                if (ac1a1c >= 0 && bc1b1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, bc1b1c)) {
                                    res.add(new Col(ac1a1c, ab1a1b, bc1b1c));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processPS(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int a1 = 0; a1 < pc; a1++) {
                    if (!liner.triangle(o, a, a1)) {
                        continue;
                    }
                    for (int ab1a1b = 0; ab1a1b < pc; ab1a1b++) {
                        if (!liner.triangle(o, a, ab1a1b) || !liner.triangle(o, a1, ab1a1b) || !liner.triangle(a, a1, ab1a1b)) {
                            continue;
                        }
                        for (int ac1a1c = 0; ac1a1c < pc; ac1a1c++) {
                            if (!liner.collinear(o, ab1a1b, ac1a1c)) {
                                continue;
                            }
                            int b = -1;
                            int b1 = -1;
                            int c = -1;
                            int c1 = -1;
                            int bc1b1c = -1;
                            for (int i = 0; i < pc; i++) {
                                if (liner.collinear(o, a, i) && liner.collinear(a1, ab1a1b, i)) {
                                    b = i;
                                }
                                if (liner.collinear(o, a1, i) && liner.collinear(a, ab1a1b, i)) {
                                    b1 = i;
                                }
                                if (liner.collinear(o, a, i) && liner.collinear(a1, ac1a1c, i)) {
                                    c = i;
                                }
                                if (liner.collinear(o, a1, i) && liner.collinear(a, ac1a1c, i)) {
                                    c1 = i;
                                }
                            }
                            if (b < 0 || b1 < 0 || c < 0 || c1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < pc; i++) {
                                if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                    bc1b1c = i;
                                    break;
                                }
                            }
                            if (bc1b1c < 0) {
                                continue;
                            }
                            res.add(new Col(ab1a1b, ac1a1c, bc1b1c));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processP1S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int a1 = 0; a1 < pc; a1++) {
                    if (!liner.triangle(o, a, a1)) {
                        continue;
                    }
                    for (int ab1a1b = 0; ab1a1b < pc; ab1a1b++) {
                        if (!liner.triangle(o, a, ab1a1b) || !liner.triangle(o, a1, ab1a1b) || !liner.triangle(a, a1, ab1a1b)) {
                            continue;
                        }
                        int b = -1;
                        int b1 = -1;
                        int ac1a1c = -1;
                        int c = -1;
                        int c1 = -1;
                        int bc1b1c = -1;
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(o, a, i) && liner.collinear(a1, ab1a1b, i)) {
                                b = i;
                            }
                            if (liner.collinear(o, a1, i) && liner.collinear(a, ab1a1b, i)) {
                                b1 = i;
                            }
                        }
                        if (b < 0 || b1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(b, b1, i) && liner.collinear(o, ab1a1b, i)) {
                                ac1a1c = i;
                                break;
                            }
                        }
                        if (ac1a1c < 0) {
                            continue;
                        }
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(a, ac1a1c, i) && liner.collinear(o, a1, i)) {
                                c1 = i;
                            }
                            if (liner.collinear(a1, ac1a1c, i) && liner.collinear(o, a, i)) {
                                c = i;
                            }
                        }
                        if (c < 0 || c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < liner.getPc(); i++) {
                            if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                bc1b1c = i;
                                break;
                            }
                        }
                        if (bc1b1c < 0) {
                            continue;
                        }
                        res.add(new Col(ab1a1b, ac1a1c, bc1b1c));
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processP2S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                                res.add(new Same(ab1a1b, i));
                                            }
                                            if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(bc1b1c, ac1a1c, i) && liner.collinear(c, c1, i)
                                                    && liner.collinear(o, bc1b1c, ac1a1c) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(b, b1, ac1a1c))) {
                                                res.add(new Col(bc1b1c, ac1a1c, i));
                                            }
                                            if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(c, c1, i) && liner.collinear(bc1b1c, ac1a1c, i)
                                                    && liner.collinear(o, bc1b1c, ac1a1c) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(b, b1, ac1a1c))) {
                                                res.add(new Col(c, c1, i));
                                            }
                                        }
                                        if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                            if (bc1b1c < 0) {
                                                bc1b1c = i;
                                            } else {
                                                res.add(new Same(bc1b1c, i));
                                            }
                                            if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, i) && liner.collinear(a, a1, i)
                                                    && liner.collinear(o, ab1a1b, ac1a1c) && (liner.collinear(c, c1, ab1a1b) || liner.collinear(b, b1, ac1a1c))) {
                                                res.add(new Col(ac1a1c, ab1a1b, i));
                                            }
                                            if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(a, a1, i) && liner.collinear(ac1a1c, ab1a1b, i)
                                                    && liner.collinear(o, ab1a1b, ac1a1c) && (liner.collinear(c, c1, ab1a1b) || liner.collinear(b, b1, ac1a1c))) {
                                                res.add(new Col(a, a1, i));
                                            }
                                        }
                                        if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                            if (ac1a1c < 0) {
                                                ac1a1c = i;
                                            } else {
                                                res.add(new Same(ac1a1c, i));
                                            }
                                            if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(ab1a1b, bc1b1c, i) && liner.collinear(b, b1, i)
                                                    && liner.collinear(o, bc1b1c, ab1a1b) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(c, c1, ab1a1b))) {
                                                res.add(new Col(ab1a1b, bc1b1c, i));
                                            }
                                            if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(b, b1, i) && liner.collinear(ab1a1b, bc1b1c, i)
                                                    && liner.collinear(o, bc1b1c, ab1a1b) && (liner.collinear(a, a1, bc1b1c) || liner.collinear(c, c1, ab1a1b))) {
                                                res.add(new Col(b, b1, i));
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
        return res;
    }

    public static List<Rel> processP3S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                                res.add(new Same(ab1a1b, i));
                                            }
                                            if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(bc1b1c, ac1a1c, i) && liner.collinear(c, c1, i)
                                                    && liner.collinear(o, bc1b1c, ac1a1c) && liner.collinear(a, a1, bc1b1c) && liner.collinear(b, b1, ac1a1c)) {
                                                res.add(new Col(bc1b1c, ac1a1c, i));
                                            }
                                            if (bc1b1c >= 0 && ac1a1c >= 0 && !liner.collinear(c, c1, i) && liner.collinear(bc1b1c, ac1a1c, i)
                                                    && liner.collinear(o, bc1b1c, ac1a1c) && liner.collinear(a, a1, bc1b1c) && liner.collinear(b, b1, ac1a1c)) {
                                                res.add(new Col(c, c1, i));
                                            }
                                        }
                                        if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                            if (bc1b1c < 0) {
                                                bc1b1c = i;
                                            } else {
                                                res.add(new Same(bc1b1c, i));
                                            }
                                            if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(ac1a1c, ab1a1b, i) && liner.collinear(a, a1, i)
                                                    && liner.collinear(o, ab1a1b, ac1a1c) && liner.collinear(c, c1, ab1a1b) && liner.collinear(b, b1, ac1a1c)) {
                                                res.add(new Col(ac1a1c, ab1a1b, i));
                                            }
                                            if (ab1a1b >= 0 && ac1a1c >= 0 && !liner.collinear(a, a1, i) && liner.collinear(ac1a1c, ab1a1b, i)
                                                    && liner.collinear(o, ab1a1b, ac1a1c) && liner.collinear(c, c1, ab1a1b) && liner.collinear(b, b1, ac1a1c)) {
                                                res.add(new Col(a, a1, i));
                                            }
                                        }
                                        if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                            if (ac1a1c < 0) {
                                                ac1a1c = i;
                                            } else {
                                                res.add(new Same(ac1a1c, i));
                                            }
                                            if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(ab1a1b, bc1b1c, i) && liner.collinear(b, b1, i)
                                                    && liner.collinear(o, bc1b1c, ab1a1b) && liner.collinear(a, a1, bc1b1c) && liner.collinear(c, c1, ab1a1b)) {
                                                res.add(new Col(ab1a1b, bc1b1c, i));
                                            }
                                            if (ab1a1b >= 0 && bc1b1c >= 0 && !liner.collinear(b, b1, i) && liner.collinear(ab1a1b, bc1b1c, i)
                                                    && liner.collinear(o, bc1b1c, ab1a1b) && liner.collinear(a, a1, bc1b1c) && liner.collinear(c, c1, ab1a1b)) {
                                                res.add(new Col(b, b1, i));
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
        return res;
    }

    public static List<Rel> processD3(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int b = 0; b < pc; b++) {
                    if (!liner.triangle(o, a, b)) {
                        continue;
                    }
                    for (int c = 0; c < pc; c++) {
                        if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c) || !liner.triangle(a, b, c)) {
                            continue;
                        }
                        int aba1b1 = -1;
                        int bcb1c1 = -1;
                        int aca1c1 = -1;
                        int a1 = -1;
                        int b1 = -1;
                        int c1 = -1;
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(o, a, i) && liner.collinear(b, c, i)) {
                                if (a1 < 0) {
                                    a1 = i;
                                }
                            }
                            if (liner.collinear(o, b, i) && liner.collinear(a, c, i)) {
                                if (b1 < 0) {
                                    b1 = i;
                                }
                            }
                            if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                if (c1 < 0) {
                                    c1 = i;
                                }
                            }
                        }
                        if (a1 < 0 || b1 < 0 || c1 < 0 || !liner.triangle(a1, b1, c1)) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                if (aba1b1 < 0) {
                                    aba1b1 = i;
                                }
                                if (bcb1c1 >= 0 && aca1c1 >= 0 && !liner.collinear(bcb1c1, aca1c1, i)) {
                                    res.add(new Col(bcb1c1, aca1c1, i));
                                }
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                if (aca1c1 < 0) {
                                    aca1c1 = i;
                                }
                                if (aba1b1 >= 0 && bcb1c1 >= 0 && !liner.collinear(aba1b1, bcb1c1, i)) {
                                    res.add(new Col(aba1b1, bcb1c1, i));
                                }
                            }
                            if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                if (bcb1c1 < 0) {
                                    bcb1c1 = i;
                                }
                                if (aba1b1 >= 0 && aca1c1 >= 0 && !liner.collinear(aba1b1, aca1c1, i)) {
                                    res.add(new Col(aba1b1, aca1c1, i));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processD2(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int b = 0; b < pc; b++) {
                    if (!liner.triangle(o, a, b)) {
                        continue;
                    }
                    for (int c = 0; c < pc; c++) {
                        if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c) || !liner.triangle(a, b, c)) {
                            continue;
                        }
                        for (int a1 = 0; a1 < pc; a1++) {
                            if (!liner.collinear(o, a, a1)) {
                                continue;
                            }
                            int aba1b1 = -1;
                            int bcb1c1 = -1;
                            int aca1c1 = -1;
                            int b1 = -1;
                            int c1 = -1;
                            for (int i = 0; i < pc; i++) {
                                if (liner.collinear(o, b, i) && liner.collinear(a, c, i)) {
                                    if (b1 < 0) {
                                        b1 = i;
                                    } else {
                                        res.add(new Same(b1, i));
                                    }
                                }
                                if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
                                    } else {
                                        res.add(new Same(c1, i));
                                    }
                                }
                            }
                            if (b1 < 0 || c1 < 0 || !liner.triangle(a1, b1, c1)) {
                                continue;
                            }
                            for (int i = 0; i < pc; i++) {
                                if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                    if (aba1b1 < 0) {
                                        aba1b1 = i;
                                    } else {
                                        res.add(new Same(aba1b1, i));
                                    }
                                    if (bcb1c1 >= 0 && aca1c1 >= 0 && !liner.collinear(bcb1c1, aca1c1, i)) {
                                        res.add(new Col(bcb1c1, aca1c1, i));
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                    if (aca1c1 < 0) {
                                        aca1c1 = i;
                                    } else {
                                        res.add(new Same(aca1c1, i));
                                    }
                                    if (aba1b1 >= 0 && bcb1c1 >= 0 && !liner.collinear(aba1b1, bcb1c1, i)) {
                                        res.add(new Col(aba1b1, bcb1c1, i));
                                    }
                                }
                                if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                    if (bcb1c1 < 0) {
                                        bcb1c1 = i;
                                    } else {
                                        res.add(new Same(bcb1c1, i));
                                    }
                                    if (aba1b1 >= 0 && aca1c1 >= 0 && !liner.collinear(aba1b1, aca1c1, i)) {
                                        res.add(new Col(aba1b1, aca1c1, i));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processD2S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
        for (int o = 0; o < pc; o++) {
            for (int a = 0; a < pc; a++) {
                if (!liner.distinct(o, a)) {
                    continue;
                }
                for (int b = 0; b < pc; b++) {
                    if (!liner.triangle(o, a, b)) {
                        continue;
                    }
                    for (int c = 0; c < pc; c++) {
                        if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c) || !liner.triangle(a, b, c)) {
                            continue;
                        }
                        int aba1b1 = -1;
                        int bcb1c1 = -1;
                        int aca1c1 = -1;
                        int a1 = -1;
                        int b1 = -1;
                        int c1 = -1;
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(o, a, i) && liner.collinear(b, c, i)) {
                                if (a1 < 0) {
                                    a1 = i;
                                } else {
                                    res.add(new Same(a1, i));
                                }
                            }
                            if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                if (c1 < 0) {
                                    c1 = i;
                                } else {
                                    res.add(new Same(c1, i));
                                }
                            }
                        }
                        if (a1 < 0 || c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                if (aca1c1 < 0) {
                                    aca1c1 = i;
                                } else {
                                    res.add(new Same(aca1c1, i));
                                }
                            }
                        }
                        if (aca1c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(o, aca1c1, i) && liner.collinear(a, b, i)) {
                                if (aba1b1 < 0) {
                                    aba1b1 = i;
                                } else {
                                    res.add(new Same(aba1b1, i));
                                }
                            }
                            if (liner.collinear(o, aca1c1, i) && liner.collinear(b, c, i)) {
                                if (bcb1c1 < 0) {
                                    bcb1c1 = i;
                                } else {
                                    res.add(new Same(bcb1c1, i));
                                }
                            }
                        }
                        if (aba1b1 < 0 || bcb1c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(a, aba1b1, i) && liner.collinear(o, b, i)) {
                                if (b1 < 0) {
                                    b1 = i;
                                } else {
                                    res.add(new Same(aba1b1, i));
                                }
                            }
                            if (liner.collinear(c, bcb1c1, i) && liner.collinear(o, b, i)) {
                                if (b1 < 0) {
                                    b1 = i;
                                } else {
                                    res.add(new Same(aba1b1, i));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processD1S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                        || !liner.collinear(a, c, b1)) {
                                    continue;
                                }
                                for (int c1 = 0; c1 < pc; c1++) {
                                    if (!liner.collinear(o, c, c1)) {
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
                                                res.add(new Same(aba1b1, i));
                                            }
                                            if (bcb1c1 >= 0 && aca1c1 >= 0 && liner.collinear(o, bcb1c1, aca1c1) && !liner.collinear(bcb1c1, aca1c1, i)) {
                                                res.add(new Col(bcb1c1, aca1c1, i));
                                            }
                                        }
                                        if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                            if (aca1c1 < 0) {
                                                aca1c1 = i;
                                            } else {
                                                res.add(new Same(aca1c1, i));
                                            }
                                            if (aba1b1 >= 0 && bcb1c1 >= 0 && liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, bcb1c1, i)) {
                                                res.add(new Col(aba1b1, bcb1c1, i));
                                            }
                                        }
                                        if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                            if (bcb1c1 < 0) {
                                                bcb1c1 = i;
                                            } else {
                                                res.add(new Same(bcb1c1, i));
                                            }
                                            if (aba1b1 >= 0 && aca1c1 >= 0 && liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, i)) {
                                                res.add(new Col(aba1b1, aca1c1, i));
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
        return res;
    }

    public static List<Rel> processD3S(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                                res.add(new Same(aba1b1, i));
                                            }
                                            if (bcb1c1 >= 0 && aca1c1 >= 0 && liner.collinear(o, bcb1c1, aca1c1) && !liner.collinear(bcb1c1, aca1c1, i)) {
                                                res.add(new Col(bcb1c1, aca1c1, i));
                                            }
                                        }
                                        if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                            if (aca1c1 < 0) {
                                                aca1c1 = i;
                                            } else {
                                                res.add(new Same(aca1c1, i));
                                            }
                                            if (aba1b1 >= 0 && bcb1c1 >= 0 && liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, bcb1c1, i)) {
                                                res.add(new Col(aba1b1, bcb1c1, i));
                                            }
                                        }
                                        if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                            if (bcb1c1 < 0) {
                                                bcb1c1 = i;
                                            } else {
                                                res.add(new Same(bcb1c1, i));
                                            }
                                            if (aba1b1 >= 0 && aca1c1 >= 0 && liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, i)) {
                                                res.add(new Col(aba1b1, aca1c1, i));
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
        return res;
    }

    public static List<Rel> processFullFano(FuzzySLiner liner) {
        List<Rel> res = new ArrayList<>(liner.getPc());
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
                                if (abcd < 0) {
                                    abcd = i;
                                } else {
                                    res.add(new Same(abcd, i));
                                }
                                if (acbd >= 0 && adbc >= 0 && !liner.collinear(acbd, adbc, i)) {
                                    res.add(new Col(acbd, adbc, i));
                                }
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                if (acbd < 0) {
                                    acbd = i;
                                } else {
                                    res.add(new Same(acbd, i));
                                }
                                if (abcd >= 0 && adbc >= 0 && !liner.collinear(abcd, adbc, i)) {
                                    res.add(new Col(abcd, adbc, i));
                                }
                            }
                            if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                if (adbc < 0) {
                                    adbc = i;
                                } else {
                                    res.add(new Same(adbc, i));
                                }
                                if (abcd >= 0 && acbd >= 0 && !liner.collinear(abcd, acbd, i)) {
                                    res.add(new Col(abcd, acbd, i));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}
