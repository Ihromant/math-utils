package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.jnauty.JNauty;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupIndex;
import ua.ihromant.mathutils.group.SubGroup;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder7CyclicTest {
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
    public void toConsole() throws IOException {
        int fixed = 0;
        int k = 3;
        int ord = 39;
        int sz = GroupIndex.groupCount(ord);
        System.out.println(sz);
        for (int i = 1; i <= sz; i++) {
            Group group = GroupIndex.group(ord, i);
            generate(group, fixed, k, base -> {
                Liner lnr = generateLiner(group, fixed, k, base);
                System.out.println(lnr.hyperbolicFreq() + " " + Arrays.toString(base));
            });
        }
    }

    @Test
    public void toFile() throws IOException {
        int fixed = 0;
        int k = 3;
        int ord = 39;
        int sz = GroupIndex.groupCount(ord);
        File folder = new File("/home/ihromant/maths/diffSets/grouped/");
        File basic = new File(folder, k + "-" + ord);
        basic.mkdir();
        System.out.println(sz);
        File lnrs = new File(basic, "liners.txt");
        Map<FixBS, Integer> idxes = new ConcurrentHashMap<>();
        AtomicInteger counter = new AtomicInteger();
        try (BufferedWriter lOut = new BufferedWriter(new FileWriter(lnrs))) {
            for (int i = 1; i <= sz; i++) {
                Group group = GroupIndex.group(ord, i);
                writeCayley(basic, group);
                File diffs = new File(basic, group.name() + "Diffs.txt");
                try (BufferedWriter dOut = new BufferedWriter(new FileWriter(diffs))) {
                    generate(group, fixed, k, base -> {
                        Liner lnr = generateLiner(group, fixed, k, base);
                        FixBS canon = lnr.smallCanon();
                        AtomicBoolean added = new AtomicBoolean();
                        int idx = idxes.computeIfAbsent(canon, _ -> {
                            added.set(true);
                            return counter.getAndIncrement();
                        });
                        if (added.get()) {
                            try {
                                lOut.write(idx + " " + lnr.graphData().autCount()  + " " + Arrays.deepToString(lnr.lines()) + "\n");
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        try {
                            dOut.write(idx + " " + Arrays.toString(base) + "\n");
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private static void writeCayley(File basic, Group group) throws IOException {
        File diffs = new File(basic, group.name() + "Cayley.txt");
        try (BufferedWriter cOut = new BufferedWriter(new FileWriter(diffs))) {
            IntStream.range(0, group.order()).forEach(i -> {
                try {
                    cOut.write(IntStream.range(0, group.order()).mapToObj(j -> String.valueOf(group.op(i, j)))
                            .collect(Collectors.joining(" ", "", "\n")));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
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

    private static void generate(Group group, int fixed, int k, Consumer<FixBS[]> cons) throws IOException {
        Group table = group.asTable();
        int ord = table.order();
        FixBS orderTwo = orderTwo(table);
        List<Des> shortDes = generateShortDes(table, orderTwo, k, fixed);
        if (shortDes.isEmpty()) {
            return;
        }
        StabState[] stabilized = getStabilizedAlt(k, table);
        Arrays.sort(stabilized, Comparator.comparing(StabState::block));
        int[][] auths = table.auth();
        System.out.println("Stabilized size " + stabilized.length + " shorts size " + shortDes.size() + " auths " + auths.length);
        List<StabState[]> initial = new ArrayList<>();
        int[] trivial = IntStream.range(0, group.order()).toArray();
        for (Des sh : shortDes) {
            FixBS shortFilter = new FixBS(ord);
            for (StabState st : sh.curr) {
                shortFilter.or(st.filter);
            }
            int leftFilter = ord - 1 - shortFilter.cardinality();
            if (leftFilter % (k * (k - 1)) == 0 && orderTwo.diff(shortFilter).isEmpty()) {
                initial.add(sh.curr());
            }
            StabState[] suitable = Arrays.stream(stabilized).filter(st -> !st.filter.intersects(sh.filter)).toArray(StabState[]::new);
            if (suitable.length == 0) {
                return;
            }
            Graph g = Graph.by(suitable, (a, b) -> !a.filter.intersects(b.filter));
            JNauty.instance().cliques(g, 1, ord, a -> {
                FixBS idx = new FixBS(a);
                FixBS ftr = sh.filter.copy();
                List<StabState> states = new ArrayList<>(Arrays.asList(sh.curr));
                for (int i = idx.nextSetBit(0); i >= 0; i = idx.nextSetBit(i + 1)) {
                    StabState st = suitable[i];
                    states.add(st);
                    ftr.or(st.filter);
                }
                if ((ord - 1 - ftr.cardinality()) % (k * (k - 1)) != 0 || !orderTwo.diff(ftr).isEmpty()) {
                    return;
                }
                initial.add(states.toArray(StabState[]::new));
            });
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
                if ((lst.length == 0 || lst.length == 1 && initial.size() == 1) && des.length == 1 && Arrays.stream(auths)
                        .anyMatch(auth -> bigger(new FixBS[]{FixBS.of(group.order(), des[0])}, auth, table))) {
                    return true;
                }
                if (des.length < bn) {
                    return false;
                }
                FixBS[] base = Stream.concat(Arrays.stream(lst).map(StabState::block),
                        Arrays.stream(des).map(a -> FixBS.of(group.order(), a))).map(a -> minimalTuple(a, trivial, group)).toArray(FixBS[]::new);
                Arrays.sort(base);
                if (Arrays.stream(auths).anyMatch(auth -> bigger(base, auth, table))) {
                    return true;
                }
                cons.accept(base);
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

    private static StabState[] getShorts(int k, int fixed, Group table) {
        if (fixed == 0) {
            return new StabState[0];
        }
        return table.subGroups().stream().filter(sg -> sg.order() == k - 1)
                .map(sg -> StabState.fromBlock(table, k, sg.elems())).toArray(StabState[]::new);
    }

    private static Liner generateLiner(Group table, int fixed, int k, FixBS[] base) {
        List<int[]> lines = new ArrayList<>();
        int ord = table.order();
        int fixedCounter = ord;
        int v = ord + fixed;
        for (FixBS arr : base) {
            Set<FixBS> set = new HashSet<>(ord);
            List<int[]> res = new ArrayList<>();
            boolean sh = arr.cardinality() == k - 1;
            for (int i = 0; i < ord; i++) {
                FixBS fbs = new FixBS(v);
                for (int el = arr.nextSetBit(0); el >= 0; el = arr.nextSetBit(el + 1)) {
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
            Arrays.stream(base.toArray()).parallel().forEach(i ->
                    find(states, intersecting, des.accept(states[i], intersecting[i], i), pr));
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

    private static boolean bigger(FixBS[] fst, int[] auth, Group table) {
        FixBS[] transformed = new FixBS[fst.length];
        for (int i = 0; i < fst.length; i++) {
            transformed[i] = minimalTuple(fst[i], auth, table);
        }
        Arrays.sort(transformed);
        int cmp = 0;
        for (int i = 0; i < transformed.length; i++) {
            cmp = transformed[i].compareTo(fst[i]);
            if (cmp != 0) {
                break;
            }
        }
        return cmp < 0;
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

    private static StabState[] getStabilizedAlt(int k, Group table) {
        List<SubGroup> sgs = table.subGroups();
        int ord = table.order();
        int[] suitable = IntStream.rangeClosed(2, k).filter(i -> k % i == 0 && ord % i == 0).toArray();
        List<StabState> states = new ArrayList<>();
        for (int sOrd : suitable) {
            List<SubGroup> subs = sgs.stream().filter(sg -> sg.order() == sOrd).toList();
            for (SubGroup sg : subs) {
                FixBS[] cosets = sg.rightCosets();
                FixBS[] arr = new FixBS[k / sOrd];
                arr[0] = cosets[0];
                findStab(cosets, arr, 1, 1, a -> {
                    FixBS block = new FixBS(ord);
                    for (FixBS f : a) {
                        block.or(f);
                    }
                    FixBS filter = new FixBS(ord);
                    for (int i = block.nextSetBit(0); i >= 0; i = block.nextSetBit(i + 1)) {
                        for (int j = block.nextSetBit(i + 1); j >= 0; j = block.nextSetBit(j + 1)) {
                            filter.set(table.op(table.inv(i), j));
                            filter.set(table.op(table.inv(j), i));
                        }
                    }
                    if (filter.cardinality() == k * (k - 1) / sOrd) {
                        states.add(StabState.fromBlock(table, k, block));
                    }
                });
            }
        }
        return states.toArray(StabState[]::new);
    }

    private static void findStab(FixBS[] cosets, FixBS[] arr, int from, int idx, Consumer<FixBS[]> cons) {
        if (idx == arr.length) {
            cons.accept(arr);
            return;
        }
        for (int i = from; i < cosets.length; i++) {
            arr[idx] = cosets[i];
            findStab(cosets, arr, i + 1, idx + 1, cons);
        }
    }
}
