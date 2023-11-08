package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

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

import static org.junit.jupiter.api.Assertions.*;

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
            HyperbolicPlaneTest.testCorrectness(p, of(order + 1), order + 1);
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
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7), 36));
        assertEquals(of(1, 2, 3, 4, 5), planes.get(0).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(1).hyperbolicIndex());
        assertEquals(of(1, 2, 3, 4, 5), planes.get(2).hyperbolicIndex());
        assertEquals(of(2, 3, 4, 5), planes.get(3).hyperbolicIndex());
    }

    @Test
    public void test175_7() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(175, 7);
        assertEquals(2, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7), 29));
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
        HyperbolicPlaneTest.testCorrectness(planes.get(0), of(5), 16);
        assertEquals(of(3), planes.get(0).hyperbolicIndex());
        HyperbolicPlaneTest.checkPlane(planes.get(0));
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
        HyperbolicPlaneTest.checkPlane(plane);
        HyperbolicPlaneTest.testCorrectness(plane, of(4), 9);

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
            HyperbolicPlaneTest.testCorrectness(projective, of(17), 17);
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
                HyperbolicPlaneTest.testCorrectness(p, of(5), 16);
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

    private boolean checkThales(HyperbolicPlane p, int dl) {
        BitSet droppedLine = p.line(dl);
        System.out.println(dl);
        for (int infty : p.points(dl)) {
            for (int la : p.lines(infty)) {
                if (la == dl) {
                    continue;
                }
                for (int lb : p.lines(infty)) {
                    if (lb == dl || la >= lb) {
                        continue;
                    }
                    for (int lc : p.lines(infty)) {
                        if (lc == dl || lb >= lc) {
                            continue;
                        }
                        for (int a1 : p.points(la)) {
                            if (a1 == infty) {
                                continue;
                            }
                            for (int a2 : p.points(la)) {
                                if (a1 == a2 || a2 == infty) {
                                    continue;
                                }
                                for (int b1 : p.points(lb)) {
                                    if (b1 == infty) {
                                        continue;
                                    }
                                    for (int b2 : p.points(lb)) {
                                        if (b1 == b2 || b2 == infty) {
                                            continue;
                                        }
                                        for (int c1 : p.points(lc)) {
                                            if (c1 == infty) {
                                                continue;
                                            }
                                            for (int c2 : p.points(lc)) {
                                                if (c1 == c2 || c2 == infty) {
                                                    continue;
                                                }
                                                if (droppedLine.get(p.intersection(p.line(a1, c1), p.line(a2, c2)))
                                                        && droppedLine.get(p.intersection(p.line(a1, b1), p.line(a2, b2)))
                                                        && !droppedLine.get(p.intersection(p.line(b1, c1), p.line(b2, c2)))) {
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
}
