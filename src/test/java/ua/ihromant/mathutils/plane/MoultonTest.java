package ua.ihromant.mathutils.plane;

import org.junit.jupiter.api.Test;
import ua.ihromant.mathutils.Rational;

import java.util.concurrent.ThreadLocalRandom;

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
        assertEquals(new PositiveLine(Rational.of(24, 5), Rational.of(2, 5)), p3.lineTo(p1));
        assertEquals(p3, p3.lineTo(p2).intersection(p3.lineTo(p1)));
        MoultonLine l1 = new NegativeLine(Rational.of(1), Rational.of(-3));
        MoultonLine l2 = new NegativeLine(Rational.of(5), Rational.of(-3));
        assertEquals(new MoultonPoint(Rational.of(-2), Rational.of(4)), l.intersection(l1));
        assertEquals(new MoultonPoint(Rational.of(1), Rational.of(2)), l.intersection(l2));
        assertEquals(new NegativeLine(Rational.of(-13), Rational.of(-2)),
                new MoultonPoint(Rational.of(-13), Rational.of(0)).lineTo(new MoultonPoint(Rational.of(0), Rational.of(-13))));
    }

    @Test
    public void testPlusPlusInverse() {
        int cnt = 10000;
        while (cnt-- > 0) {
            MoultonPoint a = new MoultonPoint(Rational.of(2), Rational.of(1));//generatePoint();
            MoultonPoint b;
            do {
                b = new MoultonPoint(Rational.of(-1), Rational.of(0));//generatePoint();
            } while (b.equals(a));
            MoultonLine ab = a.lineTo(b); // abf
            MoultonPoint c;
            do {
                c = new MoultonPoint(Rational.of(1), Rational.of(2));//generatePoint();
            } while (ab.contains(c));
            MoultonLine bc = b.lineTo(c); // bcg
            MoultonLine ad = bc.parallelThrough(a);
            MoultonLine ac = a.lineTo(c);
            MoultonLine bd = ac.parallelThrough(b); // bde
            MoultonLine ce = ab.parallelThrough(c);
            MoultonPoint d = ad.intersection(bd);
            MoultonPoint e = bd.intersection(ce);
            MoultonLine ef = bc.parallelThrough(e);
            MoultonPoint f = ab.intersection(ef);
            MoultonLine gf = bd.parallelThrough(f);
            MoultonPoint g = bc.intersection(gf);
            MoultonLine dg = d.lineTo(g);
            if (!dg.isParallel(ab)) {
                System.out.println("Counterexample");
                return;
            }
        }
        System.out.println("Satisfied");
    }

    private static MoultonPoint generatePoint() {
        return new MoultonPoint(Rational.of(ThreadLocalRandom.current().nextInt(-5, 5)),
                Rational.of(ThreadLocalRandom.current().nextInt(-5, 5)));
    }
}
