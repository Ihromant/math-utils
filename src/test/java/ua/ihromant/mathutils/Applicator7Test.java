package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.g.GSpace1;
import ua.ihromant.mathutils.g.NSState;
import ua.ihromant.mathutils.g.OrbitFilter;
import ua.ihromant.mathutils.g.State1;
import ua.ihromant.mathutils.group.GapGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Applicator7Test {
    @Test
    public void generateAll() throws IOException {
        int gs = 39;
        int k = 6;
        int[] orbits = new int[]{1, 13, 13, 39};
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            System.out.println("Reading SmallGroup(" + gs + "," + j + ")");
            GapGroup gg = GroupIndex.gapGroup(gs, j);
            Group group = gg.group();
            List<int[][]> configs = ApplicatorTest.configs(group, orbits);
            for (int[][] config : configs) {
                GSpace1 space;
                try {
                    space = new GSpace1(k, group, false, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                State1[] stab = getStabilized(space);
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
                Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                    Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                if (stab.length == 0) {
                    continue;
                }
                List<List<State1>> init = new ArrayList<>();
                JNauty.instance().cliques(g, 1, v, a -> {
                    FixBS arr = new FixBS(a);
                    List<State1> states = new ArrayList<>();
                    FixBS diffSet = new FixBS(space.diffLength());
                    for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                        State1 st = stab[i];
                        states.add(st);
                        diffSet.or(st.diffSet());
                    }
                    int card = diffSet.cardinality();
                    if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                        return;
                    }
                    if (card == space.diffLength()) {
                        System.out.println(states.stream().map(State1::block).toList());
                        return;
                    }
                    init.add(states);
                });
                System.out.println("Init " + init.size());
                AtomicInteger ai = new AtomicInteger();
                init.parallelStream().forEach(states -> {
                    int dc = space.diffLength();
                    OrbitFilter of = space.emptyOf();
                    FixBS diffSet = new FixBS(space.diffLength());
                    for (State1 st : states) {
                        st.updateFilter(of, space);
                        diffSet.or(st.diffSet());
                        dc = dc - st.diffSet().cardinality();
                    }
                    int nc = dc / k / (k - 1);
                    if (nc == 0) {
                        fCons.accept(states.toArray(State1[]::new), new NSState[0]);
                        return;
                    }
                    int nextOrbit = of.currOrbit(v);
                    int snd = of.filters()[nextOrbit].nextClearBit(0);
                    NSState in = new NSState(new int[]{space.oBeg(nextOrbit)}, diffSet, of).acceptElem(space, snd);
                    searchDesigns(space, new NSState[]{in}, nst -> {
                        if (nst.length < nc) {
                            return false;
                        }
                        fCons.accept(states.toArray(State1[]::new), nst);
                        return true;
                    });
                    int val = ai.incrementAndGet();
                    if (val % 1000 == 0) {
                        System.out.println(val);
                    }
                });
            }
        }
    }

    @Test
    public void generateByOne() throws IOException {
        int gs = 147;
        int k = 4;
        int[] orbits = new int[]{49};
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            System.out.println("Reading SmallGroup(" + gs + "," + j + ")");
            GapGroup gg = GroupIndex.gapGroup(gs, j);
            Group group = gg.group();
            List<int[][]> configs = ApplicatorTest.configs(group, orbits);
            for (int[][] config : configs) {
                GSpace1 space;
                try {
                    space = new GSpace1(k, group, false, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                State1[] stab = getStabilized(space);
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: "
                        + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
                Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                    Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                if (stab.length == 0) {
                    continue;
                }
                JNauty.instance().cliques(g, 1, space.v(), a -> {
                    FixBS arr = new FixBS(a);
                    List<State1> states = new ArrayList<>();
                    FixBS diffSet = new FixBS(space.diffLength());
                    OrbitFilter of = space.emptyOf();
                    for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                        State1 st = stab[i];
                        states.add(st);
                        diffSet.or(st.diffSet());
                        st.updateFilter(of, space);
                    }
                    int card = diffSet.cardinality();
                    if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                        return;
                    }
                    int nc = (space.diffLength() - card) / k / (k - 1);
                    if (nc == 0) {
                        fCons.accept(states.toArray(State1[]::new), new NSState[0]);
                        return;
                    }
                    System.out.println("Begin");
                    int nextOrbit = of.currOrbit(v);
                    int snd = of.filters()[nextOrbit].nextClearBit(0);
                    NSState in = new NSState(new int[]{space.oBeg(nextOrbit)}, diffSet, of).acceptElem(space, snd);
                    searchDesigns(space, new NSState[]{in}, nst -> {
                        if (nst.length < nc) {
                            return false;
                        }
                        fCons.accept(states.toArray(State1[]::new), nst);
                        return true;
                    });
                    System.out.println("Done");
                });
            }
        }
    }

    private static void searchDesigns(GSpace1 space, NSState[] currDesign, Predicate<NSState[]> cons) {
        int v = space.v();
        int li = currDesign.length - 1;
        NSState last = currDesign[li];
        OrbitFilter of = last.of();
        int[] block = last.block();
        int bl = block.length;
        if (bl == space.k()) {
            if (cons.test(currDesign)) {
                return;
            }
            int nextOrbit = of.currOrbit(v);
            int snd = of.filters()[nextOrbit].nextClearBit(0);
            NSState st = new NSState(new int[]{space.oBeg(nextOrbit)}, last.diffSet(), of).acceptElem(space, snd);
            NSState[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = st;
            searchDesigns(space, nextDesign, cons);
        } else {
            FixBS ftr = of.filters()[space.orbIdx(block[0])];
            int prev = block[bl - 1];
            for (int el = ftr.nextClearBit(prev + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                NSState nextState = last.acceptElem(space, el);
                if (nextState != null) {
                    currDesign[li] = nextState;
                    searchDesigns(space, currDesign, cons);
                }
            }
        }
    }

    private static void searchDesignsMin(GSpace1 space, NSState[] currDesign, Predicate<NSState[]> cons) {
        int v = space.v();
        int li = currDesign.length - 1;
        NSState last = currDesign[li];
        OrbitFilter of = last.of();
        int[] block = last.block();
        int bl = block.length;
        if (bl == space.k()) {
            if (cons.test(currDesign)) {
                return;
            }
            int nextOrbit = of.currOrbit(v);
            int snd = of.filters()[nextOrbit].nextClearBit(0);
            NSState st = new NSState(new int[]{space.oBeg(nextOrbit)}, last.diffSet(), of).acceptElem(space, snd);
            NSState[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = st;
            searchDesigns(space, nextDesign, cons);
        } else {
            FixBS ftr = of.filters()[space.orbIdx(block[0])];
            int prev = block[bl - 1];
            for (int el = ftr.nextClearBit(prev + 1); el >= 0 && el < v; el = ftr.nextClearBit(el + 1)) {
                NSState nextState = last.acceptElem(space, el);
                if (nextState != null && space.minimal(FixBS.of(v, nextState.block()))) {
                    currDesign[li] = nextState;
                    searchDesignsMin(space, currDesign, cons);
                }
            }
        }
    }

    @Test
    public void testTrivial() throws IOException {
        Group group = GroupIndex.group(39, 1);
        int k = 7;
        GSpace1 space = new GSpace1(k, group, false, 3, 1, 1);
        int v = space.v();
        FixBS evenDiffs = space.evenDiffs();
        if (!evenDiffs.isEmpty()) {
            throw new IllegalStateException();
        }
        Consumer<NSState[]> fCons = nst -> {
            Liner l = new Liner(space.v(), Arrays.stream(nst).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
        };
        if (space.diffLength() % (k * k - k) != 0) {
            throw new IllegalStateException();
        }
        int nc = space.diffLength() / k / (k - 1);
        OrbitFilter of = space.emptyOf();
        int nextOrbit = of.currOrbit(v);
        int snd = of.filters()[nextOrbit].nextClearBit(0);
        NSState in = new NSState(new int[]{space.oBeg(nextOrbit)}, new FixBS(space.diffLength()), of).acceptElem(space, snd);
        searchDesignsMin(space, new NSState[]{in}, nst -> {
            if (nst.length < nc) {
                return false;
            }
            fCons.accept(nst);
            return true;
        });
    }

    @Test
    public void trivialByGraph() throws IOException {
        Group group = GroupIndex.group(39, 1);
        int k = 6;
        GSpace1 space = new GSpace1(k, group, false, 3, 1, 1);
        FixBS evenDiffs = space.evenDiffs();
        if (!evenDiffs.isEmpty()) {
            throw new IllegalStateException();
        }
        Map<FixBS, NSState> states = new HashMap<>();
        Predicate<NSState[]> cons = (nst) -> {
            states.putIfAbsent(nst[0].diffSet(), nst[0]);
            return true;
        };
        for (int fst : space.oBeg()) {
            OrbitFilter whiteList = space.emptyOf();
            NSState nst = new NSState(new int[]{fst}, new FixBS(space.diffLength()), whiteList);
            searchDesigns(space, new NSState[]{nst}, cons);
        }
        System.out.println(states.size());
    }

    private static State1[] getStabilized(GSpace1 sp) {
        int k = sp.k();
        Group table = sp.group();
        List<SubGroup> sgs = table.subGroups();
        Map<FixBS, State1> states = new ConcurrentHashMap<>();
        for (SubGroup sg : sgs) {
            if (sg.order() == 1) {
                continue;
            }
            List<int[]> cosets = cosets(sp, sg, k);
            int[] initial = IntStream.range(0, cosets.size()).filter(i -> Arrays.stream(sp.oBeg())
                    .anyMatch(ob -> Arrays.binarySearch(cosets.get(i), ob) >= 0)).toArray();
            Consumer<List<int[]>> cons = a -> {
                int[] block = a.stream().flatMapToInt(Arrays::stream).toArray();
                State1 st = State1.fromBlockWithStab(sp, block, sg.elems());
                if (st != null) {
                    states.putIfAbsent(st.diffSet(), st);
                }
            };
            for (int i : initial) {
                List<int[]> res = new ArrayList<>();
                int[] fst = cosets.get(i);
                res.add(fst);
                if (fst.length >= k) {
                    if (fst.length == k) {
                        cons.accept(res);
                    }
                    continue;
                }
                IntStream.range(i + 1, cosets.size()).parallel().forEach(j -> {
                    List<int[]> rs = new ArrayList<>(res);
                    int[] snd = cosets.get(j);
                    rs.add(snd);
                    findStab(cosets, rs, j + 1, fst.length + snd.length, k, cons);
                });
            }
        }
        return states.values().toArray(State1[]::new);
    }

    private static void findStab(List<int[]> cosets, List<int[]> arr, int from, int sz, int k, Consumer<List<int[]>> cons) {
        if (sz == k) {
            cons.accept(arr);
            return;
        }
        if (sz > k) {
            return;
        }
        for (int i = from; i < cosets.size(); i++) {
            int[] cos = cosets.get(i);
            arr.addLast(cos);
            findStab(cosets, arr, i + 1, sz + cos.length, k, cons);
            arr.removeLast();
        }
    }

    private static List<int[]> cosets(GSpace1 sp, SubGroup sg, int k) {
        List<int[]> result = new ArrayList<>();
        ex: for (int x = 0; x < sp.v(); x++) {
            FixBS coset = new FixBS(sp.v());
            for (int g : sg.arr()) {
                int app = sp.apply(g, x);
                if (app < x) {
                    continue ex;
                }
                coset.set(app);
                if (coset.cardinality() > k) {
                    continue ex;
                }
            }
            result.add(coset.toArray());
        }
        return result;
    }
}
