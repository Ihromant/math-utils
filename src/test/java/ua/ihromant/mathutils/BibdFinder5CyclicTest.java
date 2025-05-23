package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.GapInteractor;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SemiDirectProduct;
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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class BibdFinder5CyclicTest {
    @Test
    public void dumpInitial() throws IOException {
        int fixed = 1;
        Group group = new CyclicProduct(8, 8);
        int v = group.order() + fixed;
        int k = 5;
        int[][] auths = auth(group);
        System.out.println(GroupIndex.identify(group) + " " + v + " " + k + " auths: " + auths.length);
        Group table = group.asTable();
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            FixBS filter = new FixBS(v);
            State[] design = new State[0];
            List<State> oneStates = new ArrayList<>();
            BiPredicate<State[], Integer> cons = (arr, blockNeeded) -> {
                State st = arr[0];
                FixBS base = st.block;
                for (int[] auth : auths) {
                    FixBS block = new FixBS(v);
                    for (int el = base.nextSetBit(0); el >= 0; el = base.nextSetBit(el + 1)) {
                        block.set(auth[el]);
                    }
                    for (int diff = block.nextSetBit(0); diff >= 0 && diff < table.order(); diff = block.nextSetBit(diff + 1)) {
                        FixBS altBlock = new FixBS(v);
                        for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                            altBlock.set(el == table.order() ? el : table.op(table.inv(diff), el));
                        }
                        if (altBlock.compareTo(base) < 0) {
                            return true;
                        }
                    }
                }
                st.filter.clear(0);
                oneStates.add(st);
                return true;
            };
            int blocksNeeded = (v + 1) * v / k / (k - 1);
            FixBS zero = FixBS.of(v, 0);
            State state = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, 1, v, k));
            searchDesigns(table, filter, design, state, v, k, 0, blocksNeeded, cons);
            System.out.println("Ones size " + oneStates.size());
            BiPredicate<State[], Integer> cons1 = (arr, bn) -> {
                for (int[] auth : auths) {
                    FixBS[] mapped = new FixBS[arr.length];
                    for (int i = 0; i < arr.length; i++) {
                        FixBS bl = arr[i].block;
                        FixBS block = new FixBS(v);
                        for (int el = bl.nextSetBit(0); el >= 0; el = bl.nextSetBit(el + 1)) {
                            block.set(auth[el]);
                        }
                        FixBS min = block;
                        for (int diff = block.nextSetBit(0); diff >= 0 && diff < table.order(); diff = block.nextSetBit(diff + 1)) {
                            FixBS altBlock = new FixBS(v);
                            for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                                altBlock.set(el == table.order() ? el : table.op(table.inv(diff), el));
                            }
                            if (altBlock.compareTo(min) < 0) {
                                min = altBlock;
                            }
                        }
                        mapped[i] = min;
                    }
                    if (mapped[0].compareTo(mapped[1]) > 0) {
                        FixBS tmp = mapped[1];
                        mapped[1] = mapped[0];
                        mapped[0] = tmp;
                    }
                    int cmp = mapped[0].compareTo(arr[0].block);
                    if (cmp < 0 || cmp == 0 && mapped[1].compareTo(arr[1].block) < 0) {
                        return true;
                    }
                }
                ps.println(Arrays.stream(arr).map(st -> st.block).map(Object::toString).collect(Collectors.joining(" ")));
                ps.flush();
                return true;
            };
            AtomicInteger ai = new AtomicInteger();
            oneStates.stream().parallel().forEach(st -> {
                int nextBlocksNeeded = blocksNeeded - group.order() / st.stabilizer().cardinality();
                State[] nextDesign = new State[]{st};
                FixBS nextFilter = st.filter();
                int min = nextFilter.nextClearBit(1);
                State nextState = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, min, v, k));
                searchDesigns(group, nextFilter, nextDesign, nextState, v, k, min, nextBlocksNeeded, cons1);
                int cnt = ai.incrementAndGet();
                if (cnt % 100 == 0) {
                    System.out.println(cnt);
                }
            });
        }
    }

    @Test
    public void toFile() throws IOException {
        int fixed = 1;
        Group group = new CyclicProduct(8, 8);
        int v = group.order() + fixed;
        int k = 5;
        int[][] auths = auth(group);
        Group table = group.asTable();
        System.out.println(group.name() + " " + GroupIndex.identify(table) + " " + v + " " + k + " auths: " + auths.length);
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + ".txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "beg.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<List<FixBS>> set = allBr.lines().map(l -> readPartial(l, v)).collect(Collectors.toSet());
            Map<List<FixBS>, Liner> liners = new ConcurrentHashMap<>();
            br.lines().forEach(str -> {
                if (str.contains("{{") || str.contains("[{") || str.contains("[[")) {
                    String[] split = str.substring(2, str.length() - 2).split("}, \\{");
                    int[][] base = Arrays.stream(split).map(bl -> Arrays.stream(bl.split(", "))
                            .mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
                    Liner l = new Liner(v, Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new));
                    if (liners.putIfAbsent(Arrays.stream(base).map(a -> FixBS.of(v, a)).toList(), l) == null) {
                        System.out.println(l.hyperbolicFreq() + " " + Arrays.deepToString(base));
                    }
                } else {
                    set.remove(readPartial(str, v));
                }
            });
            int blocksNeeded = v * (v - 1) / k / (k - 1);
            System.out.println("Processing initial of size " + set.size());
            AtomicInteger ai = new AtomicInteger();
            new ArrayList<>(set).stream().parallel().forEach(lst -> {
                State[] design = lst.stream().map(bl -> State.fromBlock(table, v, k, bl)).toArray(State[]::new);
                FixBS filter = new FixBS(v);
                int bn = blocksNeeded;
                for (State st : design) {
                    filter.or(st.filter);
                    bn = bn - table.order() / st.stabilizer.cardinality();
                }
                FixBS zero = FixBS.of(v, 0);
                State state = Objects.requireNonNull(new State(zero, zero, new FixBS(v), zero, 1).acceptElem(table, filter, filter.nextClearBit(1), v, k));
                searchDesigns(table, filter, design, state, v, k, 0, bn, (des, bln) -> {
                    if (bln > 0) {
                        return false;
                    }
                    int[][] base = Arrays.stream(des).map(st -> st.block.toArray()).toArray(int[][]::new);
                    for (int[] auth : auths) {
                        if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                            return true;
                        }
                    }
                    Liner l = new Liner(v, Arrays.stream(des).flatMap(bl -> blocks(bl.block.toArray(), v, group)).toArray(int[][]::new));
                    if (liners.putIfAbsent(Arrays.stream(base).map(a -> FixBS.of(v, a)).toList(), l) == null) {
                        ps.println(Arrays.toString(Arrays.stream(des).map(State::block).toArray()));
                        ps.flush();
                        if (ps != System.out) {
                            System.out.println(l.hyperbolicFreq() + " " + Arrays.toString(Arrays.stream(des).map(State::block).toArray()));
                        }
                    }
                    return true;
                });
                ps.println(lst.stream().map(FixBS::toString).collect(Collectors.joining(" ")));
                ps.flush();
                int cnt = ai.incrementAndGet();
                if (cnt % 100 == 0) {
                    System.out.println(cnt);
                }
            });
            System.out.println("Results: " + liners.size());
        }
    }

    private static List<FixBS> readPartial(String line, int v) {
        String[] sp = line.substring(1, line.length() - 1).split("} \\{");
        return Arrays.stream(sp).map(p -> FixBS.of(v, Arrays.stream(p.split(", ")).mapToInt(Integer::parseInt).toArray())).collect(Collectors.toList());
    }

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

    private static int[][] auth(Group group) {
        int ord = group.order();
        int[][] auth = group.auth();
        int[][] result = new int[auth.length][ord + 1];
        for (int i = 0; i < auth.length; i++) {
            System.arraycopy(auth[i], 0, result[i], 0, auth[i].length);
            result[i][ord] = ord;
        }
        return result;
    }

    @Test
    public void logBlocks() throws IOException {
        for (int i = 1; i < 16; i++) {
            int fixed = 1;
            Group group = GroupIndex.group(84, i);
            int v = group.order() + fixed;
            int k = 5;
            generate(group, v, k);
        }
    }

    private static void generate(Group group, int v, int k) throws IOException {
        int[][] auths = auth(group);
        System.out.println(group.name() + " " + new GapInteractor().identifyGroup(group) + " " + v + " " + k + " auths: " + auths.length);
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
                for (int diff = block.nextSetBit(0); diff >= 0 && diff < table.order(); diff = block.nextSetBit(diff + 1)) {
                    FixBS altBlock = new FixBS(v);
                    for (int el = block.nextSetBit(0); el >= 0; el = block.nextSetBit(el + 1)) {
                        altBlock.set(el == table.order() ? el : table.op(table.inv(diff), el));
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
            int[][] base = Arrays.stream(arr).map(st -> st.block.toArray()).toArray(int[][]::new);
            for (int[] auth : auths) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                    return true;
                }
            }
            int[][] ars = Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new);
            Liner l = new Liner(v, ars);
            liners.add(l);
            System.out.println(l.hyperbolicFreq() + " " + Arrays.deepToString(base));
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
        System.out.println("Results: " + liners.size());
    }

    public static Stream<int[]> blocks(int[] block, int v, Group gr) {
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

    private static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
        int v = gr.order() + 1;
        FixBS base = new FixBS(v);
        for (int val : tuple) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(0); val >= 0 && val < gr.order(); val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(v);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(oVal == gr.order() ? oVal : gr.op(inv, oVal));
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min.toArray();
    }

    private static boolean bigger(int[][] fst, int[][] snd) {
        int cmp = 0;
        for (int i = 0; i < fst.length; i++) {
            cmp = Combinatorics.compareArr(snd[i], fst[i]);
            if (cmp != 0) {
                break;
            }
        }
        return cmp < 0;
    }
}
