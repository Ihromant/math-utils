package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.IntStream;

public class Applicator5Test {
    @Test
    public void tst() {
        Group left = new GroupProduct(new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3)), new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3)));
        Group prod = new GroupProduct(new CyclicGroup(11), left);
        SubGroup sg = left.subGroups().stream().filter(s -> s.order() == 9).findAny().orElseThrow();
        GSpace space = new GSpace(11, prod, false, new SubGroup(prod, sg.elems().copy(prod.order())));
        Map<FixBS, State> unique = new ConcurrentHashMap<>();
        System.out.println("Begin");
        IntStream.range(1, space.v()).parallel().forEach(snd -> {
            IntStream.range(snd + 1, space.v()).parallel().forEach(trd -> {
                State st = space.forInitial(0, snd, trd);
                if (st == null) {
                    return;
                }
                simpleSearch(space, st, trd, fst -> {
                    unique.putIfAbsent(fst.diffSet(), fst);
                    return true;
                });
            });
            System.out.println(snd);
        });
        System.out.println(unique.size());
    }

    private static void simpleSearch(GSpace space, State curr, int prev, Predicate<State> cons) {
        int sz = curr.size();
        if (sz == space.k()) {
            cons.test(curr);
            return;
        }
        if (sz <= 4) {
            IntStream.range(prev + 1, space.v()).parallel().forEach(nxt -> {
                State next = curr.acceptElem(space, space.emptyFilter(), nxt);
                if (next != null) {
                    simpleSearch(space, next, nxt, cons);
                }
            });
        } else {
            for (int i = prev + 1; i < space.v(); i++) {
                State next = curr.acceptElem(space, space.emptyFilter(), i);
                if (next != null) {
                    simpleSearch(space, next, i, cons);
                }
            }
        }
    }

    @Test
    public void search() throws IOException {
        Group prod = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(6, prod, false, 1, 3, 3, 39);
        Map<FixBS, State> unique = new ConcurrentHashMap<>();
        System.out.println("Begin, differences: " + space.diffLength());
        AtomicInteger ai = new AtomicInteger();
        IntStream.range(1, space.v()).parallel().forEach(snd -> {
            IntStream.range(snd + 1, space.v()).parallel().forEach(trd -> {
                State st = space.forInitial(0, snd, trd);
                if (st == null) {
                    return;
                }
                simpleSearch(space, st, trd, fst -> {
                    unique.putIfAbsent(fst.diffSet(), fst);
                    return true;
                });
            });
            int val = ai.incrementAndGet();
            if (val % 10 == 0) {
                System.out.println(val);
            }
        });
        List<State> states = new ArrayList<>(unique.values());
        states.sort(Comparator.comparing(State::diffSet));
        System.out.println(states.size());
        Predicate<Des> pr = des -> {
            if (!des.ds.isFull(space.diffLength())) {
                return false;
            }
            Liner lnr = new Liner(space.v(), des.curr.stream().flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(lnr.hyperbolicFreq() + " " + des.curr.stream().map(State::block).toList());
            return true;
        };
        FixBS[] intersecting = intersecting(states);
        find(states, intersecting, Des.empty(space.diffLength(), states.size()), pr);
    }

    private static FixBS[] intersecting(List<State> states) {
        FixBS[] intersecting = new FixBS[states.size()];
        IntStream.range(0, states.size()).parallel().forEach(i -> {
            FixBS comp = new FixBS(states.size());
            FixBS ftr = states.get(i).diffSet();
            for (int j = 0; j < states.size(); j++) {
                if (ftr.intersects(states.get(j).diffSet())) {
                    comp.set(j);
                }
            }
            intersecting[i] = comp;
        });
        return intersecting;
    }

    private static void find(List<State> states, FixBS[] intersecting, Des des, Predicate<Des> pred) {
        if (pred.test(des)) {
            return;
        }
        FixBS available = des.available;
        if (des.curr.size() < 3) {
            IntList base = new IntList(available.cardinality());
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                base.add(i);
            }
            Arrays.stream(base.toArray()).parallel().forEach(i -> {
                find(states, intersecting, des.accept(states.get(i), intersecting[i], i), pred);
            });
        } else {
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                find(states, intersecting, des.accept(states.get(i), intersecting[i], i), pred);
            }
        }
    }

    private record Des(List<State> curr, FixBS ds, FixBS available, int idx) {
        private Des accept(State state, FixBS intersecting, int idx) {
            List<State> nextCurr = new ArrayList<>(curr);
            nextCurr.add(state);
            return new Des(nextCurr, ds.union(state.diffSet()), available.diff(intersecting), idx);
        }

        private static Des empty(int dl, int statesSize) {
            FixBS available = new FixBS(statesSize);
            available.set(0, statesSize);
            return new Des(List.of(), new FixBS(dl), available, -1);
        }
    }
}
