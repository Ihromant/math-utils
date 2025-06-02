package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
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
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Applicator1Test {
    @Test
    public void findPossible() {
        OrbitConfig conf = new OrbitConfig(65, 5, 2);
        List<IntList> res = getSuitable(conf);
        res.forEach(System.out::println);
    }

    private static List<IntList> getSuitable(OrbitConfig conf) {
        IntList il = new IntList(conf.v());
        List<IntList> res = new ArrayList<>();
        find(conf.orbitSize(), conf.k(), conf.innerFilter().cardinality(), conf.innerFilter().cardinality(), conf.outerFilter().cardinality(), il, res::add);
        return res;
    }

    private static void find(int v, int k, int left, int right, int inter, IntList lst, Consumer<IntList> cons) {
        if (left == v - 1 && right == v - 1 && inter == v) {
            cons.accept(lst);
            return;
        }
        if (left >= v || right >= v || inter > v) {
            return;
        }
        int prev = lst.isEmpty() ? 0 : lst.get(lst.size() - 1);
        for (int i = prev; i <= k; i++) {
            IntList nextLst = lst.copy();
            nextLst.add(i);
            find(v, k, left + i * (i - 1), right + (k - i) * (k - i - 1), inter + i * (k - i), nextLst, cons);
        }
    }

    private record Triple(int left, int mid, int right) {
        @Override
        public String toString() {
            return "(" + left + "," + mid + "," + right + ")";
        }
    }

    @Test
    public void test() {
        List<List<Triple>> result = new ArrayList<>();
        find(30, 7, 5, 5, 5, 0, 0, 0, new ArrayList<>(), result::add);
        result.forEach(System.out::println);
    }

    private static void find(int v, int k, int left, int mid, int right, int interLeftRight, int interLeftMid, int interMidRight, List<Triple> lst, Consumer<List<Triple>> cons) {
        if (left == v - 1 && right == v - 1 && mid == v - 1 && interLeftRight == v && interLeftMid == v && interMidRight == v) {
            cons.accept(lst);
            return;
        }
        if (left >= v || mid >= v || right >= v || interLeftRight > v || interLeftMid > v || interMidRight > v) {
            return;
        }
        Triple prev = lst.isEmpty() ? null : lst.getLast();
        int leftLast = prev == null ? 0 : prev.left();
        for (int l = leftLast; l <= k; l++) {
            int midLast = !lst.isEmpty() && l == leftLast ? prev.mid() : 0;
            for (int m = midLast; m <= k - l; m++) {
                List<Triple> nextLst = new ArrayList<>(lst);
                int r = k - l - m;
                nextLst.add(new Triple(l, m, r));
                find(v, k, left + l * (l - 1), mid + m * (m - 1), right + r * (r - 1), interLeftRight + l * r, interLeftMid + l * m, interMidRight + m * r, nextLst, cons);
            }
        }
    }

    private record State(IntList block, FixBS filter, FixBS whiteList) {
        private State acceptElem(int el, int v) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < sz; i++) {
                int val = nextBlock.get(i);
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = nextBlock.get(j);
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new State(nextBlock, newFilter, newWhiteList);
        }

        private int size() {
            return block.size();
        }
    }

    @Test
    public void generate() {
        OrbitConfig conf = new OrbitConfig(40, 4, 4);
        int[][] suitable = getSuitable(conf).stream().map(IntList::toArray).toArray(int[][]::new);
        List<int[][]> chunks = new ArrayList<>();
        List<int[][]> snc = Collections.synchronizedList(chunks);
        for (int[] sizes : suitable) {
            generateChunks(sizes, conf, snc::add);
        }
        AtomicInteger ai = new AtomicInteger();
        ChunkCallback cb = new ChunkCallback() {
            @Override
            public void onDesign(int[][][] design) {
                Liner liner = conf.fromChunks(design);
                System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
            }

            @Override
            public void onFinish(int[][] chunk) {
                System.out.println(ai.incrementAndGet());
            }
        };
        calculate(chunks, conf, cb);
    }

    @Test
    public void chunksToFile() throws IOException {
        OrbitConfig conf = new OrbitConfig(65, 5, 2);
        int[][] suitable = getSuitable(conf).stream().map(IntList::toArray).toArray(int[][]::new);
        File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Consumer<int[][]> cons = chunk -> {
                ps.println(Arrays.deepToString(chunk));
                ps.flush();
            };
            for (int[] sizes : suitable) {
                generateChunks(sizes, conf, cons);
            }
        }
    }

    private void generateChunks(int[] sizes, OrbitConfig conf, Consumer<int[][]> cons) {
        int[] freq = new int[conf.k() + 1];
        for (int val : sizes) {
            freq[val]++;
        }
        int total = Arrays.stream(freq, 2, freq.length).sum();
        System.out.println("Generate for " + conf.v() + " " + conf.k() + " " + Arrays.toString(sizes) + " " + total);
        int[] multipliers = Combinatorics.multipliers(conf.orbitSize());
        IntList newBlock = new IntList(conf.k());
        newBlock.add(0);
        FixBS filter = conf.innerFilter();
        FixBS whiteList = filter.copy();
        whiteList.flip(1, conf.orbitSize());
        List<State[]> triples = new ArrayList<>();
        searchDesigns(new State[0], freq, new State(newBlock, filter, whiteList).acceptElem(1, conf.orbitSize()), conf.orbitSize(), conf.k(), des -> {
            FixBS[] base = Arrays.stream(des).map(st -> FixBS.of(conf.orbitSize(), st.block.toArray())).toArray(FixBS[]::new);
            for (int mul : multipliers) {
                if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, mul, conf.orbitSize())).sorted().toArray(FixBS[]::new))) {
                    return true;
                }
            }
            if (des.length < Math.min(total / 2, 2)) {
                return false;
            }
            triples.add(des);
            return true;
        });
        System.out.println("Triples size: " + triples.size());
        triples.stream().parallel().forEach(des -> {
            int[] rem = freq.clone();
            for (State st : des) {
                rem[st.block.size()]--;
            }
            FixBS ftr = des[des.length - 1].filter;
            FixBS whL = ftr.copy();
            whL.flip(1, conf.orbitSize());
            IntList nwb = new IntList(conf.k());
            nwb.add(0);
            searchDesigns(des, rem, new State(nwb, ftr, whL).acceptElem(whL.nextSetBit(0), conf.orbitSize()), conf.orbitSize(), conf.k(), finDes -> {
                if (finDes.length < total) {
                    return false;
                }
                FixBS[] base = Arrays.stream(finDes).map(st -> FixBS.of(conf.orbitSize(), st.block.toArray())).toArray(FixBS[]::new);
                for (int mul : multipliers) {
                    if (bigger(base, Arrays.stream(base).map(bl -> minimalTuple(bl, mul, conf.orbitSize())).sorted().toArray(FixBS[]::new))) {
                        return true;
                    }
                }
                List<int[]> res = Arrays.stream(base).map(FixBS::toArray).collect(Collectors.toList());
                IntStream.range(0, freq[1]).forEach(i -> res.add(new int[]{0}));
                IntStream.range(0, freq[0]).forEach(i -> res.add(new int[]{}));
                cons.accept(res.toArray(int[][]::new));
                return true;
            });
        });
    }

    private boolean bigger(FixBS[] base, FixBS[] cand) {
        for (int i = 0; i < base.length; i++) {
            int cmp = base[i].compareTo(cand[i]);
            if (cmp < 0) {
                return false;
            }
            if (cmp > 0) {
                return true;
            }
        }
        return false;
    }

    private static void searchDesigns(State[] currDesign, int[] freq, State state, int v, int k, Predicate<State[]> cons) {
        IntList block = state.block;
        int size = state.size();
        if (hasNext(freq, size + 1)) {
            for (int el = state.whiteList.nextSetBit(block.getLast()); el >= 0; el = state.whiteList.nextSetBit(el + 1)) {
                State nextState = state.acceptElem(el, v);
                searchDesigns(currDesign, freq, nextState, v, k, cons);
            }
        }
        if (freq[size] > 0) {
            State[] nextDesign = Arrays.copyOf(currDesign, currDesign.length + 1);
            nextDesign[currDesign.length] = state;
            if (cons.test(nextDesign)) {
                return;
            }
            IntList newBlock = new IntList(k);
            newBlock.add(0);
            FixBS whiteList = state.filter.copy();
            whiteList.flip(1, v);
            State nextState = new State(newBlock, state.filter, whiteList).acceptElem(whiteList.nextSetBit(0), v);
            int[] newFreq = freq.clone();
            newFreq[size]--;
            searchDesigns(nextDesign, newFreq, nextState, v, k, cons);
        }
    }

    private static boolean hasNext(int[] freq, int from) {
        for (int i = from; i < freq.length; i++) {
            if (freq[i] > 0) {
                return true;
            }
        }
        return false;
    }

    private static FixBS minimalTuple(FixBS tuple, int multiplier, int v) {
        FixBS mapped = new FixBS(v);
        for (int i = tuple.nextSetBit(0); i >= 0; i = tuple.nextSetBit(i + 1)) {
            mapped.set((i * multiplier) % v);
        }
        FixBS result = mapped;
        for (int i = mapped.nextSetBit(1); i >= 0; i = mapped.nextSetBit(i + 1)) {
            FixBS cand = new FixBS(v);
            for (int j = mapped.nextSetBit(0); j >= 0; j = mapped.nextSetBit(j + 1)) {
                int dff = j - i;
                cand.set(dff < 0 ? v + dff : dff);
            }
            if (cand.compareTo(result) < 0) {
                result = cand;
            }
        }
        return result;
    }

    private static List<int[][]> read(OrbitConfig conf) throws IOException {
        File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            List<int[][]> result = new ArrayList<>();
            br.lines().forEach(l -> {
                String[] spl = l.substring(2, l.length() - 2).split("], \\[");
                result.add(Arrays.stream(spl).map(p -> p.isEmpty() ? new int[]{} : Arrays.stream(p.split(", "))
                        .mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new));
            });
            return result;
        }
    }

    @Test
    public void calculate() throws IOException {
        OrbitConfig conf = new OrbitConfig(65, 5, 2);
        List<int[][]> lefts = read(conf);
        AtomicInteger ai = new AtomicInteger();
        ChunkCallback cb = new ChunkCallback() {
            @Override
            public void onDesign(int[][][] design) {
                Liner liner = conf.fromChunks(design);
                System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
            }

            @Override
            public void onFinish(int[][] chunk) {
                System.out.println(ai.incrementAndGet());
            }
        };
        calculate(lefts, conf, cb);
    }

    private record ArrWrap(int[][] arr) {
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ArrWrap(int[][] arr1))) return false;

            return Arrays.deepEquals(arr, arr1);
        }

        @Override
        public int hashCode() {
            return Arrays.deepHashCode(arr);
        }
    }

    @Test
    public void calculateFile() throws IOException {
        try (ForkJoinPool ex = new ForkJoinPool(22)) {
            ex.submit(() -> {
                OrbitConfig conf = new OrbitConfig(96, 6, 6);
                ObjectMapper om = new ObjectMapper();
                File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + "all.txt");
                File beg = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
                try (FileOutputStream fos = new FileOutputStream(f, true);
                     BufferedOutputStream bos = new BufferedOutputStream(fos);
                     PrintStream ps = new PrintStream(bos);
                     FileInputStream allFis = new FileInputStream(beg);
                     InputStreamReader allIsr = new InputStreamReader(allFis);
                     BufferedReader allBr = new BufferedReader(allIsr);
                     FileInputStream fis = new FileInputStream(f);
                     InputStreamReader isr = new InputStreamReader(fis);
                     BufferedReader br = new BufferedReader(isr)) {
                    Set<ArrWrap> set = allBr.lines().map(s -> new ArrWrap(om.readValue(s, int[][].class))).collect(Collectors.toSet());
                    br.lines().forEach(l -> {
                        if (l.contains("[[[")) {
                            int[][][] design = om.readValue(l, int[][][].class);
                            Liner liner = conf.fromChunks(design);
                            System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                        } else {
                            set.remove(new ArrWrap(om.readValue(l, int[][].class)));
                        }
                    });
                    AtomicInteger ai = new AtomicInteger();
                    ChunkCallback cb = new ChunkCallback() {
                        @Override
                        public void onDesign(int[][][] design) {
                            Liner liner = conf.fromChunks(design);
                            System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                            ps.println(Arrays.deepToString(design));
                            ps.flush();
                        }

                        @Override
                        public void onFinish(int[][] chunk) {
                            ps.println(Arrays.deepToString(chunk));
                            ps.flush();
                            int val = ai.incrementAndGet();
                            if (val % 100 == 0) {
                                System.out.println(val);
                            }
                        }
                    };
                    calculate(set.stream().map(ArrWrap::arr).collect(Collectors.toList()), conf, cb);
                } catch (FileNotFoundException e) {
                    throw new RuntimeException(e);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private interface ChunkCallback {
        void onDesign(int[][][] design);
        void onFinish(int[][] chunk);
    }

    private static void calculate(List<int[][]> lefts, OrbitConfig conf, ChunkCallback cb) {
        System.out.println("Lefts size: " + lefts.size() + " for conf " + conf);
        lefts.stream().parallel().forEach(left -> {
            int ll = left.length;
            Predicate<RightState[]> cons = arr -> {
                if (arr[ll - 1] == null) {
                    return false;
                }
                int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left[i], arr[i].block.toArray()}).toArray(int[][][]::new);
                cb.onDesign(res);
                return true;
            };
            int[] fstLeft = left[0];
            RightState[] rights = new RightState[ll];
            FixBS whiteList = new FixBS(conf.orbitSize());
            whiteList.set(0, conf.orbitSize());
            FixBS outerFilter = conf.outerFilter();
            for (int el : fstLeft) {
                whiteList.diffModuleShifted(outerFilter, conf.orbitSize(), conf.orbitSize() - el);
            }
            RightState state = new RightState(new IntList(conf.k()), conf.innerFilter(), outerFilter, whiteList, 0);
            if (outerFilter.isEmpty()) {
                state = state.acceptElem(0, fstLeft, conf.orbitSize());
            }
            find(left, rights, state, conf, cons);
            cb.onFinish(left);
        });
    }

    private static void find(int[][] lefts, RightState[] rights, RightState currState, OrbitConfig conf, Predicate<RightState[]> cons) {
        int idx = currState.idx;
        int[] left = lefts[idx];
        int ol = conf.orbitSize();
        if (currState.block().size() == conf.k() - left.length) {
            RightState[] nextDesign = rights.clone();
            nextDesign[idx] = currState;
            if (cons.test(nextDesign)) {
                return;
            }
            int nextIdx = idx + 1;
            int[] nextLeft = lefts[nextIdx];
            FixBS nextWhitelist = new FixBS(ol);
            nextWhitelist.flip(0, ol);
            for (int el : nextLeft) {
                nextWhitelist.diffModuleShifted(currState.outerFilter, ol, ol - el);
            }
            RightState nextState = new RightState(new IntList(conf.k()), currState.filter, currState.outerFilter, nextWhitelist, nextIdx);
            find(lefts, nextDesign, nextState, conf, cons);
        } else {
            FixBS whiteList = currState.whiteList;
            for (int el = whiteList.nextSetBit(currState.last() + 1); el >= 0; el = whiteList.nextSetBit(el + 1)) {
                RightState nextState = currState.acceptElem(el, left, ol);
                find(lefts, rights, nextState, conf, cons);
            }
        }
    }

    private record RightState(IntList block, FixBS filter, FixBS outerFilter, FixBS whiteList, int idx) {
        private RightState acceptElem(int el, int[] left, int v) {
            int sz = block.size();
            IntList nextBlock = block.copy();
            nextBlock.add(el);
            FixBS newFilter = filter.copy();
            FixBS newOuterFilter = outerFilter.copy();
            FixBS newWhiteList = whiteList.copy();
            int invEl = v - el;
            for (int i = 0; i < sz; i++) {
                int val = nextBlock.get(i);
                int diff = el - val;
                int outDiff = invEl + val;
                newFilter.set(diff);
                newFilter.set(outDiff);
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((el + outDiff / 2) % v);
                }
                for (int j = 0; j <= sz; j++) {
                    int nv = nextBlock.get(j);
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            for (int l : left) {
                int diff = el < l ? v + el - l : el - l;
                newOuterFilter.set(diff);
            }
            for (int l : left) {
                newWhiteList.diffModuleShifted(newOuterFilter, v, v - l);
            }
            newWhiteList.diffModuleShifted(newFilter, v, invEl);
            return new RightState(nextBlock, newFilter, newOuterFilter, newWhiteList, idx);
        }

        public int last() {
            return block.isEmpty() ? -1 : block.getLast();
        }
    }

    private record OrbitConfig(int v, int k, int traceLength) {
        public OrbitConfig {
            if ((v - 1) % (k - 1) != 0 || (v * v - v) % (k * k - k) != 0) {
                throw new IllegalArgumentException();
            }
            int ol = v / 2;
            if (ol % 2 == 0 && (traceLength == 0 || traceLength % 2 != 0)) {
                throw new IllegalArgumentException();
            }
            if (traceLength != 0) {
                if (ol % traceLength != 0) {
                    throw new IllegalArgumentException();
                }
                int div = k / traceLength;
                if (k % 2 == 1 && (k % traceLength != 1 || div != 1 && div != 2)) {
                    throw new IllegalArgumentException();
                }
            }
        }

        public FixBS innerFilter() {
            FixBS filter = new FixBS(orbitSize());
            if (traceLength != 0) {
                for (int i = 1; i < orbitSize(); i++) {
                    if (i * traceLength % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
                if ((k == traceLength * 2 || k == traceLength) && infinity()) {
                    for (int i = 1; i < orbitSize(); i++) {
                        if (i * (k - 1) % orbitSize() == 0) {
                            filter.set(i);
                        }
                    }
                }
            }
            if (traceLength == 0 && infinity()) {
                for (int i = 1; i < orbitSize(); i++) {
                    if (i * (k - 1) % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
            }
            return filter;
        }

        public FixBS outerFilter() {
            FixBS filter = new FixBS(orbitSize());
            if (traceLength != 0 && k / traceLength != 1) {
                for (int i = 0; i < orbitSize(); i++) {
                    if (i * traceLength % orbitSize() == 0) {
                        filter.set(i);
                    }
                }
            }
            return filter;
        }

        public int orbitSize() {
            return v / 2;
        }

        public boolean infinity() {
            return v % 2 == 1;
        }

        @Override
        public String toString() {
            return traceLength == 0 ? v + "-" + k : v + "-" + k + "-" + traceLength;
        }

        public Liner fromChunks(int[][][] chunks) {
            Set<BitSet> result = new HashSet<>();
            int ol = orbitSize();
            if (v % 2 == 1) {
                int div = k / traceLength;
                boolean exact = div * traceLength == k;
                if (div == 1) {
                    for (int i = 0; i < ol; i++) {
                        BitSet lBlock = new BitSet(v);
                        BitSet rBlock = new BitSet(v);
                        if (!exact) {
                            lBlock.set(v - 1);
                            rBlock.set(v - 1);
                        }
                        for (int j = 0; j < traceLength; j++) {
                            int sh = (j * ol / traceLength + i) % ol;
                            lBlock.set(sh);
                            rBlock.set(sh + ol);
                        }
                        result.add(lBlock);
                        result.add(rBlock);
                    }
                }
                if (div == 2) {
                    for (int i = 0; i < ol; i++) {
                        BitSet block = new BitSet(v);
                        if (!exact) {
                            block.set(v - 1);
                        }
                        for (int j = 0; j < traceLength; j++) {
                            int sh = (j * ol / traceLength + i) % ol;
                            block.set(sh);
                            block.set(sh + ol);
                        }
                        result.add(block);
                    }
                }
                if (exact) {
                    for (int i = 0; i < ol; i++) {
                        BitSet lBlock = new BitSet(v);
                        BitSet rBlock = new BitSet(v);
                        lBlock.set(v - 1);
                        rBlock.set(v - 1);
                        for (int j = 0; j < k - 1; j++) {
                            int sh = (j * ol / (k - 1) + i) % ol;
                            lBlock.set(sh);
                            rBlock.set(sh + ol);
                        }
                        result.add(lBlock);
                        result.add(rBlock);
                    }
                }
            } else {
                if (traceLength != 0) {
                    if (traceLength == k) {
                        for (int i = 0; i < ol; i++) {
                            BitSet lBlock = new BitSet(v);
                            BitSet rBlock = new BitSet(v);
                            for (int j = 0; j < traceLength; j++) {
                                int sh = (j * ol / traceLength + i) % ol;
                                lBlock.set(sh);
                                rBlock.set(sh + ol);
                            }
                            result.add(lBlock);
                            result.add(rBlock);
                        }
                    }
                    if (traceLength == k / 2) {
                        for (int i = 0; i < ol; i++) {
                            BitSet block = new BitSet(v);
                            for (int j = 0; j < traceLength; j++) {
                                int sh = (j * ol / traceLength + i) % ol;
                                block.set(sh);
                                block.set(sh + ol);
                            }
                            result.add(block);
                        }
                    }
                }
            }
            for (int[][] chunk : chunks) {
                int[] left = chunk[0];
                int[] right = chunk[1];
                for (int i = 0; i < ol; i++) {
                    BitSet block = new BitSet(v);
                    for (int l : left) {
                        block.set((l + i) % ol);
                    }
                    for (int r : right) {
                        block.set((r + i) % ol + ol);
                    }
                    result.add(block);
                }
            }
            return new Liner(result.toArray(BitSet[]::new));
        }
    }

    @Test
    public void testOrbitConfig() {
        OrbitConfig oc = new OrbitConfig(16, 4, 4);
        assertEquals(FixBS.of(8), oc.outerFilter());
        assertEquals(FixBS.of(8, 2, 4, 6), oc.innerFilter());
        OrbitConfig oc1 = new OrbitConfig(16, 4, 2);
        assertEquals(FixBS.of(8, 0, 4), oc1.outerFilter());
        assertEquals(FixBS.of(8, 4), oc1.innerFilter());
        OrbitConfig oc2 = new OrbitConfig(91, 6, 0);
        assertEquals(FixBS.of(45), oc2.outerFilter());
        assertEquals(FixBS.of(45, 9, 18, 27, 36), oc2.innerFilter());
        OrbitConfig oc3 = new OrbitConfig(91, 7, 3);
        assertEquals(FixBS.of(45, 0, 15, 30), oc3.outerFilter());
        assertEquals(FixBS.of(45, 15, 30), oc3.innerFilter());
        OrbitConfig oc4 = new OrbitConfig(133, 7, 6);
        assertEquals(FixBS.of(66), oc4.outerFilter());
        assertEquals(FixBS.of(66, 11, 22, 33, 44, 55), oc4.innerFilter());
        OrbitConfig oc5 = new OrbitConfig(65, 5, 2);
        assertEquals(FixBS.of(32, 0, 16), oc5.outerFilter());
        assertEquals(FixBS.of(32, 16), oc5.innerFilter());
        OrbitConfig oc6 = new OrbitConfig(25, 4, 2);
        assertEquals(FixBS.of(12, 0, 6), oc6.outerFilter());
        assertEquals(FixBS.of(12, 4, 6, 8), oc6.innerFilter());
        OrbitConfig oc7 = new OrbitConfig(91, 6, 3);
        assertEquals(FixBS.of(45, 0, 15, 30), oc7.outerFilter());
        assertEquals(FixBS.of(45, 9, 15, 18, 27, 30, 36), oc7.innerFilter());
        OrbitConfig oc8 = new OrbitConfig(113, 8, 4);
        assertEquals(FixBS.of(56, 0, 14, 28, 42), oc8.outerFilter());
        assertEquals(FixBS.of(56, 8, 14, 16, 24, 28, 32, 40, 42, 48), oc8.innerFilter());
        OrbitConfig oc9 = new OrbitConfig(176, 8, 4);
        assertEquals(FixBS.of(88, 0, 22, 44, 66), oc9.outerFilter());
        assertEquals(FixBS.of(88, 22, 44, 66), oc9.innerFilter());
        OrbitConfig oc10 = new OrbitConfig(176, 8, 8);
        assertEquals(FixBS.of(88), oc10.outerFilter());
        assertEquals(FixBS.of(88, 11, 22, 33, 44, 55, 66, 77), oc10.innerFilter());
        OrbitConfig oc11 = new OrbitConfig(169, 8, 4);
        assertEquals(FixBS.of(84, 0, 21, 42, 63), oc11.outerFilter());
        assertEquals(FixBS.of(84, 12, 21, 24, 36, 42, 48, 60, 63, 72), oc11.innerFilter());
        OrbitConfig oc12 = new OrbitConfig(25, 4, 4);
        assertEquals(FixBS.of(12), oc12.outerFilter());
        assertEquals(FixBS.of(12, 3, 4, 6, 8, 9), oc12.innerFilter());
    }

    @Test
    public void inspect() throws IOException {
        OrbitConfig conf = new OrbitConfig(106, 6, 0);
        ObjectMapper om = new ObjectMapper();
        Map<FixBS, int[][][]> map = new HashMap<>();
        Files.lines(Path.of("/home/ihromant/maths/g-spaces/chunks", conf + "all.txt")).parallel().forEach(l -> {
            if (!l.contains("[[[")) {
                return;
            }
            int[][][] chunks = om.readValue(l, int[][][].class);
            Liner lnr = conf.fromChunks(chunks);
            map.putIfAbsent(lnr.getCanonicalOld(), chunks);
        });
        System.out.println(map.size());
        new ArrayList<>(map.values()).stream().parallel().forEach(ch -> {
            Liner lnr = conf.fromChunks(ch);
            System.out.println(lnr.autCountOld() + " " + lnr.hyperbolicFreq() + " " + Arrays.deepToString(ch));
        });
    }

    @Test
    public void calculateFileAlt() throws IOException {
        OrbitConfig conf = new OrbitConfig(96, 6, 6);
        ObjectMapper om = new ObjectMapper();
        File f = new File("/home/ihromant/maths/g-spaces/chunks", conf + "alt.txt");
        File beg = new File("/home/ihromant/maths/g-spaces/chunks", conf + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream allFis = new FileInputStream(beg);
             InputStreamReader allIsr = new InputStreamReader(allFis);
             BufferedReader allBr = new BufferedReader(allIsr);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<ArrWrap> set = allBr.lines().map(s -> new ArrWrap(om.readValue(s, int[][].class))).collect(Collectors.toSet());
            br.lines().forEach(l -> {
                if (l.contains("[[[")) {
                    int[][][] design = om.readValue(l, int[][][].class);
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                } else {
                    set.remove(new ArrWrap(om.readValue(l, int[][].class)));
                }
            });
            Map<FixBS, List<RightState>> suitables = new ConcurrentHashMap<>();
            AtomicInteger ai = new AtomicInteger();
            ChunkCallback cb = new ChunkCallback() {
                @Override
                public void onDesign(int[][][] design) {
                    Liner liner = conf.fromChunks(design);
                    System.out.println(liner.hyperbolicFreq() + " " + Arrays.deepToString(design));
                    ps.println(Arrays.deepToString(design));
                    ps.flush();
                }

                @Override
                public void onFinish(int[][] chunk) {
                    ps.println(Arrays.deepToString(chunk));
                    ps.flush();
                    int val = ai.incrementAndGet();
                    if (val % 100 == 0) {
                        System.out.println(val);
                    }
                }
            };
            FixBS innerFilter = conf.innerFilter();
            FixBS outerFilter = conf.outerFilter();
            Function<FixBS, List<RightState>> calc = fbs -> suitables.computeIfAbsent(fbs, key -> {
                List<RightState> result = new ArrayList<>();
                Predicate<RightState[]> cons = arr -> {
                    RightState st = arr[0];
                    st.filter.andNot(innerFilter);
                    st.outerFilter.andNot(outerFilter);
                    result.add(st);
                    return true;
                };
                int os = conf.orbitSize();
                FixBS wl = new FixBS(os);
                wl.set(0, os);
                for (int el = fbs.nextSetBit(0); el >= 0; el = fbs.nextSetBit(el + 1)) {
                    wl.diffModuleShifted(outerFilter, os, os - el);
                }
                RightState state = new RightState(new IntList(conf.k()), innerFilter, outerFilter, wl, 0);
                find(new int[][]{fbs.toArray()}, new RightState[1], state, conf, cons);
                return result;
            });
            calculateAlt(set.stream().map(aw -> Arrays.stream(aw.arr())
                    .map(bl -> FixBS.of(conf.v(), bl)).toList()).collect(Collectors.toList()), calc, conf, cb);
        }
    }

    private static void calculateAlt(List<List<FixBS>> lefts, Function<FixBS, List<RightState>> calc, OrbitConfig conf, ChunkCallback cb) {
        System.out.println("Lefts size: " + lefts.size() + " for conf " + conf);
        lefts.stream().parallel().forEach(left -> {
            int ll = left.size();
            Predicate<RightState[]> cons = arr -> {
                if (arr[ll - 1] == null) {
                    return false;
                }
                int[][][] res = IntStream.range(0, ll).mapToObj(i -> new int[][]{left.get(i).toArray(), arr[i].block.toArray()}).toArray(int[][][]::new);
                cb.onDesign(res);
                return true;
            };
            FixBS fstLeft = left.getFirst();
            FixBS innerFilter = conf.innerFilter();
            FixBS outerFilter = conf.outerFilter();
            for (RightState st : calc.apply(fstLeft)) {
                RightState[] rights = new RightState[ll];
                rights[0] = st;
                findAlt(left, rights, 1, innerFilter.union(st.filter), outerFilter.union(st.outerFilter), calc, cons);
            }
            cb.onFinish(left.stream().map(FixBS::toArray).toArray(int[][]::new));
        });
    }

    private static void findAlt(List<FixBS> lefts, RightState[] rights, int idx, FixBS innerFilter, FixBS outerFilter,
                                Function<FixBS, List<RightState>> calc, Predicate<RightState[]> cons) {
        if (cons.test(rights)) {
            return;
        }
        for (RightState st : calc.apply(lefts.get(idx))) {
            if (st.filter.intersects(innerFilter) || st.outerFilter.intersects(outerFilter)) {
                continue;
            }
            RightState[] nextRights = rights.clone();
            nextRights[idx] = st;
            findAlt(lefts, nextRights, idx + 1, innerFilter.union(st.filter), outerFilter.union(st.outerFilter), calc, cons);
        }
    }

    @Test
    public void observe() {
        OrbitConfig conf = new OrbitConfig(96, 6, 6);
        int ol = conf.orbitSize();
        int[][][] chunks = new ObjectMapper().readValue("[[[0, 1, 3, 13, 28], [0]], [[0, 4, 11], [17, 36, 38]], [[0, 5, 19], [1, 24, 42]], [[0, 6], [8, 9, 18, 22]], [[0, 9, 26], [4, 7, 40]], [[0, 18], [11, 28, 33, 39]]]", int[][][].class);
        System.out.println(conf.fromChunks(chunks).hyperbolicFreq() + " " + Arrays.deepToString(chunks));
        int[][][] swap = Arrays.stream(chunks).map(arr -> new int[][]{arr[1], arr[0]}).toArray(int[][][]::new);
        System.out.println(conf.fromChunks(swap).hyperbolicFreq() + " " + Arrays.deepToString(swap));
        int[][][] reorder = Arrays.stream(swap).map(arr -> {
            int min = 0;
            int[] minArr = arr[0];
            for (int i = 1; i < ol; i++) {
                int[] newArr = new int[minArr.length];
                for (int j = 0; j < minArr.length; j++) {
                    newArr[j] = (ol + arr[0][j] - i) % ol;
                }
                Arrays.sort(newArr);
                if (Combinatorics.compareArr(newArr, minArr) < 0) {
                    min = i;
                    minArr = newArr;
                }
            }
            int[] left = new int[arr[0].length];
            int[] right = new int[arr[1].length];
            for (int i = 0; i < left.length; i++) {
                left[i] = (ol + arr[0][i] - min) % ol;
            }
            for (int i = 0; i < right.length; i++) {
                right[i] = (ol + arr[1][i] - min) % ol;
            }
            Arrays.sort(left);
            Arrays.sort(right);
            return new int[][]{left, right};
        }).toArray(int[][][]::new);
        System.out.println(conf.fromChunks(reorder).hyperbolicFreq() + " " + Arrays.deepToString(reorder));
        int[][][] sorted = Arrays.stream(reorder).sorted(Comparator.comparing(arr -> FixBS.of(ol, arr[0]))).toArray(int[][][]::new);
        System.out.println(conf.fromChunks(sorted).hyperbolicFreq() + " " + Arrays.deepToString(sorted));
        int[][] lefts = Arrays.stream(sorted).map(arr -> arr[0]).toArray(int[][]::new);
        int minMul = 1;
        FixBS[] minLefts = Arrays.stream(lefts).map(arr -> FixBS.of(ol, arr)).toArray(FixBS[]::new);
        for (int mul : Combinatorics.multipliers(ol)) {
            int[][] nextLefts = new int[minLefts.length][];
            for (int i = 0; i < lefts.length; i++) {
                int[] oldLeft = lefts[i];
                nextLefts[i] = new int[oldLeft.length];
                for (int j = 0; j < oldLeft.length; j++) {
                    nextLefts[i][j] = (mul * oldLeft[j]) % ol;
                }
                Arrays.sort(nextLefts[i]);
                int[] minLeft = nextLefts[i];
                for (int j : nextLefts[i]) {
                    int[] newArr = new int[nextLefts[i].length];
                    for (int k = 0; k < minLeft.length; k++) {
                        newArr[k] = (ol + nextLefts[i][k] - j) % ol;
                    }
                    Arrays.sort(newArr);
                    if (Combinatorics.compareArr(newArr, minLeft) < 0) {
                        minLeft = newArr;
                    }
                }
                nextLefts[i] = minLeft;
            }
            FixBS[] alt = Arrays.stream(nextLefts).map(arr -> FixBS.of(ol, arr)).sorted().toArray(FixBS[]::new);
            if (Combinatorics.compareArr(alt, minLefts, Comparator.naturalOrder()) < 0) {
                minMul = mul;
                minLefts = alt;
            }
        }
        System.out.println(minMul);
    }
}
