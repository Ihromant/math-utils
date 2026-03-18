package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.Graph;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.PartialLiner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AssumptionTest {
    @Test
    public void test() {
        int p = 3;
        int n = 6;
        PrimeLinearSpace sp = new PrimeLinearSpace(p, n);
        int pow = LinearSpace.pow(p, n);
        Map<FixBS, Integer> planes = new HashMap<>();
        for (int i = 1; i < pow; i++) {
            for (int j = 1; j < pow; j++) {
                if (i == j || i == sp.neg(j)) {
                    continue;
                }
                planes.putIfAbsent(sp.hull(i, j), planes.size());
            }
        }
        System.out.println(planes.size());
        Map<FixBS, Integer> hyperCubes = new ConcurrentHashMap<>();
        for (Map.Entry<FixBS, Integer> e : planes.entrySet()) {
            hyperCubes.put(sp.orthogonal(e.getKey()), e.getValue());
        }
        System.out.println(hyperCubes.size());
        FixBS a = planes.keySet().iterator().next();
        FixBS b = planes.keySet().stream().filter(pl -> !a.intersects(pl)).findFirst().orElseThrow();
        FixBS abHull = sp.hull(a, b);
        Set<PartialLiner> liners = ConcurrentHashMap.newKeySet();
        AtomicInteger cnt = new AtomicInteger();
        planes.keySet().stream().filter(pl -> !abHull.intersects(pl)).parallel().forEach(c -> {
            FixBS acHull = sp.hull(a, c);
            FixBS bcHull = sp.hull(b, c);
            for (FixBS d : planes.keySet()) {
                if (abHull.intersects(d) || acHull.intersects(d) || bcHull.intersects(d)) {
                    continue;
                }
                FixBS adHull = sp.hull(a, d);
                FixBS bdHull = sp.hull(b, d);
                FixBS cdHull = sp.hull(c, d);
                for (FixBS e : planes.keySet()) {
                    if (abHull.intersects(e) || acHull.intersects(e) || bcHull.intersects(e)
                            || adHull.intersects(e) || bdHull.intersects(e) || cdHull.intersects(e)) {
                        continue;
                    }
                    Set<FixBS> usedPoints = new HashSet<>(List.of(a, b, c, d, e));
                    Set<FixBS> newPoints = new HashSet<>(List.of(a, b, c, d, e));
                    Map<Integer, FixBS> structure = new HashMap<>();
                    enhance(structure, hyperCubes.get(abHull), planes.get(a), planes.get(b));
                    enhance(structure, hyperCubes.get(acHull), planes.get(a), planes.get(c));
                    enhance(structure, hyperCubes.get(adHull), planes.get(a), planes.get(d));
                    enhance(structure, hyperCubes.get(sp.hull(a, e)), planes.get(a), planes.get(e));
                    enhance(structure, hyperCubes.get(bcHull), planes.get(b), planes.get(c));
                    enhance(structure, hyperCubes.get(bdHull), planes.get(b), planes.get(d));
                    enhance(structure, hyperCubes.get(sp.hull(b, e)), planes.get(b), planes.get(e));
                    enhance(structure, hyperCubes.get(cdHull), planes.get(c), planes.get(d));
                    enhance(structure, hyperCubes.get(sp.hull(c, e)), planes.get(c), planes.get(e));
                    enhance(structure, hyperCubes.get(sp.hull(d, e)), planes.get(d), planes.get(e));
                    structure = checkPlaneStruct(structure, newPoints, usedPoints, planes, hyperCubes, sp);
                    if (structure != null) {
                        int[] list = structure.values().stream().reduce(new FixBS(pow), (f, s) -> {
                            f.or(s);
                            return f;
                        }).stream().toArray();
                        int[][] lines = structure.values().stream().map(bs -> bs.stream().map(pt -> Arrays.binarySearch(list, pt)).toArray()).toArray(int[][]::new);
                        PartialLiner plane = new PartialLiner(list.length, lines);
                        if (cnt.incrementAndGet() % 10 == 0) {
                            System.out.println(cnt.get());
                        }
                        if (liners.stream().noneMatch(plane::isomorphicSel)) {
                            liners.add(plane);
                            System.out.println(liners.size());
                            System.out.println(testDesargues(plane));
                        }
                    }
                }
            }
        });
    }

    private static boolean testDesargues(PartialLiner l) {
        for (int o = 0; o < l.pointCount(); o++) {
            for (int l1 : l.point(o)) {
                for (int l2 : l.point(o)) {
                    if (l2 == l1) {
                        continue;
                    }
                    for (int l3 : l.point(o)) {
                        if (l3 == l1 || l3 == l2) {
                            continue;
                        }
                        for (int a1 : l.line(l1)) {
                            if (a1 == o) {
                                continue;
                            }
                            for (int a2 : l.line(l1)) {
                                if (a2 == o || a1 == a2) {
                                    continue;
                                }
                                for (int b1 : l.line(l2)) {
                                    if (b1 == o) {
                                        continue;
                                    }
                                    for (int b2 : l.line(l2)) {
                                        if (b2 == o || b1 == b2) {
                                            continue;
                                        }
                                        for (int c1 : l.line(l3)) {
                                            if (c1 == o) {
                                                continue;
                                            }
                                            for (int c2 : l.line(l3)) {
                                                if (c2 == o || c1 == c2) {
                                                    continue;
                                                }
                                                int i1 = l.intersection(l.line(a1, b1), l.line(a2, b2));
                                                int i2 = l.intersection(l.line(a1, c1), l.line(a2, c2));
                                                int i3 = l.intersection(l.line(c1, b1), l.line(c2, b2));
                                                if (l.line(i1, i2) != l.line(i2, i3)) {
                                                    return false;
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
        return true;
    }

    private static Map<Integer, FixBS> checkPlaneStruct(Map<Integer, FixBS> structure, Set<FixBS> newPoints, Set<FixBS> usedPoints, Map<FixBS, Integer> planes, Map<FixBS, Integer> hyperCubes, PrimeLinearSpace sp) {
        while (!newPoints.isEmpty()) {
            usedPoints.addAll(newPoints);
            Set<FixBS> nextPoints = new HashSet<>();
            for (FixBS x : newPoints) {
                for (FixBS y : usedPoints) {
                    if (planes.get(x) >= planes.get(y)) {
                        continue;
                    }
                    FixBS xy = sp.hull(x, y);
                    Integer xyIdx = hyperCubes.get(xy);
                    if (xyIdx == null) {
                        return null;
                    }
                    for (FixBS z : usedPoints) {
                        if (planes.get(x) >= planes.get(z) || planes.get(y) >= planes.get(z) || xy.intersects(z)) {
                            continue;
                        }
                        FixBS xz = sp.hull(x, z);
                        Integer xzIdx = hyperCubes.get(xz);
                        if (xzIdx == null) {
                            return null;
                        }
                        FixBS yz = sp.hull(y, z);
                        Integer yzIdx = hyperCubes.get(yz);
                        if (yzIdx == null) {
                            return null;
                        }
                        for (FixBS w : usedPoints) {
                            if (planes.get(x) >= planes.get(w) || planes.get(y) >= planes.get(w) || planes.get(z) >= planes.get(w)
                                || xy.intersects(w) || xz.intersects(w) || yz.intersects(w)) {
                                continue;
                            }
                            FixBS xw = sp.hull(x, w);
                            Integer xwIdx = hyperCubes.get(xw);
                            if (xwIdx == null) {
                                return null;
                            }
                            FixBS yw = sp.hull(y, w);
                            Integer ywIdx = hyperCubes.get(yw);
                            if (ywIdx == null) {
                                return null;
                            }
                            FixBS zw = sp.hull(z, w);
                            Integer zwIdx = hyperCubes.get(zw);
                            if (zwIdx == null) {
                                return null;
                            }
                            FixBS xyZw = xy.intersection(zw);
                            Integer xyZwIdx = planes.get(xyZw);
                            if (xyZwIdx == null) {
                                return null;
                            }
                            if (!usedPoints.contains(xyZw)) {
                                nextPoints.add(xyZw);
                            }
                            enhance(structure, xyIdx, xyZwIdx);
                            enhance(structure, zwIdx, xyZwIdx);
                            FixBS xzYw = xz.intersection(yw);
                            Integer xzYwIdx = planes.get(xzYw);
                            if (xzYwIdx == null) {
                                return null;
                            }
                            if (!usedPoints.contains(xzYw)) {
                                nextPoints.add(xzYw);
                            }
                            enhance(structure, xzIdx, xzYwIdx);
                            enhance(structure, ywIdx, xzYwIdx);
                            FixBS xwYz = xw.intersection(yz);
                            Integer xwYzIdx = planes.get(xwYz);
                            if (xwYzIdx == null) {
                                return null;
                            }
                            if (!usedPoints.contains(xwYz)) {
                                nextPoints.add(xwYz);
                            }
                            enhance(structure, xwIdx, xwYzIdx);
                            enhance(structure, yzIdx, xwYzIdx);
                        }
                    }
                }
            }
            newPoints = nextPoints;
        }
        return structure;
    }

    @Test
    public void testBijections() {
        List<Bijection> bijections = new PrimeLinearSpace(2, 4).bijections();
        System.out.println(bijections.size());
    }

    private static void enhance(Map<Integer, FixBS> map, int line, int... pts) {
        FixBS val = map.get(line);
        if (val != null) {
            for (int pt : pts) {
                val.set(pt);
            }
        } else {
            map.put(line, FixBS.of(Arrays.stream(pts).max().orElseThrow() + 1, pts));
        }
    }

    @Test
    public void testGroupable() {
        Group gr = new CyclicProduct(2, 2, 2, 2, 2, 2);
        int k = 4;
        int v = gr.order();
        int r = (v - 1) / (k - 1);
        List<SubGroup> baseSgs = gr.subGroups().stream().filter(sg -> sg.order() == k).toList();
        FixBS fstElems = FixBS.of(64, 0, 1, 2, 3);
        FixBS sndElems = FixBS.of(64, 0, 4, 8, 12);
        FixBS trdElems = FixBS.of(64, 0, 16, 32, 48);
        SubGroup fst = baseSgs.stream().filter(sg -> sg.elems().equals(fstElems)).findFirst().orElseThrow();
        SubGroup snd = baseSgs.stream().filter(sg -> sg.elems().equals(sndElems)).findFirst().orElseThrow();
        SubGroup trd = baseSgs.stream().filter(sg -> sg.elems().equals(trdElems)).findFirst().orElseThrow();
        List<int[]> initLns = new ArrayList<>();
        Arrays.stream(fst.leftCosets()).map(FixBS::toArray).forEach(initLns::add);
        Arrays.stream(snd.leftCosets()).map(FixBS::toArray).forEach(initLns::add);
        Arrays.stream(trd.leftCosets()).map(FixBS::toArray).forEach(initLns::add);
        List<SubGroup> sgs = baseSgs.stream().filter(s -> s.elems().intersection(fst.elems()).cardinality() == 1
                && s.elems().intersection(snd.elems()).cardinality() == 1 && s.elems().intersection(trd.elems()).cardinality() == 1).toList();
        System.out.println(sgs.size());
        Graph g = Graph.by(sgs, (a, b) -> a.elems().intersection(b.elems()).cardinality() == 1);
        Set<FixBS> canons = new HashSet<>();
        JNauty.instance().maximalCliques(g, r - 3, a -> {
            FixBS arr = new FixBS(a);
            List<int[]> lns = new ArrayList<>(initLns);
            List<FixBS> base = new ArrayList<>();
            for (int el = arr.nextSetBit(0); el >= 0; el = arr.nextSetBit(el + 1)) {
                SubGroup sg = sgs.get(el);
                base.add(sg.elems());
                Arrays.stream(sg.leftCosets()).map(FixBS::toArray).forEach(lns::add);
            }
            Liner lnr = new Liner(v, lns.toArray(int[][]::new));
            if (canons.add(new FixBS(lnr.graphData().canonical()))) {
                System.out.println(lnr.graphData().autCount() + " " + lnr.hyperbolicFreq());
            }
        });
        System.out.println(canons.size());
    }

    @Test
    public void groupableLiners() {
        Path path = Path.of("/home/ihromant/maths/g-spaces/final/4-64/large.txt");
        LinearSpace sp = LinearSpace.of(2, 6);
        List<Long> gens = new ArrayList<>();
        gens.add(fromMapping(sp, new int[]{4, 8, 16, 32, 1, 2}));
        gens.add(fromMapping(sp, new int[]{1, 2, 16, 32, 4, 8}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, 16}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, sp.add(16, 32)}));
        List<Long> baseStab = closure(sp, gens).stream().sorted().toList();
        FixBS fst = sp.hull(1, 2);
        fst.set(0);
        FixBS snd = sp.hull(4, 8);
        snd.set(0);
        FixBS trd = sp.hull(16, 32);
        trd.set(0);
        int r = (sp.cardinality() - 1) / (fst.cardinality() - 1);
        Set<FixBS> fixed = Set.of(fst, snd, trd);
        for (Long op : baseStab) {
            Set<FixBS> mapped = fixed.stream().map(s -> sp.applyOper(op, s)).collect(Collectors.toSet());
            assertEquals(mapped, fixed);
        }
        List<FixBS> subs = sp.subSpaces(2);
        assertEquals(subs, subs.stream().sorted().toList());
        List<FixBS> init = subs.stream().filter(s -> s.intersection(fst.union(snd).union(trd)).cardinality() == 1).toList();
        List<List<FixBS>> bases = new ArrayList<>();
        findBases(sp, baseStab, init, init, fixed, 0, s -> {
            bases.add(s.stream().sorted().toList());
        });
        System.out.println(bases.size());
        System.out.println(bases.stream().collect(Collectors.groupingBy(List::size, Collectors.counting())));
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        AtomicInteger ai = new AtomicInteger();
        bases.parallelStream().forEach(base -> {
            List<FixBS> initLns = new ArrayList<>();
            base.forEach(s -> initLns.addAll(sp.cosets(s)));
            FixBS union = base.stream().reduce(FixBS::union).orElseThrow();
            List<FixBS> suitable = subs.stream().filter(s -> s.intersection(union).cardinality() == 1).toList();
            Graph g = Graph.by(suitable, (a, b) -> a.intersection(b).cardinality() == 1);
            if (suitable.isEmpty()) {
                return;
            }
            JNauty.instance().maximalCliques(g, r - base.size(), a -> {
                FixBS arr = new FixBS(a);
                List<FixBS> lns = new ArrayList<>(initLns);
                for (int el = arr.nextSetBit(0); el >= 0; el = arr.nextSetBit(el + 1)) {
                    FixBS sg = suitable.get(el);
                    lns.addAll(sp.cosets(sg));
                }
                Liner lnr = new Liner(lns.toArray(FixBS[]::new));
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    Map<Integer, Integer> freq = lnr.hyperbolicFreq();
                    System.out.println(lnr.graphData().autCount() + " " + freq);
                    try {
                        Files.writeString(path, lnr.graphData().autCount() + " " + freq + " "
                                + Arrays.deepToString(lnr.lines()) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            int val = ai.incrementAndGet();
            if (val % 1000 == 0) {
                System.out.println(val);
            }
        });
    }

    private static void findBases(LinearSpace sp, List<Long> stab, List<FixBS> transversal, List<FixBS> full, Set<FixBS> curr, int from, Consumer<Set<FixBS>> cons) {
        if (stab.size() == 1) {
            cons.accept(curr);
            return;
        }
        QuickFind qf = new QuickFind(transversal.size());
        for (Long st : stab) {
            for (int i = 0; i < transversal.size(); i++) {
                FixBS tr = transversal.get(i);
                FixBS mapped = sp.applyOper(st, tr);
                qf.union(i, Collections.binarySearch(transversal, mapped));
            }
        }
        List<FixBS> comps = qf.components();
        for (FixBS comp : comps) {
            FixBS tr = transversal.get(comp.nextSetBit(0));
            int trIdx = Collections.binarySearch(full, tr);
            if (trIdx < from) {
                continue;
            }
            Set<FixBS> nextCurr = new HashSet<>(curr);
            nextCurr.add(tr);
            List<Long> nextStab = new ArrayList<>();
            for (Long op : stab) {
                Set<FixBS> mapped = nextCurr.stream().map(s -> sp.applyOper(op, s)).collect(Collectors.toSet());
                if (mapped.equals(nextCurr)) {
                    nextStab.add(op);
                }
            }
            List<FixBS> nextTransversal = transversal.stream().filter(s -> s.intersection(tr).cardinality() == 1).toList();
            findBases(sp, nextStab, nextTransversal, full, nextCurr, trIdx, cons);
        }
    }

    private static long fromMapping(LinearSpace sp, int[] base) {
        long result = 0L;
        for (int i = sp.n() - 1; i >= 0; i--) {
            result = result * sp.cardinality() + base[i];
        }
        return result;
    }

    private static Set<Long> closure(LinearSpace sp, List<Long> gens) {
        Set<Long> result = new HashSet<>();
        result.add(fromMapping(sp, IntStream.range(0, sp.n()).map(i ->
                sp.fromCrd(IntStream.range(0, sp.n()).map(j -> i == j ? 1 : 0).toArray())).toArray()));
        boolean added;
        do {
            added = false;
            for (Long el : result.toArray(Long[]::new)) {
                for (long gen : gens) {
                    Long xy = sp.mulOper(gen, el);
                    Long yx = sp.mulOper(el, gen);
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
            }
        } while (added);
        return result;
    }
}
