package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.ProjChar;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class TranslationPlaneTest1 {
    @Test
    public void findBeginnings() {
        int p = 2;
        int n = 8;
        System.out.println(p + " " + n);
        int half = n / 2;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        int[] v = helper.v();
        QuickFind quickFind = new QuickFind(v.length);
        int[] idxes = new int[helper.matCount()];
        for (int i = 0; i < v.length; i++) {
            idxes[v[i]] = i;
        }
        for (int b : v) {
            for (int a : helper.gl()) {
                quickFind.union(idxes[b], idxes[helper.mul(helper.mul(a, b), helper.inv(a))]);
            }
        }
        List<FixBS> components = quickFind.components();
        for (int i = 0; i < components.size(); i++) {
            System.out.println(i + " " + components.get(i).cardinality() + " " + " " + Arrays.toString(components.get(i).stream().map(j -> v[j]).toArray()));
        }
    }

    @Test
    public void readOrbits() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        int pow = LinearSpace.pow(p, half);
        int[][] orbits = readOrbits(pow);
        for (int i = 0; i < orbits.length; i++) {
            int first = orbits[i][0];
            int inv = helper.inv(first);
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j], inv) >= 0) {
                    System.out.println(i + " " + j);
                }
            }
            int added = helper.add(first, helper.unity());
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j], added) >= 0) {
                    System.out.println(i + " " + j);
                }
            }
        }
        System.out.println(Arrays.deepToString(orbits));
    }

    private int[][] readOrbits(int pow) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/trans/orbits" + pow + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(line -> {
                String[] split = line.substring(1, line.length() - 1).split(", ");
                return Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
            }).toArray(int[][]::new);
        }
    }

    private int[][] readTuples(int pow) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/trans/tuples" + pow + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(line -> {
                String[] split = line.substring(1, line.length() - 1).split(", ");
                return Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
            }).toArray(int[][]::new);
        }
    }

    private int[] splitOrder(int[] split) {
        Integer[] ixs = IntStream.range(0, split.length).boxed().toArray(Integer[]::new);
        Arrays.sort(ixs, Comparator.comparingInt(i -> -split[i]));
        return Arrays.stream(ixs).mapToInt(Integer::intValue).toArray();
    }

    @Test
    public void testSplits() {
        int p = 2;
        int n = 8;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);
        int sum = pow - 2;
        List<int[]> tuples = new ArrayList<>();
        int tl = 5;
        int[] baseTuple = new int[tl];
        generate(baseTuple, 0, sum, tuples::add);
        System.out.println(tuples.size());
        //tuples.forEach(t -> System.out.println(Arrays.toString(t)));
        List<int[]> filtered = tuples.stream().filter(t -> t[1] >= t[2] && t[1] >= t[3]).toList();
        System.out.println(filtered.size());
        filtered.forEach(t -> System.out.println(Arrays.toString(t)));
    }

    private static void generate(int[] tuple, int idx, int sum, Consumer<int[]> cons) {
        if (idx == tuple.length) {
            cons.accept(tuple.clone());
            return;
        }
        for (int i = 0; i <= sum; i++) {
            tuple[idx] = i;
            if (idx == tuple.length - 2) {
                tuple[idx + 1] = sum - i;
                generate(tuple, tuple.length, 0, cons);
                continue;
            }
            generate(tuple, idx + 1, sum - i, cons);
        }
    }

    @Test
    public void generate() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        int[][] orbits = readOrbits(pow);
        int[][] splits = readTuples(pow);

        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int mc = mini.cardinality();

        FixBS first = new FixBS(sc);
        first.set(0, mc);
        FixBS second = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            second.set(i * mc);
        }
        FixBS third = new FixBS(sc);
        for (int i = 0; i < mc; i++) {
            third.set(mc * i + i);
        }
        FixBS[] base = new FixBS[mc + 1];
        base[0] = first;
        base[1] = second;
        base[2] = third;
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = new HashMap<>();
        for (int[] tuple : splits) {
            int[] splitOrder = splitOrder(tuple);
            int[] tupleIdx = calcTupleIdx(tuple, splitOrder);
            BiConsumer<int[], List<Integer>> cons = (arr, vl) -> {
                FixBS[] newBase = base.clone();
                for (int i = 0; i < arr.length; i++) {
                    FixBS ln = new FixBS(sc);
                    int a = arr[i];
                    for (int x = 1; x < mc; x++) {
                        int ax = helper.mulVec(a, x);
                        ln.set(ax * mc + x);
                    }
                    newBase[i + 3] = ln;
                }
                int[][] lines = TranslationPlaneTest.toProjective(sp, newBase);
                Liner l = new Liner(lines.length, lines);
                if (TranslationPlaneTest.isDesargues(l, mc)) {
                    System.out.println("Desargues " + Arrays.toString(arr));
                    return;
                }
                ProjChar chr = TranslationPlaneTest.newTranslation(counter.toString(), l, projData);
                if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                    projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                    counter.incrementAndGet();
                    System.out.println(chr);
                    System.out.println(Arrays.toString(arr));
                } else {
                    System.out.println("Existing " + chr.name() + " " + Arrays.toString(arr));
                }
            };
            int[] partSpread = new int[mini.cardinality() - 2];
            tree(helper, orbits, tupleIdx, TranslationPlaneTest.filterGl(helper, p), Arrays.stream(orbits[tupleIdx[0]]).boxed().toList(), partSpread, 0, cons);
            System.out.println(Arrays.toString(tuple));
        }
    }

    @Test
    public void testForIdx() throws IOException {
        int pow = 16;
        int sum = pow - 2;
        int[][] tuples = readTuples(16);
        IntStream.range(0, 1000).forEach(i -> {
            int[] tuple = tuples[ThreadLocalRandom.current().nextInt(tuples.length)];
            int[] order = splitOrder(tuple);
            int idx = ThreadLocalRandom.current().nextInt(sum);
            System.out.println(idx + " " + Arrays.toString(tuple) + " " + forIdx(idx, tuple, order));
        });
    }

    private static int forIdx(int idx, int[] tuple, int[] tupleOrder) {
        int acc = 0;
        for (int i : tupleOrder) {
            acc = acc + tuple[i];
            if (acc > idx) {
                return i;
            }
        }
        throw new IllegalStateException();
    }

    private static int[] calcTupleIdx(int[] tuple, int[] tupleOrder) {
        return IntStream.range(0, Arrays.stream(tuple).sum()).map(i -> forIdx(i, tuple, tupleOrder)).toArray();
    }

    private void tree(ModuloMatrixHelper helper, int[][] orbits, int[] tupleIdx, List<Integer> subGl, List<Integer> v, int[] partSpread, int idx, BiConsumer<int[], List<Integer>> sink) {
        int needed = partSpread.length - idx;
        if (needed == 0) {
            sink.accept(partSpread, v);
            return;
        }
        FixBS filter = new FixBS(helper.matCount());
        for (int a : v) {
            if (filter.get(a)) {
                continue;
            }
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            int orbitIdx = tupleIdx[idx];
            List<Integer> newV = new ArrayList<>(v.size());
            boolean last = needed == 1;
            int nextOrbitIdx = last ? 0 : tupleIdx[idx + 1];
            if (!last && orbitIdx != nextOrbitIdx) {
                ex: for (int b : orbits[nextOrbitIdx]) {
                    for (int i = 0; i <= idx; i++) {
                        int el = newArr[i];
                        if (!helper.hasInv(helper.sub(b, el))) {
                            continue ex;
                        }
                    }
                    newV.add(b);
                }
            } else {
                for (int b : v) {
                    if (b > a && helper.hasInv(helper.sub(b, a))) {
                        newV.add(b);
                    }
                }
            }
            List<Integer> centralizer = new ArrayList<>(subGl.size());
            for (int el : subGl) {
                int invEl = helper.inv(el);
                int prod = helper.mul(helper.mul(invEl, a), el);
                filter.set(prod);
                if (idx == 0) {
                    filter.set(helper.inv(prod));
                }

                int lMul = helper.mul(a, el);
                int rMul = helper.mul(el, a);
                if (lMul == rMul) {
                    centralizer.add(el);
                }
            }
            tree(helper, orbits, tupleIdx, centralizer, newV, newArr, idx + 1, sink);
        }
    }
}
