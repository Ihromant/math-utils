package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class BibdFinder2Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static void calcCycles(int variants, int max, int needed, BitSet filter, BitSet whiteList,
                                   BitSet tuple, Consumer<Map.Entry<BitSet, BitSet>> sink) {
        int tLength = tuple.length();
        for (int idx = whiteList.nextSetBit(needed == 1 ? Math.max(variants - max + 1, tLength) : tLength);
             idx >= 0 && idx < Math.min(variants - bounds[needed], tLength + max - 1); idx = whiteList.nextSetBit(idx + 1)) {
            BitSet nextTuple = (BitSet) tuple.clone();
            nextTuple.set(idx);
            if (needed == 1) {
                sink.accept(Map.entry(diff(nextTuple, variants), nextTuple));
                continue;
            }
            BitSet newFilter = (BitSet) filter.clone();
            BitSet newWhiteList = (BitSet) whiteList.clone();
            for (int val = tuple.nextSetBit(0); val >= 0 ; val = tuple.nextSetBit(val + 1)) {
                int mid = val + idx;
                int outMid = val + variants - idx;
                if (mid % 2 == 0) {
                    newWhiteList.set(mid / 2, false);
                }
                if (outMid % 2 == 0) {
                    newWhiteList.set((idx + outMid / 2) % variants, false);
                }
                int diff = diff(val, idx, variants);
                newFilter.set(diff);
                for (int nv = nextTuple.nextSetBit(0); nv >= 0; nv = nextTuple.nextSetBit(nv + 1)) {
                    newWhiteList.set((nv + diff) % variants, false);
                    newWhiteList.set((nv + variants - diff) % variants, false);
                }
            }
            for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                newWhiteList.set((idx + diff) % variants, false);
                newWhiteList.set((idx + variants - diff) % variants, false);
            }
            calcCycles(variants, tLength == 1 ? idx : max, needed - 1, newFilter, newWhiteList, nextTuple, sink);
            if (tLength == 1 && filter.cardinality() <= needed) {
                System.out.println(idx);
            }
        }
    }

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        for (int i = block.nextSetBit(0); i >= 0; i = block.nextSetBit(i + 1)) {
            for (int j = block.nextSetBit(i + 1); j >= 0; j = block.nextSetBit(j + 1)) {
                result.set(diff(i, j, v));
            }
        }
        return result;
    }

    private static void calcCycles(int variants, int size, int prev, BitSet filter, int blocksNeeded, Consumer<Map.Entry<BitSet, BitSet>> sink) {
        Set<BitSet> dedup = ConcurrentHashMap.newKeySet();
        BitSet whiteList = new BitSet(variants);
        whiteList.set(1, variants);
        for (int i = filter.nextSetBit(1); i >= 0; i = filter.nextSetBit(i + 1)) {
            whiteList.set(i, false);
            whiteList.set(variants - i, false);
        }
        Consumer<Map.Entry<BitSet, BitSet>> filterSink = block -> {
            if (dedup.add(block.getKey())) {
                sink.accept(block);
            }
        };
        IntStream.range(prev, variants - blocksNeeded * bounds[size - 1]).filter(whiteList::get)
                //.parallel()
                .forEach(idx -> {
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
                    calcCycles(variants, idx, size - 2, newFilter, newWhiteList, block, filterSink);
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
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        SequencedMap<BitSet, BitSet> curr = new LinkedHashMap<>();
        long time = System.currentTimeMillis();
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        Consumer<Map<BitSet, BitSet>> designConsumer = design -> {
            if (dedup.add(design.keySet())) {
                System.out.println(design);
            }
        };
        allDifferenceSets(v, k, curr, v / k / (k - 1), filter, designConsumer);
        System.out.println("Time elapsed: " + (System.currentTimeMillis() - time));
    }

    private static void allDifferenceSets(int variants, int k, SequencedMap<BitSet, BitSet> curr, int needed, BitSet filter,
                                                                 Consumer<Map<BitSet, BitSet>> designSink) {
        int half = variants / 2;
        int prev = curr.isEmpty() ? start(variants, k) : IntStream.range(curr.lastEntry().getValue().nextSetBit(1) + 1, variants)
                .filter(i -> i > half ? !filter.get(variants - i) : !filter.get(i)).findFirst().orElse(variants);
        Consumer<Map.Entry<BitSet, BitSet>> blockSink = block -> {
            SequencedMap<BitSet, BitSet> nextCurr = new LinkedHashMap<>(curr);
            nextCurr.put(block.getKey(), block.getValue());
            if (needed == 1) {
                designSink.accept(nextCurr);
            }
            BitSet nextFilter = (BitSet) filter.clone();
            nextFilter.or(block.getKey());
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter, designSink);
        };
        calcCycles(variants, k, prev, filter, needed, blockSink);
    }

    private static int diff(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}