package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BibdFinder3Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int variants, int needed, BitSet filter, BitSet whiteList,
                                   int[] tuple, Consumer<int[]> sink) {
        int tLength = tuple[tuple.length - 1] + 1;
        int second = tuple[1];
        int min = needed == 1 ? Math.max(variants - second + 1, tLength) : tLength;
        int max = Math.min(variants - bounds[needed], tLength + second - 1);
        if (tuple.length < 3) {
            if (needed == 1) {
                max = Math.min(max, (variants + second) / 2 + 1);
            }
        } else {
            max = Math.min(max, variants - tuple[2] + second - bounds[needed - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            int[] nextTuple = Arrays.copyOf(tuple, tuple.length + 1);
            nextTuple[tuple.length] = idx;
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
            if (tLength == 1 && filter.cardinality() <= needed) {
                System.out.println(idx);
            }
        }
    }

    private static BitSet diff(int[] block, int v) {
        BitSet result = new BitSet();
        for (int i : block) {
            for (int j : block) {
                if (i >= j) {
                    continue;
                }
                result.set(j - i);
                result.set(v - j + i);
            }
        }
        return result;
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
            if (newFilter.cardinality() <= size) {
                System.out.println(idx);
            }
        });
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    @Test
    public void testDiffFamilies() {
        int v = 91;
        int k = 6;
        System.out.println(v + " " + k);
        AtomicInteger counter = new AtomicInteger();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(1, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v);
        long time = System.currentTimeMillis();
        Consumer<int[][]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.deepToString(design));
        };
        allDifferenceSets(v, k, new int[0][], v / k / (k - 1), filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, int[][] curr, int needed, BitSet filter,
                                          Consumer<int[][]> designSink) {
        int prev = curr.length == 0 ? start(variants, k) : IntStream.range(curr[curr.length - 1][1] + 1, variants)
                .filter(i -> !filter.get(i)).findFirst().orElse(variants);
        Consumer<int[]> blockSink = block -> {
            int[][] nextCurr = Arrays.copyOf(curr, curr.length + 1);
            nextCurr[curr.length] = block;
            if (needed == 1) {
                designSink.accept(nextCurr);
            }
            BitSet nextFilter = (BitSet) filter.clone();
            nextFilter.or(diff(block, variants));
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter, designSink);
        };
        calcCycles(variants, k, prev, filter, needed, blockSink);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}
