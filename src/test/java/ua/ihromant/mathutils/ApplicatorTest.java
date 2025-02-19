package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.g.GSpace;
import ua.ihromant.mathutils.g.State;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

public class ApplicatorTest {
    @Test
    public void testApplicator() {
        Group gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace applicator = new GSpace(6, gr, 1, 3, 3, 39);
        assertEquals(66, applicator.v());
        for (int x = 0; x < applicator.v(); x++) {
            for (int j = 0; j < gr.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
        gr = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        applicator = new GSpace(6, gr, 1, 1);
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
            new GSpace(6, new CyclicProduct(2, 2), 2);
            fail();
        } catch (Exception e) {
            // ok
        }
        Group g = new CyclicGroup(21);
        GSpace space = new GSpace(7, g, 1);
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
        space = new GSpace(6, g, 1);
        state = space.forInitial(0, 1);
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 2));
        assertEquals(FixBS.of(space.v(), 0, 1, 2), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        state = Objects.requireNonNull(state.acceptElem(space, space.emptyFilter(), 3));
        assertEquals(FixBS.of(space.v(), 0, 1, 2, 3, 31, 80), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        g = GroupIndex.group(40, 4);
        space = new GSpace(6, g, 1, 8, 8, 8, 8, 40);
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
        GSpace space = new GSpace(k, g, 1, 3, 3, 39);
        assertEquals(12168, space.authLength());
        int v = space.v();
        System.out.println("Randomized test");
        IntStream.range(0, 100).parallel().forEach(i -> {
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
        GSpace space1 = new GSpace(k, g, 1, 1);
        assertEquals(73728, space1.authLength());
        int ov = space1.v();
        System.out.println("Randomized test");
        IntStream.range(0, 100).parallel().forEach(i -> {
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
    public void logDesigns() throws IOException {
        int k = 6;
        Group group = GroupIndex.group(39, 1);
        GSpace space = new GSpace(k, group, 1, 3, 3, 39);
        int v = space.v();
        System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
        int sqr = v * v;
        List<State[]> singles = new ArrayList<>();
        BiPredicate<State[], FixBS> sCons = (arr, ftr) -> {
            singles.add(arr);
            return true;
        };
        int val = 1;
        State state = space.forInitial(0, val);
        searchDesignsMinimal(space, space.emptyFilter(), new State[0], state, val, sCons);
        System.out.println("Singles size: " + singles.size());
        List<State[]> tuples = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger cnt = new AtomicInteger();
        BiPredicate<State[], FixBS> tCons = (arr, ftr) -> {
            if (arr.length == 2) {
                if (space.minimal(arr)) {
                    tuples.add(arr);
                }
                return true;
            } else {
                return false;
            }
        };
        singles.stream().parallel().forEach(single -> {
            searchDesigns(space, space.emptyFilter(), new State[0], single[0], 0, tCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Tuples size: " + tuples.size());
        cnt.set(0);
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
            if (ftr.cardinality() < sqr) {
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
        tuples.stream().parallel().forEach(tuple -> {
            State[] pr = Arrays.copyOf(tuple, tuple.length - 1);
            FixBS newFilter = space.emptyFilter().copy();
            for (int i = 0; i < tuple.length - 1; i++) {
                tuple[i].updateFilter(newFilter, space);
            }
            searchDesigns(space, newFilter, pr, tuple[tuple.length - 1], 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
        System.out.println("Results " + ai);
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

    private static void searchDesignsMinimal(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
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
            searchDesignsMinimal(space, nextFilter, nextDesign, nextState, snd, cons);
        } else {
            int from = prev * v + prev + 1;
            int to = prev * v + v;
            for (int pair = filter.nextClearBit(from); pair >= 0 && pair < to; pair = filter.nextClearBit(pair + 1)) {
                int el = pair % v;
                State nextState = state.acceptElem(space, filter, el);
                if (nextState != null && (currDesign.length > 0 || space.minimal(nextState.block()))) {
                    searchDesignsMinimal(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }
}
