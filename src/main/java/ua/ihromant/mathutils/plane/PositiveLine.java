package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Rational;

import java.util.concurrent.ThreadLocalRandom;

public record PositiveLine(Rational y, Rational tan) implements MoultonLine {
    @Override
    public MoultonPoint intersection(MoultonLine other) {
        return switch (other) {
            case PositiveLine pl -> {
                if (pl.tan().equals(tan)) {
                    yield null;
                } else {
                    Rational diffY = pl.y().sub(this.y());
                    Rational diffTan = pl.tan().sub(this.tan());
                    Rational x = diffY.div(diffTan).neg();
                    yield new MoultonPoint(x, atX(x));
                }
            }
            case NegativeLine nl -> {
                int cmp = nl.y().compareTo(this.y());
                Rational diffY = nl.y().sub(this.y());
                Rational diffTan = this.tan().sub(cmp >= 0 ? nl.tan() : nl.tan().div(Rational.of(2)));
                Rational x = diffY.div(diffTan);
                yield new MoultonPoint(x, atX(x));
            }
            case VerticalLine vl -> new MoultonPoint(vl.x(), atX(vl.x()));
        };
    }

    @Override
    public boolean contains(MoultonPoint point) {
        return atX(point.x()).equals(point.y());
    }

    @Override
    public boolean isParallel(MoultonLine other) {
        return other instanceof PositiveLine pl && pl.tan().equals(tan);
    }

    @Override
    public MoultonLine parallelThrough(MoultonPoint point) {
        return new PositiveLine(point.y().sub(atX(point.x())).add(y), tan);
    }

    @Override
    public MoultonPoint randomPoint() {
        Rational x = Rational.of(ThreadLocalRandom.current().nextInt(-5, 5));
        return new MoultonPoint(x, atX(x));
    }

    public Rational atX(Rational x) {
        return y.add(x.mul(tan));
    }
}
