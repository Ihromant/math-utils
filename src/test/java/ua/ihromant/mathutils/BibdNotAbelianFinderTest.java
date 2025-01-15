package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BibdNotAbelianFinderTest {
    @Test
    public void testLeft() {
        Group g = new SemiDirectProduct(new CyclicGroup(9), new CyclicGroup(3)).asTable();
        int v = g.order();
        int k = 3;
        System.out.println(g.name() + " " + v + " " + k);
        Group tg = g.asTable();
        Map<FixBS, Set<ArrPairs>> map = new HashMap<>();
        Consumer<int[]> cons = arr -> {
            Map<Integer, ArrPairs> components = new HashMap<>();
            for (int mul = 0; mul < v; mul++) {
                int[] applied = applyLeft(arr, mul, tg);
                int[] pairs = pairs(applied, v);
                FixBS bs = FixBS.of(v * v, pairs);
                for (int pr : pairs) {
                    ArrPairs prs = components.get(pr);
                    if (prs != null && !prs.pairs.equals(bs)) {
                        return;
                    }
                }
                ArrPairs res = new ArrPairs(FixBS.of(v, applied), bs);
                for (int pr : pairs) {
                    components.put(pr, res);
                }
            }
            Set<ArrPairs> aps = new HashSet<>(components.values());
            if (map.containsKey(aps.iterator().next().arr)) {
                return;
            }
            for (ArrPairs ap : components.values()) {
                map.put(ap.arr, aps);
            }
        };
        blocks(new int[k], v, 0, 0, cons);
        System.out.println(map.size());
        List<Set<ArrPairs>> prs = new ArrayList<>(new HashSet<>(map.values()));
        System.out.println(prs.size());
        Comp[] components = prs.stream().map(sap -> {
            FixBS res = new FixBS(v * v);
            int card = 0;
            for (ArrPairs ap : sap) {
                res.or(ap.pairs);
                card = card + ap.pairs.cardinality();
            }
            return new Comp(res, card);
        }).toArray(Comp[]::new);
        System.out.println(components.length);
        List<Liner> liners = new ArrayList<>();
        calculate(components, v, 0, 0, new FixBS(v * v), new FixBS(components.length), fbs -> {
            int[][] ars = fbs.stream().boxed().flatMap(i -> prs.get(i).stream().map(pr -> pr.arr().stream().toArray())).toArray(int[][]::new);
            liners.add(new Liner(v, ars));
        });
        System.out.println("Non processed liners " + liners.size());
        Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        liners.stream().parallel().forEach(l -> {
            FixBS canon = l.getCanonicalOld();
            if (unique.putIfAbsent(canon, l) == null) {
                System.out.println(Arrays.deepToString(l.lines()));
            }
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println(unique.size());
    }

    @Test
    public void testConjugation() {
        Group g = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3)).asTable();
        int v = g.order();
        int k = 3;
        System.out.println(g.name() + " " + v + " " + k);
        Group tg = g.asTable();
        Map<FixBS, Set<ArrPairs>> map = new HashMap<>();
        Consumer<int[]> cons = arr -> {
            Map<Integer, ArrPairs> components = new HashMap<>();
            for (int mul = 0; mul < v; mul++) {
                for (int mul1 = 0; mul1 < v; mul1++) {
                    int[] applied = applyConjugation(arr, mul, mul1, tg);
                    int[] pairs = pairs(applied, v);
                    FixBS bs = FixBS.of(v * v, pairs);
                    for (int pr : pairs) {
                        ArrPairs prs = components.get(pr);
                        if (prs != null && !prs.pairs.equals(bs)) {
                            return;
                        }
                    }
                    ArrPairs res = new ArrPairs(FixBS.of(v, applied), bs);
                    for (int pr : pairs) {
                        components.put(pr, res);
                    }
                }
            }
            Set<ArrPairs> aps = new HashSet<>(components.values());
            if (map.containsKey(aps.iterator().next().arr)) {
                return;
            }
            for (ArrPairs ap : components.values()) {
                map.put(ap.arr, aps);
            }
        };
        blocks(new int[k], v, 0, 0, cons);
        System.out.println(map.size());
        List<Set<ArrPairs>> prs = new ArrayList<>(new HashSet<>(map.values()));
        System.out.println(prs.size());
        Comp[] components = prs.stream().map(sap -> {
            FixBS res = new FixBS(v * v);
            int card = 0;
            for (ArrPairs ap : sap) {
                res.or(ap.pairs);
                card = card + ap.pairs.cardinality();
            }
            return new Comp(res, card);
        }).toArray(Comp[]::new);
        System.out.println(components.length);
        List<Liner> liners = new ArrayList<>();
        calculate(components, v, 0, 0, new FixBS(v * v), new FixBS(components.length), fbs -> {
            int[][] ars = fbs.stream().boxed().flatMap(i -> prs.get(i).stream().map(pr -> pr.arr().stream().toArray())).toArray(int[][]::new);
            liners.add(new Liner(v, ars));
        });
        System.out.println("Non processed liners " + liners.size());
        Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        liners.stream().parallel().forEach(l -> {
            FixBS canon = l.getCanonicalOld();
            if (unique.putIfAbsent(canon, l) == null) {
                System.out.println(Arrays.deepToString(l.lines()));
            }
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println(unique.size());
    }

    private static void blocks(int[] curr, int v, int from, int idx, Consumer<int[]> cons) {
        if (idx == curr.length) {
            cons.accept(curr.clone());
            return;
        }
        for (int i = from; i < v; i++) {
            curr[idx] = i;
            blocks(curr, v, i + 1, idx + 1, cons);
        }
    }

    private static void calculate(Comp[] components, int v, int currCard, int from, FixBS union, FixBS curr, Consumer<FixBS> cons) {
        if (currCard == v * (v - 1)) {
            cons.accept(curr);
            return;
        }
        for (int i = from; i < components.length; i++) {
            Comp c = components[i];
            if (c.pairs.intersects(union)) {
                continue;
            }
            FixBS newCurr = curr.copy();
            newCurr.set(i);
            calculate(components, v, currCard + c.card, i + 1, union.union(c.pairs), newCurr, cons);
        }
    }

    private record Comp(FixBS pairs, int card) {}

    private record ArrPairs(FixBS arr, FixBS pairs) {}

    private static int[] pairs(int[] arr, int v) {
        int[] res = new int[arr.length * (arr.length - 1)];
        int cnt = 0;
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                res[cnt++] = arr[i] * v + arr[j];
                res[cnt++] = arr[j] * v + arr[i];
            }
        }
        return res;
    }

    private static int[] applyLeft(int[] arr, int mul, Group group) {
        int[] res = new int[arr.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = group.op(mul, arr[i]);
        }
        Arrays.sort(res);
        return res;
    }

    public int[] applyConjugation(int[] arr, int a, int b, Group group) {
        int[] res = new int[arr.length];
        for (int i = 0; i < res.length; i++) {
            res[i] = group.op(group.op(group.inv(a), arr[i]), b);
        }
        Arrays.sort(res);
        return res;
    }
}
