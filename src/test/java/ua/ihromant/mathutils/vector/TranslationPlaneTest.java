package ua.ihromant.mathutils.vector;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Liner;
import ua.ihromant.mathutils.plane.CharVals;
import ua.ihromant.mathutils.plane.Characteristic;
import ua.ihromant.mathutils.plane.ProjChar;
import ua.ihromant.mathutils.plane.ProjectiveTernaryRing;
import ua.ihromant.mathutils.plane.Quad;
import ua.ihromant.mathutils.plane.TernarMapping;
import ua.ihromant.mathutils.plane.TernaryRing;
import ua.ihromant.mathutils.plane.TernaryRingTest;
import ua.ihromant.mathutils.util.FixBS;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.IntStream;

public class TranslationPlaneTest {
    @Test
    public void writeHulls() throws IOException {
        int p = 2;
        int n = 10;
        File f = new File("/home/ihromant/maths/", "spaces-" + p + "^" + n + ".txt");
        try (FileOutputStream fos = new FileOutputStream(f, true);
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

    @Test
    public void checkSubspaces() {
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
        Set<FixBS> distinct = new HashSet<>();
        List<FixBS> lst = new ArrayList<>();
        generateSpaces(sp, union, h -> {
            if (distinct.add(h)) {
                lst.add(h);
            }
        });
        FixBS[] hulls = lst.toArray(FixBS[]::new);
        System.out.println(hulls.length + " " + Arrays.stream(hulls).takeWhile(h -> h.nextSetBit(0) == third.nextSetBit(0) + 1).count());
        FixBS fourth = hulls[0];
        union.or(fourth);
        hulls = Arrays.stream(hulls).filter(h -> !union.intersects(h)).toArray(FixBS[]::new);
        System.out.println(hulls.length + " " + Arrays.stream(hulls).takeWhile(h -> h.nextSetBit(0) == fourth.nextSetBit(0) + 1).count());
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = new HashMap<>();
        Consumer<FixBS[]> cons = arr -> {
            int[][] lines = toProjective(sp, arr);
            Liner l = new Liner(lines.length, lines);
            if (isDesargues(l, half)) {
                return;
            }
            ProjChar chr = newTranslation(counter.toString(), l, projData);
            if (chr != null) {
                projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                System.out.println(counter.incrementAndGet() + Arrays.toString(arr));
                System.out.println(chr);
            }
        };
        FixBS[] curr = new FixBS[half + 1];
        curr[0] = first;
        curr[1] = second;
        curr[2] = third;
        curr[3] = fourth;
        generate(curr, union, half - 3, hulls, cons);
        System.out.println(projData);
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
                        if (existingChars != null && existingChars.stream()
                                .flatMap(projChar -> projChar.ternars().stream())
                                .anyMatch(tm -> TernaryRingTest.ringIsomorphic(tm, matrix))) {
                            return null;
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
}
