package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Rational;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MoultonTest {
    @Test
    public void testCorrectness() {
        MoultonPoint p1 = new MoultonPoint(Rational.of(-2), Rational.of(4));
        MoultonPoint p2 = new MoultonPoint(Rational.of(1), Rational.of(2));
        MoultonLine l = new NegativeLine(Rational.of(3), Rational.of(-1));
        assertEquals(l, p1.lineTo(p2));
        assertEquals(l, p2.lineTo(p1));
        MoultonPoint p3 = new MoultonPoint(Rational.of(3), Rational.of(6));
        assertEquals(new PositiveLine(Rational.of(0), Rational.of(2)), p3.lineTo(p2));
        assertEquals(new PositiveLine(Rational.of(0), Rational.of(2)), p2.lineTo(p3));
        assertEquals(p2, p1.lineTo(p2).intersection(p3.lineTo(p2)));
        assertEquals(new PositiveLine(Rational.of(24, 25), Rational.of(2, 5)), p3.lineTo(p1));
        assertEquals(p3, p3.lineTo(p2).intersection(p3.lineTo(p1)));
        MoultonLine l1 = new NegativeLine(Rational.of(1), Rational.of(-3));
        MoultonLine l2 = new NegativeLine(Rational.of(5), Rational.of(-3));
        assertEquals(new MoultonPoint(Rational.of(-2), Rational.of(4)), l.intersection(l1));
        assertEquals(new MoultonPoint(Rational.of(1), Rational.of(2)), l.intersection(l2));
    }
}
