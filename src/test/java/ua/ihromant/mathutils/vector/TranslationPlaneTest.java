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
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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

    @Test
    public void testGenerateAlt() {
        int p = 3;
        int n = 4;
        int half = n / 2;
        LinearSpace sp = LinearSpace.of(p, n);
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace mega = LinearSpace.of(mini.cardinality(), half);
        FixBS suitable = generateOperators(mini, mega);
        System.out.println(suitable.cardinality());
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
        Consumer<int[]> cons = arr -> {
            FixBS[] cand = base.clone();
            for (int i = 0; i < arr.length; i++) {
                int a = arr[i];
                FixBS elem = new FixBS(sc);
                elem.set(0);
                for (int x = 1; x < mc; x++) {
                    elem.set(ax(mini, mega, a, x) * mc + x);
                }
                cand[i + 3] = elem;
            }
            int[][] lines = toProjective(sp, cand);
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
        int[] baseArr = new int[mc - 2];
        generateAlt(mega, suitable, baseArr, 0, 0, cons);
    }

    private static void generateAlt(LinearSpace mega, FixBS suitable, int[] arr, int prev, int idx, Consumer<int[]> sink) {
        if (idx == arr.length) {
            sink.accept(arr);
            return;
        }
        ex: for (int a = suitable.nextSetBit(prev + 1); a >= 0; a = suitable.nextSetBit(a + 1)) {
            for (int i = 0; i < idx; i++) {
                int b = arr[i];
                if (!suitable.get(mega.sub(a, b))) {
                    continue ex;
                }
            }
            int[] newArr = arr.clone();
            newArr[idx] = a;
            generateAlt(mega, suitable, newArr, a, idx + 1, sink);
        }
    }

    private static FixBS generateOperators(LinearSpace mini, LinearSpace mega) {
        int max = mega.cardinality();
        FixBS result = new FixBS(max);
        ex: for (int a = 1; a < max; a++) {
            FixBS unique = new FixBS(mini.cardinality());
            for (int x = 1; x < mini.cardinality(); x++) {
                int ax = ax(mini, mega, a, x);
                if (ax == x || unique.get(ax)) {
                    continue ex;
                }
                unique.set(ax);
            }
            result.set(a);
        }
        return result;
    }

    private static int ax(LinearSpace mini, LinearSpace mega, int a, int x) {
        int[] crd = mega.toCrd(a);
        int[] mapped = new int[mini.n()];
        for (int k = 0; k < mini.n(); k++) {
            mapped[k] = mini.scalar(crd[k], x);
        }
        return mini.fromCrd(mapped);
    }

    @Test
    public void generateAlt123() {
        int p = 3;
        int n = 4;
        System.out.println(p + " " + n);
        int half = n / 2;
        LinearSpace mini = LinearSpace.of(p, half);
        LinearSpace sp = LinearSpace.of(p, n);
        FixBS invertible = generateInvertibleAlt(mini);
        int sc = sp.cardinality();
        int mc = mini.cardinality();
        int[] gl = invertible.stream().toArray();
        Map<Integer, Integer> mapGl = generateInvertibleGlAlt(gl, p, half);
        System.out.println(gl.length);
        int[] v = Arrays.stream(gl).filter(a -> !hasEigenOne(a, p, half, invertible)).toArray();
        System.out.println(v.length);
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
        Consumer<int[]> cons = arr -> {
            FixBS[] newBase = base.clone();
            for (int i = 0; i < arr.length; i++) {
                FixBS ln = new FixBS(sc);
                int a = arr[i];
                int[][] matrix = toMatrix(a, p, half);
                for (int x = 1; x < mc; x++) {
                    int[] vec = mini.toCrd(x);
                    int ax = mini.fromCrd(multiply(matrix, vec, p));
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
            if (chr != null) {
                projData.computeIfAbsent(chr.ternars().getFirst().chr(), k -> new ArrayList<>()).add(chr);
                System.out.println(chr);
            }
            System.out.println(Arrays.toString(arr));
        };
        int[] partSpread = new int[mini.cardinality() - 2];
        tree(p, half, mapGl, invertible, gl, v, partSpread, 0, cons);
    }

    private int sub(int a, int b, int p, int n) {
        int[][] aMat = toMatrix(a, p, n);
        int[][] bMat = toMatrix(b, p, n);
        return fromMatrix(sub(aMat, bMat, p), p);
    }

    private void tree(int p, int n, Map<Integer, Integer> inv, FixBS gl, int[] subGl, int[] v, int[] partSpread, int idx, Consumer<int[]> sink) {
        if (idx == partSpread.length) {
            sink.accept(partSpread);
            return;
        }
        int sz = LinearSpace.pow(p, n * n);
        FixBS filter = new FixBS(sz);
        for (int a : v) {
            if (filter.get(a)) {
                continue;
            }
            int[] newArr = partSpread.clone();
            newArr[idx] = a;
            int[] newV = Arrays.stream(v).filter(b -> {
                if (b <= a) {
                    return false;
                }
                int sub = sub(b, a, p, n);
                return gl.get(sub);
            }).toArray();
            FixBS centralizer = new FixBS(sz);
            int[][] aMatrix = toMatrix(a, p, n);
            for (int el : subGl) {
                int invEl = inv.get(el);
                int[][] invMatrix = toMatrix(invEl, p, n);
                int[][] matrix = toMatrix(el, p, n);
                int[][] multiplied = multiply(multiply(invMatrix, aMatrix, p), matrix, p);
                int prod = fromMatrix(multiplied, p);
                filter.set(prod);
                if (idx == 0) {
                    filter.set(inv.get(prod));
                }

                int[][] lMul = multiply(aMatrix, matrix, p);
                int[][] rMul = multiply(matrix, aMatrix, p);
                if (Arrays.deepEquals(lMul, rMul)) {
                    centralizer.set(el);
                }
            }
            tree(p, n, inv, gl, centralizer.stream().toArray(), newV, newArr, idx + 1, sink);
        }
    }

    private boolean hasEigenOne(int a, int p, int n, FixBS inv) {
        int[][] matrix = toMatrix(a, p, n);
        int[][] sub = sub(matrix, unity(n), p);
        return !inv.get(fromMatrix(sub, p));
    }

    private Map<Integer, Integer> generateInvertibleGlAlt(int[] gl, int p, int n) {
        Map<Integer, Integer> result = new HashMap<>();
        for (int i : gl) {
            int[][] matrix = toMatrix(i, p, n);
            if (result.containsKey(i)) {
                continue;
            }
            try {
                int[][] rev = MatrixInverseFiniteField.inverseMatrix(matrix, p);
                int inv = fromMatrix(rev, p);
                result.put(i, inv);
                result.put(inv, i);
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private Map<Integer, Integer> generateInvertibleGl(int[] gl, int p, int n) {
        Map<Integer, Integer> result = new HashMap<>();
        int one = fromMatrix(unity(n), p);
        for (int i : gl) {
            if (result.containsKey(i)) {
                continue;
            }
            for (int j : gl) {
                int[][] first = toMatrix(i, p, n);
                int[][] second = toMatrix(j, p, n);
                if (fromMatrix(multiply(first, second, p), p) == one) {
                    result.put(i, j);
                    result.put(j, i);
                    break;
                }
            }
        }
        return result;
    }

    private FixBS generateInvertibleAlt(LinearSpace sp) {
        int cnt = LinearSpace.pow(sp.p(), sp.n() * sp.n());
        FixBS result = new FixBS(cnt);
        for (int i = 0; i < cnt; i++) {
            int[][] matrix = toMatrix(i, sp.p(), sp.n());
            try {
                MatrixInverseFiniteField.inverseMatrix(matrix, sp.p());
                result.set(i);
            } catch (ArithmeticException e) {
                // ok
            }
        }
        return result;
    }

    private FixBS generateInvertible(LinearSpace sp) {
        int cnt = LinearSpace.pow(sp.p(), sp.n() * sp.n());
        FixBS result = new FixBS(cnt);
        ex: for (int i = 0; i < cnt; i++) {
            int[][] matrix = toMatrix(i, sp.p(), sp.n());
            for (int j = 1; j < sp.cardinality(); j++) {
                int[] vec = sp.toCrd(j);
                int[] mul = multiply(matrix, vec, sp.p());
                if (sp.fromCrd(mul) == 0) {
                    continue ex;
                }
            }
            result.set(i);
        }
        return result;
    }

    private int[][] sub(int[][] first, int[][] second, int p) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                result[i][j] = (p + first[i][j] - second[i][j]) % p;
            }
        }
        return result;
    }

    private int[] multiply(int[][] first, int[] arr, int p) {
        int[] result = new int[first.length];
        for (int i = 0; i < first.length; i++) {
            int sum = 0;
            for (int j = 0; j < first.length; j++) {
                sum = sum + first[i][j] * arr[j];
            }
            result[i] = sum % p;
        }
        return result;
    }

    private int[][] multiply(int[][] first, int[][] second, int p) {
        int[][] result = new int[first.length][first.length];
        for (int i = 0; i < first.length; i++) {
            for (int j = 0; j < first.length; j++) {
                int sum = 0;
                for (int k = 0; k < first.length; k++) {
                    sum = sum + first[i][k] * second[k][j];
                }
                result[i][j] = sum % p;
            }
        }
        return result;
    }

    private int[][] toMatrix(int a, int p, int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n * n; i++) {
            result[i / n][i % n] = a % p;
            a = a / p;
        }
        return result;
    }

    private int[][] unity(int n) {
        int[][] result = new int[n][n];
        for (int i = 0; i < n; i++) {
            result[i][i] = 1;
        }
        return result;
    }

    private int fromMatrix(int[][] matrix, int p) {
        int result = 0;
        for (int i = matrix.length * matrix.length - 1; i >= 0; i--) {
            result = result * p + matrix[i / matrix.length][i % matrix.length];
        }
        return result;
    }

    @Test
    public void testMatrices() {
        int p = 3;
        int n = 4;
        int half = n / 2;
        int all = LinearSpace.pow(p, half * half);
        for (int i = 0; i < all; i++) {
            int[][] matrix = toMatrix(i, p, half);
            int dConv = fromMatrix(matrix, p);
            System.out.println(i + " " + dConv + " " + Arrays.deepToString(matrix));
        }
    }
}
