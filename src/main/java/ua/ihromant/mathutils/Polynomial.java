package ua.ihromant.mathutils;

import java.util.Arrays;

public class Polynomial {
    private static final double ERROR = 0.0001;
    private static final Complex ONE = new Complex(1.0);
    private static final Complex STARTING_COMPLEX = new Complex(0.4, 0.9);

    private final double[] coeff;

    private Polynomial(boolean needsCopy, double... coeff) {
        if (needsCopy) {
            this.coeff = filterCopy(coeff);
        } else {
            this.coeff = coeff;
        }
    }

    private static double[] filterCopy(double[] coeffs) {
        int from = 0;
        while (from < coeffs.length && coeffs[from] == 0.0) {
            from++;
        }
        double[] result = new double[coeffs.length - from];
        System.arraycopy(coeffs, from, result, 0, coeffs.length - from);
        return result;
    }

    public Polynomial(double... coeff) {
        this(true, coeff);
    }

    public Polynomial mul(Polynomial pol) {
        if (this.coeff.length == 0 && pol.coeff.length == 0) {
            return this;
        }
        double[] coeffs = new double[this.coeff.length + pol.coeff.length - 1];
        for (int i = 0; i < this.coeff.length; i++) {
            for (int j = 0; j < pol.coeff.length; j++) {
                coeffs[i + j] += this.coeff[i] * pol.coeff[j];
            }
        }
        return new Polynomial(false, coeffs);
    }

    public Polynomial negate() {
        double[] coeffs = new double[coeff.length];
        for (int i = 0; i < coeff.length; i++) {
            coeffs[i] = -coeff[i];
        }
        return new Polynomial(false, coeffs);
    }

    public double coeff(int degree) {
        if (degree < 0 || degree > coeff.length - 1) {
            return 0.0;
        }
        return coeff[coeff.length - degree - 1];
    }

    public Polynomial add(Polynomial that) {
        double[] result = new double[Math.max(this.coeff.length, that.coeff.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] += this.coeff(result.length - 1 - i) + that.coeff(result.length - 1 - i);
        }
        return new Polynomial(result.length != 0 && result[0] == 0.0, result);
    }

    public Polynomial subtract(Polynomial that) {
        double[] result = new double[Math.max(this.coeff.length, that.coeff.length)];
        for (int i = 0; i < result.length; i++) {
            result[i] += this.coeff(result.length - 1 - i) - that.coeff(result.length - 1 - i);
        }
        return new Polynomial(result.length != 0 && result[0] == 0.0, result);
    }

    public Polynomial derivative() {
        double[] result = new double[coeff.length == 0 ? 0 : coeff.length - 1];
        for (int i = 0; i < coeff.length - 1; i++) {
            result[i] = coeff[i] * (coeff.length - i - 1);
        }
        return new Polynomial(false, result);
    }

    public double at(double t) {
        double result = 0;
        for (double v : coeff) {
            result = result * t + v;
        }
        return result;
    }

    private static Complex at(Complex number, double[] coeff) {
        Complex result = new Complex(0);
        for (double v : coeff) {
            result = result.mul(number).add(v);
        }
        return result;
    }

    public double[] solve() {
        double[] normalized = normalize();
        double[] result = solveNormalized(normalized);
        if (normalized.length != coeff.length) {
            return merge(result, new double[]{0});
        }
        return result;
    }

    private static double[] solveNormalized(double[] coeff) {
        switch (coeff.length) {
            case 0:
            case 1:
                return new double[0];
            case 2:
                return new double[]{-coeff[1]};
            case 3:
                return solveQuadraticEquation(coeff[1], coeff[2]);
            case 4:
                double p = (3 * coeff[2] - coeff[1] * coeff[1]) / 3;
                double q = (2 * coeff[1] * coeff[1] * coeff[1] - 9 * coeff[1] * coeff[2] + 27 * coeff[3]) / 27;
                double[] result = solveDepressedCubic(p, q);
                for (int i = 0; i < result.length; i++) {
                    result[i] -= coeff[1] / 3;
                }
                return result;
            // uncomment if you need radical solution. It's faster, but less precise
//            case 5:
//                double shift = -coeff[1] / 4;
//                double[] roots = solveDepressedQuartic(-6 * shift * shift + coeff[2],
//                        -8 * shift * shift * shift + 2 * shift * coeff[2] + coeff[3],
//                        -3 * shift * shift * shift * shift + coeff[2] * shift * shift + shift * coeff[3] + coeff[4]);
//                for (int i = 0; i < roots.length; i++) {
//                    roots[i] += shift;
//                }
//                return roots;
            default:
                return filterComplex(durandKerner(coeff));
        }
    }

