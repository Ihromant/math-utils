package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.BitSet;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder1Test {
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int max, int prev, int needed, BitSet filter, BitSet tuple) {
        int tLength = tuple.length();
        int from = Math.max(tLength, prev);
        int tCardinality = tuple.cardinality();
        int vMax = variants - max - 1;
        return IntStream.rangeClosed(from, needed == 1 ? vMax : Math.min(vMax,
                        tLength + (variants - tLength - (1 << needed) + 1) / 2))
                .filter(idx -> !filter.get(variants - idx) && !filter.get(idx))
                .boxed().mapMulti((idx, sink) -> {
            BitSet addition = new BitSet(variants / 2 + 1);
            tuple.stream().map(t -> diff(t, idx, variants)).filter(d -> !addition.get(d) && !filter.get(d)).forEach(addition::set);
            if (addition.cardinality() != tCardinality) {
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
            calcCycles(variants, Math.max(max, idx - tLength + 1), prev, needed - 1, newFilter, nextTuple).forEach(sink);
        });
    }

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        block.stream().forEach(i -> block.stream().filter(j -> j > i).forEach(j -> result.set(diff(j, i, v))));
        return result;
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, int prev, BitSet filter) {
        Map<BitSet, BitSet> map = new HashMap<>();
        return calcCycles(variants, 0, prev, size - 1, filter, of(0)).filter(e -> map.putIfAbsent(e.getKey(), e.getValue()) == null);
    }

    @Test
    public void testDiffSets() {
        int v = 52;
        int k = 4;
        long time = System.currentTimeMillis();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        System.out.println(calcCycles(v, k, 1, filter)
                .peek(System.out::println)
                .count() + " " + (System.currentTimeMillis() - time));
    }

    @Test
    public void testDiffFamilies() {
        int v = 52;
        int k = 4;
        System.out.println(v + " " + k);
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        SequencedMap<BitSet, BitSet> curr = new LinkedHashMap<>();
        long time = System.currentTimeMillis();
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        System.out.println(allDifferenceSets(v, k, curr, v / k / (k - 1), filter).filter(res -> dedup.add(res.keySet()))
                .peek(System.out::println)
                .count() + " " + (System.currentTimeMillis() - time));
    }

    private static Stream<Map<BitSet, BitSet>> allDifferenceSets(int variants, int k, SequencedMap<BitSet, BitSet> curr, int needed, BitSet filter) {
        int prev = curr.isEmpty() ? 1 : IntStream.range(curr.lastEntry().getValue().stream().skip(1).findFirst().orElseThrow() + 1, variants)
                .filter(i -> !filter.get(i)).findFirst().orElseThrow();
        return (needed == variants / k / (k - 1) ?
                calcCycles(variants, k, prev, filter).parallel()
                : calcCycles(variants, k, prev, filter)).mapMulti((pair, sink) -> {
            SequencedMap<BitSet, BitSet> nextCurr = new LinkedHashMap<>(curr);
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
