package ua.ihromant.mathutils.field;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.PartialLiner;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LinearSpaceTest {
    @Test
    public void test() {
        int p = 3;
        int n = 6;
        LinearSpace sp = new LinearSpace(p, n);
        int pow = (int) Math.pow(p, n);
        Map<BitSet, Integer> planes = new HashMap<>();
        for (int i = 1; i < pow; i++) {
            int a = sp.convert(i);
            for (int j = 1; j < pow; j++) {
                int b = sp.convert(j);
                if (a == b || a == sp.neg(b)) {
                    continue;
                }
                planes.putIfAbsent(sp.hull(a, b), planes.size());
            }
        }
        System.out.println(planes.size());
        Map<BitSet, Integer> hyperCubes = new ConcurrentHashMap<>();
        for (Map.Entry<BitSet, Integer> e : planes.entrySet()) {
            hyperCubes.put(sp.orthogonal(e.getKey()), e.getValue());
        }
        System.out.println(hyperCubes.size());
        BitSet a = planes.keySet().iterator().next();
        BitSet b = planes.keySet().stream().filter(pl -> !a.intersects(pl)).findFirst().orElseThrow();
        BitSet abHull = sp.hull(a, b);
        Set<PartialLiner> liners = ConcurrentHashMap.newKeySet();
        AtomicInteger cnt = new AtomicInteger();
        planes.keySet().stream().filter(pl -> !abHull.intersects(pl)).parallel().forEach(c -> {
            BitSet acHull = sp.hull(a, c);
            BitSet bcHull = sp.hull(b, c);
            for (BitSet d : planes.keySet()) {
                if (abHull.intersects(d) || acHull.intersects(d) || bcHull.intersects(d)) {
                    continue;
                }
                BitSet adHull = sp.hull(a, d);
                BitSet bdHull = sp.hull(b, d);
                BitSet cdHull = sp.hull(c, d);
                for (BitSet e : planes.keySet()) {
                    if (abHull.intersects(e) || acHull.intersects(e) || bcHull.intersects(e)
                            || adHull.intersects(e) || bdHull.intersects(e) || cdHull.intersects(e)) {
                        continue;
                    }
                    Set<BitSet> usedPoints = new HashSet<>(List.of(a, b, c, d, e));
                    Set<BitSet> newPoints = new HashSet<>(List.of(a, b, c, d, e));
                    Map<Integer, BitSet> structure = new HashMap<>();
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
                        int[] list = structure.values().stream().reduce(new BitSet(), (f, s) -> {
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

    private static Map<Integer, BitSet> checkPlaneStruct(Map<Integer, BitSet> structure, Set<BitSet> newPoints, Set<BitSet> usedPoints, Map<BitSet, Integer> planes, Map<BitSet, Integer> hyperCubes, LinearSpace sp) {
        while (!newPoints.isEmpty()) {
            usedPoints.addAll(newPoints);
            Set<BitSet> nextPoints = new HashSet<>();
            for (BitSet x : newPoints) {
                for (BitSet y : usedPoints) {
                    if (planes.get(x) >= planes.get(y)) {
                        continue;
                    }
                    BitSet xy = sp.hull(x, y);
                    Integer xyIdx = hyperCubes.get(xy);
                    if (xyIdx == null) {
                        return null;
                    }
                    for (BitSet z : usedPoints) {
                        if (planes.get(x) >= planes.get(z) || planes.get(y) >= planes.get(z) || xy.intersects(z)) {
                            continue;
                        }
                        BitSet xz = sp.hull(x, z);
                        Integer xzIdx = hyperCubes.get(xz);
                        if (xzIdx == null) {
                            return null;
                        }
                        BitSet yz = sp.hull(y, z);
                        Integer yzIdx = hyperCubes.get(yz);
                        if (yzIdx == null) {
                            return null;
                        }
                        for (BitSet w : usedPoints) {
                            if (planes.get(x) >= planes.get(w) || planes.get(y) >= planes.get(w) || planes.get(z) >= planes.get(w)
                                || xy.intersects(w) || xz.intersects(w) || yz.intersects(w)) {
                                continue;
                            }
                            BitSet xw = sp.hull(x, w);
                            Integer xwIdx = hyperCubes.get(xw);
                            if (xwIdx == null) {
                                return null;
                            }
                            BitSet yw = sp.hull(y, w);
                            Integer ywIdx = hyperCubes.get(yw);
                            if (ywIdx == null) {
                                return null;
                            }
                            BitSet zw = sp.hull(z, w);
                            Integer zwIdx = hyperCubes.get(zw);
                            if (zwIdx == null) {
                                return null;
                            }
                            BitSet xyZw = inter(xy, zw);
                            Integer xyZwIdx = planes.get(xyZw);
                            if (xyZwIdx == null) {
                                return null;
                            }
                            if (!usedPoints.contains(xyZw)) {
                                nextPoints.add(xyZw);
                            }
                            enhance(structure, xyIdx, xyZwIdx);
                            enhance(structure, zwIdx, xyZwIdx);
                            BitSet xzYw = inter(xz, yw);
                            Integer xzYwIdx = planes.get(xzYw);
                            if (xzYwIdx == null) {
                                return null;
                            }
                            if (!usedPoints.contains(xzYw)) {
                                nextPoints.add(xzYw);
                            }
                            enhance(structure, xzIdx, xzYwIdx);
                            enhance(structure, ywIdx, xzYwIdx);
                            BitSet xwYz = inter(xw, yz);
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

    private static void enhance(Map<Integer, BitSet> map, int line, int... pts) {
        BitSet val = map.get(line);
        if (val != null) {
            for (int pt : pts) {
                val.set(pt);
            }
        } else {
            map.put(line, of(pts));
        }
    }

    private static BitSet inter(BitSet a, BitSet b) {
        BitSet res = (BitSet) a.clone();
        res.and(b);
        return res;
    }

    private static BitSet of(int... vals) {
        BitSet bs = new BitSet();
        for (int val : vals) {
            bs.set(val);
        }
        return bs;
    }
}
