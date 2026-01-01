package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.Combinatorics;
import ua.ihromant.mathutils.IntList;
import ua.ihromant.mathutils.auto.TernaryAutomorphisms;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public interface Group extends Loop {
    default List<SubGroup> subGroups() {
        List<SubGroup> result = new ArrayList<>();
        int order = order();
        FixBS all = new FixBS(order);
        all.set(0, order);
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(new SubGroup(this, init));
        find(init, 0, order, result::add);
        return result;
    }

    private void find(FixBS currGroup, int prev, int order, Consumer<SubGroup> cons) {
        ex: for (int gen = currGroup.nextClearBit(prev + 1); gen >= 0 && gen < order; gen = currGroup.nextClearBit(gen + 1)) {
            FixBS nextGroup = currGroup.copy();
            FixBS additional = cycle(gen);
            additional.andNot(currGroup);
            do {
                if (additional.nextSetBit(0) < gen) {
                    continue ex;
                }
                nextGroup.or(additional);
            } while (!(additional = additional(nextGroup, additional, order)).isEmpty());
            cons.accept(new SubGroup(this, nextGroup));
            find(nextGroup, gen, order, cons);
        }
    }

    default Map<Integer, List<SubGroup>> groupedSubGroups() {
        Map<Integer, List<SubGroup>> result = new ConcurrentHashMap<>();
        int order = order();
        result.put(1, new ArrayList<>(List.of(new SubGroup(this, FixBS.of(order, 0)))));
        int[][] auths = auth();
        IntStream.range(1, order).parallel().forEach(i -> {
            FixBS cycle = cycle(i);
            if (cycle.nextSetBit(1) < i) {
                return;
            }
            for (int[] arr : auths) {
                FixBS oElems = new FixBS(order);
                for (int el = cycle.nextSetBit(0); el >= 0; el = cycle.nextSetBit(el + 1)) {
                    oElems.set(arr[el]);
                }
                if (oElems.compareTo(cycle) < 0) {
                    return;
                }
            }
            result.computeIfAbsent(cycle.cardinality(), _ -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new SubGroup(this, cycle));
            find(cycle, i, order, sg -> {
                FixBS elems = sg.elems();
                for (int[] arr : auths) {
                    FixBS oElems = new FixBS(order);
                    for (int el = elems.nextSetBit(0); el >= 0; el = elems.nextSetBit(el + 1)) {
                        oElems.set(arr[el]);
                    }
                    if (oElems.compareTo(elems) < 0) {
                        return;
                    }
                }
                result.computeIfAbsent(elems.cardinality(), _ -> Collections.synchronizedList(new ArrayList<>())).add(sg);
            });
        });
        result.values().forEach(l -> l.sort(Comparator.comparing(SubGroup::elems)));
        return result;
    }

    default Map<Integer, List<SubGroup>> subsByConjugation() {
        Map<Integer, List<SubGroup>> result = new ConcurrentHashMap<>();
        int order = order();
        result.put(1, new ArrayList<>(List.of(new SubGroup(this, FixBS.of(order, 0)))));
        IntStream.range(1, order).parallel().forEach(i -> {
            FixBS cycle = cycle(i);
            if (cycle.nextSetBit(1) < i) {
                return;
            }
            for (int conj = 0; conj < order(); conj++) {
                int inv = inv(conj);
                FixBS oElems = new FixBS(order);
                for (int el = cycle.nextSetBit(0); el >= 0; el = cycle.nextSetBit(el + 1)) {
                    oElems.set(op(inv, op(el, conj)));
                }
                if (oElems.compareTo(cycle) < 0) {
                    return;
                }
            }
            result.computeIfAbsent(cycle.cardinality(), _ -> Collections.synchronizedList(new ArrayList<>()))
                    .add(new SubGroup(this, cycle));
            find(cycle, i, order, sg -> {
                FixBS elems = sg.elems();
                for (int conj = 0; conj < order(); conj++) {
                    int inv = inv(conj);
                    FixBS oElems = new FixBS(order);
                    for (int el = elems.nextSetBit(0); el >= 0; el = elems.nextSetBit(el + 1)) {
                        oElems.set(op(inv, op(el, conj)));
                    }
                    if (oElems.compareTo(elems) < 0) {
                        return;
                    }
                }
                result.computeIfAbsent(elems.cardinality(), _ -> Collections.synchronizedList(new ArrayList<>())).add(sg);
            });
        });
        result.values().forEach(l -> l.sort(Comparator.comparing(SubGroup::elems)));
        return result;
    }

    default SubGroup closure(FixBS from) {
        FixBS result = new FixBS(order());
        FixBS additional = from.copy();
        do {
            result.or(additional);
        } while (!(additional = additional(result, additional, order())).isEmpty());
        return new SubGroup(this, result);
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

    record PartialMap(FixBS keys, int[] map) {
        public PartialMap copy() {
            return new PartialMap(keys.copy(), map.clone());
        }
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
}
