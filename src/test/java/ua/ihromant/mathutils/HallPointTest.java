package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HallPointTest {
    @Test
    public void testCorrectness() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            int z = HallPoint.add(x, y);
            assertEquals(x, HallPoint.add(y, z));
            assertEquals(y, HallPoint.add(x, z));
        }));
    }

    @Test
    public void testReflexivity() {
        IntStream.range(0, HallPoint.SIZE).forEach(i -> assertEquals(i, HallPoint.add(i, i)));
    }

    @Test
    public void testSymmetric() {
        IntStream.range(0, HallPoint.SIZE).forEach(i -> IntStream.range(0, HallPoint.SIZE).forEach(j ->
                assertEquals(HallPoint.add(i, j), HallPoint.add(j, i))));
    }

    @Test
    public void testDistributive() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y ->
                IntStream.range(0, HallPoint.SIZE).forEach(z ->
                        assertEquals(HallPoint.add(x, HallPoint.add(y, z)),
                                HallPoint.add(HallPoint.add(x, y), HallPoint.add(x, z))))));
    }

    @Test
    public void checkLinealIdentity() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            if (HallPoint.collinear(x, y, x)) {
                assertEquals(x, y);
            }
        }));
    }

    @Test
    public void checkLinealReflexivity() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y -> {
            assertTrue(HallPoint.collinear(x, x, y));
            assertTrue(HallPoint.collinear(x, y, y));
        }));
    }

    @Test
    public void checkLinearExchange() {
        IntStream.range(0, HallPoint.SIZE).forEach(x -> IntStream.range(0, HallPoint.SIZE).forEach(y ->
                IntStream.range(0, HallPoint.SIZE).forEach(a -> IntStream.range(0, HallPoint.SIZE).forEach(b -> {
                    if (HallPoint.collinear(a, x, b) && HallPoint.collinear(a, y, b) && x != y) {
                        assertTrue(HallPoint.collinear(x, a, y));
                        assertTrue(HallPoint.collinear(x, b, y));
                    }
                }))));
    }

    @Test
    public void findRegularityBreak() {
        long time = System.currentTimeMillis();
        for (int o = 0; o < HallPoint.SIZE; o++) {
            for (int a = 0; a < HallPoint.SIZE; a++) {
//                if (o == a) {
//                    continue;
//                }
                for (int u = 0; u < HallPoint.SIZE; u++) {
//                    if (u == a || u == o) {
//                        continue;
//                    }
                    System.out.println(o + " " + a + " " + u);
                    outer: for (int y = 0; y < HallPoint.SIZE; y++) {
//                        if (y == o || y == a || y == u) {
//                            continue;
//                        }
                        int x = HallPoint.add(o, a);
                        int z = HallPoint.add(y, x);
                        for (int v = 0; v < HallPoint.SIZE; v++) {
                            for (int s = 0; s < HallPoint.SIZE; s++) {
                                for (int t = 0; t < HallPoint.SIZE; t++) {
                                    for (int c = 0; c < HallPoint.SIZE; c++) {
                                        if (HallPoint.collinear(o, v, u) && HallPoint.collinear(o, s, a) && HallPoint.collinear(o, t, y)
                                                && HallPoint.collinear(s, c, t) && HallPoint.collinear(v, z, c)) {
                                            break outer;
                                        }
                                    }
                                }
                            }
                        }
                        System.out.println(o + " " + a + " " + u + " " + y + " spent " + (System.currentTimeMillis() - time));
                        return;
                    }
                }
            }
        }
        System.out.println("Not found " + (System.currentTimeMillis() - time));
    }
}
