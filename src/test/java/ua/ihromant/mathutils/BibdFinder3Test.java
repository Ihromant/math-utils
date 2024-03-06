package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BibdFinder3Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int variants, int needed, BitSet filter, BitSet whiteList,
                                   int[] tuple, Consumer<int[]> sink) {
        int tl = tuple.length;
        int last = tuple[tl - 1];
        int second = tuple[1];
        int min = needed == 1 ? Math.max(variants - second + 1, last + 1) : last + 1;
        int max = Math.min(variants - bounds[needed], last + second);
        if (tl < 3) {
            if (needed == 1) {
                max = Math.min(max, (variants + second) / 2 + 1);
            }
        } else {
            max = Math.min(max, variants - tuple[2] + second - bounds[needed - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tl + 1);
            nextTuple[tl] = idx;
            if (needed == 1) {
                sink.accept(nextTuple);
                continue;
            }
            BitSet newFilter = (BitSet) filter.clone();
            BitSet newWhiteList = (BitSet) whiteList.clone();
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

    private static void calcCycles(int variants, int size, int prev, BitSet filter, int blocksNeeded, Consumer<int[]> sink) {
        BitSet whiteList = (BitSet) filter.clone();
        whiteList.flip(1, variants);
        IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get).parallel().forEach(idx -> {
            BitSet newWhiteList = (BitSet) whiteList.clone();
            BitSet newFilter = (BitSet) filter.clone();
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

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    @Test
    public void toFile() throws IOException {
        int v = 105;
        int k = 5;
        File f = new File("/home/ihromant/maths/diffSets/new", k + "-" + v + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
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
        BitSet filter = v % k == 0 ? IntStream.range(1, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v);
        AtomicInteger counter = new AtomicInteger();
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            destination.println(Arrays.deepToString(design));
        };
        allDifferenceSets(v, k, new int[0][], v / k / (k - 1), filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, int[][] curr, int needed, BitSet filter,
                                          Consumer<int[][]> designSink) {
        int cl = curr.length;
        int prev = cl == 0 ? start(variants, k) : filter.nextClearBit(curr[cl - 1][1] + 1);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, cl + 1);
            nextCurr[cl] = block;
            if (needed == 1) {
                designSink.accept(nextCurr);
            }
            BitSet nextFilter = (BitSet) filter.clone();
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

    @Test
    public void byHint() {
        findByHint(new int[]{0, 68, 69, 105, 135, 156, 160}, 217, 7);
        //findByHint(new int[]{0, 34, 36, 42, 66, 71, 80}, 91, 7);
    }

    private static void findByHint(int[] hint, int v, int k) {
        System.out.println(v + " " + k + " " + Arrays.toString(hint));
        BitSet filter = v % k == 0 ? IntStream.range(1, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v);
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
        allDifferenceSets(v, k, new int[][]{hint}, v / k / (k - 1) - 1, filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }
}
