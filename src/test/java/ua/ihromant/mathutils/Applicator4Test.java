package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace1;
import ua.ihromant.mathutils.g.OrbitFilter;
import ua.ihromant.mathutils.g.State1;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;

public class Applicator4Test {
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
        searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, 1, sCons);
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
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), 0, fCons);
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
                    searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, 1, fCons);
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
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), 0, sCons);
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
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), 0, tCons);
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
        int k = 7;
        Group group = new SemiDirectProduct(new CyclicGroup(169), new CyclicGroup(3));
        GSpace1 space = new GSpace1(k, group, true, 3);
        int v = space.v();
        System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
        List<State1[]> singles = new ArrayList<>();
        BiPredicate<State1[], Integer> fCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        State1 state = space.forInitial(0, 1);
        searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, 1, fCons);
        System.out.println("Singles size: " + singles.size());
        List<State1[]> pairs = new ArrayList<>();
        List<State1[]> sync = Collections.synchronizedList(pairs);
        BiPredicate<State1[], Integer> sCons = (arr, uu) -> {
            if (arr.length < 2) {
                return false;
            }
            if (space.parMinimal(arr)) {
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
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), 0, sCons);
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
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], newFilter.currOrbit(v), 0, tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    private static void searchDesigns(GSpace1 space, OrbitFilter filter, State1[] currDesign, State1 state, int orbit, int prev, BiPredicate<State1[], Integer> cons) {
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
            int snd = nextFilter.filters()[nextOrbit].nextClearBit(0);
            State1 nextState = space.forInitial(space.oBeg(nextOrbit), snd);
            searchDesigns(space, nextFilter, nextDesign, nextState, nextOrbit, snd, cons);
        } else {
            FixBS ftr = filter.filters()[orbit];
            for (int el = ftr.nextClearBit(prev + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                State1 nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, orbit, el, cons);
                }
            }
        }
    }

    private static void searchDesignsFirst(GSpace1 space, OrbitFilter filter, State1[] currDesign, State1 state, int orbit, int prev, BiPredicate<State1[], Integer> cons) {
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
            int snd = nextFilter.filters()[nextOrbit].nextClearBit(0);
            State1 nextState = space.forInitial(space.oBeg(nextOrbit), snd);
            searchDesignsFirst(space, nextFilter, nextDesign, nextState, nextOrbit, snd, cons);
        } else {
            FixBS ftr = filter.filters()[orbit];
            for (int el = ftr.nextClearBit(prev + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                State1 nextState = state.acceptElem(space, filter, el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, filter, currDesign, nextState, orbit, el, cons);
                }
            }
        }
    }

    @Test
    public void generateInitial() throws IOException {
        int k = 6;
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        int[] comps = new int[]{1, 3, 3, 39};
        GSpace1 space = new GSpace1(k, group, true, comps);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
            List<State1[]> singles = new ArrayList<>();
            BiPredicate<State1[], Integer> sCons = (arr, _) -> {
                singles.add(arr);
                return true;
            };
            int val = 1;
            State1 state = space.forInitial(0, val);
            searchDesignsFirst(space, space.emptyOf(), new State1[0], state, 0, 0, sCons);
            System.out.println(group.name() + " Singles size: " + singles.size());
            AtomicInteger cnt = new AtomicInteger();
            BiPredicate<State1[], Integer> tCons = (arr, _) -> {
                if (arr.length == 2) {
                    if (space.parMinimal(arr)) {
                        ps.println(Arrays.stream(arr).map(st -> st.block().toString()).collect(Collectors.joining(" ")));
                    }
                    return true;
                } else {
                    return false;
                }
            };
            singles.stream().parallel().forEach(single -> {
                searchDesigns(space, space.emptyOf(), new State1[0], single[0], 0, 0, tCons);
                int vl = cnt.incrementAndGet();
                if (vl % 10 == 0) {
                    System.out.println(vl);
                }
            });
        }
    }
}
