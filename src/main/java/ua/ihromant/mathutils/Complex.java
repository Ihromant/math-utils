package ua.ihromant.mathutils;

public record Complex(double re, double im) {
    public Complex(double re) {
        this(re, 0);
    }

    public Complex add(Complex that) {
        return new Complex(this.re + that.re, this.im + that.im);
    }

    public Complex add(double numb) {
        return new Complex(re + numb, im);
    }

    public Complex sub(Complex that) {
        return new Complex(this.re - that.re, this.im - that.im);
    }

    public Complex sub(double numb) {
        return new Complex(re - numb, im);
    }

    public Complex mul(Complex that) {
        return new Complex(this.re * that.re - this.im * that.im, this.im * that.re + this.re * that.im);
    }

    public Complex mul(double numb) {
        return new Complex(numb * re, numb * im);
    }

    public Complex div(Complex that) {
        double scale = that.re * that.re + that.im * that.im;
        return new Complex((this.re * that.re + this.im * that.im) / scale, (this.im * that.re - this.re * that.im) / scale);
    }

    public double abs() {
        return Math.hypot(re, im);
    }

    public double argument() {
        return Math.atan2(im, re);
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
        return re + (im < 0 ? "" : "+") + im + "i";
    }
}
