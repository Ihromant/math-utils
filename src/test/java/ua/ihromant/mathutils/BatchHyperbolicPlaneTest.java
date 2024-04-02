package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.group.CyclicGroup;
import ua.ihromant.mathutils.group.Group;

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
import java.util.BitSet;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
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
            HyperbolicPlane projective = readUni(br);
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
            HyperbolicPlane projective = readUni(br);
            String next;
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

    public HyperbolicPlane readTxt(BufferedReader br) throws IOException {
        List<BitSet> list = new ArrayList<>();
        String line = br.readLine();
        while (line != null) {
            BitSet bs = new BitSet();
            do {
                Arrays.stream(line.trim().split(" ")).mapToInt(Integer::parseInt).forEach(bs::set);
                line = br.readLine();
            } while (line != null && line.charAt(0) == ' ');
            list.add(bs);
        }
        return new HyperbolicPlane(list.toArray(BitSet[]::new));
    }

    @Test
    public void testAffinePappus() throws IOException {
        String name = "fig";
        int k = 64;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            HyperbolicPlane projective = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(projective, of(k + 1));
            for (int dl : projective.lines()) {
                boolean paraPappus = checkParaPappus(projective, dl);
                System.out.println("Dropped " + dl + " ParaPappus " + paraPappus);
                if (paraPappus) {
                    boolean paraDesargues = checkParaDesargues(projective, dl);
                    System.out.println("ParaDesargues " + paraDesargues);
                }
            }
        }
    }

    @Test
    public void testCubeDesargues10() throws IOException {
        String name = "semi4";
        int k = 16;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            HyperbolicPlane projective = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(projective, of(k + 1));
            for (int dl : projective.lines()) {
                System.out.println("Dropped " + dl);
                System.out.println("ParaDesargues " + checkParaDesargues(projective, dl));
                System.out.println("Cube " + checkCubeDesargues(projective, dl));
            }
        }
    }

    @Test
    public void testZigZag() throws IOException {
        String name = "fig";
        int k = 64;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            HyperbolicPlane projective = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(projective, of(k + 1));
            Set<Set<Pair>> configs = new HashSet<>();
            for (int dl : projective.lines()) {
                Set<Pair> pairs = new HashSet<>();
                BitSet droppedLine = projective.line(dl);
                boolean pappus = checkParaPappus(projective, dl);
                boolean diagonal = isDiagonal(projective, dl);
                if (!diagonal) {
                    if (pappus) {
                        System.out.println(pappus);
                    }
                    continue;
                }
                for (int o : projective.points()) {
                    if (droppedLine.get(o)) {
                        continue;
                    }
                    for (int x : projective.points()) {
                        if (droppedLine.get(x) || x == o) {
                            continue;
                        }
                        for (int y : projective.points()) {
                            if (droppedLine.get(y) || y == o || y == x || projective.line(y, o) == projective.line(y, x)) {
                                continue;
                            }
                            Pair p = new Pair(zigZagNumber(projective, dl, o, x, y), zigZagNumber(projective, dl, o, y, x));
                            pairs.add(p);
                        }
                    }
                }
                if (configs.add(pairs)) {
                    System.out.println("Dropped " + dl + " diagonal " + diagonal + " pappus " + pappus + " config " + pairs);
                }
            }
        }
    }

    @Test
    public void testCharacteristics() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            HyperbolicPlane proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : proj.lines()) {
                BitSet droppedLine = proj.line(dl);
                for (int o : proj.points()) {
                    if (droppedLine.get(o)) {
                        continue;
                    }
                    Set<Pair> pairs = new HashSet<>();
                    for (int x : proj.points()) {
                        if (droppedLine.get(x) || o == x) {
                            continue;
                        }
                        for (int y : proj.points()) {
                            if (droppedLine.get(y) || o == y || y == x || proj.line(o, y) == proj.line(o, x)) {
                                continue;
                            }
                            Pair p = new Pair(zigZagNumber(proj, dl, o, x, y), zigZagNumber(proj, dl, o, y, x));
                            pairs.add(p);
                        }
                    }
                    System.out.println("Dropped " + dl + " point " + o + " char " + pairs);
                }
            }
        }
    }

    @Test
    public void testClosure() throws IOException {
        String name = "hughes9";
        int k = 9;
        try (InputStream is = getClass().getResourceAsStream("/proj" + k + "/" + name + ".txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            System.out.println(name);
            HyperbolicPlane proj = readTxt(br);
            HyperbolicPlaneTest.testCorrectness(proj, of(k + 1));
            for (int dl : proj.lines()) {
                BitSet droppedLine = proj.line(dl);
                Set<Integer> closures = new HashSet<>();
                for (int a = 0; a < proj.pointCount(); a++) {
                    if (droppedLine.get(a)) {
                        continue;
                    }
                    for (int b = a + 1; b < proj.pointCount(); b++) {
                        if (droppedLine.get(b)) {
                            continue;
                        }
                        for (int c = b + 1; c < proj.pointCount(); c++) {
                            if (droppedLine.get(c) || proj.line(a, c) == proj.line(b, c)) {
                                continue;
                            }
                            BitSet base = of(a, b, c);
                            BitSet closure = closure(proj, dl, base);
                            if (closures.add(closure.cardinality())) {
                                System.out.println(closure.cardinality());
                            }
                        }
                    }
                }
                System.out.println("Dropped " + dl + " closures " + closures);
            }
        }
    }

    private static BitSet closure(HyperbolicPlane plane, int dl, BitSet base) {
        BitSet additional;
        while ((additional = additional(plane, dl, base)).cardinality() > base.cardinality()) {
            base = additional;
        }
        return base;
    }

    public static BitSet additional(HyperbolicPlane plane, int dl, BitSet base) {
        BitSet result = new BitSet();
        BitSet droppedLine = plane.line(dl);
        base.stream().filter(a -> !droppedLine.get(a)).forEach(a -> base.stream().filter(b -> !droppedLine.get(b) && b != a).forEach(b -> base.stream().forEach(c -> {
            if (droppedLine.get(c) || c == b || c == a || plane.line(a, c) == plane.line(b, c)) {
                return;
            }
            result.set(closure(plane, dl, a, b, c));
            result.set(closure(plane, dl, b, c, a));
            result.set(closure(plane, dl, c, a, b));
        })));
        result.or(base);
        return result;
    }

    private record Pair(int a, int b) {
        private Pair(int a, int b) {
            this.a = Math.min(a, b);
            this.b = Math.max(a, b);
        }
    }

    private boolean isDiagonal(HyperbolicPlane plane, int dl) {
        BitSet droppedLine = plane.line(dl);
        for (int x00 : plane.points()) {
            if (droppedLine.get(x00)) {
                continue;
            }
            for (int x01 : plane.points()) {
                if (droppedLine.get(x01) || x01 == x00) {
                    continue;
                }
                for (int x10 : plane.points()) {
                    if (droppedLine.get(x10) || x10 == x00 || x10 == x01 || plane.line(x10, x00) == plane.line(x10, x01)) {
                        continue;
                    }
                    int x11 = plane.intersection(parallel(plane, dl, plane.line(x00, x01), x10), parallel(plane, dl, plane.line(x00, x10), x01));
                    int x12 = plane.intersection(parallel(plane, dl, plane.line(x00, x11), x01), plane.line(x10, x11));
                    int x21 = plane.intersection(parallel(plane, dl, plane.line(x00, x11), x10), plane.line(x01, x11));
                    int x22 = plane.intersection(parallel(plane, dl, plane.line(x00, x01), x21), parallel(plane, dl, plane.line(x00, x10), x12));
                    if (!plane.line(plane.line(x00, x11)).get(x22)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private int zigZagNumber(HyperbolicPlane plane, int dl, int o, int x, int y) {
        int counter = 0;
        int base = o;
        while (x != base) {
            counter++;
            int ox = plane.line(o, x);
            int yPar = parallel(plane, dl, ox, y);
            y = plane.intersection(yPar, parallel(plane, dl, plane.line(o, y), x));
            int y1 = plane.intersection(parallel(plane, dl, plane.line(o, y), x), yPar);
            o = x;
            x = plane.intersection(parallel(plane, dl, plane.line(o, y), y1), ox);
        }
        return counter + 1;
    }

    private static int closure(HyperbolicPlane plane, int dl, int o, int x, int y) {
        return plane.intersection(parallel(plane, dl, plane.line(o, x), y), parallel(plane, dl, plane.line(o, y), x));
    }

    @Test
    public void filterIsomorphic() throws IOException {
        int v = 121;
        int k = 6;
        try (InputStream fis = new FileInputStream(new File("/home/ihromant/maths/diffSets/", "list-" + v + "-" + k + ".txt"));
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(fis));
             BufferedReader br = new BufferedReader(isr);
             FileOutputStream fos = new FileOutputStream(new File("/home/ihromant/maths/diffSets/unique", k + "-" + v + "?.txt"));
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             PrintStream ps = new PrintStream(bos)) {
            String line = br.readLine();
            CyclicGroup gr = new CyclicGroup(v);
            int[][] auths = gr.auth();
            Set<Set<BitSet>> unique = new HashSet<>();
            long time = System.currentTimeMillis();
            AtomicLong counter = new AtomicLong();
            ps.println(v + " " + k);
            while ((line = br.readLine()) != null) {
                if (line.length() < 20) {
                    continue;
                }
                String cut = line.replace("}}", "");
                String[] arrays = Arrays.stream(cut.split("\\}, \\{")).map(s -> s.substring(s.indexOf("={") + 2)).toArray(String[]::new);
                int[][] diffSet = Stream.concat(Arrays.stream(arrays).map(s -> Arrays.stream(s.split(", ")).mapToInt(Integer::parseInt)
                        .toArray()), v % k == 0 ? Stream.of(IntStream.range(0, k).map(i -> i * v / k).toArray()) : Stream.empty()).toArray(int[][]::new);
                IntStream.range(0, 1 << (diffSet.length - (v % k == 0 ? 2 : 1))).forEach(comb -> {
                    int[][] diffs = IntStream.range(0, diffSet.length)
                            .mapToObj(i -> ((1 << i) & comb) == 0 ? diffSet[i].clone() : mirrorTuple(gr, diffSet[i]))
                            .map(arr -> minimalTuple(arr, v)).toArray(int[][]::new);
                    if (Arrays.stream(auths).noneMatch(auth -> {
                        Set<BitSet> result = new HashSet<>();
                        for (int[] arr : diffs) {
                            result.add(of(minimalTuple(applyAuth(arr, auth), v)));
                        }
                        return unique.contains(result);
                    })) {
                        unique.add(Arrays.stream(diffs).map(BatchHyperbolicPlaneTest::of).collect(Collectors.toSet()));
                        ps.println(Arrays.stream(diffs).map(arr -> of(arr).toString())
                                .collect(Collectors.joining(", ", "{", "}")));
                        counter.incrementAndGet();
                    }
                });
            }
            System.out.println(counter.get() + " " + (System.currentTimeMillis() - time));
        }
    }

    private static int[] mirrorTuple(Group g, int[] tuple) {
        return IntStream.concat(IntStream.of(tuple[0], tuple[1]), IntStream.range(2, tuple.length).map(i -> g.op(tuple[1], g.inv(tuple[i])))).toArray();
    }

    private static int[] minimalTuple(int[] arr, int v) {
        Arrays.sort(arr);
        int l = arr.length;
        int[] diffs = new int[l];
        for (int i = 0; i < l; i++) {
            diffs[i] = diff(arr[i], arr[(l + i + 1) % l], v);
        }
        int minIdx = IntStream.range(0, l).boxed().max(Comparator.comparing(i -> diffs[i])).orElseThrow();
        int val = arr[minIdx];
        int[] res = Arrays.stream(arr).map(i -> i >= val ? i - val : v + i - val).toArray();
        Arrays.sort(res);
        return res;
    }

    private static int diff(int a, int b, int size) {
        int d = Math.abs(a - b);
        return Math.min(d, size - d);
    }

    private static int[] applyAuth(int[] arr, int[] auth) {
        return Arrays.stream(arr).map(i -> auth[i]).toArray();
    }

    private HyperbolicPlane readUni(BufferedReader br) throws IOException {
        String[] first = br.readLine().trim().split(" ");
        int v = Integer.parseInt(first[0]);
        int b = Integer.parseInt(first[1]);
        br.readLine();
        String next = br.readLine();
        return readPlane(v, b, next, br);
    }

    private static boolean checkBooleanPlane(HyperbolicPlane proj, int dl) {
        BitSet droppedLine = proj.line(dl);
        for (int p1 : proj.points()) {
            if (droppedLine.get(p1)) {
                continue;
            }
            for (int p2 : proj.points()) {
                if (droppedLine.get(p2) || p1 == p2) {
                    continue;
                }
                int p1p2 = proj.line(p1, p2);
                for (int p3 : proj.points()) {
                    int p1p3 = proj.line(p1, p3);
                    if (droppedLine.get(p3) || p1 == p3 || p3 == p2 || p1p3 == proj.line(p2, p3)) {
                        continue;
                    }
                    int p4 = proj.intersection(proj.line(p3, proj.intersection(p1p2, dl)), proj.line(p2, proj.intersection(p1p3, dl)));
                    if (!droppedLine.get(proj.intersection(proj.line(p1, p4), proj.line(p2, p3)))) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static boolean checkCubeDesargues(HyperbolicPlane proj, int dl) {
        BitSet droppedLine = proj.line(dl);
        for (int infl : proj.points(dl)) {
            for (int infp : proj.points(dl)) {
                if (infl == infp) {
                    continue;
                }
                for (int l1 : proj.lines(infl)) {
                    if (l1 == dl) {
                        continue;
                    }
                    for (int l2 : proj.lines(infl)) {
                        if (l2 == dl || l1 == l2) {
                            continue;
                        }
                        for (int l3 : proj.lines(infl)) {
                            if (l3 == dl || l3 == l2 || l3 == l1) {
                                continue;
                            }
                            for (int l4 : proj.lines(infl)) {
                                if (l4 == dl ||l4 == l3 || l4 == l2 || l4 == l1) {
                                    continue;
                                }
                                for (int p1 : proj.lines(infp)) {
                                    if (p1 == dl) {
                                        continue;
                                    }
                                    for (int p2 : proj.lines(infp)) {
                                        if (p2 == dl || p1 == p2) {
                                            continue;
                                        }
                                        for (int p3 : proj.lines(infp)) {
                                            if (p3 == dl || p3 == p2 || p3 == p1) {
                                                continue;
                                            }
                                            for (int p4 : proj.lines(infp)) {
                                                if (p4 == dl || p4 == p3 || p4 == p2 || p4 == p1) {
                                                    continue;
                                                }
                                                int a = proj.line(proj.intersection(p1, l3), proj.intersection(p2, l4));
                                                int b = proj.line(proj.intersection(p1, l1), proj.intersection(p2, l2));
                                                int c = proj.line(proj.intersection(l1, p3), proj.intersection(l2, p4));
                                                int d = proj.line(proj.intersection(l3, p3), proj.intersection(l4, p4));
                                                if (a == b || a == c || a == d || b == c || b == d || c == d) {
                                                    continue;
                                                }
                                                if (droppedLine.get(proj.intersection(a, b)) && droppedLine.get(proj.intersection(a, c))
                                                    && !droppedLine.get(proj.intersection(a, d))) {
                                                    return false;
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
        return true;
    }

    private static boolean checkParaDesargues(HyperbolicPlane proj, int dl) {
        BitSet droppedLine = proj.line(dl);
        for (int l1 : proj.lines()) {
            if (l1 == dl) {
                continue;
            }
            int pInf = proj.intersection(l1, dl);
            for (int l2 : proj.lines(pInf)) {
                if (l2 == dl || l2 <= l1) {
                    continue;
                }
                for (int l3 : proj.lines(pInf)) {
                    if (l3 == dl || l3 <= l2) {
                        continue;
                    }
                    for (int a1 : proj.points(l1)) {
                        if (a1 == pInf) {
                            continue;
                        }
                        for (int a2 : proj.points(l2)) {
                            if (a2 == pInf) {
                                continue;
                            }
                            for (int a3 : proj.points(l3)) {
                                if (a3 == pInf) {
                                    continue;
                                }
                                for (int b2 : proj.points(l2)) {
                                    if (b2 == pInf || b2 == a2) {
                                        continue;
                                    }
                                    int b1 = proj.intersection(l1, proj.line(proj.intersection(proj.line(a1, a2), dl), b2));
                                    int b3 = proj.intersection(l3, proj.line(proj.intersection(proj.line(a3, a2), dl), b2));
                                    if (!droppedLine.get(proj.intersection(proj.line(a1, a3), proj.line(b1, b3)))) {
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

    private static boolean checkParaPappus(HyperbolicPlane proj, int dl) {
        BitSet droppedLine = proj.line(dl);
        for (int l1 : proj.lines()) {
            if (l1 == dl) {
                continue;
            }
            int pInf = proj.intersection(l1, dl);
            for (int l2 : proj.lines(pInf)) {
                if (l2 == dl || l2 <= l1) {
                    continue;
                }
                for (int a1 : proj.points(l1)) {
                    if (a1 == pInf) {
                        continue;
                    }
                    for (int a2 : proj.points(l1)) {
                        if (a2 == pInf || a2 == a1) {
                            continue;
                        }
                        for (int a3 : proj.points(l1)) {
                            if (a3 == pInf || a3 == a2 || a3 == a1) {
                                continue;
                            }
                            for (int b2 : proj.points(l2)) {
                                if (b2 == pInf) {
                                    continue;
                                }
                                int b1 = proj.intersection(l2, proj.line(proj.intersection(proj.line(a3, b2), dl), a2));
                                int b3 = proj.intersection(l2, proj.line(proj.intersection(proj.line(a1, b2), dl), a2));
                                if (!droppedLine.get(proj.intersection(proj.line(a1, b1), proj.line(a3, b3)))) {
                                    return false;
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
    public void findSuitable() {
        for (long k = 3; k < 300; k++) {
            long den = k * (k - 1);
            for (long kp = 2; kp < 1000000; kp++) {
                long order = k + kp - 1;
                long v2 = 1 + (k - 1) * (1 + order);
                if ((v2 * (v2 - 1)) % den != 0) {
                    continue;
                }
                long b2 = v2 * (v2 - 1) / den;
                long af2 = b2 * (v2 - k);
                long v3 = 1 + (k - 1) * (1 + order + order * order);
                if ((v3 * (v3 - 1)) % den != 0) {
                    continue;
                }
                long b3 = v3 * (v3 - 1) / den;
                long af3 = b3 * (v3 - k);
                if (af3 % af2 != 0) {
                    continue;
                }
                long planes = af3 / af2;
                long[] factors = Group.factorize(order);
                if (Arrays.stream(factors).anyMatch(f -> f != factors[0]) && bruckRyser(order)) {
                    continue;
                }
                long v4 = 1 + (k - 1) * (1 + order + order * order + order * order * order);
                boolean fits = (v4 * (v4 - 1)) % den == 0;
                long b4 = (v4 * (v4 - 1)) / den;
                long af4 = b4 * (v4 - k);
                fits = fits && af4 % af2 == 0;
                long fourDimPlanesCount = af4 / af2;
                long fourDimPlanePointPairs = fourDimPlanesCount * (v4 - v2);
                long triDimPlanePointPairs = planes * (v3 - v2);
                fits = fits && fourDimPlanePointPairs % triDimPlanePointPairs == 0;
                System.out.println("k: " + k + ", kappa: " + kp + ", v2: " + v2 + ", b2: " + b2 + ", v3: " + v3 + ", b3: " + b3 + ", planes: " + planes + ", 4-dim: " + fits);
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
            for (int v2 = den + 1; v2 < 300; v2++) {
                if ((v2 - 1) % (k - 1) != 0 || (v2 * (v2 - 1)) % den != 0) {
                    continue;
                }
                int b2 = v2 * (v2 - 1) / den;
                int r2 = (v2 - 1) / (k - 1);
                int af2 = b2 * (v2 - k);
                for (int v3 = v2 + 1; v3 < 1000; v3++) {
                    if ((v3 - 1) % (k - 1) != 0 || (v3 * (v3 - 1)) % den != 0) {
                        continue;
                    }
                    int b3 = v3 * (v3 - 1) / den;
                    int r3 = (v3 - 1) / (k - 1);
                    if ((r3 - 1) % (r2 - 1) != 0 || (r3 * (r3 - 1)) % (r2 * (r2 - 1)) != 0) {
                        continue;
                    }
                    int af3 = b3 * (v3 - k);
                    if (af3 % af2 != 0) {
                        continue;
                    }
                    System.out.println(k + " " + v2 + " " + v3);
                }
            }
        }
    }

    @Test
    public void test25_4() throws IOException {
        String[][] designs = new String[18][4];
        try (InputStream is = getClass().getResourceAsStream("/S(2,4,25).txt");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String line;
            int counter = 0;
            while ((line = br.readLine()) != null) {
                designs[counter][0] = line;
                designs[counter][1] = br.readLine();
                designs[counter][2] = br.readLine();
                designs[counter][3] = br.readLine();
                counter++;
            }
        }
        for (int i = 0; i < designs.length; i++) {
            HyperbolicPlane p = new HyperbolicPlane(designs[i]);
            assertEquals(25, p.pointCount());
            assertEquals(50, p.lineCount());
            HyperbolicPlaneTest.testCorrectness(p, of(4));
            assertEquals(of(4), p.playfairIndex());
            assertEquals(i == 0 ? of(1, 2) : of(0, 1, 2), p.hyperbolicIndex()); // first is hyperaffine
            assertEquals(of(25), p.cardSubPlanes(true));
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
        HyperbolicPlane p = new HyperbolicPlane("00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95acbbacc9bbac9");
        System.out.println(checkDesargues(p));
        p = new HyperbolicPlane("00000011111222223334445556",
                "13579b3469a3467867868a7897",
                "2468ac578bc95abcbcac9babc9");
        System.out.println(checkDesargues(p));
        for (int i = 0; i < 80; i++) {
            HyperbolicPlane hp = new HyperbolicPlane(designs[i]);
            boolean checkDesargues = checkDesargues(hp);
            if (!checkDesargues) {
                System.out.print((i + 1) + " ");
            }
        }
        System.out.println();
        HyperbolicPlane tp = TaoPoint.toPlane();
        System.out.println("Tao" + parallelCheck(tp));
    }

    private static boolean parallelCheck(HyperbolicPlane pl) {
        for (int a = 0; a < pl.lineCount(); a++) {
            for (int b = a + 1; b < pl.lineCount(); b++) {
                if (pl.intersection(a, b) >= 0 || !isParallel(pl, a, b)) {
                    continue;
                }
                for (int a1 : pl.points(a)) {
                    for (int a2 : pl.points(a)) {
                        if (a1 == a2) {
                            continue;
                        }
                        for (int b1 : pl.points(b)) {
                            for (int b2 : pl.points(b)) {
                                int l1 = pl.line(a1, b1);
                                int l2 = pl.line(a2, b2);
                                if (b1 == b2 || pl.intersection(l1, l2) >= 0) {
                                    continue;
                                }
                                if (!isParallel(pl, l1, l2)) {
                                    System.out.println(TaoPoint.toString(a1) + " " + TaoPoint.toString(a2) + " "
                                            + pl.line(pl.line(a1, a2)).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",")) + " "
                                    + TaoPoint.toString(b1) + " " + TaoPoint.toString(b2) + " "
                                            + pl.line(pl.line(b1, b2)).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",")) + " "
                                    + pl.line(pl.line(a1, b1)).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",")) + " "
                                            + pl.line(pl.line(a2, b2)).stream().mapToObj(TaoPoint::toString).collect(Collectors.joining(",")));
                                    return true;
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    private static boolean isParallel(HyperbolicPlane pl, int a, int b) {
        BitSet l1 = pl.line(a);
        BitSet l2 = pl.line(b);
        boolean l1Subl2 = l1.stream().allMatch(p1 -> {
            BitSet un = (BitSet) l2.clone();
            un.set(p1);
            BitSet hull = pl.hull(un.stream().toArray());
            return l1.stream().allMatch(hull::get);
        });
        boolean l2Subl1 = l2.stream().allMatch(p2 -> {
            BitSet un = (BitSet) l1.clone();
            un.set(p2);
            BitSet hull = pl.hull(un.stream().toArray());
            return l2.stream().allMatch(hull::get);
        });
        return l2Subl1 && l1Subl2;
    }

    public static boolean checkDesargues(HyperbolicPlane pl) {
        for (int o : pl.points()) {
            for (int la : pl.lines(o)) {
                for (int lb : pl.lines(o)) {
                    if (la == lb) {
                        continue;
                    }
                    for (int lc : pl.lines(o)) {
                        if (la == lc || lb == lc) {
                            continue;
                        }
                        for (int a : pl.points(la)) {
                            if (a == o) {
                                continue;
                            }
                            for (int b : pl.points(lb)) {
                                if (b == o) {
                                    continue;
                                }
                                for (int c : pl.points(lc)) {
                                    if (c == o) {
                                        continue;
                                    }
                                    int a1 = quasiOp(pl, a, o);
                                    int b1 = quasiOp(pl, b, o);
                                    int c1 = quasiOp(pl, c, o);
                                    int c2 = pl.intersection(pl.line(a1, b1), pl.line(a, b));
                                    int a2 = pl.intersection(pl.line(b1, c1), pl.line(b, c));
                                    int b2 = pl.intersection(pl.line(a1, c1), pl.line(a, c));
                                    if (a2 >= 0 && b2 >= 0 && c2 >= 0 && a2 != b2 && a2 != c2 && b2 != c2 && quasiOp(pl, a2, b2) != c2) {
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

    private static int quasiOp(HyperbolicPlane pl, int x, int y) {
        return pl.line(pl.line(x, y)).stream().filter(p -> p != x && p != y).findAny().orElseThrow();
    }
}
