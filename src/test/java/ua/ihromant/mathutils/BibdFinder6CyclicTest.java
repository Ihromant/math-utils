package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.PermutationGroup;
import ua.ihromant.mathutils.group.SubGroup;
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
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test
    public void dumpAllBeginnings() throws IOException {
        int fixed = 5;
        int k = 5;
        int gs = 96;
        int gc = GroupIndex.groupCount(gs);
        System.out.println(gc);
        for (int i = 1; i <= gc; i++) {
            Group group = GroupIndex.group(gs, i);
            Group table = group.asTable();
            int ord = table.order();
            FixBS orderTwo = orderTwo(table);
            List<Des> shortDes = generateShortDes(table, orderTwo, k, fixed);
            if (shortDes.isEmpty()) {
                return;
            }
            StabState[] stabilized = getStabilized(k, table);
            Arrays.sort(stabilized, Comparator.comparing(StabState::block));
            int[][] auths = table.auth();
            System.out.println("Stabilized size " + stabilized.length + " shorts size " + shortDes.size() + " auths " + auths.length);
            boolean even = isEven(k, ord);
            Map<Integer, PrintStream> streams = new ConcurrentHashMap<>();
            for (Des sh : shortDes) {
                FixBS shortFilter = new FixBS(ord);
                for (StabState st : sh.curr) {
                    shortFilter.or(st.filter);
                }
                int leftFilter = ord - 1 - shortFilter.cardinality();
                StabState[] suitable = Arrays.stream(stabilized).filter(st -> !st.filter.intersects(shortFilter)).toArray(StabState[]::new);
                FixBS[] intersecting = intersecting(suitable);
                Predicate<Des> pr = des -> {
                    if ((leftFilter - des.filter.cardinality()) % (k * (k - 1)) != 0) {
                        return false;
                    }
                    if (even && !orderTwo.diff(des.filter).isEmpty()) {
                        return false;
                    }
                    StabState[] states = Stream.concat(Arrays.stream(sh.curr), Arrays.stream(des.curr)).toArray(StabState[]::new);
                    Arrays.sort(states, Comparator.comparing(StabState::block));
                    if (Arrays.stream(auths).anyMatch(auth -> bigger(states, auth, table))) {
                        return false;
                    }
                    PrintStream ps = openIfMissing(-1, streams, k, group, fixed);
                    ps.println(Arrays.deepToString(Arrays.stream(states).map(st -> st.block.toArray()).toArray(int[][]::new)));
                    ps.flush();
                    return false;
                };
                find(suitable, intersecting, Des.empty(ord, suitable.length), pr);
            }
            streams.values().forEach(PrintStream::close);
        }
    }

    private static List<Des> generateShortDes(Group table, FixBS orderTwo, int k, int fixed) {
        int ord = table.order();
        if (fixed == 0) {
            return List.of(Des.empty(ord, 1));
        }
        StabState[] shorts = getShorts(k, fixed, table);
        if (shorts.length < fixed) {
            return List.of();
        }
        Arrays.sort(shorts, Comparator.comparing(StabState::block));
        System.out.println("Shorts size " + shorts.length);
        FixBS[] intersecting = intersecting(shorts);
        List<Des> result = new ArrayList<>();
        boolean odd = k % 2 == 1 && ord % 2 == 0;
        Predicate<Des> pr = des -> {
            if (des.curr.length < fixed) {
                return false;
            }
            if (des.curr.length + shorts.length - des.idx - 1 < fixed) {
                return true;
            }
            if (odd && !orderTwo.diff(des.filter).isEmpty()) {
                return true;
            }
            synchronized (result) {
                result.add(des);
            }
            return true;
        };
        find(shorts, intersecting, Des.empty(ord, shorts.length), pr);
        return result;
    }

    private static FixBS[] intersecting(StabState[] states) {
        FixBS[] intersecting = new FixBS[states.length];
        IntStream.range(0, states.length).parallel().forEach(i -> {
            FixBS comp = new FixBS(states.length);
            FixBS ftr = states[i].filter;
            for (int j = 0; j < states.length; j++) {
                if (ftr.intersects(states[j].filter)) {
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
        FixBS orderTwo = orderTwo(table);
        List<Des> shortDes = generateShortDes(table, orderTwo, k, fixed);
        if (shortDes.isEmpty()) {
            return;
        }
        StabState[] stabilized = getStabilized(k, table);
        Arrays.sort(stabilized, Comparator.comparing(StabState::block));
        int[][] auths = table.auth();
        System.out.println("Stabilized size " + stabilized.length + " shorts size " + shortDes.size() + " auths " + auths.length);
        boolean even = isEven(k, ord);
        Map<Integer, PrintStream> streams = new ConcurrentHashMap<>();
        for (Des sh : shortDes) {
            FixBS shortFilter = new FixBS(ord);
            for (StabState st : sh.curr) {
                shortFilter.or(st.filter);
            }
            int leftFilter = ord - 1 - shortFilter.cardinality();
            StabState[] suitable = Arrays.stream(stabilized).filter(st -> !st.filter.intersects(shortFilter)).toArray(StabState[]::new);
            FixBS[] intersecting = intersecting(suitable);
            Predicate<Des> pr = des -> {
                if ((leftFilter - des.filter.cardinality()) % (k * (k - 1)) != 0) {
                    return false;
                }
                if (even && !orderTwo.diff(des.filter).isEmpty()) {
                    return false;
                }
                StabState[] states = Stream.concat(Arrays.stream(sh.curr), Arrays.stream(des.curr)).toArray(StabState[]::new);
                Arrays.sort(states, Comparator.comparing(StabState::block));
                if (Arrays.stream(auths).anyMatch(auth -> bigger(states, auth, table))) {
                    return false;
                }
                PrintStream ps = openIfMissing(states.length, streams, k, group, fixed);
                ps.println(Arrays.deepToString(Arrays.stream(states).map(st -> st.block.toArray()).toArray(int[][]::new)));
                ps.flush();
                return false;
            };
            find(suitable, intersecting, Des.empty(ord, suitable.length), pr);
        }
        streams.values().forEach(PrintStream::close);
    }

    private static boolean isEven(int k, int ord) {
        return k % 2 == 0 && ord % 2 == 0;
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
                Predicate<int[][]> fCons = design -> {
                    if (design.length < blocksNeeded) {
                        return false;
                    }
                    ps.println(Arrays.deepToString(new int[][][]{lst.stream().map(st -> st.block.toArray()).toArray(int[][]::new), design}));
                    ps.flush();
                    int[][] base = Stream.concat(lst.stream().map(st -> st.block.toArray()), Arrays.stream(design)).toArray(int[][]::new);
                    Liner lnr = generateLiner(table, fixed, k, base);
                    System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(lst.stream().map(StabState::block).toArray()) + " " + Arrays.deepToString(design));
                    return true;
                };
                if (blocksNeeded == 0) {
                    fCons.test(new int[0][]);
                } else {
                    int next = filter.nextClearBit(1);
                    DiffState diffState = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, next);
                    searchUniqueDesigns(table, k, new int[0][], diffState, fCons);
                }
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
        int ord = table.order();
        FixBS orderTwo = orderTwo(table);
        List<Des> shortDes = generateShortDes(table, orderTwo, k, fixed);
        if (shortDes.isEmpty()) {
            return;
        }
        StabState[] stabilized = getStabilized(k, table);
        Arrays.sort(stabilized, Comparator.comparing(StabState::block));
        int[][] auths = table.auth();
        System.out.println("Stabilized size " + stabilized.length + " shorts size " + shortDes.size() + " auths " + auths.length);
        boolean even = isEven(k, ord);
        List<StabState[]> initial = new ArrayList<>();
        for (Des sh : shortDes) {
            FixBS shortFilter = new FixBS(ord);
            for (StabState st : sh.curr) {
                shortFilter.or(st.filter);
            }
            int leftFilter = ord - 1 - shortFilter.cardinality();
            StabState[] suitable = Arrays.stream(stabilized).filter(st -> !st.filter.intersects(shortFilter)).toArray(StabState[]::new);
            FixBS[] intersecting = intersecting(suitable);
            Predicate<Des> pr = des -> {
                if ((leftFilter - des.filter.cardinality()) % (k * (k - 1)) != 0) {
                    return false;
                }
                if (even && !orderTwo.diff(des.filter).isEmpty()) {
                    return false;
                }
                StabState[] states = Stream.concat(Arrays.stream(sh.curr), Arrays.stream(des.curr)).toArray(StabState[]::new);
                Arrays.sort(states, Comparator.comparing(StabState::block));
                if (Arrays.stream(auths).anyMatch(auth -> bigger(states, auth, table))) {
                    return false;
                }
                synchronized (initial) {
                    initial.add(states);
                }
                return false;
            };
            find(suitable, intersecting, Des.empty(ord, suitable.length), pr);
        }
        if (initial.isEmpty()) {
            return;
        }
        System.out.println("Initial size " + initial.size() + " " + GroupIndex.identify(group) + " " + (ord + fixed) + " " + k + " auths: " + auths.length);
        AtomicInteger ai = new AtomicInteger();
        initial.stream().parallel().forEach(lst -> {
            FixBS ftr = Arrays.stream(lst).map(StabState::filter).reduce(new FixBS(ord), FixBS::union);
            int bn = (ord - 1 - ftr.cardinality()) / k / (k - 1);
            FixBS whiteList = ftr.copy();
            whiteList.flip(1, ord);
            Predicate<int[][]> fCons = des -> {
                if (des.length < bn) {
                    return false;
                }
                int[][] base = Stream.concat(Arrays.stream(lst).map(st -> st.block.toArray()),
                        Arrays.stream(des)).sorted(Combinatorics::compareArr).toArray(int[][]::new);
                Liner lnr = generateLiner(table, fixed, k, base);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(Arrays.stream(lst).map(StabState::block).toArray()) + " " + Arrays.deepToString(des));
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

    @Test
    public void minimalTest() throws IOException {
        int fixed = 0;
        int k = 8;
        Group table = GroupIndex.group(176, 35);
        int ord = table.order();
        FixBS orderTwo = orderTwo(table);
        List<Des> shortDes = generateShortDes(table, orderTwo, k, fixed);
        if (shortDes.isEmpty()) {
            return;
        }
        StabState[] stabilized = getStabilized(k, table);
        Arrays.sort(stabilized, Comparator.comparing(StabState::block));
        int[][] auths = table.auth();
        System.out.println("Stabilized size " + stabilized.length + " shorts size " + shortDes.size() + " auths " + auths.length);
        boolean even = isEven(k, ord);
        List<StabState[]> initial = new ArrayList<>();
        for (Des sh : shortDes) {
            FixBS shortFilter = new FixBS(ord);
            for (StabState st : sh.curr) {
                shortFilter.or(st.filter);
            }
            int leftFilter = ord - 1 - shortFilter.cardinality();
            StabState[] suitable = Arrays.stream(stabilized).filter(st -> !st.filter.intersects(shortFilter)).toArray(StabState[]::new);
            FixBS[] intersecting = intersecting(suitable);
            Predicate<Des> pr = des -> {
                if ((leftFilter - des.filter.cardinality()) % (k * (k - 1)) != 0) {
                    return false;
                }
                if (even && !orderTwo.diff(des.filter).isEmpty()) {
                    return false;
                }
                StabState[] states = Stream.concat(Arrays.stream(sh.curr), Arrays.stream(des.curr)).toArray(StabState[]::new);
                Arrays.sort(states, Comparator.comparing(StabState::block));
                synchronized (initial) {
                    initial.add(states);
                }
                return false;
            };
            find(suitable, intersecting, Des.empty(ord, suitable.length), pr);
        }
        if (initial.isEmpty()) {
            return;
        }
        System.out.println("Initial size " + initial.size() + " " + GroupIndex.identify(table) + " " + (ord + fixed) + " " + k + " auths: " + auths.length);
        List<int[][]> bases = Collections.synchronizedList(new ArrayList<>());
        initial.stream().parallel().forEach(stab -> {
            FixBS filter = new FixBS(ord);
            for (StabState st : stab) {
                filter.or(st.filter);
            }
            FixBS whiteList = filter.copy();
            whiteList.flip(1, ord);
            int next = filter.nextClearBit(1);
            DiffState diffState = new DiffState(new int[k], 1, filter, whiteList).acceptElem(table, next);
            searchFirst(table, diffState, ds -> {
                if (ds.idx < 6 && Arrays.stream(auths).parallel().anyMatch(auth -> !ds.isMinimal(table, auth))) {
                    return true;
                }
                if (ds.idx < 5) {
                    System.out.println(ds.block[2] + " " + ds.block[3]);
                }
                if (ds.idx < k) {
                    return false;
                }
                bases.add(Stream.concat(Arrays.stream(stab).map(st -> st.block.toArray()), Stream.of(ds.block)).toArray(int[][]::new));
                return true;
            });
        });
        System.out.println(bases.size());
        AtomicInteger ai = new AtomicInteger();
        bases.stream().parallel().forEach(lst -> {
            FixBS ftr = Arrays.stream(lst).map(arr -> filter(table, arr)).reduce(new FixBS(ord), FixBS::union);
            int bn = (ord - 1 - ftr.cardinality()) / k / (k - 1);
            FixBS whiteList = ftr.copy();
            whiteList.flip(1, ord);
            Predicate<int[][]> fCons = des -> {
                if (des.length < bn) {
                    return false;
                }
                int[][] base = Stream.concat(Arrays.stream(lst),
                        Arrays.stream(des)).sorted(Combinatorics::compareArr).toArray(int[][]::new);
                Liner lnr = generateLiner(table, fixed, k, base);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.deepToString(base));
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

    private FixBS filter(Group table, int[] arr) {
        FixBS result = new FixBS(table.order());
        for (int i = 0; i < arr.length; i++) {
            for (int j = i + 1; j < arr.length; j++) {
                result.set(table.op(table.inv(arr[i]), arr[j]));
                result.set(table.op(table.inv(arr[j]), arr[i]));
            }
        }
        return result;
    }

    private static StabState[] getShorts(int k, int fixed, Group table) {
        if (fixed == 0) {
            return new StabState[0];
        }
        return table.subGroups().stream().filter(sg -> sg.order() == k - 1)
                .map(sg -> StabState.fromBlock(table, k, sg.elems())).toArray(StabState[]::new);
    }

    private static StabState[] getStabilized(int k, Group table) {
        int ord = table.order();
        if (Combinatorics.isPrime(k) || Arrays.stream(Combinatorics.factorize(k)).noneMatch(fac -> ord % fac == 0)) {
            return table.subGroups().stream().filter(sg -> sg.order() == k)
                            .map(sg -> StabState.fromBlock(table, k, sg.elems())).toArray(StabState[]::new);
        } else {
            Map<FixBS, StabState> states = new HashMap<>();
            Consumer<StabState> cons = st -> {
                if (st.stabilizer.cardinality() > 1) {
                    states.putIfAbsent(st.filter, st);
                }
            };
            FixBS zero = FixBS.of(ord, 0);
            StabState state = new StabState(zero, zero, new FixBS(ord), zero, 1);
            searchStabilized(table, state, k, 0, cons);
            return states.values().toArray(StabState[]::new);
        }
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

    private record Des(StabState[] curr, FixBS filter, FixBS available, int idx) {
        private Des accept(StabState state, FixBS intersecting, int idx) {
            int cl = curr.length;
            StabState[] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = state;
            return new Des(nextCurr, filter.union(state.filter), available.diff(intersecting), idx);
        }

        private static Des empty(int ord, int statesSize) {
            FixBS available = new FixBS(statesSize);
            available.set(0, statesSize);
            return new Des(new StabState[0], new FixBS(ord), available, -1);
        }
    }

    private static void find(StabState[] states, FixBS[] intersecting, Des des, Predicate<Des> pr) {
        if (pr.test(des)) {
            return;
        }
        FixBS available = des.available;
        if (des.curr.length < 2) {
            IntList base = new IntList(available.cardinality());
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                base.add(i);
            }
            Arrays.stream(base.toArray()).parallel().forEach(i -> {
                find(states, intersecting, des.accept(states[i], intersecting[i], i), pr);
            });
        } else {
            for (int i = available.nextSetBit(des.idx + 1); i >= 0; i = available.nextSetBit(i + 1)) {
                find(states, intersecting, des.accept(states[i], intersecting[i], i), pr);
            }
        }
    }

    private static FixBS minimalTuple(FixBS tuple, int[] auth, Group gr) {
        int ord = gr.order();
        FixBS base = new FixBS(ord);
        for (int val = tuple.nextSetBit(0); val >= 0; val = tuple.nextSetBit(val + 1)) {
            base.set(auth[val]);
        }
        FixBS min = base;
        for (int val = base.nextSetBit(1); val >= 0 && val < ord; val = base.nextSetBit(val + 1)) {
            FixBS cnd = new FixBS(ord);
            int inv = gr.inv(val);
            for (int oVal = base.nextSetBit(0); oVal >= 0; oVal = base.nextSetBit(oVal + 1)) {
                cnd.set(gr.op(inv, oVal));
            }
            if (cnd.compareTo(min) < 0) {
                min = cnd;
            }
        }
        return min;
    }

    private static boolean bigger(StabState[] fst, int[] auth, Group table) {
        FixBS[] transformed = new FixBS[fst.length];
        for (int i = 0; i < fst.length; i++) {
            transformed[i] = minimalTuple(fst[i].block, auth, table);
        }
        Arrays.sort(transformed);
        int cmp = 0;
        for (int i = 0; i < transformed.length; i++) {
            cmp = transformed[i].compareTo(fst[i].block);
            if (cmp != 0) {
                break;
            }
        }
        return cmp < 0;
    }

    private static void searchStabilized(Group group, StabState state, int k, int prev, Consumer<StabState> cons) {
        if (state.size() == k) {
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

    private static void searchFirst(Group group, DiffState state, Predicate<DiffState> sink) {
        if (sink.test(state)) {
            return;
        }
        FixBS whiteList = state.whiteList;
        for (int el = whiteList.nextSetBit(state.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
            DiffState nextState = state.acceptElem(group, el);
            searchFirst(group, nextState, sink);
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

        public boolean isMinimal(Group gr, int[] auth) {
            for (int i = 0; i < idx; i++) {
                int inv = gr.inv(auth[block[i]]);
                int[] cnd = new int[idx];
                for (int j = 0; j < idx; j++) {
                    cnd[j] = gr.op(inv, auth[block[j]]);
                }
                if (Combinatorics.compareArr(cnd, block) < 0) {
                    return false;
                }
            }
            return true;
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
            Map<Map<Integer, Integer>, PartialLiner> plnrs = new ConcurrentHashMap<>();
            br.lines().parallel().forEach(l -> {
                if (!l.contains("[[[")) {
                    return;
                }
                int[][][] base = map.readValue(l, int[][][].class);
                Liner lnr = generateLiner(table, fixed, k, Stream.concat(Arrays.stream(base[0]), Arrays.stream(base[1])).toArray(int[][]::new));
                Map<Integer, Integer> freq = lnr.hyperbolicFreq();
                PartialLiner pl = new PartialLiner(lnr.lines());
                PartialLiner existing = plnrs.computeIfAbsent(freq, _ -> pl);
                if (pl == existing) {
                    System.out.println(freq + " " + l);
                } else {
                    if (!existing.isomorphicSel(pl)) {
                        System.out.println("Non iso " + freq + " " + l);
                    }
                }
            });
            System.out.println(plnrs.size());
        }
    }

    @Test
    public void dumpAuths() throws IOException {
        String des = "[[{0, 1, 3}, {0, 4, 12}, {0, 6, 16}, {0, 7, 20}], [[0, 2, 8, 9]]]"
                .replace('{', '[').replace('}', ']');
        int[][][] bks = new ObjectMapper().readValue(des, int[][][].class);
        int[][] base = Stream.concat(Arrays.stream(bks[0]), Arrays.stream(bks[1])).toArray(int[][]::new);
        int fixed = 4;
        int k = 4;
        Group g = GroupIndex.group(21, 1);
        Liner lnr = generateLiner(g, fixed, k, base);
        Map<Integer, Integer> freq = lnr.hyperbolicFreq();
        System.out.println(freq);
        PermutationGroup perm = lnr.automorphisms();
        String fp = freq.toString().replace(" ", "");
        System.out.println("Auth order " + perm.order());
        Files.writeString(Path.of("/home/ihromant/maths/g-spaces", "auths" + k + "-" + g.name() + "-fix" + fixed + "-" + fp + ".txt"),
                Arrays.deepToString(lnr.lines()) + "\n" + Arrays.deepToString(perm.permutations()));
    }

    @Test
    public void orbits() throws IOException {
        String des = "[[{0, 1, 3}, {0, 4, 12}, {0, 6, 16}, {0, 7, 20}], [[0, 2, 8, 9]]]"
                .replace('{', '[').replace('}', ']');
        int[][][] bks = new ObjectMapper().readValue(des, int[][][].class);
        int[][] base = Stream.concat(Arrays.stream(bks[0]), Arrays.stream(bks[1])).toArray(int[][]::new);
        int fixed = 4;
        int k = 4;
        Group g = GroupIndex.group(21, 1);
        Liner lnr = generateLiner(g, fixed, k, base);
        Map<Integer, Integer> freq = lnr.hyperbolicFreq();
        System.out.println(freq);
        String fp = freq.toString().replace(" ", "");
        String str = Files.readString(Path.of("/home/ihromant/maths/g-spaces", "auths" + k + "-" + g.name() + "-fix" + fixed + "-" + fp + ".txt"));
        PermutationGroup perm = new PermutationGroup(new ObjectMapper().readValue(str.lines().skip(1).findFirst().orElseThrow(), int[][].class));
        Map<Integer, List<SubGroup>> gsg = perm.asTable().groupedSubGroups();
        for (List<SubGroup> sgs : gsg.values()) {
            for (SubGroup sg : sgs) {
                BatchLinerTest.orbits(lnr, new SubGroup(perm, sg.elems()), 0);
            }
        }
    }
}
