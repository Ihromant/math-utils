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
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BibdFinder6CyclicTest {
    @Test
    public void dumpInitial() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
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
    public void dumpBeginnings() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        int v = group.order();
        int k = 6;
        List<State> states = new ArrayList<>();
        Files.lines(Path.of("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-ntr.txt")).forEach(l -> {
            FixBS block = FixBS.of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray());
            states.add(State.fromBlock(group, v, k, block));
        });
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-stab.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            BiPredicate<List<State>, FixBS> pred = (lst, filter) -> {
                if ((v - 1 - filter.cardinality()) % (k * (k - 1)) == 0) {
                    ps.println(lst.stream().map(st -> st.block.toString()).collect(Collectors.joining(" ")));
                }
                return false;
            };
            find(states, -1, new FixBS(v), new ArrayList<>(), pred);
        }
    }

    @Test
    public void generate() throws IOException {
        Group group = new SemiDirectProduct(new CyclicGroup(37), new CyclicGroup(3));
        Group table = group.asTable();
        int v = group.order();
        int k = 6;
        List<List<State>> states = Files.lines(Path.of("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-stab.txt"))
                .map(l -> Arrays.stream(l.substring(1, l.length() - 1).split("} \\{"))
                        .map(ln -> State.fromBlock(group, v, k, FixBS.of(v,
                                Arrays.stream(ln.split(", ")).mapToInt(Integer::parseInt).toArray()))).toList())
                .limit(10_000_000).toList();
        System.out.println("Initial size " + states.size());
        AtomicInteger ai = new AtomicInteger();
        states.stream().parallel().forEach(lst -> {
            FixBS filter = lst.stream().map(State::filter).reduce(new FixBS(v), FixBS::union);
            int blocksNeeded = (v - 1 - filter.cardinality()) / k / (k - 1);
            FixBS whiteList = filter.copy();
            whiteList.flip(1, v);
            DiffState initial = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, filter.nextClearBit(1));
            searchUniqueDesigns(table, k, new int[blocksNeeded][], blocksNeeded, initial, design -> {
                int[][] lines = Stream.concat(lst.stream().flatMap(st -> blocks(st.block.toArray(), v, table)),
                        Arrays.stream(design).flatMap(arr -> blocks(arr, v, table))).toArray(int[][]::new);
                Liner lnr = new Liner(v, lines);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(State::block).toArray()) + " " + Arrays.deepToString(design));
            });
            int inc = ai.incrementAndGet();
            if (inc % 10000 == 0) {
                System.out.println(inc);
            }
        });
    }

    private static void find(List<State> states, int prev, FixBS globalFilter, List<State> curr, BiPredicate<List<State>, FixBS> pred) {
        if (pred.test(curr, globalFilter)) {
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

    private static void searchUniqueDesigns(Group group, int k, int[][] design, int blocksNeeded, DiffState state, Consumer<int[][]> sink) {
        if (state.idx() == k) {
            int[][] nextDesign = design.clone();
            nextDesign[nextDesign.length - blocksNeeded] = state.block;
            if (blocksNeeded == 1) {
                sink.accept(nextDesign);
                return;
            }
            FixBS nextWhitelist = state.filter.copy();
            nextWhitelist.flip(1, group.order());
            DiffState nextState = new DiffState(new int[k], 1, state.filter, nextWhitelist).acceptElem(group, state.filter.nextClearBit(1));
            searchUniqueDesigns(group, k, nextDesign, blocksNeeded - 1, nextState, sink);
        } else {
            FixBS whiteList = state.whiteList;
            for (int el = whiteList.nextSetBit(state.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                DiffState nextState = state.acceptElem(group, el);
                searchUniqueDesigns(group, k, design, blocksNeeded, nextState, sink);
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

    private record DiffState(int[] block, int idx, FixBS filter, FixBS whiteList) {
        private DiffState acceptElem(Group group, int el) {
            int[] nextBlock = block.clone();
            nextBlock[idx] = el;
            int nextIdx = idx + 1;
            boolean tupleFinished = nextIdx == block.length;
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
//            if (el == group.order()) {
//                newFilter.set(el);
//                newWhiteList.clear(el);
//                return new DiffState(nextBlock, diffNeeded, nextIdx, newFilter, newWhiteList);
//            }
            int invEl = group.inv(el);
            for (int i = 0; i < idx; i++) {
                int val = block[i];
                int diff = group.op(group.inv(val), el);
                int outDiff = group.op(invEl, val);
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (tupleFinished) {
                    continue;
                }
                for (int rt : group.squareRoots(diff)) {
                    newWhiteList.clear(group.op(val, rt));
                }
                for (int rt : group.squareRoots(outDiff)) {
                    newWhiteList.clear(group.op(el, rt));
                }
                for (int j = 0; j <= idx; j++) {
                    int nv = nextBlock[j];
                    newWhiteList.clear(group.op(nv, diff));
                    newWhiteList.clear(group.op(nv, outDiff));
                }
            }
            if (!tupleFinished) {
                for (int diff = newFilter.nextSetBit(0); diff >= 0 && diff < group.order(); diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(el, diff));
                }
            }
            return new DiffState(nextBlock, nextIdx, newFilter, newWhiteList);
        }

        public int last() {
            return block[idx - 1];
        }
    }
}
