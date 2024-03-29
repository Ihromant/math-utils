package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibdFinder1Test {
    private static final int[] bounds = {0, 0, 2, 5, 10, 16, 24, 33, 43, 54, 71, 84, 105, 126};
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int max, int prev, int needed,
                                                                BitSet filter, BitSet blackList, BitSet tuple, int blocksNeeded) {
        int tLength = tuple.length();
        return IntStream.range(tLength == 1 ? prev : needed == 1 ? Math.max(variants - max + 1, tLength) : tLength,
                        tLength == 1 ? variants - blocksNeeded * bounds[needed] : Math.min(variants - bounds[needed], tLength + max - 1))
                .filter(idx -> !blackList.get(idx))
                .boxed().mapMulti((idx, sink) -> {
                    BitSet nextTuple = (BitSet) tuple.clone();
                    nextTuple.set(idx);
                    if (needed == 1) {
                        sink.accept(Map.entry(diff(nextTuple, variants), nextTuple));
                        return;
                    }
                    BitSet newFilter = (BitSet) filter.clone();
                    BitSet newBlackList = (BitSet) blackList.clone();
                    for (int val = tuple.nextSetBit(0); val >= 0; val = tuple.nextSetBit(val + 1)) {
                        int mid = val + idx;
                        int outMid = val + variants - idx;
                        if (mid % 2 == 0) {
                            newBlackList.set(mid / 2);
                        }
                        if (outMid % 2 == 0) {
                            newBlackList.set((idx + outMid / 2) % variants);
                        }
                        int diff = diff(val, idx, variants);
                        newFilter.set(diff);
                        for (int nv = nextTuple.nextSetBit(0); nv >= 0; nv = nextTuple.nextSetBit(nv + 1)) {
                            newBlackList.set((nv + diff) % variants);
                            newBlackList.set((nv + variants - diff) % variants);
                        }
                    }
                    for (int diff = newFilter.nextSetBit(0); diff >= 0; diff = newFilter.nextSetBit(diff + 1)) {
                        newBlackList.set((idx + diff) % variants);
                        newBlackList.set((idx + variants - diff) % variants);
                    }
                    calcCycles(variants, tLength == 1 ? idx : max, prev, needed - 1, newFilter, newBlackList, nextTuple, blocksNeeded).forEach(sink);
                    if (tLength == 1 && filter.cardinality() <= needed) {
                        System.out.println(idx);
                    }
        });
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

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, int prev, BitSet filter, int blocksNeeded) {
        Set<BitSet> dedup = ConcurrentHashMap.newKeySet();
        BitSet blackList = new BitSet(variants);
        for (int i = filter.nextSetBit(1); i >= 0; i = filter.nextSetBit(i + 1)) {
            blackList.set(i);
            blackList.set(variants - i);
        }
        return calcCycles(variants, 0, prev, size - 1, filter, blackList, of(0), blocksNeeded).filter(e -> dedup.add(e.getKey()));
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    @Test
    public void testDiffSets() {
        int v = 120;
        int k = 8;
        long time = System.currentTimeMillis();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        System.out.println(calcCycles(v, k, 242, filter, 1)
                .peek(System.out::println)
                .count() + " " + (System.currentTimeMillis() - time));
    }

    @Test
    public void findByHints() throws IOException {
        try (FileInputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "hint.txt"));
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(" ");
                int v = Integer.parseInt(parts[3]);
                int k = Integer.parseInt(parts[0]);
                System.out.println(line);
                while (!(line = br.readLine()).isEmpty()) {
                    BitSet hint = Arrays.stream(line.replace("{", "").replace("}", "").split(", "))
                            .mapToInt(Integer::parseInt).collect(BitSet::new, BitSet::set, BitSet::or);
                    System.out.println("Hint: " + hint);
                    findByHint(hint, v, k);
                }
                System.out.println();
            }
        }
    }

    @Test
    public void find() {
        //findByHint(of(0, 68, 69, 105, 135, 156, 160), 217, 7);
        findByHint(of(0, 41, 51, 68, 69, 72, 84), 91, 7);
    }

    private static void findByHint(BitSet hint, int v, int k) {
        System.out.println(v + " " + k + " " + hint);
        BitSet diff = diff(hint, v);
        SequencedMap<BitSet, BitSet> curr = new LinkedHashMap<>();
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        filter.or(diff);
        assertEquals((v % k == 0 ? k / 2 + 1 : 0) + k * (k - 1) / 2, filter.cardinality());
        long time = System.currentTimeMillis();
        System.out.println(allDifferenceSets(v, k, curr, (v / k / (k - 1)) - 1, filter).filter(res -> dedup.add(res.keySet()))
                .peek(res -> System.out.println(res.values()))
                .count() + " " + (System.currentTimeMillis() - time));
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
        System.out.println(allDifferenceSets(v, k, curr, v / k / (k - 1), filter).filter(res -> dedup.add(res.keySet()))
                .peek(System.out::println)
                .count() + " " + (System.currentTimeMillis() - time));
    }

    private static Stream<Map<BitSet, BitSet>> allDifferenceSets(int variants, int k, SequencedMap<BitSet, BitSet> curr, int needed, BitSet filter) {
        int half = variants / 2;
        int prev = curr.isEmpty() ? start(variants, k) : IntStream.range(curr.lastEntry().getValue().nextSetBit(1) + 1, variants)
                .filter(i -> i > half ? !filter.get(variants - i) : !filter.get(i)).findFirst().orElse(variants);
        return (needed > 1 ?
                calcCycles(variants, k, prev, filter, needed).parallel()
                : calcCycles(variants, k, prev, filter, needed)).mapMulti((pair, sink) -> {
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
}
