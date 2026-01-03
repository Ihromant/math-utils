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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;
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
        assertNull(space.forInitial(0, 3, 12));
        State state = space.forInitial(0, 3, 6);
        FixBS bs = FixBS.of(g.order(), IntStream.range(0, 7).map(i -> i * 3).toArray());
        assertEquals(bs, state.stabilizer());
        assertEquals(bs, state.block());
        state = space.forInitial(0, 7, 14);
        assertNull(state.acceptElem(space, space.emptyFilter(), 1));
        g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        space = new GSpace(6, g, false, 1);
        state = space.forInitial(0, 1, 2);
        assertEquals(FixBS.of(space.v(), 0, 1, 2), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 3));
        assertEquals(FixBS.of(space.v(), 0, 1, 2, 3, 31, 80), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        g = GroupIndex.group(40, 4);
        space = new GSpace(6, g, false, 1, 8, 8, 8, 8, 40);
        state = space.forInitial(0, 3, 43);
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
        IntStream.range(0, 100).parallel().forEach(_ -> {
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
        IntStream.range(0, 100).parallel().forEach(_ -> {
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
        Consumer<State> sCons = st -> System.out.println(st.block());
        IntStream.range(2, space.v()).forEach(trd -> {
            State state = space.forInitial(0, 1, trd);
            if (state == null) {
                return;
            }
            searchDesignsFirst(space, state, trd, sCons);
        });
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
        List<State> singles = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(2, space.v()).parallel().forEach(trd -> {
            State state = space.forInitial(0, 1, trd);
            if (state == null) {
                return;
            }
            searchDesignsFirst(space, state, trd, singles::add);
        });
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
        singles.stream().parallel().forEach(state -> {
            searchDesigns(space, space.emptyFilter(), new State[0], state, 0, fCons);
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
                List<State> singles = Collections.synchronizedList(new ArrayList<>());
                IntStream.range(2, space.v()).parallel().forEach(trd -> {
                    State state = space.forInitial(0, 1, trd);
                    if (state == null) {
                        return;
                    }
                    searchDesignsFirst(space, state, trd, singles::add);
                });
                System.out.println("Singles size: " + singles.size());
                AtomicInteger cnt = new AtomicInteger();
                AtomicInteger ai = new AtomicInteger();
                BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
                    if (!ftr.isFull(sqr)) {
                        return false;
                    }
                    if (!space.parMinimal(arr)) {
                        return true;
                    }
                    ai.incrementAndGet();
                    Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                    System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
                    return true;
                };
                singles.stream().parallel().forEach(state -> {
                    searchDesigns(space, space.emptyFilter(), new State[0], state, 0, fCons);
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
        List<State> singles = Collections.synchronizedList(new ArrayList<>());
        IntStream.range(2, space.v()).parallel().forEach(trd -> {
            State state = space.forInitial(0, 1, trd);
            if (state == null) {
                return;
            }
            searchDesignsFirst(space, state, trd, singles::add);
        });
        System.out.println("Singles size: " + singles.size());
        List<State[]> pairs = new ArrayList<>();
        List<State[]> sync = Collections.synchronizedList(pairs);
        BiPredicate<State[], FixBS> sCons = (arr, _) -> {
            if (arr.length < 2) {
                return false;
            }
            if (space.parMinimal(arr)) {
                sync.add(arr);
            }
            return true;
        };
        AtomicInteger cnt = new AtomicInteger();
        singles.stream().parallel().forEach(state -> {
            searchDesigns(space, space.emptyFilter(), new State[0], state, 0, sCons);
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
            if (!space.parMinimal(arr)) {
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
        int k = 6;
        int gs = 39;
        int c = GroupIndex.groupCount(gs);
        int[] orbits = new int[]{1, 13, 13, 39};
        System.out.println(c);
        for (int j = 1; j <= c; j++) {
            Group group = GroupIndex.group(gs, j);
            List<int[][]> configs = configs(group, orbits);
            for (int[][] config : configs) {
                GSpace space;
                try {
                    space = new GSpace(k, group, true, config);
                } catch (IllegalArgumentException e) {
                    System.out.println("Not empty");
                    continue;
                }
                int v = space.v();
                System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " configs: "
                        + Arrays.deepToString(config) + " auths: " + space.authLength());
                int sqr = v * v;
                List<State> singles = Collections.synchronizedList(new ArrayList<>());
                IntStream.range(2, space.v()).parallel().forEach(trd -> {
                    State state = space.forInitial(0, 1, trd);
                    if (state == null) {
                        return;
                    }
                    searchDesignsFirst(space, state, trd, singles::add);
                });
                System.out.println("Singles size: " + singles.size());
                List<State[]> pairs = new ArrayList<>();
                List<State[]> sync = Collections.synchronizedList(pairs);
                BiPredicate<State[], FixBS> sCons = (arr, _) -> {
                    if (!space.parMinimal(arr)) {
                        return true;
                    }
                    if (arr.length < 2) {
                        return false;
                    }
                    sync.add(arr);
                    return true;
                };
                AtomicInteger cnt = new AtomicInteger();
                singles.stream().parallel().forEach(state -> {
                    searchDesigns(space, space.emptyFilter(), new State[0], state, 0, sCons);
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
                    if (!space.parMinimal(arr)) {
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
            int fst = pair / v;
            int snd = pair % v;
            int from = snd * v + snd + 1;
            int to = snd * v + v;
            for (int pr = nextFilter.nextClearBit(from); pr >= 0 && pr < to; pr = nextFilter.nextClearBit(pr + 1)) {
                int trd = pr % v;
                State nextState = space.forInitial(fst, snd, trd);
                if (nextState == null || (nextState.size() == 3 ? nextFilter.get(fst * v + trd)
                        : Arrays.stream(nextDesign).anyMatch(st -> st.diffSet().intersects(nextState.diffSet())))) {
                    continue;
                }
                searchDesigns(space, nextFilter, nextDesign, nextState, trd, cons);
            }
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

    private static void searchDesignsFirst(GSpace space, State state, int prev, Consumer<State> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                State nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null && space.minimal(nextState.block())) {
                    searchDesignsFirst(space, nextState, el, cons);
                }
            }
        }
    }

    private static void searchDesignsFirstNoMin(GSpace space, State state, int prev, Consumer<State> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < v; el++) {
                State nextState = state.acceptElem(space, space.emptyFilter(), el);
                if (nextState != null) {
                    searchDesignsFirst(space, nextState, el, cons);
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
            System.out.println(GroupIndex.identify(group) + " " + space.v() + " " + k + " auths: " + space.authLength());
            List<State> singles = Collections.synchronizedList(new ArrayList<>());
            IntStream.range(2, space.v()).parallel().forEach(trd -> {
                State state = space.forInitial(0, 1, trd);
                if (state == null) {
                    return;
                }
                searchDesignsFirst(space, state, trd, singles::add);
            });
            System.out.println(group.name() + " Singles size: " + singles.size());
            AtomicInteger cnt = new AtomicInteger();
            BiPredicate<State[], FixBS> tCons = (arr, _) -> {
                if (arr.length == 2) {
                    if (space.parMinimal(arr)) {
                        ps.println(Arrays.stream(arr).map(st -> st.block().toString()).collect(Collectors.joining(" ")));
                    }
                    return true;
                } else {
                    return false;
                }
            };
            singles.stream().parallel().forEach(st -> {
                searchDesigns(space, space.emptyFilter(), new State[0], st, 0, tCons);
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
            Consumer<State> sCons = st -> {
                ps.println(st.block());
                ps.flush();
            };
            IntStream.range(2, space.v()).parallel().forEach(trd -> {
                State state = space.forInitial(0, 1, trd);
                if (state == null) {
                    return;
                }
                searchDesignsFirst(space, state, trd, sCons);
            });
        }
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
                if (!space.parMinimal(arr)) {
                    return true;
                }
                ai.incrementAndGet();
                ps.println(Arrays.stream(arr).map(State::block).toList());
                ps.flush();
                Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
                liners.add(l);
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
                BiPredicate<State[], FixBS> fCons = (arr, _) -> {
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

    @Test
    public void testEven() throws IOException {
        Group group = GroupIndex.group(48, 4);
        int[] comps = new int[]{1, 1};
        int k = 6;
        GSpace sp = new GSpace(k, group, true, comps);
        int v = sp.v();
        int sqr = v * v;
        Map<FixBS, State> singles = new ConcurrentHashMap<>();
        FixBS evenDiffs = new FixBS(sp.diffLength());
        for (int fst : sp.oBeg()) {
            for (int snd = fst + 1; snd < v; snd++) {
                int lft = sp.diffIdx(fst * v + snd);
                if (lft == sp.diffIdx(snd * v + fst)) {
                    evenDiffs.set(lft);
                }
            }
        }
        System.out.println(GroupIndex.identify(group) + " " + evenDiffs);
        Consumer<State> cons = st -> {
            if (!st.diffSet().intersects(evenDiffs)) {
                return;
            }
            singles.compute(st.diffSet(), (_, old) -> old != null && old.block().compareTo(st.block()) < 0 ? old : st);
        };
        IntStream.of(sp.oBeg()).parallel().forEach(fst -> {
            int[] even = IntStream.range(fst + 1, v).filter(snd -> evenDiffs.get(sp.diffIdx(fst * v + snd))).toArray();
            Arrays.stream(even).parallel().forEach(snd -> {
                IntStream.range(fst + 1, v).forEach(trd -> {
                    State state = sp.forInitial(fst, snd, trd);
                    if (state == null) {
                        return;
                    }
                    searchDesignsFirstNoMin(sp, state, trd, cons);
                });
            });
        });
        State[] base = singles.values().toArray(State[]::new);
        Arrays.sort(base, Comparator.comparing(State::block));
        System.out.println("Even blocks " + base.length);
        List<State[]> begins = Collections.synchronizedList(new ArrayList<>());
        FixBS[] intersecting = intersecting(base);
        IntStream.range(0, base.length).parallel().forEach(idx -> {
            State st = base[idx];
            find(base, intersecting, Des.of(sp.diffLength(), base.length, st, intersecting[idx], idx), des -> {
                if (!evenDiffs.diff(des.diffSet).isEmpty()) {
                    return false;
                }
                begins.add(des.curr());
                return true;
            });
        });
        begins.removeIf(arr -> !sp.parMinimal(arr));
        System.out.println("Initial configs " + begins.size());
        AtomicInteger cnt = new AtomicInteger();
        BiPredicate<State[], FixBS> tCons = (arr, ftr) -> {
            if (!ftr.isFull(sqr)) {
                return false;
            }
            Liner l = new Liner(v, Arrays.stream(arr).flatMap(st -> sp.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        begins.stream().parallel().forEach(tuple -> {
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = sp.emptyFilter().copy();
            for (State st : pr) {
                st.updateFilter(newFilter, sp);
            }
            searchDesigns(sp, newFilter, pr, tuple[tuple.length - 1], 0, tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
    }

    private static FixBS[] intersecting(State[] states) {
        FixBS[] intersecting = new FixBS[states.length];
        IntStream.range(0, states.length).parallel().forEach(i -> {
            FixBS comp = new FixBS(states.length);
            FixBS ftr = states[i].diffSet();
            for (int j = 0; j < states.length; j++) {
                if (ftr.intersects(states[j].diffSet())) {
                    comp.set(j);
                }
            }
            intersecting[i] = comp;
        });
        return intersecting;
    }

    private static void find(State[] states, FixBS[] intersecting, Des des, Predicate<Des> pr) {
        if (pr.test(des)) {
            return;
        }
        FixBS available = des.available;
        for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
            find(states, intersecting, des.accept(states[i], intersecting[i], i), pr);
        }
    }

    private static List<int[][]> configs(Group group, int[] orbitSizes) {
        List<int[][]> result = new ArrayList<>();
        Map<Integer, List<SubGroup>> subs = group.subsByConjugation();
        int[] curr = new int[orbitSizes.length];
        int[] cap = new int[orbitSizes.length];
        int[] sgSizes = new int[orbitSizes.length];
        for (int i = 0; i < orbitSizes.length; i++) {
            if (group.order() % orbitSizes[i] != 0) {
                throw new IllegalArgumentException(group.order() + " " + Arrays.toString(orbitSizes));
            }
            sgSizes[i] = group.order() / orbitSizes[i];
            if (!subs.containsKey(sgSizes[i])) {
                return result;
            }
            cap[i] = subs.get(sgSizes[i]).size();
        }
        while (curr != null) {
            int[] c = curr;
            result.add(IntStream.range(0, sgSizes.length).mapToObj(i -> new int[]{sgSizes[i], c[i]}).toArray(int[][]::new));
            curr = next(orbitSizes, cap, curr);
        }
        return result;
    }

    private static int[] next(int[] orbitSizes, int[] cap, int[] curr) {
        int[] result = curr.clone();
        int idx = curr.length - 1;
        boolean end = false;
        while (idx >= 0 && !end) {
            result[idx]++;
            if (result[idx] < cap[idx]) {
                end = true;
                for (int i = idx + 1; i < curr.length; i++) {
                    if (orbitSizes[idx] != orbitSizes[i]) {
                        continue;
                    }
                    result[i] = result[idx];
                }
            } else {
                result[idx] = 0;
                idx--;
            }
        }
        if (idx < 0) {
            return null;
        }
        return result;
    }

    private record Des(State[] curr, FixBS diffSet, FixBS available, int idx) {
        private Des accept(State state, FixBS intersecting, int idx) {
            int cl = curr.length;
            State[] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = state;
            return new Des(nextCurr, diffSet.union(state.diffSet()), available.diff(intersecting), idx);
        }

        private static Des empty(int ord, int statesSize) {
            FixBS available = new FixBS(statesSize);
            available.set(0, statesSize);
            return new Des(new State[0], new FixBS(ord), available, -1);
        }

        private static Des of(int ord, int statesSize, State state, FixBS intersecting, int idx) {
            return empty(ord, statesSize).accept(state, intersecting, idx);
        }
    }
}
