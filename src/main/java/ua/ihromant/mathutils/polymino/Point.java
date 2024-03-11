package ua.ihromant.mathutils.polymino;

import java.util.stream.Stream;

public record Point(int x, int y) {
    public Stream<Point> neighbors() {
        return Stream.of(new Point(x + 1, y), new Point(x - 1, y), new Point(x, y + 1), new Point(x, y - 1));
    }
}
