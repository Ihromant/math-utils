package ua.ihromant.mathutils;

import java.util.List;
import java.util.stream.Stream;

public record Rational(long numer, long denom) implements Comparable<Rational> {
    public static final Rational ZERO = new Rational(0, 1);

    public static long gcd(long a, long b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static Rational of(long i) {
        return new Rational(i, 1);
    }

    public static Rational of(long numer, long denom) {
        boolean neg = numer < 0;
        if (neg) {
            numer = -numer;
        }
        long gcd = gcd(numer, denom);
        return new Rational((neg ? -numer : numer) / gcd, denom / gcd);
    }

    public Rational neg() {
        return new Rational(-numer, denom);
    }

    public Rational inv() {
        if (numer == 0) {
            throw new ArithmeticException();
        }
        if (numer < 0) {
            return new Rational(-denom, -numer);
        } else {
            return new Rational(denom, numer);
        }
    }

    public Rational add(Rational that) {
        return of(this.numer * that.denom + this.denom * that.numer, this.denom * that.denom);
    }

    public Rational sub(Rational that) {
        return of(this.numer * that.denom - this.denom * that.numer, this.denom * that.denom);
    }

    public Rational mul(Rational that) {
        return of(this.numer * that.numer, this.denom * that.denom);
    }

    public Rational div(Rational that) {
        boolean neg = that.numer < 0;
        return neg ? of(-this.numer * that.denom, -this.denom * that.numer)
                : of(this.numer * that.denom, this.denom * that.numer);
    }

    public boolean isInt() {
        return denom == 1;
    }

    @Override
    public String toString() {
        if (denom == 1) {
            return String.valueOf(numer);
        } else {
            return numer + "/" + denom;
        }
    }

    public static final List<Rational> SIMPLE_SLOPES = List.of(
            Rational.of(1, 6),
            Rational.of(1, 5),
            Rational.of(1, 4),
            Rational.of(1, 3),
            Rational.of(1, 2),
            Rational.of(1, 1),
            Rational.of(2, 1),
            Rational.of(2, 3),
            Rational.of(2, 5),
            Rational.of(3, 1),
            Rational.of(3, 2),
            Rational.of(3, 4),
            Rational.of(3, 5),
            Rational.of(4, 1),
            Rational.of(4, 3),
            Rational.of(4, 5),
            Rational.of(5, 1),
            Rational.of(5, 2),
            Rational.of(5, 3),
            Rational.of(5, 4),
            Rational.of(5, 6),
            Rational.of(6, 5));

    public static final List<Rational> LATEX_SLOPES = Stream.concat(SIMPLE_SLOPES.stream(),
            SIMPLE_SLOPES.stream().map(Rational::neg)).toList();

    public long max() {
        return Math.max(Math.abs(numer), denom);
    }

    @Override
    public int compareTo(Rational o) {
        return (int) (this.numer * o.denom - this.denom * o.numer);
    }
}
