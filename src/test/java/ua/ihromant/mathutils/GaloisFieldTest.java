package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

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
    public void testFactorize() {
        assertArrayEquals(new int[]{17, 19}, GaloisField.factorize(17 * 19));
        assertArrayEquals(new int[]{333667}, GaloisField.factorize(333667));
        assertArrayEquals(new int[]{3, 7, 11, 13, 37}, GaloisField.factorize(111111));
        assertArrayEquals(new int[]{23, 23}, GaloisField.factorize(529));
        assertArrayEquals(IntStream.range(0, 25).map(i -> 2).toArray(), GaloisField.factorize(1 << 25));
    }

    @Test
    public void testCorrectness() {
        GaloisField fd = new GaloisField(61);
        testField(fd);
        fd = new GaloisField(4);
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
        assertEquals(13, fd.solve(new int[]{1, 4, 2}));
    }
}
