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
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
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
                System.out.println("Desargues");
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
        tree(helper, filterGl(helper, p), Arrays.stream(helper.v()).boxed().toList(), partSpread, 0, cons);
    }

    private List<Integer> filterGl(ModuloMatrixHelper helper, int p) {
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

    private void tree(ModuloMatrixHelper helper, List<Integer> subGl, List<Integer> v, int[] partSpread, int idx, BiConsumer<int[], List<Integer>> sink) {
        int needed = partSpread.length - idx;
        if (v.size() < needed) {
            return;
        }
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
            tree(helper, centralizer, newV, newArr, idx + 1, sink);
        }
    }

    @Test
    public void generateSimples() throws IOException {
        int p = 2;
        int n = 10;
        System.out.println(p + " " + n);
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
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
            tree(helper, filterGl(helper, p), Arrays.stream(helper.v()).boxed().toList(), partSpread, 0, cons);
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
        ModuloMatrixHelper helper = ModuloMatrixHelper.of(p, n);
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
            List<int[]> starts = br.lines().<int[]>mapMulti((line, sink) -> {
                int[] start = Arrays.stream(line.substring(1, line.length() - 1).split(", ")).mapToInt(Integer::parseInt).toArray();
                if (start.length != 5 || processed.contains(Arrays.stream(start).boxed().toList())) {
                    return;
                }
                sink.accept(start);
            }).toList();
            AtomicInteger ai = new AtomicInteger();
            System.out.println("Remaining " + starts.size());
            starts.stream().parallel().forEach(start -> {
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
                List<Integer> newV = new ArrayList<>(v.length - helper.vIdxes()[last]);
                ex: for (int i = helper.vIdxes()[last] + 1; i < v.length; i++) {
                    int b = v[i];
                    for (int a : start) {
                        if (!helper.hasInv(helper.sub(b, a))) {
                            continue ex;
                        }
                    }
                    newV.add(b);
                }
                treeSimple(helper, newV, partSpread, 0, cons);
                ps.println(Arrays.toString(start));
                ps.flush();
                if (ai.incrementAndGet() % 1000 == 0) {
                    System.out.println(ai.get());
                }
            });
        }
    }

    private void treeSimple(ModuloMatrixHelper helper, List<Integer> v, int[] partSpread, int idx, Consumer<int[]> sink) {
        int needed = partSpread.length - idx;
        if (v.size() < needed) {
            return;
        }
        if (needed == 0) {
            sink.accept(partSpread);
            return;
        }
        for (int a : v) {
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            List<Integer> newV = new ArrayList<>(v.size());
            for (int b : v) {
                if (b > a && helper.hasInv(helper.sub(b, a))) {
                    newV.add(b);
                }
            }
            treeSimple(helper, newV, newArr, idx + 1, sink);
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

    private Map<Characteristic, List<ProjChar>> readKnown(int order) throws IOException {
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

    private static final int[][] candidate = {{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31}, {32, 64, 96, 128, 160, 192, 224, 256, 288, 320, 352, 384, 416, 448, 480, 512, 544, 576, 608, 640, 672, 704, 736, 768, 800, 832, 864, 896, 928, 960, 992}, {33, 66, 99, 132, 165, 198, 231, 264, 297, 330, 363, 396, 429, 462, 495, 528, 561, 594, 627, 660, 693, 726, 759, 792, 825, 858, 891, 924, 957, 990, 1023}, {34, 67, 97, 136, 170, 203, 233, 272, 306, 339, 369, 408, 442, 475, 505, 524, 558, 591, 621, 644, 678, 711, 741, 796, 830, 863, 893, 916, 950, 983, 1013}, {35, 65, 98, 140, 175, 205, 238, 280, 315, 345, 378, 404, 439, 469, 502, 540, 575, 605, 638, 656, 691, 721, 754, 772, 807, 837, 870, 904, 939, 969, 1002}, {36, 72, 108, 144, 180, 216, 252, 257, 293, 329, 365, 401, 437, 473, 509, 518, 546, 590, 618, 662, 690, 734, 762, 775, 803, 847, 875, 919, 947, 991, 1019}, {37, 74, 111, 148, 177, 222, 251, 265, 300, 323, 358, 413, 440, 471, 498, 534, 563, 604, 633, 642, 679, 712, 749, 799, 826, 853, 880, 907, 942, 961, 996}, {38, 75, 109, 152, 190, 211, 245, 273, 311, 346, 380, 393, 431, 450, 484, 522, 556, 577, 615, 658, 692, 729, 767, 795, 829, 848, 886, 899, 933, 968, 1006}, {39, 73, 110, 156, 187, 213, 242, 281, 318, 336, 375, 389, 418, 460, 491, 538, 573, 595, 628, 646, 673, 719, 744, 771, 804, 842, 877, 927, 952, 982, 1009}, {40, 87, 127, 157, 181, 202, 226, 267, 291, 348, 372, 406, 446, 449, 489, 529, 569, 582, 622, 652, 676, 731, 755, 794, 818, 845, 869, 903, 943, 976, 1016}, {41, 85, 124, 153, 176, 204, 229, 259, 298, 342, 383, 410, 435, 463, 486, 513, 552, 596, 637, 664, 689, 717, 740, 770, 811, 855, 894, 923, 946, 974, 999}, {42, 84, 126, 149, 191, 193, 235, 283, 305, 335, 357, 398, 420, 474, 496, 541, 567, 585, 611, 648, 674, 732, 758, 774, 812, 850, 888, 915, 953, 967, 1005}, {43, 86, 125, 145, 186, 199, 236, 275, 312, 325, 366, 386, 425, 468, 511, 525, 550, 603, 624, 668, 695, 714, 737, 798, 821, 840, 867, 911, 932, 985, 1010}, {44, 95, 115, 141, 161, 210, 254, 266, 294, 341, 377, 391, 427, 472, 500, 535, 571, 584, 612, 666, 694, 709, 745, 797, 817, 834, 878, 912, 956, 975, 995}, {45, 93, 112, 137, 164, 212, 249, 258, 303, 351, 370, 395, 422, 470, 507, 519, 554, 602, 631, 654, 675, 723, 766, 773, 808, 856, 885, 908, 929, 977, 1020}, {46, 92, 114, 133, 171, 217, 247, 282, 308, 326, 360, 415, 433, 451, 493, 539, 565, 583, 617, 670, 688, 706, 748, 769, 815, 861, 883, 900, 938, 984, 1014}, {47, 94, 113, 129, 174, 223, 240, 274, 317, 332, 355, 403, 444, 461, 482, 523, 548, 597, 634, 650, 677, 724, 763, 793, 822, 839, 872, 920, 951, 966, 1001}, {48, 90, 106, 146, 162, 200, 248, 276, 292, 334, 382, 390, 438, 476, 492, 543, 559, 581, 629, 653, 701, 727, 743, 779, 827, 849, 865, 921, 937, 963, 1011}, {49, 88, 105, 150, 167, 206, 255, 284, 301, 324, 373, 394, 443, 466, 483, 527, 574, 599, 614, 665, 680, 705, 752, 787, 802, 843, 890, 901, 948, 989, 1004}, {50, 89, 107, 154, 168, 195, 241, 260, 310, 349, 367, 414, 428, 455, 501, 531, 545, 586, 632, 649, 699, 720, 738, 791, 805, 846, 892, 909, 959, 980, 998}, {51, 91, 104, 158, 173, 197, 246, 268, 319, 343, 356, 402, 417, 457, 506, 515, 560, 600, 619, 669, 686, 710, 757, 783, 828, 852, 871, 913, 930, 970, 1017}, {52, 82, 102, 130, 182, 208, 228, 277, 289, 327, 371, 407, 419, 453, 497, 537, 557, 587, 639, 667, 687, 713, 765, 780, 824, 862, 874, 910, 954, 988, 1000}, {53, 80, 101, 134, 179, 214, 227, 285, 296, 333, 376, 411, 430, 459, 510, 521, 572, 601, 620, 655, 698, 735, 746, 788, 801, 836, 881, 914, 935, 962, 1015}, {54, 81, 103, 138, 188, 219, 237, 261, 307, 340, 354, 399, 441, 478, 488, 533, 547, 580, 626, 671, 681, 718, 760, 784, 806, 833, 887, 922, 940, 971, 1021}, {55, 83, 100, 142, 185, 221, 234, 269, 314, 350, 361, 387, 436, 464, 487, 517, 562, 598, 609, 651, 700, 728, 751, 776, 831, 859, 876, 902, 945, 981, 994}, {56, 77, 117, 143, 183, 194, 250, 287, 295, 338, 362, 400, 424, 477, 485, 526, 566, 579, 635, 641, 697, 716, 756, 785, 809, 860, 868, 926, 934, 979, 1003}, {57, 79, 118, 139, 178, 196, 253, 279, 302, 344, 353, 412, 421, 467, 490, 542, 551, 593, 616, 661, 684, 730, 739, 777, 816, 838, 895, 898, 955, 973, 1012}, {58, 78, 116, 135, 189, 201, 243, 271, 309, 321, 379, 392, 434, 454, 508, 514, 568, 588, 630, 645, 703, 715, 753, 781, 823, 835, 889, 906, 944, 964, 1022}, {59, 76, 119, 131, 184, 207, 244, 263, 316, 331, 368, 388, 447, 456, 499, 530, 553, 606, 613, 657, 682, 733, 742, 789, 814, 857, 866, 918, 941, 986, 993}, {60, 69, 121, 159, 163, 218, 230, 286, 290, 347, 359, 385, 445, 452, 504, 520, 564, 589, 625, 663, 683, 722, 750, 790, 810, 851, 879, 905, 949, 972, 1008}, {61, 71, 122, 155, 166, 220, 225, 278, 299, 337, 364, 397, 432, 458, 503, 536, 549, 607, 610, 643, 702, 708, 761, 782, 819, 841, 884, 917, 936, 978, 1007}, {62, 70, 120, 151, 169, 209, 239, 270, 304, 328, 374, 409, 423, 479, 481, 516, 570, 578, 636, 659, 685, 725, 747, 778, 820, 844, 882, 925, 931, 987, 997}, {63, 68, 123, 147, 172, 215, 232, 262, 313, 322, 381, 405, 426, 465, 494, 532, 555, 592, 623, 647, 696, 707, 764, 786, 813, 854, 873, 897, 958, 965, 1018}};

    @Test
    public void test() throws IOException {
        int p = 2;
        int n = 10;
        LinearSpace sp = LinearSpace.of(p, n);
        FixBS[] spread = Arrays.stream(candidate).map(arr -> FixBS.of(1024, arr)).toArray(FixBS[]::new);
        int[][] lines = toProjective(sp, spread);
        Liner l = new Liner(lines.length, lines);
        Map<Characteristic, List<ProjChar>> projData = readKnown(32);
        ProjChar chr = newTranslation("new?", l, projData);
        if (projData.values().stream().flatMap(List::stream).noneMatch(pd -> pd == chr)) {
            System.out.println(chr);
            projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
        } else {
            System.out.println("Existing " + chr.name());
        }
    }
}
