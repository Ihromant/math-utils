package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Rational;

public record MoultonPoint(Rational x, Rational y) {
    public MoultonLine lineTo(MoultonPoint other) {
        if (other.equals(this)) {
            throw new IllegalArgumentException();
        }
        int cmp = this.x.compareTo(other.x);
        if (cmp == 0) {
            return new VerticalLine(x);
        }
        if (cmp > 0) {
            return other.lineTo(this);
        }
        Rational dx = other.x.sub(this.x);
        Rational dy = other.y.sub(this.y);
        Rational tan = dy.div(dx);
        if (tan.compareTo(Rational.ZERO) < 0) {
            if (this.x.numer() > 0 && other.x.numer() > 0) {
                Rational y = this.y.sub(this.x.mul(tan));
                return new NegativeLine(y, tan);
            }
            if (this.x.numer() < 0 && other.x.numer() < 0) {
                Rational y = this.y.sub(this.x.mul(tan));
                return new NegativeLine(y, tan.mul(Rational.of(2)));
            }
            Rational dx1 = other.x.sub(this.x.div(Rational.of(2)));
            Rational slope = dy.div(dx1);
            Rational y = other.y.sub(other.x.mul(slope));
            return new NegativeLine(y, slope);
        } else {
            Rational y = this.y.sub(this.x.mul(tan));
            return new PositiveLine(y, tan);
        }
    }
}
