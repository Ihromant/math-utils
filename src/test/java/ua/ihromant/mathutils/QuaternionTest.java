package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class QuaternionTest {
    @Test
    public void test() {
        Quaternion a1 = new Quaternion(Rational.ZERO, Rational.of(1), Rational.of(1), Rational.ZERO);
        Quaternion a2 = new Quaternion(Rational.ZERO, Rational.of(1), Rational.ZERO, Rational.of(1));
        System.out.println(a1.mul(a2));
        System.out.println(a2.mul(a1));
        assertNotEquals(a1.mul(a2), a2.mul(a1));
        Rational half = new Rational(1, 2);
        Quaternion q = new Quaternion(half.neg(), half, half, half);
        assertEquals(new Quaternion(1), q.mul(q).mul(q));
        for (int i = 0; i < 10000; i++) {
            Quaternion c = new Quaternion(Rational.of(1), generateRational(), generateRational(), generateRational());
            Quaternion a = c.inv().mul(q).inv();
            Quaternion q1 = c.inv().mul(a);
            assertNotEquals(q, q1);
            assertEquals(new Quaternion(1), q1.mul(q1).mul(q1));
        }
    }

    private Rational generateRational() {
        while (true) {
            try {
                return Rational.of(ThreadLocalRandom.current().nextInt(-5, 5), ThreadLocalRandom.current().nextInt(-5, 5));
            } catch (ArithmeticException e) {
                // ok
            }
        }
    }
}
