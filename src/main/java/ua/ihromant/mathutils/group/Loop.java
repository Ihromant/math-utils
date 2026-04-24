package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public interface Loop {
    int op(int a, int b);

    int inv(int a);

    int order();

    default int order(int a) {
        int pow = 0;
        int counter = 0;
        do {
            counter++;
            pow = op(a, pow);
        } while (pow != 0);
        return counter;
    }

    default int expOrder(int a) {
        int eul = Combinatorics.euler(order());
        int res = a;
        for (int i = 1; i < eul + 1; i++) {
            res = mul(res, a);
            if (res == a) {
                return i;
            }
        }
        return -1;
    }

    String name();

    String elementName(int a);

    default int mul(int a, int cff) {
        int result = 0;
        for (int i = 0; i < cff; i++) {
            result = op(a, result);
        }
        return result;
    }

    default int exponent(int base, int power) {
        int result = 1;
        while (power > 0) {
            if (power % 2 == 1) {
                result = mul(result, base);
            }
            base = mul(base, base);
            power = power / 2;
        }
        return result;
    }

    default int[] squareRoots(int from) {
        return IntStream.range(0, order()).filter(i -> op(i, i) == from).toArray();
    }

    default IntStream elements() {
        return IntStream.range(0, order());
    }

    default TableGroup asTable() {
        int order = order();
        int[][] table = new int[order][order];
        if (order > 1000) {
            IntStream.range(0, order).parallel().forEach(i -> {
                for (int j = 0; j < order; j++) {
                    table[i][j] = op(i, j);
                }
            });
        } else {
            for (int i = 0; i < order; i++) {
                for (int j = 0; j < order; j++) {
                    table[i][j] = op(i, j);
                }
            }
        }
        return new TableGroup(name(), table);
    }

    default int conjugate(int fst, int snd) {
        return op(op(snd, fst), inv(snd));
    }

    default List<FixBS> conjugationClasses() {
        int order = order();
        QuickFind qf = new QuickFind(order);
        for (int x = 0; x < order; x++) {
            for (int g = 0; g < order; g++) {
                int conj = op(inv(g), op(x, g));
                if (conj < x) {
                    qf.union(x, conj);
                    break;
                }
            }
        }
        return qf.components();
    }

    default boolean isCommutative() {
        return IntStream.range(1, order()).allMatch(i -> IntStream.range(1, order()).allMatch(j -> op(i, j) == op(j, i)));
    }

    default FixBS cycle(int from) {
        FixBS result = new FixBS(order());
        int el = from;
        do {
            el = op(el, from);
            result.set(el);
        } while (el != from);
        return result;
    }

    default int[][] innerAuth() {
        Set<int[]> result = new TreeSet<>(Combinatorics::compareArr);
        for (int conj = 0; conj < order(); conj++) {
            int[] arr = new int[order()];
            int inv = inv(conj);
            for (int el = 0; el < order(); el++) {
                arr[el] = op(op(inv, el), conj);
            }
            result.add(arr);
        }
        return result.toArray(int[][]::new);
    }

    default int[][] auth() {
        int[] gens = gens();
        List<int[]> result = find(gens);
        int[][] res = result.toArray(int[][]::new);
        Arrays.parallelSort(res, Combinatorics::compareArr);
        return res;
    }

    private List<int[]> find(int[] gens) {
        int order = order();
        TreeMap<Integer, FixBS> byOrders = new TreeMap<>();
        for (int i = 0; i < order; i++) {
            int ord = order(i);
            byOrders.computeIfAbsent(ord, _ -> new FixBS(order)).set(i);
        }
        int[][] bOrd = new int[byOrders.lastKey() + 1][0];
        for (Map.Entry<Integer, FixBS> e : byOrders.entrySet()) {
            bOrd[e.getKey()] = e.getValue().toArray();
        }
        List<int[]> result = Collections.synchronizedList(new ArrayList<>());
        int ord = order(gens[0]);
        Arrays.stream(bOrd[ord]).parallel().forEach(v -> {
            PartialMap pm = new PartialMap(FixBS.of(order, 0), new int[order]);
            for (int i = 1; i < ord; i++) {
                int key = mul(gens[0], i);
                int val = mul(v, i);
                pm.keys.set(key);
                pm.map[key] = val;
            }
            find(gens, result, pm, 1, bOrd, order);
        });
        return result;
    }

    private List<int[]> findAlt(int[] gens) {
        int order = order();
        TreeMap<Integer, FixBS> byOrders = new TreeMap<>();
        for (int i = 0; i < order; i++) {
            int ord = order(i);
            byOrders.computeIfAbsent(ord, _ -> new FixBS(order)).set(i);
        }
        int[][] bOrd = new int[byOrders.lastKey() + 1][0];
        for (Map.Entry<Integer, FixBS> e : byOrders.entrySet()) {
            bOrd[e.getKey()] = e.getValue().toArray();
        }
        List<int[]> result = Collections.synchronizedList(new ArrayList<>());
        int ord = order(gens[0]);
        Arrays.stream(bOrd[ord]).parallel().forEach(v -> {
            PartMap pm = new PartMap(FixBS.of(order, 0), FixBS.of(order, 0), new int[order]);
            for (int i = 1; i < ord; i++) {
                int key = mul(gens[0], i);
                int val = mul(v, i);
                pm.set(key, val);
            }
            findAlt(gens, pm, 1, bOrd, result::add);
        });
        return result;
    }

    private int[] gensAlt() {
        AtomicReference<int[]> ar = new AtomicReference<>();
        ar.set(IntStream.range(0, order()).toArray());
        IntStream.range(1, order()).parallel().forEach(gen -> {
            IntList list = new IntList(order());
            list.add(gen);
            FixBS cycle = cycle(gen);
            if (cycle.nextSetBit(1) < gen) {
                return;
            }
            gens(new int[]{gen}, cycle(gen), ar);
        });
        return ar.get();
    }

    private int[] gens() {
        AtomicReference<int[]> ar = new AtomicReference<>();
        ar.set(IntStream.range(0, order()).toArray());
        IntStream.range(1, order()).parallel().forEach(i -> {
            IntList list = new IntList(order());
            list.add(i);
            gens(2, list, cycle(i), ar);
        });
        return ar.get();
    }

    private void gens(int cap, IntList genList, FixBS currGroup, AtomicReference<int[]> currGens) {
        int ord = order();
        int sz = genList.size();
        if (currGroup.isFull(ord)) {
            currGens.updateAndGet(old -> old.length > sz ? genList.toArray() : old);
        }
        for (int gen = currGroup.nextClearBit(genList.get(sz - 1)); gen >= 0 && gen < ord; gen = currGroup.nextClearBit(gen + 1)) {
            int len = currGens.get().length;
            if (cap >= len || sz + 1 >= len) {
                return;
            }
            IntList nextGenList = genList.copy();
            nextGenList.add(gen);
            FixBS nextGroup = currGroup.copy();
            FixBS additional = cycle(gen);
            additional.andNot(currGroup);
            do {
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, ord)).isEmpty());
            gens(cap, nextGenList, nextGroup, currGens);
        }
    }

    private FixBS additional(FixBS currGroup, FixBS addition, int order) {
        FixBS result = new FixBS(order);
        for (int x = currGroup.nextSetBit(0); x >= 0; x = currGroup.nextSetBit(x + 1)) {
            for (int y = addition.nextSetBit(0); y >= 0; y = addition.nextSetBit(y + 1)) {
                result.set(op(x, y));
                result.set(op(y, x));
            }
        }
        result.andNot(currGroup);
        return result;
    }

    private PartialMap additional(PartialMap currMap, PartialMap addition, int order) {
        PartialMap result = new PartialMap(new FixBS(order), new int[order]);
        for (int x = currMap.keys.nextSetBit(0); x >= 0; x = currMap.keys.nextSetBit(x + 1)) {
            for (int y = addition.keys.nextSetBit(0); y >= 0; y = addition.keys.nextSetBit(y + 1)) {
                int key = op(x, y);
                int val = op(currMap.map[x], addition.map[y]);
                if (result.keys.get(key)) {
                    if (result.map[key] != val) {
                        return null;
                    }
                } else {
                    result.keys.set(key);
                    result.map[key] = val;
                }
            }
        }
        result.keys.andNot(currMap.keys);
        return result;
    }

    private void find(int[] gens, List<int[]> result, PartialMap currMap, int idx, int[][] byOrders, int order) {
        if (gens.length == idx) {
            if (TernaryAutomorphisms.isBijective(currMap.map())) {
                result.add(currMap.map());
            }
            return;
        }
        int gen = gens[idx];
        int ord = order(gen);
        ex: for (int suitVal : byOrders[ord]) {
            PartialMap nextGroup = currMap.copy();
            PartialMap additional = new PartialMap(new FixBS(order), new int[order]);
            for (int i = 1; i < ord; i++) {
                int key = mul(gen, i);
                int val = mul(suitVal, i);
                if (currMap.keys.get(key)) {
                    if (currMap.map[key] != val) {
                        continue ex;
                    } else {
                        continue;
                    }
                }
                additional.keys.set(key);
                additional.map[key] = val;
            }
            do {
                nextGroup.keys.or(additional.keys);
                for (int k = additional.keys.nextSetBit(0); k >= 0; k = additional.keys.nextSetBit(k + 1)) {
                    nextGroup.map[k] = additional.map[k];
                }
            } while ((additional = additional(nextGroup, additional, order)) != null && !additional.keys.isEmpty());
            if (additional != null) {
                find(gens, result, nextGroup, idx + 1, byOrders, order);
            }
        }
    }

    private void findAlt(int[] gens, PartMap currMap, int idx, int[][] byOrders, Consumer<int[]> cons) {
        if (gens.length == idx) {
            cons.accept(currMap.map());
            return;
        }
        int gen = gens[idx];
        int ord = order(gen);
        ex: for (int suitVal : byOrders[ord]) {
            if (currMap.vals.get(suitVal)) {
                continue;
            }
            PartMap nextMap = currMap.copy();
            nextMap.set(gen, suitVal);
            boolean added;
            do {
                added = false;
                for (int a : nextMap.keys.toArray()) {
                    for (int i = 0; i <= idx; i++) {
                        int b = gens[i];
                        int ab = op(a, b);
                        int mapAB = op(nextMap.map[a], nextMap.map[b]);
                        Boolean res = nextMap.set(ab, mapAB);
                        if (res == null) {
                            continue ex;
                        }
                        added = added || res;
                        int ba = op(b, a);
                        int mapBA = op(nextMap.map[b], nextMap.map[a]);
                        res = nextMap.set(ba, mapBA);
                        if (res == null) {
                            continue ex;
                        }
                        added = added || res;
                    }
                }
            } while (added);
            findAlt(gens, nextMap, idx + 1, byOrders, cons);
        }
    }

    private void gens(int[] gens, FixBS currGroup, AtomicReference<int[]> currGens) {
        int ord = order();
        int sz = gens.length;
        if (currGroup.isFull(ord)) {
            currGens.updateAndGet(old -> old.length > sz ? gens : old);
        }
        ex: for (int gen = currGroup.nextClearBit(gens[sz - 1]); gen >= 0 && gen < ord; gen = currGroup.nextClearBit(gen + 1)) {
            int len = currGens.get().length;
            if (sz + 1 >= len) {
                return;
            }
            int[] nextGens = Arrays.copyOf(gens, sz + 1);
            nextGens[sz] = gen;
            FixBS nextGroup = currGroup.copy();
            boolean added;
            do {
                added = false;
                for (int a : nextGroup.toArray()) {
                    for (int b : nextGens) {
                        int ab = op(a, b);
                        if (!currGroup.get(ab) && ab < gen) {
                            continue ex;
                        }
                        if (!nextGroup.get(ab)) {
                            added = true;
                            nextGroup.set(ab);
                        }
                        int ba = op(b, a);
                        if (!currGroup.get(ba) && ba < gen) {
                            continue ex;
                        }
                        if (!nextGroup.get(ba)) {
                            added = true;
                            nextGroup.set(ba);
                        }
                    }
                }
            } while (added);
            gens(nextGens, nextGroup, currGens);
        }
    }

    record PartialMap(FixBS keys, int[] map) {
        public PartialMap copy() {
            return new PartialMap(keys.copy(), map.clone());
        }
    }

    record PartMap(FixBS keys, FixBS vals, int[] map) {
        public PartMap copy() {
            return new PartMap(keys.copy(), vals.copy(), map.clone());
        }

        public Boolean set(int key, int val) {
            if (keys.get(key)) {
                return map[key] == val ? Boolean.FALSE : null;
            } else {
                if (vals.get(val)) {
                    return null;
                }
                keys.set(key);
                vals.set(val);
                map[key] = val;
                return Boolean.TRUE;
            }
        }
    }
}
