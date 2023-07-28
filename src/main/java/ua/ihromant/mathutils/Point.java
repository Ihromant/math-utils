package ua.ihromant.mathutils;

public record Point(Rational x, Rational y) {
    public static Point of(int x, int y) {
        return new Point(Rational.of(x), Rational.of(y));
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

    @Override
    public String toString() {
        return "(" + x + "," + y + ")";
    }
}
