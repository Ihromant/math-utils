package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.jnauty.GraphData;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.GaloisField;
import ua.ihromant.mathutils.Graph;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.LongList;
import ua.ihromant.mathutils.PartialLiner;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.GeneralLinear;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.group.TableGroup;
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
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
            for (int el = arr.nextSetBit(0); el >= 0; el = arr.nextSetBit(el + 1)) {
                SubGroup sg = sgs.get(el);
                Arrays.stream(sg.leftCosets()).map(FixBS::toArray).forEach(lns::add);
            }
            Liner lnr = new Liner(v, lns.toArray(int[][]::new));
            if (canons.add(new FixBS(lnr.graphData().canonical()))) {
                System.out.println(lnr.graphData().autCount() + " " + lnr.hyperbolicFreq());
            }
        });
        System.out.println(canons.size());
    }

    private static int hull(int a, int b) {
        if (a == b) {
            throw new IllegalArgumentException();
        }
        int c = a ^ b;
        if (a < b) {
            if (b < c) {
                return a | (b << 6) | (c << 12); // abc
            } else {
                if (a < c) {
                    return a | (c << 6) | (b << 12); // acb
                } else {
                    return c | (a << 6) | (b << 12); // cab
                }
            }
        } else {
            if (a < c) {
                return b | (a << 6) | (c << 12); // bac
            } else {
                if (b < c) {
                    return b | (c << 6) | (a << 12); // bca
                } else {
                    return c | (b << 6) | (a << 12); // cba
                }
            }
        }
    }

    private static long permutator(long a, long b, long c) {
        return (a & 63) | (((a >>> 6) & 63) << 6)
                | ((b & 31) << 12) | (((b >>> 6) & 63) << 18)
                | ((c & 31) << 24) | (((c >>> 6) & 63) << 30);
    }

    private static boolean orthogonal(int a, int b) {
        long comb = 0;
        for (int el : others(a, b)) {
            long sh = 1L << el;
            if ((comb & sh) != 0) {
                return false;
            }
            comb = comb | sh;
        }
        return true;
    }

    private static int[] others(int a, int b) {
        int a1 = a & 63;
        int a2 = (a >>> 6) & 63;
        int a3 = (a >>> 12) & 63;
        int b1 = b & 63;
        int b2 = (b >>> 6) & 63;
        int b3 = (b >>> 12) & 63;
        return new int[]{a1 ^ b1, a2 ^ b1, a3 ^ b1, a1 ^ b2, a2 ^ b2, a3 ^ b2, a1 ^ b3, a2 ^ b3, a3 ^ b3};
    }

    private static boolean orthogonal(int a, int b, int c) {
        if (!orthogonal(a, b) || !orthogonal(b, c) || !orthogonal(a, c)) {
            return false;
        }
        int[] ab = others(a, b);
        int c1 = c & 63;
        int c2 = (c >>> 6) & 63;
        int c3 = (c >>> 12) & 63;
        for (int el : ab) {
            if (c1 == el || c2 == el || c3 == el) {
                return false;
            }
        }
        int b1 = b & 63;
        int b2 = (b >>> 6) & 63;
        int b3 = (b >>> 12) & 63;
        int[] ac = others(a, c);
        for (int el : ac) {
            if (b1 == el || b2 == el || b3 == el) {
                return false;
            }
        }
        int a1 = a & 63;
        int a2 = (a >>> 6) & 63;
        int a3 = (a >>> 12) & 63;
        int[] bc = others(b, c);
        for (int el : bc) {
            if (a1 == el || a2 == el || a3 == el) {
                return false;
            }
        }
        return true;
    }

    private static int applyToSs(LinearSpace sp, long oper, int ss) {
        return hull(sp.applyOper(oper, ss & 63), sp.applyOper(oper, (ss >>> 6) & 63));
    }

    @Test
    public void testPermutator() {
        LinearSpace sp = LinearSpace.of(2, 6);
        Set<Integer> hulls = new HashSet<>();
        for (int i = 1; i < sp.cardinality(); i++) {
            for (int j = i + 1; j < sp.cardinality(); j++) {
                hulls.add(hull(i, j));
            }
        }
        int[] arr = hulls.stream().mapToInt(Integer::intValue).sorted().toArray();
        System.out.println(hulls.size());
        int[] base = new int[]{hull(1, 2), hull(4, 8), hull(16, 32)};
        System.out.println(Long.toBinaryString(permutator(base[0], base[1], base[2])));
        int[] filtered = Arrays.stream(arr).filter(i -> Arrays.stream(base).allMatch(j -> orthogonal(i, j))).toArray();
        System.out.println(filtered.length);
        List<Long> gens = new ArrayList<>();
        gens.add(fromMapping(sp, new int[]{4, 8, 16, 32, 1, 2}));
        gens.add(fromMapping(sp, new int[]{1, 2, 16, 32, 4, 8}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, 16}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, sp.add(16, 32)}));
        long[] baseStab = closure(sp, gens);
        System.out.println(baseStab.length);
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                for (int k = j + 1; k < arr.length; k++) {
                    int a = arr[i];
                    int b = arr[j];
                    int c = arr[k];
                    if (!orthogonal(a, b, c)) {
                        continue;
                    }
                    long perm = permutator(a, b, c);
                    assertEquals(a, applyToSs(sp, perm, base[0]));
                    assertEquals(b, applyToSs(sp, perm, base[1]));
                    assertEquals(c, applyToSs(sp, perm, base[2]));
                    for (long p : baseStab) {
                        long pPerm = sp.mulOper(p, perm);
                        int[] aa = new int[]{a, b, c};
                        int[] aaa = new int[]{applyToSs(sp, pPerm, base[0]), applyToSs(sp, pPerm, base[1]), applyToSs(sp, pPerm, base[2])};
                        Arrays.sort(aaa);
                        assertArrayEquals(aa, aaa);
                    }
                }
            }
        }
    }

    @Test
    public void groupableLiners() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Path path = Path.of("/home/ihromant/maths/g-spaces/final/4-64/large.txt");
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        List<String> lines = Files.readAllLines(path);
        lines.parallelStream().forEach(ln -> {
            Liner lnr = new Liner(om.readValue(ln.substring(ln.indexOf("[[")), int[][].class));
            unique.add(new FixBS(lnr.graphData().canonical()));
        });
        LinearSpace sp = LinearSpace.of(2, 6);
        List<Long> gens = new ArrayList<>();
        gens.add(fromMapping(sp, new int[]{4, 8, 16, 32, 1, 2}));
        gens.add(fromMapping(sp, new int[]{1, 2, 16, 32, 4, 8}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, 16}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, sp.add(16, 32)}));
        long[] baseStab = closure(sp, gens);
        FixBS fst = sp.hull(1, 2);
        fst.set(0);
        FixBS snd = sp.hull(4, 8);
        snd.set(0);
        FixBS trd = sp.hull(16, 32);
        trd.set(0);
        int r = (sp.cardinality() - 1) / (fst.cardinality() - 1);
        List<FixBS> fixed = new ArrayList<>(List.of(fst, snd, trd));
        for (long op : baseStab) {
            List<FixBS> mapped = fixed.stream().map(s -> sp.applyOper(op, s)).sorted().toList();
            assertEquals(mapped, fixed);
        }
        List<FixBS> subs = sp.subSpaces(2);
        assertEquals(subs, subs.stream().sorted().toList());
        List<FixBS> init = subs.stream().filter(s -> s.intersection(fst.union(snd).union(trd)).cardinality() == 1).toList();
        List<List<FixBS>> bases = new ArrayList<>();
        findBases(sp, baseStab, init, fixed, (s, stab) -> {
            if (stab.length >= sp.p() && s.size() < r) {
                return false;
            }
            bases.add(s.stream().sorted().toList());
            return true;
        });
        Set<List<FixBS>> unProcessed = ConcurrentHashMap.newKeySet();
        unProcessed.addAll(bases);
        Path basesPath = Path.of("/home/ihromant/maths/g-spaces/final/4-64/bases.txt");
        lines = Files.readAllLines(basesPath);
        lines.parallelStream().forEach(ln -> {
            ln = ln.replace('{', '[').replace('}', ']');
            int[][] arr = om.readValue(ln, int[][].class);
            List<FixBS> lst = Arrays.stream(arr).map(a -> FixBS.of(sp.cardinality(), a)).toList();
            unProcessed.remove(lst);
        });
        AtomicInteger ai = new AtomicInteger();
        System.out.println(unProcessed.size());
        new ArrayList<>(unProcessed).parallelStream().forEach(base -> {
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
                    Map<Integer, Long> freq = lnr.hyperbolicFreq();
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
            try {
                Files.writeString(basesPath, base + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void largeGroupable() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Path path = Path.of("/home/ihromant/maths/g-spaces/final/9-729/large.txt");
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        List<String> lines = Files.readAllLines(path);
        lines.parallelStream().forEach(ln -> {
            Liner lnr = new Liner(om.readValue(ln.substring(ln.indexOf("[[")), int[][].class));
            unique.add(new FixBS(lnr.graphData().canonical()));
        });
        LinearSpace sp = LinearSpace.of(3, 6);
//        List<Long> gens = new ArrayList<>();
//        gens.add(fromMapping(sp, new int[]{9, 27, 81, 243, 1, 3}));
//        gens.add(fromMapping(sp, new int[]{1, 3, 81, 243, 9, 27}));
//        gens.add(fromMapping(sp, new int[]{1, 3, 9, 27, 243, 81}));
//        gens.add(fromMapping(sp, new int[]{1, 3, 9, 27, 243, sp.add(81, 243)}));
        long[] baseStab = Arrays.stream(Files.readString(Path.of("/home/ihromant/maths/g-spaces/final/9-729/stab.txt")).split(" "))
                .mapToLong(Long::parseLong).toArray(); //closure(sp, gens).stream().sorted().toList();
        System.out.println(baseStab.length);
        FixBS fst = sp.hull(1, 3);
        fst.set(0);
        FixBS snd = sp.hull(9, 27);
        snd.set(0);
        FixBS trd = sp.hull(81, 243);
        trd.set(0);
        int r = (sp.cardinality() - 1) / (fst.cardinality() - 1);
        List<FixBS> fixed = List.of(fst, snd, trd);
        for (long op : baseStab) {
            List<FixBS> mapped = fixed.stream().map(s -> sp.applyOper(op, s)).sorted().toList();
            assertEquals(mapped, fixed);
        }
        List<FixBS> subs = sp.subSpaces(2);
        assertEquals(subs, subs.stream().sorted().toList());
        List<FixBS> init = subs.stream().filter(s -> s.intersection(fst.union(snd).union(trd)).cardinality() == 1).toList();
        List<List<FixBS>> bases = new ArrayList<>();
        Map<Integer, Long> frq = new HashMap<>();
        findBases(sp, baseStab, init, fixed, (s, stab) -> {
            if (stab.length >= sp.p() && s.size() < r) {
                return false;
            }
            System.out.println(s.size());
            frq.compute(s.size(), (_, v) -> v == null ? 1 : v + 1);
            return true;
            //bases.add(s.stream().sorted().toList());
        });
        System.out.println(frq);
        Set<List<FixBS>> unProcessed = ConcurrentHashMap.newKeySet();
        unProcessed.addAll(bases);
        Path basesPath = Path.of("/home/ihromant/maths/g-spaces/final/9-729/bases.txt");
        lines = Files.readAllLines(basesPath);
        lines.parallelStream().forEach(ln -> {
            ln = ln.replace('{', '[').replace('}', ']');
            int[][] arr = om.readValue(ln, int[][].class);
            List<FixBS> lst = Arrays.stream(arr).map(a -> FixBS.of(sp.cardinality(), a)).toList();
            unProcessed.remove(lst);
        });
        AtomicInteger ai = new AtomicInteger();
        System.out.println(unProcessed.size());
        new ArrayList<>(unProcessed).parallelStream().forEach(base -> {
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
                    Map<Integer, Long> freq = lnr.hyperbolicFreq();
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
            try {
                Files.writeString(basesPath, base + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void findBases(LinearSpace sp, long[] stab, List<FixBS> transversal, List<FixBS> curr, BiPredicate<List<FixBS>, long[]> cons) {
        if (cons.test(curr, stab)) {
            return;
        }
        List<FixBS> minimal = new ArrayList<>();
        ex: for (FixBS tr : transversal) {
            for (long st : stab) {
                FixBS mapped = sp.applyOper(st, tr);
                if (mapped.compareTo(tr) < 0) {
                    continue ex;
                }
            }
            minimal.add(tr);
        }
        for (FixBS tr : minimal) {
            if (!curr.isEmpty() && tr.compareTo(curr.get(curr.size() - 2)) < 0) {
                continue;
            }
            List<FixBS> nextCurr = new ArrayList<>(curr);
            nextCurr.add(-Collections.binarySearch(curr, tr) - 1, tr);
            LongList nextStab = new LongList(stab.length);
            for (long op : stab) {
                List<FixBS> mapped = nextCurr.stream().map(s -> sp.applyOper(op, s)).sorted().toList();
                if (mapped.equals(nextCurr)) {
                    nextStab.add(op);
                }
            }
            List<FixBS> nextTransversal = transversal.stream().filter(s -> s.intersection(tr).cardinality() == 1).toList();
            findBases(sp, nextStab.toArray(), nextTransversal, nextCurr, cons);
        }
    }

    @Test
    public void translationPlanes() throws IOException {
        ObjectMapper om = new ObjectMapper();
        Path path = Path.of("/home/ihromant/maths/g-spaces/final/25-625/large.txt");
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        List<String> lines = Files.readAllLines(path);
        lines.parallelStream().forEach(ln -> {
            Liner lnr = new Liner(om.readValue(ln.substring(ln.indexOf("[[")), int[][].class));
            unique.add(new FixBS(lnr.graphData().canonical()));
        });
        LinearSpace sp = LinearSpace.of(5, 4);
        List<Long> gens = new ArrayList<>();
        gens.add(fromMapping(sp, new int[]{25, 125, 1, 5}));
        gens.add(fromMapping(sp, new int[]{1, 5, 125, 25}));
        gens.add(fromMapping(sp, new int[]{1, 5, 125, sp.add(25, 125)}));
        gens.add(fromMapping(sp, new int[]{1, 5, sp.sub(25, 125), sp.add(25, 125)}));
        long[] baseStab = closure(sp, gens);
        List<FixBS> subs = sp.subSpaces(2);
        FixBS fst = sp.hull(1, 5);
        fst.set(0);
        FixBS snd = sp.hull(25, 125);
        snd.set(0);
        FixBS trd = subs.stream().filter(s -> s.intersection(fst.union(snd)).cardinality() == 1).findFirst().orElseThrow();
        int r = (sp.cardinality() - 1) / (fst.cardinality() - 1);
        List<FixBS> fixed = new ArrayList<>(List.of(fst, snd, trd));
        long[] applicableStab = Arrays.stream(baseStab).parallel().filter(op -> {
            List<FixBS> mapped = fixed.stream().map(s -> sp.applyOper(op, s)).sorted().toList();
            return mapped.equals(fixed);
        }).sorted().toArray();
        System.out.println(applicableStab.length);
        List<FixBS> init = subs.stream().filter(s -> s.intersection(fst.union(snd).union(trd)).cardinality() == 1).toList();
        List<List<FixBS>> bases = new ArrayList<>();
        findBases(sp, applicableStab, init, fixed, (s, stab) -> {
            if (stab.length >= sp.p() && s.size() < r) {
                return false;
            }
            bases.add(s.stream().sorted().toList());
            return true;
        });
        Set<List<FixBS>> unProcessed = ConcurrentHashMap.newKeySet();
        unProcessed.addAll(bases);
        Path basesPath = Path.of("/home/ihromant/maths/g-spaces/final/25-625/bases.txt");
        lines = Files.readAllLines(basesPath);
        lines.parallelStream().forEach(ln -> {
            ln = ln.replace('{', '[').replace('}', ']');
            int[][] arr = om.readValue(ln, int[][].class);
            List<FixBS> lst = Arrays.stream(arr).map(a -> FixBS.of(sp.cardinality(), a)).toList();
            unProcessed.remove(lst);
        });
        AtomicInteger ai = new AtomicInteger();
        System.out.println(unProcessed.size());
        new ArrayList<>(unProcessed).parallelStream().forEach(base -> {
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
                    Map<Integer, Long> freq = lnr.hyperbolicFreq();
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
            try {
                Files.writeString(basesPath, base + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static long fromMapping(LinearSpace sp, int[] base) {
        long result = 0L;
        for (int i = sp.n() - 1; i >= 0; i--) {
            result = result * sp.cardinality() + base[i];
        }
        return result;
    }

    private static long[] closure(LinearSpace sp, List<Long> gens) {
        Set<Long> result = new HashSet<>();
        result.add(fromMapping(sp, IntStream.range(0, sp.n()).map(i ->
                sp.fromCrd(IntStream.range(0, sp.n()).map(j -> i == j ? 1 : 0).toArray())).toArray()));
        boolean added;
        do {
            added = false;
            for (long el : result.toArray(Long[]::new)) {
                for (long gen : gens) {
                    long xy = sp.mulOper(gen, el);
                    long yx = sp.mulOper(el, gen);
                    added = result.add(xy) || added;
                    added = result.add(yx) || added;
                }
            }
        } while (added);
        return result.stream().mapToLong(Long::longValue).sorted().toArray();
    }

    private static final int[] baseHulls = {hull(1, 2), hull(4, 8), hull(16, 32)};

    @Test
    public void expand() throws IOException {
        int p = 2;
        int n = 6;
        int len = 7;
        LinearSpace sp = LinearSpace.of(p, n);
        ObjectMapper om = new ObjectMapper();
        List<Long> gens = new ArrayList<>();
        gens.add(fromMapping(sp, new int[]{4, 8, 16, 32, 1, 2}));
        gens.add(fromMapping(sp, new int[]{1, 2, 16, 32, 4, 8}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, 16}));
        gens.add(fromMapping(sp, new int[]{1, 2, 4, 8, 32, sp.add(16, 32)}));
        long[] baseStab = closure(sp, gens);
        Set<Integer> set = new HashSet<>();
        for (int i = 1; i < sp.cardinality(); i++) {
            for (int j = i + 1; j < sp.cardinality(); j++) {
                set.add(hull(i, j));
            }
        }
        int[] hulls = set.stream().mapToInt(Integer::intValue).sorted().toArray();
        int[] filtered = Arrays.stream(hulls).filter(i -> Arrays.stream(baseHulls).allMatch(j -> orthogonal(i, j))).toArray();
        List<String> lines = Files.readAllLines(Path.of("/home/ihromant/maths/g-spaces/final/4-64/begins-" + p + "^" + n + "-" + len + ".txt"));
        Path next = Path.of("/home/ihromant/maths/g-spaces/final/4-64/begins-" + p + "^" + n + "-" + (len + 1) + ".txt");
        System.out.println(lines.size());
        AtomicInteger cnt = new AtomicInteger();
        lines.parallelStream().forEach(ln -> {
            int[] arr = om.readValue(ln, int[].class);
            int[] newV = Arrays.stream(filtered).filter(el -> Arrays.stream(arr)
                    .allMatch(el1 -> orthogonal(el, el1))).toArray();
            findBasesB(sp, baseStab, newV, arr, s -> {
                if (s.length < len + 1) {
                    return false;
                }
                try {
                    Files.writeString(next, Arrays.toString(s) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return true;
            });
            System.out.println(cnt.getAndIncrement());
        });
    }

    private static void findBasesB(LinearSpace sp, long[] stab, int[] transversal, int[] curr, Predicate<int[]> cons) {
        if (cons.test(curr)) {
            return;
        }
        int[][] choices = Combinatorics.choices(curr.length, 3).toArray(int[][]::new);
        LongList stabilizers = new LongList(stab.length * choices.length);
        for (int[] choice : choices) {
            int a = curr[choice[0]];
            int b = curr[choice[1]];
            int c = curr[choice[2]];
            if (!orthogonal(a, b, c)) {
                continue;
            }
            long perm = permutator(a, b, c);
            for (long p : stab) {
                long pPerm = sp.mulOper(p, perm);
                if (Arrays.stream(curr).anyMatch(el -> Arrays.binarySearch(curr, applyToSs(sp, pPerm, el)) < 0)) {
                    continue;
                }
                stabilizers.add(pPerm);
            }
        }
        IntList minimals = new IntList(transversal.length);
        ex: for (int tr : transversal) {
            for (int i = 0; i < stabilizers.size(); i++) {
                long st = stabilizers.get(i);
                int mapped = applyToSs(sp, st, tr);
                if (mapped < tr) {
                    continue ex;
                }
            }
            minimals.add(tr);
        }
        for (int i = 0; i < minimals.size(); i++) {
            int tr = minimals.get(i);
            int[] nextCurr = append(curr, tr);
            if (nextCurr == null) {
                continue;
            }
            IntList nextTransversal = new IntList(transversal.length);
            for (int s : transversal) {
                if (orthogonal(s, tr)) {
                    nextTransversal.add(s);
                }
            }
            findBasesB(sp, stab, nextTransversal.toArray(), nextCurr, cons);
        }
    }

    private static int[] append(int[] curr, int tr) {
        int idx = curr.length - 1;
        while (true) {
            int c = curr[idx];
            boolean more = tr > c;
            if (more) {
                idx++;
                break;
            } else {
                if (c != baseHulls[2]) {
                    return null;
                }
            }
            idx--;
        }
        int[] nextCurr = new int[curr.length + 1];
        System.arraycopy(curr, 0, nextCurr, 0, idx);
        System.arraycopy(curr, idx, nextCurr, idx + 1, curr.length - idx);
        nextCurr[idx] = tr;
        return nextCurr;
    }

    @Test
    public void process() throws IOException {
        int p = 2;
        int n = 6;
        int len = 8;
        LinearSpace sp = LinearSpace.of(p, n);
        int v = sp.cardinality();
        int k = 4;
        int r = (v - 1) / (k - 1);
        ObjectMapper om = new ObjectMapper();
        Set<Integer> set = new HashSet<>();
        for (int i = 1; i < sp.cardinality(); i++) {
            for (int j = i + 1; j < sp.cardinality(); j++) {
                set.add(hull(i, j));
            }
        }
        int[] hulls = set.stream().mapToInt(Integer::intValue).sorted().toArray();
        List<String> lns = Files.readAllLines(Path.of("/home/ihromant/maths/g-spaces/final/4-64/begins-" + p + "^" + n + "-" + len + ".txt"));
        Set<ArrWrap> un = new HashSet<>();
        lns.forEach(l -> un.add(new ArrWrap(om.readValue(l, int[].class))));
        Path proc = Path.of("/home/ihromant/maths/g-spaces/final/4-64/begins-" + p + "^" + n + "-" + len + "proc.txt");
        List<String> processed = Files.readAllLines(proc);
        processed.forEach(l -> un.remove(new ArrWrap(om.readValue(l, int[].class))));
        Path path = Path.of("/home/ihromant/maths/g-spaces/final/4-64/large.txt");
        Set<FixBS> unique = ConcurrentHashMap.newKeySet();
        List<String> lines = Files.readAllLines(path);
        lines.parallelStream().forEach(ln -> {
            Liner lnr = new Liner(om.readValue(ln.substring(ln.indexOf("[[")), int[][].class));
            unique.add(new FixBS(lnr.graphData().canonical()));
        });
        AtomicInteger ai = new AtomicInteger();
        System.out.println(un.size());
        un.parallelStream().forEach(aw -> {
            int[] arr = aw.arr();
            int last = arr[arr.length - 1] == baseHulls[2] ? arr[arr.length - 2] : arr[arr.length - 1];
            int[] suitable = Arrays.stream(hulls).filter(i -> i > last && Arrays.stream(arr).allMatch(j -> orthogonal(i, j))).toArray();
            int needed = r - arr.length;
            if (suitable.length < needed) {
                try {
                    Files.writeString(proc, Arrays.toString(arr) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                return;
            }
            Graph g = Graph.by(suitable, AssumptionTest::orthogonal);
            JNauty.instance().maximalCliques(g, needed, a -> {
                FixBS els = new FixBS(a);
                List<FixBS> lnz = new ArrayList<>();
                for (int el = els.nextSetBit(0); el >= 0; el = els.nextSetBit(el + 1)) {
                    int sg = suitable[el];
                    FixBS subspace = FixBS.of(v, 0, sg & 63, (sg >>> 6) & 63, (sg >>> 12) & 63);
                    lnz.addAll(sp.cosets(subspace));
                }
                for (int sg : arr) {
                    FixBS subspace = FixBS.of(v, 0, sg & 63, (sg >>> 6) & 63, (sg >>> 12) & 63);
                    lnz.addAll(sp.cosets(subspace));
                }
                Liner lnr = new Liner(lnz.toArray(FixBS[]::new));
                if (unique.add(new FixBS(lnr.graphData().canonical()))) {
                    Map<Integer, Long> freq = lnr.hyperbolicFreq();
                    System.out.println(lnr.graphData().autCount() + " " + freq);
                    try {
                        Files.writeString(path, lnr.graphData().autCount() + " " + freq + " "
                                + Arrays.deepToString(lnr.lines()) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            try {
                Files.writeString(proc, Arrays.toString(arr) + "\n", StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
    }

    private record ArrWrap(int[] arr) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[] arr1))) return false;

            return Arrays.equals(arr, arr1);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(arr);
        }
    }

    @Test
    public void findNotNormalElementary() throws IOException {
//        Liner proj = new Liner(new GaloisField(4).generateSpace());
//        Liner aff = proj.subPlane(IntStream.range(0, 64).toArray());
//        int[][] linerPermutations = aff.graphData().automorphisms();
//        int[][] zeroFixed = Arrays.stream(linerPermutations).filter(arr -> arr[0] == 0).toArray(int[][]::new);
//        PermutationGroup zeroFixedPerm = new PermutationGroup(zeroFixed);
        int[][] translations = IntStream.range(0, 64).mapToObj(i -> IntStream.range(0, 64).map(j -> i ^ j).toArray()).toArray(int[][]::new);
//        PermutationGroup linerAuth = new PermutationGroup(linerPermutations);
        GeneralLinear gl = new GeneralLinear(3, new GaloisField(4));
        SubGroup closure = new SubGroup(gl, FixBS.of(gl.order(), 0));
        while (closure.order() < 64) {
            for (int el = 1; el < gl.order(); el++) {
                if (el == gl.order() - 1) {
                    System.out.println(el);
                }
                int ord = gl.order(el);
                int ee = el;
                if ((ord & (ord - 1)) != 0 || closure.elems().get(el)
                        || Arrays.stream(closure.arr()).anyMatch(e -> {
                    int ord1 = gl.order(gl.op(ee, e));
                    int ord2 = gl.order(gl.op(e, ee));
                    return (ord1 & (ord1 - 1)) != 0 || (ord2 & (ord2 - 1)) != 0;
                })) {
                    continue;
                }
                FixBS els = closure.elems().copy();
                els.set(el);
                closure = gl.closure(els);
                if (closure.order() == 64) {
                    break;
                }
            }
        }
        System.out.println(closure.elems());
        List<int[]> gens = new ArrayList<>();
        for (int mat : closure.arr()) {
            int[] arr = new int[64];
            for (int vec = 0; vec < 64; vec++) {
                arr[vec] = gl.mulVec(mat, vec);
            }
            gens.add(arr);
        }
        gens.add(IntStream.range(0, 64).map(AssumptionTest::flip).toArray());
//        GraphData gdx = new GraphData(gens.toArray(int[][]::new), new int[64], 0, null, null);
//        PermutationGroup sylow128 = new PermutationGroup(gdx.automorphisms());
//        FixBS normalizer = normalizer(zeroFixedPerm, sylow128);
        gens.addAll(Arrays.asList(translations));
        GraphData gd = new GraphData(gens.toArray(int[][]::new), new int[64], 0, null, null);
        int[][] aut = gd.automorphisms();
        System.out.println(aut.length);
        PermutationGroup pg = new PermutationGroup(aut);
        TableGroup tg = pg.asTable();
        Map<Integer, List<SubGroup>> conj = tg.subsByConjugation();
        List<SubGroup> subs = conj.get(64);
        List<SubGroup> ord64 = new ArrayList<>();
        for (SubGroup sg : subs) {
            if (IntStream.range(1, sg.order()).allMatch(i -> sg.order(i) == 2)) {
                FixBS zeroMap = new FixBS(64);
                for (int el : sg.arr()) {
                    zeroMap.set(pg.permutation(el)[0]);
                }
                if (zeroMap.cardinality() == 64) {
                    ord64.add(sg);
                }
            }
        }
        if (ord64.size() > 1) {
            System.out.println(ord64.size() + " for " + tg.order());
            int cnt = 0;
            for (SubGroup sg : ord64) {
                System.out.println(sg.isNormal());
                Path p = Path.of("/home/ihromant/workspace/math-utils/src/test/resources/gr" + (cnt++) + ".txt");
                Files.writeString(p, Arrays.deepToString(Arrays.stream(sg.arr()).mapToObj(pg::permutation).toArray(int[][]::new)));
            }
        }
    }

    private static int flip(int a) {
        return (a & 21) << 1 | (a & 42) >>> 1;
    }

    private FixBS normalizer(PermutationGroup large, PermutationGroup small) {
        FixBS result = new FixBS(large.order());
        ex: for (int g = 0; g < large.order(); g++) {
            int[] permLarge = large.permutation(g);
            int[] invPermLarge = large.permutation(large.inv(g));
            for (int h = 0; h < small.order(); h++) {
                int[] permSmall = small.permutation(h);
                int[] combined = PermutationGroup.comb(permLarge, PermutationGroup.comb(permSmall, invPermLarge));
                if (!small.contains(combined)) {
                    continue ex;
                }
            }
            result.set(g);
        }
        return result;
    }

    @Test
    public void testExistingGroupable() throws IOException {
        ObjectMapper om = new ObjectMapper();
        List<String> lns = Files.readAllLines(Path.of("/home/ihromant/maths/g-spaces/final/4-64/large.txt"));
        lns.stream().parallel().forEach(l -> {
            Liner lnr = new Liner(om.readValue(l.substring(l.indexOf("[[")), int[][].class));
            if (lnr.graphData().autCount() == 64 || lnr.graphData().autCount() > 20_000_000) {
                return;
            }
            PermutationGroup pg = new PermutationGroup(lnr.graphData().automorphisms());
            if (pg.order() > 10000) {
                System.out.println(pg.order());
            }
            TableGroup tg = pg.asTable();
            Map<Integer, List<SubGroup>> conj = tg.subsByConjugation();
            List<SubGroup> subs = conj.get(64);
            List<SubGroup> ord64 = new ArrayList<>();
            for (SubGroup sg : subs) {
                if (IntStream.range(1, sg.order()).allMatch(i -> sg.order(i) == 2)) {
                    FixBS zeroMap = new FixBS(64);
                    for (int el : sg.arr()) {
                        zeroMap.set(pg.permutation(el)[0]);
                    }
                    if (zeroMap.cardinality() == 64) {
                        ord64.add(sg);
                    }
                }
            }
            if (ord64.size() > 1) {
                System.out.println(ord64.size() + " for " + tg.order());
                for (SubGroup sg : ord64) {
                    System.out.println(sg.isNormal());
                }
            }
        });
    }

    @Test
    public void checkConjugated() {
        GeneralLinear gl = new GeneralLinear(3, new GaloisField(4));
        int[][] glPerm = new int[gl.order()][64];
        IntStream.range(0, gl.order()).parallel().forEach(i -> {
            for (int vec = 0; vec < 64; vec++) {
                glPerm[i][vec] = gl.mulVec(i, vec);
            }
        });
        int[][] flipPerm = new int[][]{IntStream.range(0, 64).toArray(), IntStream.range(0, 64).map(AssumptionTest::flip).toArray()};
        int[][] transPerm = IntStream.range(0, 64).mapToObj(i -> IntStream.range(0, 64).map(j -> i ^ j).toArray()).toArray(int[][]::new);
        int[][] comb = new int[glPerm.length * flipPerm.length * transPerm.length][];
        IntStream.range(0, glPerm.length).forEach(glIdx -> {
            for (int flIdx = 0; flIdx < flipPerm.length; flIdx++) {
                for (int trIdx = 0; trIdx < transPerm.length; trIdx++) {
                    comb[glIdx * flipPerm.length * transPerm.length + flIdx * transPerm.length + trIdx] =
                            PermutationGroup.comb(PermutationGroup.comb(glPerm[glIdx], flipPerm[flIdx]), transPerm[trIdx]);
                }
            }
        });
        System.out.println(comb.length);
        ObjectMapper om = new ObjectMapper();
        PermutationGroup pg1 = new PermutationGroup(om.readValue(Path.of("/home/ihromant/workspace/math-utils/src/test/resources/gr1.txt"), int[][].class));
        PermutationGroup pg2 = new PermutationGroup(om.readValue(Path.of("/home/ihromant/workspace/math-utils/src/test/resources/gr2.txt"), int[][].class));
        List<PermutationGroup.Wrap> g1List = IntStream.range(0, pg1.order()).mapToObj(i -> new PermutationGroup.Wrap(pg1.permutation(i))).toList();
        Set<PermutationGroup.Wrap> g2Set = new HashSet<>(IntStream.range(0, pg2.order()).mapToObj(i -> new PermutationGroup.Wrap(pg2.permutation(i))).toList());
        System.out.println("Conjugated " + Arrays.stream(comb).anyMatch(perm -> {
            int[] inv = new int[perm.length];
            for (int i = 0; i < perm.length; i++) {
                inv[perm[i]] = i;
            }
            return g1List.stream().allMatch(w -> g2Set.contains(new PermutationGroup.Wrap(PermutationGroup.comb(PermutationGroup.comb(inv, w.arr()), perm))));
        }));
    }
}
