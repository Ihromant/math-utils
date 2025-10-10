package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.group.TableGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicatorTest {
    @Test
    public void testApplicator() {
        Group gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace applicator = new GSpace(6, gr, false, 1, 3, 3, 39);
        assertEquals(66, applicator.v());
        for (int x = 0; x < applicator.v(); x++) {
            for (int j = 0; j < gr.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
        gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        applicator = new GSpace(6, gr, false, 1, 1);
        for (int g = 0; g < gr.order(); g++) {
            for (int x = 0; x < 2 * gr.order(); x++) {
                int app = applicator.apply(g, x);
                int expected = x < gr.order() ? gr.op(g, x) : gr.op(g, x - gr.order()) + gr.order();
                assertEquals(expected, app);
            }
        }
    }

    @Test
    public void testState() throws IOException {
        try {
            new GSpace(6, new CyclicProduct(2, 2), false, 2);
            fail();
        } catch (Exception e) {
            // ok
        }
        Group g = new CyclicGroup(21);
        GSpace space = new GSpace(7, g, false, 1);
        State state = space.forInitial(0, 3);
        assertEquals(FixBS.of(space.v(), 0, 3), state.block());
        assertEquals(FixBS.of(g.order(), 0), state.stabilizer());
        assertNull(state.acceptElem(space, space.emptyFilter(), 12));
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 6));
        FixBS bs = FixBS.of(g.order(), IntStream.range(0, 7).map(i -> i * 3).toArray());
        assertEquals(bs, state.stabilizer());
        assertEquals(bs, state.block());
        state = space.forInitial(0, 7);
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 14));
        assertNull(state.acceptElem(space, space.emptyFilter(), 1));
        g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        space = new GSpace(6, g, false, 1);
        state = space.forInitial(0, 1);
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 2));
        assertEquals(FixBS.of(space.v(), 0, 1, 2), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 3));
        assertEquals(FixBS.of(space.v(), 0, 1, 2, 3, 31, 80), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        g = GroupIndex.group(40, 4);
        space = new GSpace(6, g, false, 1, 8, 8, 8, 8, 40);
        state = space.forInitial(0, 3);
        assertEquals(FixBS.of(g.order(), 0, 3), state.stabilizer());
        assertEquals(FixBS.of(g.order(), 0, 3), state.block());
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 43));
        assertEquals(FixBS.of(g.order(), 0, 3), state.stabilizer());
        assertEquals(FixBS.of(g.order(), 0, 3, 43), state.block());
        assertNull(state.acceptElem(space, space.emptyFilter(), 48));
    }

    @Test
    public void testAutomorphisms() {
        int k = 6;
        Group g = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(k, g, true, 1, 3, 3, 39);
        assertEquals(12168, space.authLength());
        int v = space.v();
        System.out.println("Randomized test");
        IntStream.range(0, 100).parallel().forEach(uu -> {
            int[] auth = space.auth(ThreadLocalRandom.current().nextInt(space.authLength()));
            for (int a = 0; a < v; a++) {
                for (int b = a + 1; b < v; b++) {
                    for (int c = 0; c < v; c++) {
                        for (int d = c + 1; d < v; d++) {
                            int ab = a * v + b;
                            int cd = c * v + d;
                            boolean eq = space.diffIdx(ab) == space.diffIdx(cd);
                            int mapAb = auth[a] * v + auth[b];
                            int mapCd = auth[c] * v + auth[d];
                            boolean mapEq = space.diffIdx(mapAb) == space.diffIdx(mapCd);
                            assertEquals(eq, mapEq);
                        }
                    }
                }
            }
        });
        g = new CyclicGroup(48);
        GSpace space1 = new GSpace(k, g, true, 1, 1);
        assertEquals(73728, space1.authLength());
        int ov = space1.v();
        System.out.println("Randomized test");
        IntStream.range(0, 100).parallel().forEach(uu -> {
            int[] auth = space1.auth(ThreadLocalRandom.current().nextInt(space1.authLength()));
            for (int a = 0; a < ov; a++) {
                for (int b = a + 1; b < ov; b++) {
                    for (int c = 0; c < ov; c++) {
                        for (int d = c + 1; d < ov; d++) {
                            int ab = a * ov + b;
                            int cd = c * ov + d;
                            boolean eq = space1.diffIdx(ab) == space1.diffIdx(cd);
                            int mapAb = auth[a] * ov + auth[b];
                            int mapCd = auth[c] * ov + auth[d];
                            boolean mapEq = space1.diffIdx(mapAb) == space1.diffIdx(mapCd);
                            assertEquals(eq, mapEq);
                        }
                    }
                }
            }
        });
    }

    @Test
    public void testAuth() {
        int k = 5;
        Group group = new PermutationGroup(5, true);
        GSpace space = new GSpace(k, group, true, 1, 60, 60, 60, 60, 60);
        System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
        BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
            System.out.println(arr[0].block());
            return true;
        };
        int val = 1;
        State state = space.forInitial(0, val);
        searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, sCons);
        System.out.println(space.minimalBlock(FixBS.of(space.v(), 0, 1, 15, 46, 51)));
        FixBS[] blocks = {FixBS.of(space.v(), 0, 1, 15, 46, 51),
                FixBS.of(space.v(), 0, 3, 8, 11, 64),
                FixBS.of(space.v(), 0, 4, 10, 38, 52),
                FixBS.of(space.v(), 0, 12, 47, 59, 60),
                FixBS.of(space.v(), 0, 13, 30, 43, 61),
                FixBS.of(space.v(), 0, 14, 33, 55, 62),
                FixBS.of(space.v(), 0, 20, 34, 42, 50),
                FixBS.of(space.v(), 0, 27, 41, 53, 63),
                FixBS.of(space.v(), 60, 61, 62, 63, 64)};
        Liner l1 = new Liner(space.v(), Arrays.stream(blocks).flatMap(space::blocks).toArray(int[][]::new));
        System.out.println(l1.hyperbolicFreq() + " " + Arrays.toString(blocks));
        FixBS[] minimal = space.minimalBlocks(blocks);
        Liner l = new Liner(space.v(), Arrays.stream(minimal).flatMap(space::blocks).toArray(int[][]::new));
        System.out.println(l.hyperbolicFreq() + " " + Arrays.toString(minimal));
    }

    @Test
    public void logDesigns() throws IOException {
        int k = 6;
        Group group = GroupIndex.group(39, 1);
        GSpace space = new GSpace(k, group, false, 1, 3, 3, 39);
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        int sqr = v * v;
        List<State[]> singles = new ArrayList<>();
        BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        int val = 1;
        State state = space.forInitial(0, val);
        searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, sCons);
        System.out.println("Singles size: " + singles.size());
        AtomicInteger cnt = new AtomicInteger();
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
            if (!ftr.isFull(sqr)) {
                return false;
            }
            ai.incrementAndGet();
            Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        singles.stream().parallel().forEach(tuple -> {
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = space.emptyFilter().copy();
            for (State st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    @Test
    public void logSpecific() throws IOException {
        int k = 5;
        for (int j = 1; j <= 52; j++) {
            Group group = GroupIndex.group(48, j);
            Map<Integer, List<SubGroup>> sgr = group.groupedSubGroups();
            for (int t = 0; t < sgr.get(4).size(); t++) {
                int[][] conf = new int[][]{{1, 0}, {4, t}, {48, 0}};
                GSpace space = new GSpace(k, group, true, conf);
                int v = space.v();
                System.out.println(GroupIndex.identify(group) + " " + Arrays.deepToString(conf) + " " + space.v() + " " + k + " auths: " + space.authLength());
                int sqr = v * v;
                List<State[]> singles = new ArrayList<>();
                BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
                    singles.add(arr);
                    return true;
                };
                int val = 1;
                State state = space.forInitial(0, val);
                searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, sCons);
                System.out.println("Singles size: " + singles.size());
                AtomicInteger cnt = new AtomicInteger();
                AtomicInteger ai = new AtomicInteger();
                BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
                    if (!ftr.isFull(sqr)) {
                        return false;
                    }
                    if (!space.minimal(arr)) {
                        return true;
                    }
                    ai.incrementAndGet();
                    Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                    System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
                    return true;
                };
                singles.stream().parallel().forEach(tuple -> {
                    State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                    FixBS newFilter = space.emptyFilter().copy();
                    for (State st : pr) {
                        st.updateFilter(newFilter, space);
                    }
                    searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, fCons);
                    int vl = cnt.incrementAndGet();
                    if (vl % 100 == 0) {
                        System.out.println(vl);
                    }
                });
                System.out.println("Results " + ai);
            }
        }
    }

    @Test
    public void twoStage() throws IOException {
        int k = 6;
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(k, group, true, 1, 3, 3, 39);
        int v = space.v();
        System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
        int sqr = v * v;
        List<State[]> singles = new ArrayList<>();
        BiPredicate<State[], FixBS> fCons = (arr, uu) -> {
            singles.add(arr);
            return true;
        };
        int val = 1;
        State state = space.forInitial(0, val);
        searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, fCons);
        System.out.println("Singles size: " + singles.size());
        List<State[]> pairs = new ArrayList<>();
        List<State[]> sync = Collections.synchronizedList(pairs);
        BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
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
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = space.emptyFilter().copy();
            for (State st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, sCons);
            int vl = cnt.incrementAndGet();
            if (vl % 10 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Pairs " + pairs.size());
        AtomicInteger ai = new AtomicInteger();
        cnt.set(0);
        BiPredicate<State[], FixBS> tCons = (arr, ftr) -> {
            if (!ftr.isFull(sqr)) {
                return false;
            }
            if (!space.minimal(arr)) {
                return true;
            }
            ai.incrementAndGet();
            Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        pairs.stream().parallel().forEach(tuple -> {
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = space.emptyFilter().copy();
            for (State st : pr) {
                st.updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
    }

    @Test
    public void twoStageMul() throws IOException {
        int k = 8;
        int gs = 450;
        int mt = 2;
        int c = GroupIndex.groupCount(gs);
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            Map<Integer, List<SubGroup>> subs = group.groupedSubGroups();
            for (int t = 0; t < subs.getOrDefault(mt, List.of()).size(); t++) {
                //for (int u = 0; u < subs.getOrDefault(48, List.of()).size(); u++) {
                    GSpace space;
                    try {
                        space = new GSpace(k, group, true, new int[][]{{mt, t}});
                    } catch (IllegalArgumentException e) {
                        System.out.println("Not empty");
                        continue;
                    }
                    int v = space.v();
                    System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
                    int sqr = v * v;
                    List<State[]> singles = new ArrayList<>();
                    BiPredicate<State[], FixBS> fCons = (arr, uu) -> {
                        singles.add(arr);
                        return true;
                    };
                    int val = 1;
                    State state = space.forInitial(0, val);
                    searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, fCons);
                    System.out.println("Singles size: " + singles.size());
                    List<State[]> pairs = new ArrayList<>();
                    List<State[]> sync = Collections.synchronizedList(pairs);
                    BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
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
                        State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                        FixBS newFilter = space.emptyFilter().copy();
                        for (State st : pr) {
                            st.updateFilter(newFilter, space);
                        }
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, sCons);
                        int vl = cnt.incrementAndGet();
                        if (vl % 10 == 0) {
                            System.out.println(vl);
                        }
                    });
                    System.out.println("Pairs " + pairs.size());
                    AtomicInteger ai = new AtomicInteger();
                    cnt.set(0);
                    BiPredicate<State[], FixBS> tCons = (arr, ftr) -> {
                        if (!ftr.isFull(sqr)) {
                            return false;
                        }
                        if (!space.minimal(arr)) {
                            return true;
                        }
                        ai.incrementAndGet();
                        Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                        System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
                        return true;
                    };
                    pairs.stream().parallel().forEach(tuple -> {
                        State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
                        FixBS newFilter = space.emptyFilter().copy();
                        for (State st : pr) {
                            st.updateFilter(newFilter, space);
                        }
                        searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, tCons);
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

    private static void searchDesigns(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int pair = nextFilter.nextClearBit(0);
            int snd = pair % v;
            State nextState = space.forInitial(pair / v, snd);
            searchDesigns(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = filter.nextClearBit(from); pair >= 0 && pair < to; pair = filter.nextClearBit(pair + 1)) {
                int el = pair % v;
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null) {
                    searchDesigns(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }

    private static void searchDesignsFirst(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = filter.copy();
            state.updateFilter(nextFilter, space);
            if (cons.test(nextDesign, nextFilter)) {
                return;
            }
            int pair = nextFilter.nextClearBit(0);
            int snd = pair % v;
            State nextState = space.forInitial(pair / v, snd);
            searchDesignsFirst(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = filter.nextClearBit(from); pair >= 0 && pair < to; pair = filter.nextClearBit(pair + 1)) {
                int el = pair % v;
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }

    @Test
    public void generateInitial() throws IOException {
        int k = 6;
        Group group = GroupIndex.group(39, 1);
        int[] comps = new int[]{1, 3, 3, 39};
        GSpace space = new GSpace(k, group, true, comps);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
            List<State[]> singles = new ArrayList<>();
            BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
                singles.add(arr);
                return true;
            };
            int val = 1;
            State state = space.forInitial(0, val);
            searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, sCons);
            System.out.println(GroupIndex.identify(group) + " Singles size: " + singles.size());
            AtomicInteger cnt = new AtomicInteger();
            BiPredicate<State[], FixBS> tCons = (arr, uu) -> {
                if (arr.length == 2) {
                    if (space.twoMinimal(arr)) {
                        ps.println(Arrays.stream(arr).map(st -> st.block().toString()).collect(Collectors.joining(" ")));
                    }
                    return true;
                } else {
                    return false;
                }
            };
            singles.stream().parallel().forEach(single -> {
                searchDesigns(space, space.emptyFilter(), new State[0], single[0], 0, tCons);
                int vl = cnt.incrementAndGet();
                if (vl % 10 == 0) {
                    System.out.println(vl);
                }
            });
        }
    }

    @Test
    public void generateInitialSingle() throws IOException {
        int k = 9;
        Group semi = new SemiDirectProduct(new CyclicProduct(19, 19), new CyclicGroup(5));
        int[][] auth = semi.auth();
        TableGroup group = semi.asTable();
        group.setCachedAuth(auth);
        int[] comps = new int[]{5};
        GSpace space = new GSpace(k, group, true, comps);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
            BiPredicate<State[], FixBS> sCons = (arr, uu) -> {
                ps.println(arr[0].block());
                ps.flush();
                return true;
            };
            int val = 1;
            State state = space.forInitial(0, val);
            searchDesignsFirst(space, space.emptyFilter(), new State[0], state, val, sCons);
        }
    }

    private int[] perm462() {
        int[] perm = IntStream.range(0, 462).toArray();
        perm[1] = 4;
        perm[4] = 1;
        perm[2] = 16;
        perm[16] = 2;
        perm[33] = 3;
        perm[3] = 33;
        return perm;
    }

    @Test
    public void byInitial() throws IOException {
        int k = 6;
        Group group = GroupIndex.group(39, 1);
        int[] comps = new int[]{1, 3, 3, 39};
        GSpace space = new GSpace(k, group, true, comps);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + ".txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            int v = space.v();
            int sqr = v * v;
            Set<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
            List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
            br.lines().forEach(l -> {
                if (l.contains("[{")) {
                    String[] split = l.substring(2, l.length() - 2).split("}, \\{");
                    List<FixBS> base = Arrays.stream(split).map(bl -> FixBS.of(v, Arrays.stream(bl.split(", "))
                            .mapToInt(Integer::parseInt).toArray())).toList();
                    Liner lnr = new Liner(v, base.stream().flatMap(space::blocks).toArray(int[][]::new));
                    liners.add(lnr);
                    System.out.println(lnr.hyperbolicFreq() + " " + l);
                } else {
                    set.remove(readPartial(l, v));
                }
            });
            List<List<FixBS>> tuples = new ArrayList<>(set);
            System.out.println(GroupIndex.identify(group) + " Tuples size: " + tuples.size());
            AtomicInteger cnt = new AtomicInteger();
            AtomicInteger ai = new AtomicInteger();
            BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
                if (!ftr.isFull(sqr)) {
                    return false;
                }
                if (!space.minimal(arr)) {
                    return true;
                }
                ai.incrementAndGet();
                Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                liners.add(l);
                ps.println(Arrays.stream(arr).map(State::block).toList());
                ps.flush();
                System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
                return true;
            };
            tuples.stream().parallel().forEach(lst -> {
                State[] pr = new State[lst.size() - 1];
                for (int i = 0; i < lst.size() - 1; i++) {
                    pr[i] = State.fromBlock(space, lst.get(i));
                }
                FixBS newFilter = space.emptyFilter().copy();
                for (State st : pr) {
                    st.updateFilter(newFilter, space);
                }
                searchDesigns(space, newFilter, pr, State.fromBlock(space, lst.getLast()), 0, fCons);
                int vl = cnt.incrementAndGet();
                if (vl % 100 == 0) {
                    System.out.println(vl);
                }
                ps.println(lst.stream().map(FixBS::toString).collect(Collectors.joining(" ")));
                ps.flush();
            });
            System.out.println("Results " + liners.size());
        }
    }

    @Test
    public void expand() throws IOException {
        int k = 7;
        Group group = GroupIndex.group(42, 2);
        int[] comps = new int[]{6, 1, 1};
        GSpace space = new GSpace(k, group, true, comps);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + ".txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-beg.txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            int v = space.v();
            List<List<FixBS>> toProcess = Collections.synchronizedList(new ArrayList<>());
            Set<List<FixBS>> unprocessed = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
            List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
            br.lines().forEach(l -> {
                if (l.contains("[{")) {
                    String[] split = l.substring(2, l.length() - 2).split("}, \\{");
                    List<FixBS> base = Arrays.stream(split).map(bl -> FixBS.of(v, Arrays.stream(bl.split(", "))
                            .mapToInt(Integer::parseInt).toArray())).toList();
                    Liner lnr = new Liner(v, base.stream().flatMap(space::blocks).toArray(int[][]::new));
                    liners.add(lnr);
                    System.out.println(lnr.hyperbolicFreq() + " " + l);
                } else {
                    List<FixBS> proc = readPartial(l, v);
                    toProcess.add(proc);
                    unprocessed.remove(proc);
                }
            });
            List<List<FixBS>> tuples = new ArrayList<>(unprocessed);
            int nextLength = tuples.getFirst().size() + 1;
            File begExp = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-"
                    + Arrays.stream(comps).mapToObj(Integer::toString).collect(Collectors.joining(",")) + "-begExp.txt");
            try (FileOutputStream fos = new FileOutputStream(begExp);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 PrintStream ps = new PrintStream(bos)) {
                System.out.println("Processed: " + toProcess.size() + ", to expand: " + tuples.size() + ", next size: " + nextLength);
                AtomicInteger cnt = new AtomicInteger();
                AtomicInteger ai = new AtomicInteger();
                BiPredicate<State[], FixBS> fCons = (arr, uu) -> {
                    if (arr.length == nextLength) {
                        if (space.parMinimal(arr)) {
                            ai.incrementAndGet();
                            toProcess.add(Arrays.stream(arr).map(State::block).toList());
                        }
                        return true;
                    } else {
                        return false;
                    }
                };
                tuples.stream().parallel().forEach(lst -> {
                    State[] pr = new State[lst.size() - 1];
                    for (int i = 0; i < lst.size() - 1; i++) {
                        pr[i] = State.fromBlock(space, lst.get(i));
                    }
                    FixBS newFilter = space.emptyFilter().copy();
                    for (State st : pr) {
                        st.updateFilter(newFilter, space);
                    }
                    searchDesigns(space, newFilter, pr, State.fromBlock(space, lst.getLast()), 0, fCons);
                    System.out.println(cnt.incrementAndGet());
                });
                System.out.println("Addition " + ai.get());
                toProcess.forEach(lst -> ps.println(lst.stream().map(FixBS::toString).collect(Collectors.joining(" "))));
            }
        }
    }

    private static List<FixBS> readPartial(String line, int v) {
        String[] sp = line.substring(1, line.length() - 1).split("} \\{");
        return Arrays.stream(sp).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())).collect(Collectors.toList());
    }
}
