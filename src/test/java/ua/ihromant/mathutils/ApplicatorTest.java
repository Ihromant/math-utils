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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
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
    public void testState() {
        try {
            new GSpace(6, new CyclicProduct(2, 2), 2);
            fail();
        } catch (Exception e) {
            // ok
        }
        Group g = new CyclicGroup(21);
        GSpace space = new GSpace(7, g, 1);
        FixBS emptyFilter = new FixBS(space.v() * space.v());
        State state = space.forInitial(0, 3);
        assertEquals(FixBS.of(space.v(), 0, 3), state.block());
        assertEquals(FixBS.of(g.order(), 0), state.stabilizer());
        assertNull(state.acceptElem(space, emptyFilter, 12));
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 6));
        FixBS bs = FixBS.of(g.order(), IntStream.range(0, 7).map(i -> i * 3).toArray());
        assertEquals(bs, state.stabilizer());
        assertEquals(bs, state.block());
        state = space.forInitial(0, 7);
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 14));
        assertNull(state.acceptElem(space, emptyFilter, 1));
        g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        space = new GSpace(6, g, 1);
        state = space.forInitial(0, 1);
        emptyFilter = new FixBS(space.v() * space.v());
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 2));
        assertEquals(FixBS.of(space.v(), 0, 1, 2), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
        state = Objects.requireNonNull(state.acceptElem(space, emptyFilter, 3));
        assertEquals(FixBS.of(space.v(), 0, 1, 2, 3, 31, 80), state.block());
        assertEquals(FixBS.of(g.order(), 0, 1, 2), state.stabilizer());
    }

    @Test
    public void logDesigns() {
        int k = 6;
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(k, group, 1, 3, 3, 39);
        int v = space.v();
        System.out.println(group.name() + " " + space.v() + " " + k + " auths: " + space.authLength());
        int sqr = v * v;
        FixBS filter = new FixBS(sqr);
        for (int i = 0; i < v; i++) {
            filter.set(i * v + i);
        }
        State[] design = new State[0];
        List<State> initial = new ArrayList<>();
        BiPredicate<State[], FixBS> cons = (arr, ftr) -> {
            initial.add(arr[0]);
            return true;
        };
        int val = 1;
        State state = space.forInitial(0, val);
        searchDesignsMinimal(space, filter, design, state, val, cons);
        BiPredicate<State[], FixBS> fCons = (arr, ftr) -> {
            if (!space.minimal(arr)) {
                return true;
            }
            if (ftr.cardinality() < sqr) {
                return false;
            }
            Liner l = new Liner(space.v(), Arrays.stream(arr).flatMap(st -> space.blocks(st.block())).toArray(int[][]::new));
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(State::block).toList());
            return true;
        };
        System.out.println("Initial length: " + initial.size());
        AtomicInteger cnt = new AtomicInteger();
        initial.stream().parallel().forEach(st -> {
            searchDesigns(space, filter, design, st, 0, fCons);
            int vl = cnt.incrementAndGet();
            if (vl % 100 == 0) {
                System.out.println(vl);
            }
        });
    }

    private static void searchDesigns(GSpace space, FixBS filter, State[] currDesign, State state, int prev, BiPredicate<State[], FixBS> cons) {
        int v = space.v();
        if (state.size() == space.k()) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            FixBS nextFilter = state.updatedFilter(filter, space);
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
            FixBS nextFilter = state.updatedFilter(filter, space);
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
                if (nextState != null && space.minimal(nextState.diffSet())) {
                    searchDesignsMinimal(space, filter, currDesign, nextState, el, cons);
                }
            }
        }
    }

    private record BlockDiff(FixBS block, int card, FixBS diff) {}

    @Test
    public void logByDiff() {
        int k = 5;
        Group group = new CyclicGroup(5);
        GSpace space = new GSpace(k, group, 1, 1, 1, 1, 1, 1, 1, 1, 5);
        int v = space.v();
        System.out.println(group.name() + " " + v + " " + k + " auths: " + space.authLength());
        int sqr = v * v;
        FixBS filter = new FixBS(sqr);
        for (int i = 0; i < v; i++) {
            filter.set(i * v + i);
        }
        State[] design = new State[0];
        Map<FixBS, BlockDiff> initial = new HashMap<>();
        int sz = space.diffLength();
        BiPredicate<State[], FixBS> cons = (arr, ftr) -> {
            State st = arr[0];
            initial.putIfAbsent(st.diffSet(), new BlockDiff(st.block(), st.diffSet().cardinality(), st.diffSet()));
            return true;
        };
        for (int fst = 0; fst < v; fst++) {
            State state = new State(FixBS.of(v, fst), FixBS.of(space.gOrd(), 0), new FixBS(sz), new IntList[sz], 1);
            searchDesigns(space, filter, design, state, fst, cons);
        }
        BlockDiff[] blocks = initial.values().toArray(BlockDiff[]::new);
        Arrays.parallelSort(blocks, Comparator.comparing(BlockDiff::diff));
        int[] order = calcOrder(sz, blocks);
        System.out.println("Global length " + initial.size() + ", to process " + (order[1] - order[0]));
        AtomicInteger ai = new AtomicInteger();
        Map<Map<Integer, Integer>, Liner> liners = new ConcurrentHashMap<>();
        IntStream.range(order[0], order[1]).parallel().forEach(i -> {
            BlockDiff bd = blocks[i];
            IntList base = new IntList(sz / k);
            base.add(i);
            calculate(blocks, order, bd.card(), bd.diff(), base, (idx, card) -> {
                if (card < sz) {
                    return false;
                }
                int[][] ars = Arrays.stream(idx.toArray()).boxed().flatMap(j -> space.blocks(blocks[j].block)).toArray(int[][]::new);
                Liner l = new Liner(v, ars);
                if (liners.putIfAbsent(l.hyperbolicFreq(), l) == null) {
                    System.out.println(l.hyperbolicFreq() + " " + GroupIndex.identify(l.automorphisms()) + " " + Arrays.deepToString(l.lines()));
                }
                return true;
            });
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println(ai + " " + liners.size());
    }

    private static int[] calcOrder(int sz, BlockDiff[] comps) {
        int[] res = new int[sz];
        for (int i = 1; i < res.length; i++) {
            int prev = res[i - 1];
            FixBS top = FixBS.of(sz, i - 1, sz - 1);
            res[i] = -Arrays.binarySearch(comps, prev, comps.length, new BlockDiff(null, 0, top), Comparator.comparing(BlockDiff::diff)) - 1;
        }
        return res;
    }

    private static void calculate(BlockDiff[] blockDiffs, int[] order, int currCard, FixBS union, IntList curr, BiPredicate<IntList, Integer> cons) {
        if (cons.test(curr, currCard)) {
            return;
        }
        int hole = union.nextClearBit(0);
        for (int i = order[hole]; i < order[hole + 1]; i++) {
            BlockDiff c = blockDiffs[i];
            if (union.intersects(c.diff())) {
                continue;
            }
            IntList newCurr = curr.copy();
            newCurr.add(i);
            calculate(blockDiffs, order, currCard + c.card(), union.union(c.diff()), newCurr, cons);
        }
    }

    @Test
    public void testDifferences() {
        int k = 6;
        Group group = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        GSpace space = new GSpace(k, group, 1, 3, 3, 39);
        int v = space.v();
        for (int i = 0; i < space.diffLength(); i++) {
            FixBS dff = space.difference(i);
            System.out.println("Difference " + i + " size " + dff.cardinality() + " *****************************");
            for (int diff = dff.nextSetBit(0); diff >= 0; diff = dff.nextSetBit(diff + 1)) {
                System.out.println(diff / v + " " + diff % v);
            }
        }
    }
}
