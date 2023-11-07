package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
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
        HyperbolicPlaneTest.checkPlane(planes.get(0), 65, 65);
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
        HyperbolicPlaneTest.checkPlane(plane, 28, 28);
        HyperbolicPlaneTest.testCorrectness(plane, of(4), 9);
    }

    @Test
    public void testProjectiveAndUnitals() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/proj/pg216.uni");
             InputStreamReader isr = new InputStreamReader(Objects.requireNonNull(is));
             BufferedReader br = new BufferedReader(isr)) {
            String[] first = br.readLine().trim().split(" ");
            int v = Integer.parseInt(first[0]);
            int b = Integer.parseInt(first[1]);
            br.readLine();
            String next = br.readLine();
            HyperbolicPlane projective = readPlane(v, b, next, br);
            HyperbolicPlaneTest.testCorrectness(projective, of(17), 17);
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
}
