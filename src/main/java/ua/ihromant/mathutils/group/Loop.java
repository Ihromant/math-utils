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
        return auth(2);
    }

    default int[][] auth(int genCap) {
        List<int[]> result = Collections.synchronizedList(new ArrayList<>());
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
        AtomicReference<int[]> ar = new AtomicReference<>();
        ar.set(IntStream.range(0, order()).toArray());
        IntStream.range(1, order).parallel().forEach(i -> {
            IntList list = new IntList(order);
            list.add(i);
            gens(genCap, list, cycle(i), ar);
        });
        int[] gens = ar.get();
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
        int[][] res = result.toArray(int[][]::new);
        Arrays.parallelSort(res, Combinatorics::compareArr);
        return res;
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

    record PartialMap(FixBS keys, int[] map) {
        public PartialMap copy() {
            return new PartialMap(keys.copy(), map.clone());
        }
    }
}
