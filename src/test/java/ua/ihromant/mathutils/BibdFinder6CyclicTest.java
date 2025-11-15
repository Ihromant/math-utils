package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.group.GapInteractor;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder6CyclicTest {
    private static final ObjectMapper map = new ObjectMapper();

    private static FixBS[] intersecting(List<StabState> states) {
        FixBS[] intersecting = new FixBS[states.size()];
        IntStream.range(0, states.size()).parallel().forEach(i -> {
            FixBS comp = new FixBS(states.size());
            FixBS ftr = states.get(i).filter;
            for (int j = 0; j < states.size(); j++) {
                if (ftr.intersects(states.get(j).filter)) {
                    comp.set(j);
                }
            }
            intersecting[i] = comp;
        });
        return intersecting;
    }

    @Test
    public void dumpSeparatedBeginnings() throws IOException {
        int fixed = 1;
        Group group = GroupIndex.group(120, 5);
        Group table = group.asTable();
        int ord = table.order();
        int k = 6;
        int[][] auths = table.auth();
        FixBS orderTwo = orderTwo(table);
        Map<FixBS, StabState> states = new HashMap<>();
        Consumer<StabState> cons = st -> {
            if (st.stabilizer.cardinality() > 1) {
                states.putIfAbsent(st.filter, st);
            }
        };
        FixBS zero = FixBS.of(ord, 0);
        FixBS empty = new FixBS(ord);
        StabState state = new StabState(zero, zero, empty, zero, 1);
        searchStabilized(table, state, k, 0, cons);
        System.out.println("Stabilized " + states.size() + " auths " + auths.length + " " + GroupIndex.identify(table));
        Map<Integer, PrintStream> streams = new ConcurrentHashMap<>();
        Predicate<Des> pred = des -> {
            int[][] base = des.curr.stream().map(st -> st.block.toArray()).toArray(int[][]::new);
            if (Arrays.stream(auths).parallel().anyMatch(auth -> bigger(base,
                    Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new)))) {
                return true;
            }
            if ((ord - 1 - des.filter.cardinality()) % (k * (k - 1)) == 0) {
                if (ord % 2 == 0 && !orderTwo.diff(des.filter).isEmpty()) {
                    return false;
                }
                PrintStream ps = openIfMissing(base.length, streams, k, group, fixed);
                ps.println(Arrays.deepToString(base));
                ps.flush();
            }
            return false;
        };
        List<StabState> stabilized = new ArrayList<>(states.values());
        stabilized.sort(Comparator.comparing(StabState::block));
        FixBS[] intersecting = intersecting(stabilized);
        FixBS available = new FixBS(states.size());
        available.set(0, states.size());
        find(stabilized, intersecting, Des.empty(ord, states.size()), pred);
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
        Group group = GroupIndex.group(120, 5);
        Group table = group.asTable();
        int ord = group.order();
        int k = 6;
        int len = 4;
        File f = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + "fin.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<List<FixBS>> set = allBr.lines().map(l -> readInitial(l, ord)).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    int[][][] base = map.readValue(l, int[][][].class);
                    Liner lnr = generateLiner(table, fixed, k, Stream.concat(Arrays.stream(base[0]), Arrays.stream(base[1])).toArray(int[][]::new));
                    System.out.println(lnr.hyperbolicFreq() + " " + l);
                } else {
                    set.remove(readInitial(l, ord));
                }
            });
            List<List<StabState>> states = set.stream().map(blocks -> blocks.stream()
                    .map(bl -> StabState.fromBlock(group, k, bl)).toList()).toList();
            System.out.println("Initial size " + states.size());
            AtomicInteger ai = new AtomicInteger();
            states.stream().parallel().forEach(lst -> {
                FixBS filter = lst.stream().map(StabState::filter).reduce(new FixBS(ord), FixBS::union);
                int blocksNeeded = (ord - 1 - filter.cardinality()) / k / (k - 1);
                FixBS whiteList = filter.copy();
                whiteList.flip(1, ord);
                DiffState diffState = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, filter.nextClearBit(1));
                searchUniqueDesigns(table, k, new int[0][], diffState, design -> {
                    if (design.length < blocksNeeded) {
                        return false;
                    }
                    ps.println(Arrays.deepToString(new int[][][]{lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new), design}));
                    ps.flush();
                    int[][] base = Stream.concat(lst.stream().map(st -> st.block.toArray()), Arrays.stream(design)).toArray(int[][]::new);
                    Liner lnr = generateLiner(table, fixed, k, base);
                    System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(StabState::block).toArray()) + " " + Arrays.deepToString(design));
                    return true;
                });
                ps.println(Arrays.deepToString(lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new)));
                ps.flush();
                int inc = ai.incrementAndGet();
                if (inc % 100 == 0) {
                    System.out.println(inc);
                }
            });
        }
    }

    private static List<FixBS> readInitial(String l, int ord) {
        return Arrays.stream(map.readValue(l, int[][].class)).map(arr -> {
            FixBS result = new FixBS(ord);
            for (int el : arr) {
                if (el >= ord) {
                    continue;
                }
                result.set(el);
            }
            return result;
        }).toList();
    }

    @Test
    public void expand() throws IOException {
        int fixed = 1;
        Group group = GroupIndex.group(120, 5);
        Group table = group.asTable();
        int ord = table.order();
        int k = 6;
        int len = 4;
        File f = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + "fin.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + ".txt");
        try (FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            List<List<FixBS>> toProcess = Collections.synchronizedList(new ArrayList<>());
            Set<List<FixBS>> unprocessed = allBr.lines().map(l -> readInitial(l, ord)).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    System.out.println(l);
                } else {
                    List<FixBS> proc = readInitial(l, ord);
                    toProcess.add(proc);
                    unprocessed.remove(proc);
                }
            });
            List<List<FixBS>> tuples = new ArrayList<>(unprocessed);
            int nextLength = tuples.getFirst().size() + 1;
            File begExp = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + "Exp.txt");
            try (FileOutputStream fos = new FileOutputStream(begExp);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 PrintStream ps = new PrintStream(bos)) {
                System.out.println("Processed: " + toProcess.size() + ", to expand: " + tuples.size() + ", next size: " + nextLength);
                AtomicInteger cnt = new AtomicInteger();
                AtomicInteger ai = new AtomicInteger();
                tuples.stream().parallel().forEach(fbs -> {
                    List<StabState> lst = fbs.stream().map(bl -> StabState.fromBlock(group, k, bl)).toList();
                    FixBS filter = lst.stream().map(StabState::filter).reduce(new FixBS(ord), FixBS::union);
                    FixBS whiteList = filter.copy();
                    whiteList.flip(1, ord);
                    DiffState initial = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, filter.nextClearBit(1));
                    searchUniqueDesigns(table, k, new int[0][], initial, design -> {
                        List<FixBS> merged = Stream.concat(lst.stream().map(StabState::block), Arrays.stream(design).map(bl -> FixBS.of(ord, bl))).toList();
                        ai.incrementAndGet();
                        toProcess.add(merged);
                        return true;
                    });
                    System.out.println(cnt.incrementAndGet());
                });
                System.out.println("Addition " + ai.get());
                toProcess.forEach(lst -> ps.println(Arrays.deepToString(lst.stream().map(FixBS::toArray).toArray(int[][]::new))));
            }
        }
    }

    @Test
    public void toConsole() throws IOException {
        int fixed = 4;
        int k = 4;
        int ord = 21;
        int sz = GroupIndex.groupCount(ord);
        System.out.println(sz);
        for (int i = 1; i <= sz; i++) {
            Group group = GroupIndex.group(ord, i);
            generate(group, fixed, k);
        }
    }

    private static FixBS orderTwo(Group g) {
        FixBS orderTwo = new FixBS(g.order());
        for (int i = 0; i < g.order(); i++) {
            if (g.order(i) == 2) {
                orderTwo.set(i);
            }
        }
        return orderTwo;
    }

    private static void generate(Group group, int fixed, int k) throws IOException {
        Group table = group.asTable();
        int[][] auths = table.auth();
        int ord = table.order();
        FixBS orderTwo = orderTwo(table);
        Map<FixBS, StabState> states = new HashMap<>();
        Consumer<StabState> cons = st -> {
            if (st.stabilizer.cardinality() > 1) {
                states.putIfAbsent(st.filter, st);
            }
        };
        FixBS zero = FixBS.of(ord, 0);
        StabState state = new StabState(zero, zero, new FixBS(ord), zero, 1);
        searchStabilized(table, state, k, 0, cons);
        List<StabState> stabilized = new ArrayList<>(states.values());
        System.out.println("Stabilized size " + states.size());
        if (stabilized.stream().filter(st -> st.size == k - 1).count() < fixed) {
            return;
        }
        List<List<StabState>> initial = new ArrayList<>();
        Predicate<Des> pred = des -> {
            long fixedCount = des.curr.stream().filter(st -> st.size == k - 1).count();
            if (fixedCount > fixed) {
                return true;
            }
            int[][] base = des.curr.stream().map(st -> st.block.toArray()).toArray(int[][]::new);
            for (int[] auth : auths) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, auth, table)).sorted(Combinatorics::compareArr).toArray(int[][]::new))) {
                    return true;
                }
            }
            if (fixedCount < fixed) {
                return false;
            }
            if ((ord - 1 - des.filter.cardinality()) % (k * (k - 1)) == 0) {
                if (ord % 2 == 0 && !orderTwo.diff(des.filter).isEmpty()) {
                    return false;
                }
                synchronized (initial) {
                    initial.add(des.curr);
                }
            }
            return false;
        };
        stabilized.sort(Comparator.comparing(StabState::block));
        FixBS[] intersecting = intersecting(stabilized);
        find(stabilized, intersecting, Des.empty(ord, states.size()), pred);
        if (initial.isEmpty()) {
            return;
        }
        System.out.println("Initial size " + initial.size() + " " + new GapInteractor().identifyGroup(group) + " " + (ord + fixed) + " " + k + " auths: " + auths.length);
        AtomicInteger ai = new AtomicInteger();
        initial.stream().parallel().forEach(lst -> {
            FixBS ftr = lst.stream().map(StabState::filter).reduce(new FixBS(ord), FixBS::union);
            int bn = (ord - 1 - ftr.cardinality()) / k / (k - 1);
            FixBS whiteList = ftr.copy();
            whiteList.flip(1, ord);
            Predicate<int[][]> fCons = des -> {
                if (des.length < bn) {
                    return false;
                }
                int[][] base = Stream.concat(lst.stream().map(st -> st.block.toArray()),
                        Arrays.stream(des)).sorted(Combinatorics::compareArr).toArray(int[][]::new);
                Liner lnr = generateLiner(table, fixed, k, base);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(StabState::block).toArray()) + " " + Arrays.deepToString(des));
                return true;
            };
            if (bn == 0) {
                fCons.test(new int[0][]);
            } else {
                int next = ftr.nextClearBit(1);
                DiffState diffState = new DiffState(new int[k], 1, ftr, whiteList).acceptElem(table, next);
                searchUniqueDesigns(table, k, new int[0][], diffState, fCons);
            }
            int inc = ai.incrementAndGet();
            if (inc % 10000 == 0) {
                System.out.println(inc);
            }
        });
    }

    private static Liner generateLiner(Group table, int fixed, int k, int[][] base) {
        List<int[]> lines = new ArrayList<>();
        int ord = table.order();
        int fixedCounter = ord;
        int v = ord + fixed;
        for (int[] arr : base) {
            Set<FixBS> set = new HashSet<>(ord);
            List<int[]> res = new ArrayList<>();
            boolean sh = arr.length == k - 1;
            for (int i = 0; i < ord; i++) {
                FixBS fbs = new FixBS(v);
                for (int el : arr) {
                    if (el >= ord) {
                        sh = true;
                        continue;
                    }
                    fbs.set(table.op(i, el));
                }
                if (sh) {
                    fbs.set(fixedCounter);
                }
                if (set.add(fbs)) {
                    res.add(fbs.toArray());
                }
            }
            lines.addAll(res);
            if (sh) {
                fixedCounter++;
            }
        }
        if (fixed == k) {
            lines.add(IntStream.range(ord, v).toArray());
        }
        return new Liner(v, lines.toArray(int[][]::new));
    }

    private record Des(List<StabState> curr, FixBS filter, FixBS available, int idx) {
        private Des accept(StabState state, FixBS intersecting, int idx) {
            List<StabState> nextCurr = new ArrayList<>(curr);
            nextCurr.add(state);
            return new Des(nextCurr, filter.union(state.filter()), available.diff(intersecting), idx);
        }

        private static Des empty(int v, int statesSize) {
            FixBS available = new FixBS(statesSize);
            available.set(0, statesSize);
            return new Des(List.of(), new FixBS(v), available, -1);
        }
    }

    private static void find(List<StabState> states, FixBS[] intersecting, Des des, Predicate<Des> pred) {
        if (pred.test(des)) {
            return;
        }
        FixBS available = des.available;
        if (des.curr.size() < 3) {
            IntList base = new IntList(available.cardinality());
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                base.add(i);
            }
            Arrays.stream(base.toArray()).parallel().forEach(i -> {
                find(states, intersecting, des.accept(states.get(i), intersecting[i], i), pred);
            });
        } else {
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                find(states, intersecting, des.accept(states.get(i), intersecting[i], i), pred);
            }
        }
    }

    public static int[] minimalTuple(int[] tuple, int[] auth, Group gr) {
        int ord = gr.order();
        FixBS base = new FixBS(ord);
        for (int val : tuple) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(0); val >= 0 && val < ord; val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(ord);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(gr.op(inv, oVal));
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

    private static void searchStabilized(Group group, StabState state, int k, int prev, Consumer<StabState> cons) {
        if (state.size() == k || (state.size == (k - 1) && state.block.equals(state.stabilizer))) {
            cons.accept(state);
        } else {
            for (int el = prev + 1; el < group.order(); el++) {
                if (state.block.get(el)) {
                    continue;
                }
                StabState nextState = state.acceptElem(group, el, k);
                if (nextState != null) {
                    searchStabilized(group, nextState, k, el, cons);
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

    private record StabState(FixBS block, FixBS stabilizer, FixBS filter, FixBS selfDiff, int size) {
        public static StabState fromBlock(Group g, int k, FixBS block) {
            FixBS empty = new FixBS(g.order());
            FixBS zero = FixBS.of(g.order(), 0);
            StabState result = new StabState(zero, zero, empty, zero, 1);
            for (int el = block.nextSetBit(1); el >= 0; el = block.nextSetBit(el + 1)) {
                if (result.block().get(el)) {
                    continue;
                }
                result = Objects.requireNonNull(result.acceptSimple(g, el, k));
            }
            return result;
        }

        private StabState acceptSimple(Group group, int val, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(group.order());
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
                FixBS stabExt = new FixBS(group.order());
                FixBS selfDiffExt = new FixBS(group.order());
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
                    newFilter.set(diff);
                    int outDiff = group.op(xInv, b);
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
            return new StabState(newBlock, newStabilizer, newFilter, newSelfDiff, sz);
        }

        private StabState acceptElem(Group group, int val, int k) {
            FixBS newBlock = block.copy();
            FixBS queue = new FixBS(group.order());
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
                FixBS stabExt = new FixBS(group.order());
                FixBS selfDiffExt = new FixBS(group.order());
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
                    newFilter.set(diff);
                    int outDiff = group.op(xInv, b);
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
            return new StabState(newBlock, newStabilizer, newFilter, newSelfDiff, sz);
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
                for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                    newWhiteList.clear(group.op(el, diff));
                }
            }
            return new DiffState(nextBlock, nextIdx, newFilter, newWhiteList);
        }

        public int last() {
            return block[idx - 1];
        }
    }

    @Test
    public void refine() throws IOException {
        int fixed = 1;
        Group group = GroupIndex.group(135, 3);
        Group table = group.asTable();
        int k = 6;
        int len = 2;
        File f = new File("/home/ihromant/maths/g-spaces/initial/separated", k + "-" + group.name() + "-fix" + fixed + "-stabx" + len + "fin.txt");
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Map<Map<Integer, Integer>, PartialLiner> lnrs = new HashMap<>();
            br.lines().forEach(l -> {
                if (!l.contains("[[[")) {
                    return;
                }
                int[][][] base = map.readValue(l, int[][][].class);
                Liner lnr = generateLiner(table, fixed, k, Stream.concat(Arrays.stream(base[0]), Arrays.stream(base[1])).toArray(int[][]::new));
                Map<Integer, Integer> freq = lnr.hyperbolicFreq();
                PartialLiner pl = new PartialLiner(lnr.lines());
                if (lnrs.containsKey(freq)) {
                    if (!lnrs.get(freq).isomorphicSel(pl)) {
                        System.out.println("Non iso " + freq + " " + l);
                    }
                } else {
                    lnrs.put(freq, pl);
                    System.out.println(freq + " " + l);
                }
            });
            System.out.println(lnrs.size());
        }
    }
}
