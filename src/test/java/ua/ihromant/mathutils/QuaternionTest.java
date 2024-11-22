package ua.ihromant.mathutils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuaternionTest {
    @Test
    public void test() {
        Rational half = new Rational(1, 2);
        Quaternion q = new Quaternion(half.neg(), half, half, half);
        assertEquals(new Quaternion(1), q.mul(q).mul(q));
    }
}
