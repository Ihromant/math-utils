package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BibdFinder6CyclicTest {
    @Test
    public void dumpInitial() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(117), new CyclicGroup(3));
        int v = group.order();
        int k = 6;
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-ntr.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            int[][] auths = group.auth();
            System.out.println(group.name() + " " + v + " " + k + " auths: " + auths.length);
            Group table = group.asTable();
            FixBS filter = new FixBS(v);
            State[] design = new State[0];
            BiPredicate<State[], Integer> cons = (arr, blockNeeded) -> {
                State st = arr[0];
                if (st.stabilizer.cardinality() > 1) {
                    ps.println(st.block);
                    ps.flush();
                }
                return true;
            };
            int blocksNeeded = v * (v - 1) / k / (k - 1);
            FixBS zero = FixBS.of(v, 0);
            State state = new State(zero, zero, zero, zero, 1);
            searchDesigns(table, filter, design, state, v, k, 0, blocksNeeded, cons);
        }
    }

    @Test
    public void testBeginnings() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(77), new CyclicGroup(3));
        int v = group.order();
        int k = 6;
        List<State> states = new ArrayList<>();
        Files.lines(Path.of("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-ntr.txt")).forEach(l -> {
            FixBS block = FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray());
            states.add(State.fromBlock(group, v, k, block));
        });
        List<List<State>> bases = new ArrayList<>();
        Predicate<List<State>> pred = lst -> {
            if (lst.size() < 2) {
                return false;
            }
            bases.add(lst);
            return true;
        };
        find(states, -1, new FixBS(v), new ArrayList<>(), pred);
        FixBS zero = FixBS.of(v, 0);
        System.out.println("Initial size " + bases.size());
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State[], Integer> fCons = (arr, blockNeeded) -> {
            if (blockNeeded != 0) {
                return false;
            }
            int[][] base = Arrays.stream(arr).map(st -> st.block.toArray()).toArray(int[][]::new);
            int[][] ars = Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new);
            Liner l = new Liner(v, ars);
            liners.add(l);
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(st -> st.block().toString()).collect(Collectors.joining(", ", "{", "}")));
            return true;
        };
        bases.stream().parallel().forEach(lst -> {
            State[] des = lst.toArray(State[]::new);
            FixBS filter = new FixBS(v);
            int bn = v * (v - 1) / k / (k - 1);
            for (State st : des) {
                filter.or(st.filter);
                bn = bn - group.order() / st.stabilizer.cardinality();
            }
            int from = filter.nextClearBit(1);
            State init = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, from, v, k));
            searchDesigns(group.asTable(), filter, des, init, v, k, from, bn, fCons);
            int cnt = ai.incrementAndGet();
            if (cnt % 100 == 0) {
                System.out.println(cnt);
            }
        });
        System.out.println("Results: " + liners.size());
    }

    private static void find(List<State> states, int prev, FixBS globalFilter, List<State> curr, Predicate<List<State>> pred) {
        if (pred.test(curr)) {
            return;
        }
        for (int i = prev + 1; i < states.size(); i++) {
            State st = states.get(i);
            if (st.filter.intersects(globalFilter)) {
                continue;
            }
            List<State> nextCurr = new ArrayList<>(curr);
            nextCurr.add(st);
            find(states, i, globalFilter.union(st.filter), nextCurr, pred);
        }
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
                if (state.block.get(el)) {
                    continue;
                }
                State nextState = state.acceptElem(group, filter, el, v, k);
                if (nextState != null) {
                    searchDesigns(group, filter, currDesign, nextState, v, k, el, blocksNeeded, cons);
                }
            }
        }
    }

    private record State(FixBS block, FixBS stabilizer, FixBS filter, FixBS selfDiff, int size) {
        public static State fromBlock(Group g, int v, int k, FixBS block) {
            FixBS empty = new FixBS(v);
            FixBS zero = FixBS.of(v, 0);
            State result = new State(zero, zero, empty, zero, 1);
            for (int el = block.nextSetBit(1); el >= 0; el = block.nextSetBit(el + 1)) {
                if (result.block().get(el)) {
                    continue;
                }
                result = Objects.requireNonNull(result.acceptElem(g, empty, el, v, k));
            }
            return result;
        }

        private State acceptElem(Group group, FixBS globalFilter, int val, int v, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(v);
            queue.set(val);
            int sz = size;
            FixBS newSelfDiff = selfDiff.copy();
            FixBS newStabilizer = stabilizer.copy();
            FixBS newFilter = filter.copy();
            if (val == group.order()) {
                newFilter.set(val);
                newBlock.set(val);
                return new State(newBlock, newStabilizer, newFilter, newSelfDiff, sz + 1);
            }
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
