package ua.ihromant.mathutils.fuzzy;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

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
}
