package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

public class BibdFinder4Test {
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
        IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get).forEach(idx -> {
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
        });
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
            logResults(ps, v, k);
        }
    }

    @Test
    public void toConsole() {
        int v = 91;
        int k = 6;
        logResults(System.out, v, k);
    }

    private static void logResults(PrintStream destination, int v, int k) {
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
        allDifferenceSets(v, k, new int[0][], v / k / (k - 1), filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, int[][] curr, int needed, FixBS filter,
                                          Consumer<int[][]> designSink) {
        int cl = curr.length;
        int prev = cl == 0 ? start(variants, k) : filter.nextClearBit(curr[cl - 1][1] + 1);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = block;
            if (needed == 1) {
                designSink.accept(nextCurr);
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
        calcCycles(variants, k, prev, filter, needed, blockSink);
    }

    @Test // [[0, 68, 69, 105, 135, 156, 160], [0, 75, 86, 113, 159, 183, 203], [0, 80, 95, 98, 145, 158, 201], [0, 101, 134, 141, 143, 153, 182], [0, 110, 115, 132, 138, 164, 209]]
    public void byHint() {
        findByHint(new int[][]{{0, 68, 69, 105, 135, 156, 160}, {0, 75, 86, 113, 159, 183, 203}}, 217, 7);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[][] hints, int v, int k) {
        System.out.println(v + " " + k + " " + Arrays.deepToString(hints));
        FixBS filter = baseFilter(v, k);
        for (int[] hint : hints) {
            for (int i : hint) {
                for (int j : hint) {
                    if (i >= j) {
                        continue;
                    }
                    filter.set(j - i);
                    filter.set(v - j + i);
                }
            }
        }
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        allDifferenceSets(v, k, hints, v / k / (k - 1) - hints.length, filter, designConsumer);
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
    public void randomizeDesigns() {
        int v = 126;
        int k = 6;
        int random = 2;
        int blocksNeeded = v / k / (k - 1);
        System.out.println(v + " " + k);
        FixBS filter = baseFilter(v, k);
        IntStream.range(0, 20).parallel().forEach(x -> {
            while (true) {
                FixBS ftr = randomizeHint(v, k, blocksNeeded, random, filter);
                Consumer<int[][]> designConsumer = design -> System.out.println(Arrays.deepToString(design));
                allDifferenceSets(v, k, new int[0][], blocksNeeded - random, ftr, designConsumer);
            }
        });
    }

    private static FixBS randomizeHint(int variants, int k, int blocksNeeded, int needed, FixBS filter) {
        if (needed == 0) {
            return filter;
        }
        int prev = filter.nextClearBit(start(variants, k));
        while (true) {
            int[] block = randomizeCycle(variants, k, prev, filter, blocksNeeded + needed - 2);
            if (block != null) {
                FixBS nextFilter = filter.copy();
                for (int i = 0; i < k; i++) {
                    for (int j = i + 1; j < k; j++) {
                        int l = block[j];
                        int s = block[i];
                        nextFilter.set(l - s);
                        nextFilter.set(variants - l + s);
                    }
                }
                return randomizeHint(variants, k, blocksNeeded, needed - 1, nextFilter);
            }
        }
    }

    private static int[] randomizeCycle(int variants, int size, int prev, FixBS filter, int blocksNeeded) {
        FixBS whiteList = filter.copy();
        whiteList.flip(1, variants);
        int[] arr = IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get).toArray();
        if (arr.length < 2) {
            return null;
        }
        int idx = arr[ThreadLocalRandom.current().nextInt(arr.length / 2)];
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
        return randomizeCycle(variants, size - 2, newFilter, newWhiteList, new int[]{0, idx});
    }

    private static int[] randomizeCycle(int variants, int needed, FixBS filter, FixBS whiteList, int[] tuple) {
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
        int[] arr = IntStream.range(min, max).filter(whiteList::get).toArray();
        if (arr.length == 0) {
            return null;
        }
        int idx = arr[ThreadLocalRandom.current().nextInt(arr.length)];
        int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
        nextTuple[tl] = idx;
        if (last) {
            return nextTuple;
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
        return randomizeCycle(variants, needed - 1, newFilter, newWhiteList, nextTuple);
    }
}
