package ua.ihromant.mathutils;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class BibdFinder3Test {
    private static final int[] bounds = {-1, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int v, int k, int needed, FixBS filter, FixBS whiteList,
                                   int[] tuple, Consumer<int[]> sink) {
        int tl = tuple.length;
        int lastVal = tuple[tl - 1];
        int second = tuple[1];
        int unset = k - tl;
        int smallest = second - unset;
        int spaceMax = smallest * unset + unset * (unset - 1) / 2;
        int sp = v - lastVal;
        int dff = sp - spaceMax;
        int min = lastVal + Math.max(1, dff);
        int max = Math.min(v - bounds[unset], lastVal + second - (tl == 2 ? 1 : 0));
        if (tl == 2) {
            max = Math.min(max, lastVal + (sp - bounds[unset - 1]) / 2);
        } else {
            max = Math.min(max, v - tuple[2] + second - bounds[unset - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
            nextTuple[tl] = idx;
            if (needed == 1) {
                sink.accept(nextTuple);
                continue;
            }
            FixBS newFilter = filter.copy();
            FixBS newWhiteList = whiteList.copy();
            for (int val : tuple) {
                int diff = idx - val;
                int outDiff = v - idx + val;
                if (outDiff % 2 == 0) {
                    newWhiteList.clear((idx + outDiff / 2) % v);
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int nv : nextTuple) {
                    newWhiteList.clear((nv + diff) % v);
                    newWhiteList.clear((nv + outDiff) % v);
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear((idx + diff) % v);
            }
            calcCycles(v, k, needed - 1, newFilter, newWhiteList, nextTuple, sink);
        }
    }

    private static void calcCycles(int v, int k, int sizeNeeded, int prev, FixBS filter, int blocksNeeded, Consumer<int[]> sink) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, v);
        int cap = v - blocksNeeded * bounds[k - 1];
        for (int idx = whiteList.nextSetBit(prev); idx >= 0 && idx < cap; idx = whiteList.nextSetBit(idx + 1)) {
            int[] arr = new int[]{0, idx};
            if (sizeNeeded == 2) {
                sink.accept(arr);
                continue;
            }
            FixBS newWhiteList = whiteList.copy();
            FixBS newFilter = filter.copy();
            int rev = v - idx;
            newWhiteList.clear(rev);
            if (rev % 2 == 0) {
                newWhiteList.clear(idx + rev / 2);
            }
            newFilter.set(idx);
            newFilter.set(rev);
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.clear((idx + diff) % v);
            }
            calcCycles(v, k, sizeNeeded - 2, newFilter, newWhiteList, arr, sink);
        }
    }

    private static void calcCyclesWithInitial(int v, int k, FixBS filter, Consumer<int[]> sink, int... initial) {
        FixBS newFilter = filter.copy();
        for (int i = 0; i < initial.length; i++) {
            int fst = initial[i];
            for (int j = i + 1; j < initial.length; j++) {
                int snd = initial[j];
                int diff = snd - fst;
                int outDiff = v - snd + fst;
                newFilter.set(diff);
                newFilter.set(outDiff);
            }
        }
        FixBS whiteList = newFilter.copy();
        whiteList.flip(1, v);
        for (int i = 0; i < initial.length; i++) {
            int fst = initial[i];
            for (int j = i + 1; j < initial.length; j++) {
                int snd = initial[j];
                int outDiff = v - snd + fst;
                if (outDiff % 2 == 0) {
                    whiteList.clear((snd + outDiff / 2) % v);
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                whiteList.clear((fst + diff) % v);
            }
        }
        calcCycles(v, k, k - initial.length, newFilter, whiteList, initial, sink);
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    @Test
    public void toFile() throws IOException {
        int v = 76;
        int k = 4;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logResults(ps, v, k, null);
        }
    }

    @Test
    public void toConsole() {
        int v = 175;
        int k = 7;
        logResults(System.out, v, k, 51);
    }

    @Test
    public void logDepth() {
        int v = 76;
        int k = 4;
        int depth = 3;
        limitCores(() -> logResultsDepth(System.out, v, k, depth, Set.of()));
    }

    @Test
    public void withDepth() throws IOException {
        int v = 76;
        int k = 4;
        int depth = 3;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + "-" + depth + "t.txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            Set<FixBS> set = br.lines().<FixBS>mapMulti((l, sink) -> {
                if (l.length() > 20) {
                    System.out.println(l);
                } else {
                    sink.accept(of(v, Arrays.stream(l.substring(1, l.length() - 1).split(", "))
                            .mapToInt(Integer::parseInt).toArray()));
                }
            }).collect(Collectors.toSet());
            limitCores(() -> logResultsDepth(ps, v, k, depth, set));
        }
    }

    private static void limitCores(Runnable exec) {
        try (ForkJoinPool ex = new ForkJoinPool(22)) {
            ex.submit(exec);
        }
    }

    private static void logResultsDepth(PrintStream destination, int v, int k, int depth, Set<FixBS> processed) {
        System.out.println(v + " " + k);
        int blocksNeeded = v / k / (k - 1);
        FixBS filter = baseFilter(v, k);
        List<int[]> initial = new ArrayList<>();
        calcCycles(v, k, depth, start(v, k), filter, blocksNeeded, arr -> {
            if (!processed.contains(of(v, arr))) {
                initial.add(arr);
            }
        });
        System.out.println("Initial depth " + depth + " and size " + initial.size());
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.deepToString(design));
            }
        };
        initial.stream().parallel().forEach(init -> {
            allDifferenceSets(v, k, new int[0][], blocksNeeded, filter, designConsumer, init);
            destination.println(Arrays.toString(init));
            destination.flush();
            if (destination != System.out) {
                System.out.println(Arrays.toString(init));
            }
        });
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
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
                                          Consumer<int[][]> designSink, int... initial) {
        int cl = curr.length;
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
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter, designSink);
        };
        if (initial.length > 0) {
            calcCyclesWithInitial(variants, k, filter, blockSink, initial);
        } else {
            int prev = cl == 0 ? start(variants, k) : filter.nextClearBit(curr[cl - 1][1] + 1);
            calcCycles(variants, k, k, prev, filter, needed, blockSink);
        }
    }

    @Test // [[0, 68, 69, 105, 135, 156, 160], [0, 75, 86, 113, 159, 183, 203], [0, 80, 95, 98, 145, 158, 201], [0, 101, 134, 141, 143, 153, 182], [0, 110, 115, 132, 138, 164, 209]]
    public void byHint() {
        findByHint(new int[]{0, 68, 69, 105, 135, 156, 160}, 217, 7, 4);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[] hint, int v, int k, int depth) {
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
        int blocksNeeded = v / k / (k - 1) - 1;
        List<int[]> initial = new ArrayList<>();
        calcCycles(v, k, depth, hint[1], filter, blocksNeeded, initial::add);
        System.out.println("Initial depth " + depth + " and size " + initial.size());
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        initial.stream().parallel().forEach(init -> {
            allDifferenceSets(v, k, new int[][]{hint}, blocksNeeded, filter, designConsumer, init);
            System.out.println(Arrays.toString(init));
        });
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static FixBS baseFilter(int v, int k) {
        FixBS filter = new FixBS(v);
        if (v % k == 0) {
            for (int i = 1; i < k; i++) {
                filter.set(i * v / k);
            }
        }
        return filter;
    }

    @Test
    public void cyclesToConsole() {
        int v = 91;
        int k = 6;
        logCycles(System.out, v, k);
    }

    @Test
    public void cyclesToFile() throws IOException {
        int v = 76;
        int k = 4;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + "r.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            logCycles(ps, v, k);
        }
    }

    private void logCycles(PrintStream ps, int v, int k) {
        int prev = start(v, k);
        FixBS filter = baseFilter(v, k);
        Map<FixBS, FixBS> map = new ConcurrentHashMap<>();
        calcCycles(v, k, k, prev, filter, 1, arr -> {
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
        Range ranges = calcRanges(v, k, pairs);
        ps.println(v + " " + k);
        new Search(v, k, pairs, idxes, ranges).search(des -> {
            ps.println(Arrays.deepToString(des));
            if (k > 5) {
                ps.flush();
            }
        });
    }

    private static int[][] calcIdxes(int v, int k, DiffPair[] pairs) {
        int vk = v / (k - 1);
        int[][] idxes = new int[vk][vk];
        for (int i = 1; i < idxes.length + 1; i++) {
            int[] arr = idxes[i - 1];
            arr[0] = i == 1 ? 0 : idxes[i - 2][vk - 1];
            for (int j = 1; j < arr.length; j++) {
                FixBS top = of(v, new int[]{i, i + j, v - 1});
                arr[j] = -Arrays.binarySearch(pairs, arr[j - 1], pairs.length, new DiffPair(top, null), Comparator.comparing(DiffPair::diff).reversed()) - 1;
            }
        }
        return idxes;
    }

    private static Range calcRanges(int v, int depth, DiffPair[] pairs) {
        Range result = new Range().setDepth(depth).setMap(new HashMap<>());
        FixBS prev = first(pairs[0].diff, v, depth);
        int prevIdx = 0;
        int idx = 0;
        while (++idx < pairs.length) {
            FixBS next = first(pairs[idx].diff, v, depth);
            if (!next.equals(prev)) {
                updateMap(result, v, prev, new Range().setMin(prevIdx).setMax(idx));
                prevIdx = idx;
                prev = next;
            }
        }
        updateMap(result, v, prev, new Range().setMin(prevIdx).setMax(pairs.length));
        return result;
    }

    @Getter
    @Setter
    @Accessors(chain = true)
    private static class Range {
        private int min = Integer.MAX_VALUE;
        private int max = Integer.MIN_VALUE;
        private int depth;
        private Map<Integer, Range> map;
    }

    private static void updateMap(Range base, int v, FixBS bs, Range range) {
        for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i + 1)) {
            if (base.min > i) {
                base.min = i;
            }
            if (base.max < i) {
                base.max = i;
            }
            int d = base.depth - 1;
            base = base.map.computeIfAbsent(i, k -> new Range().setMap(new HashMap<>()).setDepth(d));
        }
        base.map = null;
        base.min = range.min;
        base.max = range.max;
    }

    private static FixBS first(FixBS bs, int v, int depth) {
        FixBS res = new FixBS(v);
        int val = bs.nextSetBit(0);
        while (depth-- > 0) {
            res.set(val);
            val = bs.nextSetBit(val + 1);
        }
        return res;
    }

    private static FixBS of(int v, int[] tuple) {
        FixBS res = new FixBS(v);
        for (int i : tuple) {
            res.set(i);
        }
        return res;
    }

    private record DiffPair(FixBS diff, FixBS tuple) {}

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

    private record Search(int v, int k, DiffPair[] pairs, int[][] idxes, Range ranges) {
        private void search(Consumer<FixBS[]> designSink) {
            FixBS filter = baseFilter(v, k);
            int needed = v / k / (k - 1);
            int vk = v / (k - 1);
            System.out.println(idxes[1][0]);
            Arrays.stream(pairs, 0, idxes[1][0]).parallel().forEach(dp -> {
                FixBS nextFilter = filter.copy();
                nextFilter.or(dp.diff);
                FixBS[] next = new FixBS[needed];
                next[0] = dp.tuple;
                search(nextFilter, needed - 1, vk, next, designSink);
            });
        }

        private void searchAlt(FixBS filter, int needed, int vk, FixBS[] curr, Consumer<FixBS[]> designSink) {
            Range v = ranges.map.get(filter.nextClearBit(1));
            if (v == null) {
                return;
            }
            searchAlt(filter, needed, vk, curr, v, designSink);
        }

        private void searchAlt(FixBS filter, int needed, int vk, FixBS[] curr, Range range, Consumer<FixBS[]> designSink) {
            for (int unMapped = range.min; unMapped <= range.max; unMapped++) {
                Range r = range.map.get(unMapped);
                if (r == null) {
                    continue;
                }
                if (r.depth == 0) {
                    searchRange(filter, needed, vk, curr, designSink, r.min, r.max);
                } else {
                    searchAlt(filter, needed, vk, curr, r, designSink);
                }
            }
        }

        private void search(FixBS filter, int needed, int vk, FixBS[] curr, Consumer<FixBS[]> designSink) {
            int unMapped = filter.nextClearBit(1);
            if (unMapped > idxes.length) {
                return;
            }
            for (int i = filter.nextClearBit(unMapped + 1); i < unMapped + vk; i = filter.nextClearBit(i + 1)) {
                int lowIdx = idxes[unMapped - 1][i - unMapped - 1];
                int hiIdx = idxes[unMapped - 1][i - unMapped];
                searchRange(filter, needed, vk, curr, designSink, lowIdx, hiIdx);
            }
        }

        private void searchRange(FixBS filter, int needed, int vk, FixBS[] curr, Consumer<FixBS[]> designSink, int lowIdx, int hiIdx) {
            Arrays.stream(pairs, lowIdx, hiIdx).filter(dp -> !dp.diff.intersects(filter)).forEach(dp -> {
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
                    search(nextFilter, needed - 1, vk, next, designSink);
                }
            });
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
