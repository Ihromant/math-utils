package ua.ihromant.mathutils.vector;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleAbstractTypeResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.BatchAffineTest;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.plane.CharVals;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.MatrixTernaryRing;
import ua.ihromant.mathutils.plane.ProjChar;
import ua.ihromant.mathutils.plane.ProjectiveTernaryRing;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.plane.TernarMapping;
import ua.ihromant.mathutils.plane.TernaryRing;
import ua.ihromant.mathutils.plane.TernaryRingTest;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class TranslationPlaneTest {
    @Test
    public void writeHulls() throws IOException {
        int p = 7;
        int n = 4;
        File f = new File("/home/ihromant/maths/", "spaces-" + p + "^" + n + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            System.out.println(p + " " + n);
            LinearSpace sp = LinearSpace.of(p, n);
            int half = sp.half();
            FixBS first = new FixBS(sp.cardinality());
            first.set(1, half);
            FixBS second = new FixBS(sp.cardinality());
            for (int i = 1; i < half; i++) {
                second.set(i * half);
            }
            FixBS union = first.union(second);
            FixBS third = new FixBS(sp.cardinality());
            for (int i = 1; i < half; i++) {
                third.set(half * i + i);
            }
            union.or(third);
            Set<FixBS> distinct = new HashSet<>();
            generateSpaces(sp, union, h -> {
                if (distinct.add(h)) {
                    ps.println(h);
                    ps.flush();
                }
            });
        }
    }

    private static FixBS[] readHulls(LinearSpace sp) throws IOException {
        File f = new File("/home/ihromant/maths/", "spaces-" + sp.p() + "^" + sp.n() + ".txt");
        try (FileInputStream fis = new FileInputStream(f);
             InputStreamReader isr = new InputStreamReader(fis);
             BufferedReader br = new BufferedReader(isr)) {
            return br.lines().map(l -> FixBS.of(sp.cardinality(), Arrays.stream(l.substring(1, l.length() - 1)
                    .split(", ")).mapToInt(Integer::parseInt).toArray())).toArray(FixBS[]::new);
        }
    }

    @Test
    public void checkSubspaces() throws IOException {
        int p = 3;
        int n = 4;
        System.out.println(p + " " + n);
        LinearSpace sp = LinearSpace.of(p, n);
        int half = sp.half();
        FixBS first = new FixBS(sp.cardinality());
        first.set(1, half);
        FixBS second = new FixBS(sp.cardinality());
        for (int i = 1; i < half; i++) {
            second.set(i * half);
        }
        FixBS union = first.union(second);
        FixBS third = new FixBS(sp.cardinality());
        for (int i = 1; i < half; i++) {
            third.set(half * i + i);
        }
        union.or(third);
        FixBS[] hulls = readHulls(sp);
        System.out.println(hulls.length + " " + Arrays.stream(hulls).takeWhile(h -> h.nextSetBit(0) == third.nextSetBit(0) + 1).count());
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = new HashMap<>();
        AtomicInteger allCounter = new AtomicInteger();
        Consumer<FixBS[]> cons = arr -> {
            int[][] lines = toProjective(sp, arr);
            allCounter.incrementAndGet();
            Liner l = new Liner(lines.length, lines);
            if (isDesargues(l, half)) {
                return;
            }
            ProjChar chr = newTranslation(counter.toString(), l, projData);
            if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                System.out.println(counter.incrementAndGet() + Arrays.toString(arr));
                System.out.println(chr);
            }
        };
        FixBS[] curr = new FixBS[half + 1];
        curr[0] = first;
        curr[1] = second;
        curr[2] = third;
        generate(curr, union, half - 2, hulls, cons);
        System.out.println(allCounter + " " + projData);
    }

    private static void generate(FixBS[] curr, FixBS union, int needed, FixBS[] hulls, Consumer<FixBS[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        int next = union.nextClearBit(1);
        for (FixBS bs : hulls) {
            FixBS[] newCurr = curr.clone();
            newCurr[curr.length - needed] = bs;
            FixBS[] nextHulls = Arrays.stream(hulls).filter(h -> !bs.intersects(h)).toArray(FixBS[]::new);
            generate(newCurr, union.union(bs), needed - 1, nextHulls, cons);
            if (bs.nextSetBit(0) != next) {
                break;
            }
        }
    }

    private static void generateSpaces(LinearSpace sp, FixBS filter, Consumer<FixBS> sink) {
        int half = sp.half();
        FixBS hull = new FixBS(sp.cardinality());
        generateSpaces(sp, hull, filter, half, sp.n() / 2, sink);
    }

    private static void generateSpaces(LinearSpace sp, FixBS hull, FixBS filter, int prev, int needed, Consumer<FixBS> sink) {
        if (needed == 0) {
            sink.accept(hull);
            return;
        }
        for (int curr = prev; curr < sp.cardinality(); curr++) {
            if (hull.get(curr)) {
                continue;
            }
            FixBS nextHull = hull.copy();
            for (int i = 1; i < sp.p(); i++) {
                int mul = sp.mul(i, curr);
                nextHull.set(mul);
            }
            for (int j = hull.nextSetBit(0); j >= 0; j = hull.nextSetBit(j + 1)) {
                for (int i = 1; i < sp.p(); i++) {
                    int mul = sp.mul(i, curr);
                    nextHull.set(sp.add(mul, j));
                }
            }
            if (filter.intersects(nextHull)) {
                continue;
            }
            generateSpaces(sp, nextHull, filter, curr, needed - 1, sink);
        }
    }

    public static int[][] toProjective(LinearSpace space, FixBS[] spread) {
        int half = space.half();
        int pc = half * half + half + 1;
        int[][] lines = new int[pc][];
        for (int i = 0; i < spread.length; i++) {
            Set<FixBS> unique = new HashSet<>();
            FixBS el = spread[i].copy();
            el.set(0);
            int cnt = 0;
            for (int j = 0; j < space.cardinality(); j++) {
                FixBS bs = new FixBS(space.cardinality());
                for (int k = el.nextSetBit(0); k >= 0; k = el.nextSetBit(k + 1)) {
                    bs.set(space.add(k, j));
                }
                if (unique.add(bs)) {
                    lines[half * i + cnt++] = IntStream.concat(bs.stream(), IntStream.of(half * half + i)).toArray();
                }
                if (cnt == half) {
                    break;
                }
            }
        }
        lines[half * half + half] = IntStream.range(half * half, half * half + half + 1).toArray();
        return lines;
    }

    public static ProjChar newTranslation(String name, Liner proj, Map<Characteristic, List<ProjChar>> map) {
        List<TernarMapping> mappings = new ArrayList<>();
        int pc = proj.pointCount();
        int order = proj.line(0).length - 1;
        int infty = pc - 1;
        for (int dl = 0; dl < infty; dl += order) {
            int inftyPt = proj.intersection(infty, dl);
            int[] line = proj.line(dl);
            for (int h : line) {
                int v = h == 0 ? inftyPt : 0;
                for (int o = 0; o < pc; o++) {
                    if (proj.flag(dl, o)) {
                        continue;
                    }
                    int oh = proj.line(o, h);
                    int ov = proj.line(o, v);
                    for (int e = 0; e < pc; e++) {
                        if (proj.flag(dl, e) || proj.flag(ov, e) || proj.flag(oh, e)) {
                            continue;
                        }
                        int w = proj.intersection(proj.line(e, h), ov);
                        int u = proj.intersection(proj.line(e, v), oh);
                        Quad base = new Quad(o, u, w, e);
                        TernaryRing ring = new ProjectiveTernaryRing(name, proj, base);
                        int two = ring.op(1, 1, 1);
                        if (two == 0) {
                            continue;
                        }
                        CharVals cv = CharVals.of(ring, two, order);
                        if (!cv.induced()) {
                            continue;
                        }
                        if (mappings.isEmpty()) {
                            mappings.add(TernaryRingTest.fillTernarMapping(ring.toMatrix(), cv, two, order));
                        }
                        Characteristic fstChr = mappings.getFirst().chr();
                        List<ProjChar> existingChars = map.get(cv.chr());
                        boolean eq = fstChr.equals(cv.chr());
                        if (!eq && existingChars == null) {
                            continue;
                        }
                        TernaryRing matrix = ring.toMatrix();
                        if (eq && mappings.stream().noneMatch(tm -> TernaryRingTest.ringIsomorphic(tm, matrix))) {
                            mappings.add(TernaryRingTest.fillTernarMapping(matrix, cv, two, order));
                        }
                        if (existingChars != null) {
                            Optional<ProjChar> opt = existingChars.stream()
                                    .filter(projChar -> projChar.ternars().stream()
                                            .anyMatch(tm -> TernaryRingTest.ringIsomorphic(tm, matrix)))
                                    .findAny();
                            if (opt.isPresent()) {
                                return opt.get();
                            }
                        }
                    }
                }
            }
        }
        return new ProjChar(name, mappings);
    }

    public static ProjChar fromProj(String name, Liner proj, int transLine) {
        List<TernarMapping> mappings = new ArrayList<>();
        int pc = proj.pointCount();
        int order = proj.line(0).length - 1;
        int zero = IntStream.range(0, proj.pointCount()).filter(p -> !proj.flag(transLine, p)).findAny().orElseThrow();
        for (int dl : proj.lines(zero)) {
            int inftyPt = proj.intersection(transLine, dl);
            int[] line = proj.line(dl);
            for (int h : line) {
                int v = h == zero ? inftyPt : zero;
                for (int o = 0; o < pc; o++) {
                    if (proj.flag(dl, o)) {
                        continue;
                    }
                    int oh = proj.line(o, h);
                    int ov = proj.line(o, v);
                    for (int e = 0; e < pc; e++) {
                        if (proj.flag(dl, e) || proj.flag(ov, e) || proj.flag(oh, e)) {
                            continue;
                        }
                        int w = proj.intersection(proj.line(e, h), ov);
                        int u = proj.intersection(proj.line(e, v), oh);
                        Quad base = new Quad(o, u, w, e);
                        TernaryRing ring = new ProjectiveTernaryRing(name, proj, base);
                        int two = ring.op(1, 1, 1);
                        if (two == 0) {
                            continue;
                        }
                        CharVals cv = CharVals.of(ring, two, order);
                        if (!cv.induced()) {
                            continue;
                        }
                        if (mappings.isEmpty()) {
                            mappings.add(TernaryRingTest.fillTernarMapping(ring.toMatrix(), cv, two, order));
                        }
                        Characteristic fstChr = mappings.getFirst().chr();
                        boolean eq = fstChr.equals(cv.chr());
                        if (!eq) {
                            continue;
                        }
                        TernaryRing matrix = ring.toMatrix();
                        if (mappings.stream().noneMatch(tm -> TernaryRingTest.ringIsomorphic(tm, matrix))) {
                            mappings.add(TernaryRingTest.fillTernarMapping(matrix, cv, two, order));
                        }
                    }
                }
            }
        }
        return new ProjChar(name, mappings);
    }

    public static boolean isDesargues(Liner liner, int order) {
        int dl = 0;
        int o = order;
        int u = order + order;
        int w = order + 1;
        int ou = order;
        int ow = 1;
        int e = liner.intersection(liner.line(u, liner.intersection(dl, ow)), liner.line(w, liner.intersection(dl, ou)));
        TernaryRing ring = new ProjectiveTernaryRing("", liner, new Quad(o, u, w, e)).toMatrix();
        for (int x = 1; x < order; x++) {
            for (int y = x + 1; y < order; y++) {
                int xy = ring.mul(x, y);
                if (xy != ring.mul(y, x)) {
                    return false;
                }
                if (ring.mul(ring.mul(x, x), y) != ring.mul(x, xy)) {
                    return false;
                }
                for (int z = 1; z < order; z++) {
                    int yz = ring.add(y, z);
                    if (ring.op(x, y, z) != ring.add(xy, z)) {
                        return false;
                    }
                    if (ring.add(ring.add(x, y), z) != ring.add(x, yz)) {
                        return false;
                    }
                    if (ring.mul(x, yz) != ring.add(xy, ring.mul(x, z))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    public static int findTranslationLine(Liner liner) {
        for (int dl : IntStream.range(0, liner.lineCount()).toArray()) {
            int o = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(dl, p)).findAny().orElseThrow();
            int[] dropped = liner.line(dl);
            int v = dropped[0];
            int h = dropped[1];
            int oh = liner.line(o, h);
            int ov = liner.line(o, v);
            int e = IntStream.range(0, liner.pointCount()).filter(p -> !liner.flag(dl, p) && !liner.flag(ov, p) && !liner.flag(oh, p)).findAny().orElseThrow();
            int w = liner.intersection(liner.line(e, h), ov);
            int u = liner.intersection(liner.line(e, v), oh);
            Quad base = new Quad(o, u, w, e);
            TernaryRing ring = new ProjectiveTernaryRing("", liner, base).toMatrix();
            if (ring.isLinear() && ring.addAssoc() && ring.addComm() && ring.isRightDistributive()) {
                return dl;
            }
        }
        return -1;
    }

    @Test
    public void generateAlt() {
        int p = 3;
        int n = 4;
        System.out.println(p + " " + n);
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int mc = mini.cardinality();
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
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
            int[][] lines = toProjective(sp, newBase);
            Liner l = new Liner(lines.length, lines);
            if (isDesargues(l, mc)) {
                System.out.println("Desargues " + Arrays.toString(arr));
                return;
            }
            ProjChar chr = newTranslation(counter.toString(), l, projData);
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
        tree(helper, filterGl(helper, p), helper.gl(), Arrays.stream(helper.v()).boxed().toList(), partSpread, 0, cons);
    }

    private static List<Integer> filterGl(ModuloMatrixHelper helper, int p) {
        int[] gl = helper.gl();
        List<Integer> result = new ArrayList<>();
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

    private static boolean less(int[] cand, int[] arr) {
        for (int i = 0; i < cand.length; i++) {
            if (arr[i] < cand[i]) {
                return false;
            }
            if (cand[i] < arr[i]) {
                return true;
            }
        }
        return false;
    }

    private void tree(ModuloMatrixHelper helper, List<Integer> subGl, int[] gl, List<Integer> v, int[] partSpread, int idx, BiConsumer<int[], List<Integer>> sink) {
        int needed = partSpread.length - idx;
        if (v.size() < needed) {
            return;
        }
        if (needed == 0) {
            sink.accept(partSpread, v);
            return;
        }
        FixBS filter = new FixBS(helper.matCount());
        ex: for (int a : v) {
            if (filter.get(a)) {
                continue;
            }
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            for (int b : gl) {
                int[] res = new int[partSpread.length];
                for (int i = 0; i <= idx; i++) {
                    res[i] = helper.mul(helper.mul(b, newArr[i]), helper.inv(b));
                }
                Arrays.sort(res, 0, idx + 1);
                if (less(res, newArr)) {
                    continue ex;
                }
            }
            List<Integer> newV = new ArrayList<>(v.size());
            for (int b : v) {
                if (b > a && helper.hasInv(helper.sub(b, a))) {
                    newV.add(b);
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
            tree(helper, centralizer, gl, newV, newArr, idx + 1, sink);
        }
    }

    @Test
    public void generateSimples() throws IOException {
        int p = 2;
        int n = 8;
        System.out.println(p + " " + n);
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n / 2);
        int all = LinearSpace.pow(p, n / 2) - 2;
        File f = new File("/home/ihromant/maths/trans/", "simples-" + p + "^" + n + "x.txt");
        try (FileOutputStream fos = new FileOutputStream(f);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            BiConsumer<int[], List<Integer>> cons = (arr, vl) -> {
                int rest = all - IntStream.range(0, arr.length).filter(i -> arr[i] < 0).findAny().orElse(arr.length);
                if (rest <= vl.size()) {
                    ps.println(Arrays.toString(Arrays.stream(arr).filter(a -> a >= 0).toArray()));
                    ps.flush();
                }
            };
            int[] partSpread = new int[all];
            Arrays.fill(partSpread, -1);
            tree(helper, filterGl(helper, p), helper.gl(), Arrays.stream(helper.v()).boxed().toList(), partSpread, 0, cons);
        }
    }

    @Test
    public void printStatistics() throws IOException {
        int p = 3;
        int n = 6;
        System.out.println(p + " " + n);
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/trans/", "simples-" + p + "^" + n + "x.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr)) {
            int[] frq = new int[LinearSpace.pow(p, n / 2)];
            br.lines().forEach(line -> {
                int[] start = Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray();
                frq[start.length]++;
            });
            System.out.println(Arrays.toString(frq));
        }
    }

    @Test
    public void generateBySimple() throws IOException {
        int p = 2;
        int n = 10;
        System.out.println(p + " " + n);
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        int sc = sp.cardinality();
        int order = mini.cardinality();
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, half);
        FixBS first = new FixBS(sc);
        first.set(0, order);
        FixBS second = new FixBS(sc);
        for (int i = 0; i < order; i++) {
            second.set(i * order);
        }
        FixBS third = new FixBS(sc);
        for (int i = 0; i < order; i++) {
            third.set(order * i + i);
        }
        FixBS[] base = new FixBS[order + 1];
        base[0] = first;
        base[1] = second;
        base[2] = third;
        AtomicInteger counter = new AtomicInteger();
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
        om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
        Map<Characteristic, List<ProjChar>> projData = new ConcurrentHashMap<>(readKnown(order));
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/trans/", "simples-" + p + "^" + n + "x.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/simples-" + p + "^" + n + "processed.txt", true);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos);
             FileInputStream pris = new FileInputStream("/home/ihromant/maths/trans/simples-" + p + "^" + n + "processed.txt");
             InputStreamReader prisr = new InputStreamReader(pris);
             BufferedReader prbr = new BufferedReader(prisr)) {
            Set<List<Integer>> processed = new HashSet<>();
            prbr.lines().forEach(line -> processed.add(
                    Arrays.stream(line.substring(1, line.length() - 1).split(", ")).map(Integer::parseInt).toList()));
            int[][] starts = br.lines().<int[]>mapMulti((line, sink) -> {
                int[] start = Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray();
                if (processed.contains(Arrays.stream(start).boxed().toList())) {
                    return;
                }
                sink.accept(start);
            }).toArray(int[][]::new);
            AtomicInteger ai = new AtomicInteger();
            System.out.println("Remaining " + starts.length);
            Arrays.stream(starts).parallel().forEach(start -> {
                FixBS[] newBase = base.clone();
                for (int i = 0; i < start.length; i++) {
                    FixBS ln = new FixBS(sc);
                    int a = start[i];
                    for (int x = 1; x < order; x++) {
                        int ax = helper.mulVec(a, x);
                        ln.set(ax * order + x);
                    }
                    newBase[i + 3] = ln;
                }
                int from = 3 + start.length;
                Consumer<int[]> cons = arr -> {
                    FixBS[] finalBase = newBase.clone();
                    for (int i = 0; i < arr.length; i++) {
                        FixBS ln = new FixBS(sc);
                        int a = arr[i];
                        for (int x = 1; x < order; x++) {
                            int ax = helper.mulVec(a, x);
                            ln.set(ax * order + x);
                        }
                        finalBase[i + from] = ln;
                    }
                    int[][] lines = toProjective(sp, finalBase);
                    Liner l = new Liner(lines.length, lines);
                    if (isDesargues(l, order)) {
                        System.out.println("Desargues " + Arrays.toString(start) + " " + Arrays.toString(arr));
                        return;
                    }
                    ProjChar chr = newTranslation(counter.toString(), l, projData);
                    if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
                        projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new CopyOnWriteArrayList<>()).add(chr);
                        counter.incrementAndGet();
                        try {
                            System.out.println(om.writeValueAsString(chr));
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        System.out.println("New " + Arrays.toString(start) + " " + Arrays.toString(arr));
                        System.out.println("Spread " + Arrays.deepToString(finalBase));
                    } else {
                        System.out.println("Existing " + chr.name() + " " + Arrays.toString(start) + " " + Arrays.toString(arr));
                    }
                };
                int[] partSpread = new int[mini.cardinality() - 2 - start.length];
                int last = start[start.length - 1];
                int[] v = helper.v();
                int[] newV = new int[v.length - helper.vIdxes()[last]];
                int newVSize = 0;
                ex: for (int i = helper.vIdxes()[last] + 1; i < v.length; i++) {
                    int b = v[i];
                    for (int a : start) {
                        if (!helper.hasInv(helper.sub(b, a))) {
                            continue ex;
                        }
                    }
                    newV[newVSize++] = b;
                }
                treeSimple(helper, newV, newVSize, partSpread, 0, cons);
                ps.println(Arrays.toString(start));
                ps.flush();
                if (ai.incrementAndGet() % 1000 == 0) {
                    System.out.println(ai.get());
                }
            });
        }
    }

    private void treeSimplePar(ModuloMatrixHelper helper, int[] v, int vSize, int[] partSpread, int idx, Consumer<int[]> sink) {
        int needed = partSpread.length - idx;
        if (vSize < needed) {
            return;
        }
        if (needed == 0) {
            sink.accept(partSpread);
            return;
        }
        (idx <= 1 ? IntStream.range(0, vSize).parallel() : IntStream.range(0, vSize)).forEach(i -> {
            int a = v[i];
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            int[] newV = new int[vSize - i];
            int newVSize = 0;
            for (int j = i + 1; j < vSize; j++) {
                int b = v[j];
                if (helper.hasInv(helper.sub(b, a))) {
                    newV[newVSize++] = b;
                }
            }
            treeSimplePar(helper, newV, newVSize, newArr, idx + 1, sink);
        });
    }

    private void treeSimple(ModuloMatrixHelper helper, int[] v, int vSize, int[] partSpread, int idx, Consumer<int[]> sink) {
        int needed = partSpread.length - idx;
        if (vSize < needed) {
            return;
        }
        if (needed == 0) {
            sink.accept(partSpread);
            return;
        }
        for (int i = 0; i < vSize; i++) {
            int a = v[i];
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            int[] newV = new int[vSize - i];
            int newVSize = 0;
            for (int j = i + 1; j < vSize; j++) {
                int b = v[j];
                if (helper.hasInv(helper.sub(b, a))) {
                    newV[newVSize++] = b;
                }
            }
            treeSimple(helper, newV, newVSize, newArr, idx + 1, sink);
        }
    }

    @Test
    public void generateKnown() throws IOException {
        int k = 25;
        String desarg = "s1";
        try (FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/known-" + k + ".txt");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            Arrays.stream(Objects.requireNonNull(new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + k).listFiles())).parallel().forEach(f -> {
                String name = f.getName().substring(0, f.getName().indexOf('.'));
                if (desarg.equals(name)) {
                    return;
                }
                try (FileInputStream is = new FileInputStream(f);
                     InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
                     BufferedReader br = new BufferedReader(isr)) {
                    Liner proj = BatchAffineTest.readProj(br);
                    int transLine = findTranslationLine(proj);
                    if (transLine < 0) {
                        System.out.println("Not translation " + name);
                        return;
                    }
                    ProjChar chr = fromProj(name, proj, transLine);
                    ObjectMapper om = new ObjectMapper();
                    om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE);
                    om.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
                    ps.println(om.writeValueAsString(chr));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    public static Map<Characteristic, List<ProjChar>> readKnown(int order) throws IOException {
        List<ProjChar> chars = new ArrayList<>();
        try (FileInputStream is = new FileInputStream("/home/ihromant/maths/trans/known-" + order + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            ObjectMapper om = new ObjectMapper();
            SimpleModule module = new SimpleModule("CustomModel", Version.unknownVersion());

            SimpleAbstractTypeResolver resolver = new SimpleAbstractTypeResolver();
            resolver.addMapping(TernaryRing.class, MatrixTernaryRing.class);

            module.setAbstractTypes(resolver);

            om.registerModule(module);
            while ((line = br.readLine()) != null) {
                ProjChar chr = om.readValue(line, ProjChar.class);
                chars.add(chr);
            }
        }
        return chars.stream().collect(Collectors.groupingBy(pc -> pc.ternars().getFirst().chr(), Collectors.toCollection(CopyOnWriteArrayList::new)));
    }

    @Test
    public void filterSimples() throws IOException {
        int p = 2;
        int n = 10;
        System.out.println(p + " " + n);
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(2, n / 2);
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/trans/", "simples-" + p + "^" + n + "x.txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream("/home/ihromant/maths/trans/simples-" + p + "^" + n + "xx.txt");
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            int[] v = helper.v();
            br.lines().forEach(line -> {
                int[] start = Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray();
                if (Arrays.stream(v).parallel().anyMatch(b -> {
                    int[] res = new int[start.length];
                    for (int i = 0; i < start.length; i++) {
                        res[i] = helper.mul(helper.mul(b, start[i]), helper.inv(b));
                    }
                    Arrays.sort(res);
                    return less(res, start);
                })) {
                    return;
                }
                ps.println(line);
                ps.flush();
            });
        }
    }
}
