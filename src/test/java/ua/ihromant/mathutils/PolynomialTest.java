package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

public class PolynomialTest {
    @Test
    public void testConstructor() {
        assertEquals(new Polynomial(0, 1, 2, 3), new Polynomial(1, 2, 3));
    }

    @Test
    public void testAdd() {
        Polynomial first = new Polynomial(1, 2, 3);
        Polynomial second = new Polynomial(1, 2);
        Polynomial third = new Polynomial(2, 3, 4);
        assertEquals(first.add(second), second.add(first));
        assertEquals(new Polynomial(1, 3, 5), first.add(second));
        assertEquals(new Polynomial(3, 5, 7), first.add(third));
    }

    @Test
    public void testSubtract() {
        Polynomial first = new Polynomial(1, 2, 3);
        Polynomial second = new Polynomial(1, 2);
        Polynomial third = new Polynomial(2, 3, 4);
        assertEquals(first.subtract(second), second.subtract(first).negate());
        assertEquals(new Polynomial(1, 1, 1), first.subtract(second));
        assertEquals(new Polynomial(-1, -1, -1), first.subtract(third));
    }

    @Test
    public void testDerivative() {
        assertEquals(new Polynomial(4, 6, 6, 4), new Polynomial(1, 2, 3, 4, 5).derivative());
        assertEquals(new Polynomial(0), new Polynomial(1).derivative());
    }

    @Test
    public void testAt() {
        Polynomial pol = new Polynomial(1, 2, 3);
        assertEquals(3, pol.at(0), 0);
        assertEquals(6, pol.at(1), 0);
        assertEquals(11, pol.at(2), 0);
    }

    @Test
    public void testSolve() {
        Polynomial pol = new Polynomial(1, 5);
        assertEqualsDoubles(new double[]{-5.0}, pol.solve());
        pol = new Polynomial(1, 5, 6);
        assertEqualsDoubles(new double[]{-2.0, -3.0}, pol.solve());
        pol = new Polynomial(1, 4, 4);
        assertEqualsDoubles(new double[]{-2.0}, pol.solve());
        pol = new Polynomial(1, 4, 5);
        assertEqualsDoubles(new double[0], pol.solve());
        pol = new Polynomial(1, 0, 3, -4);
        assertEqualsDoubles(new double[]{1.0}, pol.solve());
        pol = new Polynomial(1, 0, -3, 2);
        assertEqualsDoubles(new double[]{1.0}, pol.solve());
        pol = new Polynomial(1, 3, 3, 1);
        assertEqualsDoubles(new double[]{-1.0}, pol.solve());
        pol = new Polynomial(1, 0, -3, 2);
        assertEqualsDoubles(new double[]{1.0, -2.0}, pol.solve());
        pol = new Polynomial(1, 0, -3, -2);
        assertEqualsDoubles(new double[]{-1.0, 2.0}, pol.solve());
        pol = new Polynomial(1, 6, 11, 6);
        assertEqualsDoubles(new double[]{-1.0, -2.0, -3.0}, pol.solve());
        pol = new Polynomial(1, 21, 126, 216);
        assertEqualsDoubles(new double[]{-3.0, -6.0, -12.0}, pol.solve());
        pol = new Polynomial(1, 2, 0, 0, 0);
        assertEqualsDoubles(new double[]{-2.0, 0.0}, pol.solve());
        pol = new Polynomial(1, -Math.PI).mul(new Polynomial(1, 4, 5));
        assertEqualsDoubles(new double[]{Math.PI}, pol.solve());
        pol = new Polynomial(1, 0, -1, 60);
        assertEqualsDoubles(new double[]{-4.0}, pol.solve());
    }

    private static double generate() {
        return 1000 * ThreadLocalRandom.current().nextDouble() - 500;
    }

    @Test
    public void quickCheckCubic() {
        for (int i = 0; i < 1000000; i++) {
            double p = generate();
            double r = generate();
            double s = generate();
            Polynomial first = new Polynomial(1, p);
            Polynomial second = new Polynomial(1, r, s);
            assertEqualsDoubles(Polynomial.merge(first.solve(), second.solve()), first.mul(second).solve());
        }
    }

    // long-running, uncomment just to check correctness
    //@Test
    public void quickCheckQuartic() {
        for (int i = 0; i < 1000000; i++) {
            double p = generate();
            double q = generate();
            double r = generate();
            double s = generate();
            Polynomial first = new Polynomial(1, p, q);
            Polynomial second = new Polynomial(1, r, s);
            assertEqualsDoubles(Polynomial.merge(first.solve(), second.solve()), first.mul(second).solve());
        }
    }

    //@Test
    public void testHigher() {
        // testing for 5 only. Testing for 6 starts failing, more degrees - more fails
        for (int k = 5; k < 6; k++) {
            for (int i = 0; i < 100000; i++) {
                Polynomial p = new Polynomial(1);
                double[] roots = new double[k];
                for (int j = 0; j < k; j++) {
                    double root = generate();
                    p = p.mul(new Polynomial(1, -root));
                    roots[j] = root;
                }
                assertEqualsDoubles(roots, p.solve());
            }
        }
    }

    @Test
    public void testMul() {
        Polynomial pol = new Polynomial(1, 2);
        Polynomial pol1 = new Polynomial(1, 3);
        assertEquals(new Polynomial(1, 5, 6), pol.mul(pol1));
    }

    static boolean close(double first, double second, double error) {
        double dist = Math.abs(first - second);
        return dist < error || dist / Math.abs(first) < error || dist / Math.abs(second) < error;
    }

    private void assertEqualsDoubles(double[] expected, double[] actual) {
        assertEqualsDoubles(expected, actual, 0.001);
    }

    private void assertEqualsDoubles(double[] expected, double[] actual, double error) {
        for (double e : expected) {
            boolean present = false;
            for (double a : actual) {
                if (close(e, a, error)) {
                    present = true;
                    break;
                }
            }
            if (!present) {
                fail("Solution " + e + " from " + Arrays.toString(expected) + " is not present in " + Arrays.toString(actual));
            }
        }
    }
}
