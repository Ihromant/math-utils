package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.IntStream;

public interface Group {
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
        return new TableGroup(table);
    }

    default int conjugate(int fst, int snd) {
        return op(op(snd, fst), inv(snd));
    }

    default List<FixBS> conjugationClasses() {
        int order = order();
        QuickFind qf = new QuickFind(order);
        for (int g = 0; g < order; g++) {
            for (int x = 0; x < order; x++) {
                qf.union(x, op(inv(g), op(x, g)));
            }
        }
        return qf.components();
    }

    default boolean isCommutative() {
        return IntStream.range(1, order()).allMatch(i -> IntStream.range(1, order()).allMatch(j -> op(i, j) == op(j, i)));
    }

    default List<SubGroup> subGroups() {
        List<SubGroup> result = new ArrayList<>();
        int order = order();
        FixBS all = new FixBS(order);
        all.set(0, order);
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(new SubGroup(this, init));
        find(result, init, 0, order);
        return result;
    }

    private void find(List<SubGroup> result, FixBS currGroup, int prev, int order) {
        ex: for (int gen = currGroup.nextClearBit(prev + 1); gen >= 0 && gen < order; gen = currGroup.nextClearBit(gen + 1)) {
            FixBS nextGroup = currGroup.copy();
            FixBS additional = new FixBS(order);
            additional.set(gen);
            do {
                if (additional.nextSetBit(0) < gen) {
                    continue ex;
                }
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, order)).isEmpty());
            result.add(new SubGroup(this, nextGroup));
            find(result, nextGroup, gen, order);
        }
    }

    private int[] gens(IntList genList, FixBS currGroup) {
        int ord = order();
        if (currGroup.cardinality() == ord) {
            return genList.toArray();
        }
        int gen = currGroup.nextClearBit(0);
        genList.add(gen);
        FixBS nextGroup = currGroup.copy();
        FixBS additional = new FixBS(ord);
        additional.set(gen);
        do {
            nextGroup.or(additional);
        } while (!(additional = additional(nextGroup, additional, ord)).isEmpty());
        return gens(genList, nextGroup);
    }

    private FixBS additional(FixBS currGroup, FixBS addition, int order) {
        FixBS result = new FixBS(order);
        for (int x = currGroup.nextSetBit(0); x >= 0; x = currGroup.nextSetBit(x + 1)) {
            for (int y = addition.nextSetBit(0); y >= 0; y = addition.nextSetBit(y + 1)) {
                result.set(op(x, y));
            }
        }
        result.andNot(currGroup);
        return result;
    }

    default int[][] auth() {
        Set<int[]> result = new TreeSet<>(Combinatorics::compareArr);
        int order = order();
        FixBS init = FixBS.of(order, 0);
        int[] map = new int[order];
        TreeMap<Integer, FixBS> byOrders = new TreeMap<>();
        for (int i = 0; i < order; i++) {
            int ord = order(i);
            byOrders.computeIfAbsent(ord, k -> new FixBS(order)).set(i);
        }
        int[][] bOrd = new int[byOrders.lastKey() + 1][0];
        for (Map.Entry<Integer, FixBS> e : byOrders.entrySet()) {
            bOrd[e.getKey()] = e.getValue().toArray();
        }
        IntList list = new IntList(order);
        int[] gens = gens(list, init);
        find(gens, result, new PartialMap(init, map), 0, bOrd, order);
        return result.toArray(int[][]::new);
    }

    record PartialMap(FixBS keys, int[] map) {
        public PartialMap copy() {
            return new PartialMap(keys.copy(), map.clone());
        }
    }

    private void find(int[] gens, Set<int[]> result, PartialMap currMap, int idx, int[][] byOrders, int order) {
        if (gens.length == idx) {
            if (TernaryAutomorphisms.isBijective(currMap.map())) {
                result.add(currMap.map());
            }
            return;
        }
        int gen = gens[idx];
        int ord = order(gen);
        for (int suitVal : byOrders[ord]) {
            PartialMap nextGroup = currMap.copy();
            PartialMap additional = new PartialMap(new FixBS(order), new int[order]);
            additional.keys.set(gen);
            additional.map[gen] = suitVal;
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

    default boolean isSimple() {
        List<SubGroup> subGroups = subGroups();
        return subGroups.stream().allMatch(sg -> sg.order() == 1 || sg.order() == order() || !sg.isNormal());
    }
}
