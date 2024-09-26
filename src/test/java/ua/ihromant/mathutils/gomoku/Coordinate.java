package ua.ihromant.mathutils.gomoku;

import java.util.stream.IntStream;
import java.util.stream.Stream;

public record Coordinate(int x, int y) {
    public Stream<Coordinate> neighbors() {
        return IntStream.range(x - 2, x + 2).filter(Model.range::contains).boxed().flatMap(x1 -> IntStream.range(y - 2, y + 2)
                .filter(Model.range::contains).mapToObj(y1 -> new Coordinate(x1, y1)));
    }
}
