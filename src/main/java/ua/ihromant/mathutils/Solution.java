package ua.ihromant.mathutils;

import java.math.BigInteger;
import java.util.Arrays;

public class Solution {
    private static final BigInteger TWO = BigInteger.valueOf(2);
    private static final Fraction HALF = new Fraction(new Polynomial(BigInteger.ONE), new Polynomial(TWO));

    public static void main(String[] args) {
        Fraction b = new Fraction(new Polynomial(BigInteger.ONE), new Polynomial(BigInteger.ONE));
        Fraction a = new Fraction(new Polynomial(BigInteger.ONE, BigInteger.ZERO), new Polynomial(BigInteger.ONE));
        int counter = 0;
        while (counter < 7) {
            System.out.println(counter + " a: " + a);
            System.out.println(counter + " b: " + b);
            Fraction e = a.mul(a).add(new Fraction(new Polynomial(BigInteger.ONE.negate(), BigInteger.ZERO), new Polynomial(BigInteger.ONE)));
            System.out.println(counter + " da: " + e);
            Fraction d = b.mul(b).add(new Fraction(new Polynomial(BigInteger.ONE.negate(), BigInteger.ZERO), new Polynomial(BigInteger.ONE)));
            System.out.println(counter + " db: " + d);
            Fraction c = harmonic(a, b);
            b = arithm(a, b);
            a = c;
            counter++;
        }
    }

    private static Fraction harmonic(Fraction first, Fraction second) {
        return new Fraction(new Polynomial(TWO, BigInteger.ZERO), new Polynomial(BigInteger.ONE)).mul(first.add(second).inv());
    }

    private static Fraction arithm(Fraction first, Fraction second) {
        return HALF.mul(first.add(second));
    }

    public static class Polynomial {
        private final BigInteger[] coeff;

        private Polynomial(boolean needsCopy, BigInteger... coeff) {
            if (needsCopy) {
                this.coeff = filterCopy(coeff);
            } else {
                this.coeff = coeff;
            }
        }

        private BigInteger[] filterCopy(BigInteger[] coeffs) {
            int from = 0;
            while (from < coeffs.length && coeffs[from].equals(BigInteger.ZERO)) {
                from++;
            }
            BigInteger[] result = new BigInteger[coeffs.length - from];
            System.arraycopy(coeffs, from, result, 0, coeffs.length - from);
            return result;
        }

        public Polynomial(BigInteger... coeff) {
            this(true, coeff);
        }

        public Polynomial mul(Polynomial pol) {
            if (this.coeff.length == 0 && pol.coeff.length == 0) {
                return this;
            }
            BigInteger[] coeffs = new BigInteger[this.coeff.length + pol.coeff.length - 1];
            Arrays.fill(coeffs, BigInteger.ZERO);
            for (int i = 0; i < this.coeff.length; i++) {
                for (int j = 0; j < pol.coeff.length; j++) {
                    coeffs[i + j] = coeffs[i + j].add(this.coeff[i].multiply(pol.coeff[j]));
                }
            }
            return new Polynomial(false, coeffs);
        }

        public BigInteger coeff(int degree) {
            if (degree < 0 || degree > coeff.length - 1) {
                return BigInteger.ZERO;
            }
            return coeff[coeff.length - degree - 1];
        }

        public Polynomial add(Polynomial that) {
            BigInteger[] result = new BigInteger[Math.max(this.coeff.length, that.coeff.length)];
            for (int i = 0; i < result.length; i++) {
                result[i] = this.coeff(result.length - 1 - i).add(that.coeff(result.length - 1 - i));
            }
            return new Polynomial(result.length != 0 && result[0].equals(BigInteger.ZERO), result);
        }

        @Override
        public String toString() {
            if (coeff.length == 0) {
                return "0";
            }
            StringBuilder result = new StringBuilder(coeff[0] + (coeff.length == 1 ? "" : ("x^" + (coeff.length - 1))));
            for (int i = 1; i < coeff.length; i++) {
                if (!coeff[i].equals(BigInteger.ZERO)) {
                    result.append(coeff[i].compareTo(BigInteger.ZERO) > 0 ? "+" + coeff[i] : coeff[i]);
                    result.append(i == coeff.length - 1 ? "" : ("x^" + (coeff.length - i - 1)));
                }
            }
            return result.toString();
        }
    }

    public static class Fraction {
        private final Polynomial numerator;
        private final Polynomial denominator;

        public Fraction(Polynomial numerator, Polynomial denominator) {
            this.numerator = numerator;
            this.denominator = denominator;
        }

        public Fraction add(Fraction other) {
            return new Fraction(this.numerator.mul(other.denominator).add(this.denominator.mul(other.numerator)), this.denominator.mul(other.denominator));
        }

        public Fraction mul(Fraction other) {
            return new Fraction(this.numerator.mul(other.numerator), this.denominator.mul(other.denominator));
        }

        public Fraction inv() {
            return new Fraction(this.denominator, this.numerator);
        }

        @Override
        public String toString() {
            return numerator.toString() + "/" + denominator.toString();
        }
    }
}
