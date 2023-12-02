package ua.ihromant.mathutils.group;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.GaloisField;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
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

public class FinderTest {
    private final CyclicGroup left = new CyclicGroup(25);
    private final GroupProduct pr = new GroupProduct(left, new CyclicGroup(2));
    private final SemiDirectProduct semi = new SemiDirectProduct(left, new CyclicGroup(2));

    @Test
    public void generate6() {
        int[][] base = new int[][]{
                {0, 1, 2, 3},
                {3, 4, 5, 6},
                {6, 7, 8, 0},
                {1, 4, 7},
                {9, 10}
        };
        int from = Arrays.stream(base).mapToInt(arr -> Arrays.stream(arr).reduce(Integer.MIN_VALUE, Math::max)).reduce(Integer.MIN_VALUE, Math::max) + 1;
        int range = 10;
        for (int v = from; v < from + range; v++) {
            int k = 3;
            BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
            BitSet[] blocks = Arrays.stream(base).map(arr -> {
                enhanceFrequencies(frequencies, arr);
                return of(arr);
            }).toArray(BitSet[]::new);
            int left = Arrays.stream(frequencies).mapToInt(bs -> frequencies.length - 1 - bs.cardinality()).sum();
            if ((2 * left) % ((k - 1) * k) != 0) {
                continue;
            }
            int[] nextPossible = nextPossible(frequencies, null, v);
            System.out.println(v);
            designs(v, k, true, nextPossible, blocks, frequencies).forEach(arr -> System.out.println(Arrays.toString(arr)));
        }
    }

    private void enhanceFrequencies(BitSet[] frequencies, int[] block) {
        for (int i = 0; i < block.length - 1; i++) {
            int t = block[i];
            for (int j = i + 1; j < block.length; j++) {
                int u = block[j];
                frequencies[u].set(t);
                frequencies[t].set(u);
            }
        }
    }

    private int[] nextPossible(BitSet[] frequencies, int[] prev, int v) {
        int fst = prev == null ? 0 : IntStream.range(prev[0], v)
                .filter(i -> frequencies[i].cardinality() + 1 != v).findAny().orElse(v);
        return IntStream.range(fst, v).filter(i -> {
            BitSet frq = frequencies[i];
            return !frq.get(fst) && frq.cardinality() + 1 != v;
        }).toArray();
    }

    @Test
    public void generate3() {
        int v = 16;
        int k = 4;
        int r = (v - 1) / (k - 1);
        BitSet[] frequencies = IntStream.range(0, v).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        BitSet[] blocks = new BitSet[r + 1];
        IntStream.range(0, r).forEach(i -> {
            int[] block = IntStream.concat(IntStream.of(0), IntStream.range(0, k - 1).map(j -> 1 + i * (k - 1) + j)).toArray();
            enhanceFrequencies(frequencies, block);
            blocks[i] = of(block);
        });
        int[] initial = IntStream.range(0, k).map(i -> 1 + (k - 1) * i).toArray();
        enhanceFrequencies(frequencies, initial);
        blocks[r] = of(initial);
        int[] nextPossible = nextPossible(frequencies, initial, v);
        designs(v, k, true, nextPossible, blocks, frequencies).forEach(arr -> System.out.println(Arrays.toString(arr)));
    }

