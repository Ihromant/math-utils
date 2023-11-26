package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder1Test {
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int max, int needed, BitSet filter, BitSet tuple) {
        int tLength = tuple.length();
        int tCardinality = tuple.cardinality();
        int vMax = variants - max - 1;
        return IntStream.rangeClosed(tLength, needed == 1 ? vMax : Math.min(vMax,
                        tLength + (variants - tLength - (1 << needed) + 1) / 2))
                .boxed().mapMulti((idx, sink) -> {
            BitSet addition = new BitSet(variants / 2 + 1);
            int present = tuple.stream().reduce(0, (acc, set) -> {
                int d = diff(set, idx, variants);
                addition.set(d);
                return filter.get(d) ? acc + 1 : acc;
            });
            if (present > 0 || addition.cardinality() != tCardinality) {
                return;
            }
            BitSet nextTuple = (BitSet) tuple.clone();
            nextTuple.set(idx);
            if (needed == 1) {
                sink.accept(Map.entry(diff(nextTuple, variants), nextTuple));
                return;
            }
            BitSet newFilter = (BitSet) filter.clone();
            newFilter.or(addition);
            calcCycles(variants, Math.max(max, idx - tLength + 1), needed - 1, newFilter, nextTuple).forEach(sink);
        });
    }

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        block.stream().forEach(i -> block.stream().filter(j -> j > i).forEach(j -> result.set(diff(j, i, v))));
        return result;
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, BitSet filter) {
        Map<BitSet, BitSet> map = new HashMap<>();
        return calcCycles(variants, 0, size - 1, filter, of(0)).filter(e -> map.putIfAbsent(e.getKey(), e.getValue()) == null);
    }

    @Test
    public void testDiffSets() {
        int v = 141;
        int k = 5;
        long time = System.currentTimeMillis();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        System.out.println(calcCycles(v, k, filter).count() + " " + (System.currentTimeMillis() - time));
    }

    @Test
    public void testDiffFamilies() {
        int v = 217;
        int k = 7;
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        Map<BitSet, BitSet> curr = new HashMap<>();
        if (v % k == 0) {
            curr.put(filter, IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or));
        }
        long time = System.currentTimeMillis();
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        allDifferenceSets(v, k, curr, v / k / (k - 1), filter).filter(res -> dedup.add(res.keySet())).forEach(System.out::println);
        System.out.println(System.currentTimeMillis() - time);
    }

    private static Stream<Map<BitSet, BitSet>> allDifferenceSets(int variants, int k, Map<BitSet, BitSet> curr, int needed, BitSet filter) {
        return (needed == variants / k / (k - 1) ?
                calcCycles(variants, k, filter).parallel()
                : calcCycles(variants, k, filter)).mapMulti((pair, sink) -> {
            HashMap<BitSet, BitSet> nextCurr = new HashMap<>(curr);
            nextCurr.put(pair.getKey(), pair.getValue());
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet nextFilter = (BitSet) filter.clone();
            nextFilter.or(pair.getKey());
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter).forEach(sink);
        });
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

    private static boolean isMinimal(int[] tuple, int v) {
        int l = tuple.length;
        int last = v - tuple[l - 1];
        for (int i = 1; i < l; i++) {
            if (tuple[i] - tuple[i - 1] >= last) {
                return false;
            }
        }
        return true;
    }

    private static BitSet minimalTuple(BitSet tuple, int v) {
        int[] arr = tuple.stream().toArray();
        int l = arr.length;
        int[] diffs = new int[l];
        for (int i = 0; i < l; i++) {
            diffs[i] = diff(arr[i], arr[(l + i - 1) % l], v);
        }
        int minIdx = IntStream.range(0, l).boxed().max(Comparator.comparing(i -> diffs[i])).orElseThrow();
        int val = arr[minIdx];
        return tuple.stream().map(i -> i >= val ? i - val : v + i - val).collect(BitSet::new, BitSet::set, BitSet::or);
    }
}
