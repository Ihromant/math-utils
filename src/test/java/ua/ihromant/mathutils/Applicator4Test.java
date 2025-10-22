package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace1;
import ua.ihromant.mathutils.g.OrbitFilter;
import ua.ihromant.mathutils.g.State1;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.GroupProduct;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.group.TableGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;

public class Applicator4Test {
    @Test
    public void tst() throws IOException {
        int k = 11;
        Group g = GroupIndex.group(819, 6);
        Group group = new GroupProduct(new CyclicGroup(11), g);
        GSpace1 space = new GSpace1(k, group, false,
                new SubGroup(group, FixBS.of(group.order(), g.subGroups().stream().filter(sg -> sg.order() == 9).findAny().orElseThrow().elems().toArray())));
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        List<State1[]> singles = new ArrayList<>();
        BiPredicate<State1[], Integer> sCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        int sz = space.differences().length;
        State1 state = new State1(FixBS.of(v, 0), FixBS.of(group.order(), 0), new FixBS(sz), new IntList[sz], 1);
        searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, sCons);
        System.out.println("Singles size: " + singles.size());
    }

    @Test
    public void logDesigns() throws IOException {
        int k = 4;
        Group group = new CyclicProduct(5, 5);
        GSpace1 space = new GSpace1(k, group, true, 1);
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        List<State1[]> singles = new ArrayList<>();
        BiPredicate<State1[], Integer> sCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        State1 state = space.forInitial(0, 1);
        searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, sCons);
        System.out.println("Singles size: " + singles.size());
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State1[], Integer> fCons = (arr, orbit) -> {
            if (orbit < space.orbitCount()) {
                return false;
            }
            ai.incrementAndGet();
            Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State1::block).toList());
            return true;
        };
        singles.stream().parallel().forEach(tuple -> {
            State1[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            OrbitFilter newFilter = space.emptyOf();
            for (State1 st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    @Test
    public void twoStageMul1() throws IOException {
        int k = 6;
        int gs = 570;
        int mt = 2;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            Map<Integer, List<SubGroup>> subs = group.groupedSubGroups();
            String groupId = GroupIndex.identify(group);
            for (int t = 0; t < subs.getOrDefault(mt, List.of()).size(); t++) {
                //for (int u = 0; u < subs.getOrDefault(8, List.of()).size(); u++) {
                    GSpace1 space;
                    try {
                        space = new GSpace1(k, group, true, new int[][]{{gs, 0}, {mt, t}});
                    } catch (IllegalArgumentException e) {
                        System.out.println("Not empty");
                        continue;
                    }
                    int v = space.v();
                    System.out.println(groupId + " " + space.v() + " " + k + " auths: " + space.authLength());
                    int sqr = v * v;
                    List<State1[]> singles = new ArrayList<>();
                    BiPredicate<State1[], Integer> fCons = (arr, uu) -> {
                        singles.add(arr);
                        return true;
                    };
                    State1 state = space.forInitial(0, 1);
                    searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, fCons);
                    System.out.println("Singles size: " + singles.size());
                    List<State1[]> pairs = new ArrayList<>();
                    List<State1[]> sync = Collections.synchronizedList(pairs);
                    BiPredicate<State1[], Integer> sCons = (arr, uu) -> {
                        if (arr.length < 2) {
                            return false;
                        }
                        if (space.twoMinimal(arr)) {
                            sync.add(arr);
                        }
                        return true;
                    };
                    AtomicInteger cnt = new AtomicInteger();
                    singles.stream().parallel().forEach(tuple -> {
                        State1[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                        OrbitFilter newFilter = space.emptyOf();
                        for (State1 st : pr) {
                            st.updateFilter(newFilter, space);
                        }
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), sCons);
                        int vl = cnt.incrementAndGet();
                        if (vl % 10 == 0) {
                            System.out.println(vl);
                        }
                    });
                    System.out.println("Pairs " + pairs.size());
                    AtomicInteger ai = new AtomicInteger();
                    cnt.set(0);
                    BiPredicate<State1[], Integer> tCons = (arr, orbit) -> {
                        if (orbit < space.orbitCount()) {
                            return false;
                        }
                        if (!space.minimal(arr)) {
                            return true;
                        }
                        ai.incrementAndGet();
                        Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                        System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State1::block).toList());
                        return true;
                    };
                    pairs.stream().parallel().forEach(tuple -> {
                        State1[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                        OrbitFilter newFilter = space.emptyOf();
                        for (State1 st : pr) {
                            st.updateFilter(newFilter, space);
                        }
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), tCons);
                        int vl = cnt.incrementAndGet();
                        if (vl % 100 == 0) {
                            System.out.println(vl);
                        }
                    });
                    System.out.println("Results " + ai);
                //}
            }
        }
    }

    @Test
    public void twoStage() throws IOException {
        int k = 6;
        Group semi = new SemiDirectProduct(new CyclicGroup(19), new CyclicGroup(3));
        int[][] auth = semi.auth();
        TableGroup group = semi.asTable();
        group.setCachedAuth(auth);
        GSpace1 space = new GSpace1(k, group, true, 3, 1);
        int v = space.v();
        System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
        List<State1[]> singles = new ArrayList<>();
        BiPredicate<State1[], Integer> fCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        State1 state = space.forInitial(0, 1);
        searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, fCons);
        System.out.println("Singles size: " + singles.size());
        List<State1[]> pairs = new ArrayList<>();
        List<State1[]> sync = Collections.synchronizedList(pairs);
        BiPredicate<State1[], Integer> sCons = (arr, uu) -> {
            if (arr.length < 2) {
                return false;
            }
            if (space.twoMinimal(arr)) {
                sync.add(arr);
            }
            return true;
        };
        AtomicInteger cnt = new AtomicInteger();
        singles.stream().parallel().forEach(tuple -> {
            State1[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            OrbitFilter newFilter = space.emptyOf();
            for (State1 st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), sCons);
            int vl = cnt.incrementAndGet();
            if (vl % 10 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Pairs " + pairs.size());
        AtomicInteger ai = new AtomicInteger();
        cnt.set(0);
        BiPredicate<State1[], Integer> tCons = (arr, orbit) -> {
            if (orbit < space.orbitCount()) {
                return false;
            }
            if (!space.minimal(arr)) {
                return true;
            }
            ai.incrementAndGet();
            Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State1::block).toList());
            return true;
        };
        pairs.stream().parallel().forEach(tuple -> {
            State1[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            OrbitFilter newFilter = space.emptyOf();
            for (State1 st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    private static void searchDesigns(GSpace1 space, OrbitFilter filter, State1[] currDesign, State1 state, int orbit, BiPredicate<State1[], Integer> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            State1[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            OrbitFilter nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            int nextOrbit = nextFilter.currOrbit(v);
            if (cons.test(nextDesign, nextOrbit)) {
                return;
            }
            State1 nextState = space.forInitial(space.oBeg(nextOrbit), nextFilter.filters()[nextOrbit].nextClearBit(0));
            searchDesigns(space, nextFilter, nextDesign, nextState, nextOrbit, cons);
        } else {
            FixBS ftr = filter.filters()[orbit];
            for (int el = ftr.nextClearBit(state.block().previousSetBit(v) + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                State1 nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, orbit, cons);
                }
            }
        }
    }

    private static void searchDesignsFirst(GSpace1 space, OrbitFilter filter, State1[] currDesign, State1 state, int orbit, BiPredicate<State1[], Integer> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            State1[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            OrbitFilter nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            int nextOrbit = nextFilter.currOrbit(v);
            if (cons.test(nextDesign, nextOrbit)) {
                return;
            }
            State1 nextState = space.forInitial(space.oBeg(nextOrbit), nextFilter.filters()[nextOrbit].nextClearBit(0));
            searchDesignsFirst(space, nextFilter, nextDesign, nextState, nextOrbit, cons);
        } else {
            FixBS ftr = filter.filters()[orbit];
            for (int el = ftr.nextClearBit(state.block().previousSetBit(v) + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                State1 nextState = state.acceptElem(space, filter, el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, filter, currDesign, nextState, orbit, cons);
                }
            }
        }
    }
}