    private Stream<BitSet[]> designs(int v, int k, boolean parallel, int[] possible, BitSet[] curr, BitSet[] frequencies) {
        int cl = curr.length;
        return (parallel ? Stream.iterate(IntStream.range(0, k).toArray(), ch -> ch != null && ch[0] == 0 && ch[1] == 1, ch -> GaloisField.nextChoice(possible.length, ch)).parallel()
                : Stream.iterate(IntStream.range(0, k).toArray(), ch -> ch != null && ch[0] == 0 && ch[1] == 1, ch -> GaloisField.nextChoice(possible.length, ch)))
                .map(ch -> Arrays.stream(ch).map(i -> possible[i]).toArray())
                .filter(perm -> {
                    for (int i = 1; i < k - 1; i++) {
                        for (int j = i + 1; j < k; j++) {
                            if (frequencies[perm[i]].get(perm[j])) {
                                return false;
                            }
                        }
                    }
                    return true;
                }).mapMulti((perm, sink) -> {
                    BitSet[] nextCurr = new BitSet[cl + 1];
                    System.arraycopy(curr, 0, nextCurr, 0, cl);
                    BitSet block = of(perm);
                    nextCurr[cl] = block;
                    BitSet[] nextFrequencies = Arrays.stream(frequencies).map(bs -> (BitSet) bs.clone()).toArray(BitSet[]::new);
                    enhanceFrequencies(nextFrequencies, perm);
                    int[] nextPossible = nextPossible(nextFrequencies, perm, v);
                    if (nextPossible.length == 0) {
                        sink.accept(nextCurr);
                        return;
                    }
                    if (nextPossible.length < k) {
                        return;
                    }
                    designs(v, k, false, nextPossible, nextCurr, nextFrequencies).forEach(sink);
                });
    }

    @Test
    public void generate2() {
        int len = 6;
        CyclicGroup left = new CyclicGroup(17);
        GroupProduct pr = new GroupProduct(left, new CyclicGroup(3));
        int[] start = IntStream.range(0, len).toArray();
        AtomicLong counter = new AtomicLong();
        Map<BitSet, BitSet> dedup = new ConcurrentHashMap<>();
        int order = pr.order();
        Set<BitSet> dedup1 = ConcurrentHashMap.newKeySet();
        Stream.iterate(start, ch -> ch[0] < 3, ch -> GaloisField.nextChoice(order, ch)).parallel().forEach(choice -> {
            BitSet filter = new BitSet();
            BitSet from = of(choice);
            boolean present = !left.elements().mapToObj(i -> from.stream().map(el -> pr.op(el, pr.fromArr(i, 0))).toArray())
                    .allMatch(arr -> !dedup1.contains(of(arr)) && IntStream.range(0, len).allMatch(i -> IntStream.range(i + 1, len)
                            .allMatch(j -> {
                                int numb = new Pair(arr[i], arr[j]).toInt(order);
                                boolean set = filter.get(numb);
                                filter.set(numb);
                                return !set;
                            })));
            if (!present) {
                dedup1.add(from);
            }
            if (present || dedup.putIfAbsent(filter, from) != null) {
                return;
            }
            long v = counter.incrementAndGet();
            if ((v & ((1 << 10) - 1)) == 0) {
                System.out.println(v);
            }
        });
        List<BitSet> base = new ArrayList<>(dedup.keySet());
        System.out.println(base.size());
        IntStream.range(0, base.size()).parallel().forEach(i -> {
            BitSet first = base.get(i);
            for (int j = i + 1; j < base.size(); j++) {
                BitSet second = base.get(j);
                if (second.intersects(first)) {
                    continue;
                }
                second = (BitSet) second.clone();
                second.or(first);
                for (int k = j + 1; k < base.size(); k++) {
                    BitSet third = base.get(k);
                    if (third.intersects(second)) {
                        continue;
                    }
                    third = (BitSet) third.clone();
                    third.or(second);
                    for (int l = k + 1; l < base.size(); l++) {
                        BitSet fourth = base.get(l);
                        if (fourth.intersects(third)) {
                            continue;
                        }
                        fourth = (BitSet) fourth.clone();
                        fourth.or(third);
                        for (int m = l + 1; m < base.size(); m++) {
                            BitSet fifth = base.get(m);
                            if (!fifth.intersects(fourth)) {
                                System.out.println(dedup.get(base.get(i)) + " " + dedup.get(base.get(j))
                                        + " " + dedup.get(base.get(k)) + " " + dedup.get(base.get(l)) + " " + dedup.get(base.get(m)));
                            }
                        }
                    }
                }
            }
            if ((i & ((1 << 10) - 1)) == 0) {
                System.out.println(i);
            }
        });
    }

