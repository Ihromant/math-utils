package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.CyclicProduct;
import ua.ihromant.mathutils.group.GapInteractor;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.GroupProduct;
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
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;
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

    private static final String s1 = """
            [
            [[0, 1, 71, 86, 395, 401], [0, 3, 35, 56, 194, 254], [0, 4, 151, 211, 316, 318], [0, 5, 50, 197, 198, 236], [0, 6, 24, 47, 235, 298], [0, 8, 166, 240, 358, 437], [0, 29, 168, 203, 362, 415], [0, 38, 91, 188, 263, 357], [0, 13, 108, 182, 310, 356], [0, 9, 106, 139, 334, 430], [0, 26, 99, 216, 252, 293], [0, 22, 70, 165, 339, 394], [0, 66, 176, 220, 352, 440], [0, 81, 185, 223, 343, 425], [0, 72, 175, 230, 350, 424], [0, 89, 164, 207, 301, 433], [0, 88, 174, 242, 287, 331], [0, 103, 143, 245, 304, 342], [0, 101, 136, 292, 371, 426], [0, 97, 172, 248, 279, 335]],
            [[0, 1, 68, 84, 398, 399], [0, 3, 14, 35, 107, 425], [0, 5, 64, 96, 293, 311], [0, 39, 105, 271, 368, 430], [0, 24, 118, 182, 193, 420], [0, 7, 222, 249, 386, 403], [0, 6, 31, 40, 56, 59], [0, 85, 170, 233, 274, 315], [0, 8, 10, 192, 205, 209], [0, 75, 168, 210, 348, 432], [0, 9, 90, 196, 256, 402], [0, 26, 81, 101, 137, 389], [0, 22, 139, 216, 288, 325], [0, 23, 108, 157, 349, 438], [0, 30, 289, 307, 335, 362], [0, 33, 131, 188, 217, 241], [0, 120, 160, 224, 253, 437], [0, 100, 162, 236, 254, 361], [0, 69, 166, 227, 350, 409], [0, 89, 180, 237, 290, 333]],
            [[0, 1, 74, 86, 392, 401], [0, 3, 35, 56, 68, 380], [0, 4, 213, 241, 266, 412], [0, 14, 90, 196, 275, 312], [0, 29, 71, 288, 308, 337], [0, 26, 137, 183, 205, 368], [0, 5, 126, 146, 223, 409], [0, 6, 111, 220, 292, 420], [0, 8, 10, 318, 331, 335], [0, 69, 168, 210, 342, 426], [0, 9, 180, 191, 263, 375], [0, 30, 89, 353, 396, 408], [0, 22, 178, 227, 281, 370], [0, 23, 110, 136, 328, 440], [0, 27, 260, 299, 415, 427], [0, 36, 91, 121, 206, 251], [0, 31, 50, 112, 128, 282], [0, 34, 59, 222, 317, 436], [0, 75, 173, 226, 355, 431], [0, 100, 157, 192, 314, 428]],
            [[0, 1, 74, 85, 392, 400], [0, 3, 14, 56, 152, 338], [0, 5, 107, 230, 232, 280], [0, 7, 227, 285, 302, 426], [0, 28, 51, 133, 299, 326], [0, 30, 46, 214, 275, 386], [0, 6, 145, 240, 312, 325], [0, 31, 204, 217, 271, 397], [0, 8, 10, 192, 205, 209], [0, 75, 168, 210, 348, 432], [0, 9, 88, 208, 265, 406], [0, 66, 136, 243, 369, 394], [0, 22, 71, 186, 360, 395], [0, 23, 111, 223, 283, 435], [0, 24, 277, 304, 317, 368], [0, 39, 128, 182, 229, 244], [0, 27, 65, 125, 343, 376], [0, 36, 163, 172, 380, 425], [0, 69, 187, 224, 353, 430], [0, 92, 184, 241, 293, 318]],
            [[0, 1, 74, 86, 392, 401], [0, 3, 35, 56, 68, 380], [0, 4, 200, 350, 377, 420], [0, 5, 84, 243, 301, 428], [0, 7, 120, 248, 298, 406], [0, 14, 119, 154, 172, 271], [0, 6, 222, 251, 288, 305], [0, 72, 169, 206, 332, 430], [0, 8, 10, 192, 205, 209], [0, 75, 168, 210, 348, 432], [0, 9, 37, 57, 227, 290], [0, 22, 140, 237, 309, 326], [0, 26, 102, 225, 264, 284], [0, 66, 143, 246, 366, 389], [0, 31, 50, 104, 171, 268], [0, 80, 170, 211, 345, 439], [0, 34, 59, 196, 375, 404], [0, 73, 185, 223, 347, 429], [0, 69, 182, 229, 346, 437], [0, 94, 188, 239, 289, 318]],
            [[0, 1, 68, 86, 398, 401], [0, 3, 35, 56, 128, 320], [0, 4, 28, 95, 120, 311], [0, 14, 30, 247, 418, 437], [0, 24, 124, 229, 257, 440], [0, 39, 110, 209, 277, 424], [0, 5, 16, 200, 201, 208], [0, 93, 147, 198, 303, 420], [0, 6, 73, 225, 285, 397], [0, 75, 167, 197, 323, 416], [0, 8, 10, 255, 268, 272], [0, 72, 168, 210, 345, 429], [0, 9, 149, 237, 297, 347], [0, 26, 166, 283, 384, 404], [0, 22, 101, 172, 376, 407], [0, 23, 108, 157, 349, 438], [0, 29, 139, 179, 214, 246], [0, 97, 169, 248, 288, 329], [0, 38, 292, 300, 325, 371], [0, 100, 183, 236, 275, 319]]
            ]
            """;

    @Test
    public void tst2() {
        int[][][] bases = new ObjectMapper().readValue(s1, int[][][].class);
        SemiDirectProduct left = new SemiDirectProduct(new CyclicGroup(7), new CyclicGroup(3));
        GroupProduct group = new GroupProduct(left, left);
        int v = group.order();
        Map<Map<Integer, Integer>, int[][]> ordered = new TreeMap<>((a, b) -> {
            int[] fst = IntStream.range(0, 6).map(i -> a.getOrDefault(i, 0)).toArray();
            int[] snd = IntStream.range(0, 6).map(i -> b.getOrDefault(i, 0)).toArray();
            return Combinatorics.compareArr(fst, snd);
        });
        Arrays.stream(bases).forEach(base -> {
            Liner lnr = new Liner(v, Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new));
            ordered.put(lnr.hyperbolicFreq(), base);
        });
        ordered.forEach((freq, base) -> {
            String[][] strs = Arrays.stream(base).map(arr -> {
                return Arrays.stream(arr).mapToObj(el -> {
                    if (el == group.order()) {
                        return "\\infty";
                    }
                    int l = el / left.order();
                    int r = el % left.order();
                    return IntStream.concat(IntStream.of(left.to(l)), IntStream.of(left.to(r))).mapToObj(String::valueOf).collect(Collectors.joining());
                }).toArray(String[]::new);
            }).toArray(String[][]::new);
            System.out.println("\\item $" + freq.toString().replace("{", "[").replace("}", "]") + " \\newline "
                    + Arrays.stream(strs).gather(Gatherers.windowFixed(2))
                    .map(l -> l.stream().map(a -> Arrays.stream(a).collect(Collectors.joining(", ", "\\{", "\\}")))
                            .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining(", \\newline ")) + "$");
        });
    }

    private static final String s4 = """
[
[[0, 1, 14, 26, 35, 41], [0, 3, 73, 103, 175, 199], [0, 4, 46, 111, 165, 181], [0, 5, 21, 30, 47, 182], [0, 10, 56, 78, 97, 179], [0, 28, 49, 164, 174, 203], [0, 17, 127, 151, 177, 200], [0, 23, 67, 75, 173, 208], [0, 13, 62, 161, 202, 207], [0, 25, 86, 113, 144, 178], [0, 16, 129, 143, 166, 209], [0, 22, 50, 61, 146, 213], [0, 43, 65, 98, 112, 147], [0, 31, 69, 128, 185, 223], [0, 72, 99, 171, 198, 225]],
[[0, 1, 14, 26, 35, 41], [0, 3, 76, 100, 172, 202], [0, 4, 62, 101, 173, 224], [0, 5, 97, 121, 142, 157], [0, 10, 33, 89, 103, 149], [0, 12, 19, 71, 122, 205], [0, 15, 43, 85, 164, 221], [0, 22, 30, 116, 160, 218], [0, 11, 133, 143, 166, 210], [0, 20, 50, 61, 150, 211], [0, 38, 66, 98, 112, 151], [0, 29, 67, 132, 185, 223], [0, 47, 55, 118, 128, 171], [0, 65, 82, 137, 163, 198], [0, 54, 108, 162, 216, 225]],
[[0, 1, 14, 26, 35, 41], [0, 3, 10, 22, 31, 37], [0, 4, 74, 134, 152, 200], [0, 5, 85, 106, 178, 193], [0, 9, 92, 118, 146, 163], [0, 18, 55, 65, 182, 190], [0, 11, 133, 143, 166, 210], [0, 20, 50, 61, 150, 211], [0, 38, 66, 98, 112, 151], [0, 29, 67, 132, 185, 223], [0, 13, 62, 84, 119, 142], [0, 25, 49, 113, 123, 191], [0, 17, 79, 100, 120, 158], [0, 32, 73, 87, 175, 197], [0, 72, 99, 171, 198, 225]],
[[0, 1, 14, 26, 35, 41], [0, 3, 65, 131, 149, 209], [0, 4, 57, 127, 145, 219], [0, 5, 92, 102, 137, 174], [0, 9, 18, 27, 36, 225], [0, 10, 42, 56, 116, 196], [0, 19, 30, 110, 157, 221], [0, 15, 37, 88, 170, 218], [0, 16, 33, 83, 100, 152], [0, 11, 133, 143, 166, 210], [0, 20, 50, 61, 150, 211], [0, 38, 66, 98, 112, 151], [0, 29, 67, 132, 185, 223], [0, 13, 62, 161, 202, 207], [0, 25, 86, 113, 144, 178]],
[[0, 1, 14, 26, 35, 41], [0, 3, 68, 128, 146, 212], [0, 4, 57, 97, 142, 219], [0, 5, 15, 42, 92, 137], [0, 10, 56, 79, 98, 177], [0, 19, 75, 103, 110, 185], [0, 37, 105, 143, 205, 218], [0, 28, 50, 164, 175, 201], [0, 11, 116, 151, 181, 213], [0, 20, 67, 136, 147, 221], [0, 16, 127, 150, 179, 200], [0, 31, 64, 101, 203, 210], [0, 13, 62, 161, 202, 207], [0, 25, 86, 113, 144, 178], [0, 72, 99, 171, 198, 225]],
[[0, 1, 14, 26, 35, 41], [0, 3, 83, 122, 158, 191], [0, 4, 57, 97, 142, 219], [0, 5, 15, 42, 92, 137], [0, 10, 56, 88, 123, 143], [0, 19, 50, 110, 121, 192], [0, 37, 98, 159, 196, 218], [0, 28, 84, 157, 164, 185], [0, 11, 116, 151, 181, 213], [0, 20, 67, 136, 147, 221], [0, 16, 127, 150, 179, 200], [0, 31, 64, 101, 203, 210], [0, 13, 62, 161, 202, 207], [0, 25, 86, 113, 144, 178], [0, 81, 117, 153, 189, 225]],
[[0, 1, 14, 26, 35, 41], [0, 3, 56, 113, 167, 218], [0, 4, 89, 104, 176, 197], [0, 5, 118, 133, 151, 154], [0, 11, 74, 81, 127, 181], [0, 20, 101, 117, 136, 208], [0, 16, 59, 73, 98, 174], [0, 22, 78, 100, 116, 185], [0, 43, 102, 143, 199, 221], [0, 31, 50, 170, 172, 204], [0, 17, 109, 150, 184, 212], [0, 23, 66, 142, 152, 217], [0, 44, 49, 68, 132, 163], [0, 32, 55, 97, 134, 210], [0, 54, 108, 162, 216, 225]]
]
            """;

    @Test
    public void tst4() {
        int[][][] bases = new ObjectMapper().readValue(s4, int[][][].class);
        CyclicProduct left = new CyclicProduct(5, 5, 3);
        SemiDirectProduct group = new SemiDirectProduct(left, new CyclicGroup(3));
        int v = group.order() + 1;
        Map<Map<Integer, Integer>, int[][]> ordered = new TreeMap<>((a, b) -> {
            int[] fst = IntStream.range(0, 6).map(i -> a.getOrDefault(i, 0)).toArray();
            int[] snd = IntStream.range(0, 6).map(i -> b.getOrDefault(i, 0)).toArray();
            return Combinatorics.compareArr(fst, snd);
        });
        Arrays.stream(bases).forEach(base -> {
            Liner lnr = new Liner(v, Arrays.stream(base).flatMap(bl -> blocks(bl, v, group)).toArray(int[][]::new));
            ordered.put(lnr.hyperbolicFreq(), base);
        });
        ordered.forEach((freq, base) -> {
            String[][] strs = Arrays.stream(base).map(arr -> {
                return Arrays.stream(arr).mapToObj(el -> {
                    if (el == group.order()) {
                        return "\\infty";
                    }
                    int[] toPair = group.to(el);
                    return IntStream.concat(IntStream.of(left.toArr(toPair[0])), IntStream.of(toPair[1])).mapToObj(String::valueOf).collect(Collectors.joining());
                }).toArray(String[]::new);
            }).toArray(String[][]::new);
            System.out.println("\\item $" + freq.toString().replace("{", "[").replace("}", "]") + " \\newline "
                    + Arrays.stream(strs).gather(Gatherers.windowFixed(2))
                    .map(l -> l.stream().map(a -> Arrays.stream(a).collect(Collectors.joining(", ", "\\{", "\\}")))
                            .collect(Collectors.joining(", ")))
                    .collect(Collectors.joining(", \\newline ")) + "$");
        });
    }
}
