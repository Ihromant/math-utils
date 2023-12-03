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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BibdFinder1ExtTest {
    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int prev, int needed, BitSet filter, BitSet blackList, BitSet tuple) {
        int tLength = tuple.length();
        return IntStream.range(tLength == 1 ? prev : needed == 1 ? Math.max(variants - prev + 1, tLength) : tLength,
                        tLength == 1 ? variants - (needed - 1) * (needed - 2) : Math.min(variants, tLength + prev - 1))
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
                    tuple.stream().forEach(val -> {
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
                        nextTuple.stream().forEach(nv -> {
                            newBlackList.set((nv + diff) % variants);
                            newBlackList.set((nv + variants - diff) % variants);
                        });
                    });
                    newFilter.stream().forEach(diff -> {
                        newBlackList.set((idx + diff) % variants);
                        newBlackList.set((idx + variants - diff) % variants);
                    });
                    calcCyclesFixed(variants, tLength == 1 ? idx : prev, needed - 1, newFilter, newBlackList, nextTuple).forEach(sink);
                });
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCyclesFixed(int variants, int prev, int needed, BitSet filter, BitSet blackList, BitSet tuple) {
        int tLength = tuple.length();
        int max = prev > variants / 2 ? variants - prev : prev;
        return IntStream.range(needed == 1 ? Math.max(variants - max + 1, tLength) : tLength, Math.min(variants, tLength + max - 1))
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
                    tuple.stream().forEach(val -> {
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
                        nextTuple.stream().forEach(nv -> {
                            newBlackList.set((nv + diff) % variants);
                            newBlackList.set((nv + variants - diff) % variants);
                        });
                    });
                    newFilter.stream().forEach(diff -> {
                        newBlackList.set((idx + diff) % variants);
                        newBlackList.set((idx + variants - diff) % variants);
                    });
                    calcCyclesFixed(variants, prev, needed - 1, newFilter, newBlackList, nextTuple).forEach(sink);
                });
    }

    private static BitSet diff(BitSet block, int v) {
        BitSet result = new BitSet();
        block.stream().forEach(i -> block.stream().filter(j -> j > i).forEach(j -> result.set(diff(j, i, v))));
        return result;
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, int prev, BitSet filter) {
        Set<BitSet> dedup = new HashSet<>();
        BitSet blackList = (BitSet) filter.clone();
        filter.stream().forEach(i -> blackList.set(variants - i));
        return calcCycles(variants, prev, size - 1, filter, blackList, of(0)).filter(e -> dedup.add(e.getKey()));
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCyclesFixed(int variants, int size, int prev, BitSet filter) {
        BitSet newFilter = (BitSet) filter.clone();
        newFilter.set(diff(0, prev, variants));
        BitSet blackList = (BitSet) newFilter.clone();
        newFilter.stream().forEach(dff -> {
            blackList.set(variants - dff);
            blackList.set((prev + dff) % variants);
            blackList.set((prev + variants - dff) % variants);
        });
        int outMid = variants - prev;
        if (prev % 2 == 0) {
            blackList.set(prev / 2);
        }
        if (outMid % 2 == 0) {
            blackList.set((prev + outMid / 2) % variants);
        }
        Set<BitSet> dedup = new HashSet<>();
        return calcCyclesFixed(variants, prev, size - 2, newFilter, blackList, of(0, prev)).filter(e -> dedup.add(e.getKey()));
    }

    private static int start(int v, int k) {
        return v / k + (k + 1) / 2;
    }

    // 217,7: 149=520181 148=620587
    @Test
    public void testDiffSets() {
        int prev = 148;
        int v = 217;
        int k = 7;
        System.out.println(v + " " + k + " " + prev);
        long time = System.currentTimeMillis();
        BitSet filter = v % k == 0 ? IntStream.rangeClosed(0, k / 2).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(v / 2 + 1);
        System.out.println(calcCyclesFixed(v, k, prev, filter)
                //.peek(System.out::println)
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
        findByHint(of(0, 68, 69, 105, 135, 156, 160), 217, 7);
        //findByHint(of(0, 32, 35, 50, 69, 81, 83), 91, 7);
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
    public void testDiffFamilies1() {
        int v = 217;
        int k = 7;
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
        int base = variants / k / (k - 1);
        boolean first = needed == base;
        AtomicLong counter = new AtomicLong();
        return (first ? calcCyclesFixed(variants, k, 150, filter)
                : needed + 1 == base ? calcCycles(variants, k, prev, filter).parallel() : calcCycles(variants, k, prev, filter)).mapMulti((pair, sink) -> {
            SequencedMap<BitSet, BitSet> nextCurr = new LinkedHashMap<>(curr);
            nextCurr.put(pair.getKey(), pair.getValue());
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet nextFilter = (BitSet) filter.clone();
            nextFilter.or(pair.getKey());
            allDifferenceSets(variants, k, nextCurr, needed - 1, nextFilter).forEach(sink);
            if (needed == base) {
                long cnt = counter.incrementAndGet();
                if ((cnt & ((1 << 10) - 1)) == 0) {
                    System.out.println(cnt);
                }
            }
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
