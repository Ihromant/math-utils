package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Rational;

import java.util.concurrent.ThreadLocalRandom;

public record NegativeLine(Rational y, Rational tan) implements MoultonLine {
    @Override
    public MoultonPoint intersection(MoultonLine other) {
        if (other instanceof NegativeLine nl) {
            if (nl.tan().equals(tan)) {
                return null;
            } else {
                Rational diffY = nl.y().sub(this.y());
                Rational diffTan = nl.tan().sub(this.tan());
                Rational x = diffY.div(diffTan).neg();
                if (x.numer() < 0) {
                    x = x.mul(Rational.of(2));
                }
                return new MoultonPoint(x, atX(x));
            }
        } else {
            return other.intersection(this);
        }
    }

    @Override
    public boolean contains(MoultonPoint point) {
        return atX(point.x()).equals(point.y());
    }

    @Override
    public boolean isParallel(MoultonLine other) {
        return other instanceof NegativeLine nl && nl.tan().equals(tan);
    }

    @Override
    public MoultonLine parallelThrough(MoultonPoint point) {
        return new NegativeLine(point.y().sub(atX(point.x())).add(y), tan);
    }

    @Override
    public MoultonPoint randomPoint() {
        Rational x = Rational.of(ThreadLocalRandom.current().nextInt(-5, 5));
        return new MoultonPoint(x, atX(x));
    }

    public Rational atX(Rational x) {
        Rational slope = x.numer() < 0 ? tan.div(Rational.of(2)) : tan;
        return y.add(x.mul(slope));
    }
}
