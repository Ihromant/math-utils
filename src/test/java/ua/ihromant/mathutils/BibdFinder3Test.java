package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BibdFinder3Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int variants, int needed, FixBS filter, FixBS whiteList,
                                   int[] tuple, Consumer<int[]> sink) {
        int tl = tuple.length;
        int lastVal = tuple[tl - 1];
        int second = tuple[1];
        boolean last = needed == 1;
        int min = last ? Math.max(variants - second + 1, lastVal + 1) : lastVal + 1;
        int max = Math.min(variants - bounds[needed], lastVal + second);
        if (tl < 3) {
            if (last) {
                max = Math.min(max, (variants + second) / 2 + 1);
            }
        } else {
            max = Math.min(max, variants - tuple[2] + second - bounds[needed - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
            nextTuple[tl] = idx;
            if (last) {
                sink.accept(nextTuple);
                continue;
            }
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            for (int val : tuple) {
                int diff = idx - val;
                int outDiff = variants - idx + val;
                if (outDiff % 2 == 0) {
                    newWhiteList.set((idx + outDiff / 2) % variants, false);
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int nv : nextTuple) {
                    newWhiteList.set((nv + diff) % variants, false);
                    newWhiteList.set((nv + outDiff) % variants, false);
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.set((idx + diff) % variants, false);
            }
            calcCycles(variants, needed - 1, newFilter, newWhiteList, nextTuple, sink);
        }
    }

    private static void calcCycles(int variants, int size, int prev, FixBS filter, int blocksNeeded, Consumer<int[]> sink) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, variants);
        IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get).parallel().forEach(idx -> {
            FixBS newWhiteList = whiteList.copy();
            FixBS newFilter = filter.copy();
            int rev = variants - idx;
            newWhiteList.set(rev, false);
            if (rev % 2 == 0) {
                newWhiteList.set(idx + rev / 2, false);
            }
            newFilter.set(idx);
            newFilter.set(rev);
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.set((idx + diff) % variants, false);
            }
            calcCycles(variants, size - 2, newFilter, newWhiteList, new int[]{0, idx}, sink);
            if (filter.cardinality() <= size) {
                System.out.println(idx);
            }
        });
    }

    private static void calcCyclesSingle(int variants, int size, int idx, FixBS filter, Consumer<int[]> sink) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, variants);
        FixBS newWhiteList = whiteList.copy();
        FixBS newFilter = filter.copy();
        int rev = variants - idx;
        newWhiteList.set(rev, false);
        if (rev % 2 == 0) {
            newWhiteList.set(idx + rev / 2, false);
        }
        newFilter.set(idx);
        newFilter.set(rev);
        for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
            newWhiteList.set((idx + diff) % variants, false);
        }
        calcCycles(variants, size - 2, newFilter, newWhiteList, new int[]{0, idx}, sink);
        if (filter.cardinality() <= size) {
            System.out.println(idx);
        }
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    @Test
    public void toFile() throws IOException {
        int v = 101;
        int k = 5;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logResults(ps, v, k, 28);
        }
    }

    @Test
    public void toConsole() {
        int v = 175;
        int k = 7;
        logResults(System.out, v, k, 32);
    }

    private static void logResults(PrintStream destination, int v, int k, Integer single) {
        if (destination != System.out) {
            System.out.println(v + " " + k);
        }
        destination.println(v + " " + k);
        FixBS filter = baseFilter(v, k);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
            destination.flush();
        };
        allDifferenceSets(v, k, new int[0][], v / k / (k - 1), filter, designConsumer, single);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, int[][] curr, int needed, FixBS filter,
                                          Consumer<int[][]> designSink, Integer single) {
        int cl = curr.length;
        int prev = cl == 0 ? start(variants, k) : filter.nextClearBit(curr[cl - 1][1] + 1);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = block;
            if (needed == 1) {
                designSink.accept(nextCurr);
                return;
            }
            FixBS nextFilter = filter.copy();
            for (int i = 0; i < k; i++) {
                for (int j = i + 1; j < k; j++) {
                    int l = block[j];
                    int s = block[i];
                    nextFilter.set(l - s);
                    nextFilter.set(variants - l + s);
                }
            }
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter, designSink, null);
        };
        if (single != null) {
            calcCyclesSingle(variants, k, single, filter, blockSink);
        } else {
            calcCycles(variants, k, prev, filter, needed, blockSink);
        }
    }

    @Test // [[0, 68, 69, 105, 135, 156, 160], [0, 75, 86, 113, 159, 183, 203], [0, 80, 95, 98, 145, 158, 201], [0, 101, 134, 141, 143, 153, 182], [0, 110, 115, 132, 138, 164, 209]]
    public void byHint() {
        findByHint(new int[]{0, 68, 69, 105, 135, 156, 160}, 217, 7);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[] hint, int v, int k) {
        System.out.println(v + " " + k + " " + Arrays.toString(hint));
        FixBS filter = baseFilter(v, k);
        for (int i : hint) {
            for (int j : hint) {
                if (i >= j) {
                    continue;
                }
                filter.set(j - i);
                filter.set(v - j + i);
            }
        }
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        allDifferenceSets(v, k, new int[][]{hint}, v / k / (k - 1) - 1, filter, designConsumer, null);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        if (v % k == 0) {
            IntStream.range(1, k).forEach(i -> filter.set(i * v / k));
        }
        return filter;
    }

    @Test
    public void cyclesToConsole() throws IOException {
        int v = 91;
        int k = 6;
        logCycles(System.out, v, k);
    }

    @Test
    public void cyclesToFile() throws IOException {
        int v = 101;
        int k = 5;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + "r.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logCycles(ps, v, k);
        }
    }

    @Test
    public void pairsToConsole() {
        int v = 121;
        int k = 6;
        logPairs(System.out, v, k);
    }

    private void logPairs(PrintStream ps, int v, int k) {
        FixBS filter = baseFilter(v, k);
        Set<FixBS> set = ConcurrentHashMap.newKeySet();
        Consumer<int[][]> designConsumer = design -> {
            DiffPair dp = diff(v, design);
            FixBS df = dp.diff();
            set.add(df);
            FixBS comp = new FixBS(v);
            comp.set(1, v);
            comp.xor(filter);
            comp.xor(df);
            if (set.contains(comp)) {
                ps.println(df);
            }
        };
        allDifferenceSets(v, k, new int[0][], 2, filter, designConsumer, null);
        System.out.println(set.size());
    }

    private void logCycles(PrintStream ps, int v, int k) throws IOException {
        int prev = start(v, k);
        FixBS filter = baseFilter(v, k);
        Map<FixBS, FixBS> map = new ConcurrentHashMap<>();
        calcCycles(v, k, prev, filter, 1, arr -> {
            DiffPair dp = diff(v, arr);
            map.put(dp.diff, dp.tuple);
        });
        System.out.println(map.size());
        DiffPair[] pairs = map.entrySet().stream().map(e -> new DiffPair(e.getKey(), e.getValue())).toArray(DiffPair[]::new);
        map.clear();
        Arrays.parallelSort(pairs, Comparator.comparing(DiffPair::diff).reversed());
        //dump(pairs, v, k);
        processPairs(ps, v, k, pairs);
    }

    private static void processPairs(PrintStream ps, int v, int k, DiffPair[] pairs) {
        int[][] idxes = calcIdxes(v, k, pairs);
        new Search(v, v / k, pairs, idxes).search(des -> {
            ps.println(Arrays.deepToString(des));
            ps.flush();
        });
    }

    private static int[][] calcIdxes(int v, int k, DiffPair[] pairs) {
        int vk = v / k;
        int[][] idxes = new int[vk][vk + 1];
        for (int i = 1; i < idxes.length + 1; i++) {
            int[] arr = idxes[i - 1];
            arr[0] = i == 1 ? 0 : idxes[i - 2][vk];
            for (int j = 1; j < arr.length; j++) {
                FixBS top = of(v, new int[]{i, i + j, v - 1});
                arr[j] = -Arrays.binarySearch(pairs, arr[j - 1], pairs.length, new DiffPair(top, null), Comparator.comparing(DiffPair::diff).reversed()) - 1;
            }
        }
        return idxes;
    }

    private static void checkGraph(DiffPair[] pairs, int[] idxes) {
        int[][] graph = new int[pairs.length][];
        IntStream.range(0, pairs.length).parallel().forEach(i -> {
            FixBS diff = pairs[i].diff;
            IntStream.Builder arr = IntStream.builder();
            for (int j = diff.nextClearBit(diff.nextSetBit(0)); j < idxes.length; j = diff.nextClearBit(j + 1)) {
                int top = idxes[j];
                for (int l = idxes[j - 1]; l < top; l++) {
                    if (!pairs[l].diff.intersects(diff)) {
                        arr.accept(l);
                    }
                }
            }
            graph[i] = arr.build().toArray();
        });
        System.out.println(Arrays.stream(graph).mapToInt(g -> g.length).sum());
    }

    private static void search(int v, int vk, DiffPair[] pairs, int[][] idxes, FixBS filter, int needed, FixBS[] curr, Consumer<FixBS[]> designSink) {
        int unMapped = filter.nextClearBit(1);
        if (unMapped >= idxes.length) {
            return;
        }
        if (curr.length == 0) {
            System.out.println(idxes[0][0] + " " + idxes[1][0]);
        }
        for (int i = filter.nextClearBit(unMapped + 1); i < unMapped + vk + 1; i = filter.nextClearBit(i + 1)) {
            int lowIdx = idxes[unMapped - 1][i - unMapped - 1];
            int hiIdx = idxes[unMapped - 1][i - unMapped];
            IntStream.range(lowIdx, hiIdx).parallel().mapToObj(idx -> pairs[idx]).forEach(dp -> {
                if (dp.diff.intersects(filter)) {
                    return;
                }
                FixBS nextFilter = filter.copy();
                nextFilter.or(dp.diff);
                if (needed == 2) {
                    nextFilter.flip(1, v);
                    int idx = Arrays.binarySearch(pairs, new DiffPair(nextFilter, null), Comparator.comparing(DiffPair::diff).reversed());
                    if (idx < 0) {
                        return;
                    }
                    FixBS[] fin = curr.clone();
                    fin[curr.length - 2] = dp.tuple;
                    fin[curr.length - 1] = pairs[idx].tuple;
                    designSink.accept(fin);
                } else {
                    FixBS[] next = curr.clone();
                    next[curr.length - needed] = dp.tuple;
                    search(v, vk, pairs, idxes, nextFilter, needed - 1, next, designSink);
                }
            });
        }
    }

    private static FixBS of(int v, int[] tuple) {
        FixBS res = new FixBS(v);
        for (int i : tuple) {
            res.set(i);
        }
        return res;
    }

    private record DiffPair(FixBS diff, FixBS tuple) {}

    private DiffPair diff(int v, int[][] tuples) {
        FixBS diff = new FixBS(v);
        FixBS tpl = new FixBS(v);
        for (int[] tuple : tuples) {
            for (int i = 0; i < tuple.length; i++) {
                int fst = tuple[i];
                for (int j = i + 1; j < tuple.length; j++) {
                    int dff = tuple[j] - fst;
                    diff.set(dff);
                    diff.set(v - dff);
                }
                tpl.set(fst);
            }
        }
        return new DiffPair(diff, tpl);
    }

    private DiffPair diff(int v, int[] tuple) {
        FixBS diff = new FixBS(v);
        FixBS tpl = new FixBS(v);
        for (int i = 0; i < tuple.length; i++) {
            int fst = tuple[i];
            for (int j = i + 1; j < tuple.length; j++) {
                int dff = tuple[j] - fst;
                diff.set(dff);
                diff.set(v - dff);
            }
            tpl.set(fst);
        }
        return new DiffPair(diff, tpl);
    }

    private record Search(int v, int vk, DiffPair[] pairs, int[][] idxes) {
        private void search(Consumer<FixBS[]> designSink) {
            int k = v / vk;
            FixBS filter = baseFilter(v, k);
            int needed = v / k / (k - 1);
            System.out.println(idxes[1][0]);
            search(filter, needed, new FixBS[needed], designSink);
        }

        private void search(FixBS filter, int needed, FixBS[] curr, Consumer<FixBS[]> designSink) {
            int unMapped = filter.nextClearBit(1);
            if (unMapped > idxes.length) {
                return;
            }
            for (int i = filter.nextClearBit(unMapped + 1); i < unMapped + vk + 1; i = filter.nextClearBit(i + 1)) {
                int lowIdx = idxes[unMapped - 1][i - unMapped - 1];
                int hiIdx = idxes[unMapped - 1][i - unMapped];
                IntStream.range(lowIdx, hiIdx).parallel().mapToObj(idx -> pairs[idx]).forEach(dp -> {
                    if (dp.diff.intersects(filter)) {
                        return;
                    }
                    FixBS nextFilter = filter.copy();
                    nextFilter.or(dp.diff);
                    if (needed == 2) {
                        nextFilter.flip(1, v);
                        int idx = Arrays.binarySearch(pairs, new DiffPair(nextFilter, null), Comparator.comparing(DiffPair::diff).reversed());
                        if (idx < 0) {
                            return;
                        }
                        FixBS[] fin = curr.clone();
                        fin[curr.length - 2] = dp.tuple;
                        fin[curr.length - 1] = pairs[idx].tuple;
                        designSink.accept(fin);
                    } else {
                        FixBS[] next = curr.clone();
                        next[curr.length - needed] = dp.tuple;
                        search(nextFilter, needed - 1, next, designSink);
                    }
                });
            }
        }
    }

    private static void dump(DiffPair[] pairs, int v, int k) throws IOException {
        File f = new File("/home/ihromant/maths/diffSets/new", "pairs" + k + "-" + v + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             DataOutputStream dos = new DataOutputStream(bos)) {
            for (DiffPair pair : pairs) {
                for (long word : pair.diff.words()) {
                    dos.writeLong(word);
                }
                for (long word : pair.tuple.words()) {
                    dos.writeLong(word);
                }
            }
        }
    }

    private static final Map<Integer, Integer> sizes = Map.of(169, 478054096, 175, 253044480);

    private static DiffPair[] read(int v, int k) throws IOException {
        File f = new File("/home/ihromant/maths/diffSets/new", "pairs" + k + "-" + v + ".txt");
        try (FileInputStream fis = new FileInputStream(f);
             BufferedInputStream bis = new BufferedInputStream(fis);
             DataInputStream dis = new DataInputStream(bis)) {
            DiffPair[] result = new DiffPair[sizes.get(v)];
            int idx = 0;
            while (idx < result.length) {
                int sz = FixBS.len(v);
                long[] diff = new long[sz];
                for (int i = 0; i < sz; i++) {
                    diff[i] = dis.readLong();
                }
                long[] tuple = new long[sz];
                for (int i = 0; i < sz; i++) {
                    tuple[i] = dis.readLong();
                }
                result[idx] = new DiffPair(new FixBS(diff), new FixBS(tuple));
                idx++;
            }
            return result;
        }
    }
}
