package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.SemiDirectProduct;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
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
            FixBS base = st.block;
            for (int[] auth : auths) {
                FixBS block = new FixBS(v);
                for (int el = base.nextSetBit(0); el >= 0; el = base.nextSetBit(el + 1)) {
                    block.set(auth[el]);
                }
                for (int diff = block.nextSetBit(0); diff >= 0; diff = block.nextSetBit(diff + 1)) {
                    FixBS altBlock = new FixBS(v);
                    int inv = table.inv(diff);
                    for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                        altBlock.set(table.op(inv, el));
                    }
                    if (altBlock.compareTo(base) < 0) {
                        return true;
                    }
                }
            }
            st.filter.clear(0);
            states.add(st);
            return true;
        };
        int blocksNeeded = v * (v - 1) / k / (k - 1);
        FixBS zero = FixBS.of(v, 0);
        int val = 1;
        State state = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, val, v, k));
        searchDesigns(table, filter, design, state, v, k, val, blocksNeeded, cons);
        System.out.println("Initial size " + states.size());
        List<Liner> liners = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger ai = new AtomicInteger();
        BiPredicate<State[], Integer> fCons = (arr, blockNeeded) -> {
            if (blockNeeded != 0) {
                return false;
            }
            int[][] ars = Arrays.stream(arr).flatMap(st -> blocks(st.block().toArray(), v, group)).toArray(int[][]::new);
            Liner l = new Liner(v, ars);
            liners.add(l);
            System.out.println(l.hyperbolicFreq() + " " + Arrays.stream(arr).map(st -> st.block().toString()).collect(Collectors.joining(", ", "{", "}")));
            return true;
        };
        states.stream().parallel().forEach(st -> {
            State[] des = new State[]{st};
            int from = st.filter.nextClearBit(1);
            State init = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, from, v, k));
            searchDesigns(table, st.filter, des, init, v, k, from, blocksNeeded - group.order() / st.stabilizer.cardinality(), fCons);
            int cnt = ai.incrementAndGet();
            if (cnt % 100 == 0) {
                System.out.println(cnt);
            }
        });
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

    private static class SubGroupConfig {
        private final Group gr;
        private final FixBS[] cosets;
        private final int[] idx;

        public SubGroupConfig(SubGroup sg) {
            this.gr = sg.group();
            Set<FixBS> set = new HashSet<>();
            for (int i = 0; i < gr.order(); i++) {
                FixBS coset = new FixBS(gr.order());
                for (int el : sg.arr()) {
                    coset.set(gr.op(i, el));
                }
                set.add(coset);
            }
            this.cosets = set.toArray(FixBS[]::new);
            Arrays.sort(cosets);
            this.idx = new int[gr.order()];
            for (int i = 0; i < cosets.length; i++) {
                FixBS coset = cosets[i];
                for (int el = coset.nextSetBit(0); el >= 0; el = coset.nextSetBit(el + 1)) {
                    idx[el] = i;
                }
            }
        }

        private int convert(int g) {
            return idx[g];
        }

        private int apply(int g, int el) {
            return idx[gr.op(g, cosets[el].nextSetBit(0))];
        }
    }

    private static class OrbitApplicator {
        private final SubGroupConfig[] subs;
        private final int[] oBeg;
        private final int v;

        public OrbitApplicator(SubGroup[] subs) {
            this.subs = new SubGroupConfig[subs.length];
            this.oBeg = new int[subs.length];
            int min = 0;
            for (int i = 0; i < subs.length; i++) {
                oBeg[i] = min;
                SubGroupConfig conf = new SubGroupConfig(subs[i]);
                this.subs[i] = conf;
                min = min + conf.cosets.length;
            }
            this.v = min;
        }

        public int apply(int g, int x) {
            int idx = Arrays.binarySearch(oBeg, x);
            if (idx < 0) {
                idx = -idx - 2;
            }
            int min = oBeg[idx];
            return subs[idx].apply(g, x - min) + min;
        }
    }

    @Test
    public void testApplicator() {
        Group g = new SemiDirectProduct(new CyclicGroup(13), new CyclicGroup(3));
        SubGroup tr = new SubGroup(g, FixBS.of(g.order(), 0));
        SubGroup small = new SubGroup(g, FixBS.of(g.order(), 0, 1, 2));
        FixBS f = new FixBS(g.order());
        f.set(0, g.order());
        SubGroup fix = new SubGroup(g, f);
        OrbitApplicator applicator = new OrbitApplicator(new SubGroup[]{tr, small, small, fix});
        assertEquals(66, applicator.v);
        for (int x = 0; x < applicator.v; x++) {
            for (int j = 0; j < g.order(); j++) {
                System.out.println(j + "*" + x + "=" + applicator.apply(j, x));
            }
        }
    }
}
