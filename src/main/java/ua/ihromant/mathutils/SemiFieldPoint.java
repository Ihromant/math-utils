package ua.ihromant.mathutils;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class SemiFieldPoint {
    private static final SemiFieldPoint[][] CACHE = IntStream.range(0, SemiField.SIZE)
            .mapToObj(i -> IntStream.range(0, SemiField.SIZE).mapToObj(j -> new SemiFieldPoint(i, j))
                    .toArray(SemiFieldPoint[]::new))
            .toArray(SemiFieldPoint[][]::new);

    private final int x;
    private final int y;

    private Map<SemiFieldPoint, Integer> line;

    private SemiFieldPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public static SemiFieldPoint of(int x, int y) {
        return CACHE[x][y];
    }

    public static Stream<SemiFieldPoint> points() {
        return Arrays.stream(CACHE).flatMap(Arrays::stream);
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

    public Map<SemiFieldPoint, Integer> line() {
        if (line == null) {
            line = IntStream.range(0, SemiField.SIZE).filter(i -> i != SemiField.ZERO)
                    .filter(i -> this.mul(i) != CACHE[SemiField.ZERO][SemiField.ZERO]).boxed()
                    .collect(Collectors.toMap(this::mul, Function.identity(), (a, b) -> a));
        }
        return line;
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
