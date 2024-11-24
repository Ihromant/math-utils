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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TranslationPlaneTest {
    @Test
    public void checkSubspaces() {
        int p = 3;
        int n = 4;
        LinearSpace sp = LinearSpace.of(p, n);
        int half = sp.half();
        FixBS first = new FixBS(sp.cardinality());
        first.set(1, half);
        FixBS second = new FixBS(sp.cardinality());
        for (int i = 1; i < half; i++) {
            second.set(i * half);
        }
        FixBS union = first.union(second);
        FixBS[] hulls = generateSpaces(sp, union).toArray(FixBS[]::new);
        Arrays.sort(hulls, Comparator.reverseOrder());
        FixBS third = hulls[0];
        union.or(third);
        hulls = Arrays.stream(hulls).filter(h -> !union.intersects(h)).toArray(FixBS[]::new);
        System.out.println(hulls.length);
        FixBS fourth = hulls[0];
        union.or(fourth);
        hulls = Arrays.stream(hulls).filter(h -> !union.intersects(h)).toArray(FixBS[]::new);
        System.out.println(hulls.length);
        int[] idxes = calcIdxes(sp, hulls);
        AtomicInteger counter = new AtomicInteger();
        Map<Characteristic, List<ProjChar>> projData = new HashMap<>();
        Consumer<FixBS[]> cons = arr -> {
            int[][] lines = toProjective(sp, arr);
            Liner l = new Liner(lines.length, lines);
            if (isDesargues(l)) {
                return;
            }
            ProjChar chr = newTranslation(counter.toString(), l, projData);
            if (chr != null) {
                projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                System.out.println(counter.incrementAndGet() + Arrays.toString(arr));
            }
        };
        FixBS[] curr = new FixBS[half + 1];
        curr[0] = first;
        curr[1] = second;
        curr[2] = third;
        curr[3] = fourth;
        generate(curr, union, half - 3, hulls, idxes, cons);
        System.out.println(projData);
    }

    private static int[] calcIdxes(LinearSpace sp, FixBS[] spaces) {
        int max = sp.cardinality();
        int[] idxes = new int[max];
        for (int i = 1; i < idxes.length; i++) {
            FixBS top = FixBS.of(max, i);
            idxes[i] = -Arrays.binarySearch(spaces, idxes[i - 1], spaces.length, top, Comparator.reverseOrder()) - 1;
        }
        return idxes;
    }

    private static void generate(FixBS[] curr, FixBS union, int needed, FixBS[] hulls, int[] idxes, Consumer<FixBS[]> cons) {
        if (needed == 0) {
            cons.accept(curr);
            return;
        }
        int next = union.nextClearBit(1);
        for (int i = idxes[next - 1]; i < idxes[next]; i++) {
            FixBS bs = hulls[i];
            if (bs.intersects(union)) {
                continue;
            }
            FixBS[] newCurr = curr.clone();
            newCurr[curr.length - needed] = bs;
            generate(newCurr, union.union(bs), needed - 1, hulls, idxes, cons);
        }
    }

    private static Set<FixBS> generateSpaces(LinearSpace sp, FixBS filter) {
        int half = sp.half() - 1;
        return IntStream.range(half + 1, sp.cardinality()).boxed().flatMap(i -> {
            int[] curr = new int[sp.n() / 2];
            curr[0] = i;
            return generateSpaces(sp, curr, curr.length - 1);
        }).filter(ssp -> ssp.cardinality() == half && !ssp.intersects(filter)).collect(Collectors.toSet());
    }

    private static Stream<FixBS> generateSpaces(LinearSpace sp, int[] curr, int needed) {
        if (needed == 0) {
            return Stream.of(sp.hull(curr));
        }
        int prev = curr[curr.length - needed - 1];
        return IntStream.range(prev + 1, sp.cardinality()).boxed().flatMap(i -> {
            int[] newCurr = curr.clone();
            newCurr[curr.length - needed] = i;
            return generateSpaces(sp, newCurr, needed - 1);
        });
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

    public static boolean isDesargues(Liner liner) {
        int l0 = 0;
        int o = 0;
        for (int la : liner.lines(o)) {
            if (la == o) {
                continue;
            }
            for (int lb : liner.lines(o)) {
                if (lb == o || lb == la) {
                    continue;
                }
                for (int lc : liner.lines(o)) {
                    if (lc == o || lc == la || lc == lb) {
                        continue;
                    }
                    for (int a : liner.points(la)) {
                        if (a == o) {
                            continue;
                        }
                        for (int b : liner.points(lb)) {
                            if (b == o) {
                                continue;
                            }
                            int x = liner.intersection(liner.line(a, b), l0);
                            for (int c : liner.points(lc)) {
                                if (c == o || liner.collinear(a, b, c)) {
                                    continue;
                                }
                                int y = liner.intersection(liner.line(a, c), l0);
                                int z = liner.intersection(liner.line(b, c), l0);
                                for (int a1 : liner.points(la)) {
                                    if (a1 == a || a1 == o) {
                                        continue;
                                    }
                                    int b1 = liner.intersection(liner.line(a1, x), lb);
                                    int c1 = liner.intersection(liner.line(a1, y), lc);
                                    if (!liner.collinear(b1, c1, z)) {
                                        return false;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }
}
