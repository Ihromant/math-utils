package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.FuzzyLiner;
import ua.ihromant.mathutils.Pair;
import ua.ihromant.mathutils.Triple;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

public class FuzzyLinerTest {
    @Test
    public void testFanoNearMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
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
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        base = connectTwos(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
    }

    @Test
    public void testBaseFanoNotMoufangVariant1() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
                {0, 1, 2, 16, 17},
                {0, 3, 4, 12, 13},
                {0, 5, 6, 15, 14},
                {0, 7, 8, 10, 11},
                {1, 3, 7, 14},
                {1, 5, 8, 12},
                {2, 4, 7, 15},
                {2, 6, 8, 13},
                {3, 5, 9, 11, 16},
                {4, 6, 9, 10, 17}
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        singleByContradiction(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        List<FuzzyLiner> variants = multipleByContradiction(base);
        System.out.println(variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
    }

    @Test
    public void testBaseFanoNotMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
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
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        singleByContradiction(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        List<FuzzyLiner> variants = multipleByContradiction(base);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
//        for (int i = 0; i < variants.size(); i++) {
//            FuzzyLiner l = variants.get(i);
//            List<Quad> quads = l.quads(1);
//            for (int j = 0; j < quads.size(); j++) {
//                FuzzyLiner l1 = joinOne(l, quads.get(j));
//                List<Triple> undefined = l1.undefinedTriples();
//                System.out.println(i + " " + j + " " + undefined.size());
//            }
//        }
        List<FuzzyLiner> vs = new ArrayList<>(variants);
        variants = IntStream.range(0, variants.size()).boxed().flatMap(i -> {
            FuzzyLiner var = vs.get(i);
            System.out.println(var.lines());
            List<FuzzyLiner> expanded = expand(joinOne(var, new Quad(3, 5, 7, 8)));
            System.out.println("Variant " + i + " yields to " + expanded.size() + " " + new Quad(3, 5, 7, 8));
            return expanded.stream();
        }).toList();
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = expand(variants, 2);
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
    }

    @Test
    public void test9Case() {
        int[][] lines = new int[][]{{1, 5, 8, 14}, {2, 5, 11}, {3, 5, 9}, {0, 5, 6, 15, 19}, {4, 5, 12}, {4, 11, 21},
                {6, 14, 20}, {0, 1, 2, 9, 12, 13, 17, 18, 20, 21, 22, 23, 24, 25}, {9, 16, 19}, {8, 19, 23}, {11, 19, 25},
                {12, 14, 19}, {7, 16, 18}, {7, 14, 17}, {12, 15, 16}, {6, 7, 24}, {6, 10, 25}, {8, 15, 22}, {9, 14, 15},
                {10, 13, 14}, {11, 15, 24}, {0, 7, 8, 10, 11}, {1, 6, 11, 16}, {2, 6, 8}, {3, 11, 18}, {3, 8, 17}, {3, 6, 12},
                {1, 3, 7, 15}, {2, 3, 10}, {5, 16, 20}, {4, 6, 9}, {4, 8, 13}, {2, 4, 7}, {0, 3, 4, 14, 16}, {1, 4, 10, 19},
                {10, 16, 21}, {3, 19, 20}, {4, 15, 20}, {5, 7, 22}, {5, 10, 23}};
        FuzzyLiner base = new FuzzyLiner(lines, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        base.update();
        enhanceFullFano(base);
        singleByContradiction(base);
        List<FuzzyLiner> variants = List.of(base);
        List<Quad> quads = base.quads(2);
        System.out.println(quads);
        for (int i = 0; i < quads.size(); i++) {
            System.out.println(i + " " + expand(joinTwo(base, quads.get(i))).size());
        }
        variants = variants.stream().flatMap(var -> expand(joinTwo(var, var.quad(2))).stream()).toList();
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = variants.stream().parallel().flatMap(var -> expand(joinTwo(var, var.quad(2))).stream()).toList();
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = variants.stream().parallel().flatMap(var -> expand(joinTwo(var, var.quad(2))).stream()).toList();
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
        variants = variants.stream().parallel().flatMap(var -> expand(joinTwo(var, var.quad(2))).stream()).toList();
        System.out.println(variants.getFirst().getPc() + " " + variants.size());
    }

    @Test
    public void testFanoNotMoufang() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
                {0, 1, 2},
                {0, 3, 4},
                {0, 5, 6},
                {0, 7, 8, 10, 11},
                {1, 3, 7},
                {1, 5, 8},
                {2, 4, 7},
                {2, 6, 8},
                {3, 5, 9, 10},
                {4, 6, 9, 11}
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        base = connectTwos(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        base = connectOnes(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        Quad q;
        while ((q = base.quad(0)) != null) {
            int newPt = base.getPc();
            System.out.println("Adding three points " + newPt + " " + (newPt + 1) + " for quadruple " + q);
            base = base.addPoint().addPoint().addPoint();
            base.colline(q.a(), q.b(), newPt);
            base.colline(q.c(), q.d(), newPt);
            base.colline(q.a(), q.c(), newPt + 1);
            base.colline(q.b(), q.d(), newPt + 1);
            base.colline(q.a(), q.d(), newPt + 2);
            base.colline(q.b(), q.c(), newPt + 2);
            base.colline(newPt, newPt + 1, newPt + 2);
            base = connectTwos(base);
        }
    }

    @Test
    public void depthFirstSearch() {
        FuzzyLiner base = new FuzzyLiner(new int[][]{
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
        }, new Triple[]{new Triple(1, 3, 5), new Triple(2, 4, 6),
                new Triple(0, 1, 3), new Triple(0, 1, 5), new Triple(0, 3, 5),
                new Triple(0, 7, 9)});
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        enhanceFullFano(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        singleByContradiction(base);
        System.out.println(base.getD().size() + " " + base.getL().size() + " " + base.getT().size() + " " + (base.getL().size() + base.getT().size()));
        while (true) {
            System.out.println(base.getPc() + " " + base.lines().size() + " " + base.lines());
            base = depthByContradiction(base, base.undefinedTriples(), 0);
            List<Quad> twos = base.quads(2);
            Optional<Quad> min2 = twos.stream().min(Comparator.comparingInt(Quad::d));
            if (min2.isPresent()) {
                System.out.println("Joining two " + min2.get());
                base = joinTwoSimple(base, min2.get());
            } else {
                List<Quad> ones = base.quads(1);
                Quad min1 = ones.stream().min(Comparator.comparingInt(Quad::d)).orElseThrow();
                System.out.println("Joining one " + min1);
                base = joinOneSimple(base, min1);
            }
        }
    }

    private static FuzzyLiner joinOne(FuzzyLiner base, Quad q1) {
        try {
            FuzzyLiner result = joinOneSimple(base, q1);
            singleByContradiction(result);
            return result;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static FuzzyLiner joinOneSimple(FuzzyLiner base, Quad q1) {
        int newPt = base.getPc();
        FuzzyLiner result = base.addPoint().addPoint();
        Pair ab = new Pair(q1.a(), q1.b());
        Pair cd = new Pair(q1.c(), q1.d());
        int abcd = result.intersection(ab, cd);
        Pair ac = new Pair(q1.a(), q1.c());
        Pair bd = new Pair(q1.b(), q1.d());
        int acbd = result.intersection(ac, bd);
        Pair ad = new Pair(q1.a(), q1.d());
        Pair bc = new Pair(q1.b(), q1.c());
        int adbc = result.intersection(ad, bc);
        if (abcd >= 0) {
            result.colline(q1.a(), q1.c(), newPt);
            result.colline(q1.b(), q1.d(), newPt);
            result.colline(q1.a(), q1.d(), newPt + 1);
            result.colline(q1.b(), q1.c(), newPt + 1);
            result.colline(abcd, newPt, newPt + 1);
        } else {
            result.colline(q1.a(), q1.b(), newPt);
            result.colline(q1.c(), q1.d(), newPt);
            if (acbd >= 0) {
                result.colline(q1.a(), q1.d(), newPt + 1);
                result.colline(q1.b(), q1.c(), newPt + 1);
                result.colline(acbd, newPt, newPt + 1);
            } else {
                result.colline(q1.a(), q1.c(), newPt + 1);
                result.colline(q1.b(), q1.d(), newPt + 1);
                result.colline(adbc, newPt, newPt + 1);
            }
        }
        result.update();
        enhanceFullFano(result);
        return result;
    }

    private static FuzzyLiner joinTwo(FuzzyLiner base, Quad q2) {
        try {
            FuzzyLiner result = joinTwoSimple(base, q2);
            singleByContradiction(result);
            return result;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static FuzzyLiner joinTwoSimple(FuzzyLiner base, Quad q2) {
        int newPt = base.getPc();
        FuzzyLiner result = base.addPoint();
        Pair ab = new Pair(q2.a(), q2.b());
        Pair cd = new Pair(q2.c(), q2.d());
        int abcd = result.intersection(ab, cd);
        Pair ac = new Pair(q2.a(), q2.c());
        Pair bd = new Pair(q2.b(), q2.d());
        int acbd = result.intersection(ac, bd);
        Pair ad = new Pair(q2.a(), q2.d());
        Pair bc = new Pair(q2.b(), q2.c());
        int adbc = result.intersection(ad, bc);
        if (abcd < 0) {
            result.colline(q2.a(), q2.b(), newPt);
            result.colline(q2.c(), q2.d(), newPt);
            result.colline(acbd, adbc, newPt);
        } else {
            if (acbd < 0) {
                result.colline(q2.a(), q2.c(), newPt);
                result.colline(q2.b(), q2.d(), newPt);
                result.colline(abcd, adbc, newPt);
            } else {
                result.colline(q2.a(), q2.d(), newPt);
                result.colline(q2.b(), q2.c(), newPt);
                result.colline(abcd, acbd, newPt);
            }
        }
        result.update();
        enhanceFullFano(result);
        return result;
    }

    private static List<FuzzyLiner> expand(FuzzyLiner result) {
        if (result == null) {
            return List.of();
        }
        if (result.isFull()) {
            return List.of(result);
        }
        return multipleByContradiction(result);
    }

    private static List<FuzzyLiner> expand(List<FuzzyLiner> liners, int quads) {
        ConcurrentHashMap<FixBS, FuzzyLiner> unique = new ConcurrentHashMap<>();
        liners.stream().parallel().forEach(var -> {
            Quad q = var.quad(quads);
            if (q == null) {
                return;
            }
            List<FuzzyLiner> next = switch (quads) {
                case 1 -> expand(joinOne(var, q));
                case 2 -> expand(joinTwo(var, q));
                default -> throw new IllegalArgumentException();
            };
            for (FuzzyLiner exp : next) {
                unique.putIfAbsent(exp.toLiner().getCanonical(), exp);
            }
        });
        return new ArrayList<>(unique.values());
    }

    private static List<FuzzyLiner> multipleByContradictionAlt(FuzzyLiner base) {
        List<Triple> undefined = base.undefinedTriples();
        if (undefined.size() > Long.SIZE - 1) {
            throw new IllegalStateException(Integer.toString(undefined.size()));
        }
        System.out.println("Checking " + undefined.size());
        List<FuzzyLiner> result = new ArrayList<>();
        long max = 1L << undefined.size();
        for (long i = 0; i < max; i++) {
            FuzzyLiner copy = base.copy();
            for (int j = 0; j < undefined.size(); j++) {
                Triple t = undefined.get(j);
                boolean bit = (i & (1L << j)) != 0;
                if (bit) {
                    copy.colline(t.f(), t.s(), t.t());
                } else {
                    copy.triangule(t.f(), t.s(), t.t());
                }
            }
            try {
                copy.update();
                enhanceFullFano(copy);
                result.add(copy);
            } catch (IllegalArgumentException e) {
                // ok
            }
        }
        return result;
    }

    private static List<FuzzyLiner> multipleByContradiction(FuzzyLiner base) {
        List<Triple> undefined = base.undefinedTriples();
        if (undefined.isEmpty()) {
            return List.of(base);
        }
        List<FuzzyLiner> result = new ArrayList<>();
        Triple first = undefined.getFirst();
        FuzzyLiner colCopy = base.copy();
        colCopy.colline(first.f(), first.s(), first.t());
        FuzzyLiner trCopy = base.copy();
        trCopy.triangule(first.f(), first.s(), first.t());
        singleByContradiction(colCopy);
        singleByContradiction(trCopy);
        result.addAll(multipleByContradiction(colCopy));
        result.addAll(multipleByContradiction(trCopy));
        return result;
    }

    private static FuzzyLiner depthByContradiction(FuzzyLiner base, List<Triple> triples, int idx) {
        if (idx == triples.size()) {
            try {
                base.update();
                enhanceFullFano(base);
                return base;
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        Triple first = triples.get(idx);
        FuzzyLiner trCopy = base.copy();
        trCopy.triangule(first.f(), first.s(), first.t());
        FuzzyLiner res = depthByContradiction(trCopy, triples, idx + 1);
        if (res != null) {
            return res;
        }
        FuzzyLiner colCopy = base.copy();
        colCopy.colline(first.f(), first.s(), first.t());
        return depthByContradiction(colCopy, triples, idx + 1);
    }

    private static void singleByContradiction(FuzzyLiner base) {
        List<Triple> undefined = base.undefinedTriples();
        int size;
        do {
            size = undefined.size();
            for (Triple t : undefined) {
                Boolean needsToBeCollinear = identifyCollinearity(base, t);
                if (needsToBeCollinear == null) {
                    continue;
                }
                if (needsToBeCollinear) {
                    base.colline(t.f(), t.s(), t.t());
                } else {
                    base.triangule(t.f(), t.s(), t.t());
                }
                base.update();
            }
            undefined = base.undefinedTriples();
        } while (undefined.size() != size);
    }

    private static Boolean identifyCollinearity(FuzzyLiner l, Triple t) {
        Boolean result = null;
        try {
            FuzzyLiner copy = l.copy();
            copy.colline(t.f(), t.s(), t.t());
            copy.update();
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            result = false;
        }
        try {
            FuzzyLiner copy = l.copy();
            copy.triangule(t.f(), t.s(), t.t());
            copy.update();
            enhanceFullFano(copy);
        } catch (IllegalArgumentException e) {
            if (result != null) {
                throw new IllegalArgumentException("Total impossibility");
            }
            result = true;
        }
        return result;
    }

    private static FuzzyLiner connectOnes(FuzzyLiner base) {
        Quad q1;
        while ((q1 = base.quad(1)) != null) {
            int newPt = base.getPc();
            System.out.println("Adding two points " + newPt + " " + (newPt + 1) + " for quadruple " + q1);
            base = base.addPoint().addPoint();
            Pair ab = new Pair(q1.a(), q1.b());
            Pair cd = new Pair(q1.c(), q1.d());
            int abcd = base.intersection(ab, cd);
            Pair ac = new Pair(q1.a(), q1.c());
            Pair bd = new Pair(q1.b(), q1.d());
            int acbd = base.intersection(ac, bd);
            Pair ad = new Pair(q1.a(), q1.d());
            Pair bc = new Pair(q1.b(), q1.c());
            int adbc = base.intersection(ad, bc);
            if (abcd >= 0) {
                base.colline(q1.a(), q1.c(), newPt);
                base.colline(q1.b(), q1.d(), newPt);
                base.colline(q1.a(), q1.d(), newPt + 1);
                base.colline(q1.b(), q1.c(), newPt + 1);
                base.colline(abcd, newPt, newPt + 1);
            } else {
                base.colline(q1.a(), q1.b(), newPt);
                base.colline(q1.c(), q1.d(), newPt);
                if (acbd >= 0) {
                    base.colline(q1.a(), q1.d(), newPt + 1);
                    base.colline(q1.b(), q1.c(), newPt + 1);
                    base.colline(acbd, newPt, newPt + 1);
                } else {
                    base.colline(q1.a(), q1.c(), newPt + 1);
                    base.colline(q1.b(), q1.d(), newPt + 1);
                    base.colline(adbc, newPt, newPt + 1);
                }
            }
            base = connectTwos(base);
        }
        return base;
    }

    private static FuzzyLiner connectTwos(FuzzyLiner base) {
        Quad q2;
        while ((q2 = base.quad(2)) != null) {
            int newPt = base.getPc();
            System.out.println("Adding point " + newPt + " to almost full quad " + q2);
            base = base.addPoint();
            Pair ab = new Pair(q2.a(), q2.b());
            Pair cd = new Pair(q2.c(), q2.d());
            int abcd = base.intersection(ab, cd);
            Pair ac = new Pair(q2.a(), q2.c());
            Pair bd = new Pair(q2.b(), q2.d());
            int acbd = base.intersection(ac, bd);
            Pair ad = new Pair(q2.a(), q2.d());
            Pair bc = new Pair(q2.b(), q2.c());
            int adbc = base.intersection(ad, bc);
            if (abcd < 0) {
                base.colline(q2.a(), q2.b(), newPt);
                base.colline(q2.c(), q2.d(), newPt);
                base.colline(acbd, adbc, newPt);
            } else {
                if (acbd < 0) {
                    base.colline(q2.a(), q2.c(), newPt);
                    base.colline(q2.b(), q2.d(), newPt);
                    base.colline(abcd, adbc, newPt);
                } else {
                    base.colline(q2.a(), q2.d(), newPt);
                    base.colline(q2.b(), q2.c(), newPt);
                    base.colline(abcd, acbd, newPt);
                }
            }
            enhanceFullFano(base);
        }
        enhanceFullFano(base);
        return base;
    }

    private static void enhanceFullFano(FuzzyLiner fl) {
        boolean incorrect = true;
        while (incorrect) {
            incorrect = false;
            List<Quad> full = fl.quads(3);
            for (Quad q : full) {
                int abcd = fl.intersection(new Pair(q.a(), q.b()), new Pair(q.c(), q.d()));
                int acbd = fl.intersection(new Pair(q.a(), q.c()), new Pair(q.b(), q.d()));
                int adbc = fl.intersection(new Pair(q.a(), q.d()), new Pair(q.b(), q.c()));
                if (!fl.collinear(abcd, acbd, adbc)) {
                    incorrect = true;
                    fl.colline(abcd, acbd, adbc);
                }
            }
            fl.update();
        }
    }
}
