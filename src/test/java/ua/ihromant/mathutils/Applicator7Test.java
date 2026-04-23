package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.g.GSpace1;
import ua.ihromant.mathutils.g.NSState;
import ua.ihromant.mathutils.g.OrbitFilter;
import ua.ihromant.mathutils.g.State1;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.GapGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
                if (stab.length == 0) {
                    continue;
                }
                Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                    Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
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
                        fCons.accept(states.toArray(State1[]::new), new NSState[0]);
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
    public void generateSpecific() throws IOException {
        int k = 6;
        int[] orbits = new int[]{1, 13, 13, 39};
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
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
            System.out.println(GroupIndex.identify(group) + " " + v + " " + k + " configs: "
                    + Arrays.deepToString(config) + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
            if (stab.length == 0) {
                continue;
            }
            Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
            BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                        Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                        + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
            };
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
                    fCons.accept(states.toArray(State1[]::new), new NSState[0]);
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
                    System.out.println("Begin " + nc + " " + states.stream().map(State1::block).toList());
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

    @Test
    public void generateWithAuth() throws IOException {
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
                    space = new GSpace1(k, group, true, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                State1[] stab = Arrays.stream(getStabilized(space)).parallel().map(bl -> bl.minimizeBlock(space)).toArray(State1[]::new);
                Arrays.parallelSort(stab, Comparator.comparing(State1::block));
                int[] idxes = IntStream.range(0, stab.length).filter(i -> space.minimal(stab[i].block())).toArray();
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: " + Arrays.deepToString(config) + " auths: " + space.authLength()
                        + " minimals: " + idxes.length + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
                BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                    Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                List<List<State1>> init = Collections.synchronizedList(new ArrayList<>());
                Arrays.stream(idxes).parallel().forEach(idx -> {
                    State1 fstBlock = stab[idx];
                    List<State1> initList = List.of(fstBlock);
                    if ((space.diffLength() - fstBlock.diffSet().cardinality()) % (k * k - k) == 0 && evenDiffs.diff(fstBlock.diffSet()).isEmpty()) {
                        init.add(initList);
                    }
                    State1[] filteredStab = Arrays.stream(stab, idx, stab.length).filter(st -> !st.diffSet().intersects(fstBlock.diffSet())).toArray(State1[]::new);
                    System.out.println("For block with idx " + idx + " filtered size " + filteredStab.length);
                    Graph g = Graph.by(filteredStab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                    JNauty.instance().cliques(g, 1, v, a -> {
                        FixBS arr = new FixBS(a);
                        List<State1> states = new ArrayList<>(initList);
                        FixBS diffSet = fstBlock.diffSet().copy();
                        for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                            State1 st = filteredStab[i];
                            states.add(st);
                            diffSet.or(st.diffSet());
                        }
                        int card = diffSet.cardinality();
                        if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                            return;
                        }
                        if (card == space.diffLength()) {
                            fCons.accept(states.toArray(State1[]::new), new NSState[0]);
                            return;
                        }
                        init.add(states);
                    });
                    System.out.println("Block with idx " + idx + " done");
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
    public void generateByOneWithAuth() throws IOException {
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
                    space = new GSpace1(k, group, true, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                FixBS evenDiffs = space.evenDiffs();
                State1[] stab = Arrays.stream(getStabilized(space)).parallel().map(bl -> bl.minimizeBlock(space)).toArray(State1[]::new);
                Arrays.parallelSort(stab, Comparator.comparing(State1::block));
                int[] idxes = IntStream.range(0, stab.length).filter(i -> space.minimal(stab[i].block())).toArray();
                System.out.println(GroupIndex.identify(gg) + " " + v + " " + k + " configs: " + Arrays.deepToString(config) + " auths: " + space.authLength()
                        + " minimals: " + idxes.length + " stab: " + stab.length + " diffs: " + evenDiffs.cardinality());
                BiConsumer<State1[], NSState[]> fCons = (sts, nst) -> {
                    Liner l = new Liner(space.v(), Stream.concat(Arrays.stream(sts).flatMap(st -> space.blocks(st.block())),
                            Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.stream(sts).map(State1::block).toList()
                            + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
                };
                Arrays.stream(idxes).parallel().forEach(idx -> {
                    State1 fstBlock = stab[idx];
                    List<State1> initList = List.of(fstBlock);
                    State1[] filteredStab = Arrays.stream(stab, idx, stab.length).filter(st -> !st.diffSet().intersects(fstBlock.diffSet())).toArray(State1[]::new);
                    System.out.println("For block with idx " + idx + " filtered size " + filteredStab.length);
                    Graph g = Graph.by(filteredStab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
                    JNauty.instance().cliques(g, 1, v, a -> {
                        FixBS arr = new FixBS(a);
                        List<State1> states = new ArrayList<>(initList);
                        FixBS diffSet = fstBlock.diffSet().copy();
                        OrbitFilter of = space.emptyOf();
                        fstBlock.updateFilter(of, space);
                        for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                            State1 st = filteredStab[i];
                            states.add(st);
                            diffSet.or(st.diffSet());
                            st.updateFilter(of, space);
                        }
                        int card = diffSet.cardinality();
                        if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
                            return;
                        }
                        if (card == space.diffLength()) {
                            fCons.accept(states.toArray(State1[]::new), new NSState[0]);
                            return;
                        }
                        int nc = (space.diffLength() - card) / k / (k - 1);
                        System.out.println("Begin " + nc + " " + states.stream().map(State1::block).toList());
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
                    });
                    System.out.println("Block with idx " + idx + " done");
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
    public void testAlmostTrivial() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(287), new CyclicGroup(5));
        int k = 8;
        GSpace1 space = new GSpace1(k, group, true, 5, 1435);
        int v = space.v();
        State1 stabilized = State1.fromBlock(space, FixBS.of(v, IntStream.concat(IntStream.range(0, 7).map(i -> i * 41), IntStream.of(v - 1)).toArray()));
        FixBS evenDiffs = space.evenDiffs().diff(stabilized.diffSet());
        if (!evenDiffs.isEmpty()) {
            throw new IllegalStateException();
        }
        Consumer<NSState[]> fCons = nst -> {
            Liner l = new Liner(space.v(), Stream.concat(space.blocks(stabilized.block()),
                    Arrays.stream(nst).flatMap(st -> space.blocks(st.block()))).toArray(int[][]::new));
            System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + Arrays.deepToString(Arrays.stream(nst).map(NSState::block).toArray(int[][]::new)));
        };
        if ((space.diffLength() - stabilized.diffSet().cardinality()) % (k * k - k) != 0) {
            throw new IllegalStateException();
        }
        int nc = (space.diffLength() - stabilized.diffSet().cardinality()) / k / (k - 1);
        OrbitFilter of = space.emptyOf();
        stabilized.updateFilter(of, space);
        int nextOrbit = of.currOrbit(v);
        int snd = of.filters()[nextOrbit].nextClearBit(0);
        FixBS diffSet = new FixBS(space.diffLength());
        diffSet.or(stabilized.diffSet());
        NSState in = new NSState(new int[]{space.oBeg(nextOrbit)}, diffSet, of).acceptElem(space, snd);
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
        int v = sp.v();
        int b = v * (v - 1) / k / (k - 1);
        Group table = sp.group();
        List<SubGroup> sgs = table.subGroups();
        Map<FixBS, State1> states = new ConcurrentHashMap<>();
        for (SubGroup sg : sgs) {
            if (sg.order() == 1 || table.order() / sg.order() > b) {
                continue;
            }
            List<int[]> cosets = cosets(sp, sg, k);
            int[] initial = IntStream.range(0, cosets.size()).filter(i -> Arrays.stream(sp.oBeg())
                    .anyMatch(ob -> Arrays.binarySearch(cosets.get(i), ob) >= 0)).toArray();
            Consumer<State1> cons = st -> states.putIfAbsent(st.diffSet(), st);
            for (int i : initial) {
                int[] fst = cosets.get(i);
                if (fst.length > k) {
                    continue;
                }
                State1 st = new State1(new FixBS(v), sg.elems(), new FixBS(sp.diffLength()), new int[sp.diffLength()][0], 0);
                for (int el : fst) {
                    st = Objects.requireNonNull(st.acceptElemWithStab(sp, el));
                }
                if (st.size() == k) {
                    cons.accept(State1.fromBlockWithStab(sp, fst, sg.elems()));
                    continue;
                }
                State1 state = st;
                IntStream.range(i + 1, cosets.size()).parallel().forEach(j -> {
                    int[] snd = cosets.get(j);
                    if (state.size() + snd.length > k) {
                        return;
                    }
                    State1 nextSt = state;
                    for (int el : snd) {
                        nextSt = nextSt.acceptElemWithStab(sp, el);
                        if (nextSt == null) {
                            return;
                        }
                    }
                    findStab(cosets, sp, nextSt, j + 1, cons);
                });
            }
        }
        return states.values().toArray(State1[]::new);
    }

    private static void findStab(List<int[]> cosets, GSpace1 sp, State1 state, int from, Consumer<State1> cons) {
        int k = sp.k();
        if (state.size() == k) {
            cons.accept(state);
            return;
        }
        ex: for (int i = from; i < cosets.size(); i++) {
            int[] cos = cosets.get(i);
            if (state.size() + cos.length > k) {
                continue;
            }
            State1 nextSt = state;
            for (int el : cos) {
                nextSt = nextSt.acceptElemWithStab(sp, el);
                if (nextSt == null) {
                    continue ex;
                }
            }
            findStab(cosets, sp, nextSt, i + 1, cons);
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
            State1 st = new State1(new FixBS(sp.v()), sg.elems(), new FixBS(sp.diffLength()), new int[sp.diffLength()][0], 0);
            int[] arr = coset.toArray();
            for (int el : arr) {
                st = st.acceptElemWithStab(sp, el);
                if (st == null) {
                    continue ex;
                }
            }
            result.add(arr);
        }
        return result;
    }

    @Test
    public void generateByTwo() throws IOException {
        int k = 4;
        int clSize = 0;
        Group group = new SemiDirectProduct(new CyclicGroup(73), new CyclicGroup(3));
        GSpace1 space = new GSpace1(k, group, false, 3);
        FixBS evenDiffs = space.evenDiffs();
        State1[] stab = getStabilized(space);
        Graph g = Graph.by(stab, (a, b) -> !a.diffSet().intersects(b.diffSet()));
        if (stab.length == 0 || clSize == 0) {
            generateTwo(space, List.of(), evenDiffs);
            return;
        }
        JNauty.instance().cliques(g, clSize, clSize, a -> {
            FixBS arr = new FixBS(a);
            List<State1> states = new ArrayList<>();
            for (int i = arr.nextSetBit(0); i >= 0; i = arr.nextSetBit(i + 1)) {
                states.add(stab[i]);
            }
            generateTwo(space, states, evenDiffs);
        });
    }

    private static void generateTwo(GSpace1 space, List<State1> states, FixBS evenDiffs) {
        int k = space.k();
        int v = space.v();
        FixBS diffSet = new FixBS(space.diffLength());
        OrbitFilter of = space.emptyOf();
        for (State1 st : states) {
            diffSet.or(st.diffSet());
            st.updateFilter(of, space);
        }
        int card = diffSet.cardinality();
        if ((space.diffLength() - card) % (k * k - k) != 0 || !evenDiffs.diff(diffSet).isEmpty()) {
            return;
        }
        int nc = (space.diffLength() - card) / k / (k - 1);
        if (nc != 2) {
            throw new IllegalStateException();
        }
        System.out.println("Begin " + states.stream().map(State1::block).toList());
        int oBeg = space.oBeg(of.currOrbit(v));
        NSState in = new NSState(new int[]{oBeg}, diffSet, of);
        FixBS allowed = diffSet.copy();
        allowed.flip(0, space.diffLength());
        FixBS ftr = of.filters()[space.orbIdx(oBeg)].copy();
        ftr.flip(0, v);
        ftr.clear(0, oBeg + 1);
        int[] possible = ftr.toArray();
        Map<FixBS, int[]> blocks = new ConcurrentHashMap<>();
        IntStream.of(possible).parallel().forEach(snd -> {
            NSState[] init = new NSState[]{Objects.requireNonNull(in.acceptElem(space, snd))};
            searchDesigns(space, init, nst -> {
                NSState st = nst[0];
                blocks.putIfAbsent(st.diffSet(), st.block());
                FixBS candidate = allowed.diff(st.diffSet());
                int[] existing = blocks.get(candidate);
                if (existing != null) {
                    Liner l = new Liner(space.v(), Stream.concat(states.stream().flatMap(s -> space.blocks(s.block())),
                            Stream.of(st, new NSState(existing, null, null)).flatMap(s -> space.blocks(s.block()))).toArray(int[][]::new));
                    System.out.println(l.graphData().autCount() + " " + l.hyperbolicFreq() + " " + states.stream().map(State1::block).toList()
                            + " " + Arrays.deepToString(Stream.of(st, new NSState(existing, null, null)).map(NSState::block).toArray(int[][]::new)));
                }
                return true;
            });
        });
        System.out.println("Done");
    }
}
