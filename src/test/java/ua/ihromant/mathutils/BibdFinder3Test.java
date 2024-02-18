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
                                   BitSet tuple, Consumer<BitSet> sink) {
        int tLength = tuple.length();
        int second = tuple.nextSetBit(1);
        int third = tuple.nextSetBit(second + 1);
        int min = needed == 1 ? Math.max(variants - second + 1, tLength) : tLength;
        int max = Math.min(variants - bounds[needed], tLength + second - 1);
        if (third < 0) {
            if (needed == 1) {
                max = Math.min(max, (variants + second) / 2 + 1);
            }
        } else {
            max = Math.min(max, variants - third + second - bounds[needed - 1]);
        }
        for (int idx = whiteList.nextSetBit(min); idx >= 0 && idx < max; idx = whiteList.nextSetBit(idx + 1)) {
            BitSet nextTuple = (BitSet) tuple.clone();
            nextTuple.set(idx);
            if (needed == 1) {
                sink.accept(nextTuple);
                continue;
            }
            BitSet newFilter = (BitSet) filter.clone();
            BitSet newWhiteList = (BitSet) whiteList.clone();
            for (int val = tuple.nextSetBit(0); val >= 0; val = tuple.nextSetBit(val + 1)) {
                int diff = idx - val;
                int outDiff = variants - idx + val;
                if (outDiff % 2 == 0) {
                    newWhiteList.set((idx + outDiff / 2) % variants, false);
                }
                newFilter.set(diff);
                newFilter.set(outDiff);
                for (int nv = nextTuple.nextSetBit(0); nv >= 0; nv = nextTuple.nextSetBit(nv + 1)) {
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

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        for (int i = block.nextSetBit(0); i >= 0; i = block.nextSetBit(i + 1)) {
            for (int j = block.nextSetBit(i + 1); j >= 0; j = block.nextSetBit(j + 1)) {
                result.set(j - i);
                result.set(v - j + i);
            }
        }
        return result;
    }

    private static void calcCycles(int variants, int size, int prev, BitSet filter, int blocksNeeded, Consumer<BitSet> sink) {
        BitSet whiteList = (BitSet) filter.clone();
        whiteList.flip(1, variants);
        IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get).parallel().forEach(idx -> {
            BitSet newWhiteList = (BitSet) whiteList.clone();
            BitSet newFilter = (BitSet) filter.clone();
            BitSet block = of(0, idx);
            newWhiteList.set(idx, false);
            newWhiteList.set(variants - idx, false);
            if (idx % 2 == 0) {
                newWhiteList.set(idx / 2, false);
            }
            int rev = variants - idx;
            if (rev % 2 == 0) {
                newWhiteList.set(idx + rev / 2, false);
            }
            newFilter.set(idx <= (variants + 1) / 2 ? idx : rev);
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.set((idx + diff) % variants, false);
                newWhiteList.set((idx + variants - diff) % variants, false);
            }
            calcCycles(variants, size - 2, newFilter, newWhiteList, block, sink);
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
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        long time = System.currentTimeMillis();
        Consumer<BitSet[]> designConsumer = design -> {
            counter.incrementAndGet();
            System.out.println(Arrays.toString(design));
        };
        allDifferenceSets(v, k, new BitSet[0], v / k / (k - 1), filter, designConsumer);
        System.out.println("Results: " + counter.get() + ", time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, BitSet[] curr, int needed, BitSet filter,
                                          Consumer<BitSet[]> designSink) {
        int prev = curr.length == 0 ? start(variants, k) : IntStream.range(curr[curr.length - 1].nextSetBit(1) + 1, variants)
                .filter(i -> !filter.get(i)).findFirst().orElse(variants);
        Consumer<BitSet> blockSink = block -> {
            BitSet[] nextCurr = new BitSet[curr.length + 1];
            System.arraycopy(curr, 0, nextCurr, 0, curr.length);
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
