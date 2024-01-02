package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.Group;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchHyperbolicPlaneTest {
    private ZipInputStream getZis(InputStream is) throws IOException {
        ZipInputStream zis = new ZipInputStream(Objects.requireNonNull(is));
        zis.getNextEntry();
        return zis;
    }

    private HyperbolicPlane readProjective(int order, int v, int batchSize, String name) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/proj" + order + "/" + name);
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            BitSet[] lines = new BitSet[v];
            for (int i = 0; i < v; i++) {
                BitSet bs = new BitSet();
                for (int j = 0; j < batchSize; j++) {
                    Arrays.stream(br.readLine().trim().split(" ")).mapToInt(Integer::parseInt).forEach(bs::set);
                }
                lines[i] = bs;
            }
            return new HyperbolicPlane(lines);
        }
    }

    @Test
    public void testProjectives1() throws IOException {
        int order = 25;
        File root = new File("/home/ihromant/workspace/math-utils/src/test/resources/proj" + order);
        for (File f : Objects.requireNonNull(root.listFiles())) {
            if ("s1.txt".equals(f.getName())) { // filter out desarg
                continue;
            }
            int v = order * order + order + 1;
            HyperbolicPlane p = readProjective(order, v, 2, f.getName());
            HyperbolicPlaneTest.testCorrectness(p, of(order + 1));
            System.out.println(f.getName());
            for (int i : p.lines()) {
                if (checkPappus(p, i)) {
                    System.out.println(i + " " + f.getName());
                }
            }
        }
    }

    private List<HyperbolicPlane> readPlanes(int v, int k) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/inc/S2-" + k + "-" + v + ".inc.zip");
             ZipInputStream zis = getZis(is);
             InputStreamReader isr = new InputStreamReader(zis);
             BufferedReader br = new BufferedReader(isr)) {
            String[] first = br.readLine().trim().split(" ");
            assertEquals(Integer.parseInt(first[0]), v);
            int b = Integer.parseInt(first[1]);
            assertEquals(b, v * (v - 1) / k / (k - 1));
            List<HyperbolicPlane> result = new ArrayList<>();
            while (true) {
                String next = br.readLine();
                if (next == null) {
                    zis.getNextEntry();
                    return result;
                }
                while (next.trim().isEmpty()) {
                    next = br.readLine();
                }
                result.add(readPlane(v, b, next, br));
            }
        }
    }

    private HyperbolicPlane readPlane(int v, int b, String next, BufferedReader br) throws IOException {
        BitSet[] lines = IntStream.range(0, b).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
        for (int i = 0; i < v; i++) {
            char[] chars = next.trim().toCharArray();
            for (int j = 0; j < b; j++) {
                if (chars[j] == '1') {
                    lines[j].set(i);
                }
            }
            next = br.readLine();
        }
        return new HyperbolicPlane(lines);
    }

    private static BitSet of(int... values) {
        BitSet bs = new BitSet();
        IntStream.of(values).forEach(bs::set);
        return bs;
    }

    @Test
    public void test217_7() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(217, 7);
        assertEquals(4, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7)));
        assertEquals(of(1, 2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(2).hyperbolicIndex());
        assertEquals(of(2, 3, 4, 5), planes.get(3).hyperbolicIndex());
    }

    @Test
    public void test175_7() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(175, 7);
        assertEquals(2, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7)));
        assertEquals(of(2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
    }

    @Test
    public void test66_6() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(66, 6);
        assertEquals(3, planes.size());
        planes.forEach(p -> assertEquals(of(0, 1, 2, 3, 4), p.hyperbolicIndex()));
    }

    @Test
    public void test65_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(65, 5);
        assertEquals(1777, planes.size());
        HyperbolicPlaneTest.testCorrectness(planes.get(0), of(5));
        assertEquals(of(3), planes.get(0).hyperbolicIndex());
        assertEquals(of(65), planes.get(0).cardSubPlanes(true));
    }

    @Test
    public void test41_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(41, 5);
        assertEquals(15, planes.size());
    }

    @Test
    public void test45_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(45, 5);
        assertEquals(30, planes.size());
    }

    @Test
    public void test37_4() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(37, 4);
        assertEquals(51402, planes.size());
    }

    @Test
    public void test28_4() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(28, 4);
        assertEquals(4466, planes.size());
        HyperbolicPlane plane = planes.get(1001);
        assertEquals(of(2), plane.hyperbolicIndex());
        assertEquals(of(28), plane.cardSubPlanes(true));
        HyperbolicPlaneTest.testCorrectness(plane, of(4));

        String[] design = printDesign(planes.get(3429));
        Arrays.stream(design).forEach(System.out::println);

        for (int i = 2600; i < planes.size(); i++) {
            HyperbolicPlane p1 = planes.get(i);
            Set<BitSet> subaffines = new HashSet<>();
            for (int t0 : p1.points()) {
                for (int t1 : p1.points()) {
                    if (t0 >= t1) {
                        continue;
                    }
                    for (int t3 : p1.points()) {
                        if (t1 >= t3 || p1.collinear(t0, t1, t3)) {
                            continue;
                        }
                        for (int t2 : p1.points(p1.line(t0, t1))) {
                            if (t2 == t0 || t2 == t1) {
                                continue;
                            }
                            for (int t6 : p1.points(p1.line(t0, t3))) {
                                if (t6 == t0 || t6 == t3) {
                                    continue;
                                }
                                for (int t4 : p1.points(p1.line(t2, t6))) {
                                    if (t2 == t4 || t6 == t4) {
                                        continue;
                                    }
                                    for (int t8 : p1.points(p1.line(t0, t4))) {
                                        if (t8 == t0 || t8 == t4 || !p1.collinear(t1, t3, t8)) {
                                            continue;
                                        }
                                        for (int t7 : p1.points(p1.line(t6, t8))) {
                                            if (t7 == t6 || t7 == t8 || !p1.collinear(t2, t3, t7) || !p1.collinear(t1, t4, t7)) {
                                                continue;
                                            }
                                            for (int t5 : p1.points(p1.line(t2, t8))) {
                                                if (t5 == t2 || t5 == t8 || !p1.collinear(t3, t4, t5) || !p1.collinear(t0, t5, t7) || !p1.collinear(t1, t5, t6)) {
                                                    continue;
                                                }
                                                BitSet aff = of(t0, t1, t2, t3, t4, t5, t6, t7, t8);
                                                if (subaffines.add(aff)) {
                                                    System.out.println(i + " " + aff);
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private String[] printDesign(HyperbolicPlane plane) {
        return IntStream.range(0, plane.line(0).cardinality()).mapToObj(i -> StreamSupport.stream(plane.lines().spliterator(), false)
                .map(plane::line).map(bs -> String.valueOf(Character.forDigit(bs.stream()
                        .skip(i).findAny().orElseThrow(), 36))).collect(Collectors.joining())).toArray(String[]::new);
    }

    @Test
    public void testFixed() throws IOException {
        String name = "semi4";
        try (InputStream is = getClass().getResourceAsStream("/proj/" + name + ".uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            String[] first = br.readLine().trim().split(" ");
            int v = Integer.parseInt(first[0]);
            int b = Integer.parseInt(first[1]);
            br.readLine();
            String next = br.readLine();
            HyperbolicPlane projective = readPlane(v, b, next, br);
            for (int dl : projective.lines()) {
                System.out.println(projective.line(dl) + " " + testThalesVectors(projective, dl));
            }
        }
    }

    private static int parallel(HyperbolicPlane pl, int dl, int line, int p) {
        return pl.line(p, pl.intersection(line, dl));
    }

    private static BitSet testThalesVectors(HyperbolicPlane pl, int droppedLine) {
        BitSet result = new BitSet();
        BitSet dl = pl.line(droppedLine);
        int base = IntStream.range(0, pl.pointCount()).filter(p -> !dl.get(p)).findFirst().orElseThrow();
        for (int end = base + 1; end < pl.pointCount(); end++) {
            if (!dl.get(end) && testThales(pl, droppedLine, base, end)) {
                result.set(end);
            }
        }
        return result;
    }

    private static boolean testThales(HyperbolicPlane pl, int droppedLine, int base, int end) {
        BitSet dl = pl.line(droppedLine);
        int infty = pl.intersection(droppedLine, pl.line(base, end));
        int bl = pl.line(base, infty);
        BitSet baseLine = pl.line(bl);
        for (int fst : pl.points()) {
            if (dl.get(fst) || baseLine.get(fst)) {
                continue;
            }
            BitSet fstLine = pl.line(pl.line(fst, infty));
            int fstEnd = pl.intersection(parallel(pl, droppedLine, bl, fst), parallel(pl, droppedLine, pl.line(base, fst), end));
            for (int snd : pl.points()) {
                if (dl.get(snd) || baseLine.get(snd) || fstLine.get(snd)) {
                    continue;
                }
                int sndEnd = pl.intersection(parallel(pl, droppedLine, bl, snd), parallel(pl, droppedLine, pl.line(base, snd), end));
                int fstEndAlt = pl.intersection(parallel(pl, droppedLine, pl.line(snd, sndEnd), fst), parallel(pl, droppedLine, pl.line(fst, snd), sndEnd));
                if (fstEndAlt != fstEnd) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void testProjectiveAndUnitals7() throws IOException {
        String name = "semi4";
        try (InputStream is = getClass().getResourceAsStream("/proj/" + name + ".uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            String[] first = br.readLine().trim().split(" ");
            int v = Integer.parseInt(first[0]);
            int b = Integer.parseInt(first[1]);
            br.readLine();
            String next = br.readLine();
            HyperbolicPlane projective = readPlane(v, b, next, br);
            HyperbolicPlaneTest.testCorrectness(projective, of(17));
            for (int i : projective.lines()) {
                //if (checkThales(projective, i)) {
                //    System.out.println("Thales " + i);
                    System.out.println("Pappus " + i + " " + checkPappus(projective, i));
                //}
            }
            int ub = Integer.parseInt(br.readLine());
            br.readLine();
            while ((next = br.readLine()) != null) {
                String[] numbers = next.split(" ");
                assertEquals(ub, numbers.length);
                BitSet points = Arrays.stream(numbers).mapToInt(Integer::parseInt).collect(BitSet::new, BitSet::set, BitSet::or);
                BitSet[] lines = StreamSupport.stream(projective.lines().spliterator(), false).map(l -> {
                    BitSet result = new BitSet();
                    result.or(projective.line(l));
                    result.and(points);
                    return result;
                }).filter(l -> l.cardinality() > 1).toArray(BitSet[]::new);
                int[] pointArray = points.stream().toArray();
                lines = Arrays.stream(lines).map(l -> l.stream()
                        .map(p -> Arrays.binarySearch(pointArray, p)).collect(BitSet::new, BitSet::set, BitSet::or)).toArray(BitSet[]::new);
                HyperbolicPlane p = new HyperbolicPlane(lines);
                HyperbolicPlaneTest.testCorrectness(p, of(5));
                System.out.println(p.hyperbolicIndex());
            }
        }
    }

    private boolean checkPappus(HyperbolicPlane p, int dl) {
        BitSet droppedLine = p.line(dl);
        for (int o : p.points()) {
            if (droppedLine.get(o)) {
                continue;
            }
            for (int l1 : p.lines(o)) {
                for (int l2 : p.lines(o)) {
                    if (l1 >= l2) {
                        continue;
                    }
                    for (int a1 : p.points(l1)) {
                        if (a1 == o || droppedLine.get(a1)) {
                            continue;
                        }
                        for (int b1 : p.points(l1)) {
                            if (b1 == o || a1 == b1 || droppedLine.get(b1)) {
                                continue;
                            }
                            for (int a2 : p.points(l2)) {
                                if (a2 == o || droppedLine.get(a2)) {
                                    continue;
                                }
                                for (int b2 : p.points(l2)) {
                                    if (b2 == o || a2 == b2 || droppedLine.get(b2)) {
                                        continue;
                                    }
                                    int a2b1 = p.line(a2, b1);
                                    int a2b1Infty = p.intersection(a2b1, dl);
                                    int b2c1 = p.line(b2, a2b1Infty);
                                    int c1 = p.intersection(b2c1, l1);

                                    int a1b2 = p.line(a1, b2);
                                    int a1b2Infty = p.intersection(a1b2, dl);
                                    int b1c2 = p.line(b1, a1b2Infty);
                                    int c2 = p.intersection(b1c2, l2);

                                    int a1a2 = p.line(a1, a2);
                                    int c1c2 = p.line(c1, c2);
                                    if (a1a2 != c1c2 && !droppedLine.get(p.intersection(a1a2, c1c2))) {
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

    @Test
    public void testAffinePappus() {
        HyperbolicPlane pl = new HyperbolicPlane(15, new int[][]{{0, 1, 4}, {0, 6, 8}, {0, 5, 10}});
        System.out.println(pl.cardSubPlanes(true));
        System.out.println(pl.hyperbolicIndex());
        for (int l1 = 0; l1 < pl.lineCount(); l1++) {
            for (int l2 = l1 + 1; l2 < pl.lineCount(); l2++) {
                if (pl.intersection(l1, l2) >= 0) {
                    continue;
                }
                int[] pts1 = pl.line(l1).stream().toArray();
                int[] pts2 = pl.line(l2).stream().toArray();
                GaloisField.permutations(pts1).forEach(prm1 -> GaloisField.permutations(pts2).forEach(prm2 -> {
                    if (pl.intersection(pl.line(prm1[0], prm2[1]), pl.line(prm1[2], prm2[1])) < 0
                            && pl.intersection(pl.line(prm2[0], prm1[1]), pl.line(prm2[2], prm1[1])) < 0
                            && pl.intersection(pl.line(prm1[0], prm2[0]), pl.line(prm1[2], prm2[2])) >= 0) {
                        System.out.println("Fail Pappus");
                    }
                }));
            }
        }
        System.out.println("Lines");
        for (int l : pl.lines()) {
            System.out.println(pl.line(l));
        }
        for (int l1 = 0; l1 < pl.lineCount(); l1++) {
            for (int l2 = l1 + 1; l2 < pl.lineCount(); l2++) {
                if (pl.intersection(l1, l2) >= 0) {
                    continue;
                }
                for (int l3 = l2 + 1; l3 < pl.lineCount(); l3++) {
                    if (pl.intersection(l1, l3) >= 0 || pl.intersection(l2, l3) >= 0) {
                        continue;
                    }
                    for (int a1 : pl.points(l1)) {
                        for (int b1 : pl.points(l1)) {
                            for (int a2 : pl.points(l2)) {
                                for (int b2 : pl.points(l2)) {
                                    for (int a3 : pl.points(l3)) {
                                        for (int b3 : pl.points(l3)) {
                                            if (pl.intersection(pl.line(a1, a2), pl.line(b1, b2)) < 0
                                                    && pl.intersection(pl.line(a3, a2), pl.line(b3, b2)) < 0
                                                    && pl.intersection(pl.line(a1, a3), pl.line(b1, b3)) >= 0) {
                                                System.out.println("Fail Par Desargues");
                                                System.out.println("l1 " + pl.line(l1));
                                                System.out.println("l2 " + pl.line(l2));
                                                System.out.println("l3 " + pl.line(l3));
                                                System.out.println("a1 " + a1);
                                                System.out.println("a2 " + a2);
                                                System.out.println("a3 " + a3);
                                                System.out.println("b1 " + b1);
                                                System.out.println("b2 " + b2);
                                                System.out.println("b3 " + b3);
                                                System.out.println("a1a2 " + pl.line(pl.line(a1, a2)));
                                                System.out.println("b1b2 " + pl.line(pl.line(b1, b2)));
                                                System.out.println("a2a3 " + pl.line(pl.line(a2, a3)));
                                                System.out.println("b2b3 " + pl.line(pl.line(b2, b3)));
                                                System.out.println("a1a3 " + pl.line(pl.line(a1, a3)));
                                                System.out.println("b1b3 " + pl.line(pl.line(b1, b3)));
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        for (int o : pl.points()) {
            for (int l1 : pl.lines(o)) {
                for (int l2 : pl.lines(o)) {
                    if (l2 <= l1) {
                        continue;
                    }
                    for (int l3 : pl.lines(o)) {
                        if (l3 <= l2) {
                            continue;
                        }
                        for (int a1 : pl.points(l1)) {
                            if (a1 == o) {
                                continue;
                            }
                            for (int b1 : pl.points(l1)) {
                                if (b1 == o) {
                                    continue;
                                }
                                for (int a2 : pl.points(l2)) {
                                    if (a2 == o) {
                                        continue;
                                    }
                                    for (int b2 : pl.points(l2)) {
                                        if (b2 == o) {
                                            continue;
                                        }
                                        for (int a3 : pl.points(l3)) {
                                            if (a3 == o) {
                                                continue;
                                            }
                                            for (int b3 : pl.points(l3)) {
                                                if (b3 == o) {
                                                    continue;
                                                }
                                                if (pl.intersection(pl.line(a1, a2), pl.line(b1, b2)) < 0
                                                        && pl.intersection(pl.line(a3, a2), pl.line(b3, b2)) < 0
                                                        && pl.intersection(pl.line(a1, a3), pl.line(b1, b3)) >= 0) {
                                                    System.out.println("Fail Conc Desargues");
                                                    System.out.println("l1 " + pl.line(l1));
                                                    System.out.println("l2 " + pl.line(l2));
                                                    System.out.println("l3 " + pl.line(l3));
                                                    System.out.println("a1 " + a1);
                                                    System.out.println("a2 " + a2);
                                                    System.out.println("a3 " + a3);
                                                    System.out.println("b1 " + b1);
                                                    System.out.println("b2 " + b2);
                                                    System.out.println("b3 " + b3);
                                                    System.out.println("a1a2 " + pl.line(pl.line(a1, a2)));
                                                    System.out.println("b1b2 " + pl.line(pl.line(b1, b2)));
                                                    System.out.println("a2a3 " + pl.line(pl.line(a2, a3)));
                                                    System.out.println("b2b3 " + pl.line(pl.line(b2, b3)));
                                                    System.out.println("a1a3 " + pl.line(pl.line(a1, a3)));
                                                    System.out.println("b1b3 " + pl.line(pl.line(b1, b3)));
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    public void findSuitable() {
        for (long k = 3; k < 300; k++) {
            long den = k * (k - 1);
            for (long kp = 2; kp < 1000000; kp++) {
                long beam = k + kp - 1;
                long p = 1 + (k - 1) * (1 + beam);
                if ((p * (p - 1)) % den != 0) {
                    continue;
                }
                long vp = p * (p - 1) / den;
                long gp = vp * (p - k);
                long s = 1 + (k - 1) * (1 + beam + beam * beam);
                if ((s * (s - 1)) % den != 0) {
                    continue;
                }
                long vs = s * (s - 1) / den;
                long gs = vs * (s - k);
                if (gs % gp != 0) {
                    continue;
                }
                long planes = gs / gp;
                long[] factors = Group.factorize(beam);
                if (Arrays.stream(factors).anyMatch(f -> f != factors[0]) && bruckRyser(beam)) {
                    continue;
                }
                long hs = 1 + (k - 1) * (1 + beam + beam * beam + beam * beam * beam);
                boolean fits = (hs * (hs - 1)) % den == 0;
                long vhs = (hs * (hs - 1)) / den;
                long ghs = vhs * (hs - k);
                fits = fits && ghs % gp == 0;
                long fourDimPlanesCount = ghs / gp;
                long fourDimPlanePointPairs = fourDimPlanesCount * (hs - p);
                long triDimPlanePointPairs = planes * (s - p);
                fits = fits && fourDimPlanePointPairs % triDimPlanePointPairs == 0;
                System.out.println("k: " + k + ", kappa: " + kp + ", p: " + p + ", vp: " + vp + ", s: " + s + ", vs: " + vs + ", planes: " + planes + ", 4-dim: " + fits);
            }
        }
    }

    private static boolean bruckRyser(long val) {
        if (val % 4 == 0 || val % 4 == 3) {
            return false;
        }
        long sqrt = (long) Math.ceil(Math.sqrt(val));
        for (long i = 0; i <= sqrt; i++) {
            for (long j = 0; j <= sqrt; j++) {
                if (val == i * i + j * j) {
                    return false;
                }
            }
        }
        return true;
    }

    @Test
    public void findSuitableNotWeaklyRegular() {
        for (int k = 3; k < 10; k++) {
            int den = k * (k - 1);
            for (int vp = den + 1; vp < 300; vp++) {
                if ((vp - 1) % (k - 1) != 0 || (vp * (vp - 1)) % den != 0) {
                    continue;
                }
                int bp = vp * (vp - 1) / den;
                int rp = (vp - 1) / (k - 1);
                int gp = bp * (vp - k);
                for (int vs = vp + 1; vs < 1000; vs++) {
                    if ((vs - 1) % (k - 1) != 0 || (vs * (vs - 1)) % den != 0) {
                        continue;
                    }
                    int bs = vs * (vs - 1) / den;
                    int rs = (vs - 1) / (k - 1);
                    if ((rs - 1) % (rp - 1) != 0 || (rs * (rs - 1)) % (rp * (rp - 1)) != 0) {
                        continue;
                    }
                    int gs = bs * (vs - k);
                    if (gs % gp != 0) {
                        continue;
                    }
                    System.out.println(k + " " + vp + " " + vs);
                }
            }
        }
    }

    @Test
    public void readSts15() throws IOException {
        String[][] designs = new String[80][3];
        try (InputStream is = getClass().getResourceAsStream("/S(2,3,15).txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                designs[counter][0] = line;
                designs[counter][1] = br.readLine();
                designs[counter][2] = br.readLine();
                counter++;
            }
        }
        ex: for (int i = 0; i < 80; i++) {
            HyperbolicPlane hp = new HyperbolicPlane(designs[i]);
            for (int l1 = 0; l1 < hp.lineCount(); l1++) {
                for (int l2 = l1 + 1; l2 < hp.lineCount(); l2++) {
                    if (hp.intersection(l1, l2) >= 0) {
                        continue;
                    }
                    for (int l3 = l2 + 1; l3 < hp.lineCount(); l3++) {
                        if (hp.intersection(l1, l3) >= 0 || hp.intersection(l2, l3) >= 0) {
                            continue;
                        }
                        for (int l4 = l3 + 1; l4 < hp.lineCount(); l4++) {
                            if (hp.intersection(l1, l4) >= 0 || hp.intersection(l2, l4) >= 0 || hp.intersection(l3, l4) >= 0) {
                                continue;
                            }
                            System.out.println("Has " + i);
                            continue ex;
                        }
                    }
                }
                System.out.println("Has not " + i);
            }
        }
    }
}
