package ua.ihromant.mathutils.fuzzy;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.SequencedMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class ContradictionUtil {
    public static void printContradiction(FuzzyLiner base, UnaryOperator<FuzzyLiner> op) {
        try {
            op.apply(base);
        } catch (ContradictionException e) {
            Rel rel = e.rel();
            Rel opposite = switch (rel) {
                case Dist(int a, int b) -> new Same(a, b);
                case Same(int a, int b) -> new Dist(a, b);
                case Col(int a, int b, int c) -> new Trg(a, b, c);
                case Trg(int a, int b, int c) -> new Col(a, b, c);
            };
            System.out.println("From one side: ");
            SequencedMap<Rel, Update> stack = new LinkedHashMap<>();
            reconstruct(rel, base.getRelations(), stack);
            for (Update u : stack.reversed().values()) {
                System.out.println(u.base() + " follows from " + u.reasonName() + " due to "
                        + Arrays.stream(u.reasons()).map(Object::toString).collect(Collectors.joining(" ")));
            }
            System.out.println("But from the other side: ");
            stack = new LinkedHashMap<>();
            reconstruct(opposite, base.getRelations(), stack);
            for (Update u : stack.reversed().values()) {
                System.out.println(u.base() + " follows from " + u.reasonName() + " due to "
                        + Arrays.stream(u.reasons()).map(Object::toString).collect(Collectors.joining(" ")));
            }
            System.out.println("Contradiction");
        }
    }

    private static void reconstruct(Rel rel, Map<Rel, Update> updates, SequencedMap<Rel, Update> stack) {
        if (stack.containsKey(rel)) {
            return;
        }
        Update u = updates.get(rel);
        stack.put(rel, u);
        for (Rel r : u.reasons()) {
            reconstruct(r, updates, stack);
        }
    }

    public static void multipleByContradiction(FuzzyLiner base, boolean onlyDist, UnaryOperator<FuzzyLiner> op, Consumer<FuzzyLiner> sink) {
        recur(base, onlyDist, l -> {
            try {
                FuzzyLiner next = op.apply(l);
                sink.accept(next);
            } catch (ContradictionException e) {
                // ok
            }
        });
    }

    public static void recur(FuzzyLiner base, boolean onlyDist, Consumer<FuzzyLiner> sink) {
//        Pair p = base.undefinedPair();
//        if (p != null) {
//            try {
//                Queue<Rel> rels = new ArrayDeque<>();
//                rels.add(new Same(p.f(), p.s()));
//                FuzzyLiner copy = base.copy();
//                copy.update(rels);
//                recur(copy, onlyDist, sink);
//            } catch (ContradictionException e) {
//                // ok
//            }
//            try {
//                Queue<Rel> rels = new ArrayDeque<>();
//                rels.add(new Dist(p.f(), p.s()));
//                FuzzyLiner copy = base.copy();
//                copy.update(rels);
//                recur(copy, onlyDist, sink);
//            } catch (ContradictionException e) {
//                // ok
//            }
//            return;
//        }
//        if (onlyDist) {
//            sink.accept(base);
//            return;
//        }
//        Triple tr = base.undefinedTriple();
//        if (tr == null) {
//            sink.accept(base);
//            return;
//        }
//        try {
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Trg(tr.f(), tr.s(), tr.t()));
//            FuzzyLiner copy = base.copy();
//            copy.update(rels);
//            recur(copy, onlyDist, sink);
//        } catch (ContradictionException e) {
//            // ok
//        }
//        try {
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Col(tr.f(), tr.s(), tr.t()));
//            FuzzyLiner copy = base.copy();
//            copy.update(rels);
//            recur(copy, onlyDist, sink);
//        } catch (ContradictionException e) {
//            // ok
//        }
    }

    public static FuzzyLiner singleByContradiction(FuzzyLiner ln, boolean onlyDist, UnaryOperator<FuzzyLiner> op) {
        List<Pair> pairs = ln.undefinedPairs(); // TODO
//        Queue<Rel> q = new ConcurrentLinkedDeque<>();
//        pairs.stream().parallel().forEach(p -> {
//            Boolean dist = identifyDistinction(ln, p, op);
//            if (dist == null) {
//                return;
//            }
//            if (dist) {
//                q.add(new Dist(p.f(), p.s()));
//            } else {
//                q.add(new Same(p.f(), p.s()));
//            }
//        });
//        ln.update(q);
        FuzzyLiner afterDist = op.apply(ln);
//        if (onlyDist) {
//            return afterDist;
//        }
//        List<Triple> triples = afterDist.undefinedTriples();
//        Queue<Rel> q1 = new ConcurrentLinkedDeque<>();
//        triples.stream().parallel().forEach(tr -> {
//            Boolean coll = identifyCollinearity(afterDist, tr, op);
//            if (coll == null) {
//                return;
//            }
//            if (coll) {
//                q1.add(new Col(tr.f(), tr.s(), tr.t()));
//            } else {
//                q1.add(new Trg(tr.f(), tr.s(), tr.t()));
//            }
//        });
//        afterDist.update(q1);
        return op.apply(afterDist);
    }

    public static Boolean identifyDistinction(FuzzyLiner l, Pair p, UnaryOperator<FuzzyLiner> op) {
        Boolean result = null; // TODO
//        try {
//            FuzzyLiner copy = l.copy();
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Same(p.f(), p.s()));
//            copy.update(rels);
//            op.apply(copy);
//        } catch (ContradictionException e) {
//            result = true;
//        }
//        try {
//            FuzzyLiner copy = l.copy();
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Dist(p.f(), p.s()));
//            copy.update(rels);
//            op.apply(copy);
//        } catch (ContradictionException e) {
//            if (result != null) {
//                throw new IllegalArgumentException("Total impossibility");
//            }
//            result = false;
//        }
        return result;
    }

    public static Boolean identifyCollinearity(FuzzyLiner l, Triple t, UnaryOperator<FuzzyLiner> op) {
        Boolean result = null; // TODO
//        try {
//            FuzzyLiner copy = l.copy();
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Col(t.f(), t.s(), t.t()));
//            copy.update(rels);
//            op.apply(copy);
//        } catch (ContradictionException e) {
//            result = false;
//        }
//        try {
//            FuzzyLiner copy = l.copy();
//            Queue<Rel> rels = new ArrayDeque<>();
//            rels.add(new Trg(t.f(), t.s(), t.t()));
//            copy.update(rels);
//            op.apply(copy);
//        } catch (ContradictionException e) {
//            if (result != null) {
//                throw new IllegalArgumentException("Total impossibility");
//            }
//            result = true;
//        }
        return result;
    }

    public static FuzzyLiner process(FuzzyLiner liner, List<Function<FuzzyLiner, List<Update>>> processors) {
        while (true) {
            Queue<Update> queue = processors.stream().parallel().flatMap(p -> p.apply(liner).stream())
                    .collect(Collectors.toCollection(ArrayDeque::new));
            if (queue.isEmpty()) {
                return liner;
            }
            liner.update(queue);
            // TODO liner = liner.quotient();
        }
    }

    public static List<Update> processPAlt(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
        for (int a = 0; a < pc; a++) {
            for (int c = a + 1; c < pc; c++) {
                for (int ab1a1b = 0; ab1a1b < pc; ab1a1b++) {
                    if (!liner.triangle(a, c, ab1a1b)) {
                        continue;
                    }
                    for (int bc1b1c = 0; bc1b1c < pc; bc1b1c++) {
                        if (!liner.triangle(a, c, bc1b1c) || !liner.triangle(a, ab1a1b, bc1b1c) || !liner.triangle(c, ab1a1b, bc1b1c)) {
                            continue;
                        }
                        for (int a1 = 0; a1 < pc; a1++) {
                            if (!liner.triangle(a, c, a1) || !liner.triangle(a1, ab1a1b, bc1b1c) || !liner.triangle(a1, ab1a1b, a)
                                    || !liner.triangle(a1, bc1b1c, c) || !liner.triangle(c, ab1a1b, a1)) {
                                continue;
                            }
                            int b = -1;
                            int b1 = -1;
                            int c1 = -1;
                            int ac1a1c = -1;
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a1, ab1a1b, i) && liner.collinear(a, c, i)) {
                                    if (b < 0) {
                                        b = i;
                                    }
                                }
                                if (liner.collinear(a, ab1a1b, i) && liner.collinear(c, bc1b1c, i)) {
                                    if (b1 < 0) {
                                        b1 = i;
                                    }
                                }
                            }
                            if (b1 < 0 || b < 0 || !liner.triangle(a1, b1, a) || !liner.triangle(a1, b1, b) || !liner.triangle(a1, b1, c) || !liner.triangle(a, c, b1)) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a1, b1, i) && liner.collinear(b, bc1b1c, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
                                    }
                                }
                            }
                            if (c1 < 0 || !liner.triangle(a, b, c1)) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a1, c, i) && liner.collinear(a, c1, i)) {
                                    if (ac1a1c < 0) {
                                        ac1a1c = i;
                                    }
                                }
                            }
                            if (ac1a1c < 0) {
                                continue;
                            }
                            if (!liner.collinear(bc1b1c, ac1a1c, ab1a1b)) {
                                res.add(new Update(new Col(bc1b1c, ac1a1c, ab1a1b), "P", new Col(a, b, c), new Col(a1, b1, c1),
                                        new Trg(a, b, a1), new Trg(a, b, b1), new Trg(a, b, c1), new Trg(a1, b1, a), new Trg(a1, b1, b), new Trg(a1, b1, c),
                                        new Col(a, b1, ab1a1b), new Col(a1, b, ab1a1b), new Col(a, c1, ac1a1c), new Col(a1, c, ac1a1c),
                                        new Col(b, c1, bc1b1c), new Col(b1, c, bc1b1c)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processP(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                        if (!liner.triangle(a, b, a1)) {
                            continue;
                        }
                        for (int b1 = 0; b1 < pc; b1++) {
                            if (!liner.triangle(a, b, b1) || !liner.triangle(a1, b1, a) || !liner.triangle(a1, b1, b) || !liner.triangle(a1, b1, c)) {
                                continue;
                            }
                            for (int c1 = 0; c1 < pc; c1++) {
                                if (!liner.collinear(a1, b1, c1) || !liner.triangle(a, b, c1)) {
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
                                    }
                                    if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                        if (bc1b1c < 0) {
                                            bc1b1c = i;
                                        }
                                    }
                                    if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                        if (ac1a1c < 0) {
                                            ac1a1c = i;
                                        }
                                    }
                                }
                                if (ab1a1b < 0 || ac1a1c < 0 || bc1b1c < 0) {
                                    continue;
                                }
                                if (!liner.collinear(bc1b1c, ac1a1c, ab1a1b)) {
                                    res.add(new Update(new Col(bc1b1c, ac1a1c, ab1a1b), "P", new Col(a, b, c), new Col(a1, b1, c1),
                                            new Trg(a, b, a1), new Trg(a, b, b1), new Trg(a, b, c1), new Trg(a1, b1, a), new Trg(a1, b1, b), new Trg(a1, b1, c),
                                            new Col(a, b1, ab1a1b), new Col(a1, b, ab1a1b), new Col(a, c1, ac1a1c), new Col(a1, c, ac1a1c),
                                            new Col(b, c1, bc1b1c), new Col(b1, c, bc1b1c)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processP1(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                        if (!liner.triangle(a, b, a1)) {
                            continue;
                        }
                        for (int b1 = 0; b1 < pc; b1++) {
                            if (!liner.triangle(a, a1, b1) || !liner.triangle(b, a1, b1) || !liner.triangle(c, a1, b1) || !liner.triangle(a, b, b1)) {
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
                            if (c1 < 0 || !liner.triangle(a, b, c1)) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(b, c1, i) && liner.collinear(b1, c, i)) {
                                    bc1b1c = i;
                                }
                                if (liner.collinear(a, c1, i) && liner.collinear(a1, c, i)) {
                                    ac1a1c = i;
                                }
                            }
                            if (ac1a1c < 0 || bc1b1c < 0) {
                                continue;
                            }
                            if (!liner.collinear(ac1a1c, ab1a1b, bc1b1c)) {
                                res.add(new Update(new Col(ac1a1c, ab1a1b, bc1b1c), "P1", new Col(a, b, c), new Col(a1, b1, c1),
                                        new Trg(a, b, a1), new Trg(a, b, b1), new Trg(a, b, c1), new Trg(a1, b1, a), new Trg(a1, b1, b), new Trg(a1, b1, c),
                                        new Col(a, b1, ab1a1b), new Col(a, b1, ab1a1b), new Col(c, c1, ab1a1b),
                                        new Col(a, c1, ac1a1c), new Col(a1, c, ac1a1c), new Col(b, c1, bc1b1c), new Col(b1, c, bc1b1c)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processPS(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                            if (!liner.collinear(ab1a1b, ac1a1c, bc1b1c)) {
                                res.add(new Update(new Col(ab1a1b, ac1a1c, bc1b1c), "PS", new Trg(o, a, a1), new Col(a, b, c), new Col(a1, b1, c1),
                                        new Col(a, b1, ab1a1b), new Col(a1, b, ab1a1b), new Col(a, c1, ac1a1c), new Col(a1, c, ac1a1c),
                                        new Col(b, c1, bc1b1c), new Col(b1, c, bc1b1c), new Col(o, ab1a1b, ac1a1c)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processP1S(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                        if (!liner.collinear(ab1a1b, ac1a1c, bc1b1c)) {
                            res.add(new Update(new Col(ab1a1b, ac1a1c, bc1b1c), "P1S", new Trg(o, a, a1), new Col(o, a, b), new Col(a, b, c),
                                    new Col(o, a1, b1), new Col(a1, b1, c1), new Col(a, b1, ab1a1b), new Col(a1, b, ab1a1b),
                                    new Col(a, c1, ac1a1c), new Col(a1, c, ac1a1c), new Col(b, c1, bc1b1c), new Col(b1, c, bc1b1c),
                                    new Col(o, ab1a1b, ac1a1c)));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processP2S(FuzzyLiner liner) {
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
                        if (bc1b1c < 0 || !liner.collinear(a, a1, bc1b1c)) {
                            continue;
                        }
                        if (!liner.collinear(ab1a1b, ac1a1c, bc1b1c)) {
                            res.add(new Col(ab1a1b, ac1a1c, bc1b1c));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Rel> processP3S(FuzzyLiner liner) {
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

    public static List<Update> processD3(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                if (bcb1c1 >= 0 && aca1c1 >= 0 && !liner.collinear(bcb1c1, aca1c1, aba1b1)) {
                                    res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D3", new Trg(o, a, b), new Trg(o, b, c), new Trg(o, a, c),
                                            new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                            new Col(a, b, c1), new Col(a, c, b1), new Col(b, c, a1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                            new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
                                }
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                if (aca1c1 < 0) {
                                    aca1c1 = i;
                                }
                                if (aba1b1 >= 0 && bcb1c1 >= 0 && !liner.collinear(aba1b1, bcb1c1, aca1c1)) {
                                    res.add(new Update(new Col(aba1b1, bcb1c1, aca1c1), "D3", new Trg(o, a, b), new Trg(o, b, c), new Trg(o, a, c),
                                            new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                            new Col(a, b, c1), new Col(a, c, b1), new Col(b, c, a1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                            new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
                                }
                            }
                            if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                if (bcb1c1 < 0) {
                                    bcb1c1 = i;
                                }
                                if (aba1b1 >= 0 && aca1c1 >= 0 && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                    res.add(new Update(new Col(aba1b1, aca1c1, bcb1c1), "D3", new Trg(o, a, b), new Trg(o, b, c), new Trg(o, a, c),
                                            new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                            new Col(a, b, c1), new Col(a, c, b1), new Col(b, c, a1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                            new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
                                }
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processD2(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                    }
                                }
                                if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
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
                                    }
                                    if (bcb1c1 >= 0 && aca1c1 >= 0 && !liner.collinear(bcb1c1, aca1c1, aba1b1)) {
                                        res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D2", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a, c, b1), new Col(o, a, a1), new Col(o, b, b1),
                                                new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                    if (aca1c1 < 0) {
                                        aca1c1 = i;
                                    }
                                    if (aba1b1 >= 0 && bcb1c1 >= 0 && !liner.collinear(aba1b1, bcb1c1, aca1c1)) {
                                        res.add(new Update(new Col(aba1b1, bcb1c1, aca1c1), "D2", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a, c, b1), new Col(o, a, a1), new Col(o, b, b1),
                                                new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
                                    }
                                }
                                if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                    if (bcb1c1 < 0) {
                                        bcb1c1 = i;
                                    }
                                    if (aba1b1 >= 0 && aca1c1 >= 0 && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                        res.add(new Update(new Col(aba1b1, aca1c1, bcb1c1), "D2", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a, c, b1), new Col(o, a, a1), new Col(o, b, b1),
                                                new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1)));
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

    public static List<Update> processD2S(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                a1 = i;
                            }
                            if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                c1 = i;
                            }
                        }
                        if (a1 < 0 || c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                aca1c1 = i;
                            }
                        }
                        if (aca1c1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(o, aca1c1, i) && liner.collinear(a, b, i)) {
                                aba1b1 = i;
                            }
                        }
                        if (aba1b1 < 0) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(a1, aba1b1, i) && liner.collinear(o, b, i)) {
                                b1 = i;
                            }
                        }
                        if (b1 < 0 || !liner.triangle(a1, b1, c1)) {
                            continue;
                        }
                        for (int i = 0; i < pc; i++) {
                            if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                bcb1c1 = i;
                            }
                        }
                        if (bcb1c1 < 0) {
                            continue;
                        }
                        if (!liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                            res.add(new Update(new Col(aba1b1, aca1c1, bcb1c1), "D2S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                    new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                    new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                    new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1),
                                    new Col(b, c, a1), new Col(a, b, c1), new Col(o, aba1b1, aca1c1)));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processD1S(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                    if (!liner.collinear(o, c, c1) || !liner.triangle(a1, b1, c1)) {
                                        continue;
                                    }
                                    int aba1b1 = -1;
                                    int bcb1c1 = -1;
                                    int aca1c1 = -1;
                                    for (int i = 0; i < liner.getPc(); i++) {
                                        if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                            if (aba1b1 < 0) {
                                                aba1b1 = i;
                                            }
                                        }
                                        if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                            if (aca1c1 < 0) {
                                                aca1c1 = i;
                                            }
                                        }
                                        if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                            if (bcb1c1 < 0) {
                                                bcb1c1 = i;
                                            }
                                        }
                                    }
                                    if (aba1b1 < 0 || aca1c1 < 0 || bcb1c1 < 0) {
                                        continue;
                                    }
                                    if (liner.collinear(o, bcb1c1, aca1c1) && !liner.collinear(bcb1c1, aca1c1, aba1b1)) {
                                        res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D1S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, bcb1c1, aca1c1)));
                                    }
                                    if (liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, bcb1c1, aca1c1)) {
                                        res.add(new Update(new Col(aba1b1, bcb1c1, aca1c1), "D1S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, aba1b1, bcb1c1)));
                                    }
                                    if (liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                        res.add(new Update(new Col(aba1b1, aca1c1, bcb1c1), "D1S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                                new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, aba1b1, aca1c1)));
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

    public static List<Update> processD3S(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                    if (!liner.collinear(o, c, c1) || !liner.collinear(a, b, c1) || !liner.triangle(a1, b1, c1)) {
                                        continue;
                                    }
                                    int aba1b1 = -1;
                                    int bcb1c1 = -1;
                                    int aca1c1 = -1;
                                    for (int i = 0; i < liner.getPc(); i++) {
                                        if (liner.collinear(a, b, i) && liner.collinear(a1, b1, i)) {
                                            if (aba1b1 < 0) {
                                                aba1b1 = i;
                                            }
                                        }
                                        if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                            if (aca1c1 < 0) {
                                                aca1c1 = i;
                                            }
                                        }
                                        if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                            if (bcb1c1 < 0) {
                                                bcb1c1 = i;
                                            }
                                        }
                                    }
                                    if (aba1b1 < 0 || aca1c1 < 0 || bcb1c1 < 0) {
                                        continue;
                                    }
                                    if (liner.collinear(o, bcb1c1, aca1c1) && !liner.collinear(bcb1c1, aca1c1, aba1b1)) {
                                        res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D3S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(b, c, a1), new Col(a, c, b1), new Col(a, b, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                                new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1),
                                                new Col(o, bcb1c1, aca1c1)));
                                    }
                                    if (liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, bcb1c1, aca1c1)) {
                                        res.add(new Update(new Col(aba1b1, bcb1c1, aca1c1), "D3S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(b, c, a1), new Col(a, c, b1), new Col(a, b, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                                new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1),
                                                new Col(o, aba1b1, bcb1c1)));
                                    }
                                    if (liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                        res.add(new Update(new Col(aba1b1, aca1c1, bcb1c1), "D3S", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                                new Trg(a, b, c), new Trg(a1, b1, c1), new Col(o, a, a1), new Col(o, b, b1), new Col(o, c, c1),
                                                new Col(b, c, a1), new Col(a, c, b1), new Col(a, b, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1),
                                                new Col(a, c, aca1c1), new Col(a1, c1, aca1c1), new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1),
                                                new Col(o, aba1b1, aca1c1)));
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

    public static List<Update> processFullFano(FuzzyLiner liner) {
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                }
                            }
                            if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                if (acbd < 0) {
                                    acbd = i;
                                }
                            }
                            if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                if (adbc < 0) {
                                    adbc = i;
                                }
                            }
                        }
                        if (abcd < 0 || acbd < 0 || adbc < 0) {
                            continue;
                        }
                        if (!liner.collinear(abcd, acbd, adbc)) {
                            res.add(new Update(new Col(abcd, acbd, adbc), "F", new Trg(a, b, c), new Trg(a, c, d), new Trg(a, b, d), new Trg(b, c, d),
                                    new Col(a, b, abcd), new Col(c, d, abcd), new Col(a, c, acbd), new Col(b, d, acbd), new Col(a, d, adbc), new Col(b, c, adbc)));
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processInversivePlus(FuzzyLiner liner) {
        List<Update> res = new ArrayList<>(liner.getPc());
        for (int a = 0; a < liner.getPc(); a++) {
            for (int b = 0; b < liner.getPc(); b++) {
                if (!liner.distinct(a, b)) {
                    continue;
                }
                for (int c = 0; c < liner.getPc(); c++) {
                    if (!liner.triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = 0; d < liner.getPc(); d++) {
                        if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(b, c, d)) {
                            continue;
                        }
                        for (int a1 = 0; a1 < liner.getPc(); a1++) {
                            if (!liner.collinear(a, d, a1)) {
                                continue;
                            }
                            int h = -1;
                            int v = -1;
                            int o = -1;
                            int b1 = -1;
                            int c1 = -1;
                            int d1 = -1;
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                    if (h < 0) {
                                        h = i;
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                    if (o < 0) {
                                        o = i;
                                    }
                                }
                            }
                            if (h < 0 || o < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(h, o, i) && liner.collinear(a, d, i)) {
                                    if (v < 0) {
                                        v = i;
                                    }
                                }
                            }
                            if (v < 0 || !liner.distinct(a1, v)) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(o, a1, i) && liner.collinear(v, c, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
                                    }
                                }
                            }
                            if (c1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(v, a1, i) && liner.collinear(h, c1, i)) {
                                    if (d1 < 0) {
                                        d1 = i;
                                    }
                                }
                            }
                            if (d1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(h, a1, i) && liner.collinear(o, d1, i)) {
                                    if (b1 < 0) {
                                        b1 = i;
                                    }
                                }
                            }
                            if (b1 < 0) {
                                continue;
                            }
                            if (!liner.collinear(v, b, b1)) {
                                res.add(new Update(new Col(v, b, b1), "InvPl", new Trg(a, b, c), new Trg(b, c, d), new Trg(a, b, d), new Trg(a, c, d),
                                        new Col(a, d, a1), new Col(a, c, o), new Col(b, d, o), new Col(a, b, h), new Col(c, d, h),
                                        new Col(a, d, v), new Col(o, h, v), new Dist(a1, v), new Col(o, a1, c1), new Col(v, c, c1),
                                        new Col(v, a1, d1), new Col(h, c1, d1), new Col(h, a1, b1), new Col(o, d1, b1)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processAssocPlus(FuzzyLiner liner) {
        List<Update> res = new ArrayList<>(liner.getPc());
        for (int a = 0; a < liner.getPc(); a++) {
            for (int b = 0; b < liner.getPc(); b++) {
                if (!liner.distinct(a, b)) {
                    continue;
                }
                for (int c = 0; c < liner.getPc(); c++) {
                    if (!liner.triangle(a, b, c)) {
                        continue;
                    }
                    for (int d = 0; d < liner.getPc(); d++) {
                        if (!liner.triangle(a, b, d) || !liner.triangle(a, c, d) || !liner.triangle(b, c, d)) {
                            continue;
                        }
                        for (int a1 = 0; a1 < liner.getPc(); a1++) {
                            if (!liner.triangle(a, c, a1) || !liner.triangle(a, b, a1)) {
                                continue;
                            }
                            int h = -1;
                            int v = -1;
                            int o = -1;
                            int b1 = -1;
                            int c1 = -1;
                            int d1 = -1;
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(a, b, i) && liner.collinear(c, d, i)) {
                                    if (h < 0) {
                                        h = i;
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                    if (v < 0) {
                                        v = i;
                                    }
                                }
                            }
                            if (h < 0 || v < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(h, v, i) && liner.collinear(a, a1, i)) {
                                    if (o < 0) {
                                        o = i;
                                    }
                                }
                            }
                            if (o < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(v, a1, i) && liner.collinear(o, c, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
                                    }
                                }
                            }
                            if (c1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(o, d, i) && liner.collinear(h, c1, i)) {
                                    if (d1 < 0) {
                                        d1 = i;
                                    }
                                }
                            }
                            if (d1 < 0) {
                                continue;
                            }
                            for (int i = 0; i < liner.getPc(); i++) {
                                if (liner.collinear(h, a1, i) && liner.collinear(v, d1, i)) {
                                    if (b1 < 0) {
                                        b1 = i;
                                    }
                                }
                            }
                            if (b1 < 0) {
                                continue;
                            }
                            if (!liner.collinear(o, b, b1)) {
                                res.add(new Update(new Col(o, b, b1), "AssocPl", new Trg(a, b, c), new Trg(b, c, d), new Trg(a, b, d), new Trg(a, c, d),
                                        new Trg(a, b, a1), new Trg(a, c, a1), new Col(a, c, v), new Col(b, d, v), new Col(a, b, h), new Col(c, d, h),
                                        new Col(o, h, v), new Col(a, a1, o), new Col(v, a1, c1), new Col(o, c, c1),
                                        new Col(o, d, d1), new Col(h, c1, d1), new Col(h, a1, b1), new Col(v, b1, d1)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    public static List<Update> processD11S(FuzzyLiner liner) {
        int pc = liner.getPc();
        List<Update> res = new ArrayList<>(liner.getPc());
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
                                if (liner.collinear(a, b, i) && liner.collinear(a1, c, i)) {
                                    if (aba1b1 < 0) {
                                        aba1b1 = i;
                                    }
                                }
                                if (liner.collinear(o, c, i) && liner.collinear(a, b, i)) {
                                    if (c1 < 0) {
                                        c1 = i;
                                    }
                                }
                                if (liner.collinear(o, b, i) && liner.collinear(a1, c, i)) {
                                    if (b1 < 0) {
                                        b1 = i;
                                    }
                                }
                            }
                            if (aba1b1 < 0 || c1 < 0 || b1 < 0 || !liner.triangle(a1, b1, c1)) {
                                continue;
                            }
                            for (int i = 0; i < pc; i++) {
                                if (liner.collinear(a, c, i) && liner.collinear(a1, c1, i)) {
                                    if (aca1c1 < 0) {
                                        aca1c1 = i;
                                    }
                                }
                                if (liner.collinear(b, c, i) && liner.collinear(b1, c1, i)) {
                                    if (bcb1c1 < 0) {
                                        bcb1c1 = i;
                                    }
                                }
                            }
                            if (aca1c1 < 0 || bcb1c1 < 0) {
                                continue;
                            }
                            if (liner.collinear(o, aba1b1, aca1c1) && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D11", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                        new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a1, b1, c), new Col(o, a, a1), new Col(o, b, b1),
                                        new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                        new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, aba1b1, aca1c1)));
                            }
                            if (liner.collinear(o, aba1b1, bcb1c1) && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D11", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                        new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a1, b1, c), new Col(o, a, a1), new Col(o, b, b1),
                                        new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                        new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, aba1b1, bcb1c1)));
                            }
                            if (liner.collinear(o, aca1c1, bcb1c1) && !liner.collinear(aba1b1, aca1c1, bcb1c1)) {
                                res.add(new Update(new Col(bcb1c1, aca1c1, aba1b1), "D11", new Trg(o, a, b), new Trg(o, a, c), new Trg(o, b, c),
                                        new Trg(a, b, c), new Trg(a1, b1, c1), new Col(a, b, c1), new Col(a1, b1, c), new Col(o, a, a1), new Col(o, b, b1),
                                        new Col(o, c, c1), new Col(a, b, aba1b1), new Col(a1, b1, aba1b1), new Col(a, c, aca1c1), new Col(a1, c1, aca1c1),
                                        new Col(b, c, bcb1c1), new Col(b1, c1, bcb1c1), new Col(o, aca1c1, bcb1c1)));
                            }
                        }
                    }
                }
            }
        }
        return res;
    }
}
