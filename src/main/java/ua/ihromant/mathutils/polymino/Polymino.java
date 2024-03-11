package ua.ihromant.mathutils.polymino;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Polymino {
    private final Set<Point> points;

    public Polymino(Set<Point> points) {
        int minX = points.stream().mapToInt(Point::x).min().orElseThrow();
        int minY = points.stream().mapToInt(Point::y).min().orElseThrow();
        this.points = points.stream().map(p -> new Point(p.x() - minX, p.y() - minY)).collect(Collectors.toSet());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Polymino polymino = (Polymino) o;

        return points.equals(polymino.points);
    }

    private Polymino rotate() {
        return new Polymino(points.stream().map(p -> new Point(p.y(), -p.x())).collect(Collectors.toSet()));
    }

    private Polymino flip() {
        return new Polymino(points.stream().map(p -> new Point(p.x(), -p.y())).collect(Collectors.toSet()));
    }

    public Stream<Polymino> variations() {
        return Stream.of(this, this.rotate(), this.rotate().rotate(), this.rotate().rotate().rotate(),
                this.flip(), this.rotate().flip(), this.rotate().rotate().flip(), this.rotate().rotate().rotate().flip());
    }

    public Stream<Polymino> extended() {
        return points.stream().flatMap(Point::neighbors).filter(nb -> !points.contains(nb)).map(nb -> {
            Set<Point> pts = new HashSet<>(points);
            pts.add(nb);
            return new Polymino(pts);
        });
    }

    @Override
    public int hashCode() {
        return points.hashCode();
    }

    public Set<Point> getPoints() {
        return points;
    }

    @Override
    public String toString() {
        int maxX = points.stream().mapToInt(Point::x).max().orElseThrow();
        int maxY = points.stream().mapToInt(Point::y).max().orElseThrow();
        StringBuilder result = new StringBuilder();
        for (int y = 0; y <= maxY; y++) {
            for (int x = 0; x <= maxX; x++) {
                if (points.contains(new Point(x, y))) {
                    result.append("*");
                } else {
                    result.append(' ');
                }
            }
            result.append('\n');
        }
        return result.toString();
    }
}
