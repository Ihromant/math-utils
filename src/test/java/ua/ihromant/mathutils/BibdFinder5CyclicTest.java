package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BibdFinder5CyclicTest {
    @Test
    public void logDesigns() {
        Group group = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        int v = group.order();
        int k = 6;
        int[][] auths = group.auth();
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        FixBS filter = new FixBS(v);
        State[] design = new State[0];
        BiPredicate<State[], Integer> cons = (arr, blockNeeded) -> {
            if (blockNeeded != 0) {
                return false;
            }
            System.out.println(Arrays.deepToString(arr));
            return true;
        };
        int blocksNeeded = v * (v - 1) / k / (k - 1);
        FixBS zero = FixBS.of(v, 0);
        int val = 1;
        State state = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, val, v, k));
        searchDesigns(table, filter, design, state, v, k, val, blocksNeeded, cons);
    }

    @Test
    public void logBlocks() {
        Group group = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        int v = group.order();
        int k = 6;
        int[][] auths = group.auth();
        System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        FixBS filter = new FixBS(v);
        State[] design = new State[0];
        List<State> states = new ArrayList<>();
        BiPredicate<State[], Integer> cons = (arr, blockNeeded) -> {
            State st = arr[0];
            FixBS block = st.block;
            for (int diff = block.nextSetBit(1); diff >= 0; diff = block.nextSetBit(diff + 1)) {
                FixBS altBlock = new FixBS(v);
                int inv = table.inv(diff);
                for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                    altBlock.set(table.op(inv, el));
                }
                if (altBlock.compareTo(block) < 0) {
                    return true;
                }
            }
            st.filter.clear(0);
            states.add(st);
            return true;
        };
        int blocksNeeded = v * (v - 1) / k / (k - 1);
        FixBS zero = FixBS.of(v, 0);
        State state = new State(zero, zero, zero, zero, 1);
        searchDesigns(table, filter, design, state, v, k, 0, blocksNeeded, cons);
        State[] statesArr = states.toArray(State[]::new);
        Arrays.parallelSort(statesArr, Comparator.comparing(State::filter));
        int[] order = calcOrder(statesArr, v);
        System.out.println(statesArr.length);
        State[] filteredStatesArr = Stream.concat(Arrays.stream(statesArr, order[1], order[2])
                .filter(st -> {
                    FixBS base = st.block;
                    for (int[] auth : auths) {
                        FixBS block = new FixBS(v);
                        for (int el = base.nextSetBit(0); el >= 0; el = base.nextSetBit(el + 1)) {
                            block.set(auth[el]);
                        }
                        for (int diff = block.nextSetBit(1); diff >= 0; diff = block.nextSetBit(diff + 1)) {
                            FixBS altBlock = new FixBS(v);
                            int inv = table.inv(diff);
                            for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                                altBlock.set(table.op(inv, el));
                            }
                            if (altBlock.compareTo(base) < 0) {
                                return false;
                            }
                        }
                    }
                    return true;
                }), Arrays.stream(statesArr, order[2], order[v - 1])).toArray(State[]::new);
        int[] newOrder = calcOrder(filteredStatesArr, v);
        System.out.println(filteredStatesArr.length);
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        System.out.println("To process " + (newOrder[2] - newOrder[1]));
        IntStream.range(newOrder[1], newOrder[2]).parallel().forEach(i -> {
            State st = filteredStatesArr[i];
            IntList base = new IntList(v);
            base.add(i);
            calculate(filteredStatesArr, newOrder, v, st.filter().cardinality(), st.filter(), base, fbs -> {
                int[] arr = fbs.toArray();
                int[][] ars = Arrays.stream(arr).boxed().flatMap(j -> blocks(filteredStatesArr[j].block().toArray(), v, group)).toArray(int[][]::new);
                Liner l = new Liner(v, ars);
                liners.add(l);
                System.out.println(l.hyperbolicFreq() + " " + Arrays.deepToString(l.lines()));
            });
            int val = ai.incrementAndGet();
            if (val % 100 == 0) {
                System.out.println(val);
            }
        });
        System.out.println(liners.size());
    }

    private static Stream<int[]> blocks(int[] block, int v, Group gr) {
        int ord = gr.order();
        Set<FixBS> set = new HashSet<>(ord);
        List<int[]> res = new ArrayList<>();
        for (int i = 0; i < ord; i++) {
            FixBS fbs = new FixBS(v);
            for (int el : block) {
                fbs.set(el == ord ? ord : gr.op(i, el));
            }
            if (set.add(fbs)) {
                res.add(fbs.toArray());
            }
        }
        return res.stream();
    }

    private static void calculate(State[] components, int[] order, int v, int currCard, FixBS union, IntList curr, Consumer<IntList> cons) {
        if (currCard == v - 1) {
            cons.accept(curr);
            return;
        }
        int hole = union.nextClearBit(1);
        for (int i = order[hole]; i < order[hole + 1]; i++) {
            State c = components[i];
            if (c.filter.intersects(union)) {
                continue;
            }
            IntList newCurr = curr.copy();
            newCurr.add(i);
            calculate(components, order, v, currCard + c.filter().cardinality(), union.union(c.filter()), newCurr, cons);
        }
    }

    private static int[] calcOrder(State[] comps, int v) {
        int[] res = new int[v];
        for (int i = 1; i < res.length; i++) {
            int prev = res[i - 1];
            FixBS top = FixBS.of(v, i - 1, v);
            res[i] = -Arrays.binarySearch(comps, prev, comps.length, new State(null, null, top, null, 1), Comparator.comparing(State::filter)) - 1;
        }
        return res;
    }

    private static void searchDesigns(Group group, FixBS filter, State[] currDesign, State state, int v, int k, int prev, int blocksNeeded, BiPredicate<State[], Integer> cons) {
        if (state.size() == k) {
            int nextBlocksNeeded = blocksNeeded - group.order() / state.stabilizer().cardinality();
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign, nextBlocksNeeded)) {
                return;
            }
            FixBS nextFilter = filter.union(state.filter());
            FixBS zero = FixBS.of(v, 0);
            int val = nextFilter.nextClearBit(1);
            State nextState = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, val, v, k));
            searchDesigns(group, nextFilter, nextDesign, nextState, v, k, 0, nextBlocksNeeded, cons);
        } else {
            for (int el = filter.nextClearBit(prev + 1); el >= 0 && el < v; el = filter.nextClearBit(el + 1)) {
                State nextState = state.acceptElem(group, filter, el, v, k);
                if (nextState != null) {
                    searchDesigns(group, filter, currDesign, nextState, v, k, el, blocksNeeded, cons);
                }
            }
        }
    }

    private record State(FixBS block, FixBS stabilizer, FixBS filter, FixBS selfDiff, int size) {
        private State acceptElem(Group group, FixBS globalFilter, int val, int v, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(v);
            queue.set(val);
            int sz = size;
            FixBS newSelfDiff = selfDiff.copy();
            FixBS newStabilizer = stabilizer.copy();
            FixBS newFilter = filter.copy();
            while (!queue.isEmpty()) {
                if (++sz > k) {
                    return null;
                }
                int x = queue.nextSetBit(0);
                if (x < val) {
                    return null;
                }
                FixBS stabExt = new FixBS(v);
                FixBS selfDiffExt = new FixBS(v);
                for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                    int bInv = group.inv(b);
                    int xInv = group.inv(x);
                    int xb = group.op(x, bInv);
                    selfDiffExt.set(xb);
                    if (newSelfDiff.get(xb) || newBlock.get(group.op(xb, x))) {
                        stabExt.set(xb);
                    }
                    int bx = group.op(b, xInv);
                    if (newSelfDiff.get(bx)) {
                        stabExt.set(bx);
                    }
                    selfDiffExt.set(bx);
                    int diff = group.op(bInv, x);
                    if (globalFilter.get(diff)) {
                        return null;
                    }
                    int outDiff = group.op(xInv, b);
                    newFilter.set(diff);
                    if (globalFilter.get(outDiff)) {
                        return null;
                    }
                    newFilter.set(outDiff);
                }
                newBlock.set(x);
                stabExt.andNot(newStabilizer);
                for (int st = newStabilizer.nextSetBit(1); st >= 0; st = newStabilizer.nextSetBit(st + 1)) {
                    queue.set(group.op(st, x));
                }
                for (int st = stabExt.nextSetBit(1); st >= 0; st = stabExt.nextSetBit(st + 1)) {
                    for (int b = newBlock.nextSetBit(0); b >= 0; b = newBlock.nextSetBit(b + 1)) {
                        queue.set(group.op(st, b));
                    }
                }
                newStabilizer.or(stabExt);
                newSelfDiff.or(selfDiffExt);
                queue.andNot(newBlock);
            }
            return new State(newBlock, newStabilizer, newFilter, newSelfDiff, sz);
        }
    }

    @Test
    public void testState() {
        Group g = new CyclicGroup(21);
        int v = g.order();
        int k = 5;
        FixBS zero = FixBS.of(v, 0);
        State state = new State(zero, zero, zero, zero, 1);
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 3, v, k));
        assertEquals(FixBS.of(v, 0, 3), state.block);
        assertEquals(FixBS.of(v, 0), state.stabilizer);
        assertEquals(FixBS.of(v, 0, 3, 18), state.selfDiff);
        assertNull(state.acceptElem(g, new FixBS(v), 6, v, k));
        assertNull(state.acceptElem(g, new FixBS(v), 12, v, 7));
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 6, v, 7));
        FixBS bs = FixBS.of(v, IntStream.range(0, 7).map(i -> i * 3).toArray());
        assertEquals(bs, state.selfDiff);
        assertEquals(bs, state.stabilizer);
        assertEquals(bs, state.block);
        state = new State(zero, zero, zero, zero, 1);
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 7, v, k));
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 14, v, k));
        assertNull(state.acceptElem(g, new FixBS(v), 1, v, 6));
        g = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        v = g.order();
        k = 6;
        zero = FixBS.of(v, 0);
        state = new State(zero, zero, zero, zero, 1);
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 1, v, k));
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 2, v, k));
        assertEquals(FixBS.of(v, 0, 1, 2), state.selfDiff);
        assertEquals(FixBS.of(v, 0, 1, 2), state.block);
        assertEquals(FixBS.of(v, 0, 1, 2), state.stabilizer);
        state = Objects.requireNonNull(state.acceptElem(g, new FixBS(v), 3, v, k));
        assertEquals(FixBS.of(v, 0, 1, 2, 3, 31, 80), state.block);
        assertEquals(FixBS.of(v, 0, 1, 2), state.stabilizer);
    }
}
