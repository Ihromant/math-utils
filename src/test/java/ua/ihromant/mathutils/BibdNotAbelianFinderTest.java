package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BibdNotAbelianFinderTest {
    @Test
    public void testLeft() {
        Group g = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        int v = g.order() + 1;
        int k = 4;
        System.out.println(g.name() + " " + v + " " + k);
        Applicator app = new LeftApplicator(g.asTable());
        findDesigns(app, v, k);
    }

    private static void findDesigns(Applicator app, int v, int k) {
        Set<Set<ArrPairs>> set = ConcurrentHashMap.newKeySet();
        Consumer<int[]> cons = arr -> {
            Map<Integer, ArrPairs> components = new HashMap<>();
            for (int mul = 0; mul < app.size(); mul++) {
                int[] applied = app.apply(arr, mul);
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
            set.add(aps);
        };
        app.blocks(v, k, cons);
        System.out.println(set.size());
        Comp[] components = set.stream().map(sap -> {
            FixBS res = new FixBS(v * v);
            int card = 0;
            for (ArrPairs ap : sap) {
                res.or(ap.pairs);
                card = card + ap.pairs.cardinality();
            }
            return new Comp(res, card, sap);
        }).toArray(Comp[]::new);
        Arrays.parallelSort(components, Comparator.<Comp, FixBS>comparing(c -> c.pairs).reversed());
        int[] order = calcOrder(v, components);
        System.out.println(components.length);
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        FixBS empty = empty(v);
        IntStream.range(order[1], order[2]).parallel().forEach(i -> {
            Comp comp = components[i];
            calculate(components, order, v, v + comp.card, empty.union(comp.pairs), FixBS.of(components.length, i), fbs -> {
                int[][] ars = fbs.stream().boxed().flatMap(j -> components[j].set().stream().map(pr -> pr.arr().stream().toArray())).toArray(int[][]::new);
                Liner l = new Liner(v, ars);
                liners.add(l);
                System.out.println(l.hyperbolicFreq() + " " + Arrays.deepToString(l.lines()));
            });
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        processUniqueLiners(liners);
    }

    private static FixBS empty(int v) {
        FixBS res = new FixBS(v * v);
        for (int i = 0; i < v; i++) {
            res.set(v * i + i);
        }
        return res;
    }

    private static void processUniqueLiners(List<Liner> liners) {
        System.out.println("Non processed liners " + liners.size());
        Map<FixBS, Liner> unique = new ConcurrentHashMap<>();
        AtomicInteger ai = new AtomicInteger();
        liners.stream().parallel().forEach(l -> {
            FixBS canon = l.getCanonicalOld();
            if (unique.putIfAbsent(canon, l) == null) {
                System.out.println(l.autCountOld() + " " + Arrays.deepToString(l.lines()));
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
        Group g = new SemiDirectProduct(new CyclicProduct(3, 3), new CyclicGroup(3));
        int v = g.order();
        int k = 3;
        System.out.println(g.name() + " " + v + " " + k);
        Group tg = g.asTable();
        Set<Set<ArrPairs>> set = ConcurrentHashMap.newKeySet();
        Consumer<int[]> cons = arr -> {
            Map<Integer, ArrPairs> components = new HashMap<>();
            for (int mul = 0; mul < g.order(); mul++) {
                for (int mul1 = 0; mul1 < g.order(); mul1++) {
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
            set.add(aps);
        };
        IntStream.range(1, v).parallel().forEach(i -> {
            int[] curr = new int[k];
            curr[1] = i;
            blocks(curr, v, i + 1, 2, cons);
        });
        System.out.println(set.size());
        Comp[] components = set.stream().map(sap -> {
            FixBS res = new FixBS(v * v);
            int card = 0;
            for (ArrPairs ap : sap) {
                res.or(ap.pairs);
                card = card + ap.pairs.cardinality();
            }
            return new Comp(res, card, sap);
        }).toArray(Comp[]::new);
        Arrays.parallelSort(components, Comparator.<Comp, FixBS>comparing(c -> c.pairs).reversed());
        int[] order = calcOrder(v, components);
        System.out.println(components.length);
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        FixBS empty = empty(v);
        IntStream.range(order[1], order[2]).parallel().forEach(i -> {
            Comp comp = components[i];
            calculate(components, order, v, v + comp.card, empty.union(comp.pairs), FixBS.of(components.length, i), fbs -> {
                int[][] ars = fbs.stream().boxed().flatMap(j -> components[j].set().stream().map(pr -> pr.arr().stream().toArray())).toArray(int[][]::new);
                Liner l = new Liner(v, ars);
                liners.add(l);
            });
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        processUniqueLiners(liners);
    }

    private static int[] calcOrder(int v, Comp[] comps) {
        int[] res = new int[v * v];
        for (int i = 1; i < res.length; i++) {
            int prev = res[i - 1];
            FixBS top = FixBS.of(v * v, i - 1, v * v - 1);
            res[i] = -Arrays.binarySearch(comps, prev, comps.length, new Comp(top, 0, null), Comparator.comparing(Comp::pairs).reversed()) - 1;
        }
        return res;
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

    private static void calculate(Comp[] components, int[] order, int v, int currCard, FixBS union, FixBS curr, Consumer<FixBS> cons) {
        if (currCard == v * v) {
            cons.accept(curr);
            return;
        }
        int hole = union.nextClearBit(0);
        for (int i = order[hole]; i < order[hole + 1]; i++) {
            Comp c = components[i];
            if (c.pairs.intersects(union)) {
                continue;
            }
            FixBS newCurr = curr.copy();
            newCurr.set(i);
            calculate(components, order, v, currCard + c.card, union.union(c.pairs), newCurr, cons);
        }
    }

    private record Comp(FixBS pairs, int card, Set<ArrPairs> set) {}

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

    private interface Applicator {
        int apply(int el, int mul);
        int size();

        default int[] apply(int[] arr, int mul) {
            int[] res = new int[arr.length];
            for (int i = 0; i < res.length; i++) {
                res[i] = apply(arr[i], mul);
            }
            Arrays.sort(res);
            return res;
        }

        void blocks(int v, int k, Consumer<int[]> cons);
    }

    private static class LeftApplicator implements Applicator {
        private final Group gr;
        private final int order;

        private LeftApplicator(Group gr) {
            this.gr = gr;
            this.order = gr.order();
        }

        @Override
        public int apply(int el, int mul) {
            return el >= order ? el : gr.op(mul, el);
        }

        @Override
        public int size() {
            return order;
        }

        @Override
        public void blocks(int v, int k, Consumer<int[]> cons) {
            IntStream.range(1, v).parallel().forEach(i -> {
                int[] curr = new int[k];
                curr[1] = i;
                BibdNotAbelianFinderTest.blocks(curr, v, i + 1, 2, cons);
            });
        }
    }

    public int[] applyConjugation(int[] arr, int a, int b, Group group) {
        int[] res = new int[arr.length];
        for (int i = 0; i < res.length; i++) {
            int el = arr[i];
            res[i] = el >= group.order() ? el : group.op(group.op(group.inv(a), el), b);
        }
        Arrays.sort(res);
        return res;
    }

    private static class CustomApplicator implements Applicator {
        private final Group gr = new CyclicGroup(7).asTable();
        private final int sz = gr.order();
        private final int cnt = 25 / sz;

        @Override
        public int apply(int el, int mul) {
            int bs = el / sz;
            if (bs >= cnt) {
                return el;
            }
            int el1 = el % sz;
            int ap = gr.op(mul, el1);
            return bs * sz + ap;
        }

        @Override
        public int size() {
            return sz;
        }

        @Override
        public void blocks(int v, int k, Consumer<int[]> cons) {
            IntStream.range(1, v - k + 1).parallel().forEach(i -> {
                int[] curr = new int[k];
                curr[0] = i;
                BibdNotAbelianFinderTest.blocks(curr, v, i + 1, 1, cons);
            });
        }
    }

    private static class MillsApplicator implements Applicator {
        private final Group gr;
        private final int fOrd;
        private final int lOrd;
        private final int rOrd;
        private final int fCap;
        private final int lCap;

        public MillsApplicator(int v, Group full, Group left, Group right) {
            this.gr = full.asTable();
            this.fOrd = gr.order();
            this.lOrd = left.order();
            this.rOrd = right.order();
            this.fCap = v / fOrd * fOrd;
            this.lCap = (v - fCap) / lOrd * lOrd + fCap;
        }

        @Override
        public int apply(int el, int mul) {
            if (el >= lCap) {
                return el;
            }
            if (el < fCap) {
                int cff = el / fOrd;
                int el1 = el % fOrd;
                return cff * fOrd + gr.op(mul, el1);
            } else {
                int el1 = el - fCap;
                int bs = el1 / lOrd;
                el1 = el1 % lOrd * rOrd;
                int ap = gr.op(mul, el1);
                return fCap + bs * lOrd + ap / rOrd;
            }
        }

        @Override
        public int size() {
            return fOrd;
        }

        @Override
        public void blocks(int v, int k, Consumer<int[]> cons) {
            IntStream.concat(IntStream.range(0, fCap / fOrd).map(i -> i * fOrd),
                    IntStream.rangeClosed(0, (lCap - fCap) / lOrd).map(i -> fCap + i * lOrd)).parallel().forEach(fst ->
                    IntStream.range(fst + 1, v).parallel().forEach(snd -> {
                        int[] curr = new int[k];
                        curr[0] = fst;
                        curr[1] = snd;
                        BibdNotAbelianFinderTest.blocks(curr, v, snd + 1, 2, cons);
                    }));
        }
    }

    @Test
    public void testApplicator() {
        int v = 66;
        int k = 6;
        Group left = new CyclicGroup(13);
        CyclicGroup right = new CyclicGroup(3);
        Applicator app = new MillsApplicator(v, new SemiDirectProduct(left, right), left, right);
        findDesigns(app, v, k);
    }
}
