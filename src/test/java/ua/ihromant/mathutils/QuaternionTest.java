package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import java.util.concurrent.ThreadLocalRandom;

import static org.junit.jupiter.api.Assertions.*;

public class QuaternionTest {
    @Test
    public void test() {
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
