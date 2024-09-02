package ua.ihromant.mathutils.plane;

import ua.ihromant.mathutils.Rational;

import java.util.concurrent.ThreadLocalRandom;

public record VerticalLine(Rational x) implements MoultonLine {
    @Override
    public MoultonPoint intersection(MoultonLine other) {
        return switch (other) {
            case PositiveLine pl -> new MoultonPoint(x, pl.atX(x));
            case NegativeLine nl -> new MoultonPoint(x, nl.atX(x));
            case VerticalLine vl -> null;
        };
    }

    @Override
    public boolean contains(MoultonPoint point) {
        return point.x().equals(x);
    }

    @Override
    public boolean isParallel(MoultonLine other) {
        return other instanceof VerticalLine;
    }

    @Override
    public MoultonLine parallelThrough(MoultonPoint point) {
        return new VerticalLine(point.x());
    }

    @Override
    public MoultonPoint randomPoint() {
        Rational y = Rational.of(ThreadLocalRandom.current().nextInt(-5, 5));
        return new MoultonPoint(x, y);
    }
}
