package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SemiFieldPoint {
    private static final SemiFieldPoint[][] CACHE = IntStream.range(0, SemiField.SIZE)
            .mapToObj(i -> IntStream.range(0, SemiField.SIZE).mapToObj(j -> new SemiFieldPoint(i, j))
                    .toArray(SemiFieldPoint[]::new))
            .toArray(SemiFieldPoint[][]::new);
    private static final SemiFieldPoint ZERO = CACHE[SemiField.ZERO][SemiField.ZERO];

    private final int x;
    private final int y;

    private SemiFieldPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static Stream<SemiFieldPoint> nonZeroPoints() {
        return Arrays.stream(CACHE).flatMap(Arrays::stream).filter(p -> !p.equals(ZERO));
    }

    public static SemiFieldPoint of(int x, int y) {
        return CACHE[x][y];
    }

    public SemiFieldPoint add(SemiFieldPoint that) {
        return SemiFieldPoint.of(SemiField.add(this.x, that.x), SemiField.add(this.y, that.y));
    }

    public SemiFieldPoint sub(SemiFieldPoint that) {
        return SemiFieldPoint.of(SemiField.sub(this.x, that.x), SemiField.sub(this.y, that.y));
    }

    public SemiFieldPoint mul(int scalar) {
        return SemiFieldPoint.of(SemiField.mul(scalar, this.x), SemiField.mul(scalar, this.y));
    }

    public SemiFieldPoint neg() {
        return of(SemiField.neg(this.x), SemiField.neg(this.y));
    }

    @Override
    public String toString() {
        return "(" + SemiField.toString(x) + "," + SemiField.toString(y) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        SemiFieldPoint that = (SemiFieldPoint) o;
        return this.x == that.x && this.y == that.y;
    }

    @Override
    public int hashCode() {
        int result = x;
        result = 31 * result + y;
        return result;
    }

    public static SemiFieldPoint parse(String from) {
        int iolb = from.indexOf('(');
        int ioc = from.indexOf(',');
        int iorb = from.indexOf(')');
        return of(SemiField.parse(from.substring(iolb + 1, ioc)), SemiField.parse(from.substring(ioc + 1, iorb)));
    }

    public Integer multiplier(SemiFieldPoint that) {
        return IntStream.range(0, SemiField.SIZE).filter(i -> this.equals(that.mul(i))).boxed().findAny().orElse(null);
    }

    public boolean wrongParallel(SemiFieldPoint that) {
        return SemiField.mul(that.x, this.y) == SemiField.mul(this.x, that.y);
    }
}
