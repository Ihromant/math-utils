package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibdFinderTest {
    @Test
    public void testDifferenceSets2() throws IOException, InterruptedException {
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "5-85.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             ExecutorService service = Executors.newFixedThreadPool(12)) {
            String line = br.readLine();
            int v = Integer.parseInt(line.split(" ")[0]);
            int k = Integer.parseInt(line.split(" ")[1]);
            Set<BitSet> planes = ConcurrentHashMap.newKeySet();
            AtomicLong counter = new AtomicLong();
            AtomicLong waiter = new AtomicLong();
            waiter.incrementAndGet();
            long time = System.currentTimeMillis();
            while ((line = br.readLine()) != null) {
                String cut = line.replace("{{", "").replace("}}", "");
                waiter.incrementAndGet();
                service.execute(() -> {
                    String[] arrays = cut.split("\\}, \\{");
                    int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                                    .toArray()), v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty())
                            .map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                    IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                        int[][] ds = IntStream.range(0, diffSet.length)
                                .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i] : mirrorTuple(diffSet[i])).toArray(int[][]::new);
                        HyperbolicPlane p = new HyperbolicPlane(v, ds);
                        checkHypIndex(p, planes, cut, ds);
                    });

                    long cnt = counter.incrementAndGet();
                    if (cnt % 1000 == 0) {
                        System.out.println(cnt);
                    }
                    if (waiter.decrementAndGet() == 0) {
                        service.shutdown();
                    }
                });
            }
            waiter.decrementAndGet();
            boolean res = service.awaitTermination(1, TimeUnit.DAYS);
            System.out.println(res + " " + counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    private static void checkSubPlanes(HyperbolicPlane p, Set<BitSet> unique, String cut, int[][] ds) {
        BitSet subPlanes = p.cardSubPlanes(false);
        if (unique.add(subPlanes)) {
            System.out.println(subPlanes + " " + Arrays.deepToString(ds));
        }
    }

    private static void checkHypIndex(HyperbolicPlane p, Set<BitSet> unique, String cut, int[][] ds) {
        BitSet index = p.hyperbolicIndex();
        if (unique.add(index)) {
            System.out.println(index + " " + Arrays.deepToString(ds));
        }
    }

    @Test
    public void checkCorrectness() throws IOException {
        try (FileInputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "4-37.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            String line = br.readLine();
            int v = Integer.parseInt(line.split(" ")[0]);
            int k = Integer.parseInt(line.split(" ")[1]);
            Set<Set<BitSet>> diffs = new HashSet<>();
            int counter = 0;
            while ((line = br.readLine()) != null) {
                line = line.replace("{{", "");
                line = line.replace("}}", "");
                String[] arrays = line.replace("{{", "").replace("}}", "").split("\\}, \\{");
                Set<BitSet> difference = Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", "))
                                .mapToInt(Integer::parseInt).collect(BitSet::new, BitSet::set, BitSet::or))
                        .map(bs -> bs.stream().flatMap(i -> bs.stream().filter(j -> i != j)
                                .map(j -> diff(i, j, v))).collect(BitSet::new, BitSet::set, BitSet::or))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                assertTrue(diffs.add(difference));
                BitSet filter = v % k != 0 ? new BitSet() : IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or);
                BitSet expected = IntStream.rangeClosed(1, v / 2).filter(i -> !filter.get(i)).collect(BitSet::new, BitSet::set, BitSet::or);
                assertTrue(difference.stream().allMatch(bs -> bs.stream().allMatch(i -> expected.get(i) && !filter.get(i))));
                counter++;
            }
            System.out.println(v + " " + k + " " + counter);
            assertEquals(diffs.size(), counter);
        }
    }

    @Test
    public void generate() throws IOException {
        generateDiffSets(85, 4);
    }

    private static void generateDiffSets(int v, int k) throws IOException {
        System.out.println("Generating for " + v + " " + k);
        long time = System.currentTimeMillis();
        Map<BitSet, BitSet> cycles = new ConcurrentHashMap<>();
        calcCycles(v, k, v % k == 0 ? IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or)
                : new BitSet()).forEach(e -> cycles.putIfAbsent(e.getKey(), e.getValue()));
        System.out.println("Calculated possible cycles: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
        List<BitSet> diffs = new ArrayList<>(cycles.keySet());
        time = System.currentTimeMillis();
        File f = new File("/home/ihromant/maths/diffSets/", k + "-" + v + ".txt");
        AtomicLong counter = new AtomicLong();
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(v + " " + k);
            allDifferenceSets(diffs, v / k / (k - 1), new BitSet[0], new BitSet()).forEach(ds -> {
                counter.incrementAndGet();
                printDifferenceSet(ds, ps, cycles, v, false); // set multiple to true if you wish to print all results
                if (k > 4) {
                    ps.flush();
                }
            });
        }
        System.out.println("Calculated difference sets size: " + counter.longValue() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private static void printDifferenceSet(BitSet[] ds, PrintStream ps, Map<BitSet, BitSet> cycles, int v, boolean multiple) {
        if (multiple) {
            IntStream.range(0, 1 << (ds.length - 1)).forEach(comb -> ps.println(IntStream.range(0, ds.length)
                    .mapToObj(i -> ((1 << i) & comb) == 0 ? minimalTuple(cycles.get(ds[i]), v)
                            : mirrorTuple(minimalTuple(cycles.get(ds[i]), v)))
                    .map(BitSet::toString).collect(Collectors.joining(", ", "{", "}"))));
        } else {
            ps.println(Arrays.stream(ds).map(cycles::get).map(BitSet::toString)
                    .collect(Collectors.joining(", ", "{", "}")));
        }
    }

    private static Stream<BitSet[]> allDifferenceSets(List<BitSet> diffs, int needed, BitSet[] curr, BitSet present) {
        int currSize = curr.length;
        int diffSize = diffs.size();
        return (currSize == 0 ? IntStream.range(0, diffSize - needed + 1).boxed().parallel()
                : IntStream.range(0, diffSize - needed + 1).boxed()).mapMulti((idx, sink) -> {
            if (currSize == 0 && idx == Integer.highestOneBit(idx)) {
                System.out.println(idx);
            }
            BitSet diff = diffs.get(idx);
            if (present.intersects(diff)) {
                return;
            }
            BitSet[] nextCurr = new BitSet[currSize + 1];
            System.arraycopy(curr, 0, nextCurr, 0, currSize);
            nextCurr[currSize] = diff;
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet nextPresent = (BitSet) present.clone();
            nextPresent.or(diff);
            allDifferenceSets(diffs.subList(idx + 1, diffSize), needed - 1,
                    nextCurr, nextPresent).forEach(sink);
        });
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size) {
        return calcCycles(variants, size, new BitSet());
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, BitSet filter) {
        return calcCycles(variants, size - 1, filter, of(0), new BitSet());
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int needed, BitSet filter, BitSet tuple, BitSet currDiff) {
        int tLength = tuple.length();
        int tCardinality = tuple.cardinality();
        return (currDiff.isEmpty() ? IntStream.range(tLength, variants - needed + 1).boxed().parallel()
                : IntStream.range(tLength, variants - needed + 1).boxed()).mapMulti((idx, sink) -> {
            BitSet addition = new BitSet(variants);
            tuple.stream().forEach(set -> addition.set(diff(set, idx, variants)));
            if (addition.cardinality() != tCardinality || filter.intersects(addition) || addition.intersects(currDiff)) {
                return;
            }
            BitSet nextTuple = (BitSet) tuple.clone();
            nextTuple.set(idx);
            BitSet nextDiff = (BitSet) currDiff.clone();
            nextDiff.or(addition);
            if (needed == 1) {
                sink.accept(Map.entry(nextDiff, minimalTuple(nextTuple, variants)));
                return;
            }
            calcCycles(variants, needed - 1, filter, nextTuple, nextDiff).forEach(sink);
        });
    }

    @Test
    public void test() {
        assertEquals(of(1, 3, 4, 9, 10, 12), diffFromTuple(of(0, 1, 4), 13));
        assertEquals(of(2, 5, 6, 7, 8, 11), diffFromTuple(of(0, 6, 8), 13));
        assertEquals(of(0, 1, 4), minimalTuple(of(0, 1, 4), 13));
        assertEquals(of(0, 2, 7), minimalTuple(of(0, 6, 8), 13));
        BitSet[][] testSet = new BitSet[][]{
                {of(0, 20, 21, 34), of(0, 1, 14, 17)},
                {of(0, 12, 19, 27), of(0, 7, 15, 25)},
                {of(0, 26, 28, 32), of(0, 2, 6, 11)},
                {of(0, 12, 18, 22), of(0, 12, 18, 22)},
                {of(0, 1, 8, 21), of(0, 1, 8, 21)},
                {of(0, 23, 26, 28), of(0, 3, 5, 14)},
                {of(0, 21, 23, 26), of(0, 2, 5, 16)},
                {of(0, 12, 13, 20), of(0, 12, 13, 20)},
                {of(0, 18, 22, 28), of(0, 4, 10, 19)},
                {of(0, 1, 14, 21), of(0, 1, 14, 21)},
                {of(0, 10, 18, 22), of(0, 10, 18, 22)},
                {of(0, 26, 28, 31), of(0, 2, 5, 11)},
                {of(0, 21, 23, 34), of(0, 2, 13, 16)},
                {of(0, 12, 19, 20), of(0, 12, 19, 20)},
                {of(0, 22, 27, 31), of(0, 5, 9, 15)},
                {of(0, 21, 23, 24), of(0, 2, 3, 16)},
                {of(0, 12, 19, 29), of(0, 7, 17, 25)},
                {of(0, 22, 26, 31), of(0, 4, 9, 15)}
        };
        Arrays.stream(testSet).forEach(test -> {
            BitSet minimal = minimalTuple(test[0], 37);
            assertEquals(test[1], minimal);
            assertEquals(diffFromTuple(minimal, 37), diffFromTuple(test[0], 37));
        });
        BitSet tuple = of(0, 22, 26, 31);
        BitSet diff = diffFromTuple(tuple, 37);
        Set<Set<BitSet>> rotations = new HashSet<>();
        generateAlternatives(tuple, 37).forEach(altTuple -> {
            assertEquals(diff, diffFromTuple(altTuple, 37));
            assertTrue(rotations.add(IntStream.range(0, 37).mapToObj(i -> altTuple.stream().map(j -> (i + j) % 37)
                    .collect(BitSet::new, BitSet::set, BitSet::or)).collect(Collectors.toSet())));
        });
        assertEquals(2, rotations.size());

        BitSet tuple1 = of(0, 1, 4);
        BitSet diff1 = diffFromTuple(tuple1, 13);
        Set<Set<BitSet>> rotations1 = new HashSet<>();
        generateAlternatives(tuple1, 13).forEach(altTuple -> {
            assertEquals(diff1, diffFromTuple(altTuple, 13));
            assertTrue(rotations1.add(IntStream.range(0, 13).mapToObj(i -> altTuple.stream().map(j -> (i + j) % 13)
                    .collect(BitSet::new, BitSet::set, BitSet::or)).collect(Collectors.toSet())));
        });
        assertEquals(2, rotations1.size());

        BitSet tuple2 = of(0, 2, 7);
        BitSet tuple3 = of(0, 5, 7);
        assertEquals(IntStream.range(0, 13).mapToObj(i -> tuple2.stream().map(j -> (i + j) % 13)
                        .collect(BitSet::new, BitSet::set, BitSet::or)).collect(Collectors.toSet()),
                IntStream.range(0, 13).mapToObj(i -> tuple3.stream().map(j -> (i + j) % 13)
                        .collect(BitSet::new, BitSet::set, BitSet::or)).collect(Collectors.toSet()));
    }

    private Stream<BitSet> generateAlternatives(BitSet tuple, int v) {
        int[] arr = minimalTuple(tuple, v).stream().toArray();
        int l = arr.length;
        int[] diffs = new int[l - 1];
        for (int i = 0; i < l - 1; i++) {
            diffs[i] = arr[i + 1] - arr[i];
        }
        BitSet diff = diffFromTuple(tuple, v);
        return GaloisField.permutations(diffs).map(perm -> {
            BitSet result = new BitSet();
            int sum = 0;
            result.set(0);
            for (int d : perm) {
                sum = sum + d;
                result.set(sum);
            }
            return result;
        }).filter(oDiff -> diff.equals(diffFromTuple(oDiff, v)));
    }

    private static BitSet mirrorTuple(BitSet tuple) {
        int max = tuple.length() - 1;
        return tuple.stream().map(i -> max - i).collect(BitSet::new, BitSet::set, BitSet::or);
    }

    private static int[] mirrorTuple(int[] tuple) {
        int max = tuple[tuple.length - 1];
        return IntStream.range(0, tuple.length).map(i -> max - tuple[tuple.length - i - 1]).toArray();
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

    private static int[] minimalTuple(int[] arr, int v) {
        int l = arr.length;
        int[] diffs = new int[l];
        for (int i = 0; i < l; i++) {
            diffs[i] = diff(arr[i], arr[(l + i - 1) % l], v);
        }
        int minIdx = IntStream.range(0, l).boxed().max(Comparator.comparing(i -> diffs[i])).orElseThrow();
        int val = arr[minIdx];
        int[] res = Arrays.stream(arr).map(i -> i >= val ? i - val : v + i - val).toArray();
        Arrays.sort(res);
        return res;
    }

    private static BitSet diffFromTuple(BitSet tuple, int v) {
        BitSet diff = new BitSet();
        tuple.stream().forEach(i -> tuple.stream().filter(j -> i != j).forEach(j -> {
            int abs = Math.abs(i - j);
            diff.set(abs);
            diff.set(Math.abs(v - abs));
        }));
        return diff;
    }

    private static int diff(int a, int b, int size) {
        return Math.min(Math.abs(a - b), Math.abs(Math.abs(a - b) - size));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }
}