    @Test
    public void generate() {
        Set<Pair> filter = new HashSet<>();
        IntStream.range(0, 5).forEach(a -> IntStream.range(0, 2).forEach(b -> IntStream.range(0, 5).forEach(i -> IntStream.range(i + 1, 5).forEach(j -> {
            filter.add(new Pair(pr.fromArr(i * 5 + a, b), pr.fromArr(j * 5 + a, b)));
        }))));
        assertEquals(100, filter.size());
        int[] start = IntStream.range(0, 6).toArray();
        AtomicLong counter = new AtomicLong();
        Set<BitSet> dedup = ConcurrentHashMap.newKeySet();
        BitSet[] base = Stream.iterate(start, Objects::nonNull, ch -> GaloisField.nextChoice(50, ch)).parallel().<BitSet>mapMulti((choice, sink) -> {
            Set<Pair> innerFilter = new HashSet<>(filter);
            BitSet from = of(choice);
            if (!left.elements().mapToObj(i -> from.stream().map(el -> pr.op(el, pr.fromArr(i, 0))).toArray())
                    .allMatch(arr -> !dedup.contains(of(arr)) && IntStream.range(0, 6).allMatch(i -> IntStream.range(i + 1, 6)
                            .allMatch(j -> innerFilter.add(new Pair(arr[i], arr[j])))))) {
                return;
            }
            dedup.add(from);
            long v = counter.incrementAndGet();
            if ((v & ((1 << 10) - 1)) == 0) {
                System.out.println(v);
            }
            sink.accept(from);
        }).toArray(BitSet[]::new);
        System.out.println(base.length);
        IntStream.range(0, base.length).parallel().forEach(i -> {
            Set<Pair> fFilter = new HashSet<>(filter);
            BitSet first = base[i];
            boolean fMatches = IntStream.range(0, 25).mapToObj(sh -> first.stream().map(el -> pr.op(el, pr.fromArr(sh, 0))).toArray())
                    .allMatch(arr -> IntStream.range(0, 6).allMatch(f -> IntStream.range(f + 1, 6)
                            .allMatch(s -> fFilter.add(new Pair(arr[f], arr[s])))));
            if (!fMatches) {
                throw new IllegalStateException();
            }
            for (int j = i + 1; j < base.length; j++) {
                Set<Pair> sFilter = new HashSet<>(fFilter);
                BitSet second = base[j];
                boolean sMatches = IntStream.range(0, 25).mapToObj(sh -> second.stream().map(el -> pr.op(el, pr.fromArr(sh, 0))).toArray())
                        .allMatch(arr -> IntStream.range(0, 6).allMatch(f -> IntStream.range(f + 1, 6)
                                .allMatch(s -> sFilter.add(new Pair(arr[f], arr[s])))));
                if (!sMatches) {
                    continue;
                }
                for (int k = j + 1; k < base.length; k++) {
                    Set<Pair> tFilter = new HashSet<>(sFilter);
                    BitSet third = base[k];
                    boolean tMatches = IntStream.range(0, 25).mapToObj(sh -> third.stream().map(el -> pr.op(el, pr.fromArr(sh, 0))).toArray())
                            .allMatch(arr -> IntStream.range(0, 6).allMatch(f -> IntStream.range(f + 1, 6)
                                    .allMatch(s -> tFilter.add(new Pair(arr[f], arr[s])))));
                    if (tMatches) {
                        System.out.println(first + " " + second + " " + third);
                    }
                }
            }
            if ((i & ((1 << 10) - 1)) == 0) {
                System.out.println(i);
            }
        });
    }

