package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

    default boolean isSimple() {
        List<SubGroup> subGroups = subGroups();
        return subGroups.stream().allMatch(sg -> sg.order() == 1 || sg.order() == order() || !sg.isNormal());
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
}
