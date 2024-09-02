package ua.ihromant.mathutils;

import java.util.List;
import java.util.stream.Stream;

public record Rational(int numer, int denom) implements Comparable<Rational> {
    public static final Rational ZERO = new Rational(0, 1);

    public static int gcd(int a, int b) {
        return b == 0 ? a : gcd(b, a % b);
    }

    public static Rational of(int i) {
        return new Rational(i, 1);
    }

    public static Rational of(int numer, int denom) {
        boolean neg = numer < 0;
        if (neg) {
            numer = -numer;
        }
        int gcd = gcd(numer, denom);
        return new Rational((neg ? -numer : numer) / gcd, denom / gcd);
    }

    public Rational neg() {
        return new Rational(-numer, denom);
    }

    public Rational add(Rational that) {
        return of(this.numer * that.denom + this.denom * that.numer, that.denom * that.denom);
    }

    public Rational sub(Rational that) {
        return of(this.numer * that.denom - this.denom * that.numer, that.denom * that.denom);
    }

    public Rational mul(Rational that) {
        return of(this.numer * that.numer, this.denom * that.denom);
    }

    public Rational div(Rational that) {
        return of(this.numer * that.denom, this.denom * that.numer);
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

    public int max() {
        return Math.max(Math.abs(numer), denom);
    }

    @Override
    public int compareTo(Rational o) {
        return this.numer * o.denom - this.denom * o.numer;
    }
}
