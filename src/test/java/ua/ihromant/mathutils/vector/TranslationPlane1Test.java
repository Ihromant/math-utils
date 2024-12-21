package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.QuickFind;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.ProjChar;
import ua.ihromant.mathutils.util.FixBS;

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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class TranslationPlane1Test {
    @Test
    public void findBeginnings() {
        int p = 2;
        int n = 10;
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
            if (quickFind.size(idxes[b]) > 1) {
                continue;
            }
            for (int a : helper.gl()) {
                quickFind.union(idxes[b], idxes[helper.mul(helper.mul(a, b), helper.inv(a))]);
            }
        }
        List<FixBS> components = quickFind.components();
        for (int i = 0; i < components.size(); i++) {
            System.out.println(i + " " + components.get(i).cardinality() + " " + Arrays.toString(components.get(i).stream().map(j -> v[j]).toArray()));
        }
    }

    private int[][] readOrbits(int pow) throws IOException {
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/orbits" + pow + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(line -> {
                int idx = line.indexOf('[');
                if (idx < 0) {
                    return null;
                }
                String[] split = line.substring(idx + 1, line.length() - 1).split(", ");
                return Arrays.stream(split).mapToInt(Integer::parseInt).toArray();
            }).filter(Objects::nonNull).toArray(int[][]::new);
        }
    }

    private int[][] readTuples(int pow) throws IOException {
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/tuples" + pow + ".txt");
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
        int n = 10;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);
        int sum = pow - 2;
        List<int[]> tuples = new ArrayList<>();
        int tl = 8;
        int[] baseTuple = new int[tl];
        generate(baseTuple, 0, sum, tuples::add);
        System.out.println(tuples.size());
        //tuples.forEach(t -> System.out.println(Arrays.toString(t)));
        List<int[]> filtered = tuples.stream().filter(t -> t[0] >= t[1]
                && t[2] >= t[3] && t[2] >= t[4] && t[2] >= t[5] && t[2] >= t[6] && t[2] >= t[7]).toList();
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
        int n = 10;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        int[][] orbits = readOrbits(pow);
        int[][] tuples = readTuples(pow);

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
        Map<Characteristic, List<ProjChar>> projData = TranslationPlaneTest.readKnown(mc);
        for (int[] tuple : tuples) {
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
            tree(helper, orbits, tupleIdx, filterGl(helper, p), Arrays.stream(orbits[tupleIdx[0]]).boxed().toList(), partSpread, 0, cons);
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

    private void tree(ModuloMatrixHelper helper, int[][] orbits, int[] tupleIdx, IntList subGl, List<Integer> v, int[] partSpread, int idx, BiConsumer<int[], List<Integer>> sink) {
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
            IntList centralizer = new IntList(subGl.size());
            for (int i = 0; i < subGl.size(); i++) {
                int el = subGl.get(i);
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

    @Test
    public void generateAlt() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int mc = mini.cardinality();

        int[][] orbits = readOrbits(mc);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

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
        Map<Characteristic, List<ProjChar>> projData = TranslationPlaneTest.readKnown(mc);
        for (FixBS comp : find.components()) {
            int min = comp.nextSetBit(0);
            Callback cons = (arr, func, subGl) -> {
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
                    System.out.println("Desargues " + Arrays.toString(arr) + " " + func);
                    return;
                }
                ProjChar chr = TranslationPlaneTest.newTranslation(counter.toString(), l, projData);
                if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                    projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                    counter.incrementAndGet();
                    System.out.println(chr);
                    System.out.println(Arrays.toString(arr) + " " + func);
                } else {
                    System.out.println("Existing " + chr.name() + " " + Arrays.toString(arr) + " " + func);
                }
            };
            int[] partSpread = new int[mini.cardinality() - 2];
            treeAlt(helper, filterGl(helper, p), new Func(orbits, new int[]{min}, new int[]{0}, 1, 0), new IntList(orbits[min]), partSpread, cons);
        }
    }

    private static QuickFind orbitComponents(ModuloMatrixHelper helper, int[][] orbits) {
        QuickFind find = new QuickFind(orbits.length);
        for (int i = 0; i < orbits.length; i++) {
            int first = orbits[i][0];
            int inv = helper.inv(first);
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j], inv) >= 0) {
                    find.union(i, j);
                }
            }
            int added = helper.add(first, helper.unity());
            for (int j = 0; j < orbits.length; j++) {
                if (Arrays.binarySearch(orbits[j], added) >= 0) {
                    find.union(i, j);
                }
            }
        }
        return find;
    }

    private record Func(int[][] orbits, int[] dom, int[] rng, int len, int sum) {
        private int[] possibleJumps() {
            FixBS possible = new FixBS(orbits.length);
            possible.set(0, orbits.length);
            int last = rng[len - 1];
            for (int used : dom) {
                possible.clear(used);
            }
            if (last == 1) {
                possible.clear(0, dom[len - 1]);
            }
            return possible.stream().toArray();
        }

        private boolean canJump(int needed) {
            return rng[len - 1] * (orbits.length - len) + sum >= needed;
        }

        private boolean canStay() {
            if (len <= 1) {
                return true;
            }
            int last = rng[len - 1];
            int preLast = rng[len - 2];
            int diff = preLast - last;
            if (diff == 1) {
                for (int i = len - 2; i >= 0; i--) {
                    if (rng[i] != preLast) {
                        break;
                    }
                    if (dom[len - 1] < dom[i]) {
                        return false;
                    }
                }
                return true;
            }
            return diff > 0;
        }

        private Func inc() {
            int[] nextRng = rng.clone();
            nextRng[len - 1]++;
            return new Func(orbits, dom, nextRng, len, sum + 1);
        }

        private Func extendDom(int next) {
            int[] nextDom = Arrays.copyOf(dom, len + 1);
            nextDom[len] = next;
            int[] nextRng = Arrays.copyOf(rng, len + 1);
            nextRng[len] = 1;
            return new Func(orbits, nextDom, nextRng, len + 1, sum + 1);
        }

        @Override
        public String toString() {
            return "Func{" +
                    "dom=" + Arrays.toString(dom) +
                    ", rng=" + Arrays.toString(rng) +
                    ", len=" + len + '}';
        }
    }

    private static IntList filterGl(ModuloMatrixHelper helper, int p) {
        int[] gl = helper.gl();
        IntList result = new IntList(gl.length);
        FixBS filter = new FixBS(helper.matCount());
        for (int i = 1; i < p; i++) {
            filter.set(helper.mulCff(helper.unity(), i));
        }
        for (int i : gl) {
            if (filter.get(i)) {
                continue;
            }
            for (int j = 2; j < p; j++) {
                filter.set(helper.mulCff(i, j));
            }
            result.add(i);
        }
        return result;
    }

    private static IntList filterOrbit(ModuloMatrixHelper helper, int[] partSpread, int[] orbit, int idx) {
        IntList result = new IntList(orbit.length);
        ex: for (int b : orbit) {
            for (int i = 0; i < idx; i++) {
                int el = partSpread[i];
                if (!helper.hasInv(helper.sub(b, el))) {
                    continue ex;
                }
            }
            result.add(b);
        }
        return result;
    }

    private void treeAlt(ModuloMatrixHelper helper, IntList subGl, Func func, IntList v, int[] partSpread, Callback sink) {
        int idx = func.sum();
        if (idx == partSpread.length) {
            sink.accept(partSpread, func, subGl);
            return;
        }
        boolean canJump = func.canJump(partSpread.length);
        boolean canStay = func.canStay();
        if (canStay) {
            Func next = func.inc();
            FixBS filter = new FixBS(helper.matCount());
            for (int j = 0; j < v.size(); j++) {
                int a = v.get(j);
                if (filter.get(a)) {
                    continue;
                }
                int[] newArr = partSpread.clone();
                newArr[idx] = a;
                IntList newV = new IntList(v.size());
                for (int i = j + 1; i < v.size(); i++) {
                    int b = v.get(i);
                    if (helper.hasInv(helper.sub(b, a))) {
                        newV.add(b);
                    }
                }
                IntList centralizer = new IntList(subGl.size());
                for (int i = 0; i < subGl.size(); i++) {
                    int el = subGl.get(i);
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
                if (centralizer.isEmpty()) {
                    treeAltNoSubGl(helper, next, newV, newArr, sink);
                } else {
                    treeAlt(helper, centralizer, next, newV, newArr, sink);
                }
            }
        }
        if (canJump) {
            for (int possible : func.possibleJumps()) {
                int[] orbit = func.orbits[possible];
                v = filterOrbit(helper, partSpread, orbit, idx);
                Func next = func.extendDom(possible);
                FixBS filter = new FixBS(helper.matCount());
                for (int j = 0; j < v.size(); j++) {
                    int a = v.get(j);
                    if (filter.get(a)) {
                        continue;
                    }
                    int[] newArr = partSpread.clone();
                    newArr[idx] = a;
                    IntList newV = new IntList(v.size());
                    for (int i = j + 1; i < v.size(); i++) {
                        int b = v.get(i);
                        if (helper.hasInv(helper.sub(b, a))) {
                            newV.add(b);
                        }
                    }
                    IntList centralizer = new IntList(subGl.size());
                    for (int i = 0; i < subGl.size(); i++) {
                        int el = subGl.get(i);
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
                    if (centralizer.isEmpty()) {
                        treeAltNoSubGl(helper, next, newV, newArr, sink);
                    } else {
                        treeAlt(helper, centralizer, next, newV, newArr, sink);
                    }
                }
            }
        }
    }

    private void treeAltNoSubGl(ModuloMatrixHelper helper, Func func, IntList v, int[] partSpread, Callback sink) {
        int idx = func.sum();
        if (idx == partSpread.length) {
            sink.accept(partSpread, func, new IntList(0));
            return;
        }
        boolean canJump = func.canJump(partSpread.length);
        boolean canStay = func.canStay();
        if (canStay) {
            Func next = func.inc();
            for (int j = 0; j < v.size(); j++) {
                int a = v.get(j);
                int[] newArr = partSpread.clone();
                newArr[idx] = a;
                IntList newV = new IntList(v.size());
                for (int i = j + 1; i < v.size(); i++) {
                    int b = v.get(i);
                    if (b > a && helper.hasInv(helper.sub(b, a))) {
                        newV.add(b);
                    }
                }
                treeAltNoSubGl(helper, next, newV, newArr, sink);
            }
        }
        if (canJump) {
            for (int possible : func.possibleJumps()) {
                int[] orbit = func.orbits[possible];
                v = filterOrbit(helper, partSpread, orbit, idx);
                Func next = func.extendDom(possible);
                for (int j = 0; j < v.size(); j++) {
                    int a = v.get(j);
                    int[] newArr = partSpread.clone();
                    newArr[idx] = a;
                    IntList newV = new IntList(v.size());
                    for (int i = j + 1; i < v.size(); i++) {
                        int b = v.get(i);
                        if (b > a && helper.hasInv(helper.sub(b, a))) {
                            newV.add(b);
                        }
                    }
                    treeAltNoSubGl(helper, next, newV, newArr, sink);
                }
            }
        }
    }

    @FunctionalInterface
    private interface Callback {
        void accept(int[] spread, Func func, IntList subGl);
    }

    @Test
    public void dumpGl() throws IOException {
        int p = 2;
        int n = 5;
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/gl-" + p + "^" + n + ".txt");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            for (int el : helper.gl()) {
                ps.println(el + "=" + helper.inv(el));
            }
        }
    }

    private ModuloMatrixHelper readGl(int p, int n) throws IOException {
        if (p != 2) {
            return ModuloMatrixHelper.of(p, n);
        }
        int matCount = LinearSpace.pow(p, n * n);
        int[] mapGl = new int[matCount];
        try (InputStream is = new FileInputStream("/home/ihromant/maths/trans/gl-" + p + "^" + n + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            br.lines().forEach(ln -> {
                String[] sp = ln.split("=");
                mapGl[Integer.parseInt(sp[0])] = Integer.parseInt(sp[1]);
            });
        }
        return new TwoMatrixHelper(n, mapGl);
    }

    @Test
    public void generateBegins() throws IOException {
        int p = 2;
        int n = 8;
        int half = n / 2;
        int pow = LinearSpace.pow(p, half);

        int[][] orbits = readOrbits(pow);
        ModuloMatrixHelper helper = readGl(p, half);
        QuickFind find = orbitComponents(helper, orbits);
        System.out.println("Components " + find.components());

        File f = new File("/home/ihromant/maths/trans/", "begins-" + p + "^" + n + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            for (FixBS comp : find.components()) {
                int min = comp.nextSetBit(0);
                Callback cons = (arr, func, subGl) -> {
                    ps.println(Arrays.toString(Arrays.copyOf(arr, 3)) + " " + Arrays.toString(func.dom)
                            + " " + Arrays.toString(func.rng) + " " + Arrays.toString(subGl.toArray()));
                };
                int[] partSpread = new int[pow - 2];
                treeAlt(helper, filterGl(helper, p), new Func(orbits, new int[]{min}, new int[]{0}, 1, 0), new IntList(orbits[min]), partSpread, cons);
            }
        }
    }
}
