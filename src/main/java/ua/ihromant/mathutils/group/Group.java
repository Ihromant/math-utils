package ua.ihromant.mathutils.group;

import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public interface Group extends Loop {
    default List<SubGroup> subGroups() {
        List<SubGroup> result = new ArrayList<>();
        int order = order();
        FixBS init = new FixBS(order);
        init.set(0);
        result.add(new SubGroup(this, init));
        find(init, new int[0], result::add);
        return result;
    }

    private void find(FixBS currGroup, int[] gens, Consumer<SubGroup> cons) {
        int l = gens.length;
        int last = l == 0 ? 0 : gens[l - 1];
        ex: for (int gen = currGroup.nextClearBit(last); gen >= 0 && gen < order(); gen = currGroup.nextClearBit(gen + 1)) {
            int[] nextGens = Arrays.copyOf(gens, l + 1);
            nextGens[l] = gen;
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
            cons.accept(new SubGroup(this, nextGroup));
            find(nextGroup, nextGens, cons);
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
            find(cycle, new int[]{i}, sg -> {
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
            find(cycle, new int[]{i}, sg -> {
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
        FixBS result = from.copy();
        int[] arr = from.toArray();
        boolean added;
        do {
            added = false;
            for (int a : result.toArray()) {
                for (int b : arr) {
                    int ab = op(a, b);
                    if (!result.get(ab)) {
                        added = true;
                        result.set(ab);
                    }
                    int ba = op(b, a);
                    if (!result.get(ba)) {
                        added = true;
                        result.set(ba);
                    }
                }
            }
        } while (added);
        return new SubGroup(this, result);
    }

    default boolean isSimple() {
        List<SubGroup> subGroups = subGroups();
        return subGroups.stream().allMatch(sg -> sg.order() == 1 || sg.order() == order() || !sg.isNormal());
    }

    default List<SubGroup[]> gSpaceConfigs(int... orbits) {
        List<SubGroup[]> result = new ArrayList<>();
        Map<Integer, List<SubGroup>> grouped = subGroups().stream().collect(Collectors.groupingBy(Group::order));
        grouped.values().forEach(sgs -> sgs.sort(Comparator.comparing(SubGroup::elems)));
        int[] sizes = Arrays.stream(orbits).map(i -> order() / i).toArray();
        List<int[]> auths = Arrays.asList(auth());
        gSpaceConfigs(grouped, sizes, auths, new SubGroup[sizes.length], 0, result::add);
        return result;
    }

    private void gSpaceConfigs(Map<Integer, List<SubGroup>> grouped, int[] sizes, List<int[]> auths, SubGroup[] subs, int idx, Consumer<SubGroup[]> cons) {
        if (idx == subs.length) {
            cons.accept(subs.clone());
            return;
        }
        List<SubGroup> ofSize = grouped.get(sizes[idx]);
        boolean prevSame = idx > 0 && sizes[idx - 1] == sizes[idx];
        if (ofSize.size() == 1) {
            subs[idx] = ofSize.getFirst();
            gSpaceConfigs(grouped, sizes, auths, subs, idx + 1, cons);
        } else {
            ex: for (SubGroup sub : ofSize) {
                if (prevSame && sub.elems().compareTo(subs[idx - 1].elems()) < 0) {
                    continue;
                }
                List<int[]> fixingAuths = new ArrayList<>();
                for (int[] aut : auths) {
                    FixBS next = new FixBS(order());
                    for (int el : sub.arr()) {
                        next.set(aut[el]);
                    }
                    int comp = next.compareTo(sub.elems());
                    if (comp < 0) {
                        continue ex;
                    }
                    if (comp == 0) {
                        fixingAuths.add(aut);
                    }
                }
                subs[idx] = sub;
                gSpaceConfigs(grouped, sizes, fixingAuths, subs, idx + 1, cons);
            }
        }
    }
}
