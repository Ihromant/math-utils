package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SequencedMap;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class BibdFinder1Test {
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int max, int prev, int needed, BitSet filter, BitSet tuple) {
        int half = variants / 2;
        int tLength = tuple.length();
        return IntStream.range(tLength == 1 ? prev : needed == 1 ? Math.max(variants - max + 1, tLength) : tLength,
                        tLength == 1 ? variants - (needed - 1) * (needed - 2) : Math.min(variants, tLength + max - 1))
                .filter(idx -> idx > half ? !filter.get(variants - idx) : !filter.get(idx))
                .boxed().mapMulti((idx, sink) -> {
                    BitSet addition = new BitSet(half + 1);
                    if (tuple.stream().map(pr -> diff(pr, idx, variants)).anyMatch(d -> {
                        boolean result = filter.get(d) || addition.get(d);
                        addition.set(d);
                        return result;
                    })) {
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
                    calcCycles(variants, tLength == 1 ? idx : max, prev, needed - 1, newFilter, nextTuple).forEach(sink);
        });
    }

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        block.stream().forEach(i -> block.stream().filter(j -> j > i).forEach(j -> result.set(diff(j, i, v))));
        return result;
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, int prev, BitSet filter) {
        Set<BitSet> dedup = new HashSet<>();
        return calcCycles(variants, 0, prev, size - 1, filter, of(0)).filter(e -> dedup.add(e.getKey()));
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }


    @Test
    public void testDiffSets() {
        int v = 64;
        int k = 4;
        long time = System.currentTimeMillis();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        System.out.println(calcCycles(v, k, start(v, k), filter)
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
        findByHint(of(0, 1, 8, 19, 59, 64), 151, 6);
    }

    private static void findByHint(BitSet hint, int v, int k) {
        BitSet diff = diff(hint, v);
        SequencedMap<BitSet, BitSet> curr = new LinkedHashMap<>();
        curr.put(diff, hint);
        Set<Set<BitSet>> dedup = ConcurrentHashMap.newKeySet();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        filter.or(diff);
        allDifferenceSets(v, k, curr, (v / k / (k - 1)) - 1, filter).filter(res -> dedup.add(res.keySet()))
                .forEach(res -> System.out.println(res.values()));
    }

    @Test
    public void testDiffFamilies() {
        int v = 45;
        int k = 3;
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
        int prev = curr.isEmpty() ? start(variants, k) : IntStream.range(curr.lastEntry().getValue().stream().skip(1).findFirst().orElseThrow() + 1, variants)
                .filter(i -> i > half ? !filter.get(variants - i) : !filter.get(i)).findFirst().orElse(variants);
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
