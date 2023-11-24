package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;
import ua.ihromant.mathutils.group.GroupProduct;

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
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BibdFinderTest {
    // 1428546
    @Test
    public void filterIsomorphic() throws IOException {
        int v = 85;
        int k = 5;
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", k + "-" + v + ".txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/diffSets/unique", k + "-" + v + ".txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            String line = br.readLine();
            CyclicGroup gr = new CyclicGroup(v);
            int[][] auths = gr.auth();
            Set<Set<BitSet>> unique = new HashSet<>();
            long time = System.currentTimeMillis();
            AtomicLong counter = new AtomicLong();
            ps.println(v + " " + k);
            while ((line = br.readLine()) != null) {
                String cut = line.replace("{{", "").replace("}}", "");
                String[] arrays = cut.split("\\}, \\{");
                int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                        .toArray()), v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty()).toArray(int[][]::new);
                IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                    int[][] diffs = IntStream.range(0, diffSet.length)
                            .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i].clone() : mirrorTuple(gr, diffSet[i]))
                            .map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                    if (Arrays.stream(auths).noneMatch(auth -> {
                        Set<BitSet> result = new HashSet<>();
                        for (int[] arr : diffs) {
                            result.add(of(minimalTuple(applyAuth(arr, auth), v)));
                        }
                        return unique.contains(result);
                    })) {
                        unique.add(Arrays.stream(diffs).map(BibdFinderTest::of).collect(Collectors.toSet()));
                        ps.println(Arrays.stream(diffs).map(arr -> of(arr).toString())
                                .collect(Collectors.joining(", ", "{", "}")));
                        counter.incrementAndGet();
                    }
                });
            }
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    private static int[] applyAuth(int[] arr, int[] auth) {
        return Arrays.stream(arr).map(i -> auth[i]).toArray();
    }

    @Test
    public void testGroupDifferenceSets() throws IOException {
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "5-Z9xZ9.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            String l = br.readLine();
            Group g = new GroupProduct(new CyclicGroup(3), new CyclicGroup(3), new CyclicGroup(3), new CyclicGroup(3));
            String[] chunks = l.split(" ");
            int k = Integer.parseInt(chunks[1]);
            int[] degenerate = chunks.length <= 3 ? new int[0] : Arrays.stream(chunks, 3, chunks.length)
                    .map(ch -> ch.replace(",", "").replace("{", "").replace("}", ""))
                    .mapToInt(Integer::parseInt).toArray();
            Set<BitSet> planes = ConcurrentHashMap.newKeySet();
            AtomicLong counter = new AtomicLong();
            long time = System.currentTimeMillis();
            br.lines().parallel().forEach(line -> {
                String cut = line.replace("{{", "").replace("}}", "");
                String[] arrays = cut.split("\\}, \\{");
                int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                        .toArray()), g.order() % k == 0 ? Stream.of(degenerate) : Stream.empty()).toArray(int[][]::new);
                IntStream.range(0, 1 << (diffSet.length - (g.order() % k == 0 ? 2 : 1))).forEach(comb -> {
                    int[][] ds = IntStream.range(0, diffSet.length)
                            .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i] : mirrorTuple(g, diffSet[i])).toArray(int[][]::new);
                    HyperbolicPlane p = new HyperbolicPlane(g, ds);
                    checkHypIndex(p, planes, ds);
                });

                long cnt = counter.incrementAndGet();
                if (cnt % 1000 == 0) {
                    System.out.println(cnt);
                }
            });
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    @Test
    public void testDifferenceSets1() throws IOException {
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/unique", "5-85.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            String l = br.readLine();
            int v = Integer.parseInt(l.split(" ")[0]);
            int k = Integer.parseInt(l.split(" ")[1]);
            Set<BitSet> planes = ConcurrentHashMap.newKeySet();
            AtomicLong counter = new AtomicLong();
            long time = System.currentTimeMillis();
            br.lines().parallel().forEach(line -> {
                String cut = line.replace("{{", "").replace("}}", "");
                String[] arrays = cut.split("\\}, \\{");
                int[][] diffSet = Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                        .toArray()).map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                HyperbolicPlane p = new HyperbolicPlane(v, diffSet);
                HyperbolicPlaneTest.testCorrectness(p, of(5));
                checkHypIndex(p, planes, diffSet);

                long cnt = counter.incrementAndGet();
                if (cnt % 10000 == 0) {
                    System.out.println(cnt);
                }
            });
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    @Test
    public void testDifferenceSets() throws IOException, InterruptedException {
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/old", "33-3.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            String l = br.readLine();
            int v = Integer.parseInt(l.split(" ")[0]);
            int k = Integer.parseInt(l.split(" ")[1]);
            Set<BitSet> planes = ConcurrentHashMap.newKeySet();
            AtomicLong counter = new AtomicLong();
            long time = System.currentTimeMillis();
            br.lines().parallel().forEach(line -> {
                String cut = line.replace("{{", "").replace("}}", "");
                String[] arrays = cut.split("\\}, \\{");
                int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                                .toArray()), v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty())
                        .map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                    int[][] ds = IntStream.range(0, diffSet.length)
                            .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i] : mirrorTuple(diffSet[i])).toArray(int[][]::new);
                    HyperbolicPlane p = new HyperbolicPlane(v, ds);
                    checkHypIndex(p, planes, ds);
                });

                long cnt = counter.incrementAndGet();
                if (cnt % 1000 == 0) {
                    System.out.println(cnt);
                }
            });
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    private static void checkSubPlanes(HyperbolicPlane p, Set<BitSet> unique, int[][] ds) {
        BitSet subPlanes = p.cardSubPlanes(false);
        if (unique.add(subPlanes)) {
            System.out.println(subPlanes + " " + Arrays.deepToString(ds));
        }
    }

    private static void checkHypIndex(HyperbolicPlane p, Set<BitSet> unique, int[][] ds) {
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
                BitSet filter = v % k != 0 ? new BitSet(0) : IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or);
                BitSet expected = IntStream.rangeClosed(1, v / 2).filter(i -> !filter.get(i)).collect(BitSet::new, BitSet::set, BitSet::or);
                assertTrue(difference.stream().allMatch(bs -> bs.stream().allMatch(i -> expected.get(i) && !filter.get(i))));
                counter++;
            }
            System.out.println(v + " " + k + " " + counter);
            assertEquals(diffs.size(), counter);
        }
    }

    @Test
    public void generate4() throws IOException {
        //generateDiffSets1(15, 3);
        generateDiffSets(new GroupProduct(new CyclicGroup(9), new CyclicGroup(9)), 5);
    }

    private static void printDifferencesToFile(int v, int k) throws IOException {
        System.out.println("Printing for " + v + " " + k);
        long time = System.currentTimeMillis();
        Set<BitSet> cycles = ConcurrentHashMap.newKeySet();
        BitSet filter = v % k == 0 ? IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(0);
        try (FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/diffSets/diffs", k + "-" + v + "diffs.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            AtomicLong counter = new AtomicLong();
            ps.println(v + " " + k);
            calcDistinctDifferences(v, k, filter).filter(cycles::add).forEach(diff -> {
                ps.println(diff);
                long c = counter.incrementAndGet();
                if (c % 1000_000 == 0) {
                    System.out.println(c);
                }
            });
        }
        System.out.println("Printed possible differences: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private static final int CONST = (1 << 20) - 1;

    private static void generateDiffSets(int v, int k) throws IOException {
        System.out.println("Generating for " + v + " " + k);
        long time = System.currentTimeMillis();
        Map<BitSet, BitSet> cycles = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        BitSet filter = v % k == 0 ? IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(0);
        calcCyclesAlt(v, k, filter).forEach(e -> {
            if (cycles.putIfAbsent(e.getKey(), e.getValue()) == null) {
                long c = counter.incrementAndGet();
                if ((c & CONST) == 0) {
                    System.out.println(c);
                }
            }
        });
        System.out.println("Calculated possible cycles: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        counter.set(0);
        File f = new File("/home/ihromant/maths/diffSets/", k + "-" + v + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(v + " " + k + " " + cycles.size());
            int needed = v / k / (k - 1);
            allDifferenceSets(new ArrayList<>(cycles.keySet()), needed, new BitSet[0], new BitSet()).forEach(ds -> {
            //altAllDifferenceSets(cycles, v, IntStream.range(0, k).toArray(), needed, new BitSet[needed], filter, ConcurrentHashMap.newKeySet()).forEach(ds -> {
                long c = counter.incrementAndGet();
                printDifferenceSet(ds, ps, cycles, false); // set multiple to true if you wish to print all results
                if ((c & CONST) == 0) {
                    System.out.println(c);
                }
                if (k > 4) {
                    ps.flush();
                }
            });
        }
        System.out.println("Calculated difference sets size: " + counter.longValue() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private static void generateDiffSets1(int v, int k) throws IOException {
        System.out.println("Generating for " + v + " " + k);
        long time = System.currentTimeMillis();
        Map<BitSet, BitSet> cycles = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        BitSet filter = v % k == 0 ? IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or) : new BitSet(0);
        calcCyclesAlt(v, k, filter).forEach(e -> {
            if (cycles.putIfAbsent(e.getKey(), e.getValue()) == null) {
                long c = counter.incrementAndGet();
                if ((c & CONST) == 0) {
                    System.out.println(c);
                }
            }
        });
        System.out.println("Calculated possible cycles: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        counter.set(0);
        int needed = v / k / (k - 1);
        allDifferenceSets(new ArrayList<>(cycles.keySet()), needed, new BitSet[0], new BitSet()).forEach(ds -> {
            //altAllDifferenceSets(cycles, v, IntStream.range(0, k).toArray(), needed, new BitSet[needed], filter, ConcurrentHashMap.newKeySet()).forEach(ds -> {
            long c = counter.incrementAndGet();
            int[][] dss = Stream.concat(Arrays.stream(ds).map(cycles::get).map(bs -> bs.stream().toArray()),
                    v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> v * i / k).toArray()) : Stream.of()).toArray(int[][]::new);
            IntStream.range(0, 1 << (dss.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                int[][] dsss = IntStream.range(0, dss.length)
                        .mapToObj(i -> ((1 << i) & comb) == 0 ? dss[i] : mirrorTuple(dss[i])).toArray(int[][]::new);
                HyperbolicPlane hp = new HyperbolicPlane(v, dsss);
                BitSet csp = hp.cardSubPlanes(false);
                if (!csp.get(v)) {
                    System.out.println(csp + " " + Arrays.deepToString(dsss));
                }
            });
            if ((c & CONST) == 0) {
                System.out.println(c);
            }
        });
        System.out.println("Calculated difference sets size: " + counter.longValue() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private static void generateDiffSets(Group g, int k) throws IOException {
        System.out.println("Generating for " + g.name() + " " + k);
        Map<BitSet, BitSet> degenerated = new ConcurrentHashMap<>();
        calcCyclesAlt(g.asTable(), k, new BitSet(), g.order() % k == 0 ? k - 1 : 0).forEach(e -> {
            if (degenerated.putIfAbsent(e.getKey(), e.getValue()) == null) {
                System.out.println("Degenerated " + e.getKey() + " " + e.getValue());
            }
        });
        if (g.order() % k == 0) {
            if (degenerated.isEmpty()) {
                System.out.println("Impossible to generate for " + g + " " + k);
                return;
            }
            for (BitSet filter : degenerated.keySet()) {
                generateDiffSets(g, k, filter);
            }
        } else {
            generateDiffSets(g, k, new BitSet(0));
        }
    }

    private static void generateDiffSets(Group g, int k, BitSet filter) throws IOException {
        long time = System.currentTimeMillis();
        Map<BitSet, BitSet> cycles = new ConcurrentHashMap<>();
        AtomicLong counter = new AtomicLong();
        calcCyclesAlt(g.asTable(), k, filter, k * (k - 1)).forEach(e -> {
            if (cycles.putIfAbsent(e.getKey(), e.getValue()) == null) {
                long c = counter.incrementAndGet();
                if ((c & CONST) == 0) {
                    System.out.println(c);
                }
            }
        });
        System.out.println("Calculated possible cycles: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        counter.set(0);
        File f = new File("/home/ihromant/maths/diffSets/", k + "-" + g.name() + ".txt"); // TODO possible multiple
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            filter.set(0);
            ps.println(g.name() + " " + k + " " + cycles.size() + (filter.cardinality() == 1 ? "" : " " + filter));
            int needed = g.order() / k / (k - 1);
            allDifferenceSets(new ArrayList<>(cycles.keySet()), needed, new BitSet[0], new BitSet()).forEach(ds -> {
                //altAllDifferenceSets(cycles, v, IntStream.range(0, k).toArray(), needed, new BitSet[needed], filter, ConcurrentHashMap.newKeySet()).forEach(ds -> {
                long c = counter.incrementAndGet();
                printDifferenceSet(ds, ps, g, cycles); // set multiple to true if you wish to print all results
                if ((c & CONST) == 0) {
                    System.out.println(c);
                }
                if (k > 4) {
                    ps.flush();
                }
            });
        }
        System.out.println("Calculated difference sets size: " + counter.longValue() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private static void printDifferenceSet(BitSet[] ds, PrintStream ps, Map<BitSet, BitSet> cycles, boolean multiple) {
        if (multiple) {
            IntStream.range(0, 1 << (ds.length - 1)).forEach(comb -> ps.println(IntStream.range(0, ds.length)
                    .mapToObj(i -> ((1 << i) & comb) == 0 ? cycles.get(ds[i]) : mirrorTuple(cycles.get(ds[i])))
                    .map(BitSet::toString).collect(Collectors.joining(", ", "{", "}"))));
        } else {
            ps.println(Arrays.stream(ds).map(cycles::get).map(BitSet::toString)
                    .collect(Collectors.joining(", ", "{", "}")));
        }
    }

    private static void printDifferenceSet(BitSet[] ds, PrintStream ps, Group gr, Map<BitSet, BitSet> cycles) {
        ps.println(Arrays.stream(ds).map(cycles::get).map(bs -> bs.stream()
                        .mapToObj(String::valueOf).collect(Collectors.joining(", ", "{", "}")))
                .collect(Collectors.joining(", ", "{", "}")));
    }

    private static Stream<BitSet[]> allDifferenceSets(List<BitSet> diffs, int needed, BitSet[] curr, BitSet present) {
        int currSize = curr.length;
        int diffSize = diffs.size();
        return (currSize == 0 ? IntStream.range(0, diffSize - needed + 1).boxed().parallel()
                : IntStream.range(0, diffSize - needed + 1).boxed()).mapMulti((idx, sink) -> {
            if (currSize == 0 && idx == Integer.lowestOneBit(idx)) {
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
        return calcCycles(variants, size, new BitSet(0));
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCycles(int variants, int size, BitSet filter) {
        return calcCycles(variants, size - 1, filter, of(0), new BitSet());
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCyclesAlt(int variants, int needed, BitSet filter) {
        int expectedCard = needed * (needed - 1) / 2;
        int cap = variants - variants / needed;
        int nBits = variants / 2 + 1;
        return Stream.iterate(IntStream.range(0, needed).toArray(), ch -> ch[0] == 0, ch -> GaloisField.nextChoice(cap, ch))
                .parallel().mapMulti((choice, sink) -> {
                    if (!isMinimal(choice, variants)) {
                        return;
                    }
                    BitSet diff = new BitSet(nBits);
                    for (int i = 0; i < needed; i++) {
                        for (int j = i + 1; j < needed; j++) {
                            diff.set(diff(choice[i], choice[j], variants));
                        }
                    }
                    if (diff.cardinality() != expectedCard || filter.intersects(diff)) {
                        return;
                    }
                    sink.accept(Map.entry(diff, of(choice)));
                });
    }

    private static Stream<Map.Entry<BitSet, BitSet>> calcCyclesAlt(Group g, int needed, BitSet filter, int expectedCard) {
        int variants = g.order();
        return Stream.iterate(IntStream.range(0, needed).toArray(), ch -> ch[0] == 0, ch -> GaloisField.nextChoice(variants, ch))
                .parallel().mapMulti((choice, sink) -> {
                    BitSet diff = new BitSet(variants);
                    for (int i = 0; i < needed; i++) {
                        for (int j = i + 1; j < needed; j++) {
                            diff.set(g.op(choice[i], g.inv(choice[j])));
                            diff.set(g.op(g.inv(choice[i]), choice[j]));
                        }
                    }
                    if (diff.cardinality() != expectedCard || filter.intersects(diff)) {
                        return;
                    }
                    sink.accept(Map.entry(diff, of(choice)));
                });
    }

    private static Stream<BitSet> calcDistinctDifferences(int variants, int needed, BitSet filter) {
        int expectedCard = needed * (needed - 1) / 2;
        int cap = variants - variants / needed;
        int nBits = variants / 2 + 1;
        return Stream.iterate(IntStream.range(0, needed).toArray(), ch -> ch[0] == 0, ch -> GaloisField.nextChoice(cap, ch))
                .parallel().mapMulti((choice, sink) -> {
                    if (!isMinimal(choice, variants)) {
                        return;
                    }
                    BitSet diff = new BitSet(nBits);
                    for (int i = 0; i < needed; i++) {
                        for (int j = i + 1; j < needed; j++) {
                            diff.set(diff(choice[i], choice[j], variants));
                        }
                    }
                    if (diff.cardinality() != expectedCard || filter.intersects(diff)) {
                        return;
                    }
                    sink.accept(diff);
                });
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

    private static int[] mirrorTuple(Group g, int[] tuple) {
        return Arrays.stream(tuple).map(g::inv).toArray();
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

    private static int[] minimalTuple(int[] arr, int v) {
        Arrays.sort(arr);
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

    private static int diff(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private static Stream<BitSet[]> altAllDifferenceSets(Map<BitSet, BitSet> existing, int variants, int[] start, int needed,
                                                         BitSet[] curr, BitSet present, Set<Set<BitSet>> added) {
        int k = start.length;
        int cap = variants - variants / k;
        int cl = curr.length;
        int nBits = variants / 2 + 1;
        return (needed == cl ? Stream.iterate(start, ch -> ch[0] == 0, ch -> GaloisField.nextChoice(cap, ch)).parallel()
                : Stream.iterate(start, ch -> ch[0] == 0, ch -> GaloisField.nextChoice(cap, ch))).mapMulti((perm, sink) -> {
            BitSet diff = new BitSet(nBits);
            for (int i = 0; i < k; i++) {
                for (int j = i + 1; j < k; j++) {
                    diff.set(diff(perm[i], perm[j], variants));
                }
            }
            if (!existing.containsKey(diff) || present.intersects(diff)) {
                return;
            }
            BitSet[] nextCurr = curr.clone();
            nextCurr[cl - needed] = diff;
            if (needed == 1) {
                if (added.add(Arrays.stream(nextCurr).collect(Collectors.toSet()))) {
                    sink.accept(nextCurr);
                }
                return;
            }
            BitSet nextPresent = (BitSet) present.clone();
            nextPresent.or(diff);
            //noinspection ConstantConditions
            altAllDifferenceSets(existing, variants, GaloisField.nextChoice(cap, start),
                    needed - 1, nextCurr, nextPresent, added).forEach(sink);
        });
    }
}