    @Test
    public void print() throws IOException {
        try (FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/diffSets/", "len50.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            int[] start = IntStream.range(0, 6).toArray();
            Set<BitSet> dedup = ConcurrentHashMap.newKeySet();
            Stream.iterate(start, Objects::nonNull, ch -> GaloisField.nextChoice(50, ch)).parallel().<BitSet>mapMulti((choice, sink) -> {
                Set<Pair> innerFilter = new HashSet<>();
                BitSet from = of(choice);
                if (!semi.elements().mapToObj(i -> from.stream().map(el -> semi.op(el, i)).toArray())
                        .allMatch(arr -> !dedup.contains(of(arr)) && IntStream.range(0, 6).allMatch(i -> IntStream.range(i + 1, 6)
                                .allMatch(j -> innerFilter.add(new Pair(arr[i], arr[j])))))) {
                    return;
                }
                dedup.add(from);
                sink.accept(from);
            }).forEach(bs -> ps.println(bs.stream().mapToObj(Integer::toString).collect(Collectors.joining(" "))));
//            Set<BitSet> dedup1 = ConcurrentHashMap.newKeySet();
//            Stream.iterate(start, Objects::nonNull, ch -> GaloisField.nextChoice(50, ch)).parallel().<BitSet>mapMulti((choice, sink) -> {
//                Set<Pair> innerFilter = new HashSet<>();
//                BitSet from = of(choice);
//                semi.elements().mapToObj(i -> from.stream().map(el -> semi.op(el, i)).toArray())
//                        .forEach(arr -> IntStream.range(0, 6).forEach(i -> IntStream.range(i + 1, 6)
//                                .forEach(j -> innerFilter.add(new Pair(arr[i], arr[j])))));
//                if (!semi.elements().mapToObj(i -> from.stream().map(el -> semi.op(el, i)).toArray())
//                        .allMatch(arr -> {
//                            IntStream.range(0, 6).forEach(i -> IntStream.range(i + 1, 6)
//                                    .forEach(j -> innerFilter.add(new Pair(arr[i], arr[j]))));
//                            return !dedup1.contains(of(arr));
//                        }) || innerFilter.size() != 375) {
//                    return;
//                }
//                dedup1.add(from);
//                sink.accept(from);
//            }).forEach(bs -> ps.println(bs.stream().mapToObj(Integer::toString).collect(Collectors.joining(" "))));
        }
    }

    @Test
    public void generate1() throws IOException {
        int[][] len25 = Files.lines(Path.of(URI.create("file:///home/ihromant/maths/diffSets/len25.txt")))
                .map(l -> Arrays.stream(l.split(" ")).mapToInt(Integer::parseInt).toArray()).toArray(int[][]::new);
        IntStream.range(0, len25.length).parallel().forEach(i -> {
            Set<Pair> fFilter = new HashSet<>();
            int[] first = len25[i];
            semi.elements().mapToObj(sh -> Arrays.stream(first).map(el -> semi.op(el, sh)).toArray())
                    .forEach(arr -> IntStream.range(0, 6).forEach(f -> IntStream.range(f + 1, 6)
                            .forEach(s -> fFilter.add(new Pair(arr[f], arr[s])))));
            if (fFilter.size() != 375) {
                throw new IllegalStateException();
            }
            for (int j = i + 1; j < len25.length; j++) {
                Set<Pair> sFilter = new HashSet<>(fFilter);
                int[] second = len25[j];
                boolean sMatches = semi.elements().mapToObj(sh -> Arrays.stream(second).map(el -> semi.op(el, sh)).toArray())
                        .allMatch(arr -> IntStream.range(0, 6).allMatch(f -> IntStream.range(f + 1, 6)
                                .allMatch(s -> {
                                    Pair p = new Pair(arr[f], arr[s]);
                                    sFilter.add(p);
                                    return !fFilter.contains(p);
                                })));
                if (!sMatches) {
                    continue;
                }
                for (int k = j + 1; k < len25.length; k++) {
                    int[] third = len25[k];
                    boolean tMatches = semi.elements().mapToObj(sh -> Arrays.stream(third).map(el -> semi.op(el, sh)).toArray())
                            .allMatch(arr -> IntStream.range(0, 6).allMatch(f -> IntStream.range(f + 1, 6)
                                    .noneMatch(s -> sFilter.contains(new Pair(arr[f], arr[s])))));
                    if (tMatches) {
                        System.out.println(Arrays.toString(first) + " " + Arrays.toString(second) + " " + Arrays.toString(third));
                    }
                }
            }
            if ((i & ((1 << 10) - 1)) == 0) {
                System.out.println(i);
            }
        });
    }

    private record Pair(int x, int y) {
        private Pair(int x, int y) {
            this.x = Math.min(x, y);
            this.y = Math.max(x, y);
        }

        public int toInt(int cap) {
            return x * cap + y;
        }
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet(values[values.length - 1] + 1);
        for (int v : values) {
            bs.set(v);
        }
        return bs;
    }
}
