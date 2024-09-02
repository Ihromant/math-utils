package ua.ihromant.mathutils;

import java.math.BigInteger;

public record Point(Rational x, Rational y) {
    public static Point of(long x, long y) {
        return new Point(Rational.of(x), Rational.of(y));
    }

    public static Point of(BigInteger x, BigInteger y) {
        return new Point(Rational.of(x.longValue()), Rational.of(y.longValue()));
    }

    public Point add(Point that) {
        return new Point(this.x.add(that.x), this.y.add(that.y));
    }

    public Point sub(Point that) {
        return new Point(this.x.sub(that.x), this.y.sub(that.y));
    }

    public Rational slope() {
        return y.div(x);
    }

    public Point mul(int c) {
        Rational mult = Rational.of(c);
        return new Point(x.mul(mult), y.mul(mult));
    }

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
