package ua.ihromant.mathutils;

import java.util.Objects;

public class Complex {
    // x + iy
    private final double x;
    private final double y;

    public Complex(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public Complex(double x) {
        this(x, 0);
    }

    public double re() {
        return x;
    }

    public double im() {
        return y;
    }

    public Complex add(Complex that) {
        return new Complex(this.x + that.x, this.y + that.y);
    }

    public Complex add(double numb) {
        return new Complex(x + numb, y);
    }

    public Complex sub(Complex that) {
        return new Complex(this.x - that.x, this.y - that.y);
    }

    public Complex sub(double numb) {
        return new Complex(x - numb, y);
    }

    public Complex mul(Complex that) {
        return new Complex(this.x * that.x - this.y * that.y, this.y * that.x + this.x * that.y);
    }

    public Complex mul(double numb) {
        return new Complex(numb * x, numb * y);
    }

    public Complex div(Complex that) {
        double scale = that.x * that.x + that.y * that.y;
        return new Complex((this.x * that.x + this.y * that.y) / scale, (this.y * that.x - this.x * that.y) / scale);
    }

    public double abs() {
        return Math.hypot(x, y);
    }

    public double argument() {
        return Math.atan2(y, x);
    }

    public static Complex fromPolar(double abs, double arg) {
        return new Complex(abs * Math.cos(arg), abs * Math.sin(arg));
    }

    public Complex[] nthRoots(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException();
        }
        double abs = Math.pow(abs(), 1.0 / n);
        double angle = argument() / n;
        Complex[] result = new Complex[n];
        for (int i = 0; i < n; i++) {
            result[i] = fromPolar(abs, angle + 2 * Math.PI * i / n);
        }
        return result;
    }

    @Override
    public String toString() {
        return x + (y < 0 ? "" : "+") + y + "i";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Complex that = (Complex) o;
        return Double.compare(that.x, x) == 0 && Double.compare(that.y, y) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}
