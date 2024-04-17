package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.*;

public class GaloisFieldTest {
    @Test
    public void testOrder() {
        GaloisField pf = new GaloisField(61);
        assertEquals(1, pf.mulOrder(1));
        assertEquals(60, pf.mulOrder(2));
        assertEquals(10, pf.mulOrder(3));
        assertEquals(30, pf.mulOrder(4));
        assertEquals(30, pf.mulOrder(5));
        assertEquals(3, pf.power(2, 6));
        GaloisField pf1 = new GaloisField(13);
        assertEquals(4, pf1.primitives().count());
    }

    @Test
    public void testCorrectness() {
        GaloisField fd = new GaloisField(61);
        testField(fd);
        fd = new GaloisField(4);
        testField(fd);
        fd = new GaloisField(32);
        testField(fd);
        fd = new GaloisField(27);
        testField(fd);
        fd = new GaloisField(64);
        testField(fd);
        try {
            new GaloisField(12);
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    private void testField(GaloisField field) {
        assertEquals(field.cardinality(), field.elements().count());
        field.elements().forEach(a -> field.elements() // a + b = b + a
                .forEach(b -> assertEquals(field.add(a, b), field.add(b, a))));
        field.elements().forEach(a -> field.elements().forEach(b -> field.elements() // (a + b) + c = a + (b + c)
                .forEach(c -> assertEquals(field.add(a, field.add(b, c)), field.add(field.add(a, b), c)))));
        field.elements().forEach(a -> assertEquals(field.add(a, 0), a)); // a + 0 = a
        field.elements().forEach(a -> assertEquals(field.mul(a, 0), 0)); // a * 0 = 0
        field.elements().forEach(a -> assertEquals(field.mul(a, 1), a)); // a * 1 = a
        field.elements().forEach(a -> field.elements()  // a * b = b * a
                .forEach(b -> assertEquals(field.mul(a, b), field.mul(b, a))));
        field.elements().forEach(a -> field.elements().forEach(b -> field.elements() // (a * b) * c = a * (b * c)
                .forEach(c -> assertEquals(field.mul(a, field.mul(b, c)), field.mul(field.mul(a, b), c)))));
        field.elements().forEach(a -> field.elements().forEach(b -> field.elements() // (a + b) * c = a * c + b * c
                .forEach(c -> assertEquals(field.mul(field.add(a, b), c), field.add(field.mul(a, c), field.mul(b, c))))));
        field.elements().forEach(a -> field.elements().forEach(b -> field.elements() // // a * (b + c) = a * b + a * c
                .forEach(c -> assertEquals(field.mul(a, field.add(b, c)), field.add(field.mul(a, b), field.mul(a, c))))));
        field.elements().skip(1).forEach(a -> assertEquals(field.mul(a, field.inverse(a)), 1));
    }

    @Test // a + b = b + a
    public void testCommutativeAddition() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                assertEquals(SemiField.add(a, b), SemiField.add(b, a));
            }
        }
    }

    @Test // (a + b) + c = a + (b + c)
    public void testAssociativeAddition() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.add(a, SemiField.add(b, c)), SemiField.add(SemiField.add(a, b), c));
                }
            }
        }
    }

    @Test // (a * b) * c != (a * b) * c
    public void testAssociativeMultiplication() {
        int counter = 0;
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    int ABc = SemiField.mul(SemiField.mul(a, b), c);
                    int aBC = SemiField.mul(a, SemiField.mul(b, c));
                    if (ABc != aBC) {
                        counter++;
                    }
                }
            }
        }
        assertEquals(10368, counter); // 2 ^ 7 * 3 ^ 4
    }

    @Test // a * b = b * a
    public void testCommutativeMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                assertEquals(SemiField.mul(a, b), SemiField.mul(b, a));
            }
        }
    }

    @Test // (a + b) * c = a * c + b * c
    public void testRightDistributiveMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.mul(SemiField.add(a, b), c),
                            SemiField.add(SemiField.mul(a, c), SemiField.mul(b, c)));
                }
            }
        }
    }

    @Test // a * (b + c) = a * b + a * c
    public void testLeftDistributiveMultiplication() {
        for (int a = 0; a < SemiField.SIZE; a++) {
            for (int b = 0; b < SemiField.SIZE; b++) {
                for (int c = 0; c < SemiField.SIZE; c++) {
                    assertEquals(SemiField.mul(a, SemiField.add(b, c)),
                            SemiField.add(SemiField.mul(a, b), SemiField.mul(a, c)));
                }
            }
        }
    }

    @Test
    public void testSolve() {
        GaloisField fd = new GaloisField(25);
        assertArrayEquals(new int[]{13, 18}, fd.solve(new int[]{1, 4, 2}).toArray());
        GaloisField fd1 = new GaloisField(4);
        assertArrayEquals(new int[]{2, 3}, fd1.solve(new int[]{1, 1, 1}).toArray());
        assertArrayEquals(new int[]{1}, fd1.solve(new int[]{1, 0, 1}).toArray());
    }

    @Test
    public void testPermutations() {
        assertArrayEquals(new int[][]{{0, 2, 4}, {0, 4, 2}, {2, 0, 4}, {2, 4, 0}, {4, 0, 2}, {4, 2, 0}}, GaloisField.permutations(new int[]{0, 2, 4}).toArray(int[][]::new));
        assertEquals(120, GaloisField.permutations(IntStream.range(0, 5).toArray()).count());
        assertEquals(39916800, GaloisField.permutations(IntStream.range(0, 11).toArray()).count());
    }

    @Test
    public void testChoices() {
        assertArrayEquals(new int[][]{{0, 1}, {0, 2}, {0, 3}, {1, 2}, {1, 3}, {2, 3}}, GaloisField.choices(4, 2).toArray(int[][]::new));
        assertEquals(35, GaloisField.choices(7, 4).count());
        assertEquals(126, GaloisField.choices(9, 5).count());
    }

    //@Test
    public void testDistinctPermutations() {
        int[] diffSet = new int[]{3, 6, 7, 12, 14};
        HyperbolicPlane plane = new HyperbolicPlane(new int[][]{diffSet});
        List<BitSet> lines = StreamSupport.stream(plane.lines().spliterator(), false).map(plane::line).toList();
        System.out.println(lines.size());
        int counter = 1_000_000_000;
        int[] arr = IntStream.range(0, lines.size()).toArray();
        while (counter-- >= 0) {
            int[] fPerm = arr.clone();
            shuffle(fPerm);
            List<BitSet> fLines = lines.stream().map(line -> line.stream()
                    .map(i -> fPerm[i]).collect(BitSet::new, BitSet::set, BitSet::or)).toList();
            if (fLines.stream().noneMatch(lines::contains)) {
                int[] sPerm = arr.clone();
                shuffle(sPerm);
                List<BitSet> sLines = lines.stream().map(line -> line.stream()
                        .map(i -> sPerm[i]).collect(BitSet::new, BitSet::set, BitSet::or)).toList();
                if (fLines.stream().noneMatch(sLines::contains) && lines.stream().noneMatch(sLines::contains)) {
                    int[] tPerm = arr.clone();
                    shuffle(tPerm);
                    List<BitSet> tLines = lines.stream().map(line -> line.stream()
                            .map(i -> tPerm[i]).collect(BitSet::new, BitSet::set, BitSet::or)).toList();
                    if (fLines.stream().noneMatch(tLines::contains) && lines.stream().noneMatch(tLines::contains)
                            && sLines.stream().noneMatch(tLines::contains)) {
                        int[] fourPerm = arr.clone();
                        shuffle(fourPerm);
                        List<BitSet> fourLines = lines.stream().map(line -> line.stream()
                                .map(i -> fourPerm[i]).collect(BitSet::new, BitSet::set, BitSet::or)).toList();
                        if (fLines.stream().noneMatch(fourLines::contains) && lines.stream().noneMatch(fourLines::contains)
                                && sLines.stream().noneMatch(fourLines::contains) && tLines.stream().noneMatch(fourLines::contains)) {
                            printDesign(lines);
                            printDesign(fLines);
                            printDesign(sLines);
                            printDesign(tLines);
                            printDesign(fourLines);
                            System.out.println();
                        }
                    }
                }
            }
        }
    }

    private void printDesign(List<BitSet> design) {
        System.out.println(design.stream().map(bitSet -> String.valueOf(Character.forDigit(bitSet.stream()
                .skip(0).findAny().orElseThrow(), 36))).collect(Collectors.joining()));
        System.out.println(design.stream().map(bitSet -> String.valueOf(Character.forDigit(bitSet.stream()
                .skip(1).findAny().orElseThrow(), 36))).collect(Collectors.joining()));
        System.out.println(design.stream().map(bitSet -> String.valueOf(Character.forDigit(bitSet.stream()
                .skip(2).findAny().orElseThrow(), 36))).collect(Collectors.joining()));
        System.out.println(design.stream().map(bitSet -> String.valueOf(Character.forDigit(bitSet.stream()
                .skip(3).findAny().orElseThrow(), 36))).collect(Collectors.joining()));
        System.out.println(design.stream().map(bitSet -> String.valueOf(Character.forDigit(bitSet.stream()
                .skip(4).findAny().orElseThrow(), 36))).collect(Collectors.joining()));
        System.out.println("------------------------------------------------------------------");
    }

    public static void shuffle(int[] arr) {
        int size = arr.length;
        for (int i = size; i > 1; i--) {
            swap(arr, i - 1, ThreadLocalRandom.current().nextInt(i));
        }
    }

    private static void swap(int[] arr, int i, int j) {
        int tmp = arr[i];
        arr[i] = arr[j];
        arr[j] = tmp;
    }

    @Test
    public void printGood() {
        for (int p = 1; p < 20; p++) {
            int k = 6 * p + 1;
            int v = 18 * p + 7;
            if (v % 12 == 1) {
                System.out.println(k + " " + v);
            }
        }
        for (int p = 1; p < 20; p++) {
            int k = 6 * p + 3;
            int v = 18 * p + 13;
            if (v % 12 == 1) {
                System.out.println(k + " " + v);
            }
        }
    }

    @Test
    public void testParity() {
        System.out.println(GaloisField.permutations(new int[]{0, 1, 2, 3}).collect(Collectors.groupingBy(GaloisField::parity, Collectors.counting())));
        System.out.println(GaloisField.permutations(new int[]{0, 1, 2, 3, 4}).collect(Collectors.groupingBy(GaloisField::parity, Collectors.counting())));
        System.out.println(GaloisField.permutations(new int[]{0, 1, 2, 3, 4, 5}).collect(Collectors.groupingBy(GaloisField::parity, Collectors.counting())));
        System.out.println(GaloisField.permutations(new int[]{0, 1, 2, 3, 4, 5, 6, 7}).collect(Collectors.groupingBy(GaloisField::parity, Collectors.counting())));
        System.out.println(GaloisField.permutations(new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8}).collect(Collectors.groupingBy(GaloisField::parity, Collectors.counting())));
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    private static int[] getHomogenousSpace(int p, int ord) {
        int cb = ord * ord * ord;
        if (p < cb) {
            return new int[]{p / ord / ord, p / ord % ord, p % ord, 1};
        }
        p = p - cb;
        int sqr = ord * ord;
        if (p < sqr) {
            return new int[]{p / ord, p % ord, 1, 0};
        }
        p = p - sqr;
        if (p < ord) {
            return new int[]{p, 1, 0, 0};
        } else {
            return new int[]{1, 0, 0, 0};
        }
    }

    private static int[] getHomogenous(int p, int ord) {
        int sqr = ord * ord;
        if (p < sqr) {
            return new int[]{p / ord, p % ord, 1};
        }
        p = p - sqr;
        if (p < ord) {
            return new int[]{p, 1, 0};
        } else {
            return new int[]{1, 0, 0};
        }
    }

    private static int fromHomogeneous(int[] p, GaloisField fd) {
        int inv = fd.inverse(p[2] == 0 ? p[1] == 0 ? p[0] : p[1] : p[2]);
        int[] normed = IntStream.of(p).map(i -> fd.mul(i, inv)).toArray();
        if (p[2] == 1) {
            return normed[0] * fd.cardinality() + normed[1];
        }
        if (p[1] == 1) {
            return fd.cardinality() * fd.cardinality() + normed[0];
        }
        return fd.cardinality() * fd.cardinality() + fd.cardinality();
    }

    @Test
    public void generateUnital() {
        int q = 4;
        int ord = q * q;
        int v = ord * ord + ord + 1;
        GaloisField fd = new GaloisField(ord);
        HyperbolicPlane pl = new HyperbolicPlane(fd.generatePlane());
        assertEquals(v, pl.pointCount());
        assertEquals(v, pl.lineCount());
        BitSet unital = new BitSet();
        for (int p : pl.points()) {
            int[] hom = getHomogenous(p, ord);
            int val = Arrays.stream(hom).map(crd -> fd.power(crd, q + 1)).reduce(0, fd::add);
            if (val == 0) {
                unital.set(p);
            }
        }
        HyperbolicPlane uni = pl.subPlane(unital.stream().toArray());
        assertEquals(ord * q + 1, uni.pointCount());
        HyperbolicPlaneTest.testCorrectness(uni, of(q + 1));
        System.out.println(uni.hyperbolicIndex());
    }

    @Test
    public void generate3DUnital() {
        int q = 3;
        int ord = q * q;
        GaloisField fd = new GaloisField(ord);
        HyperbolicPlane pl = new HyperbolicPlane(fd.generateSpace());
        HyperbolicPlaneTest.testCorrectness(pl, of(ord + 1));
        //assertEquals(of(ord * ord + ord + 1), pl.cardSubPlanes(false));
        //checkSpace(pl, pl.pointCount(), pl.pointCount());
        BitSet unital = new BitSet();
        for (int p : pl.points()) {
            int[] hom = getHomogenousSpace(p, ord);
            int val = Arrays.stream(hom).map(crd -> fd.power(crd, q + 1)).reduce(0, fd::add);
            if (val == 0) {
                unital.set(p);
            }
        }
        HyperbolicPlane uni = pl.subPlane(unital.stream().toArray());
        assertEquals(280, uni.pointCount());
        //HyperbolicPlaneTest.testCorrectness(uni, of(q + 1, ord + 1));
        //assertEquals(of(28, 37), uni.cardSubPlanes(true));
        //checkSpace(uni, 280, 280);
        //System.out.println(uni.hyperbolicIndex());
        int[] point37Array = of(0, 1, 2, 3, 31, 32, 33, 34, 68, 69, 70, 71, 83, 84, 85, 86, 117, 118, 119, 120, 130, 131, 132, 133, 178, 179, 180, 181, 191, 192, 193, 194, 228, 229, 230, 231, 268).stream().toArray();
        int[] point28Array = of(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 276, 277, 278, 279).stream().toArray();
        HyperbolicPlane pl37 = uni.subPlane(point37Array);
        HyperbolicPlane pl28 = uni.subPlane(point28Array);
        HyperbolicPlaneTest.testCorrectness(pl37, of(q + 1, ord + 1));
        HyperbolicPlaneTest.testCorrectness(pl28, of(q + 1));
        System.out.println(pl37.hyperbolicIndex());
        System.out.println(pl28.hyperbolicIndex());
    }

    @Test
    public void generate3DUnital1() {
        int q = 2;
        int ord = q * q;
        GaloisField fd = new GaloisField(ord);
        HyperbolicPlane pl = new HyperbolicPlane(fd.generateSpace());
        HyperbolicPlaneTest.testCorrectness(pl, of(ord + 1));
        //assertEquals(of(ord * ord + ord + 1), pl.cardSubPlanes(false));
        //checkSpace(pl, pl.pointCount(), pl.pointCount());
        BitSet unital = new BitSet();
        for (int p : pl.points()) {
            int[] hom = getHomogenousSpace(p, ord);
            int val = Arrays.stream(hom).map(crd -> fd.power(crd, q + 1)).reduce(0, fd::add);
            if (val == 0) {
                unital.set(p);
            }
        }
        HyperbolicPlane uni = pl.subPlane(unital.stream().toArray());
        assertEquals(45, uni.pointCount());
        HyperbolicPlaneTest.testCorrectness(uni, of(q + 1, ord + 1));
        assertEquals(of(9, 13), uni.cardSubPlanes(true));
        System.out.println(uni.hyperbolicIndex());
        int[] point13Array = of(0, 1, 2, 7, 8, 9, 20, 21, 22, 33, 34, 35, 39).stream().toArray();
        int[] point9Array = of(0, 1, 2, 3, 4, 5, 42, 43, 44).stream().toArray();
        HyperbolicPlane pl13 = uni.subPlane(point13Array);
        HyperbolicPlane pl9 = uni.subPlane(point9Array);
        HyperbolicPlaneTest.testCorrectness(pl13, of(q + 1, ord + 1));
        HyperbolicPlaneTest.testCorrectness(pl9, of(q + 1));
        System.out.println(pl13.hyperbolicIndex());
        System.out.println(pl9.hyperbolicIndex());
    }

    @Test
    public void generateBeltramiKlein() {
        int q = 9;
        GaloisField fd = new GaloisField(q);
        HyperbolicPlane prSp = new HyperbolicPlane(fd.generateSpace());
        BitSet pts = new BitSet();
        for (int p : prSp.points()) {
            int[] crds = getHomogenousSpace(p, q);
            if (Arrays.stream(crds).allMatch(c -> c != 0)) {
                pts.set(p);
            }
        }
        HyperbolicPlane bks = prSp.subPlane(pts.stream().toArray());
        HyperbolicPlaneTest.testCorrectness(bks, of(q - 3, q - 2, q - 1));
        Set<BitSet> planes = new HashSet<>();
        for (int i = 0; i < bks.pointCount(); i++) {
            for (int j = i + 1; j < bks.pointCount(); j++) {
                for (int k = j + 1; k < bks.pointCount(); k++) {
                    if (bks.line(i, j) == bks.line(j, k)) {
                        continue;
                    }
                    BitSet plane = bks.hull(i, j, k);
                    if (!planes.add(plane)) {
                        continue;
                    }
                    int[] plPts = plane.stream().toArray();
                    HyperbolicPlane bkp = bks.subPlane(plPts);
                    assertTrue(bkp.isRegular());
                    assertEquals(bkp.playfairIndex(), bkp.pointCount() == (q - 1) * (q - 1) ? of(2, 3) : of(2, 3, 4));
                    for (int b : plPts) {
                        for (int l : bks.lines(b)) {
                            BitSet line = bks.line(l);
                            if (line.stream().allMatch(plane::get)) {
                                continue;
                            }
                            BitSet hull = new BitSet();
                            for (int p1 : plPts) {
                                for (int p2 : bks.points(l)) {
                                    if (p1 == p2) {
                                        continue;
                                    }
                                    hull.or(bks.line(bks.line(p1, p2)));
                                }
                            }
                            if (hull.cardinality() < bks.pointCount()) {
                                System.out.println(hull.cardinality() + " " + bks.pointCount());
                                return;
                            }
                        }
                    }
                }
            }
        }
        System.out.println("Regular");
        //assertEquals(of((q - 2) * (q - 1), (q - 2) * (q - 1) + 1, (q - 1) * (q - 1)), bk.cardSubPlanes(true));
        assertEquals((q - 1) * (q - 1) * (q - 1), bks.pointCount());
    }

    private static int mapPoint(int pt, GaloisField fd, int q) {
        return fromHomogeneous(Arrays.stream(getHomogenous(pt, fd.cardinality())).map(i -> fd.power(i, q)).toArray(), fd);
    }

    private static BitSet mapLine(BitSet line, GaloisField fd, int q) {
        return line.stream().map(pt -> mapPoint(pt, fd, q)).collect(BitSet::new, BitSet::set, BitSet::or);
    }

    @Test
    public void testFigueroa() {
        int q = 4;
        GaloisField fd = new GaloisField(q * q * q);
        HyperbolicPlane proj = new HyperbolicPlane(fd.generatePlane());
        int[] pointTypes = IntStream.range(0, proj.pointCount()).map(pt -> {
            int pa = mapPoint(pt, fd, q);
            int pa2 = mapPoint(pa, fd, q);
            if (pa == pt) {
                return 0;
            }
            return proj.line(pt, pa) == proj.line(pa, pa2) ? 1 : 2;
        }).toArray();
        int[] lineTypes = IntStream.range(0, proj.lineCount()).map(l -> {
            BitSet l0 = proj.line(l);
            BitSet la = mapLine(l0, fd, q);
            if (l0.equals(la)) {
                return 0;
            }
            int common = l0.stream().filter(p -> la.get(p)).findAny().orElseThrow();
            return mapLine(la, fd, q).get(common) ? 1 : 2;
        }).toArray();
        System.out.println("b");
        int[][] incidence = new int[proj.pointCount()][proj.lineCount()];
        for (int i = 0; i < proj.pointCount(); i++) {
            System.out.println(i);
            for (int j = 0; j < proj.lineCount(); j++) {
                if (pointTypes[i] == 2 && lineTypes[j] == 2) {
                    int pa = mapPoint(i, fd, q);
                    int pm = proj.line(pa, mapPoint(pa, fd, q));
                    BitSet la = mapLine(proj.line(j), fd, q);
                    BitSet la2 = mapLine(la, fd, q);
                    int lm = la.stream().filter(la2::get).findAny().orElseThrow();
                    incidence[i][j] = proj.line(pm).get(lm) ? 1 : 0;
                } else {
                    incidence[i][j] = proj.line(j).get(i) ? 1 : 0;
                }
            }
        }
        System.out.println("c");
        BitSet[] lines = IntStream.range(0, proj.pointCount()).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        for (int i = 0; i < proj.pointCount(); i++) {
            int[] chars = incidence[i];
            for (int j = 0; j < proj.lineCount(); j++) {
                if (chars[j] == 1) {
                    lines[j].set(i);
                }
            }
        }
        System.out.println("d");
        HyperbolicPlane figueroa = new HyperbolicPlane(lines);
        HyperbolicPlaneTest.testCorrectness(figueroa, of(fd.cardinality() + 1));
        for (int l : figueroa.lines()) {
            System.out.println(figueroa.line(l).stream().mapToObj(Integer::toString).collect(Collectors.joining(" ")));
        }
    }
}
