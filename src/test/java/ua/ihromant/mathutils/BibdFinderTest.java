package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashMap;
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
    @Test
    public void testDifferenceSets1() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/diffSets/73-4.txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line = br.readLine();
            int v = Integer.parseInt(line.split(" ")[0]);
            int k = Integer.parseInt(line.split(" ")[1]);
            Map<BitSet, String> planes = new HashMap<>();
            int counter = 0;
            while ((line = br.readLine()) != null) {
                counter++;
                line = line.replace("{{", "");
                line = line.replace("}}", "");
                String[] arrays = line.replace("{{", "").replace("}}", "").split("\\}, \\{");
                int[][] diffSet = Stream.concat(v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty(),
                        Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt).toArray())).toArray(int[][]::new);
                HyperbolicPlane p = new HyperbolicPlane(v, diffSet);
                if (!planes.containsKey(p.cardSubPlanes())) {
                    planes.putIfAbsent(p.cardSubPlanes(), line);
                    System.out.println(p.cardSubPlanes() + " " + line);
                }
                HyperbolicPlaneTest.testCorrectness(p, of(k), (v - 1) / (k - 1));
            }
            System.out.println(counter);
        }
    }

    @Test
    public void checkCorrectness() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/diffSets/37-4.txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
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
        generateDiffSets(45, 5);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private void generateDiffSets(int v, int k) throws IOException {
        long time = System.currentTimeMillis();
        Map<BitSet, BitSet> cycles = new ConcurrentHashMap<>();
        calcCycles(v, k, v % k == 0 ? IntStream.range(0, k).map(i -> i * v / k).collect(BitSet::new, BitSet::set, BitSet::or)
                : new BitSet()).forEach(e -> cycles.putIfAbsent(e.getKey(), e.getValue()));
        List<BitSet> diffs = new ArrayList<>(cycles.keySet());
        System.out.println("Calculated possible cycles: " + cycles.size() + ", time spent " + (System.currentTimeMillis() - time));
        time = System.currentTimeMillis();
        File f = new File("/home/ihromant/workspace/math-utils/src/test/resources/diffSets", v + "-" + k + ".txt");
        AtomicLong counter = new AtomicLong();
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            ps.println(v + " " + k);
            allDifferenceSets(diffs, v / k / (k - 1), new BitSet[0], new BitSet()).forEach(ds -> {
                counter.incrementAndGet();
                ps.println(Arrays.stream(ds).map(cycles::get).map(BitSet::toString)
                        .collect(Collectors.joining(", ", "{", "}")));
                if (k > 4) {
                    ps.flush();
                }
            });
        }
        System.out.println("Calculated difference sets size: " + counter.longValue() + ", time spent " + (System.currentTimeMillis() - time));
    }

    private Stream<BitSet[]> allDifferenceSets(List<BitSet> diffs, int needed, BitSet[] curr, BitSet present) {
        return (curr.length == 0 ? IntStream.range(0, diffs.size() - needed + 1).boxed().parallel()
                : IntStream.range(0, diffs.size() - needed + 1).boxed()).mapMulti((idx, sink) -> {
            if (present.intersects(diffs.get(idx))) {
                return;
            }
            int size = curr.length;
            BitSet[] nextCurr = new BitSet[size + 1];
            System.arraycopy(curr, 0, nextCurr, 0, size);
            nextCurr[size] = diffs.get(idx);
            if (needed == 1) {
                sink.accept(nextCurr);
                return;
            }
            BitSet diff = diffs.get(idx);
            BitSet nextPresent = (BitSet) present.clone();
            nextPresent.or(diff);
            allDifferenceSets(diffs.subList(idx + 1, diffs.size()), needed - 1,
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
        return (currDiff.isEmpty() ? IntStream.range(tuple.length(), variants - needed + 1).boxed().parallel()
                : IntStream.range(tuple.length(), variants - needed + 1).boxed()).mapMulti((idx, sink) -> {
            BitSet addition = new BitSet(variants);
            tuple.stream().forEach(set -> addition.set(diff(set, idx, variants)));
            if (addition.cardinality() != tuple.cardinality() || filter.intersects(addition) || addition.intersects(currDiff)) {
                return;
            }
            BitSet nextTuple = (BitSet) tuple.clone();
            nextTuple.set(idx);
            BitSet nextDiff = (BitSet) currDiff.clone();
            nextDiff.or(addition);
            if (needed == 1) {
                sink.accept(Map.entry(nextDiff, nextTuple));
                return;
            }
            calcCycles(variants, needed - 1, filter, nextTuple, nextDiff).forEach(sink);
        });
    }

    private static int diff(int a, int b, int size) {
        return Math.min(Math.abs(a - b), Math.abs(Math.abs(a - b) - size));
    }
}
