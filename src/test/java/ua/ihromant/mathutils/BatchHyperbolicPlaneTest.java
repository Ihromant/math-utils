package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
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
             BufferedReader bf = new BufferedReader(isr)) {
            String[] first = bf.readLine().trim().split(" ");
            assertEquals(Integer.parseInt(first[0]), v);
            int b = Integer.parseInt(first[1]);
            assertEquals(b, v * (v - 1) / k / (k - 1));
            List<HyperbolicPlane> result = new ArrayList<>();
            while (true) {
                String next = bf.readLine();
                if (next == null) {
                    zis.getNextEntry();
                    return result;
                }
                while (next.trim().isEmpty()) {
                    next = bf.readLine();
                }
                BitSet[] lines = IntStream.range(0, b).mapToObj(i -> new BitSet()).toArray(BitSet[]::new);
                for (int i = 0; i < v; i++) {
                    char[] chars = next.trim().toCharArray();
                    for (int j = 0; j < b; j++) {
                        if (chars[j] == '1') {
                            lines[j].set(i);
                        }
                    }
                    next = bf.readLine();
                }
                result.add(new HyperbolicPlane(lines));
            }
        }
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
//        HyperbolicPlaneTest.testHyperbolicIndex(planes.get(0), 1, 5);
//        HyperbolicPlaneTest.testHyperbolicIndex(planes.get(1), 1, 5);
//        HyperbolicPlaneTest.testHyperbolicIndex(planes.get(2), 1, 5);
        assertArrayEquals(new int[]{2, 5}, planes.get(3).hyperbolicIndex());
    }

    @Test
    public void test175_7() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(175, 7);
        assertEquals(2, planes.size());
        planes.forEach(p -> HyperbolicPlaneTest.testCorrectness(p, of(7), 29));
        assertArrayEquals(new int[]{2, 5}, planes.get(0).hyperbolicIndex());
        assertArrayEquals(new int[]{1, 5}, planes.get(1).hyperbolicIndex());
    }

    @Test
    public void test66_6() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(66, 6);
        assertEquals(3, planes.size());
        planes.forEach(p -> assertArrayEquals(new int[]{0, 4}, p.hyperbolicIndex()));
    }

    @Test
    public void test65_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(65, 5);
        assertEquals(1777, planes.size());
        HyperbolicPlaneTest.testCorrectness(planes.get(0), of(5), 16);
        assertArrayEquals(new int[]{3, 3}, planes.get(0).hyperbolicIndex());
        HyperbolicPlaneTest.checkPlane(planes.get(0), 65, 65);
//        planes.forEach(p -> {
//            assertArrayEquals(new int[]{1, 3}, p.hyperbolicIndex());
//        });
    }

    @Test
    public void test41_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(41, 5);
        assertEquals(15, planes.size());
//        int idx = IntStream.range(0, planes.size()).filter(i -> Arrays.equals(new int[]{3, 3}, planes.get(i).hyperbolicIndex())).findAny().orElseThrow();
//        System.out.println(idx);
    }

    @Test
    public void test45_5() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(45, 5);
        assertEquals(30, planes.size());
//        int idx = IntStream.range(0, planes.size()).filter(i -> Arrays.equals(new int[]{2, 2}, planes.get(i).hyperbolicIndex())).findAny().orElseThrow();
//        System.out.println(idx);
    }

    @Test
    public void test37_4() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(37, 4);
        assertEquals(51402, planes.size());
        int idx = IntStream.range(0, planes.size()).filter(i -> Arrays.equals(new int[]{1, 1}, planes.get(i).hyperbolicIndex())).findAny().orElseThrow();
        System.out.println(idx);
    }

    @Test
    public void test28_4() throws IOException {
        List<HyperbolicPlane> planes = readPlanes(28, 4);
        assertEquals(4466, planes.size());
        HyperbolicPlane plane = planes.get(1001);
        assertArrayEquals(new int[]{2, 2}, plane.hyperbolicIndex());
        HyperbolicPlaneTest.checkPlane(plane, 28, 28);
        HyperbolicPlaneTest.testCorrectness(plane, of(4), 9);
        int[][] sorted = StreamSupport.stream(plane.lines().spliterator(), false).map(plane::line)
                .map(bs -> bs.stream().toArray())
                .sorted((f, s) -> {
                    int d0 = f[0] - s[0];
                    if (d0 != 0) {
                        return d0;
                    }
                    int d1 = f[1] - s[1];
                    if (d1 != 0) {
                        return d1;
                    }
                    return f[2] - s[2];
                }).toArray(int[][]::new);
        String[] design = IntStream.range(0, sorted[0].length).mapToObj(i -> Arrays.stream(sorted)
                .map(ints -> String.valueOf(Character.forDigit(ints[i], 36))).collect(Collectors.joining())).toArray(String[]::new);
        Arrays.stream(design).forEach(System.out::println);
    }
}
