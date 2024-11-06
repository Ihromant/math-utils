package ua.ihromant.mathutils.fuzzy;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class FuzzySLinerTest {
    @Test
    public void testMoufang() {
        int[][] antiMoufang = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner firstBase = FuzzySLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        int pc = firstBase.getPc();
        Set<FixBS> lines = Arrays.stream(antiMoufang).map(l -> FixBS.of(pc, l)).collect(Collectors.toSet());
        firstBase.printChars();
        //firstBase = distByContradiction(firstBase);
        //System.out.println(multipleByContradiction(firstBase).size());
        firstBase.printChars();
        FuzzySLiner firstClosed = firstBase.intersectLines();
        firstClosed.printChars();
        firstClosed = enhanceFullFano(firstClosed);
        firstClosed.printChars();
        firstBase = firstClosed.subLiner(firstBase.getPc());
        firstBase.printChars();
        firstClosed.update(moufangQueue(firstClosed, pc, lines));
        System.out.println("Enhanced Antimoufang");
        firstClosed = enhanceFullFano(firstClosed);
        firstClosed.printChars();
        System.out.println("Antimoufang " + findAntiMoufangQuick(firstClosed, firstClosed.determinedSet()).size());
        FixBS det = firstClosed.determinedSet();
        //System.out.println(multipleByContradiction(firstClosed.subLiner(det)).size());
        det.clear(26);
        det.clear(27);
        det.clear(45);
        det.clear(49);
        det.clear(54); // comment and clear(43) and clear(95) to have 13-point config
        det.clear(120);
        det.clear(144);
        System.out.println("Second step......................................");
        System.out.println(det);
        FuzzySLiner secondBase = firstClosed.subLiner(det);
        secondBase.printChars();
        System.out.println("Antimoufang " + findAntiMoufangQuick(secondBase, secondBase.determinedSet()).size());
        FuzzySLiner secondClosed = secondBase.intersectLines();
        secondClosed.printChars();
        secondClosed = enhanceFullFano(secondClosed);
        secondClosed.printChars();
        secondClosed.update(moufangQueue(secondClosed, pc, lines));
        System.out.println("Enhanced Antimoufang");
        // secondClosed = enhanceFullFano(secondClosed);
        // configs = findAntiMoufangQuick(secondClosed, secondClosed.determinedSet());
        secondClosed.printChars();
        // System.out.println("Antimoufang " + configs.size());
        det = secondClosed.determinedSet();
        System.out.println("Third step......................................");
        System.out.println(det);
        FuzzySLiner thirdBase = secondClosed.subLiner(det);
        thirdBase.printChars();
        System.out.println("Antimoufang " + findAntiMoufangQuick(thirdBase, thirdBase.determinedSet()).size());
        FuzzySLiner thirdClosed = thirdBase.intersectLines();
        thirdClosed.printChars();
        thirdClosed = enhanceFullFano(thirdClosed);
        thirdClosed.printChars();
        thirdClosed.update(moufangQueue(thirdClosed, pc, lines));
        System.out.println("Enhanced Antimoufang");
        thirdClosed = enhanceFullFano(thirdClosed);
        thirdClosed.printChars();
        det = thirdClosed.determinedSet();
        FuzzySLiner finished = thirdClosed.subLiner(det);
        System.out.println("Closed " + det + " " + finished.undefinedPairs() + " " + finished.undefinedTriples());
        System.out.println(finished.lines());
        List<int[]> am = findAntiMoufangQuick(finished, finished.determinedSet());
        am.forEach(l -> System.out.println(Arrays.toString(l)));
        //System.out.println("Antimoufang " + findAntiMoufangQuick(thirdClosed, det).size());
    }

    private FuzzySLiner singleByContradiction(FuzzySLiner ln, boolean onlyDist) {
        ln.printChars();
        List<Pair> pairs = ln.undefinedPairs();
        Queue<Rel> q = new ArrayDeque<>();
        for (Pair p : pairs) {
            Boolean dist = identifyDistinction(ln, p);
            if (dist == null) {
                continue;
            }
            if (dist) {
                q.add(new Dist(p.f(), p.s()));
            } else {
                q.add(new Same(p.f(), p.s()));
            }
        }
        ln.update(q);
        //ln.printChars();
        ln = enhanceFullFano(ln);
        //ln.printChars();
        if (onlyDist) {
            return ln;
        }
        FuzzySLiner lnr = ln;
        List<Triple> triples = ln.undefinedTriples().stream()
                .filter(t -> lnr.distinct(t.f(), t.s()) && lnr.distinct(t.f(), t.t()) && lnr.distinct(t.s(), t.t())).toList();
        q = new ArrayDeque<>();
        for (Triple tr : triples) {
            Boolean coll = identifyCollinearity(ln, tr);
            if (coll == null) {
                continue;
            }
            if (coll) {
                q.add(new Col(tr.f(), tr.s(), tr.t()));
            } else {
                q.add(new Trg(tr.f(), tr.s(), tr.t()));
            }
        }
        ln.update(q);
        //ln.printChars();
        ln = enhanceFullFano(ln);
        //ln.printChars();
        return ln;
    }

    private static Queue<Rel> moufangQueue(FuzzySLiner closed, int pc, Set<FixBS> lines) {
        List<int[]> configs = findAntiMoufangQuick(closed, closed.determinedSet());
        System.out.println("Antimoufang " + configs.size());
        Queue<Rel> queue = new ArrayDeque<>(closed.getPc());
        for (int i = 0; i < pc; i++) {
            for (int j = i + 1; j < pc; j++) {
                for (int k = j + 1; k < pc; k++) {
                    FixBS bs = FixBS.of(pc, i, j, k);
                    boolean line = lines.contains(bs);
                    for (int[] config : configs) {
                        if (line) {
                            queue.add(new Col(config[i], config[j], config[k]));
                        } else {
                            queue.add(new Trg(config[i], config[j], config[k]));
                        }
                    }
                }
            }
        }
        return queue;
    }

    public FuzzySLiner enhanceFullFano(FuzzySLiner liner) {
        //int cnt = 0;
        while (true) {
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
                                    if (abcd < 0) {
                                        abcd = i;
                                    } else {
                                        queue.add(new Same(abcd, i));
                                    }
                                    if (acbd >= 0 && adbc >= 0 && !liner.collinear(acbd, adbc, i)) {
                                        queue.add(new Col(acbd, adbc, i));
                                    }
                                }
                                if (liner.collinear(a, c, i) && liner.collinear(b, d, i)) {
                                    if (acbd < 0) {
                                        acbd = i;
                                    } else {
                                        queue.add(new Same(acbd, i));
                                    }
                                    if (abcd >= 0 && adbc >= 0 && !liner.collinear(abcd, adbc, i)) {
                                        queue.add(new Col(abcd, adbc, i));
                                    }
                                }
                                if (liner.collinear(a, d, i) && liner.collinear(b, c, i)) {
                                    if (adbc < 0) {
                                        adbc = i;
                                    } else {
                                        queue.add(new Same(adbc, i));
                                    }
                                    if (abcd >= 0 && acbd >= 0 && !liner.collinear(abcd, acbd, i)) {
                                        queue.add(new Col(abcd, acbd, i));
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
                        Queue<Rel> queue = new ArrayDeque<>(liner.getPc());
                        if (abcd >= 0 && acbd >= 0 && adbc < 0) {
                            queue.add(new Col(a, d, liner.getPc()));
                            queue.add(new Col(b, c, liner.getPc()));
                            queue.add(new Col(abcd, acbd, liner.getPc()));
                        }
                        if (abcd >= 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, liner.getPc()));
                            queue.add(new Col(b, d, liner.getPc()));
                            queue.add(new Col(abcd, adbc, liner.getPc()));
                        }
                        if (abcd < 0 && acbd >= 0 && adbc >= 0) {
                            queue.add(new Col(a, b, liner.getPc()));
                            queue.add(new Col(c, d, liner.getPc()));
                            queue.add(new Col(acbd, adbc, liner.getPc()));
                        }
                        if (!queue.isEmpty()) {
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
                            queue.add(new Col(abcd, acbd, pt));
                            pt++;
                        }
                        if (abcd >= 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            queue.add(new Col(abcd, adbc, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd >= 0 && adbc >= 0) {
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            queue.add(new Col(acbd, adbc, pt));
                            pt++;
                        }
                        if (abcd >= 0 && acbd < 0 && adbc < 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            queue.add(new Col(abcd, pt, pt + 1));
                            pt++;
                            queue.add(new Col(a, d, pt));
                            queue.add(new Col(b, c, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd >= 0 && adbc < 0) {
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            queue.add(new Col(acbd, pt, pt + 1));
                            pt++;
                            queue.add(new Col(a, d, pt));
                            queue.add(new Col(b, c, pt));
                            pt++;
                        }
                        if (abcd < 0 && acbd < 0 && adbc >= 0) {
                            queue.add(new Col(a, c, pt));
                            queue.add(new Col(b, d, pt));
                            queue.add(new Col(adbc, pt, pt + 1));
                            pt++;
                            queue.add(new Col(a, b, pt));
                            queue.add(new Col(c, d, pt));
                            pt++;
                        }
                    }
                }
            }
        }
        liner = liner.addPoints(pt - liner.getPc());
        liner.update(queue);
        System.out.println("After closure " + liner.getPc());
        return liner;
    }

    private Boolean identifyCollinearity(FuzzySLiner l, Triple t) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Col(t.f(), t.s(), t.t()));
            copy.update(rels);
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            result = false;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Trg(t.f(), t.s(), t.t()));
            copy.update(rels);
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = true;
        }
        return result;
    }

    private Boolean identifyDistinction(FuzzySLiner l, Pair p) {
        Boolean result = null;
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Same(p.f(), p.s()));
            copy.update(rels);
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            result = true;
        }
        try {
            FuzzySLiner copy = l.copy();
            Queue<Rel> rels = new ArrayDeque<>();
            rels.add(new Dist(p.f(), p.s()));
            copy.update(rels);
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = false;
        }
        return result;
    }

    @Test
    public void testMoufang3() {
        int[][] antiMoufang = {
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9},
                {4, 6, 9}
        };
        FuzzySLiner first = FuzzySLiner.of(antiMoufang, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        int pc = first.getPc();
        Set<FixBS> lines = Arrays.stream(antiMoufang).map(l -> FixBS.of(pc, l)).collect(Collectors.toSet());
        first.printChars();
        FuzzySLiner second = intersect56(first);
        second.printChars();
        second = enhanceFullFano(second);
        second.printChars();
        second = singleByContradiction(second, false);
        second.printChars();
        FuzzySLiner next;
        while ((next = intersect6(second)) != null) {
//            if (next.getPc() % 5 == 0) {
//                next = singleByContradiction(next, false);
//                next = enhanceFullFano(next);
//                FixBS dt = next.determinedSet();
//                List<int[]> am = findAntiMoufangQuick(next, dt);
//                am.forEach(l -> System.out.println(Arrays.toString(l)));
//                System.out.println(next.determinedLines(dt).stream().filter(l -> l.cardinality() > 2).toList());
//            }
            second = enhanceFullFano(next);
            second.printChars();
        }
    }

    private List<FuzzySLiner> multipleByContradiction(FuzzySLiner base) {
        return recur(base).stream().<FuzzySLiner>mapMulti((l, sink) -> {
            try {
                sink.accept(enhanceFullFano(l));
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

    public List<int[]> findAntiMoufang(FuzzySLiner liner) {
        int pc = liner.getPc();
        List<int[]> result = new ArrayList<>();
        for (int o = 0; o < pc; o++) {
            System.out.println(o);
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
                        for (int b1 = 0; b1 < pc; b1++) {
                            if (!liner.collinear(o, b, b1)) {
                                continue;
                            }
                            for (int c = 0; c < pc; c++) {
                                if (!liner.triangle(o, a, c) || !liner.triangle(o, b, c)) {
                                    continue;
                                }
                                for (int c1 = 0; c1 < pc; c1++) {
                                    if (!liner.collinear(o, c, c1)) {
                                        continue;
                                    }
                                    for (int x = 0; x < pc; x++) {
                                        if (!liner.collinear(x, a, b) || !liner.collinear(x, a1, b1) || !liner.triangle(x, o, c)) {
                                            continue;
                                        }
                                        for (int y = 0; y < pc; y++) {
                                            if (!liner.collinear(y, a, c) || !liner.collinear(y, a1, c1) || !liner.collinear(o, x, y)) {
                                                continue;
                                            }
                                            for (int z = 0; z < pc; z++) {
                                                if (!liner.collinear(z, b, c) || !liner.collinear(z, b1, c1) || !liner.triangle(z, x, y)) {
                                                    continue;
                                                }
                                                result.add(new int[]{o, a, a1, b, b1, c, c1, x, y, z});
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
        return result;
    }

    public static List<int[]> findAntiMoufangQuick(FuzzySLiner liner, FixBS determined) {
        System.out.println("Looking Antimoufang for determined " + determined);
        List<int[]> result = new ArrayList<>();
        Liner l = new Liner(liner.getPc(), liner.determinedLines(determined).stream().filter(ln -> ln.cardinality() > 2).map(ln -> ln.stream().toArray()).toArray(int[][]::new));
        for (int o = 0; o < l.pointCount(); o++) {
            int[] lines = l.lines(o);
            if (lines.length < 4) {
                continue;
            }
            for (int l1 : lines) {
                for (int l2 : lines) {
                    if (l1 == l2) {
                        continue;
                    }
                    for (int l3 : lines) {
                        if (l1 == l3 || l2 == l3) {
                            continue;
                        }
                        for (int l4 : lines) {
                            if (l1 == l4 || l2 == l4 || l3 == l4) {
                                continue;
                            }
                            int[] line1 = l.line(l1);
                            int[] line2 = l.line(l2);
                            int[] line3 = l.line(l3);
                            for (int a : line1) {
                                if (o == a) {
                                    continue;
                                }
                                for (int a1 : line1) {
                                    if (o == a1 || a == a1) {
                                        continue;
                                    }
                                    for (int b : line2) {
                                        if (b == o || l.line(a, b) < 0 || !liner.triangle(o, a, b)) {
                                            continue;
                                        }
                                        for (int b1 : line2) {
                                            if (b1 == o || b1 == b || l.line(a1, b1) < 0) {
                                                continue;
                                            }
                                            for (int c : line3) {
                                                if (c == o || l.line(a, c) < 0 || l.line(b, c) < 0 || !liner.triangle(o, a, c) || !liner.triangle(o, b, c)) {
                                                    continue;
                                                }
                                                for (int c1 : line3) {
                                                    if (c1 == c || c1 == o || l.line(a1, c1) < 0 || l.line(b1, c1) < 0) {
                                                        continue;
                                                    }
                                                    int x = l.intersection(l.line(a, b), l.line(a1, b1));
                                                    int y = l.intersection(l.line(a, c), l.line(a1, c1));
                                                    int z = l.intersection(l.line(b, c), l.line(b1, c1));
                                                    if (x < 0 || y < 0 || z < 0) {
                                                        continue;
                                                    }
                                                    if (l.flag(l4, x) && l.flag(l4, y)
                                                            && (liner.triangle(o, x, z) || liner.triangle(o, y, z) || liner.triangle(x, y, z))) {
                                                        result.add(new int[]{o, a, a1, b, b1, c, c1, x, y, z});
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
            }
        }
        return result;
    }
}
