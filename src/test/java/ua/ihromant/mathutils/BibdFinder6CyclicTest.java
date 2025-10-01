package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.GapInteractor;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SimpleLinear;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder6CyclicTest {
    @Test
    public void dumpInitial() throws IOException {
        int fixed = 1;
        Group group = new SimpleLinear(2, new GaloisField(3));
        int v = group.order() + fixed;
        int k = 3;
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "-ntr.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Group table = group.asTable();
            FixBS filter = new FixBS(v);
            State[] design = new State[0];
            Predicate<State[]> cons = arr -> {
                State st = arr[0];
                if (st.stabilizer.cardinality() > 1) {
                    ps.println(st.block);
                    ps.flush();
                }
                return true;
            };
            FixBS zero = FixBS.of(v, 0);
            State state = new State(zero, zero, zero, zero, 1);
            searchDesigns(table, filter, design, state, v, k, 0, cons);
        }
    }

    @Test
    public void dumpBeginnings() throws IOException {
        int fixed = 1;
        Group group = new SimpleLinear(2, new GaloisField(3));
        Group table = group.asTable();
        int v = table.order() + fixed;
        int k = 6;
        int[][] auths = auth(table);
        List<State> states = new ArrayList<>();
        Predicate<State[]> cons = arr -> {
            State st = arr[0];
            if (st.stabilizer.cardinality() > 1) {
                states.add(st);
            }
            return true;
        };
        FixBS zero = FixBS.of(v, 0);
        FixBS empty = new FixBS(v);
        State state = new State(zero, zero, empty, zero, 1);
        searchDesigns(table, empty, new State[0], state, v, k, 0, cons);
        System.out.println("Stabilized " + states.size() + " auths " + auths.length + " " + GroupIndex.identify(table));
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "-stab1.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            BiPredicate<List<State>, FixBS> pred = (lst, filter) -> {
                int[][] base = lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new);
                for (int[] auth : auths) {
                    if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                        return true;
                    }
                }
                if ((v - 1 - filter.cardinality()) % (k * (k - 1)) == 0) {
                    ps.println(Arrays.deepToString(base));
                    ps.flush();
                }
                return false;
            };
            IntStream.range(0, states.size()).parallel().forEach(i -> {
                List<State> init = new ArrayList<>();
                State st = states.get(i);
                init.add(st);
                find(states, i, st.filter, init, pred);
            });
        }
    }

    @Test
    public void dumpSeparatedBeginnings() throws IOException {
        int fixed = 1;
        Group group = GroupIndex.group(120, 5);
        Group table = group.asTable();
        int v = table.order() + fixed;
        int k = 6;
        int[][] auths = auth(table);
        List<State> states = new ArrayList<>();
        Predicate<State[]> cons = arr -> {
            State st = arr[0];
            if (st.stabilizer.cardinality() > 1) {
                states.add(st);
            }
            return true;
        };
        FixBS zero = FixBS.of(v, 0);
        FixBS empty = new FixBS(v);
        State state = new State(zero, zero, empty, zero, 1);
        searchDesigns(table, empty, new State[0], state, v, k, 0, cons);
        System.out.println("Stabilized " + states.size() + " auths " + auths.length + " " + GroupIndex.identify(table));
        Map<Integer, PrintStream> streams = new ConcurrentHashMap<>();
        BiPredicate<List<State>, FixBS> pred = (lst, filter) -> {
            int[][] base = lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new);
            for (int[] auth : auths) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                    return true;
                }
            }
            if ((v - 1 - filter.cardinality()) % (k * (k - 1)) == 0) {
                PrintStream ps = openIfMissing(base.length, streams, k, group, fixed);
                ps.println(Arrays.deepToString(base));
                ps.flush();
            }
            return false;
        };
        states.sort(Comparator.comparing(State::block));
        IntStream.range(0, states.size()).parallel().forEach(i -> {
            List<State> init = new ArrayList<>();
            State st = states.get(i);
            init.add(st);
            find(states, i, st.filter, init, pred);
        });
        streams.values().forEach(PrintStream::close);
    }

    private static PrintStream openIfMissing(int cnt, Map<Integer, PrintStream> streams, int k, Group group, int fixed) {
        return streams.computeIfAbsent(cnt, key -> {
            File f = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + key + ".txt");
            FileOutputStream fos;
            try {
                fos = new FileOutputStream(f);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            return new PrintStream(bos);
        });
    }

    @Test
    public void generate() throws IOException {
        int fixed = 1;
        Group group = new SimpleLinear(2, new GaloisField(3));
        Group table = group.asTable();
        int v = group.order() + fixed;
        int k = 3;
        File f = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "-stab1fin.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial", k + "-" + group.name() + "-fix" + fixed + "-stab1.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<List<FixBS>> set = allBr.lines().map(l -> readInitial(l, v)).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    System.out.println(l);
                } else {
                    set.remove(readInitial(l, v));
                }
            });
            List<List<State>> states = set.stream().map(blocks -> blocks.stream()
                    .map(bl -> State.fromBlock(group, v, k, bl)).toList()).toList();
            System.out.println("Initial size " + states.size());
            AtomicInteger ai = new AtomicInteger();
            states.stream().parallel().forEach(lst -> {
                FixBS filter = lst.stream().map(State::filter).reduce(new FixBS(v), FixBS::union);
                filter.clear(group.order());
                int blocksNeeded = (group.order() - 1 - filter.cardinality()) / k / (k - 1);
                FixBS whiteList = filter.copy();
                whiteList.flip(1, group.order());
                DiffState initial = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, filter.nextClearBit(1));
                searchUniqueDesigns(table, k, new int[0][], initial, design -> {
                    if (design.length < blocksNeeded) {
                        return false;
                    }
                    int[][] lines = Stream.concat(lst.stream().flatMap(st -> blocks(st.block.toArray(), v, table)),
                            Arrays.stream(design).flatMap(arr -> blocks(arr, v, table))).toArray(int[][]::new);
                    Liner lnr = new Liner(v, lines);
                    System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(State::block).toArray()) + " " + Arrays.deepToString(design));
                    ps.println(Arrays.deepToString(new int[][][]{lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new), design}));
                    ps.flush();
                    return true;
                });
                ps.println(Arrays.deepToString(lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new)));
                ps.flush();
                int inc = ai.incrementAndGet();
                if (inc % 10000 == 0) {
                    System.out.println(inc);
                }
            });
        }
    }

    private List<FixBS> readInitial(String l, int v) {
        return Arrays.stream(l.substring(2, l.length() - 2).split("], \\["))
                .map(ln -> FixBS.of(v,
                        Arrays.stream(ln.split(", ")).mapToInt(Integer::parseInt).toArray())).toList();
    }

    @Test
    public void toConsole() throws IOException {
        int fixed = 1;
        int k = 3;
        int ord = 24;
        int sz = GroupIndex.groupCount(ord);
        System.out.println(sz);
        for (int i = 1; i <= sz; i++) {
            Group group = GroupIndex.group(ord, i);
            generate(group, fixed, k);
        }
    }

    private static void generate(Group group, int fixed, int k) throws IOException {
        Group table = group.asTable();
        int[][] auths = auth(table);
        int ord = table.order();
        int v = ord + fixed;
        State[] design = new State[0];
        List<State> stabilized = new ArrayList<>();
        Predicate<State[]> cons = arr -> {
            State st = arr[0];
            if (st.stabilizer.cardinality() > 1) {
                stabilized.add(st);
            }
            return true;
        };
        FixBS zero = FixBS.of(v, 0);
        State state = new State(zero, zero, new FixBS(v), zero, 1);
        searchDesigns(table, new FixBS(v), design, state, v, k, 0, cons);
        System.out.println("Stabilized size " + stabilized.size());
        List<List<State>> states = new ArrayList<>();
        BiPredicate<List<State>, FixBS> pred = (lst, filter) -> {
            int[][] base = lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new);
            for (int[] auth : auths) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                    return true;
                }
            }
            if ((v - 1 - filter.cardinality()) % (k * (k - 1)) == 0) {
                if (ord % 2 == 0) {
                    for (int el = filter.nextClearBit(1); el >= 0 && el < ord; el = filter.nextClearBit(el + 1)) {
                        if (el == table.inv(el)) {
                            return false;
                        }
                    }
                }
                synchronized (states) {
                    states.add(lst);
                }
            }
            return false;
        };
        stabilized.sort(Comparator.comparing(State::block));
        IntStream.range(0, stabilized.size()).parallel().forEach(i -> {
            List<State> init = new ArrayList<>();
            State st = stabilized.get(i);
            init.add(st);
            find(stabilized, i, st.filter, init, pred);
        });
        if (states.isEmpty()) {
            return;
        }
        System.out.println("Initial size " + states.size() + " " + new GapInteractor().identifyGroup(group) + " " + v + " " + k + " auths: " + auths.length);
        AtomicInteger ai = new AtomicInteger();
        states.stream().parallel().forEach(lst -> {
            FixBS ftr = lst.stream().map(State::filter).reduce(new FixBS(v), FixBS::union);
            ftr.clear(group.order());
            int bn = (group.order() - 1 - ftr.cardinality()) / k / (k - 1);
            FixBS whiteList = ftr.copy();
            whiteList.flip(1, group.order());
            int next = ftr.nextClearBit(1);
            DiffState initial = new DiffState(new int[k], 1, ftr, whiteList).acceptElem(table, next);
            searchUniqueDesigns(table, k, new int[0][], initial, des -> {
                if (des.length < bn) {
                    return false;
                }
                int[][] base = Stream.concat(lst.stream().map(st -> st.block.toArray()),
                        Arrays.stream(des)).sorted(Combinatorics::compareArr).toArray(int[][]::new);
                int[][] lines = Arrays.stream(base).flatMap(arr -> blocks(arr, v, table)).toArray(int[][]::new);
                Liner lnr = new Liner(v, lines);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(State::block).toArray()) + " " + Arrays.deepToString(des));
                return true;
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
        IntStream.range(prev + 1, states.size()).parallel().forEach(i -> {
            State st = states.get(i);
            if (st.filter.intersects(globalFilter)) {
                return;
            }
            List<State> nextCurr = new ArrayList<>(curr);
            nextCurr.add(st);
            find(states, i, globalFilter.union(st.filter), nextCurr, pred);
        });
    }

    public static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
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

    public static boolean bigger(int[][] fst, int[][] snd) {
        int cmp = 0;
        for (int i = 0; i < fst.length; i++) {
            cmp = Combinatorics.compareArr(snd[i], fst[i]);
            if (cmp != 0) {
                break;
            }
        }
        return cmp < 0;
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

    public static int[][] auth(Group group) {
        int ord = group.order();
        int[][] auth = group.auth();
        int[][] result = new int[auth.length][ord + 1];
        for (int i = 0; i < auth.length; i++) {
            System.arraycopy(auth[i], 0, result[i], 0, auth[i].length);
            result[i][ord] = ord;
        }
        return result;
    }

    private static void searchDesigns(Group group, FixBS filter, State[] currDesign, State state, int v, int k, int prev, Predicate<State[]> cons) {
        if (state.size() == k) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign)) {
                return;
            }
            FixBS nextFilter = filter.union(state.filter());
            FixBS zero = FixBS.of(v, 0);
            int val = nextFilter.nextClearBit(1);
            State nextState = Objects.requireNonNull(new State(zero, zero, zero, zero, 1).acceptElem(group, filter, val, v, k));
            searchDesigns(group, nextFilter, nextDesign, nextState, v, k, 0, cons);
        } else {
            for (int el = filter.nextClearBit(prev + 1); el >= 0 && el < v; el = filter.nextClearBit(el + 1)) {
                if (state.block.get(el)) {
                    continue;
                }
                State nextState = state.acceptElem(group, filter, el, v, k);
                if (nextState != null) {
                    searchDesigns(group, filter, currDesign, nextState, v, k, el, cons);
                }
            }
        }
    }

    private static void searchUniqueDesigns(Group group, int k, int[][] design, DiffState state, Predicate<int[][]> sink) {
        if (state.idx() == k) {
            int[][] nextDesign = Arrays.copyOf(design, design.length + 1);
            nextDesign[design.length] = state.block;
            if (sink.test(nextDesign)) {
                return;
            }
            FixBS nextWhitelist = state.filter.copy();
            nextWhitelist.flip(1, group.order());
            DiffState nextState = new DiffState(new int[k], 1, state.filter, nextWhitelist).acceptElem(group, state.filter.nextClearBit(1));
            searchUniqueDesigns(group, k, nextDesign, nextState, sink);
        } else {
            FixBS whiteList = state.whiteList;
            for (int el = whiteList.nextSetBit(state.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                DiffState nextState = state.acceptElem(group, el);
                searchUniqueDesigns(group, k, design, nextState, sink);
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
            if (sz > 2 && sz > k / 2 && newStabilizer.cardinality() == 1) {
                return null;
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