    private static double[] solveDepressedQuartic(double c, double d, double e) {
        double[] cubicRoots = solveNormalized(new double[]{1, 2 * c, c * c - 4 * e, -d * d});
        double p = Double.NaN;
        for (double root : cubicRoots) {
            if (root >= 0) {
                p = Math.sqrt(root);
            }
        }
        double r = -p;
        double s = 0.5 * (c + p * p + d / p);
        double q = 0.5 * (c + p * p - d / p);
        return merge(solveQuadraticEquation(p, q), solveQuadraticEquation(r, s));
    }

    static double[] merge(double[] first, double[] second) {
        double[] result = new double[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    static boolean zero(double numb) {
        return Math.abs(numb) < ERROR;
    }

    private double[] normalize() {
        int counter = 0;
        while (counter < coeff.length - 1 && coeff[coeff.length - 1 - counter] == 0.0) {
            counter++;
        }
        double[] coeffs = new double[coeff.length - counter];
        for (int i = 0; i < coeffs.length; i++) {
            coeffs[i] = coeff[i] / coeff[0];
        }
        return coeffs;
    }

    private static double[] solveQuadraticEquation(double b, double c) {
        double discr = b * b - 4 * c;
        if (discr < 0) {
            return new double[0];
        } else if (discr == 0) {
            return new double[]{-0.5 * b};
        } else {
            double sqrt = Math.sqrt(discr);
            return new double[]{0.5 * (sqrt - b), -0.5 * (b + sqrt)};
        }
    }

    private static double[] solveDepressedCubic(double p, double q) {
        if (q == 0) {
            if (p < 0) {
                return new double[]{0.0, Math.sqrt(-p), -Math.sqrt(-p)};
            } else {
                return new double[]{0.0};
            }
        }
        double left = 4 * p * p * p;
        double right = 27 * q * q;
        double discr = left + right;
        if (discr == 0) {
            return new double[]{3 * q / p, -1.5 * q / p};
        }
        if (discr < 0) {
            double[] result = new double[3];
            double coeff = 2 * Math.sqrt(-p / 3);
            double acos = Math.acos(-Math.signum(q) * Math.sqrt(-right / left));
            for (int i = 0; i < 3; i++) {
                result[i] = coeff * Math.cos((acos - 2 * Math.PI * i) / 3);
            }
            return result;
        }
        // discr > 0
        double sqrt = Math.sqrt(discr / 108);
        double cubed = -0.5 * q + sqrt;
        double c = cubed >= 0 ? Math.pow(-0.5 * q + sqrt, 1.0 / 3) : -Math.pow(-cubed, 1.0 / 3);
        return new double[]{c - p / (3 * c)};
    }

    private static double[] filterComplex(Complex[] roots) {
        int size = 0;
        for (Complex root : roots) {
            size += zero(root.im()) ? 1 : 0;
        }
        double[] result = new double[size];
        int counter = 0;
        for (Complex root : roots) {
            if (zero(root.im())) {
                result[counter++] = root.re();
            }
        }
        return result;
    }

    private static Complex[] durandKerner(double[] coeff) {
        int degree = coeff.length - 1;

        Complex[] roots = new Complex[degree];
        Complex[] next = new Complex[degree];

        roots[0] = ONE;
        for (int i = 1; i < degree; i++) {
            roots[i] = STARTING_COMPLEX.mul(roots[i - 1]);
        }

        boolean finished = false;
        int counter = 0;
        while (!finished && counter++ < 200) {
            for (int j = 0; j < degree; j++) {
                Complex root = roots[j];

                Complex denominator = ONE;
                for (int k = 0; k < degree; k++) {
                    if (k != j) {
                        denominator = denominator.mul(root.sub(roots[k]));
                    }
                }

                next[j] = root.sub(at(root, coeff).div(denominator));
            }

            finished = true;
            for (int i = 0; i < degree; i++) {
                if (!zero(next[i].sub(roots[i]).mul(100).abs())) {
                    finished = false;
                    break;
                }
            }

            Complex[] swap = next;
            next = roots;
            roots = swap;
        }

        if (!finished) {
            return new Complex[0];
        }

        return roots;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Polynomial that = (Polynomial) o;
        return Arrays.equals(coeff, that.coeff);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(coeff);
    }

    @Override
    public String toString() {
        if (coeff.length == 0) {
            return "0";
        }
        StringBuilder result = new StringBuilder(coeff[0] + (coeff.length == 1 ? "" : ("x^" + (coeff.length - 1))));
        for (int i = 1; i < coeff.length; i++) {
            if (coeff[i] != 0.0) {
                result.append(coeff[i] > 0 ? "+" + coeff[i] : coeff[i]);
                result.append(i == coeff.length - 1 ? "" : ("x^" + (coeff.length - i - 1)));
            }
        }
        return result.toString();
    }
}
