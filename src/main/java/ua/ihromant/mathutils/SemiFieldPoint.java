package ua.ihromant.mathutils;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SemiFieldPoint {
    private static final SemiFieldPoint[][] CACHE = IntStream.range(0, SemiField.SIZE)
            .mapToObj(i -> IntStream.range(0, SemiField.SIZE).mapToObj(j -> new SemiFieldPoint(i, j))
                    .toArray(SemiFieldPoint[]::new))
            .toArray(SemiFieldPoint[][]::new);

    private final int x;
    private final int y;

    private Set<SemiFieldPoint> line;

    private SemiFieldPoint(int x, int y) {
        this.x = x;
        this.y = y;
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
        return SemiFieldPoint.of(SemiField.mul(this.x, scalar), SemiField.mul(this.y, scalar));
    }

    public Set<SemiFieldPoint> line() {
        if (line == null) {
            line = IntStream.range(0, SemiField.SIZE).filter(i -> i != SemiField.ZERO)
                    .mapToObj(this::mul).collect(Collectors.toSet());
        }
        return line;
    }

    public boolean parallel(SemiFieldPoint that) {
        return SemiField.sub(SemiField.mul(that.x, this.y), SemiField.mul(this.x, that.y)) == SemiField.ZERO;
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
}
